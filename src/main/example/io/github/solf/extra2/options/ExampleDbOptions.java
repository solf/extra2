/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.options;

import java.util.MissingResourceException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.config.FlatConfiguration;
import lombok.Getter;

/**
 * Example for how to use {@link BaseOptions}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
