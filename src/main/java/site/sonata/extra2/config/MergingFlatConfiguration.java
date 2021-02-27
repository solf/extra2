/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.config;

import static site.sonata.extra2.util.NullUtil.nullable;

import java.util.HashSet;
import java.util.MissingResourceException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Implementation of {@link FlatConfiguration} that 'merges' two other
 * configurations together.
 * 
 * Values are first searched in the first (overriding) configuration and if not
 * found then in second (base) configuration.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
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
	 * @see site.sonata.extra2.config.FlatConfiguration#getString(java.lang.String)
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
	 * @see site.sonata.extra2.config.FlatConfiguration#getAllKeys()
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
