/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.email;

/**
 * Exceptions for stuff that happens when accessing IMAP.
 *
 * @author Sergey Olefir
 */
public class ImapException extends RuntimeException
{
	/**
	 * Constructor.
	 */
	private ImapException(Throwable parent)
	{
		super("IMAP exception: " + parent, parent);
	}
	
	/**
	 * Constructor.
	 */
	public ImapException(String message)
	{
		super(message);
	}
	
	/**
	 * Wraps exception into {@link ImapException} unless it's already {@link ImapException}.
	 */
	public static ImapException wrap(Throwable e)
	{
		if (e instanceof ImapException)
			return (ImapException)e;
		
		return new ImapException(e);
	}
}
