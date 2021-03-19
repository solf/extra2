/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.exception;

import java.io.EOFException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate end-of-file or stream.
 * 
 * Unlike {@link EOFException} this one is {@link RuntimeException}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class EOFRuntimeException extends RuntimeException
{

	/**
	 * 
	 */
	public EOFRuntimeException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EOFRuntimeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public EOFRuntimeException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public EOFRuntimeException(Throwable cause)
	{
		super(cause);
	}
	
}
