/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.concurrent.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is wrapper class to rethrow {@link InterruptedException} as unchecked.
 * <p>
 * There should always be a 'cause' and it should be of {@link InterruptedException}
 * type.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAInterruptedException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	public WAInterruptedException(InterruptedException cause)
	{
		super("Rethrown unchecked: " + cause, cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public WAInterruptedException(String message, InterruptedException cause)
	{
		super(message, cause);
	}
}