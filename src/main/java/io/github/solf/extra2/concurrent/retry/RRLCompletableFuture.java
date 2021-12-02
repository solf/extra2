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

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Actual {@link CompletableFuture} used by {@link RetryAndRateLimitService}
 * under the hood.
 * <p>
 * This is not a generally-usable implementation, therefore it is package-visible only.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
/*package*/ class RRLCompletableFuture<Input, Output> extends CompletableFuture<Output>
	implements RRLFuture<Input, Output>
{
	/**
	 * Returns true if this future completed successfully.
	 * <p> 
	 * NOTE: implementation of this is NOT robust here, it should ONLY be set
	 * by {@link RetryAndRateLimitService}
	 */
	@Getter @Setter(AccessLevel.PACKAGE)
	private volatile boolean successful = false;
	
	/**
	 * Gets the request/task that this future is for.
	 */
	@Getter
	private final Input task;
	
	@Deprecated
	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return requestCancellation();
	}

	@Override
	public boolean requestCancellation()
	{
		// zzz needs impl
		return false;
	}

}
