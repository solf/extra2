/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.codegenerate.stepbuilder;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;

import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContextBuilder;
import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContextBuilder.ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5;
import io.github.solf.extra2.codegenerate.stepbuilder.unused.UnusedInterface;
import io.github.solf.extra2.util.TypeUtil;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Step Builder generator.
 * <p>
 * NB: must have access to source Java code in order to generate builder.
 * <p>
 * The idea is to replace multi-argument constructor invocations (which can
 * get very confusing particularly when many arguments share the same type)
 * with a builder chain like this: PersonBuilder.lastName("Smith").firstName("John"). ... .build()
 * <p>
 * Unlike traditional builder concept (such as in Lombok), Step Builder ensures
 * at compile time that all required parameters have been specified.
 * <p>
 * See https://stackoverflow.com/questions/1638722/how-to-improve-the-builder-pattern/
 * for some related discussion.
 * <p>
 * Example usage:
 * <p>
 * new StepBuilderGenerator("src/main/java", "src/main/java-generated", SuppressWarningsMode.NONE, null).generateBuilderFileForAllConstructors(IORuntimeException.class, null);
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class StepBuilderGenerator
{
	/**
	 * Java parser used by this instance.
	 */
	private final JavaParser instanceJavaParser;
	
	/**
	 * Source directory.
	 */
	private final File srcDir;
	
	/**
	 * Destination directory.
	 */
	private final File destDir;
	
	/**
	 * Selects what is suppressed at the top class level in generated builder --
	 * this is relevant because e.g. unnecessary imports may be copied over.
	 */
	private final SuppressWarningsMode builderClassWarningsSuppressMode;
	
	/*
	 * Preprocessor implementation for processing hooks, see {@link DefaultStepBuilderPreprocessor}
	 * for the default implementation.
	 */
	private final StepBuilderPreprocessor preprocessor;

	/*
	 * If true, additional unused import will be added to the generated class 
	 * ({@link UnusedInterface}) -- this ensures that @SuppressWarnings("unused") 
	 * doesn't generate 'unnecessary warning' warning.
	 */
	private final boolean generateUnusedImport;
	
	/**
	 * If false, the final build method is called 'build()', otherwise it is
	 * called 'buildTargetClassName()'; this mainly has to do with Eclipse's
	 * problem where it takes forever to calculate callers hierarchy for a
	 * build() method (for whatever reason)
	 * 
	 */
	private final boolean generateLongBuildMethodNames;
	
	/**
	 * It seems sometimes combination of complex code & Lombok & Eclipse may
	 * result in unexpected null-warnings in the new CCC(...) code -- which also
	 * may be different depending on whether running full project build or just
	 * the builder file; set this to true to suppress all warnings on the final
	 * build() method to help with this.
	 */
	private final boolean suppressAllWarningsOnBuildMethod;
	
	/**
	 * Optional additional marker annotation to be added to the final buildXXX() method --
	 * e.g. 'javax.annotation.Nonnull' to mark as non-null return for classes
	 * that don't declare {@link ParametersAreNonnullByDefault}
	 */
	@Nullable
	private final String finalBuildMethodAdditionalMarkerAnnotation;

	/**
	 * Constructor.
	 * <p>
	 * Generates unused import, long builder method names, doesn't suppress
	 * warnings on the final build method, doesn't add annotation on final build method, uses 
	 * {@link SuppressWarningsMode#UNUSED} mode, default {@link JavaParser} 
	 * and {@link DefaultStepBuilderPreprocessor}
	 */
	public StepBuilderGenerator(String srcDir, String destDir)
		throws IllegalArgumentException
	{
		this(srcDir, destDir, true, true, false, null, SuppressWarningsMode.UNUSED, null, null);
	}

	/**
	 * Constructor.
	 * <p>
	 * Generates unused import, long builder method names, doesn't add annotation on final build method, uses 
	 * {@link SuppressWarningsMode#UNUSED} mode, default {@link JavaParser} 
	 * and {@link DefaultStepBuilderPreprocessor}
	 */
	public StepBuilderGenerator(String srcDir, String destDir, boolean suppressAllWarningsOnBuildMethod)
		throws IllegalArgumentException
	{
		this(srcDir, destDir, true, true, suppressAllWarningsOnBuildMethod, null, SuppressWarningsMode.UNUSED, null, null);
	}

	/**
	 * Constructor.
	 * <p>
	 * Generates unused import, long builder method names, uses 
	 * {@link SuppressWarningsMode#UNUSED} mode, default {@link JavaParser} 
	 * and {@link DefaultStepBuilderPreprocessor}
	 */
	public StepBuilderGenerator(String srcDir, String destDir, boolean suppressAllWarningsOnBuildMethod, 
		@Nullable String finalBuildMethodAdditionalMarkerAnnotation)
		throws IllegalArgumentException
	{
		this(srcDir, destDir, true, true, suppressAllWarningsOnBuildMethod, finalBuildMethodAdditionalMarkerAnnotation, SuppressWarningsMode.UNUSED, null, null);
	}

	/**
	 * Constructor.
	 * <p>
	 * Generates unused import, long builder method names, doesn't suppress
	 * warnings on the final build method, uses 
	 * {@link SuppressWarningsMode#UNUSED} mode, default {@link JavaParser} 
	 * and {@link DefaultStepBuilderPreprocessor}
	 */
	public StepBuilderGenerator(String srcDir, String destDir, 
		@Nullable String finalBuildMethodAdditionalMarkerAnnotation)
		throws IllegalArgumentException
	{
		this(srcDir, destDir, true, true, false, finalBuildMethodAdditionalMarkerAnnotation, SuppressWarningsMode.UNUSED, null, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param generateLongBuildMethodNames if false, the final build method is
	 * 		called 'build()', otherwise it is called 'buildTargetClassName()';
	 * 		this mainly has to do with Eclipse's problem where it takes forever
	 * 		to calculate callers hierarchy for a build() method (for whatever reason)
	 * @param it seems sometimes combination of complex code & Lombok & Eclipse
	 * 		may result in unexpected null-warnings in the new CCC(...) code --
	 * 		which also may be different depending on whether running full project
	 * 		build or just the builder file; set this to true to suppress all
	 * 		warnings on the final build() method to help with this
	 * @param instanceJavaParser may be null, in which case default-configuration {@link JavaParser} is used
	 * @param preprocessor may be null, in which case {@link DefaultStepBuilderPreprocessor}
	 * 		instance is used
	 */
	public StepBuilderGenerator(String srcDir, String destDir, 
		boolean generateUnusedImport, boolean generateLongBuildMethodNames,
		boolean suppressAllWarningsOnBuildMethod,
		@Nullable String finalBuildMethodAdditionalMarkerAnnotation,
		SuppressWarningsMode builderClassWarningsSuppressMode, 
		@Nullable JavaParser instanceJavaParser, @Nullable StepBuilderPreprocessor preprocessor)
		throws IllegalArgumentException
	{
		this.generateUnusedImport = generateUnusedImport;
		this.generateLongBuildMethodNames = generateLongBuildMethodNames;
		this.suppressAllWarningsOnBuildMethod = suppressAllWarningsOnBuildMethod;
		{
			String tmp = finalBuildMethodAdditionalMarkerAnnotation;
			if (tmp != null)
			{
				if (tmp.startsWith("@") && tmp.length() > 1) // we don't need leading @
					tmp = tmp.substring(1);
			}
			this.finalBuildMethodAdditionalMarkerAnnotation = tmp;
		}
		
		if (instanceJavaParser != null)
			this.instanceJavaParser = instanceJavaParser;
		else
			this.instanceJavaParser = new JavaParser();
		
		if (preprocessor != null)
			this.preprocessor = preprocessor;
		else
			this.preprocessor = new DefaultStepBuilderPreprocessor();
		
		this.srcDir = new File(srcDir);
		if (!this.srcDir.isDirectory())
			throw new IllegalArgumentException("Not a directory (src): " + srcDir);
		this.destDir = new File(destDir);
		if (!this.destDir.isDirectory())
			throw new IllegalArgumentException("Not a directory (dest): " + destDir);
		this.builderClassWarningsSuppressMode = builderClassWarningsSuppressMode;
	}
	
	/**
	 * Generates Step Builder for the given source compilation unit.
	 * 
	 * @param javaParser java parser instance to be used for parsing
	 * @param srcCompilationUnit source compilation unit for which builder will be generated
	 * @param srcClassName class name (simple) for which builders will be generated
	 * @param buildersToGenerate constructors for which builders will be generated
	 * 		+ optional (may be null) suffix used to de-collide constructors
	 *  	that start with the same-named first argument; suffix may also start
	 *  	with '^' symbol in which case it comprises the entire initial builder
	 *  	method name rather than just a suffix ('^' is stripped in this case)
	 */
	@SafeVarargs // this allows to use ... varargs here without warnings
	public final CompilationUnit generate(JavaParser javaParser, CompilationUnit srcCompilationUnit, 
		String srcClassName, String builderClassName, 
		Pair<ConstructorDeclaration, @Nullable String>... buildersToGenerate)
	{
		return generate(javaParser, srcCompilationUnit, srcClassName, builderClassName, Arrays.asList(buildersToGenerate));
	}
		
	/**
	 * Generates Step Builder for the given source compilation unit.
	 * 
	 * @param javaParser java parser instance to be used for parsing
	 * @param srcCompilationUnit source compilation unit for which builder will be generated
	 * @param srcClassName class name (simple) for which builders will be generated
	 * @param buildersToGenerate constructors for which builders will be generated
	 * 		+ optional (may be null) suffix used to de-collide constructors
	 *  	that start with the same-named first argument; suffix may also start
	 *  	with '^' symbol in which case it comprises the entire initial builder
	 *  	method name rather than just a suffix ('^' is stripped in this case)
	 */
	public CompilationUnit generate(JavaParser javaParser, CompilationUnit srcCompilationUnit, 
		String srcClassName, String builderClassName, 
		Collection<Pair<ConstructorDeclaration, @Nullable String>> buildersToGenerate)
	{
		if (buildersToGenerate.size() == 0)
			throw new IllegalArgumentException("Zero builders requested for: " + srcClassName);
		
		final String buildMethodName = generateLongBuildMethodNames ? 
			"build" + srcClassName : "build";
		
		CompilationUnit generatedCompilationUnit = srcCompilationUnit.clone();
		ClassOrInterfaceDeclaration generatedBuilderClass = generatedCompilationUnit.getClassByName(srcClassName).get();
		ClassOrInterfaceDeclaration srcClass = generatedBuilderClass.clone();
		
		boolean postbuildHook = false;
		for (ClassOrInterfaceType impl : srcClass.getImplementedTypes())
		{
			if (impl.getNameAsString().equals(PostbuildHook.class.getSimpleName()))
				postbuildHook = true;
		}
		
		// Set target class name.
		generatedBuilderClass.setName(builderClassName);
		
		ZBSI_StepBuilderContextBuilder_srcCompilationUnit_arg5 contextBuilder = StepBuilderContextBuilder
			.srcCompilationUnit(srcCompilationUnit)
			.srcClass(srcClass)
			.generatedCompilationUnit(generatedCompilationUnit)
			.generatedBuilderClass(generatedBuilderClass)
			;
		
		// Remove all the 'body' of the class
		for (Node node : new ArrayList<>(generatedBuilderClass.getChildNodes()))
		{
			if (node instanceof BodyDeclaration)
				generatedBuilderClass.remove(node); // remove everything from class body.
			else if (node instanceof ClassOrInterfaceType)
				generatedBuilderClass.remove(node); // remove extends and implements
		}
		
		// Pre-process Builder class -- e.g. to remove all unnecessary annotations.
		{
			StepBuilderContext context = contextBuilder.srcConstructor(null).srcParam(null).buildStepBuilderContext();
			
			preprocessor.processBuilderClass(context, generatedBuilderClass);
		}
		
		if (generateUnusedImport)
			generatedCompilationUnit.getImports().addLast(new ImportDeclaration(UnusedInterface.class.getCanonicalName(), false, false));
		
		// Add SuppressWarnings() as required
		{
			String suppress  = builderClassWarningsSuppressMode.getWarningsSuppressString();
			
			if (suppress != null)
			{
				SingleMemberAnnotationExpr anno = new SingleMemberAnnotationExpr();
				anno.setName("SuppressWarnings");
				anno.setMemberValue(new StringLiteralExpr(suppress));
				generatedBuilderClass.addAnnotation(anno);
			}
		}
		
		// top-level class javadoc
		{
			Optional<Javadoc> oJavadoc = generatedBuilderClass.getJavadoc();
			String newJavadocText = "Step Builder class for {@link " + srcClassName + "}";
			if (oJavadoc.isPresent())
				newJavadocText += "\n<p>\n" + oJavadoc.get().toText();
			
			Javadoc newJavadoc = new Javadoc(JavadocDescription.parseText(newJavadocText));
			
			generatedBuilderClass.setJavadocComment(newJavadoc);
		}
		
		// And loop over all constructors to generate builders from.
		HashSet<String> usedUniqueChainIds = new HashSet<>();
		for (Pair<ConstructorDeclaration, @Nullable String> pair : buildersToGenerate)
		{
			ConstructorDeclaration srcConstr = pair.getValue0();
			String uniqueSuffix = pair.getValue1();
			
			// Figure out available javadoc comments for all the args.
			HashMap<String, Javadoc> argToJavadocMap = new HashMap<>();
			HashMap<String, Javadoc> fieldToJavadocMap = new HashMap<>();
			{
				{
					Optional<Javadoc> oJavadoc = srcConstr.getJavadoc();
					if (oJavadoc.isPresent())
					{
						Javadoc javadoc = oJavadoc.get();
						for (JavadocBlockTag jtag : javadoc.getBlockTags())
						{
							if ("param".equals(jtag.getTagName()))
							{
								Optional<String> oName = jtag.getName();
								if (oName.isPresent())
								{
									Javadoc argJavadoc = new Javadoc(JavadocDescription.parseText(jtag.getContent().toText()));
									if (argToJavadocMap.put(oName.get(), argJavadoc) != null)
										throw new IllegalStateException("Duplicate comment for: " + jtag);
								}
							}
						}
					}
				}
				
				for (FieldDeclaration field : srcClass.findAll(FieldDeclaration.class))
				{
					Optional<Javadoc> oJavadoc = field.getJavadoc();
					if (oJavadoc.isPresent())
					{
						Javadoc javadoc = oJavadoc.get();
						for (VariableDeclarator var : field.getVariables())
						{
							argToJavadocMap.put(var.getNameAsString(), javadoc);
							fieldToJavadocMap.put(var.getNameAsString(), javadoc);
						}
					}
				}
				
			}

			final NodeList<Type> typeArguments;
			{
				NodeList<TypeParameter> typeParameters = generatedBuilderClass.getTypeParameters();
				if (typeParameters.isEmpty())
					typeArguments = null;
				else
					typeArguments = TypeUtil.coerceForceNonnull((Object)typeParameters); // a hack
			}
			
			NodeList<Parameter> srcParameters = srcConstr.getParameters();
			if (srcParameters.size() == 0)
				throw new IllegalStateException("Unable to create Builder for constructor with zero arguments: " + srcConstr);
			
			final Parameter firstParameter = srcParameters.get(0);
			
			// Used to de-collide multiple builder chains.
			final String uniqueChainId;
			if (uniqueSuffix != null)
			{
				if (uniqueSuffix.startsWith("^"))
					uniqueChainId = uniqueSuffix.substring(1);
				else
					uniqueChainId = firstParameter.getNameAsString() + uniqueSuffix;
			}
			else
				uniqueChainId = firstParameter.getNameAsString();
			
			if (!usedUniqueChainIds.add(uniqueChainId))
				throw new IllegalStateException("Duplicate unique identification of builder chain: '" + uniqueChainId + "'; use suffixes to distinguish between constructors with the same-named first argument.");
			
			final String interfaceNamePrefix = "ZBSI_" + builderClassName + "_" + uniqueChainId + "_";
			
			final String builderInterfaceName = interfaceNamePrefix + "builder";
			final String builderImplName = interfaceNamePrefix + "builderClass";
			
			ArrayList<FieldDeclaration> builderFields = new ArrayList<>(srcParameters.size());
			ArrayList<MethodDeclaration> builderMethods = new ArrayList<>(srcParameters.size());
			ArrayList<ClassOrInterfaceType> builderImplementedInterfaces = new ArrayList<>(srcParameters.size() + 1);

			@Nonnull ClassOrInterfaceDeclaration firstInterface;
			@Nonnull ClassOrInterfaceDeclaration secondInterface = fakeNonNull();
			
			// Create builder interface.
			final ClassOrInterfaceType finalReturnType;
			{
				StepBuilderContext context = contextBuilder.srcConstructor(srcConstr).srcParam(null).buildStepBuilderContext();
				
				finalReturnType = new ClassOrInterfaceType(null, srcClassName);
				finalReturnType.setTypeArguments(typeArguments);

				ClassOrInterfaceDeclaration iface = new ClassOrInterfaceDeclaration(
					NodeList.nodeList(Modifier.publicModifier()), true, builderInterfaceName);
				setTypeParameters(iface, typeArguments);
				
				MethodDeclaration method = new MethodDeclaration(
					NodeList.nodeList(Modifier.publicModifier()), 
					buildMethodName, 
					finalReturnType, 
					NodeList.nodeList()
				).removeBody(); // removeBody() removes empty method body which is required for interface to be correct
				if (finalBuildMethodAdditionalMarkerAnnotation != null)
					method.addAnnotation(new MarkerAnnotationExpr(finalBuildMethodAdditionalMarkerAnnotation));
				iface.addMember(method); 
				
				preprocessor.processInterfaceBuildMethod(context, iface, method);
				
				generatedBuilderClass.addMember(iface);
				builderImplementedInterfaces.add(toClassOrInterfaceType(iface));
				
				firstInterface = iface;
			}
			
			// In reverse constructor argument order.
			boolean last = true;
			
			for (int i = srcParameters.size(); i > 0; i--)
			{
				Parameter srcParam = srcParameters.get(i - 1);
				
				StepBuilderContext context = contextBuilder.srcConstructor(srcConstr).srcParam(srcParam).buildStepBuilderContext();
				
				MethodDeclaration methodDeclaration;				
				// Build step interface
				{
					final String returnTypeName = last ? builderInterfaceName : 
						interfaceNamePrefix + "arg" + (i + 1);
					last = false;
					
					final String interfaceName = interfaceNamePrefix + "arg" + i;
					
					ClassOrInterfaceType returnType = new ClassOrInterfaceType(null, returnTypeName);
					returnType.setTypeArguments(typeArguments);
	
					ClassOrInterfaceDeclaration iface = new ClassOrInterfaceDeclaration(
						NodeList.nodeList(Modifier.publicModifier()), true, interfaceName);
					setTypeParameters(iface, typeArguments);
					
					String argName = srcParam.getNameAsString();
					methodDeclaration = new MethodDeclaration(
						NodeList.nodeList(Modifier.publicModifier()), 
						argName, 
						returnType, 
						NodeList.nodeList(srcParam.clone())
					);
					{
						Javadoc javadoc = argToJavadocMap.get(argName);
						if (javadoc != null)
							methodDeclaration.setJavadocComment(javadoc);
					}
					MethodDeclaration method = methodDeclaration.clone() 
						.removeBody(); // removeBody() removes empty method body which is required for interface to be correct
					iface.addMember(method);
					
					preprocessor.processInterfaceSetter(context, iface, method, method.getParameter(0));
					
					generatedBuilderClass.addMember(iface);
					builderImplementedInterfaces.add(toClassOrInterfaceType(iface));
					
					secondInterface = firstInterface;
					firstInterface = iface;
				}
				
				// Build field for storing the value
				{
					Type fieldType = srcParam.getType();
					FieldDeclaration field = new FieldDeclaration(
						NodeList.nodeList(Modifier.privateModifier()), 
						fieldType, 
						srcParam.getNameAsString()
					);
					
					// Copy annotations over
					field.setAnnotations(new NodeList<>(srcParam.getAnnotations()));
					
					{
						SingleMemberAnnotationExpr anno = new SingleMemberAnnotationExpr();
						anno.setName("SuppressWarnings");
						anno.setMemberValue(new StringLiteralExpr("all"));
						field.addAnnotation(anno);
					}
					
					preprocessor.processBuilderField(context, field);
					
					builderFields.add(field);
				}
				
				// Build method for actually doing stuff.
				{
					methodDeclaration.addAnnotation(new MarkerAnnotationExpr("Override"));
					{
						SingleMemberAnnotationExpr anno = new SingleMemberAnnotationExpr();
						anno.setName("SuppressWarnings");
						anno.setMemberValue(new StringLiteralExpr("hiding"));
						methodDeclaration.addAnnotation(anno);
					}
					
					methodDeclaration.setBody(handleResult(javaParser.parseBlock(
						"{ this." + srcParam.getNameAsString() + "= " + srcParam.getNameAsString() + "; return this; }")));
					
					preprocessor.processBuilderSetter(context, methodDeclaration, methodDeclaration.getParameter(0));
					
					builderMethods.add(methodDeclaration);
				}
			}
			
			// Build builder instance itself.
			ClassOrInterfaceDeclaration builderImpl;
			{
				builderImpl = new ClassOrInterfaceDeclaration(
					NodeList.nodeList(Modifier.privateModifier(), Modifier.staticModifier(), Modifier.finalModifier()), 
					false /*interface flag*/,  
					builderImplName
				);
				setTypeParameters(builderImpl, typeArguments);
				
				for (ClassOrInterfaceType iface : builderImplementedInterfaces)
					builderImpl.addImplementedType(iface);
				
				for (FieldDeclaration field : builderFields)
					builderImpl.addMember(field);
				
				for (MethodDeclaration method : builderMethods)
					builderImpl.addMember(method);
				
				// Final 'build()' method in builder impl
				{
					StepBuilderContext context = contextBuilder.srcConstructor(srcConstr).srcParam(null).buildStepBuilderContext();
					
					MethodDeclaration methodDeclaration = new MethodDeclaration(
						NodeList.nodeList(Modifier.publicModifier()), 
						buildMethodName, 
						finalReturnType, 
						NodeList.nodeList()
					); 
					
					methodDeclaration.addAnnotation(new MarkerAnnotationExpr("Override"));
					if (finalBuildMethodAdditionalMarkerAnnotation != null)
						methodDeclaration.addAnnotation(new MarkerAnnotationExpr(finalBuildMethodAdditionalMarkerAnnotation));
					
					if (suppressAllWarningsOnBuildMethod)
					{
						SingleMemberAnnotationExpr anno = new SingleMemberAnnotationExpr();
						anno.setName("SuppressWarnings");
						anno.setMemberValue(new StringLiteralExpr("all"));
						methodDeclaration.addAnnotation(anno);
					}
					
					StringBuilder sb = new StringBuilder(256);
					sb.append("{ return ");
					if (postbuildHook)
						sb.append("(" + srcClassName + ")"); // Need to cast since the method itself returns Object
					sb.append("new ");
					sb.append(finalReturnType);
					sb.append('(');
					
					boolean first = true;
					for (Parameter param : srcParameters) 
					{
						if (!first)
							sb.append(", ");
						first = false;
						
						sb.append(param.getNameAsString());
					}
					
					sb.append(")");
					if (postbuildHook)
						sb.append(".postbuild()");
					sb.append("; }");

					methodDeclaration.setBody(handleResult(javaParser.parseBlock(sb.toString())));
					
					preprocessor.processBuilderBuildMethod(context, methodDeclaration);
					
					builderImpl.addMember(methodDeclaration);
				}
				
				generatedBuilderClass.addMember(builderImpl);
				
				// Static entry point in top-level builder class to create builder implementation. 
				{
					StepBuilderContext context = contextBuilder.srcConstructor(srcConstr).srcParam(firstParameter).buildStepBuilderContext();
					
					String argName = firstParameter.getNameAsString();
					
					MethodDeclaration methodDeclaration = new MethodDeclaration(
						NodeList.nodeList(Modifier.publicModifier(), Modifier.staticModifier()), 
						uniqueChainId, 
						toClassOrInterfaceType(secondInterface), 
						NodeList.nodeList(firstParameter.clone())
					); 
					
					setTypeParameters(methodDeclaration, typeArguments);
					methodDeclaration.setBody(handleResult(javaParser.parseBlock(
						"{ return new " + toClassOrInterfaceType(builderImpl) + "()." + argName + "(" + argName + "); }")));
					
					Javadoc fieldJavadoc = fieldToJavadocMap.get(argName);
					
					Javadoc mainJavadoc;
					Optional<Javadoc> oConstrJavadoc = srcConstr.getJavadoc();
					if (oConstrJavadoc.isPresent())
						mainJavadoc = oConstrJavadoc.get();
					else
						mainJavadoc = argToJavadocMap.get(argName);
					
					if (mainJavadoc == null)
					{
						if (fieldJavadoc != null)
							methodDeclaration.setJavadocComment(fieldJavadoc);
					}
					else
					{
						Javadoc newJavadoc;
						if (fieldJavadoc == null)
							newJavadoc = mainJavadoc;
						else
						{
							// Have to combine two Javadocs, field goes first because it skips tags
							String fieldJavadocDesc = fieldJavadoc.getDescription().toText();
							newJavadoc = new Javadoc(JavadocDescription.parseText(
								"FIELD COMMENT: " + fieldJavadocDesc + "\n<p>\nCONSTRUCTOR COMMENT: " + mainJavadoc.toText()));
						}
						
						methodDeclaration.setJavadocComment(newJavadoc);
					}
					
					preprocessor.processBuilderEntryPoint(context, methodDeclaration, methodDeclaration.getParameter(0));
					
					generatedBuilderClass.addMember(methodDeclaration);
				}
			}
		} // end of constructor loop
		
		return generatedCompilationUnit;
	}
	
	/**
	 * Converts ClassOrInterfaceDeclaration to the corresponding ClassOrInterfaceType
	 * (i.e. from class/interface definition with body etc. to class/interface
	 * class reference including all type parameters). 
	 */
	private static ClassOrInterfaceType toClassOrInterfaceType(ClassOrInterfaceDeclaration src)
	{
		src = src.clone(); // for safety
		ClassOrInterfaceType result = new ClassOrInterfaceType(null, src.getNameAsString());
		
		setTypeArguments(result, src.getTypeParameters());
		
		return result;
	}
	
	/**
	 * Handles result from {@link JavaParser} -- throws exception if not successful,
	 * returns result if everything is ok.
	 */
    private static <T extends Node> T handleResult(ParseResult<T> result) {
        if (result.isSuccessful()) {
            return result.getResult().get();
        }
        throw new ParseProblemException(result.getProblems());
    }
    
    /**
     * This only exists because it is impossible to remove parameters once they
     * are set via setTypeParameters() -- and it is also not allowed to set null parameters.
     * <p>
     * So to avoid calling setTypeParameters() accidentally, variable with them
     * is not made directly available.
     */
    private static void setTypeParameters(NodeWithTypeParameters<?> target, @Nullable NodeList<Type> typeArguments)
    {
    	if ((typeArguments == null) || (typeArguments.isEmpty()))
    		return;
    	
    	target.setTypeParameters(TypeUtil.coerceForceNonnull((Object)new NodeList<>(typeArguments))); // a hack
    }
    
    /**
     * This only exists to avoid problems with setting empty type arguments, see
     * {@link #setTypeParameters(NodeWithTypeParameters, NodeList)} for more details.
     */
    private static void setTypeArguments(NodeWithTypeArguments<?> target, @Nullable NodeList<TypeParameter> typeArguments)
    {
    	if ((typeArguments == null) || (typeArguments.isEmpty()))
    		return;
    	
    	NodeList<Type> args = TypeUtil.coerceForceNonnull((Object)new NodeList<>(typeArguments)); // a hack
    	target.setTypeArguments(args);
    }

    /**
     * Generates Step Builder Java file for the specific class.
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param srcClass source class, used to determine where to find source
     * 		Java file & determine name of the class for which builder is generated
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     * @param constructorsSelector this function is given parsed source Java
     * 		file and must provide a list of constructors + optional de-collision
     * 		suffixes for which builders will be generated; see {@link #generate(JavaParser, CompilationUnit, String, String, Collection)}
     * 		for more details about suffixes
     */
    public CompilationUnit generateBuilderFile(Class<?> srcClass, @Nullable String builderClassName,
    	Function<ClassOrInterfaceDeclaration, Collection<Pair<ConstructorDeclaration, @Nullable String>>> constructorsSelector
    )
    throws IllegalStateException, IOException
    {
    	final String fullPackageName = srcClass.getPackage().getName();
    	final String srcClassName = srcClass.getSimpleName();
    	
    	final String javaPackagePath = fullPackageName.replaceAll("\\.", "/");
    	
    	File srcFile = new File(new File(srcDir, javaPackagePath), srcClassName + ".java");
    	if (!srcFile.isFile())
    		throw new IllegalStateException("Not a file (src): " + srcFile);

		CompilationUnit srcCompilationUnit = handleResult(instanceJavaParser.parse(srcFile));
    	
		ClassOrInterfaceDeclaration srcClazz = srcCompilationUnit.getClassByName(srcClassName).get();
    	
		final String finalBuilderClassName = builderClassName != null ? builderClassName : srcClassName + "Builder";
		
		CompilationUnit result = generate(instanceJavaParser, srcCompilationUnit, 
			srcClassName, finalBuilderClassName, 
			constructorsSelector.apply(srcClazz.clone())); // clone to avoid interference
		
		File outDir = new File(destDir, javaPackagePath);
		if (!outDir.isDirectory())
			if (!outDir.mkdirs())
				throw new IllegalStateException("Failed to create required directories [dest]: " + outDir);
		
		File outFile = new File(outDir, finalBuilderClassName + ".java");
		if (outFile.isDirectory())
			throw new IllegalStateException("Output file is a directory [dest]: " + outFile);
		
		Files.write(outFile.toPath(), result.toString().getBytes());
		
		return result;
    }
    
    /**
     * Generates Step Builder for a given class based on the presence of
     * {@link StepBuilder} annotations on constructors.
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     */
    public CompilationUnit generateBuilderFileByAnnotation(Class<?> srcClass, @Nullable String builderClassName)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFile(srcClass, builderClassName, clazz ->
    	{
    		ArrayList<Pair<ConstructorDeclaration, @Nullable String>> matchingConstructors = new ArrayList<>();
    		
    		for (ConstructorDeclaration constr: clazz.getConstructors())
    		{
    			Optional<AnnotationExpr> oAnno = constr.getAnnotationByClass(StepBuilder.class);
    			if (oAnno.isPresent())
    			{
    				final AnnotationExpr anno = oAnno.get();
    				final String suffix; 
    				if (anno instanceof MarkerAnnotationExpr)
    					suffix = null;
    				else if (anno instanceof SingleMemberAnnotationExpr)
    					suffix = ((StringLiteralExpr)((SingleMemberAnnotationExpr)anno).getMemberValue()).asString();
    				else
    					throw new IllegalStateException("Unsupported annotation type: " + anno.getClass() + " -- " + anno);
    					
					matchingConstructors.add(new Pair<>(constr, suffix));
    			}
    		}
    		
    		return matchingConstructors;
    	});
    }
    
    /**
     * Generates Step Builder for a given class based on the presence of
     * {@link StepBuilder} annotations on constructors.
     * <p>
     * Uses default builder class name: source name + 'Builder'
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     */
    public CompilationUnit generateBuilderFileByAnnotation(Class<?> srcClass)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFileByAnnotation(srcClass, null);
    }
    

    /**
     * Generates Step Builder for a given class based for all fields present
     * in the class (the corresponding all-fields constructor must also be present);
     * this is mostly useful in combination with {@link AllArgsConstructor}
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     * @param decollisionSuffix de-collision suffix (may be null); see {@link #generate(JavaParser, CompilationUnit, String, String, Collection)}
     * 		for more details about suffixes
     */
    public CompilationUnit generateBuilderFileForAllFields(Class<?> srcClass, 
    	@Nullable String builderClassName, @Nullable String decollisionSuffix)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFile(srcClass, builderClassName, clazz ->
    	{
    		ArrayList<Parameter> parameters = new ArrayList<>();
    		
			for (FieldDeclaration field : clazz.findAll(FieldDeclaration.class))
			{
				// skip static fields
				if (!field.getModifiers().contains(Modifier.staticModifier()))
				{
					for (VariableDeclarator var : field.getVariables())
					{
						Parameter parameter = new Parameter(var.getType(), var.getName());
						parameter.setAnnotations(field.getAnnotations());
						
						parameters.add(parameter);
					}
				}
			}
			
			ConstructorDeclaration constr = new ConstructorDeclaration();
			constr.setParameters(new NodeList<Parameter>(parameters));
			
			return Arrays.asList(new Pair<>(constr, decollisionSuffix));
    	});
    }

    /**
     * Generates Step Builder for a given class based for all fields present
     * in the class (the corresponding all-fields constructor must also be present);
     * this is mostly useful in combination with {@link AllArgsConstructor}
     * <p>
     * Uses default builder class name: source name + 'Builder'
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     */
    public CompilationUnit generateBuilderFileForAllFields(Class<?> srcClass)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFileForAllFields(srcClass, null, null);
    }
    

    /**
     * Generates Step Builder for a given class based for required fields present
     * in the class (the corresponding all-fields constructor must also be present);
     * this is mostly useful in combination with {@link RequiredArgsConstructor}
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     * @param decollisionSuffix de-collision suffix (may be null); see {@link #generate(JavaParser, CompilationUnit, String, String, Collection)}
     * 		for more details about suffixes
     */
    public CompilationUnit generateBuilderFileForRequiredFields(Class<?> srcClass, 
    	@Nullable String builderClassName, @Nullable String decollisionSuffix)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFile(srcClass, builderClassName, clazz ->
    	{
    		ArrayList<Parameter> parameters = new ArrayList<>();
    		
			for (FieldDeclaration field : clazz.findAll(FieldDeclaration.class))
			{
				boolean include = false;
				
				if (field.getModifiers().contains(Modifier.finalModifier()))
					include = true;
				
				if (field.getAnnotationByClass(NonNull.class).isPresent())
					include = true;
				
				if (field.getAnnotationByClass(Nonnull.class).isPresent())
					include = true;
				
				if (include)
				{
					for (VariableDeclarator var : field.getVariables())
					{
						Parameter parameter = new Parameter(field.getElementType(), var.getName());
						parameter.setAnnotations(field.getAnnotations());
						
						parameters.add(parameter);
					}
				}
			}
			
			ConstructorDeclaration constr = new ConstructorDeclaration();
			constr.setParameters(new NodeList<Parameter>(parameters));
			
			return Arrays.asList(new Pair<>(constr, decollisionSuffix));
    	});
    }

    /**
     * Generates Step Builder for a given class based for all fields present
     * in the class (the corresponding all-fields constructor must also be present);
     * this is mostly useful in combination with {@link RequiredArgsConstructor}
     * <p>
     * Uses default builder class name: source name + 'Builder'
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     */
    public CompilationUnit generateBuilderFileForRequiredFields(Class<?> srcClass)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFileForRequiredFields(srcClass, null, null);
    }
    

    /**
     * Generates Step Builder for a given class based for all constructors present
     * in the class.
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     */
    public CompilationUnit generateBuilderFileForAllConstructors(Class<?> srcClass, 
    	@Nullable String builderClassName)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFile(srcClass, builderClassName, clazz ->
    	{
    		ArrayList<Pair<ConstructorDeclaration, @Nullable String>> matchingConstructors = new ArrayList<>();
    		
    		for (ConstructorDeclaration constr: clazz.getConstructors())
					matchingConstructors.add(new Pair<>(constr, null));
    		
    		return matchingConstructors;
    	});
    }
    
    /**
     * Generates Step Builder for a given class based for all constructors present
     * in the class.
     * <p>
     * Uses default builder class name: source name + 'Builder'
     * <p>
     * Source files is read from source directory, generated file is written
     * into destination directory.
     * 
     * @param builderClassName class name for the generated builder; if null, 
     * 		default name is used (source name + 'Builder'); package is the same as source
     */
    public CompilationUnit generateBuilderFileForAllConstructors(Class<?> srcClass)
    	throws IllegalStateException, IOException 
    {
    	return generateBuilderFileForAllConstructors(srcClass, null);
    }
}
