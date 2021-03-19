/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.file;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides an interface to read file line-by-line and provides support for
 * including files into each other via:
 * [[path/filename]]
 * notation (on a separate line).
 * <p>
 * Also provides support for 'pushing back' one or more lines to be read by 
 * subsequent {@link #readLine()}
 * <p>
 * Externally it all looks just like a collection of lines.
 * <p>
 *  * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class IncludingFileReader extends SimpleFileReader
{

	/**
	 * @param file
	 * @throws IllegalStateException
	 */
	public IncludingFileReader(File file)
		throws IllegalStateException
	{
		super(file, true);
	}

	/**
	 * @param fileName
	 * @throws IllegalStateException
	 */
	public IncludingFileReader(String fileName)
		throws IllegalStateException
	{
		super(fileName, true);
	}
	
}
