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
package io.github.solf.extra2.cache.wbrb;

import org.eclipse.jdt.annotation.NonNullByDefault;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

/**
 * A bean-ified version of {@link WBRBStatus} to be used for e.g. JMX export.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@AllArgsConstructor
public class WBRBBeanForStatus
{
	/**
	 * Cache instance to get status from.
	 */
	private final WriteBehindResyncInBackgroundCache<?, ?, ?, ?, ?, ?, ?> cache;
	
	/**
	 * Interval that status data is valid (in ms) for (for caching calculated statuses
	 * to reduce the potential load).
	 * <p>
	 * Use 0 (zero) for no caching. 
	 */
	private final long cacheStatusForMs;
	
	/**
	 * Gets current cache status based on {@link #cacheStatusForMs}
	 */
	@Delegate // expose everything from the status as public methods in here
	protected WBRBStatus getCurrentWBRBStatus()
	{
		return cache.getStatus(cacheStatusForMs);
	}
}
