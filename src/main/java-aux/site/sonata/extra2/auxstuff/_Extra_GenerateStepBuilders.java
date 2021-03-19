/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.auxstuff;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import site.sonata.extra2.cache.wbrb.WBRBStatus;
import site.sonata.extra2.codegenerate.stepbuilder.StepBuilderContext;
import site.sonata.extra2.codegenerate.stepbuilder.StepBuilderGenerator;

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
