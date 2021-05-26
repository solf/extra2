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

import java.util.List;
import java.util.MissingResourceException;

import javax.annotation.NonNullByDefault;

import io.github.solf.extra2.config.FlatConfiguration;
import io.github.solf.extra2.options.BaseOptions;
import io.github.solf.extra2.options.OptionConstraint;

/**
 * Non-generated extensions to {@link GeneratedVolatileWBRBConfig} that make
 * it a bit more usable (e.g. via JMX).
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class VolatileWBRBConfig extends GeneratedVolatileWBRBConfig
{
	/**
	 * @param initializeFrom
	 * @throws MissingResourceException
	 * @throws NumberFormatException
	 */
	public VolatileWBRBConfig(BaseOptions initializeFrom)
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
	public VolatileWBRBConfig(FlatConfiguration configuration)
		throws MissingResourceException,
		NumberFormatException
	{
		super(configuration);
	}

	/** Default: 1,2,3,4,9 ; a list of exactly 5 int thresholds to be used as thresholds (equal or less) for monitoring 'for how many full cycles items is in the cache' at the end of return queue; values MUST be in ascending order */
	public void setMonitoringFullCacheCyclesThresholdsFromString(String str)
	{
		List<Integer> list = getRawOptions().getIntList("option-doesn't-exist", str, OptionConstraint.POSITIVE, OptionConstraint.NON_EMPTY_COLLECTION);
		validateThresholdsListIsAscending(list, "monitoringFullCacheCyclesThresholds");
		
		setMonitoringFullCacheCyclesThresholds(list);
	}

	/** Default: 5s,10s,15s,20s,25s ; a list of exactly 5 time intervals to be used as thresholds (equal or less) for monitoring 'time since last access' at the end of return queue; values MUST be in ascending order */
	public void setMonitoringTimeSinceAccessThresholdsFromString(String str)
	{
		List<Long> list = getRawOptions().getTimeIntervalList("option-doesn't-exist", str, OptionConstraint.NON_NEGATIVE, OptionConstraint.NON_EMPTY_COLLECTION);
		validateThresholdsListIsAscending(list, "monitoringTimeSinceAccessThresholds");
		
		setMonitoringTimeSinceAccessThresholds(list);
	}

}
