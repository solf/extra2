/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that an assertion has failed.
 * <p>
 * Unlike {@link AssertionError} this is an instance of {@link RuntimeException}
 * -- and thus instance of {@link Exception} -- whereas {@link AssertionError} is
 * an {@link Error} and thus is not caught by {@link Exception} catch block.
 * <p>
 * So this exception is much safer to use vs. various error handlers.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class AssertionException extends RuntimeException
{

	/**
	 * 
	 */
	public AssertionException()
	{
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AssertionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public AssertionException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public AssertionException(Throwable cause)
	{
		super(cause);
	}

}
