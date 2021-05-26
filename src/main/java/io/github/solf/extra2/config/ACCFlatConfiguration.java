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
package io.github.solf.extra2.config;

import static io.github.solf.extra2.util.NullUtil.nnc;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.Iterator;
import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * Apache commons-configuration based implementation.
 * 
 * Since Apache stuff used here is not thread-safe -- synchronize inside of
 * each method.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class ACCFlatConfiguration implements FlatConfiguration
{
	/**
	 * Data node to read data from.
	 */
	private final SubnodeConfiguration dataNode;
	
	/**
	 * Parent object which is used for synchronization (since Apache stuff used
	 * here is not thread-safe).
	 */
	private final HierarchicalConfiguration parentToSyncTo;
	
	/**
	 * Constructor.
	 */
	public ACCFlatConfiguration(SubnodeConfiguration dataNode, HierarchicalConfiguration parentToSyncTo)
	{
		if (nullable(dataNode) == null)
			throw new NullPointerException("Configuration node must be specified.");
		if (nullable(parentToSyncTo) == null)
			throw new NullPointerException("Parent to sync to must be specified.");
		
		this.dataNode = dataNode;
		this.parentToSyncTo = parentToSyncTo;
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.FlatConfiguration#getString(java.lang.String)
	 */
	@Override
	public String getString(String key)
		throws MissingResourceException, NullPointerException
	{
		if (nullable(key) == null)
			throw new NullPointerException("Key is null.");
		
		synchronized(parentToSyncTo)
		{
			String result = dataNode.getString(key);
			if (result == null)
				throw new MissingResourceException("Missing configuration option: " + key, null, key);
			
			return result;
		}
	}

	/* (non-Javadoc)
	 * @see io.github.solf.extra2.config.FlatConfiguration#getAllKeys()
	 */
	@Override
	public Iterable<String> getAllKeys()
	{
		synchronized(parentToSyncTo)
		{	
			final Iterator<String> result = nnc(dataNode.getKeys());
			return new Iterable<String>()
			{
				@Override
				public Iterator<String> iterator()
				{
					return result;
				}
			};
		}
	}
}
