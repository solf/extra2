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

import static io.github.solf.extra2.testutil.AssertExtra.assertBetweenInclusive;
import static io.github.solf.extra2.testutil.AssertExtra.assertContains;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.config.Configuration;
import io.github.solf.extra2.config.OverrideFlatConfiguration;
import io.github.solf.extra2.log.example.ExampleLogMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests for {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@Slf4j
public class TestLoggingUtilitity
{
	@Test
	public void testLogging() throws InterruptedException
	{
		long start = System.currentTimeMillis();
		
		TestLog logger = new TestLog(
			new LoggingConfig(Configuration.fromPropertiesFile("logging/logutil.properties")));
	
		// Test basic logging and thresholds
		logger.log(ExampleLogMessage.TEST_TRACE, null);
		assertLoggerContainsAndClear(logger, 0, "msgSubstring", 0); // TRACE is not logged
		logger.log(ExampleLogMessage.TEST_DEBUG, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_DEBUG", 1);
		logger.log(ExampleLogMessage.TEST_INFO, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_INFO", 1);
		logger.log(ExampleLogMessage.TEST_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_WARN", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_INFO, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_INFO", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_WARN", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);
	
		// Test logging arguments
		logger.log(ExampleLogMessage.TEST_INFO, null, "testarg1", new Exception("testexc2"), "testarg2");
		assertLoggerContainsAndClear(logger, 1, "TEST_INFO [testarg1, java.lang.Exception: testexc2, testarg2]", 1);
		
		// Test exception stack trace logging
		logger.log(ExampleLogMessage.TEST_ERROR, new Exception("test failure"), "errarg1", "errarg2");
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR [errarg1, errarg2] EXCLOG: java.lang.Exception: test failure", 1);
		
		{
			// Test naming prefix
			OverrideFlatConfiguration config = new OverrideFlatConfiguration("logging/logutil.properties");
			config.override("commonNamingPrefix", "namingPrefix");
			TestLog logger2 = new TestLog(new LoggingConfig(config));
			
			logger2.log(ExampleLogMessage.TEST_INFO, null, "arg1");
			assertLoggerContainsAndClear(logger2, 1, "namingPrefix TEST_INFO [arg1]", 1);
		}
		
		// Test throttling
		// new logger instance to reset any throttling
		logger = new TestLog(new LoggingConfig(Configuration.fromPropertiesFile("logging/logutil.properties")));
		for (int i = 0; i < 15; i++)
			logger.log(ExampleLogMessage.TEST_ERROR, null);
		for (int i = 0; i < 20; i++)
			logger.log(ExampleLogMessage.TEST_INFO, null);
		assertLoggerContains(logger, 22, "TEST_ERROR", 11); // 11th is the throttling message
		assertLoggerContains(logger, 22, "TEST_INFO", 11); // 11th is the throttling message
		assertLoggerContains(logger, 22, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [TEST_ERROR,", 1);
		assertLoggerContainsAndClear(logger, 22, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [TEST_INFO,", 1);
		logger.setTimeFactor(100);
		Thread.sleep(200); // should reset throttling window
		logger.log(ExampleLogMessage.TEST_ERROR, null); // should produce 2 messages
		logger.log(ExampleLogMessage.TEST_INFO, null);
		logger.setTimeFactor(Float.NaN);
		for (int i = 0; i < 14; i++)
			logger.log(ExampleLogMessage.TEST_INFO, null);
		assertLoggerContains(logger, 12 + 2, "TEST_INFO", 12); // 11th is the throttling message, 12th is the 'skipped X' msg
		assertLoggerContains(logger, 12 + 2, "LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED [TEST_INFO, 10]", 1);
		assertLoggerContainsAndClear(logger, 12 + 2, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [TEST_INFO,", 1);
		logger.setTimeFactor(100);
		Thread.sleep(200); // should reset throttling window
		logger.log(ExampleLogMessage.TEST_INFO, null);
		logger.setTimeFactor(Float.NaN);
		assertLoggerContains(logger, 2, "TEST_INFO", 2); // 2nd is the 'skipped X' msg
		assertLoggerContainsAndClear(logger, 2, "LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED [TEST_INFO, 5]", 1);
		
		// Log more stuff for monitoring testing
		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_WARN", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_WARN", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);

		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_EXTERNAL_WARN, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_WARN", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);

		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_EXTERNAL_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);

		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_EXTERNAL_DATA_LOSS", 1);
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);

		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_ERROR, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_ERROR", 1);
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);
		
		Thread.sleep(3); // advance time
		logger.log(ExampleLogMessage.TEST_CRITICAL, null);
		assertLoggerContainsAndClear(logger, 1, "TEST_CRITICAL", 1);
		
		LoggingStatus status = logger.getStatus(10000);
		long afterFirstStatus = System.currentTimeMillis(); 
		log.info("First status: {}", status);
		
		assertBetweenInclusive(status.getStatusCreatedAt(), start, afterFirstStatus);
		assertBetweenInclusive(status.getLastLoggedWarnTimestamp(), start, afterFirstStatus);
		assertBetweenInclusive(status.getLastLoggedErrorTimestamp(), start, afterFirstStatus);
		assertBetweenInclusive(status.getLastLoggedCriticalTimestamp(), start, afterFirstStatus);
		
		assertContains(status.getLastLoggedWarnText(), "TEST_EXTERNAL_WARN");
		assertContains(status.getLastLoggedErrorText(), "TEST_ERROR");
		assertContains(status.getLastLoggedCriticalText(), "TEST_CRITICAL");
		
		assertEquals(status.getLoggedWarnCount(), 1);
		assertEquals(status.getLoggedExternalWarnCount(), 2);
		assertEquals(status.getLoggedExternalErrorCount(), 3);
		assertEquals(status.getLoggedExternalDataLossCount(), 4);
		assertEquals(status.getLoggedErrorCount(), 21); // 5 + 15 in a loop + 1 to reset throttling
		assertEquals(status.getLoggedCriticalCount(), 6);
		assertEquals(status.getLoggedTotalWarnOrHigherCount(), 37);
		assertEquals(status.getLoggedTotalErrorOrHigherCount(), 34);
		
		Thread.sleep(5);
		long start2 = System.currentTimeMillis();
		
		logger.log(ExampleLogMessage.TEST_WARN, null);
		logger.log(ExampleLogMessage.TEST_EXTERNAL_DATA_LOSS, null);
		logger.log(ExampleLogMessage.ASSERTION_FAILED, null);
		
		{
			LoggingStatus status2 = logger.getStatus(10000); // must hit status cache
			assertEquals(status2, status);
		}
		
		logger.setTimeFactor(200);
		Thread.sleep(100);
		LoggingStatus status2 = logger.getStatus(10000); // this should be a new status
		assertNotEquals(status2, status);
		long afterSecondStatus = System.currentTimeMillis();
		log.info("Second status: {}", status2);
		
		assertBetweenInclusive(status2.getStatusCreatedAt(), start2, afterSecondStatus);
		assertBetweenInclusive(status2.getLastLoggedWarnTimestamp(), start2, afterSecondStatus);
		assertBetweenInclusive(status2.getLastLoggedErrorTimestamp(), start2, afterSecondStatus);
		assertBetweenInclusive(status2.getLastLoggedCriticalTimestamp(), start2, afterSecondStatus);
		
		assertContains(status2.getLastLoggedWarnText(), "TEST_WARN");
		assertContains(status2.getLastLoggedErrorText(), "TEST_EXTERNAL_DATA_LOSS");
		assertContains(status2.getLastLoggedCriticalText(), "ASSERTION_FAILED");
		
		assertEquals(status2.getLoggedWarnCount(), 2);
		assertEquals(status2.getLoggedExternalWarnCount(), 2);
		assertEquals(status2.getLoggedExternalErrorCount(), 3);
		assertEquals(status2.getLoggedExternalDataLossCount(), 5);
		assertEquals(status2.getLoggedErrorCount(), 21); // 5 + 15 in a loop + 1 to reset throttling
		assertEquals(status2.getLoggedCriticalCount(), 7);
		assertEquals(status2.getLoggedTotalWarnOrHigherCount(), 40);
		assertEquals(status2.getLoggedTotalErrorOrHigherCount(), 36);
		
		// ====================== TEST NON-CLASSIFIED LOGGING BELOW ===================
		
		// Test throttling
		// new logger instance to reset any throttling
		logger = new TestLog(new LoggingConfig(Configuration.fromPropertiesFile("logging/logutil.properties")));
		for (int i = 0; i < 15; i++)
			logger.logNonClassified(LogMessageSeverity.INFO, "typeA", null);
		for (int i = 0; i < 20; i++)
			logger.logNonClassified(LogMessageSeverity.INFO, "typeB", null);
		assertLoggerContains(logger, 22, "typeA", 11); // 11th is the throttling message
		assertLoggerContains(logger, 22, "typeB", 11); // 11th is the throttling message
		assertLoggerContains(logger, 22, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [typeA_INFO,", 1);
		assertLoggerContainsAndClear(logger, 22, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [typeB_INFO,", 1);
		for (int i = 0; i < 13; i++)
			logger.logNonClassified(LogMessageSeverity.WARN, "typeA", null);
		assertLoggerContains(logger, 11, "typeA", 11); // 11th is the throttling message
		assertLoggerContainsAndClear(logger, 11, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [typeA_WARN,", 1);
		
		
		logger.setTimeFactor(100);
		Thread.sleep(200); // should reset throttling window
		logger.logNonClassified(LogMessageSeverity.INFO, "typeB", null); // should produce 2 messages
		logger.logNonClassified(LogMessageSeverity.INFO, "typeA", null);
		logger.setTimeFactor(Float.NaN);
		for (int i = 0; i < 14; i++)
			logger.logNonClassified(LogMessageSeverity.INFO, "typeA", null);
		assertLoggerContains(logger, 12 + 2, "typeA", 12); // 11th is the throttling message, 12th is the 'skipped X' msg
		assertLoggerContains(logger, 12 + 2, "LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED [typeA_INFO, 5]", 1);
		assertLoggerContainsAndClear(logger, 12 + 2, "LOG_MESSAGE_TYPE_MESSAGES_MAY_BE_SKIPPED_FOR [typeA_INFO,", 1);
		logger.setTimeFactor(100);
		Thread.sleep(200); // should reset throttling window
		logger.logNonClassified(LogMessageSeverity.INFO, "typeA", null);
		logger.setTimeFactor(Float.NaN);
		assertLoggerContains(logger, 2, "typeA", 2); // 2nd is the 'skipped X' msg
		assertLoggerContainsAndClear(logger, 2, "LOG_MESSAGE_TYPE_PREVIOUS_MESSAGES_SKIPPED [typeA_INFO, 5]", 1);
		
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
