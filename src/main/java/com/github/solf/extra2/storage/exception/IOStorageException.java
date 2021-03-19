/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.storage.exception;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate general IO problem.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class IOStorageException extends BaseStorageException
{
	/**
	 * Constructor.
	 */
	public IOStorageException(String message)
	{
		super(message);
	}
	
	/**
	 * Constructor.
	 */
	public IOStorageException(IOException e)
	{
		super("Unexpected IO problem: " + e, e);
	}
}
