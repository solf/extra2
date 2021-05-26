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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

/**
 * Class for editing files.
 * NOT thread safe.
 * 
 * The idea is you read file line by line and either keep line as is or write in
 * some modifications; then at the end you have option to 'commit' your changes
 * or 'revert' them.
 * 
 * By default, reading lines sequentially (without writing in-between) will transfer
 * line contents to the result (so that you don't need to worry about writing out
 * lines that you don't modify); but see {@link #setSpoolUnmodifiedLines(boolean)}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class FileEditor
{
	/**
	 * Input file reader.
	 */
	private final BufferedReader reader;
	
	/**
	 * Output file writer.
	 */
	private final OutputStreamWriter writer;
	
	/**
	 * Input file.
	 */
	private final File inputFile;
	
	/**
	 * Output file (temporal).
	 */
	private final File outputFile;
	
	/**
	 * Bak file.
	 */
	private final File bakFile;
	
	/**
	 * Line delimiter.
	 */
	private final String lineDelimiter;
	
	/**
	 * Whether to spool unmodified lines to output (by default true).
	 */
	private boolean spoolUnmodifiedLines = true;
	
	/**
	 * Line for spooling (if any).
	 */
	@Nullable
	private String lineForSpooling = null;
	
	/**
	 * Constructor (defaults to default encoding).
	 */
	public FileEditor(String fileName) 
	{
		this(new File(fileName));
	}
	
	/**
	 * Constructor.
	 */
	public FileEditor(String fileName, String encoding) 
	{
		this(new File(fileName), encoding);
	}

	
	/**
	 * Constructor (defaults to default encoding).
	 */
	public FileEditor(File file) 
	{
		this(file, Charset.defaultCharset().displayName());
	}
	
	/**
	 * Constructor.
	 */
	public FileEditor(File file, String encoding) 
	{
		if (!file.exists())
			throw new IllegalArgumentException("File doesn't exist: " + file);
		if (!file.isFile())
			throw new IllegalArgumentException("File is not a file: " + file);
		
		inputFile = file;
		outputFile = new File(inputFile.getAbsolutePath() + ".tmp");
		bakFile = new File(inputFile.getAbsolutePath() + ".bak");
		
		if (outputFile.exists())
			throw new IllegalStateException("Temporal output file already exists: " + outputFile);
		if (bakFile.exists())
			throw new IllegalStateException("Bak file already exists: " + bakFile);
		
		try
		{
			lineDelimiter = detectLineDelimiter(inputFile);
			
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), encoding));
			writer = new OutputStreamWriter(new FileOutputStream(outputFile), encoding); 
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException: " + e, e);
		}
	}

	/**
	 * Gets whether to spool unmodified lines to output (by default true).
	 * @return whether to spool unmodified lines to output
	 */
	public boolean getSpoolUnmodifiedLines()
	{
		return spoolUnmodifiedLines;
	}

	/**
	 * Sets whether to spool unmodified lines to output (by default true).
	 * @param newSpoolUnmodifiedLines new value of whether to spool unmodified lines to output
	 */
	public void setSpoolUnmodifiedLines(boolean newSpoolUnmodifiedLines)
	{
		spoolUnmodifiedLines = newSpoolUnmodifiedLines;
	}
	
	/**
	 * Detects line delimiter used in particular file.
	 * If no line feeds found, assumes simple \n (without \r)
	 */
	public static String detectLineDelimiter(String fileName) throws IOException
	{
		return detectLineDelimiter(new File(fileName));
	}
	
	/**
	 * Detects line delimiter used in particular file.
	 * If no line feeds found, assumes simple \n (without \r)
	 */
	public static String detectLineDelimiter(File file) throws IOException
	{
		try (FileReader fr = new FileReader(file))
		{
			while(true)
			{
				int c = fr.read();
				if (c < 0)
					return "\n"; // Default option.
				if (c == '\n')
					return "\n"; // It's not CR-LF, so it's just LF
				if (c == '\r')
				{
					// Decide between CR and CR-LF
					c = fr.read();
					if (c == '\n')
						return "\r\n"; // CR-LF
					else
						return "\r";
				}
			}
		}
	}
	
	/**
	 * Reads next line.
	 * 
	 * @return next line or null if end-of-file
	 */
	@Nullable
	public String readLine()
	{
		if (spoolUnmodifiedLines)
			if (lineForSpooling != null)
				writeLine(lineForSpooling);
		
		try
		{
			lineForSpooling = reader.readLine(); // Remember in case we'll need to spool it.
			return lineForSpooling;
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException: " + e, e);
		}
	}
	
	/**
	 * Writes a line to the file.
	 * Line is terminated with auto-detected line delimiter.
	 * Written lines become effective only if you 'commit' changes.
	 */
	public void writeLine(String line)
	{
		try
		{
			charactersWrite(line);
			writer.write(lineDelimiter);
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException: " + e, e);
		}
	}
	
	/**
	 * Writes a sequence of characters to file (without line termination).
	 * Written lines become effective only if you 'commit' changes.
	 */
	public void charactersWrite(String characters)
	{
		try
		{
			lineForSpooling = null; // Reset this as we wrote out some modifications
			writer.write(characters);
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException: " + e, e);
		}
	}
	
	/**
	 * Gets current line as it was read from file.
	 * <p>
	 * Returns null if called before reading first line, after reading last line,
	 * or if the current line was modified in any way (by calling any write).
	 */
	@Nullable
	public String getCurrentLine()
	{
		return lineForSpooling;
	}
	
	/**
	 * Removes the line that was just read from the file (which only has meaning
	 * if {@link #spoolUnmodifiedLines} is true (which it is by default)).
	 * 
	 * @throws IllegalStateException if line wasn't yet read, or if line was 
	 * 		already removed or modified 
	 */
	public void removeReadLine() throws IllegalStateException
	{
		if (lineForSpooling == null)
			throw new IllegalStateException("Unable to remove read line, either nothing was read or it was already removed or modified.");
		
		lineForSpooling = null;
	}
	
	/**
	 * Reverts any changes.
	 * Closes all streams.
	 * No method invocations will work after this.
	 */
	public void revert()
	{
		try
		{
			writer.close();
			reader.close();
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException on revert: " + e, e);
		}
		
		if (!outputFile.delete())
			throw new IllegalStateException("Failed to delete output file on revert: " + outputFile);
	}
	
	/**
	 * Commits current state to file (content includes everything previously written via writeLine).
	 * Closes all streams.
	 * No method invocations will work after this.
	 */
	public void truncateAndCommit()
	{
		try
		{
			writer.close();
			reader.close();
		} catch (IOException e)
		{
			throw new IllegalStateException("IOException on commit: " + e, e);
		}
		
		if (!inputFile.renameTo(bakFile))
			throw new IllegalStateException("Failed to rename input file on commit: " + inputFile + "; to: " + bakFile);

		if (!outputFile.renameTo(inputFile))
			throw new IllegalStateException("Failed to rename output file on commit: " + outputFile + "; to: " + inputFile);
		
		if (!bakFile.delete())
			throw new IllegalStateException("Failed to delete bak file on commit: " + outputFile);
	}
	
	/**
	 * Spools the remainder of the file without changes and commits (i.e. use
	 * this method if you already made all the changes you need and just want
	 * to leave the remainder of the file as is).
	 */
	public void spoolAndCommit()
	{
		while(true)
		{
			String line = readLine();
			if (line == null)
				break;
			writeLine(line);
		}
		
		truncateAndCommit();
	}
	
	/**
	 * Replaces first match (of given regexp) within file with given replacement
	 * string (including expanding group matching expressions $1, $2, etc.).
	 * Starts to search at the current position (rather than at beginning).
	 * Exactly similar to {@link String#replaceFirst(String, String)} except
	 * works on file.
	 * 
	 * Does not commit or revert editor.
	 * Position is set after the matching line (if match found) or at the end
	 * (if match is not found). 
	 * 
	 * @throws IllegalStateException if match not found or other kinds of IOErrors occur
	 */
	public void replaceFirst(String regexp, String replacement)
		throws IllegalStateException
	{
		Pattern pattern = Pattern.compile(regexp);
		
		while(true)
		{
			String line = readLine();
			if (line == null)
				throw new IllegalStateException("Match not found for: " + regexp);
			
			Matcher m = pattern.matcher(line);
			if (!m.find())
			{
				writeLine(line);
				continue;
			}
			
			// Found a match!
			writeLine(line.replaceFirst(regexp, replacement));
			return;
		}
	}
	
	/**
	 * Replaces first match (of given regexp) within file with given replacement
	 * string (including expanding group matching expressions $1, $2, etc.).
	 * Starts to search at the current position (rather than at beginning).
	 * Exactly similar to {@link String#replaceFirst(String, String)} except
	 * works on file.
	 * 
	 * Spools & commits in case of success, reverts in case of failure.
	 * 
	 * @throws IllegalStateException if match not found or other kinds of IOErrors occur
	 */
	public void replaceFirstAndCommit(String regexp, String replacement)
		throws IllegalStateException
	{
		try
		{
			replaceFirst(regexp, replacement);
			spoolAndCommit();
		} catch (RuntimeException e)
		{
			revert();
			throw e;
		}
	}
	
	/**
	 * Replaces all matches (of given regexp) within file with given replacement
	 * string (including expanding group matching expressions $1, $2, etc.).
	 * Starts to search at the current position (rather than at beginning).
	 * Exactly similar to {@link String#replaceAll(String, String)} except
	 * works on file.
	 *<p> 
	 * Does not commit or revert editor.
	 * <p>
	 * Position is set at the end of file.
	 * 
	 * @throws IllegalStateException if match not found or other kinds of IOErrors occur
	 */
	public void replaceAll(String regexp, String replacement)
		throws IllegalStateException
	{
		Pattern pattern = Pattern.compile(regexp);
		boolean haveMatch = false;
		
		while(true)
		{
			String line = readLine();
			if (line == null)
				if (haveMatch)
					return;
				else
					throw new IllegalStateException("Match not found for: " + regexp);
			
			Matcher m = pattern.matcher(line);
			if (!m.find())
			{
				writeLine(line);
				continue;
			}
			
			// Found a match!
			writeLine(line.replaceAll(regexp, replacement));
			haveMatch = true;
		}
	}
	
	/**
	 * Replaces all matches (of given regexp) within file with given replacement
	 * string (including expanding group matching expressions $1, $2, etc.).
	 * Starts to search at the current position (rather than at beginning).
	 * Exactly similar to {@link String#replaceAll(String, String)} except
	 * works on file.
	 * 
	 * Spools & commits in case of success, reverts in case of failure.
	 * 
	 * @throws IllegalStateException if match not found or other kinds of IOErrors occur
	 */
	public void replaceAllAndCommit(String regexp, String replacement)
		throws IllegalStateException
	{
		try
		{
			replaceAll(regexp, replacement);
			spoolAndCommit();
		} catch (RuntimeException e)
		{
			revert();
			throw e;
		}
	}
	
	/**
	 * Finds next (starting from current position) match for given regexp.
	 * Lookup is done via {@link Matcher#find()}, so add ^ and/or $ if you want
	 * to match start/end of line.
	 * 
	 * Position is set after matching line (if you do a {@link #writeLine(String)},
	 * you'll replace this last line).
	 * 
	 * You can read groups from the returned matcher.
	 * 
	 * @throws IllegalStateException if no match is found or other kind of IOError
	 */
	public Matcher find(String regexp) throws IllegalStateException
	{
		Pattern pattern = Pattern.compile(regexp);
		
		while(true)
		{
			String line = readLine();
			if (line == null)
				throw new IllegalStateException("Match not found for: " + regexp);
			
			Matcher m = pattern.matcher(line);
			if (!m.find())
			{
				writeLine(line);
				continue;
			}
			
			// Found a match!
			return m;
		}
	}
	
	/**
	 * Re-creates editor -- reverts this instance and creates another copy so that
	 * you can start processing from the beginning. 
	 */
	public FileEditor recreate()
	{
		revert();
		return new FileEditor(inputFile);
	}
}
