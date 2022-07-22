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
package io.github.solf.extra2.retry.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.DateTime;

import io.github.solf.extra2.retry.RRLConfig;
import io.github.solf.extra2.retry.RRLEventListener;
import io.github.solf.extra2.retry.RetryAndRateLimitService;

/**
 * An example service that can be used to asynchronously submit (and rate-limit)
 * requests to some HTTP endpoint.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExampleAsyncHttpSubmitterService extends RetryAndRateLimitService<String, Integer>
{

	/**
	 * @param config
	 * @param eventListener
	 */
	public ExampleAsyncHttpSubmitterService(RRLConfig config,
		RRLEventListener<String, Integer> eventListener)
	{
		super(config, eventListener);
	}

	/**
	 * @param config
	 */
	public ExampleAsyncHttpSubmitterService(RRLConfig config)
	{
		super(config);
	}

	@Override
	protected Integer processRequest(String input, int attemptNumber)
		throws InterruptedException,
		Exception
	{
		URL url = new URL("http://example.org/submitRequest?input=" + input);
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.connect();
		
		int responseCode = conn.getResponseCode();
		
		System.out.println(new DateTime() + " ATTEMPT: " + attemptNumber + "; Result code: " + responseCode);
		
		if ((responseCode >= 200) && (responseCode < 300))
			return responseCode;
		
		throw new IOException("Invalid response code: " + responseCode);
	}

	
}
