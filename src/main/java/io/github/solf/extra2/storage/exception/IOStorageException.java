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
package io.github.solf.extra2.storage.exception;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Thrown to indicate general IO problem.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
