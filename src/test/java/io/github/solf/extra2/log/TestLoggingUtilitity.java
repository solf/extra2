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

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.log.example.ExampleLogMessage;

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
		TestLog logger = new TestLog(
			new LoggingConfig(Configuration.fromPropertiesFile("logging/logutil.properties")));
	
		// Test basic logging and thresholds
		logger.logMessage(ExampleLogMessage.TEST_TRACE, null);
		assertLoggerContainsAndClear(logger, 0, "msgSubstring", 0); // TRACE is not logged
		logger.logMessage(ExampleLogMessage.TEST_DEBUG, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_DEBUG", 1);
		logger.logMessage(ExampleLogMessage.TEST_INFO, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_INFO", 1);
		logger.logMessage(ExampleLogMessage.TEST_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_WARN", 1);
		logger.logMessage(ExampleLogMessage.TEST_EXTERNAL_INFO, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_INFO", 1);
		logger.logMessage(ExampleLogMessage.TEST_EXTERNAL_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_WARN", 1);
		logger.logMessage(ExampleLogMessage.TEST_EXTERNAL_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_ERROR", 1);
		logger.logMessage(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.logMessage(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.logMessage(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);
	
		// Test logging arguments
		logger.logMessage(ExampleLogMessage.TEST_INFO, null, "testarg1", new Exception("testexc2"), "testarg2");
		assertLoggerContainsAndClear(logger, 1, "TEST_INFO [testarg1, java.lang.Exception: testexc2, testarg2]", 1);
		
		// Test exception stack trace logging
		logger.logMessage(ExampleLogMessage.TEST_ERROR, new Exception("test failure"), "errarg1", "errarg2");
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR [errarg1, errarg2] EXCLOG: java.lang.Exception: test failure", 1);
		
		{
			// Test naming prefix
			OverrideFlatConfiguration config = new OverrideFlatConfiguration("logging/logutil.properties");
			config.override("commonNamingPrefix", "namingPrefix");
			TestLog logger2 = new TestLog(new LoggingConfig(config));
			
			logger2.logMessage(ExampleLogMessage.TEST_INFO, null, "arg1");
			assertLoggerContainsAndClear(logger2, 1, "namingPrefix TEST_INFO [arg1]", 1);
		}
		
		// aaa test monitoring
	}
	
	
	/**
	 * Asserts that the logger contains given number of matching messages and
	 * (optionally) total messages count.
	 * <p>
	 * Clears collected messages afterwards.
	 */
	private void assertLoggerContainsAndClear(TestLog logger, @Nullable Integer totalNumMessages, String msgSubstring, int matchingMessageCount)
	{
		assertLoggerContains(logger, totalNumMessages, msgSubstring, matchingMessageCount);
		
		logger.getLoggedMessages().clear();
	}
	
	/**
	 * Asserts that the logger contains given number of matching messages and
	 * (optionally) total messages count.
	 */
	private void assertLoggerContains(TestLog logger, @Nullable Integer totalNumMessages, String msgSubstring, int matchingMessageCount)
	{
		ArrayList<String> msgs = logger.getLoggedMessages();
		if (totalNumMessages != null)
			assertEquals(msgs.size(), (int)totalNumMessages);
		
		int matches = 0;
		for (String msg : msgs)
		{
			if (msg.contains(msgSubstring))
				matches++;
		}
		
		assertEquals(matches, matchingMessageCount);
	}
}
