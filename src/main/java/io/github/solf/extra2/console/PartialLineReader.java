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
package io.github.solf.extra2.console;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Class that provides capability to read whole or partial (via timeout) lines
 * from the source.
 * 
 * Useful for e.g. reading console output from the process.
 * 
 * NOT THREAD-SAFE.
 *
 * @author Sergey Olefir
 */
public class PartialLineReader implements Closeable
{
	/**
	 * Reader that is used to convert from bytes to characters.
	 */
	private final Reader reader;
	
	/**
	 * Limit on how long we wait for partial line to complete.
	 */
	private final long waitLineCompletionMs = 200;
	
	/**
	 * Whether next LF should be skipped if read.
	 */
	boolean skipLF = false;
	
	/**
	 * Constructor.
	 */
	public PartialLineReader(InputStream inputStream)
	{
		if (inputStream == null)
			throw new IllegalArgumentException("Input stream must not be null.");
		
		this.reader = new InputStreamReader(inputStream);
	}
	
	/**
	 * Closes underlying streams.
	 */
	@Override
	public void close() throws IOException
	{
		reader.close();
	}
	
	/**
	 * Reads line.
	 * In case of partial line being available (not finished with new line delimiter),
	 * waits up to configured time for the line to complete -- returns partial
	 * line if line doesn't complete in the alloted time.
	 * 
	 * @return line (possibly partial) or null in case end-of-stream reached
	 */
	public String readLine() throws IOException
	{
		int first = readNextChar(true);
		if (first < 0)
			return null;
		
		char firstCh = (char)first;
		if (isLineTerminationChar(firstCh))
			return "";
		
		// Ok, if we are here, we have start of a line.
		StringBuilder sb = new StringBuilder();
		sb.append(firstCh);
		long timeLimit = System.currentTimeMillis() + waitLineCompletionMs;
		
		boolean slept = false;
		while(true)
		{
			int code = readNextChar(false);
			if (code == -1)
				break; // End of stream.
			if (code == -2)
			{
				if (slept)
				{
					break; // Consider this the end of partial line.
				}
				else
				{
					// Sleep some to let partial line complete.
					long duration = timeLimit - System.currentTimeMillis();
					if (duration > 0) 
					{
						try
						{
							Thread.sleep(duration);
						} catch( InterruptedException e )
						{
							// Ignore.
						}
					}
					slept = true;
					continue; // Go to next symbol.
				}
			}
			
			// Okay, we've read normal character.
			char ch = (char)code;
			if (isLineTerminationChar(ch))
				break; // End of line.
			sb.append(ch); // If it is just a normal char -- append it.
		}
		
		return sb.toString();
	}
	
	/**
	 * Reads next character (blocking).
	 * 
	 * @return character, -1 if end-of-stream, -2 if non-blocking and not available
	 */
	private int readNextChar(boolean blocking) throws IOException
	{
		if (!blocking)
		{
			if (!reader.ready())
				return -2;
		}
		
		int code = reader.read();
		if (code < 0)
			return -1;
		
		char ch = (char)code;
		if (skipLF)
		{
			skipLF = false;
			if (ch == '\n')
			{
				return readNextChar(blocking); // Return next character instead.
			}
		}
		
		if (ch == '\r')
			skipLF = true;
		
		return ch;
	}
	
	/**
	 * Determines whether character terminates line.
	 */
	private boolean isLineTerminationChar(char ch)
	{
		if (ch == '\r')
			return true;
		if (ch == '\n')
			return true;
		
		return false;
	}
}
