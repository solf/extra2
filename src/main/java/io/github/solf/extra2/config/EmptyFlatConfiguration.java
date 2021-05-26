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
package io.github.solf.extra2.config;

import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.Collections;
import java.util.MissingResourceException;

import javax.annotation.NonNullByDefault;

/**
 * 'Empty' configuration -- doesn't contain any properties.
 * <p>
 * Might be useful for work-in-progress or testing.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class EmptyFlatConfiguration implements FlatConfiguration
{

	@Override
	public String getString(String key)
		throws MissingResourceException,
		NullPointerException
	{
		if (nullable(key) == null)
			throw new NullPointerException("Key may not be null.");
		
		throw new MissingResourceException("EmptyFlatConfiguration doesn't have any properties", "EmptyFlatConfiguration", key);		
	}

	@Override
	public Iterable<String> getAllKeys()
	{
		return Collections.EMPTY_LIST;
	}

}
