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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.log.BaseLoggingUtility;
import io.github.solf.extra2.log.LoggingConfig;
import io.github.solf.extra2.log.example.ExampleLogMessage;
import io.github.solf.extra2.log.example.ExampleLoggingUtility;

/**
 * Tests for {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestLoggingUtilitity
{
	@Test
	public void testLogging()
	{
		ExampleLoggingUtility logger = new ExampleLoggingUtility(
			new LoggingConfig(Configuration.fromPropertiesFile("logging/logutil.properties")));
		
		logger.logMessage(ExampleLogMessage.STARTED, null);
	}
}
