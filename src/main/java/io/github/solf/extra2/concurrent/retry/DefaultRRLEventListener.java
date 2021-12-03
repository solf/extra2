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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;

import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService.RRLAfterRequestAttemptFailedDecision;
import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService.RRLDelayQueueProcessingDecision;
import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService.RRLEntry;
import io.github.solf.extra2.concurrent.retry.RetryAndRateLimitService.RRLMainQueueProcessingDecision;
import io.github.solf.extra2.exception.AssertionException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Events listener for {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
//Exclude TYPE_ARGUMENT as we will allow null return values.
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD, DefaultLocation.TYPE_BOUND, DefaultLocation.ARRAY_CONTENTS})
@RequiredArgsConstructor
@Slf4j
public class DefaultRRLEventListener<@Nonnull Input, Output> implements RRLEventListener<Input, Output>
{
	/**
	 * Service name that this instance is for, used in some log messages.
	 */
	@Getter
	private final String serviceName;
	
	@Override
	public void errorAssertionError(@Nullable RRLEntry<Input, Output> entry, String message)
	{
		log.error("ASSERTION FAILED in {} RetryAndRateLimitService: '{}' while processing: {}", getServiceName(), message, entry, new AssertionException());
	}

	@Override
	public void errorRequestRejected(@Nonnull Input request, long timeLimitMs,
		long delayBeforeFirstAttempMs, @Nonnull String errMsg)
	{
		log.error("Request rejected in {} RetryAndRateLimitService service: '{}' while submitting: {}", getServiceName(), errMsg, request, new RuntimeException());
	}

	@Override
	public void errorSpiMethodException(@Nullable RRLEntry<Input, Output> entry, Throwable t)
	{
		log.error("SPI method exception in {} RetryAndRateLimitService while processing: {}", getServiceName(), entry, t);
	}

	@Override
	public void errorEventListenerMethodException(@Nullable RRLEntry<Input, Output> entry, Throwable t)
	{
		log.error("EventListener method exception in {} RetryAndRateLimitService while processing: {}", getServiceName(), entry, t);
	}

	@Override
	public void errorUnexpectedInterruptedException(InterruptedException e, String message)
	{
		log.error("Unexpected InterruptedException in {} RetryAndRateLimitService: '{}'", getServiceName(), message, e);
	}

	@Override
	public void errorUnexpectedRuntimeException(RuntimeException e, String message)
	{
		log.error("Unexpected RuntimeException in {} RetryAndRateLimitService: '{}'", getServiceName(), message, e);
	}
	
	@Override
	public void requestFinalFailure(RRLEntry<Input, Output> entry, @Nullable Throwable t)
	{
		// empty
	}
	
	@Override
	public void requestFinalTimeout(RRLEntry<Input, Output> entry, long remainingValidityTime)
	{
		// empty
	}
	
	@Override
	public void requestExecuting(RRLEntry<Input, Output> entry, int attemptNumber, long remainingValidityTime)
	{
		// empty
	}
	
	@Override
	public void requestSuccess(RRLEntry<Input, Output> entry, Output result, int attemptNumber, long requestDuration)
	{
		// empty
	}
	
	@Override
	public void requestAttemptFailed(RRLEntry<Input, Output> entry, Exception exception, int attemptNumber, long requestDuration)
	{
		// empty
	}
	
	@Override
	public void requestAttemptFailedDecision(RRLEntry<Input, Output> entry, Pair<RRLAfterRequestAttemptFailedDecision, Long> decision)
	{
		// empty
	}
	
	@Override
	public void requestAdded(RRLEntry<Input, Output> entry)
	{
		// empty
	}
	
	@Override
	public void requestRemoved(RRLEntry<Input, Output> entry)
	{
		// empty
	}
	
	
	@Override
	public void mainQueueProcessingDecision(@Nullable RRLEntry<Input, Output> entry,
		Pair<RRLMainQueueProcessingDecision, Long> decision, long itemProcessingSince)
	{
		// empty
	}
	
	@Override
	public void mainQueueThreadObtained(@Nullable RRLEntry<Input, Output> entry,
		long itemProcessingSince, long timeTakenToObtainThread)
	{
		// empty
	}
	
	@Override
	public void mainQueueTicketObtainAttempt(@Nullable RRLEntry<Input, Output> entry,
		long itemProcessingSince, boolean ticketObtained, long timeTakenVirtualMs)
	{
		// empty
	}
	
	@Override
	public void mainQueueProcessingCompleted(@Nullable RRLEntry<Input, Output> entry,
		long itemProcessingSince, long timeTakenVirtualMs)
	{
		// empty
	}
	
	
	@Override
	public void delayQueueItemBeforeDelayStep(@Nullable RRLEntry<Input, Output> entry, 
		long queueDelayMs, long remainingDelay)
	{
		// empty
	}
	
	@Override
	public void delayQueueDecisionAfterDelayStep(@Nullable RRLEntry<Input, Output> entry, long queueDelayMs, 
		RRLDelayQueueProcessingDecision decision, long sleptFor, long remainingDelay)
	{
		// empty
	}
}
