/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that cache is full and no further elements can be added
 * at this time (for operations that require adding elements).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheFullException extends CacheIllegalStateException
{
	/**
	 * Constructor.
	 */
	public CacheFullException(String cacheName, long currentSize, long maxSize)
	{
		super("Cache [" + cacheName + "] cannot add element due to exceeded capacity: " + Long.toString(currentSize) + "/" + Long.toString(maxSize));
	}
}
