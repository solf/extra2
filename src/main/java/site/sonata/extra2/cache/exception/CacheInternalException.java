/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that cache operation failed due to some kind of internal exception.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheInternalException extends BaseCacheException
{

	/**
	 * 
	 */
	public CacheInternalException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheInternalException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheInternalException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheInternalException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheInternalException(Throwable cause)
	{
		super(cause);
	}

}
