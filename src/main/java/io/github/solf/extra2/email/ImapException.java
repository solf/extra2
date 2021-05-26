/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.email;

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
