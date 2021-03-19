/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.config;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Flat configuration implementation that allows overriding particular items.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class OverrideFlatConfiguration implements FlatConfiguration
{
	/**
	 * String to be put in that represents null / non-specified value.
	 */
	public static final String NULL_STRING = "<null-string>";
	
	/**
	 * Original configuration.
	 */
	private final FlatConfiguration originalConfig;
	
	/**
	 * Override map.
	 */
	private final Map<String, String> overrideMap = new ConcurrentHashMap<String, String>();
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(FlatConfiguration originalConfig)
	{
		this.originalConfig = originalConfig;
	}
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(String propertiesFileName)
	{
		this(Configuration.fromPropertiesFile(propertiesFileName));
	}
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(File propertiesFile)
	{
		this(Configuration.fromPropertiesFile(propertiesFile));
	}
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(FlatConfiguration originalConfig, String... overridePairs)
		throws IllegalArgumentException
	{
		this(originalConfig);
		
		overridePairs(overridePairs);
	}
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(String propertiesFileName, String... overridePairs)
		throws IllegalArgumentException
	{
		this(propertiesFileName);
		
		overridePairs(overridePairs);
	}
	
	/**
	 * Constructor.
	 */
	public OverrideFlatConfiguration(File propertiesFile, String... overridePairs)
		throws IllegalArgumentException
	{
		this(propertiesFile);
		
		overridePairs(overridePairs);
	}
	
	
	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.FlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key)
		throws MissingResourceException, NullPointerException
	{
		String value = overrideMap.get(key);
		if (value == NULL_STRING)
			throw new MissingResourceException("Missing option: " + key, null, key);
		if (value != null)
			return value;
		
		return originalConfig.getString(key);
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.FlatConfiguration#getAllKeys()
	 */
	@Override
	public Iterable<String> getAllKeys()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Set particular value override.
	 */
	public OverrideFlatConfiguration override(String prop, String value)
	{
		overrideMap.put(prop, value);
		
		return this;
	}

	/**
	 * Set particular values overrides in a batch (argument must contain pairs
	 * of strings 'option', 'value', ...).
	 */
	public OverrideFlatConfiguration overridePairs(String... overridePairs)
	{
		if ((overridePairs.length % 2) != 0)
			throw new IllegalArgumentException("Overrides must contain even number of elements (pairs): " + Arrays.toString(overridePairs));
		
		for (int i = 0; i < overridePairs.length; i+=2)
			override(overridePairs[i], overridePairs[i + 1]);
		
		return this;
	}

}
