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

import static io.github.solf.extra2.util.NullUtil.fakeVoid;
import static io.github.solf.extra2.util.NullUtil.nnChecked;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.joda.time.LocalDate;

import io.github.solf.extra2.concurrent.ConsumerWithExceptionType2;
import io.github.solf.extra2.concurrent.FunctionWithExceptionType2;

/**
 * TODO ccc - class description
 *
 * aaa provide path-based access for using with e.g. Files utils
 * 
 * @author Sergey Olefir
 */
@NonNullByDefault
public class DirManager
{
	/**
	 * Suffix added to filenames for safe writes (data is written to these files
	 * first).
	 */
	private static final String TMP_FILE_SUFFIX = ".tmp";
	
	// ccc
	private final File rootDir;
	
	//ccc
	private final File backupDir;
	
	//ccc
	private final int daysToBackup;
	
	/**
	 * Constructor.
	 * ccc
	 */
	public DirManager(String rootDir, boolean createIfMissing, @Nullable LocalDate todayForBackup) 
			throws IOException
	{
		this(rootDir, createIfMissing, todayForBackup, null, null);
	}
	
	/**
	 * Constructor.
	 * ccc
	 */
	public DirManager(File rootDir, boolean createIfMissing, @Nullable LocalDate todayForBackup) 
			throws IOException
	{
		this(rootDir, createIfMissing, todayForBackup, null, null);
	}

	/**
	 * Constructor.
	 * ccc
	 */
	public DirManager(String rootDir, boolean createIfMissing, @Nullable LocalDate todayForBackup,
		@Nullable String backupDir, @Nullable Integer daysToBackup) 
			throws IOException
	{
		this(new File(rootDir), createIfMissing, todayForBackup, 
			backupDir == null ? null : new File(backupDir), 
			daysToBackup);
	}
	
	/**
	 * Constructor.
	 * 
	 * ccc
	 */
	public DirManager(File rootDir, boolean createIfMissing, @Nullable LocalDate todayForBackup,
		@Nullable File backupDir, @Nullable Integer daysToBackup) 
			throws IOException
	{
		this.rootDir = rootDir;
		this.backupDir = backupDir == null ? new File(rootDir.getParent(), rootDir.getName() + "_bak") : backupDir;
		this.daysToBackup = daysToBackup == null ? 31 : daysToBackup;
		
		if (rootDir.exists())
		{
			if (!rootDir.isDirectory())
				throw new IOException("Root directory is not a directory: " + rootDir);
		}
		else
		{
			if (!createIfMissing)
				throw new IOException("Root directory doesn't exist and creation is not allowed: " + rootDir);
			
			if (!rootDir.mkdirs())
				throw new IOException("Failed to create root directory : " + rootDir);
		}
		
		if (todayForBackup != null)
		{
			runDailyBackup(todayForBackup);
		}
	}
	
	//ccc
	public void runDailyBackup(LocalDate todayForBackup) throws IOException
	{
		try
		{
			WAFileUtils.directoryRollingBackup(todayForBackup, rootDir, backupDir, daysToBackup, "yesterday");
		} catch (Exception e)
		{
			throw new IOException("Failed to do directory rolling backup: " + e, e);
		}
	}
	
	/**
	 * Creates a {@link File} object for the given filename managed by this
	 * instance.
	 */
	private File createFile(String filename)
	{
		return new File(rootDir, filename);
	}
	
	//zzz
//	public void writeFile(String filename, ConsumerWithIOException<FileOutputStream> fos) throws IOException
//	{
//		writeFile(filename, _fos -> {
//			fos.accept(_fos);
//			return fakeVoid();
//		});
//	}

	//ccc
	public <E extends Exception> void writeFile(String filename, ConsumerWithExceptionType2<FileOutputStream, IOException, E> fos) throws IOException, E
	{
		writeFile(filename, false, fos);
	}
	
	//ccc
	public <E extends Exception> void writeFile(String filename, boolean append, ConsumerWithExceptionType2<FileOutputStream, IOException, E> fos) throws IOException, E
	{
		writeFile(filename, append,
			_fos -> {
				fos.accept(_fos);
				return fakeVoid();
			}); 
	}
	
	//zzz
//	public <R> R writeFile(String filename, FunctionWithIOException<FileOutputStream, R> fos) throws IOException
//	{
//		return writeFileWithException(filename,
//			_fos -> {return fos.apply(_fos);}); 
//	}

	//ccc
	public <R, E extends Exception> R writeFile(String filename, FunctionWithExceptionType2<FileOutputStream, R, IOException, E> fos) throws IOException, E
	{
		return writeFile(filename, false, fos);
	}
	
	//ccc
	public <R, E extends Exception> R writeFile(String filename, boolean append, FunctionWithExceptionType2<FileOutputStream, R, IOException, E> fos) throws IOException, E
	{
		File targetFile = createFile(filename);
		if (targetFile.exists() && !targetFile.isFile())
			throw new IOException("Not a file: " + targetFile);
		
		// Try to ensure the path exists.
		nnChecked(targetFile.getParentFile()).mkdirs();
		
		File tmpFile = createFile(getTmpFileNameFor(filename));
		if (tmpFile.exists())
			throw new IOException("TMP file already exists: " + tmpFile);
		
		if (append && targetFile.exists())
		{
			Files.copy(targetFile.toPath(), tmpFile.toPath());
		}
		
		// Write data to temporary file first and only use that data is there
		// were no exceptions.
		R result;
		boolean success = false;
		try
		{
			try (FileOutputStream _fos = new FileOutputStream(tmpFile))
			{
				result = fos.apply(_fos);
			}
			success = true;
		} finally
		{
			if (success)
			{
				// Will throw exception if something goes wrong here.
				renameTmpFileToOriginal(targetFile, tmpFile);
			}
			else
			{
				try
				{
					tmpFile.delete();
				} catch (Exception e)
				{
					// ignore it to keep original exception.
				}
			}
		}
		
		return result;
	}
	
	// ccc
	private void renameTmpFileToOriginal(File originalFile, File tmpFile) throws IOException
	{
		if (!originalFile.delete())
			throw new IOException("Failed to delete old original file: " + originalFile);
		
		if (!tmpFile.renameTo(originalFile))
			throw new IOException("Failed to rename tmp file to original file: " + tmpFile + "; to: " + originalFile);
	}
	
	/**
	 * Gets temporarily file name for the given target (this is the file used
	 * to write data to protect original file until write is successfully completed).
	 * <p>
	 * Public to allow client code to clean-up after crashes or something.
	 */
	public String getTmpFileNameFor(String filename)
	{
		return filename + TMP_FILE_SUFFIX;
	}

	//ccc
	public <E extends Exception> void readFile(String filename, ConsumerWithExceptionType2<FileInputStream, IOException, E> fis) 
		throws FileNotFoundException, IOException, E
	{
		readFile(filename,
			_fis -> {
				fis.accept(_fis);
				return fakeVoid();
			}); 
	}
	
	//ccc
	public <R, E extends Exception> R readFile(String filename, FunctionWithExceptionType2<FileInputStream, R, IOException, E> fis) 
		throws FileNotFoundException, IOException, E
	{
		File targetFile = createFile(filename);
		if (!targetFile.exists())
			throw new FileNotFoundException("File not found: " + targetFile);
		if (!targetFile.isFile())
			throw new FileNotFoundException("Not a file: " + targetFile);
		
		try (FileInputStream _fis = new FileInputStream(targetFile))
		{
			return fis.apply(_fis);
		}
	}
	
	//ccc 
	public boolean exists(String filename)
	{
		return createFile(filename).exists();
	}
	
	//ccc
	public boolean isFile(String filename)
	{
		return createFile(filename).isFile();
	}
	
	//ccc
	public void delete(String filename) throws IOException
	{
		Files.delete(createFile(filename).toPath());
	}
	
	/**
	 * This is provided for the cases when no other API would solve a problem
	 * for some reason. AVOID using this unless absolutely necessary.
	 * <p>
	 * Retieves {@link File} object for a file managed by this instance
	 * 
	 * @deprecated AVOID using this unless absolutely necessary; instead use
	 * 		other methods that provide safe-ish way of manipulating files.
	 */
	@Deprecated
	public File asFile(String filename)
	{
		return createFile(filename);
	}
}
