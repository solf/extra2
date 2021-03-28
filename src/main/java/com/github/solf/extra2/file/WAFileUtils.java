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
package com.github.solf.extra2.file;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;

/**
 * Some file utilities.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class WAFileUtils
{
	/**
	 * Copies a file.
	 * 
	 * Throws exception if fails.
	 */
	public static void copyFile(String src, String dest)
		throws IllegalStateException
	{
		try
		{
			FileUtils.copyFile(new File(src), new File(dest));
		} catch( IOException e )
		{
			throw new IllegalStateException("Failure copying file: " + e, e);
		}
	}
	
	/**
	 * Rolling backup (once per day) for a given directory.
	 * The target directory contains backup (one per day, named in YYYY-MM-DD
	 * format).
	 * Optionally backup directory can also contain named directory that is used
	 * to store latest backup (in addition to normal location) -- this is useful
	 * for e.g. automatic off-site backups from that directory.
	 * 
	 * NOTE: will check if 'today' backup already exists, if it does -- then does
	 * nothing (i.e. no more than one actual backup per day)
	 * 
	 * NOTE2: old backup deletion is date-based; if the method is not invoked at
	 * least once per day, some old backups may never be deleted
	 * 
	 * @param today today's date (used for naming backup directory); it is provided
	 * 		as it could differ from system date depending on usage
	 * @param dirToBackup which directory to backup; exception if directory does
	 * 		not exist
	 * @param storageDir where to backup (this directory will contain YYYY-MM-DD
	 * 		dirs and optional 'yesterday' dir)
	 * @param numberOfDays for how many days to keep backups
	 * @param additionalDirectory if not null, latest backup will also be copied
	 * 		to that directory (in addition to normal location)
	 * 
	 * @return true if backup was performed, false if not (if backup was already
	 * 		done today)
	 * 
	 * @throws IllegalStateException if something is wrong, e.g. missing dir to
	 * 		backup
	 */
	public static boolean directoryRollingBackup(LocalDate today, String dirToBackup,
		String storageDir, int numberOfDays, @Nullable String additionalDirectory)
		throws IllegalStateException
	{
		File fDirToBackup = new File(dirToBackup);
		if (!fDirToBackup.exists())
			throw new IllegalArgumentException("Missing source directory for rolling backup: " + dirToBackup);
		if (!fDirToBackup.isDirectory())
			throw new IllegalArgumentException("Source for rolling backup is not a directory: " + dirToBackup);
		
		// Backup directory.
		{
			String backupName = today.toString();
			File bakDir = new File(storageDir + File.separatorChar + backupName);
			
			// Only if backup doesn't exist yet.
			if( bakDir.exists() )
				return false;
				
			bakDir.mkdirs();
			try
			{
				FileUtils.copyDirectory(fDirToBackup, bakDir);
			} catch( IOException e )
			{
				// Whoops, backup failed.
				throw new IllegalStateException("Failed to backup database directory: " + e, e);
			}

			// Also create 'named' backup directory if requested.
			if (additionalDirectory != null)
			{
				File fAdditionalDirectory = new File(storageDir + File.separatorChar + additionalDirectory);
				if( fAdditionalDirectory.exists() )
				{
					try
					{
						FileUtils.deleteDirectory(fAdditionalDirectory);
					} catch( IOException e )
					{
						// Whoops, old backup cleanup failed.
						throw new IllegalStateException("Failed to delete old backup directory: " + e, e);
					}
				}
				try
				{
					FileUtils.copyDirectory(fDirToBackup, fAdditionalDirectory);
				} catch( IOException e )
				{
					// Whoops, backup failed.
					throw new IllegalStateException("Failed to backup database directory to additional [" + additionalDirectory + "] folder: " + e, e);
				}
			}
		}

		// Delete old backup.
		{
			String monthAgo = today.minusDays(numberOfDays).toString();
			File oldBakDir = new File(storageDir + File.separatorChar + monthAgo);
			// Delete old backup directory if exists.
			if (oldBakDir.exists() && oldBakDir.isDirectory())
			{
				try
				{
					FileUtils.deleteDirectory(oldBakDir);
				} catch( IOException e )
				{
					// Whoops, old backup cleanup failed.
					throw new IllegalStateException("Failed to delete old backup directory: " + e, e);
				}
			}
		}
		
		// All good, we did backup.
		return true;
	}
}
