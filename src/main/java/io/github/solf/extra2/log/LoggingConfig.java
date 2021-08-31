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
package io.github.solf.extra2.log;

import java.util.MissingResourceException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.options.BaseDelegatingOptions;
import io.github.solf.extra2.options.BaseOptions;
import lombok.Getter;

/**
 * Configuration for {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class LoggingConfig extends BaseDelegatingOptions
{
	/** Common naming prefix to be used for all messages, can be empty */
	@Getter
	private final String commonNamingPrefix = getRawOptions().getStringPossiblyEmpty("commonNamingPrefix");
	
	
	/** Default: 10s; 'time window' over which log throttling calculates maximum number of allowed messages (per message type) */
	@Getter
	private final long logThrottleTimeInterval = getRawOptions().getTimeIntervalPositive("logThrottleTimeInterval", "10s");

	/** Default: 10; zero value disabled throttling; how many messages of a single type can be logged per log throttling 'time window' */
	@Getter
	private final int logThrottleMaxMessagesOfTypePerTimeInterval = getRawOptions().getIntNonNegative("logThrottleMaxMessagesOfTypePerTimeInterval", 10);

	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public LoggingConfig(BaseOptions initializeFrom)
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
	public LoggingConfig(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}
}
