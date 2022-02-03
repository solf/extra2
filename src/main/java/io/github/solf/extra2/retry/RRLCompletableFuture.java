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
package io.github.solf.extra2.retry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.exception.ExecutionInterruptedRuntimeException;
import io.github.solf.extra2.concurrent.exception.ExecutionRuntimeException;
import io.github.solf.extra2.exception.AssertionException;
import io.github.solf.extra2.retry.RetryAndRateLimitService.RRLEntry;
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
/*package*/ class RRLCompletableFuture<@Nonnull Input, Output> extends CompletableFuture<Output>
	implements RRLFuture<Input, Output>
{
	/**
	 * Entry this future is associated with.
	 */
	private final RRLEntry<@Nonnull Input, Output> entry;
	
	/**
	 * Returns true if this future completed successfully.
	 * <p> 
	 * NOTE: implementation of this is NOT robust here, it should ONLY be set
	 * by {@link RetryAndRateLimitService}
	 */
	@Getter @Setter(AccessLevel.PACKAGE)
	private volatile boolean successful = false;
	

	@Override
	public @Nonnull Input getTask()
	{
		return entry.getInput();
	}

	@Override
	public void requestCancellation()
	{
		entry.setCancelRequested(true);
	}

	@Override
	public Output get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException,
		ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException
	{
		try
		{
			return super.get(timeout, unit);
		} catch (ExecutionException e)
		{
			Throwable cause = e.getCause();
			if (cause == null)
			{
				String msg = "No cause discovered in ExecutionException in RRLCompletableFuture";
				entry.getService().logAssertionError(entry, msg);
				cause = new AssertionException(msg);
			}
			
			if (cause instanceof InterruptedException)
				throw new ExecutionInterruptedRuntimeException((InterruptedException)cause);
			else if (cause instanceof RRLTimeoutException)
				throw new RRLTimeoutException((RRLTimeoutException)cause);
			else
				throw new ExecutionRuntimeException(cause);
		}
	}

	@Override
	public Output get()
		throws InterruptedException, ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException
	{
		return RRLFuture.super.get();
	}
}
