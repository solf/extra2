/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Web-related utilities, e.g. for reading content from URL and such.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WebUtil
{
	/**
	 * Default connect timeout (60 seconds).
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;
	
	/**
	 * Default read timeout (300 seconds).
	 */
	public static int DEFAULT_READ_TIMEOUT = 300000;
	
	/**
	 * Reads contents at the given URL as String.
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static String readURL(String url) throws IllegalStateException
	{
		return readURL(url, null, null);
	}
	
	/**
	 * Reads contents at the given URL as String.
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static String readURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword) 
		throws IllegalStateException
	{
		return readURL(url, basicAuthLogin, basicAuthPassword, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}
	
	/**
	 * Reads contents at the given URL as String.
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param socketConnectTimeout in milliseconds; 0 means infinite timeout
	 * @param socketReadTimeout in milliseconds; 0 means infinite timeout
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static String readURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword,
		int socketConnectTimeout, int socketReadTimeout)
		throws IllegalStateException
	{
		try
		{
			try (InputStream is = readBinaryURL(url, basicAuthLogin, basicAuthPassword, socketConnectTimeout, socketReadTimeout))
			{
				byte[] buff = new byte[1024];
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				while(true)
				{
					int len = is.read(buff);
					if (len < 0)
						break;
					bos.write(buff, 0, len);
				}
				
				final String body = new String(bos.toByteArray());
	//			System.out.println(body);
				return body;
			}
		} catch (Exception e)
		{
			throw new IllegalStateException("Exception reading URL [" + url + "]: " + e, e);
		}
	}
	
	/**
	 * Reads contents at the given URL as {@link InputStream}
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static InputStream readBinaryURL(String url) 
		throws IllegalStateException
	{
		return readBinaryURL(url, null, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}
	
	/**
	 * Reads contents at the given URL as {@link InputStream}
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static InputStream readBinaryURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword) 
		throws IllegalStateException
	{
		return readBinaryURL(url, basicAuthLogin, basicAuthPassword, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}
	
	/**
	 * Reads contents at the given URL as {@link InputStream}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param socketConnectTimeout in milliseconds; 0 means infinite timeout
	 * @param socketReadTimeout in milliseconds; 0 means infinite timeout
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static InputStream readBinaryURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword,
		int socketConnectTimeout, int socketReadTimeout)
		throws IllegalStateException
	{
		try
		{
			URLConnection conn = new URL(url).openConnection();
			conn.setReadTimeout(socketReadTimeout);
			conn.setConnectTimeout(socketConnectTimeout);
			
			if (basicAuthLogin != null)
			{
				String userpass = basicAuthLogin + ":" + (basicAuthPassword == null ? "" : basicAuthPassword);
				String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes());

				conn.setRequestProperty ("Authorization", basicAuth);
			}
			
			conn.connect();
			if (conn instanceof HttpURLConnection)
			{
				int responseCode = ((HttpURLConnection)conn).getResponseCode();
				if (responseCode != 200)
					throw new IllegalStateException("Response code [" + responseCode + "] while reading URL: " + url);
			}
			
			return conn.getInputStream();
		} catch (Exception e)
		{
			throw new IllegalStateException("Exception reading URL [" + url + "]: " + e, e);
		}
	}
}