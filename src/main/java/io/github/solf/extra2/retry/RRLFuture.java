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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.exception.ExecutionInterruptedRuntimeException;
import io.github.solf.extra2.concurrent.exception.ExecutionRuntimeException;
import io.github.solf.extra2.exception.AssertionException;

/**
 * Futures returned by {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface RRLFuture<Input, Output>
{
	/**
	 * Returns true if this future completed successfully.
	 */
	boolean isSuccessful();

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    boolean isDone();
	
	/**
	 * Gets the request/task that this future is for.
	 */
	Input getTask();
	
	
	/**
	 * Requests cancellation of this task; there's no guarantee that cancellation
	 * will happen, it's on the best-attempt basis.
	 * <p>
	 * Check {@link #isCancelled()} to determine if processing was cancelled
	 * (eventually, it is unlikely to happen immediately).
	 */
	void requestCancellation();

	/**
	 * A version of {@link Future#get(long, TimeUnit)} that replaces {@link ExecutionException}
	 * with {@link ExecutionRuntimeException} and adds some more relevant
	 * exceptions that help to debug the cause.
	 * <p>
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
	 * 
	 * @throws ExecutionInterruptedRuntimeException if the underlying request
	 * 		processing was interrupted
	 * @throws RRLTimeoutException if the underlying request has timed out
	 * 		(haven't been successfully completed before the maximum allowed time elapsed)
	 * @throws ExecutionRuntimeException if the underlying request completed
	 * 		with exception AND no further retries are going to happen; 
	 * 		original exception can be retrieved via {@link ExecutionRuntimeException#getCause()};
	 * 		the exception is wrapped in order to add the get(..) caller's stack trace,
	 * 		otherwise it will be very difficult to trace
	 * @throws CancellationException if the underlying request processing was
	 * 		cancelled (see {@link #requestCancellation()})
	 * @throws TimeoutException if timed out waiting for the result (request is
	 * 		still processing by the time wait has expired)
	 * @throws InterruptedException invoking thread was interrupted while waiting 
	 * 		for the result
	 */
	Output get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException,
		ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException, CancellationException;
	
	/**
	 * A version of {@link #get(long, TimeUnit)} where time limit is always specified in milliseconds
	 * 
	 * @throws ExecutionInterruptedRuntimeException if the underlying request
	 * 		processing was interrupted
	 * @throws RRLTimeoutException if the underlying request has timed out
	 * 		(haven't been successfully completed before the maximum allowed time elapsed)
	 * @throws ExecutionRuntimeException if the underlying request completed
	 * 		with exception AND no further retries are going to happen; 
	 * 		original exception can be retrieved via {@link ExecutionRuntimeException#getCause()};
	 * 		the exception is wrapped in order to add the get(..) caller's stack trace,
	 * 		otherwise it will be very difficult to trace
	 * @throws CancellationException if the underlying request processing was
	 * 		cancelled (see {@link #requestCancellation()})
	 * @throws TimeoutException if timed out waiting for the result (request is
	 * 		still processing by the time wait has expired)
	 * @throws InterruptedException invoking thread was interrupted while waiting 
	 * 		for the result
	 */
	default Output get(long timeoutMs) throws InterruptedException, TimeoutException,
		ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException, CancellationException
	{
		return get(timeoutMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * A version of {@link Future#get()} that replaces {@link ExecutionException}
	 * with {@link ExecutionRuntimeException} and adds some more relevant
	 * exceptions that help to debug the cause.
	 * <p>
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
	 * 
	 * @throws ExecutionInterruptedRuntimeException if the underlying request
	 * 		processing was interrupted
	 * @throws RRLTimeoutException if the underlying request has timed out
	 * 		(haven't been successfully completed before the maximum allowed time elapsed)
	 * @throws ExecutionRuntimeException if the underlying request completed
	 * 		with exception AND no further retries are going to happen; 
	 * 		original exception can be retrieved via {@link ExecutionRuntimeException#getCause()};
	 * 		the exception is wrapped in order to add the get(..) caller's stack trace,
	 * 		otherwise it will be very difficult to trace
	 * @throws CancellationException if the underlying request processing was
	 * 		cancelled (see {@link #requestCancellation()})
	 * @throws InterruptedException invoking thread was interrupted while waiting 
	 * 		for the result
	 */
	default Output get()
		 throws InterruptedException, ExecutionInterruptedRuntimeException, 
		 	RRLTimeoutException, ExecutionRuntimeException, CancellationException
	{
		try
		{
			return get(1234 * 365, TimeUnit.DAYS); // use effectively infinite time
		} catch (TimeoutException e)
		{
			throw new AssertionException("TimeoutException is not supposed to happen here: " + e, e);
		}
	}
	
	/**
	 * A version of {@link #get(long, TimeUnit)} that doesn't throw {@link TimeoutException} --
	 * instead null is returned if data cannot be retrieved in the given time
	 * window.
	 * 
	 * @throws ExecutionInterruptedRuntimeException if the underlying request
	 * 		processing was interrupted
	 * @throws RRLTimeoutException if the underlying request has timed out
	 * 		(haven't been successfully completed before the maximum allowed time elapsed)
	 * @throws ExecutionRuntimeException if the underlying request completed
	 * 		with exception AND no further retries are going to happen; 
	 * 		original exception can be retrieved via {@link ExecutionRuntimeException#getCause()};
	 * 		the exception is wrapped in order to add the get(..) caller's stack trace,
	 * 		otherwise it will be very difficult to trace
	 * @throws CancellationException if the underlying request processing was
	 * 		cancelled (see {@link #requestCancellation()})
	 * @throws InterruptedException invoking thread was interrupted while waiting 
	 * 		for the result
	 */
	@Nullable
	default Output getOrNull(long timeout, TimeUnit unit) throws InterruptedException, 
		ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException, CancellationException
	{
		try
		{
			return get(timeout, unit);
		} catch (TimeoutException e)
		{
			return null;
		}
	}
	
	/**
	 * A version of {@link #get(long, TimeUnit)} that doesn't throw {@link TimeoutException} --
	 * instead null is returned if data cannot be retrieved in the given time
	 * window AND time limit is always specified in milliseconds
	 * 
	 * @throws ExecutionInterruptedRuntimeException if the underlying request
	 * 		processing was interrupted
	 * @throws RRLTimeoutException if the underlying request has timed out
	 * 		(haven't been successfully completed before the maximum allowed time elapsed)
	 * @throws ExecutionRuntimeException if the underlying request completed
	 * 		with exception AND no further retries are going to happen; 
	 * 		original exception can be retrieved via {@link ExecutionRuntimeException#getCause()};
	 * 		the exception is wrapped in order to add the get(..) caller's stack trace,
	 * 		otherwise it will be very difficult to trace
	 * @throws CancellationException if the underlying request processing was
	 * 		cancelled (see {@link #requestCancellation()})
	 * @throws InterruptedException invoking thread was interrupted while waiting 
	 * 		for the result
	 */
	@Nullable
	default Output getOrNull(long timeoutMs) throws InterruptedException, 
		ExecutionInterruptedRuntimeException, RRLTimeoutException, ExecutionRuntimeException, CancellationException
	{
		try
		{
			return get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e)
		{
			return null;
		}
	}
}
