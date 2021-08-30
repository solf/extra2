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
package io.github.solf.extra2.cache.wbrb.example;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.cache.wbrb.WBRBConfig;
import io.github.solf.extra2.cache.wbrb.WriteBehindResyncInBackgroundCache;
import io.github.solf.extra2.config.OverrideFlatConfiguration;

/**
 * Example of {@link WriteBehindResyncInBackgroundCache} usage via {@link ExampleSignalCache}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
