/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that cache operation failed due to the invalid EXTERNAL cache state,
 * such as problems accessing the underlying storage.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class CacheIllegalExternalStateException extends CacheIllegalStateException
{

	/**
	 * 
	 */
	public CacheIllegalExternalStateException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheIllegalExternalStateException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheIllegalExternalStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheIllegalExternalStateException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheIllegalExternalStateException(Throwable cause)
	{
		super(cause);
	}

}
