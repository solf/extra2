/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.options;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WBRBConfig;
import com.github.solf.extra2.config.FlatConfiguration;

import lombok.Getter;

/**
 * Provides a superclass to initialze reference options value ({@link #getRawOptions()}}
 * before fields are initialized, e.g. see {@link WBRBConfig}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class BaseDelegatingOptions
{
	/**
	 * Raw underlying options.
	 */
	@Getter
	private final BaseOptions rawOptions;
	
	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	protected BaseDelegatingOptions(BaseOptions initializeFrom)
		throws MissingResourceException,
		NumberFormatException
	{
		this.rawOptions = initializeFrom;
	}

	/**
	 * @param configuration
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	protected BaseDelegatingOptions(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		this.rawOptions = new BaseOptions(configuration);
	}
}
