/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.config;

import static site.sonata.extra2.util.NullUtil.nullable;

import java.util.Collections;
import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 'Empty' configuration -- doesn't contain any properties.
 * <p>
 * Might be useful for work-in-progress or testing.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class EmptyFlatConfiguration implements FlatConfiguration
{

	@Override
	public String getString(String key)
		throws MissingResourceException,
		NullPointerException
	{
		if (nullable(key) == null)
			throw new NullPointerException("Key may not be null.");
		
		throw new MissingResourceException("EmptyFlatConfiguration doesn't have any properties", "EmptyFlatConfiguration", key);		
	}

	@Override
	public Iterable<String> getAllKeys()
	{
		return Collections.EMPTY_LIST;
	}

}
