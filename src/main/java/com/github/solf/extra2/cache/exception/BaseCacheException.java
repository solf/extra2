/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.exception;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base exception to be thrown by cache issues.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class BaseCacheException extends RuntimeException
{
	
	/**
	 * 
	 */
	public BaseCacheException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected BaseCacheException(String message, @Nullable Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public BaseCacheException(String message, @Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public BaseCacheException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public BaseCacheException(@Nullable Throwable cause)
	{
		super(cause);
	}

}
