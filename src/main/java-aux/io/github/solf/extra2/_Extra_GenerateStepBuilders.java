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
package io.github.solf.extra2;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.wbrb.WBRBStatus;
import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderContext;
import io.github.solf.extra2.codegenerate.stepbuilder.StepBuilderGenerator;
import io.github.solf.extra2.log.LoggingStatus;
import io.github.solf.extra2.retry.RRLControlState;
import io.github.solf.extra2.retry.RRLStatus;

/**
 * Generates step builders via {@link StepBuilderGenerator}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
		stepBuilderSuppressUnused.generateBuilderFileForAllFields(RRLStatus.class);
		stepBuilderSuppressUnused.generateBuilderFileForAllFields(RRLControlState.class);
		
		stepBuilderSuppressUnusedAndAllOnFinalBuild.generateBuilderFileForAllFields(WBRBStatus.class);
		stepBuilderSuppressUnusedAndAllOnFinalBuild.generateBuilderFileForAllFields(LoggingStatus.class);
		
		System.out.println("Code generation done.");
	}

}
