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

import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Implementation of {@link SectionedConfiguration} that 'merges' two other
 * configurations together.
 * <p>
 * Values are first searched in the first (overriding) configuration and if not
 * found then in second (base) configuration.
 * <p>
 * NOTE: performance of this is not necessarily optimal -- particularly in
 * {@link #getAllSectionKeys()} -- but in many cases this should not matter.
 * <p>
 * NOTE2: overriding configuration is allowed to skip sections that it does not
 * wish to override.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class MergingSectionedConfiguration implements SectionedConfiguration
{
	/**
	 * Overriding configuration.
	 */
	protected final SectionedConfiguration overridingConfiguration;
	
	/**
	 * Base configuration.
	 */
	protected final SectionedConfiguration baseConfiguration;
	
	/**
	 * Constructor.
	 * 
	 * @param overridingConfiguration values in this configuration take precedence
	 * 		over base configuration; this configuration may skip sections that
	 * 		it does not wish to override
	 * @param baseConfiguration values in this configuration are used only if
	 * 		they are not present in overriding configuration
	 */
	public MergingSectionedConfiguration(SectionedConfiguration overridingConfiguration, 
		SectionedConfiguration baseConfiguration)
	{
		this.overridingConfiguration = overridingConfiguration;
		this.baseConfiguration = baseConfiguration;
	}
	
	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.SectionedConfiguration#getGlobalSection()
	 */
	@Override
	public FlatConfiguration getGlobalSection()
	{
		return new MergingFlatConfiguration(overridingConfiguration.getGlobalSection(), baseConfiguration.getGlobalSection());
	}



	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.SectionedConfiguration#getSection(java.lang.String)
	 */
	@Override
	public FlatConfiguration getSection(String sectionKey)
		throws MissingResourceException,
		NullPointerException
	{
		FlatConfiguration override;
		try
		{
			override = overridingConfiguration.getSection(sectionKey);
		} catch (MissingResourceException e)
		{
			return baseConfiguration.getSection(sectionKey);
		}
		
		FlatConfiguration base;
		try
		{
			base = baseConfiguration.getSection(sectionKey);
		} catch (MissingResourceException e)
		{
			return override;
		}
		
		return new MergingFlatConfiguration(override, base);
	}


	/**
	 * Iterable for all section keys in this configuration (excluding global section).
	 * <p>
	 * NOTE: this particular implementation is very inefficient, it reads all
	 * sections in both configurations and merges them into a single set -- and
	 * it does so on each call. However in many cases this should not matter. 
	 */
	@Override
	public Iterable<String> getAllSectionKeys()
	{
		Set<String> result = new HashSet<>();
		
		for (String entry : baseConfiguration.getAllSectionKeys())
			result.add(entry);
		
		for (String entry : overridingConfiguration.getAllSectionKeys())
			result.add(entry);
		
		return result;
	}
}
