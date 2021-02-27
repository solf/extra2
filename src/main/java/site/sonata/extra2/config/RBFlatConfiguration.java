/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.config;

import static site.sonata.extra2.util.NullUtil.nnc;
import static site.sonata.extra2.util.NullUtil.nonNull;
import static site.sonata.extra2.util.NullUtil.nullable;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * ResourceBundle-based implementation.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class RBFlatConfiguration implements FlatConfiguration
{
	/**
	 * Resource bundle used for data.
	 */
	private final ResourceBundle bundle;
	
	/**
	 * Constructor.
	 */
	public RBFlatConfiguration(ResourceBundle bundle)
	{
		if (nullable(bundle) == null)
			throw new NullPointerException("Bundle must be specified.");
		
		this.bundle = bundle;
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.config.FlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key)
		throws MissingResourceException, NullPointerException
	{
		return nonNull(bundle.getString(key));
	}

	/* (non-Javadoc)
	 * @see site.sonata.extra2.config.FlatConfiguration#getAllKeys()
	 */
	@Override
	public Iterable<String> getAllKeys()
	{
		return nnc(bundle.keySet());
	}

}
