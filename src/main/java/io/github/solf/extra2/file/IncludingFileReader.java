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

import java.io.File;

import org.eclipse.jdt.annotation.NonNullByDefault;

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
 * NB!!: NOT thread-safe.
 * <p>
 * You can override {@link #findIncludedFile(File, String)} 
 * (or even {@link #createContainerForIncludedFile(Container, String)}) in
 * order to modify how included files are looked up.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
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
	
	
	/**
	 * Creates container based on given (probably relative) file name and container
	 * that is used to determine current directory.
	 */
	@Override
	protected Container createContainerForIncludedFile(Container parentContainer, String fileName) throws IllegalStateException
	{
		return createContainer(findIncludedFile(parentContainer.file, fileName));
	}
	
	/**
	 * Based on the 'parent file' (file currently being read) and included file
	 * name must create a {@link File} instance that will be used to read
	 * included file.
	 * <p>
	 * This method is provided specifically as an extension point so that
	 * custom file lookup implementations are easily possible.
	 */
	protected File findIncludedFile(File parentFile, String fileName) throws IllegalStateException
	{
		return new File(parentFile.getParentFile(), fileName);
	}

}
