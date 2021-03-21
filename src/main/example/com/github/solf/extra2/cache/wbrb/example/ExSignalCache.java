/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb.example;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Example in-cache representation of signal storage.
 * <p>
 * This consists of 'our' value which is modifiable and 'other' value which is
 * fixed at the time of read.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@AllArgsConstructor
/*package*/ class ExSignalCache
{
	/**
	 * 'our' signal value, modifiable.
	 */
	@Getter
	private final StringBuilder ourSignalValue;
	
	/**
	 * 'other' signal value, not modifiable
	 */
	@Getter
	private final String otherSignalValue;
}
