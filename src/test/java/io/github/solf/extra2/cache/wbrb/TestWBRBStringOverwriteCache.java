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
package io.github.solf.extra2.cache.wbrb;

import java.util.MissingResourceException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.config.FlatConfiguration;

/**
 * Simplest cache storage impl that overwrites storage value.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestWBRBStringOverwriteCache extends TestAbstractWBRBStringCache
{
	/**
	 * Constructor.
	 */
	public TestWBRBStringOverwriteCache(String cacheName,
		FlatConfiguration fConfig, long readDelayMs, long writeDelayMs)
		throws IllegalArgumentException,
		IllegalStateException,
		MissingResourceException,
		NumberFormatException
	{
		super(cacheName, fConfig, readDelayMs, writeDelayMs);
	}

	@Override
	protected void spiSynchronized_applyWrite(String writeData,
		TestCacheStorageEntry entry)
	{
		entry.setValue(writeData);
	}
	
}
