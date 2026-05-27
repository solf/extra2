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
package io.github.solf.extra2.file;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.BiConsumerWithExceptionType;
import io.github.solf.extra2.concurrent.TriConsumerWithExceptionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TODO ccc - class description
 * <p>
 * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
public class OutputStreamWithAfterClose extends OutputStream
{
	/**
	 * Target where the data is actually written.
	 */
	private final OutputStream target;
	
	/**
	 * Hook for what happens after the stream is closed.
	 */
	private final TriConsumerWithExceptionType<OutputStream, Boolean, Boolean, IOException> afterCloseHook;
	
	/**
	 * Whether there were any failures (exceptions) during operations.
	 */
//aaa uncomment	@Getter
	private boolean hadFailures = false;
	
	/**
	 * Whether the client (of this {@link OutputStream}) indicated that everything 
	 * is successful.
	 * <p>
	 * This is false initially and client must invoke setter to indicate the
	 * success; the final value is passed to {@link #afterCloseHook}
	 */
	//aaa add getter, setter
	private boolean clientSuccess = false;

	@Override
	public void write(int b)
		throws IOException
	{
		boolean success = false;
		try
		{
			target.write(b);
			success = true;
		} finally
		{
			if (!success)
				hadFailures = true;
		}
	}

	@Override
	public void write(byte[] b)
		throws IOException
	{
		boolean success = false;
		try
		{
			target.write(b);
			success = true;
		} finally
		{
			if (!success)
				hadFailures = true;
		}
	}

	@Override
	public void write(byte[] b, int off, int len)
		throws IOException
	{
		boolean success = false;
		try
		{
			target.write(b, off, len);
			success = true;
		} finally
		{
			if (!success)
				hadFailures = true;
		}
	}

	@Override
	public void flush()
		throws IOException
	{
		boolean success = false;
		try
		{
			target.flush();
			success = true;
		} finally
		{
			if (!success)
				hadFailures = true;
		}
	}

	@Override
	public void close()
		throws IOException
	{
		// qqq Auto-generated method stub
		super.close();
	}
	
	
}
