/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.storage.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that data (e.g. data file or metadata) is absent when
 * it is expected to be present.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class MissingDataStorageException extends BaseStorageException
{
	/**
	 * Constructor.
	 */
	public MissingDataStorageException(String message)
	{
		super(message);
	}
	
	/**
	 * Constructor.
	 */
	public MissingDataStorageException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
