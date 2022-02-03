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

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;

/**
 * Controls the running state of {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@RequiredArgsConstructor
@ToString
@NonNullByDefault
public class RRLControlState
{
	/**
	 * Initial service state -- before it started.
	 * <p>
	 * Service is set to this state when started.
	 */
	public static RRLControlState NOT_STARTED = RRLControlStateBuilder
		.description("NOT_STARTED")
		.rejectRequestsString("Service has not been started yet.")
		.ignoreDelays(false)
		.timeoutAllPendingRequests(false)
		.timeoutRequestsAfterFailedAttempt(false)
		.spooldownTargetTimestamp(-1)
		.limitWaitingForProcessingThread(false)
		.limitWaitingForTicket(false)
		.waitForTickets(true)
		.buildRRLControlState();
	
	/**
	 * Standard 'running' service state.
	 * <p>
	 * Service is set to this state when started.
	 */
	public static RRLControlState RUNNING = RRLControlStateBuilder
		.description("RUNNING")
		.rejectRequestsString(null)
		.ignoreDelays(false)
		.timeoutAllPendingRequests(false)
		.timeoutRequestsAfterFailedAttempt(false)
		.spooldownTargetTimestamp(-1)
		.limitWaitingForProcessingThread(false)
		.limitWaitingForTicket(false)
		.waitForTickets(true)
		.buildRRLControlState();
	
	/**
	 * Service is being shut down.
	 * <p>
	 * NOTE: this probably should be customized with {@link #withSpooldownTargetTimestamp(long)}
	 * and other values.
	 */
	public static RRLControlState SHUTDOWN_IN_PROGRESS = RRLControlStateBuilder
		.description("SHUTDOWN_IN_PROGRESS")
		.rejectRequestsString("Service is being shut down.")
		.ignoreDelays(true)
		.timeoutAllPendingRequests(false)
		.timeoutRequestsAfterFailedAttempt(true)
		.spooldownTargetTimestamp(-1)
		.limitWaitingForProcessingThread(true)
		.limitWaitingForTicket(true)
		.waitForTickets(true)
		.buildRRLControlState();
	
	/**
	 * Service has been shutdown.
	 */
	public static RRLControlState SHUTDOWN = RRLControlStateBuilder
		.description("SHUTDOWN")
		.rejectRequestsString("Service has been shut down.")
		.ignoreDelays(true)
		.timeoutAllPendingRequests(false)
		.timeoutRequestsAfterFailedAttempt(true)
		.spooldownTargetTimestamp(-1)
		.limitWaitingForProcessingThread(true)
		.limitWaitingForTicket(true)
		.waitForTickets(true)
		.buildRRLControlState();

	
	/**
	 * Description of this control state for logging and similar stuff (e.g.
	 * "NOT_STARTED", "RUNNING", "SHUTDOWN").
	 */
	@Getter
	private final String description;
	
	/**
	 * Null value means requests are accepted; non-null value means they are
	 * rejected with the given string as part of an exception.
	 */
	@Getter
	@Nullable
	private final String rejectRequestsString;
	
	/**
	 * Whether specified delays (initial and after retries) are respected; if
	 * true, the delays will be ignored (which might be useful during the
	 * shutdown and/or spooldown).
	 */
	@With
	@Getter
	private final boolean ignoreDelays;
	
	/**
	 * If true, all pending requests will timeout without making further attempts;
	 * this can be useful e.g. during quick shutdown.
	 */
	@Getter
	private final boolean timeoutAllPendingRequests;
	
	/**
	 * If true, requests will automatically time-out after a failed attempt;
	 * this can be useful e.g. during shutdown.
	 */
	@Getter
	private final boolean timeoutRequestsAfterFailedAttempt;
	
	/**
	 * Timestamp by which service should attempt to spooldown (complete 
	 * processing of all the pending requests).
	 * <p>
	 * Non-positive value means there's no target for spooldown.
	 */
	@With
	@Getter
	private final long spooldownTargetTimestamp;
	
	/**
	 * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then 
	 * waiting for processing thread is limited by however many
	 * requests are still to be processed (are in main queue) and the spooldown
	 * target timestamp.
	 */
	@Getter
	private final boolean limitWaitingForProcessingThread;
	
	/**
	 * If true AND {@link #getSpooldownTargetTimestamp()} is positive, then 
	 * waiting for ticket is limited by however many
	 * requests are still to be processed (are in main queue) and the spooldown
	 * target timestamp.
	 */
	@Getter
	private final boolean limitWaitingForTicket;
	
	/**
	 * Specifies whether to wait for tickets:
	 * <p>
	 * true -- wait for tickets normally (potentially indefinitely, depending
	 * 		on {@link #isLimitWaitingForTicket()})
	 * <p>
	 * false -- do not wait for tickets at all, obtain ticket if immediately
	 * 		available, otherwise fail
	 * <p>
	 * NULL -- ignore tickets completely, assume they are available and proceed
	 * 		with processing 
	 */
	@With
	@Getter
	@Nullable
	private final Boolean waitForTickets;
}
