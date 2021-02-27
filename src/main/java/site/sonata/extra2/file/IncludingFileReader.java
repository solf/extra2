/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.file;

import static site.sonata.extra2.util.NullUtil.nnChecked;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AllArgsConstructor;

/**
 * Provides an interface to read file line-by-line and provides support for
 * including files into each other via:
 * [[path/filename]]
 * notation (on a separate line).
 * 
 * Externally it all looks just like a collection of lines.
 * 
 * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class IncludingFileReader
{
	/**
	 * File inclusion pattern.
	 */
	private static final Pattern fileInclusionPattern = Pattern.compile("^ *\\[\\[(.*)\\]\\] *$");
		
	/**
	 * Stack of readers currently open (top-most is currently being read).
	 */
	private final Stack<Container> readerStack = new Stack<Container>();
	
	/**
	 * Container for file & reader.
	 */
	@AllArgsConstructor
	private static class Container
	{
		public final File file;
		public final BufferedReader reader;
	}
	
	/**
	 * Constructor.
	 */
	public IncludingFileReader(String fileName) throws IllegalStateException
	{
		this(new File(fileName));
	}
	
	/**
	 * Constructor.
	 */
	public IncludingFileReader(File file) throws IllegalStateException
	{
		readerStack.push(createContainer(file));
	}
	
	/**
	 * Creates file reader.
	 */
	private static BufferedReader createReader(File file) throws IllegalStateException
	{
		if (!file.exists())
			throw new IllegalStateException("File doesn't exist: " + file);
		if (!file.isFile())
			throw new IllegalStateException("Not a file: " + file);
		
		try
		{
			return new BufferedReader(new FileReader(file));
		} catch( FileNotFoundException e )
		{
			throw new IllegalStateException("IOError: " + e, e);
		}
	}
	
	/**
	 * Creates container for a given file.
	 */
	private static Container createContainer(File file) throws IllegalStateException
	{
		return new Container(file, createReader(file));
	}
	
	/**
	 * Creates container based on given (probably relative) file name and container
	 * that is used to determine current directory.
	 */
	private static Container createContainer(Container parentContainer, String fileName) throws IllegalStateException
	{
		return createContainer(new File(parentContainer.file.getParentFile(), fileName));
	}
	
	/**
	 * Reads next line.
	 * 
	 * @return next line or null if no more lines
	 */
	@Nullable
	public String readLine() throws IllegalStateException
	{
		String line;
		Container container;
		try
		{
			while (true)
			{
				if (readerStack.isEmpty())
					return null; // Finished reading entire stack.
				
				container = readerStack.peek();
				line = container.reader.readLine(); 
				if (line != null)
					break;
				
				// Finished reading file.
				container.reader.close();
				readerStack.pop();
			}
		} catch (IOException e)
		{
			throw new IllegalStateException("IOError: " + e, e);
		}
		
		Matcher matcher = fileInclusionPattern.matcher(line);
		if (matcher.matches())
		{
			// Handle file inclusion.
			String includedFileName = nnChecked(matcher.group(1));
			readerStack.push(createContainer(container, includedFileName));
			
			// Recursively re-invoke so we read line from the included file.
			return readLine();
		}
		
		return line;
	}
	
	/**
	 * Closes this reader.
	 * If you've read all lines, you don't need to close it (though you still can).
	 */
	public void close() throws IllegalStateException
	{
		while(!readerStack.isEmpty())
		{
			try
			{
				readerStack.pop().reader.close();
			} catch( IOException e )
			{
				throw new IllegalStateException("IOError: " + e, e);
			}
		}
	}
}
