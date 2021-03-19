/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate an invalid user input.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class InvalidUserInputException extends RuntimeException
{

	/**
	 * 
	 */
	public InvalidUserInputException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InvalidUserInputException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidUserInputException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public InvalidUserInputException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public InvalidUserInputException(Throwable cause)
	{
		super(cause);
	}

}
