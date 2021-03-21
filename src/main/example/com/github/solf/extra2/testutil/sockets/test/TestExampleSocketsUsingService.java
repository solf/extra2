/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.testutil.sockets.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import com.github.solf.extra2.testutil.MockSocketData;
import com.github.solf.extra2.testutil.TestUtil;
import com.github.solf.extra2.testutil.TestUtil.AsyncTestRunner;
import com.github.solf.extra2.testutil.sockets.ExampleSocketsUsingService;

/**
 * Tests {@link ExampleSocketsUsingService}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TestExampleSocketsUsingService
{
	@Test
	public void test() throws Exception
	{
		MockExampleSocketsUsingService service = new MockExampleSocketsUsingService("anaddr", 1234, 5000, 5000);
		
		// Run service check asynchronously
		AsyncTestRunner<Void> asyncFuture = TestUtil.runAsynchronously(() -> {
			service.checkRemoteIsAlive();
		});
		
		// Run our checks with time limit to prevent unexpected hang-ups
		TestUtil.runWithTimeLimit(10000, () -> {
		
			MockSocketData mockSocketData = service.getMockSocketService().waitForAndClearTheOnlyConnectedSocketMock(2000);
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(mockSocketData.getInputStream()));
				PrintWriter writer = new PrintWriter(mockSocketData.getOutputStream(), true/*auto-flush*/);)
			{
				// Check correct prompt
				String line = reader.readLine();
				assertEquals("PING", line);
				
				writer.println("ACK"); // send correct response
				
				// Check socket is closed.
				assertNull(reader.readLine());
			}
		});
		
		asyncFuture.getResult(1000); // will throw exception if async thread doesn't finish or throws exception 
	}
}
