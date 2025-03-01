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
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Factory class for creating configuration (options) objects.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class Configuration
{
	/**
	 * Creates configuration from resource bundle.
	 */
	public static FlatConfiguration fromResourceBundle(ResourceBundle bundle)
		throws NullPointerException
	{
		return new RBFlatConfiguration(bundle);
	}
	
	/**
	 * Creates configuration from properties file which must be located on the
	 * classpath.
	 * <p>
	 * If you want to load from non-classpath location, consider using
	 * {@link #fromPropertiesFile(File)}
	 * <p>
	 * This method generally follows {@link ResourceBundle#getBundle(String)}.
	 * <p>
	 * However unlike {@link ResourceBundle#getBundle(String)} this method will
	 * accept full file name (including .properties suffix).
	 */
	public static FlatConfiguration fromPropertiesFile(String propertiesFileName)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(propertiesFileName) == null)
			throw new NullPointerException("Resource file name must be specified.");
		
		String baseName = propertiesFileName;
		final String trimSuffix = ".properties";
		final int trimSuffixLength = trimSuffix.length();
		final int fileNameLength = propertiesFileName.length();
		if (fileNameLength > trimSuffixLength)
		{
			if (propertiesFileName.endsWith(trimSuffix))
			{
				baseName = propertiesFileName.substring(0, fileNameLength - trimSuffixLength);
			}
		}
		
		return fromResourceBundle(nonNull(ResourceBundle.getBundle(baseName)));
	}
	
	/**
	 * Creates configuration from given properties file.
	 */
	public static FlatConfiguration fromPropertiesFile(File file)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(file) == null)
			throw new NullPointerException("File must be specified.");
		
		try
		{
			try (FileInputStream fis = new FileInputStream(file))
			{
				return fromResourceBundle(new PropertyResourceBundle(fis));
			}
		} catch (IOException e)
		{
			throw new MissingResourceException("Unable to load configuration from file [" + file + "]: " + e, null, file.toString());
		}
	}
	
	/**
	 * Creates configuration from specified ini file which must be located on the classpath.
	 * NOTE: requires full file name (e.g. including .ini suffix).
	 */
	public static SectionedConfiguration fromIniFile(String iniFileName)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(iniFileName) == null)
			throw new NullPointerException("File name must be specified.");
		
		try
		{
			HierarchicalINIConfiguration aCfg = new HierarchicalINIConfiguration();
			aCfg.setDelimiterParsingDisabled(true);
			aCfg.load(iniFileName);
			return new ACCSectionedConfiguration(aCfg);
		} catch (ConfigurationException e)
		{
			throw new MissingResourceException("Unable to load configuration from file [" + iniFileName + "]: " + e, null, iniFileName.toString());
		}
		
	}
	
	/**
	 * Creates configuration from specified ini file
	 */
	public static SectionedConfiguration fromIniFile(File iniFile)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(iniFile) == null)
			throw new NullPointerException("File must be specified.");
		
		try
		{
			HierarchicalINIConfiguration aCfg = new HierarchicalINIConfiguration();
			aCfg.setDelimiterParsingDisabled(true);
			aCfg.load(iniFile);
			return new ACCSectionedConfiguration(aCfg);
		} catch (ConfigurationException e)
		{
			throw new MissingResourceException("Unable to load configuration from file [" + iniFile + "]: " + e, null, iniFile.toString());
		}
	}
	
	/**
	 * Merges two configurations.
	 * 
	 * Values are first searched in the first (overriding) configuration and if not
	 * found then in second (base) configuration.
	 * 
	 */
	public static FlatConfiguration merge(FlatConfiguration overridingConfiguration, 
		FlatConfiguration baseConfiguration)
	{
		return new MergingFlatConfiguration(overridingConfiguration, baseConfiguration);
	}
	
	/**
	 * Merges two configurations.
	 * <p>
	 * Values are first searched in the first (overriding) configuration and if not
	 * found then in second (base) configuration.
	 * <p>
	 * Note that performance of this isn't optimal, but for the most cases it
	 * shouldn't matter.
	 * 
	 * @see MergingSectionedConfiguration
	 */
	public static SectionedConfiguration merge(SectionedConfiguration overridingConfiguration, 
		SectionedConfiguration baseConfiguration)
	{
		return new MergingSectionedConfiguration(overridingConfiguration, baseConfiguration);
	}
}
