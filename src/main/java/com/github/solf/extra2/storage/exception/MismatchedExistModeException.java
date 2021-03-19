/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.storage.exception;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.storage.ExistMode;

/**
 * Thrown to indicate that requested {@link ExistMode} doesn't match actual one.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class MismatchedExistModeException extends BaseStorageException
{
	/**
	 * Constructor.
	 */
	public MismatchedExistModeException(String msg)
	{
		super(msg);
	}
}
