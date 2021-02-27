/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.testutil;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import site.sonata.extra2.testutil.MockSocketData;
import site.sonata.extra2.testutil.TestUtil;
import site.sonata.extra2.testutil.TestUtil.AsyncTestRunner;

/**
 * Tests for {@link MockSocketData}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TestMockSocketData
{
	/** self-documenting */
	@Test
	public void testSocketInputPipe() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(100);
			
			try (
				InputStream input = mockSocketData.getMockSocket().getInputStream();
				OutputStream output = mockSocketData.getOutputStream())
			{
				for (int i = 0; i < 50; i++)
					output.write(i);
				output.flush();
				
				for (int i = 0; i < 50; i++)
					assertEquals(input.read(), i);
			}
		});
	}
	
	/** self-documenting */
	@Test
	public void testSocketOutputPipe() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(100);
			
			try (
				InputStream input = mockSocketData.getInputStream();
				OutputStream output = mockSocketData.getMockSocket().getOutputStream())
			{
				for (int i = 0; i < 50; i++)
					output.write(i);
				output.flush();
				
				for (int i = 0; i < 50; i++)
					assertEquals(input.read(), i);
			}
		});
	}
	
	/** self-documenting */
	@Test
	public void testInetAddress() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(100);
			
			assertEquals(mockSocketData.getMockSocket().getInetAddress(), MockSocketData.MOCK_SOCKET_INET_ADDRESS);
		});
	}
	
	/** self-documenting */
	@Test
	public void testSmallBufferSizeSocketOutputPipe() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(10);
			
			final AtomicInteger counter = new AtomicInteger(0);
			
			AsyncTestRunner<Void> asyncThread = TestUtil.runAsynchronously(() -> {
				for (int i = 0; i < 50; i++)
				{
					mockSocketData.getMockSocket().getOutputStream().write(i);
					counter.incrementAndGet();
				}
			});
			
			try
			{
				asyncThread.getResult(1000);
				assert false;
			} catch (ExecutionException e)
			{
				assert e.getCause() instanceof java.io.InterruptedIOException : e;
			}
			
			int v = counter.get();
			assert v < 50 : v;
		});
	}
	
	/** self-documenting */
	@Test
	public void testSmallBufferSizeSocketInputPipe() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(10);
			
			final AtomicInteger counter = new AtomicInteger(0);
			
			AsyncTestRunner<Void> asyncThread = TestUtil.runAsynchronously(() -> {
				for (int i = 0; i < 50; i++)
				{
					mockSocketData.getOutputStream().write(i);
					counter.incrementAndGet();
				}
			});
			
			try
			{
				asyncThread.getResult(1000);
				assert false;
			} catch (ExecutionException e)
			{
				assert e.getCause() instanceof java.io.InterruptedIOException : e;
			}
			
			int v = counter.get();
			assert v < 50 : v;
		});
	}
	
	/** self-documenting */
	@Test
	public void testSocketInputControl() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(10);
			
			mockSocketData.getControlForSocketInput().kill();
			
			assertEquals(mockSocketData.getMockSocket().getInputStream().read(), -1);
		});
	}
	
	/** self-documenting */
	@Test
	public void testSocketOutputControl() throws Exception
	{
		TestUtil.runWithTimeLimit(5000, () -> {
			MockSocketData mockSocketData = MockSocketData.createSocket(10);
			
			mockSocketData.getControlForSocketOutput().kill();
			
			try
			{
				mockSocketData.getMockSocket().getOutputStream().write(5);
				assert false;
			} catch (IOException e)
			{
				assert e.toString().contains("Stream [temporarily] killed") : e;
			}
		});
	}
}
