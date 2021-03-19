/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

/**
 * A bean-ified version of {@link WBRBStatus} to be used for e.g. JMX export.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
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
