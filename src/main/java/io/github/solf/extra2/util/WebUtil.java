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
package io.github.solf.extra2.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

/**
 * Web-related utilities, e.g. for reading content from URL and such.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param method HTTP method, it's an error if this is set on non-http request
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static String readURL(String url, 
		@Nullable String basicAuthLogin, @Nullable String basicAuthPassword,
		@Nullable String method) 
			throws IllegalStateException
	{
		return readURL(url, basicAuthLogin, basicAuthPassword, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, method);
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
		return readURL(url, basicAuthLogin, basicAuthPassword, socketConnectTimeout, socketReadTimeout, null);
	}
	
	/**
	 * Reads contents at the given URL as String.
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param socketConnectTimeout in milliseconds; 0 means infinite timeout
	 * @param socketReadTimeout in milliseconds; 0 means infinite timeout
	 * @param method HTTP method, it's an error if this is set on non-http request
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static String readURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword,
		int socketConnectTimeout, int socketReadTimeout, @Nullable String method)
		throws IllegalStateException
	{
		try
		{
			try (InputStream is = readBinaryURL(url, basicAuthLogin, basicAuthPassword, socketConnectTimeout, socketReadTimeout, method))
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
	 * <p>
	 * Connect timeout is set to {@link #DEFAULT_CONNECT_TIMEOUT} 60 seconds 
	 * and read timeout is set to {@link #DEFAULT_READ_TIMEOUT}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param method HTTP method, it's an error if this is set on non-http request
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static InputStream readBinaryURL(String url, @Nullable String basicAuthLogin, 
		@Nullable String basicAuthPassword, @Nullable String method) 
		throws IllegalStateException
	{
		return readBinaryURL(url, basicAuthLogin, basicAuthPassword, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, method);
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
		return readBinaryURL(url, basicAuthLogin, basicAuthPassword, socketConnectTimeout, socketReadTimeout, null);
	}
	
	/**
	 * Reads contents at the given URL as {@link InputStream}
	 * 
	 * @param basicAuthLogin if not null, specifies basic-auth login
	 * @param basicAuthPassword if not null, specifies basic-auth password
	 * @param socketConnectTimeout in milliseconds; 0 means infinite timeout
	 * @param socketReadTimeout in milliseconds; 0 means infinite timeout
	 * @param method HTTP method, it's an error if this is set on non-http request
	 * 
	 * @throws IllegalStateException if something goes wrong
	 */
	public static InputStream readBinaryURL(String url, @Nullable String basicAuthLogin, @Nullable String basicAuthPassword,
		int socketConnectTimeout, int socketReadTimeout, @Nullable String method)
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
			
			if (method != null)
			{
				if (conn instanceof HttpURLConnection)
				{
					((HttpURLConnection)conn).setRequestMethod(method);
				}
				else
					throw new IllegalStateException("Unable to set 'method' on non-HTTP connection, got: " + conn.getClass());
			}
			
			conn.connect();
			if (conn instanceof HttpURLConnection)
			{
				int responseCode = ((HttpURLConnection)conn).getResponseCode();
				if ((responseCode < 200) || (responseCode >= 300))
					throw new IllegalStateException("Response code [" + responseCode + "] while reading URL: " + url);
			}
			
			return conn.getInputStream();
		} catch (Exception e)
		{
			throw new IllegalStateException("Exception reading URL [" + url + "]: " + e, e);
		}
	}
}
