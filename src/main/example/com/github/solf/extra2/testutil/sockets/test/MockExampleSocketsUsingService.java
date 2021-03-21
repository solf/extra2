/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.testutil.sockets.test;

import java.io.IOException;
import java.net.Socket;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.solf.extra2.testutil.MockSocketService;
import com.github.solf.extra2.testutil.sockets.ExampleSocketsUsingService;

import lombok.Getter;

/**
 * Used for testing operation of {@link ExampleSocketsUsingService}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class MockExampleSocketsUsingService extends ExampleSocketsUsingService
{
	/**
	 * Mock socket service used for faking sockets for testing.
	 */
	@Getter
	private final MockSocketService mockSocketService = new MockSocketService(10240);
	

	/**
	 * Constructor.
	 */
	public MockExampleSocketsUsingService(String remoteAddr, int remotePort,
		long connectTimeout, long soTimeout)
	{
		super(remoteAddr, remotePort, connectTimeout, soTimeout);
	}


	/*
	 * mock the socket!
	 */
	@Override
	protected Socket connectSocket(String destAddress, int destPort,
		long connectTimeoutTime)
		throws IOException
	{
		return mockSocketService.connectSocket(destAddress, destPort, connectTimeoutTime);
	}
}
