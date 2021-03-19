/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.config;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents a flat configuration -- that is a configuration that simply contains
 * a flat set of options.
 * 
 * NOTE: instances are created via {@link Configuration} class.
 * 
 * NOTE: all implementations must be thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public interface FlatConfiguration
{
	/**
	 * Gets a string options value for the given key.
	 *
	 * @param key the key for the desired string
	 * @exception NullPointerException if <code>key</code> is <code>null</code>
	 * @exception MissingResourceException if no object for the given key can be found
	 */
	public String getString(String key) throws MissingResourceException, NullPointerException;
	
	/**
	 * Iterable for all keys specified in this configuration.
	 */
	public Iterable<String> getAllKeys();
}
