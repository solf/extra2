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
package com.github.solf.extra2.config;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents a flat configuration -- that is a configuration that simply contains
 * a flat set of options.
 * 
 * NOTE: instances are created via {@link Configuration} class.
 * 
 * NOTE: all implementations must be thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface FlatConfiguration
{
	/**
	 * Gets a string options value for the given key.
	 *
	 * @param key the key for the desired string
	 * @exception NullPointerException if <code>key</code> is <code>null</code>
	 * @exception MissingResourceException if no object for the given key can be found
	 */
	public String getString(String key) throws MissingResourceException, NullPointerException;
	
	/**
	 * Iterable for all keys specified in this configuration.
	 */
	public Iterable<String> getAllKeys();
}
