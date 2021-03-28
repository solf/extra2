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
package com.github.solf.extra2.codegenerate.stepbuilder;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Defines processing hooks for adjusting the contents of the files generated
 * by {@link StepBuilderGenerator}.
 * <p>
 * See {@link DefaultStepBuilderPreprocessor} which is a default implementation
 * that is concerned with filtering out unnecessary annotations.
 * 
 * @see DefaultStepBuilderPreprocessor
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface StepBuilderPreprocessor
{

	/**
	 * Processes build() method in the builder interface.
	 * 
	 * @param builderInterface builder interface -- contains only build() method with no args
	 * @param buildMethod actual build method
	 */
	void processInterfaceBuildMethod(StepBuilderContext context,
		ClassOrInterfaceDeclaration builderInterface,
		MethodDeclaration buildMethod);

	/**
	 * Processes (a sole) setter method in a generated step interface.
	 * 
	 * @param stepInterface step interface being generated
	 * @param stepInterfaceMethod method in step interface that is being generated
	 * @param methodParam method parameter
	 */
	void processInterfaceSetter(StepBuilderContext context,
		ClassOrInterfaceDeclaration stepInterface,
		MethodDeclaration stepInterfaceMethod, Parameter methodParam);

	/**
	 * Processes field that will be added to the builder implementation for storing
	 * a particular constructor parameter
	 */
	void processBuilderField(StepBuilderContext context,
		FieldDeclaration field);

	/**
	 * Processes setter method in actual builder implementation. 
	 * 
	 * @param stepMethodDeclaration actual generated method declaration for setting value
	 * @param methodParam method parameter
	 */
	void processBuilderSetter(StepBuilderContext context,
		MethodDeclaration stepMethodDeclaration, Parameter methodParam);

	/**
	 * Processes build() method in actual builder implementation.
	 * 
	 * @param buildMethod actual build method
	 */
	void processBuilderBuildMethod(StepBuilderContext context,
		MethodDeclaration buildMethod);

	/**
	 * Processes static entry points in builder class. 
	 * 
	 * @param entryPointMethodDeclaration actual generated method declaration for entry point
	 * @param methodParam method parameter
	 */
	void processBuilderEntryPoint(StepBuilderContext context,
		MethodDeclaration entryPointMethodDeclaration, Parameter methodParam);

	/**
	 * Processes Builder class itself -- e.g. to remove unnecessary class-level
	 * annotations.
	 */
	void processBuilderClass(StepBuilderContext context,
		ClassOrInterfaceDeclaration generatedBuilderClass);

}
