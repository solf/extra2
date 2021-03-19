/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.exception;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is wrapper class to rethrow {@link IOException} as unchecked.
 * <p>
 * There's always should be a 'cause' and it should be of {@link IOException}
 * type.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class IORuntimeException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	public IORuntimeException(IOException cause)
	{
		super("Rethrown unchecked: " + cause, cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public IORuntimeException(String message, IOException cause)
	{
		super(message, cause);
	}
}
