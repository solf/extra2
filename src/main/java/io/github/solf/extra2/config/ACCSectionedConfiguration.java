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

import static io.github.solf.extra2.util.NullUtil.nnc;
import static io.github.solf.extra2.util.NullUtil.nonNull;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.MissingResourceException;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Apache commons-configuration based implementation.
 * 
 * Since Apache stuff used here is not thread-safe -- synchronize inside of
 * each method.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
/*package*/ class ACCSectionedConfiguration implements SectionedConfiguration
{
	/**
	 * Ini configuration.
	 */
	private final HierarchicalINIConfiguration iniConfiguration;
	
	/**
	 * Constructor.
	 */
	public ACCSectionedConfiguration(HierarchicalINIConfiguration iniConfiguration)
		throws NullPointerException
	{
		if (nullable(iniConfiguration) == null)
			throw new NullPointerException("INI configuration must be specified.");
		
		this.iniConfiguration = iniConfiguration;
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.SectionedConfiguration#getGlobalSection()
	 */
	@Override
	public FlatConfiguration getGlobalSection()
	{
		synchronized(iniConfiguration)
		{
			return new ACCFlatConfiguration(nonNull(iniConfiguration.getSection(null)), iniConfiguration);
		}
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.SectionedConfiguration#getSection(java.lang.String)
	 */
	@Override
	public FlatConfiguration getSection(String sectionKey)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(sectionKey) == null)
			throw new NullPointerException("Section key must be specified.");
		
		synchronized(iniConfiguration)
		{
			SubnodeConfiguration subNode;
			try
			{
				subNode = nonNull(iniConfiguration.configurationAt(sectionKey));
			} catch( IllegalArgumentException iex )
			{
				throw new MissingResourceException("Missing configuration section: " + sectionKey, null, sectionKey);
			}

			return new ACCFlatConfiguration(subNode, iniConfiguration);
		}
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.SectionedConfiguration#getAllSectionKeys()
	 */
	@Override
	public Iterable<String> getAllSectionKeys()
	{
		synchronized(iniConfiguration)
		{
			Set<String> result = nnc(iniConfiguration.getSections());
			result.remove(null); // if you remove this line, make sure to adjust collection cast above
			
			return result;
		}
	}

}
