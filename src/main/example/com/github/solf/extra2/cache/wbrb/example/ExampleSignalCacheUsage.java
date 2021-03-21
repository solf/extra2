/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.cache.wbrb.example;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.cache.wbrb.WBRBConfig;
import com.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import com.github.solf.extra2.config.Configuration;

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
		ExampleSignalCache hiCache = new ExampleSignalCache(
			new WBRBConfig(Configuration.fromPropertiesFile("example-cache.properties")), 
			true /*hiCache*/, 
			new ConcurrentHashMap<Long, ExSignalStorage>() /*underlyingStorage*/
		);
		
		Long key = 123L;
		
		hiCache.preloadCache(key);
		
		/*some processing*/
		
		ExSignalSample signalSample = hiCache.readForOrException(key, 1000L);
		
		System.out.println(signalSample);
		/*some processing*/
		
		hiCache.writeIfCachedOrException(key, 456);
	}

}
