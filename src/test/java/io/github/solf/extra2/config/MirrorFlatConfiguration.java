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

import static io.github.solf.extra2.util.NullUtil.nonNull;

import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.config.RBFlatConfiguration;

/**
 * {@link PropertyResourceBundle} that returns property name as property value
 * for options that are not otherwise specified.
 * NOTE it can also return null if you use {@link #NULL_PREFIX}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class MirrorFlatConfiguration extends RBFlatConfiguration
{
	/**
	 * Prefix to start property name with if you want to have null result.
	 */
	public static final String NULL_PREFIX = "<null-string>";
	
	/**
	 * Constructor.
	 */
	public MirrorFlatConfiguration(String propertyFileName)
	{
		super(nonNull(ResourceBundle.getBundle(propertyFileName)));
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.RBFlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key)
		throws MissingResourceException, NullPointerException
	{
		if (key.startsWith(NULL_PREFIX) )
			throw new MissingResourceException("Missing option: " + key, null, key);
		
		try
		{
			return super.getString(key);
		} catch (MissingResourceException e)
		{
			return key;
		}
	}
	
	
}
