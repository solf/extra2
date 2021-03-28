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
package com.github.solf.extra2.cache.exception;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thrown to indicate that cache is full and no further elements can be added
 * at this time (for operations that require adding elements).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class CacheFullException extends CacheIllegalStateException
{
	/**
	 * Constructor.
	 */
	public CacheFullException(String cacheName, long currentSize, long maxSize)
	{
		super("Cache [" + cacheName + "] cannot add element due to exceeded capacity: " + Long.toString(currentSize) + "/" + Long.toString(maxSize));
	}
}
