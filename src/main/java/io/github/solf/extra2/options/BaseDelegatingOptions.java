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

import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.wbrb.WBRBConfig;
import io.github.solf.extra2.config.FlatConfiguration;
import lombok.Getter;

/**
 * Provides a superclass to initialize reference options value ({@link #getRawOptions()}}
 * before fields are initialized, e.g. see {@link WBRBConfig}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
