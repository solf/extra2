/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.config;

import static com.github.solf.extra2.util.NullUtil.nonNull;

import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.config.RBFlatConfiguration;

/**
 * {@link PropertyResourceBundle} that returns property name as property value
 * for options that are not otherwise specified.
 * NOTE it can also return null if you use {@link #NULL_PREFIX}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
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
	 * @see site.sonata.extra2.config.RBFlatConfiguration#getString(java.lang.String)
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
