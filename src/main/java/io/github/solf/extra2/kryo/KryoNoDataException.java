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
package io.github.solf.extra2.kryo;

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
