/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.options;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.config.Configuration;
import com.github.solf.extra2.config.FlatConfiguration;

import lombok.Getter;

/**
 * Example for how to use {@link BaseOptions}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExampleDbOptions extends BaseOptions
{
	/** Host for databases, defaults to 'localhost' */
	@Getter private final String host = getString("host", "localhost");
	
	/** Login for database, defaults to 'root' */
	@Getter private final String login = getString("login", "root");
	
	/** Password for database, defaults to 'mysql' */
	@Getter private final String password = getString("password", "mysql");

	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public ExampleDbOptions(BaseOptions initializeFrom)
		throws MissingResourceException,
		NumberFormatException
	{
		super(initializeFrom);
	}

	/**
	 * @param configuration
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public ExampleDbOptions(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}

	
	/**
	 * Example for how to load options from property file.
	 */
	public static ExampleDbOptions exampleLoadFromProperties()
	{
		return new ExampleDbOptions(Configuration.fromPropertiesFile("config.properties"));
	}
	
	/**
	 * Example for how to load options from a section in the ini file.
	 */
	public static ExampleDbOptions exampleLoadFromIni()
	{
		return new ExampleDbOptions(
			Configuration.fromIniFile("config.ini").getSection("db"));
	}
}
