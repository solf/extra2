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

/**
 * Interface used for requests rate limiting in {@link RetryAndRateLimitService}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface RRLRateLimiter<Ticket>
{
	/**
	 * Obtains (waiting if necessary) processing ticket required to process
	 * a request.
	 * 
	 * @param maxWaitRealMs maximum wait time in real ms; CAN BE zero (do not
	 * 		wait, return ticket if immediately available)
	 * 
	 * @return ticket representation (that can be passed to {@link #returnUnusedTicket(Object)})
	 * 		if successful; null if timed out
	 */
	@Nullable
	public Ticket obtainTicket(long maxWaitRealMs) throws InterruptedException;
	
	/**
	 * Returns processing ticket if that was not used (e.g. after ticket was
	 * obtained the decision was still not to proceed with request).
	 * <p>
	 * This is done 'very soon' after obtaining the ticket unless implementation
	 * somehow makes decision process to be slow.
	 * 
	 * @param ticket instance of the ticket previously obtained from {@link #obtainTicket()}
	 */
	public void returnUnusedTicket(Ticket ticket);
	
	/**
	 * Gets an estimation of the number of currently available tickets.
	 * <p>
	 * This is mostly useful for monitoring, should NOT be relied on for any
	 * business logic.
	 */
	public long getAvailableTicketsEstimation();
}
