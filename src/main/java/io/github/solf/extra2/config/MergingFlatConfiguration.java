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

import java.util.HashSet;
import java.util.MissingResourceException;

import javax.annotation.Nonnull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implementation of {@link FlatConfiguration} that 'merges' two other
 * configurations together.
 * 
 * Values are first searched in the first (overriding) configuration and if not
 * found then in second (base) configuration.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class MergingFlatConfiguration implements FlatConfiguration
{
	/**
	 * Overriding configuration.
	 */
	protected final FlatConfiguration overridingConfiguration;
	
	/**
	 * Base configuration.
	 */
	protected final FlatConfiguration baseConfiguration;
	
	/**
	 * Constructor.
	 * 
	 * @param overridingConfiguration values in this configuration take precedence
	 * 		over base configuration
	 * @param baseConfiguration values in this configuration are used only if
	 * 		they are not present in overriding configuration
	 */
	public MergingFlatConfiguration(FlatConfiguration overridingConfiguration, 
		FlatConfiguration baseConfiguration)
	{
		this.overridingConfiguration = overridingConfiguration;
		this.baseConfiguration = baseConfiguration;
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.FlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public @Nonnull String getString(@Nonnull String key)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(key) == null)
			throw new NullPointerException("Key is null.");
		
		try
		{
			return overridingConfiguration.getString(key);
		} catch (MissingResourceException e)
		{
			// Ok, we'll try base configuration below
		}
		
		return baseConfiguration.getString(key);
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.FlatConfiguration#getAllKeys()
	 */
	@Override
	public @Nonnull Iterable<@Nonnull String> getAllKeys()
	{
		@Nonnull Iterable<@Nonnull String> it1 = overridingConfiguration.getAllKeys();
		@Nonnull Iterable<@Nonnull String> it2 = baseConfiguration.getAllKeys();
		
		HashSet<String> result = new HashSet<>();
		it1.forEach(e -> result.add(e));
		it2.forEach(e -> result.add(e));
		
		return result;
	}
	
	
}
