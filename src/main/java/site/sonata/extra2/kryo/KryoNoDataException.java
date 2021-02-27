/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.kryo;

/**
 * Thrown to indicate that there's no stored Kryo data.
 *
 * @author Sergey Olefir
 */
public class KryoNoDataException extends RuntimeException
{
	/**
	 * 
	 */
	public KryoNoDataException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public KryoNoDataException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public KryoNoDataException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public KryoNoDataException(Throwable cause)
	{
		super(cause);
	}

}
