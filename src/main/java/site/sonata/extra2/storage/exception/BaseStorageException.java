/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.storage.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base class for storage exceptions.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class BaseStorageException extends RuntimeException
{

	/**
	 * Constructor.
	 */
	public BaseStorageException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public BaseStorageException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public BaseStorageException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public BaseStorageException(String message)
	{
		super(message);
}

	/**
	 * @param cause
	 */
	public BaseStorageException(Throwable cause)
	{
		super(cause);
	}

}
