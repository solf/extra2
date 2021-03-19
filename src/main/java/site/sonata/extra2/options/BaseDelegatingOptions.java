/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.options;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import site.sonata.extra2.cache.wbrb.WBRBConfig;
import site.sonata.extra2.config.FlatConfiguration;

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
