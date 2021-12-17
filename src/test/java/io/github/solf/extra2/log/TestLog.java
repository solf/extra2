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

import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;

import io.github.solf.extra2.log.example.ExampleLogMessage;
import io.github.solf.extra2.log.example.ExampleLoggingUtility;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Testing extensions for {@link BaseLoggingUtility}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault({PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}) // exclude ARRAY_CONTENTS because we accept null msg args
@Slf4j
public class TestLog extends ExampleLoggingUtility
{
	/**
	 * Messages that were actually logged via this logger.
	 */
	@Getter
	private final ArrayList<String> loggedMessages = new ArrayList<>(10);
	
	/**
	 * Time factor (compression factor) for this instance.
	 */
	@Getter @Setter
	private float timeFactor = Float.NaN; 
	
	/**
	 * @param config
	 */
	public TestLog(LoggingConfig config)
	{
		super(config);
	}

	@Override
	protected Logger spiGetLogger(ExampleLogMessage msg,
		@Nullable Throwable exception,
		Object @Nonnull... args)
		throws InterruptedException
	{
		return log;
	}

	@Override
	protected void spiLogMessage_FinalLogMessage(Logger theLog,
		String formattedMsg, ExampleLogMessage msg,
		@Nullable Throwable exception,
		Object @Nonnull... args)
	{
		String m = exception == null ? formattedMsg : formattedMsg + " EXCLOG: " + exception.toString();
		loggedMessages.add(m);
		
		super.spiLogMessage_FinalLogMessage(theLog, formattedMsg, msg, exception, args);
	}

	@Override
	protected float timeFactor()
	{
		return timeFactor;
	}
	
	
	
}
