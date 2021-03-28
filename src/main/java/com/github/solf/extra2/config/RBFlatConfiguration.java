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
package com.github.solf.extra2.config;

import static com.github.solf.extra2.util.NullUtil.nnc;
import static com.github.solf.extra2.util.NullUtil.nonNull;
import static com.github.solf.extra2.util.NullUtil.nullable;

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
	 * @see com.github.solf.extra2.config.FlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key)
		throws MissingResourceException, NullPointerException
	{
		return nonNull(bundle.getString(key));
	}

	/* (non-Javadoc)
	 * @see com.github.solf.extra2.config.FlatConfiguration#getAllKeys()
	 */
	@Override
	public Iterable<String> getAllKeys()
	{
		return nnc(bundle.keySet());
	}

}
