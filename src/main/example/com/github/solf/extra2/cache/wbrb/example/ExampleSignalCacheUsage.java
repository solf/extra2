/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb.example;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WBRBConfig;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import com.github.solf.extra2.config.OverrideFlatConfiguration;

/**
 * Example of {@link WriteBehindResyncInBackgroundCache} usage via {@link ExampleSignalCache}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
/*package*/ class ExampleSignalCacheUsage
{

	/**
	 * Main method
	 */
	public static void main(String[] args)
	{
		// Use test config for some valid configuration
		OverrideFlatConfiguration config = new OverrideFlatConfiguration("wbrb/wbrb-default.properties");
		config.override("cacheName", "ExampleSignalCache"); // test config doesn't specify this, so have to set it
		
		ExampleSignalCache hiCache = new ExampleSignalCache(
			new WBRBConfig(config), 
			true /*hiCache*/, 
			new ConcurrentHashMap<Long, ExSignalStorage>() /*underlyingStorage*/
		);
		hiCache.start();
		
		Long key = 123L;
		
		hiCache.preloadCache(key);
		
		/*some processing*/
		
		ExSignalSample signalSample = hiCache.readForOrException(key, 1000L);
		
		System.out.println(signalSample);
		/*some processing*/
		
		hiCache.writeIfCachedOrException(key, 456);
		
		/*some processing*/
		System.out.println(hiCache.readForOrException(key, 1000L));
		
		if (!hiCache.shutdownFor(1000))
			throw new IllegalStateException("Failed to shutdown");
	}

}
