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
package io.github.solf.extra2.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import org.javatuples.Pair;

/**
 * This is special version of {@link ByteArrayInputStream} that is able to
 * read directly from {@link ByteArrayOutputStream} (involves reflection access
 * to access protected member of {@link ByteArrayOutputStream}).
 * 
 * Constructor makes a kind of 'snapshot' of the {@link ByteArrayOutputStream}
 * state at the moment of creation -- so if there are any concurrent additions
 * to {@link ByteArrayOutputStream}, they will NOT be reflected in this input
 * stream.
 * 
 * ATTN: using {@link ByteArrayOutputStream#reset()} concurrently (while this
 * input stream is being used) will result in INCONSISTENT DATA. This is the only
 * method that MAY NOT be called concurrently.
 *
 * @author Sergey Olefir
 */
public class BAOSInputStream extends ByteArrayInputStream
{
	/**
	 * Reflection accessor for {@link ByteArrayOutputStream} byte buffer.
	 */
	private static final Field byteBufferAccessor;
	
	static
	{
		try
		{
			byteBufferAccessor = ByteArrayOutputStream.class.getDeclaredField("buf");
			byteBufferAccessor.setAccessible(true);
		} catch (Exception e)
		{
			throw new RuntimeException("Failed to establish accessor to ByteArrayOutputStream buffer: " + e, e);
		}
	}
	
	/**
	 * Constructor.
	 */
	public BAOSInputStream(ByteArrayOutputStream baos)
	{
		this(extractBufferDefinition(baos));
	}
	
	/**
	 * Constructor.
	 */
	public BAOSInputStream(Pair<byte[], Integer> bufferDefinition)
	{
		super(bufferDefinition.getValue0(), 0, bufferDefinition.getValue1());
	}
	
	/**
	 * Extracts buffer definition from {@link ByteArrayOutputStream}
	 * 
	 * @throws IllegalStateException if access to buffer fails due to {@link IllegalAccessException}
	 */
	public static Pair<byte[], Integer> extractBufferDefinition(ByteArrayOutputStream baos)
		throws IllegalStateException
	{
		try
		{
			// Put a lock on BAOS so we can consistently get multiple values.
			synchronized(baos)
			{
				byte[] buffer = (byte[])byteBufferAccessor.get(baos);
				
				return new Pair<byte[], Integer>(buffer, baos.size());
			}
		} catch (IllegalAccessException e)
		{
			throw new IllegalStateException("Failed to read buffer data from ByteArrayOutputStream: " + e, e);
		}
	}
}
