/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent.exception;

import java.util.concurrent.ExecutionException;

/**
 * This is a wrapper to rethrow {@link ExecutionException} as unchecked.
 *
 * @author Sergey Olefir
 */
public class WAExecutionException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	public WAExecutionException(ExecutionException cause)
	{
		super("Rethrown unchecked: " + cause, cause);
	}
}
