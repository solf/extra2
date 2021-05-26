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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Context for whatever {@link StepBuilderGenerator} is currently processing.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@AllArgsConstructor
public class StepBuilderContext
{
	/**
	 * Source compilation unit for which we are currently generating.
	 * <p>
	 * Live view.
	 */
	@Getter
	private final CompilationUnit srcCompilationUnit;
	
	/**
	 * Source class for which we are currently generating.
	 * <p>
	 * Live view.
	 */
	@Getter
	private final ClassOrInterfaceDeclaration srcClass;
	
	/**
	 * Generated compilation unit which we are currently generating.
	 * <p>
	 * Live view.
	 */
	@Getter
	private final CompilationUnit generatedCompilationUnit;
	
	/**
	 * Generated top-level builder class which we are currently generating.
	 * <p>
	 * Live view.
	 */
	@Getter
	private final ClassOrInterfaceDeclaration generatedBuilderClass;
	
	
	/**
	 * Source constructor for which we are currently generating.
	 * <p>
	 * Live view.
	 * <p>
	 * Can be null -- in {@link StepBuilderPreprocessor#processBuilderClass(StepBuilderContext, ClassOrInterfaceDeclaration)}
	 */
	@Getter
	@Nullable
	private final ConstructorDeclaration srcConstructor;
	
	/**
	 * Source constructor parameter for which we are currently generating.
	 * <p>
	 * Live view.
	 * <p>
	 * Can be null -- when processing something that is not tied to specific
	 * parameter, e.g. 
	 * {@link StepBuilderPreprocessor#processInterfaceBuildMethod(StepBuilderContext, ClassOrInterfaceDeclaration, com.github.javaparser.ast.body.MethodDeclaration)} 
	 */
	@Getter
	@Nullable
	private final Parameter srcParam;
}
