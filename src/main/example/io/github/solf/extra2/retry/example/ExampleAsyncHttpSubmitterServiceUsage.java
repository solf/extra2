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
package io.github.solf.extra2.retry.example;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.exception.ExecutionInterruptedRuntimeException;
import io.github.solf.extra2.concurrent.exception.ExecutionRuntimeException;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.retry.RRLConfig;
import io.github.solf.extra2.retry.RRLFuture;
import io.github.solf.extra2.retry.RRLTimeoutException;

/**
 * An example service that can be used to asynchronously submit (and rate-limit)
 * requests to some HTTP endpoint.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
/*package*/ class ExampleAsyncHttpSubmitterServiceUsage
{

	/**
	 * Main method
	 */
	public static void main(String[] args) throws CancellationException, ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException, InterruptedException, TimeoutException
	{
		// Use test config for some valid configuration
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("retry/simpleCasesTest.properties");
		
		ExampleAsyncHttpSubmitterService service = new ExampleAsyncHttpSubmitterService(new RRLConfig(config));
		service.start();
		try
		{
			final long start = System.currentTimeMillis();
			RRLFuture<String, Integer> future = service.submitFor("asdqwe", 10000);
			
			System.out.println("Result code: " + future.get(10000) + "   in " + (System.currentTimeMillis() - start) + " ms");
		} finally
		{
			if (service.shutdownFor(10000, true, false) != 0)
				throw new IllegalStateException("Failed to cleanly shutdown");
		}
	}

	
}
