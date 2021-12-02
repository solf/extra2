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
package io.github.solf.extra2.concurrent.retry;

import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Futures returned by {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface RRLFuture<Input, Output> extends Future<Output>
{
	/**
	 * Returns true if this future completed successfully.
	 */
	boolean isSuccessful();
	
	/**
	 * Gets the request/task that this future is for.
	 */
	Input getTask();
	
	/**
	 * This future doesn't support proper semantics of cancel method, therefore
	 * it is recommended to use {@link #requestCancellation()} for clarity.
	 * 
	 * @deprecated use {@link #requestCancellation()} instead
	 */
	@Deprecated
	@Override
	boolean cancel(boolean mayInterruptIfRunning);
	
	
	/**
	 * Requests cancellation of this task; there's no guarantee that cancellation
	 * will happen, it's on the best-attempt basis.
	 * <p>
	 * Check {@link #isCancelled()} to determine if processing was cancelled
	 * (eventually, it is unlikely to happen immediately).
	 * 
	 * @return false if the cancellation request couldn't be carried out (e.g.
	 * 		if it was already done previously or task has been completed);
	 * 		true if an attempt to request cancel succeeded (but no guarantee
	 * 		that it will actually cancel)
	 */
	boolean requestCancellation();
}
