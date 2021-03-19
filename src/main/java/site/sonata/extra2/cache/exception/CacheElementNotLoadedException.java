/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.cache.exception;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that element has not been loaded (possibly 'yet' or loading has
 * completely failed).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class CacheElementNotLoadedException extends CacheIllegalStateException
{

	/**
	 * 
	 */
	public CacheElementNotLoadedException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	protected CacheElementNotLoadedException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheElementNotLoadedException(String message, @Nullable Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CacheElementNotLoadedException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheElementNotLoadedException(Throwable cause)
	{
		super(cause);
	}
	
}
