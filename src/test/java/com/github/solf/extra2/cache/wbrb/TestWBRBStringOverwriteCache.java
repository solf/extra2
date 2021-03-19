/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb;

import java.util.MissingResourceException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.config.FlatConfiguration;

/**
 * Simplest cache storage impl that overwrites storage value.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
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
