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
 * Implementation of {@link RRLRateLimiter} that doesn't place any rate limit
 * restrictions (tickets are always available).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class RRLUnlimitedRateLimiter implements RRLRateLimiter<String>
{

	@Override
	public @Nullable String obtainTicket(long maxWaitRealMs)
		throws InterruptedException
	{
		return "RRLUnlimitedRateLimiter";
	}

	@Override
	public void returnUnusedTicket(String ticket)
	{
		// nothing
	}
}
