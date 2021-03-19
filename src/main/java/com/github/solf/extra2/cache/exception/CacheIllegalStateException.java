/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that cache operation failed due to the invalid cache state.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheIllegalStateException extends BaseCacheException
{

	/**
	 * 
	 */
	public CacheIllegalStateException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheIllegalStateException(String message, @Nullable Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheIllegalStateException(String message, @Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheIllegalStateException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheIllegalStateException(@Nullable Throwable cause)
	{
		super(cause);
	}

}
