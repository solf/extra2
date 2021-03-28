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
package com.github.solf.extra2;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WBRBStatus;
import com.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext;
import com.github.solf.extra2.codegenerate.stepbuilder.StepBuilderGenerator;

/**
 * Generates step builders via {@link StepBuilderGenerator}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class _Extra_GenerateStepBuilders
{

	/**
	 * Main.
	 */
	public static void main(String[] args) throws IOException
	{
		final StepBuilderGenerator stepBuilderSuppressUnused = new StepBuilderGenerator(
			"src/main/java", "src/main/java-generated");
		final StepBuilderGenerator stepBuilderSuppressUnusedAndAllOnFinalBuild = new StepBuilderGenerator(
			"src/main/java", "src/main/java-generated", true);
		
		stepBuilderSuppressUnused.generateBuilderFileForAllFields(StepBuilderContext.class);
		
		stepBuilderSuppressUnusedAndAllOnFinalBuild.generateBuilderFileForAllFields(WBRBStatus.class);
		
		System.out.println("Code generation done.");
	}

}
