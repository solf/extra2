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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.NonNullByDefault;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Default implementation of {@link StepBuilderPreprocessor}
 * <p>
 * This implementation is concerned with filtering out unnecessary annotations
 * using provided white lists (defaults to {@link #getDefaultWhitelistedClassAnnotations()}
 * and {@link #getDefaultWhitelistedParameterAnnotations()}).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
public class DefaultStepBuilderPreprocessor implements StepBuilderPreprocessor
{
	/**
	 * Default set of annotations that are whitelisted for parameters/fields.
	 */
	@Getter
	private static final Set<String> defaultWhitelistedParameterAnnotations = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList("SuppressWarnings", "Nullable", "Nonnull")));

	/**
	 * Default set of annotations that are whitelisted for class.
	 */
	@Getter
	private static final Set<String> defaultWhitelistedClassAnnotations = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList("ParametersAreNonnullByDefault")));
	
	/**
	 * Class annotations that are whitelisted for this instance.
	 */
	private final Set<String> whitelistedClassAnnotations;
	
	/**
	 * Parameter/field annotations that are whitelisted for this instance.
	 */
	private final Set<String> whitelistedParameterAnnotations;
	
	/**
	 * Constructor.
	 * <p>
	 * Uses {@link #getDefaultWhitelistedClassAnnotations()} and {@link #getDefaultWhitelistedParameterAnnotations()}
	 */
	public DefaultStepBuilderPreprocessor()
	{
		this(getDefaultWhitelistedClassAnnotations(), getDefaultWhitelistedParameterAnnotations());
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processInterfaceBuildMethod(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.ClassOrInterfaceDeclaration, com.github.javaparser.ast.body.MethodDeclaration)
	 */
	@Override
	public void processInterfaceBuildMethod(StepBuilderContext context, 
		ClassOrInterfaceDeclaration builderInterface,
		MethodDeclaration buildMethod)
	{
		// nothing in default implementation
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processInterfaceSetter(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.ClassOrInterfaceDeclaration, com.github.javaparser.ast.body.MethodDeclaration, com.github.javaparser.ast.body.Parameter)
	 */
	@Override
	public void processInterfaceSetter(StepBuilderContext context,
		ClassOrInterfaceDeclaration stepInterface,
		MethodDeclaration stepInterfaceMethod, Parameter methodParam)
	{
		filterParameterAnnotations(methodParam.getAnnotations());
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processBuilderField(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.FieldDeclaration)
	 */
	@Override
	public void processBuilderField(StepBuilderContext context, FieldDeclaration field)
	{
		filterParameterAnnotations(field.getAnnotations());
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processBuilderSetter(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.MethodDeclaration, com.github.javaparser.ast.body.Parameter)
	 */
	@Override
	public void processBuilderSetter(StepBuilderContext context,
		MethodDeclaration stepMethodDeclaration,
		Parameter methodParam)
	{
		filterParameterAnnotations(methodParam.getAnnotations());
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processBuilderBuildMethod(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.MethodDeclaration)
	 */
	@Override
	public void processBuilderBuildMethod(StepBuilderContext context, MethodDeclaration buildMethod)
	{
		// nothing in default implementation
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processBuilderEntryPoint(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.MethodDeclaration, com.github.javaparser.ast.body.Parameter)
	 */
	@Override
	public void processBuilderEntryPoint(StepBuilderContext context, MethodDeclaration entryPointMethodDeclaration,
		Parameter methodParam)
	{
		filterParameterAnnotations(methodParam.getAnnotations());
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderPreprocessor#processBuilderClass(io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext, com.github.javaparser.ast.body.ClassOrInterfaceDeclaration)
	 */
	@Override
	public void processBuilderClass(StepBuilderContext context, ClassOrInterfaceDeclaration generatedBuilderClass)
	{
		filterClassAnnotations(generatedBuilderClass.getAnnotations());
	}

	/**
	 * Filters given annotations for a parameter/field using whitelisted 
	 * annotations (for parameters) from the constructor
	 * via calling {@link #filterAnnotations(NodeList, Set)}
	 */
	public void filterParameterAnnotations(NodeList<AnnotationExpr> annotations)
	{
		filterAnnotations(annotations, whitelistedParameterAnnotations);
	}

	/**
	 * Filters given annotations for a class using whitelisted 
	 * annotations (for class) from the constructor
	 * via calling {@link #filterAnnotations(NodeList, Set)}
	 */
	public void filterClassAnnotations(NodeList<AnnotationExpr> annotations)
	{
		filterAnnotations(annotations, whitelistedClassAnnotations);
	}

	/**
	 * Filters given annotations using provided whitelist (only annotations
	 * present in the list are retained).
	 */
	public void filterAnnotations(NodeList<AnnotationExpr> annotations, Set<String> whitelist)
	{
		for (@Nonnull Iterator<@Nonnull AnnotationExpr> iter = annotations.iterator(); iter.hasNext();)
		{
			AnnotationExpr anno = iter.next();
			
			if (!whitelist.contains(anno.getNameAsString()))
				iter.remove();
		}
	}
}
