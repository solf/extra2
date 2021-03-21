/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.testutil.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This is an example of trivial service using Sockets.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class ExampleSocketsUsingService
{
	@Getter
	private final String remoteAddr;
	
	@Getter
	private final int remotePort;
	
	@Getter
	private final long connectTimeout;

	/**
	 * Socket read timeout so that reads can't hang indefinitely.
	 */
	@Getter
	private final long soTimeout;

	
	/**
	 * Factory method to create sockets -- so it can be e.g. overridden in tests.
	 * @throws IOException 
	 */
	protected Socket connectSocket(String destAddress, int destPort, long connectTimeoutTime) throws IOException
	{
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(destAddress, destPort), (int)connectTimeoutTime);
		
		return socket;
	}
	
	/**
	 * Checks that remote service is alive.
	 */
	public void checkRemoteIsAlive() throws IOException
	{
		try (Socket socket = connectSocket(remoteAddr, remotePort, connectTimeout))
		{
			socket.setSoTimeout((int)soTimeout);
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter writer = new PrintWriter(socket.getOutputStream()))
			{
				writer.println("PING");
				writer.flush();
				
				String response = reader.readLine();
				if (!"ACK".equals(response))
					throw new IllegalStateException("Remote service sent a wrong answer: " + response);
			}
		}
	}
}
