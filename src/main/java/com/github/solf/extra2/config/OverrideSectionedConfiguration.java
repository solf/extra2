/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.config;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Sectioned configuration implementation that allows to override particular
 * values in particular sections.
 * Note that it doesn't allow adding new sections.
 * 
 * To override something -- get relevant section and set override there (including
 * global).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class OverrideSectionedConfiguration implements SectionedConfiguration
{
	/**
	 * String to be put in that represents null / non-specified value.
	 */
	public static final String NULL_STRING = OverrideFlatConfiguration.NULL_STRING;
	
	/**
	 * Key for global section.
	 */
	private static final String GLOBAL_SECTION_KEY = "zxcm,9123ui478mv!@#^*%!#mvm, - global section   ";
	
	/**
	 * Original configuration.
	 */
	private final SectionedConfiguration originalConfig;
	
	/**
	 * Internal map of already created flat configurations. 
	 */
	private final Map<String, OverrideFlatConfiguration> flatCfgMap = new ConcurrentHashMap<String, OverrideFlatConfiguration>();
	
	/**
	 * Constructor.
	 */
	public OverrideSectionedConfiguration(SectionedConfiguration originalConfig)
	{
		this.originalConfig = originalConfig;
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.SectionedConfiguration#getGlobalSection()
	 */
	@Override
	public OverrideFlatConfiguration getGlobalSection()
	{
		return getSection(GLOBAL_SECTION_KEY);
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.SectionedConfiguration#getSection(java.lang.String)
	 */
	@Override
	public synchronized OverrideFlatConfiguration getSection(String sectionKey)
		throws MissingResourceException, NullPointerException
	{
		OverrideFlatConfiguration cfg = flatCfgMap.get(sectionKey);
		if (cfg == null)
		{
			if (GLOBAL_SECTION_KEY.equals(sectionKey))
				cfg = new OverrideFlatConfiguration(originalConfig.getGlobalSection());
			else
				cfg = new OverrideFlatConfiguration(originalConfig.getSection(sectionKey));
			
			flatCfgMap.put(sectionKey, cfg);
		}
		
		return cfg;
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.SectionedConfiguration#getAllSectionKeys()
	 */
	@Override
	public Iterable<String> getAllSectionKeys()
	{
		return originalConfig.getAllSectionKeys();
	}
	
}
