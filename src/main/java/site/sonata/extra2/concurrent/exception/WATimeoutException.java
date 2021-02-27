/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent.exception;

import java.util.concurrent.TimeoutException;

/**
 * This is a version of {@link TimeoutException} that is unchecked.
 *
 * @author Sergey Olefir
 */
public class WATimeoutException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	public WATimeoutException(TimeoutException cause)
	{
		super("Rethrown unchecked: " + cause, cause);
	}
	
	/**
	 * Constructor.
	 */
	public WATimeoutException(String message)
	{
		super(message);
	}
}
