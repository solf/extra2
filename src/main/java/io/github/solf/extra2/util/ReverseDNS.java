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

import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * An attempt at reverse DNS in java.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ReverseDNS
{
    /**
     * System property name that can be used to override {@link #getOwnHostName()}
     * value -- if set, then this name will be used instead of auto-detection.
     */
    public static final String OWN_HOST_NAME_OVERRIDE = "com.github.solf.extra2.ownHostName";
    
	/**
	 * Own ip address (if null, then it is not known, throw {@value #exceptionOwnIp}
	 * instead).
	 */
	@Nullable
	private static final String ownIp;
	
	/**
	 * If {@link #ownIp} is null, then this is the exception to be thrown when trying to
	 * access {@link #getOwnIp()} -- this allows to defer failure until IP
	 * is actually requested -- so other methods can be used even if own IP
	 * is not known.
	 */
	private static final IllegalStateException exceptionOwnIp;
	
	/**
	 * Own name.
	 */
	private static final String ownHostName;
	
	static
	{
		InetAddress localAddr = null;
		{
			String ip = null;
			IllegalStateException exc = new IllegalStateException("assertion error"); // dummy
			try
			{
				// Create socket to remote host -- this should provide us with
				// usable local endpoint.
				
				// Used to use google here, but it became problematic in Russia
				// And 'example.com' is a better choice anyway, it is reserved
				//try (Socket sock = new Socket("www.google.com", 80))
				
				try (Socket sock = new Socket("example.com", 80))
				{
					localAddr = sock.getLocalAddress();
				
					ip = localAddr.getHostAddress();
				}
			} catch (Exception e)
			{
				exc = new IllegalStateException("Unable to determine local host address: " + e, e);
				ip = null;
			}
			
			ownIp = ip;
			exceptionOwnIp = exc;
		}
		
		// Try to determine hostname.
		String hName = System.getProperty(OWN_HOST_NAME_OVERRIDE);
		
		if(hName == null) {
    		try
    		{
    			Process p = Runtime.getRuntime().exec("hostname -f");
    			p.waitFor();
    			if (p.exitValue() == 0)
    			{
    				BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
    				hName = reader.readLine();
    			}
    		} catch (Exception e)
    		{
    			// Ignored.
    		}
		}
		
		// Try differently if the above fails.
		if (hName == null)
		{
			try
			{
				Process p = Runtime.getRuntime().exec("hostname");
				p.waitFor();
				if (p.exitValue() == 0)
				{
					BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
					hName = reader.readLine();
				}
			} catch (Exception e)
			{
				// Ignored.
			}
		}
		
		// Try differently if the above fails.
		if (hName == null)
		{
			if (nullable(localAddr) != null) // Looks like Eclipse bug -- it doesn't think localAddr can be null here. 
			{
				String h = reverseDNS(localAddr);
				// If resolved not to IP...
				if (!h.equals(ownIp))
					hName = h;
			}
		}
		
		// Try differently if the above fails.
		if (hName == null)
		{
			try
			{
				hName = InetAddress.getLocalHost().getHostName();
			} catch (Exception e)
			{
				throw new IllegalStateException("Unable to determine local host name: " + e, e);
			}
		}
		
		if (hName == null)
			throw new IllegalStateException("Unable to determine local host name.");
		
		ownHostName = hName;
	}
	
	/**
	 * Makes a best effort reverse-DNS attempt.
	 * If fails, string representing IP is returned.
	 */
	public static String reverseDNS(InetAddress address)
	{
		try
		{
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

			DirContext ctx = new InitialDirContext(env);
			try
			{
				StringBuilder attrName = new StringBuilder(30);
				byte[] addr = address.getAddress();
				for (int i = addr.length - 1; i >= 0; i--)
				{
					int b = addr[i];
					// Unsign byte.
					if (b < 0)
						b = 256 + b;
					attrName.append(Integer.toString(b));
					attrName.append('.');
				}
				attrName.append("in-addr.arpa");

				// Lookup.
				Attributes attrs = ctx.getAttributes(attrName.toString(), new String[]{"PTR"});
	
				Attribute attr = attrs.get("PTR");
				return (String)nnChecked(attr.get()); // In theory there could be more than one.
			} finally
			{
				ctx.close();
			}
		}
		catch( Exception e )
		{
			// Fallback
			return address.getCanonicalHostName();
		}
	}
	
	/**
	 * Gets own IP (this is cached on initialization so doesn't reflect any
	 * potential changes of IP).
	 * 
	 * @throws IllegalStateException if was unable to determine its own IP
	 * 		during initialization (e.g. if unable to connect to www.google.com)
	 */
	public static String getOwnIp() throws IllegalStateException
	{
		if (ownIp != null)
			return ownIp;
		
		throw exceptionOwnIp;
	}
	
	/**
	 * Gets own name (best effort at Fully Qualified Domain Name; fallback 
	 * names if fails to obtain FQDN).
	 */
	public static String getOwnHostName()
	{
		return ownHostName;
	}
}
