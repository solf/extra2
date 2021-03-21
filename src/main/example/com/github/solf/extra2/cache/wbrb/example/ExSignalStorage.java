/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb.example;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.Setter;

/**
 * Example class -- stores some abstract signal information as byte arrays of
 * hi/lo values. 
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class ExSignalStorage
{
	/**
	 * 'hi' portion of the stored signal
	 */
	@Getter
	@Setter
	private volatile byte[] hi = new byte[] {};
	
	/**
	 * 'lo' portion of the stored signal
	 */
	@Getter
	@Setter
	private volatile byte[] lo = new byte[] {};
}
