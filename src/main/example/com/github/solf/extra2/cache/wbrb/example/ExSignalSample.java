/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb.example;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Example signal sample returned from example cache -- contains a pair of
 * hi/lo values.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
/*package*/ class ExSignalSample
{
	/**
	 * 'hi' part of the signal.
	 */
	@Getter
	private final String hi;
	
	/**
	 * 'lo' part of the signal.
	 */
	@Getter
	private final String lo;
}
