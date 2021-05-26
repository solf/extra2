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

import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nnc;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.NonNullByDefault;

import org.jvnet.winp.WinProcess;

import io.github.solf.extra2.exception.EOFRuntimeException;
import io.github.solf.extra2.exception.InvalidUserInputException;

/**
 * Several 'console'-related utilities.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class Console
{
	/**
	 * Whether we are running on Windows.
	 */
	public static boolean isWindows = System.getProperty("os.name").contains("Windows");
	
	/**
	 * Reader for console input (lazily initialized, so access only inside synchronized).
	 */
	@Nullable
	private static BufferedReader consoleReader = null;
	
	/**
	 * Runs given external command in a given directory, prints out all the output
	 * to System.out, and returns return code.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 */
	public static int runExternal(String workingDir, String... command)
		throws IllegalArgumentException, IOException
	{
		return runExternal(new File(workingDir), command);
	}

	
	/**
	 * Runs given external command in a given directory, prints out all the output
	 * to System.out, and returns return code.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 */
	public static int runExternal(File workingDir, String... command)
		throws IllegalArgumentException, IOException
	{
		return runExternal(workingDir, true, command);
	}
	
	/**
	 * Runs given external command in a given directory, prints out all the output
	 * to System.out, and returns return code.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param printCommand if false, then the command itself is not printed;
	 * 		(useful e.g. if command line contains passwords or something)
	 * @param command command and its arguments
	 */
	public static int runExternal(File workingDir, boolean printCommand, String... command)
		throws IllegalArgumentException, IOException
	{
		if (nullable(workingDir) == null)
			throw new IllegalArgumentException("Working dir must be specified.");
		if (command.length == 0)
			throw new IllegalArgumentException("Command must be specified.");
		
		if (!workingDir.exists())
			throw new IllegalArgumentException("Directory doesn't exist: " + workingDir);
		if (!workingDir.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + workingDir);
		
		String[] cmdList = convertCommand(command);
		
		StringBuilder cmdString = new StringBuilder(100);
		for (String item : cmdList)
		{
			cmdString.append(item);
			cmdString.append(' ');
		}
		
		System.out.println();
		System.out.println("Executing: " + (printCommand ? cmdString : "<hidden>"));
		System.out.println("    in: " + workingDir.getAbsolutePath());
		System.out.println("---------------------------------------------------------");
		
		ProcessBuilder pb = new ProcessBuilder(cmdList);
		pb.directory(workingDir);
		pb.redirectErrorStream(true);
		
		Process process = pb.start();
		
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = output.readLine()) != null)
        {
        	System.out.println(line);
        }
		System.out.println("---------------------------------------------------------");
        
		return getExitCode(process);
	}
	
	/**
	 * Gets exit code of the finished process.
	 */
	public static int getExitCode(Process process)
	{
        
		try
		{
			return process.exitValue();
		} catch (IllegalThreadStateException e)
		{
			// I've had issues with processes on UNIX not exiting yet after console runs out, so wait some..
			try
			{
				Thread.sleep(500);
			} catch( InterruptedException e1 )
			{
				// Whatever.
			}
			
			// And read exit code again...
			return process.exitValue();
		}
	}

	/**
	 * Prepared {@link ProcessBuilder} for running external command in a given 
	 * directory, redirects stderr to stdout (so that entire output can be 
	 * retrieved via {@link Process#getInputStream()}
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 * @param announce if true, created process parameters are dumped to stdout
	 */
	public static ProcessBuilder prepareExternalProcess(String workingDir, boolean announce, String... command)
		throws IllegalArgumentException
	{
		return prepareExternalProcess(new File(workingDir), announce, command);
	}

	/**
	 * Prepared {@link ProcessBuilder} for running external command in a given 
	 * directory, redirects stderr to stdout (so that entire output can be 
	 * retrieved via {@link Process#getInputStream()}
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 * @param announce if true, created process parameters are dumped to stdout
	 */
	public static ProcessBuilder prepareExternalProcess(File workingDir, boolean announce, String... command)
		throws IllegalArgumentException
	{
		if (nullable(workingDir) == null)
			throw new IllegalArgumentException("Working dir must be specified.");
		if (command.length == 0)
			throw new IllegalArgumentException("Command must be specified.");
		
		if (!workingDir.exists())
			throw new IllegalArgumentException("Directory doesn't exist: " + workingDir);
		if (!workingDir.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + workingDir);
		
		String[] cmdList = convertCommand(command);
		
		if (announce)
		{
			
			StringBuilder cmdString = new StringBuilder(100);
			for (String item : cmdList)
			{
				cmdString.append(item);
				cmdString.append(' ');
			}
			
			System.out.println();
			System.out.println("Process: " + cmdString);
			System.out.println("     in: " + workingDir.getAbsolutePath());
			System.out.println("---------------------------------------------------------");
		}
		
		ProcessBuilder pb = new ProcessBuilder(cmdList);
		pb.directory(workingDir);
		pb.redirectErrorStream(true);
		return pb;
	}
	
	/**
	 * Sends line to process (finishes it with cr/lf as per platform).
	 */
	public static void sendLineToProcess(Process process, String line)
	{
		PrintWriter pw = new PrintWriter(process.getOutputStream());
		pw.println(line);
		pw.flush();
	}
	
	/**
	 * Sends partial line to process (DOES NOT finish it with cr/lf).
	 */
	public static void sendPartialLineToProcess(Process process, String line)
	{
		PrintWriter pw = new PrintWriter(process.getOutputStream());
		pw.print(line);
		pw.flush();
	}
	
	/**
	 * Runs given external command in a given directory asynchronously, optionally
	 * dumps output to System.out, provides way to control running process and
	 * send commands to it.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * Doesn't collect output for processing.
	 * 
	 * @param command command and its arguments
	 */
	public static ExternalProcess asyncRunExternal(File workingDir, boolean dumpOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		
		return new ExternalProcess(workingDir, dumpOutput, command);
	}
	
	/**
	 * Runs given external command in a given directory asynchronously, optionally
	 * dumps output to System.out, provides way to control running process and
	 * send commands to it.
	 * Tried to be platform-independent (at least works under Windows and Unix).

	 * Doesn't collect output for processing.
	 * 
	 * @param command command and its arguments
	 */
	public static ExternalProcess asyncRunExternal(String workingDir, boolean dumpOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		return new ExternalProcess(workingDir, dumpOutput, command);
	}
	
	/**
	 * Runs given external command in a given directory asynchronously, optionally
	 * dumps output to System.out, provides way to control running process and
	 * send commands to it.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 */
	public static ExternalProcess asyncRunExternal(File workingDir, boolean dumpOutput, boolean collectOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		
		return new ExternalProcess(workingDir, dumpOutput, collectOutput, command);
	}
	
	/**
	 * Runs given external command in a given directory asynchronously, optionally
	 * dumps output to System.out, provides way to control running process and
	 * send commands to it.
	 * Tried to be platform-independent (at least works under Windows and Unix).
	 * 
	 * @param command command and its arguments
	 */
	public static ExternalProcess asyncRunExternal(String workingDir, boolean dumpOutput, boolean collectOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		return new ExternalProcess(workingDir, dumpOutput, collectOutput, command);
	}

	/**
	 * Reads a single line from console if available.
	 * Result is trimmed for ease-of-use
	 * Result can be empty but cannot be null
	 * 
	 * @throws EOFRuntimeException if received null from console -- happens e.g.
	 * 		when console input is redirected and we reach end-of-file
	 * @throws IllegalStateException if exception occurs or console is not available
	 */
	public static String readConsoleLine() throws EOFRuntimeException, IllegalStateException
	{
		return nn(readConsoleLineUntrimmed().trim());
	}

	/**
	 * Reads a single line from console if available.
	 * Result is NOT trimmed
	 * Result can be empty but cannot be null
	 * 
	 * @throws EOFRuntimeException if received null from console -- happens e.g.
	 * 		when console input is redirected and we reach end-of-file
	 * @throws IllegalStateException if exception occurs or console is not available
	 */
	public static synchronized String readConsoleLineUntrimmed() throws EOFRuntimeException, IllegalStateException
	{
		try
		{
			// Must use 'old' reader copy if present to support e.g. input redirection (so input is not lost).
			BufferedReader reader = consoleReader;
			if (reader == null)
			{
				reader = new BufferedReader(new InputStreamReader(System.in));
				consoleReader = reader;
			}
			String line = reader.readLine();
			if (line == null)
				throw new EOFRuntimeException("Received 'null' from console -- probably reached end-of-file.");
			return nn(line);
		} catch (IOException e)
		{
			throw new IllegalStateException("Exception reading from console: " + e, e);
		}
	}
	
	/**
	 * Prompts user to enter some command (from console).
	 * If entered command is not valid, retries up to specified number of times.
	 * This is case-insensitive.
	 * 
	 * @param prompt this is shown to user using System.out each time s/he is
	 * 		(re)prompted; it is shown as is (i.e. new line is NOT appended)
	 * 
	 * @return user selection which is one of the valid responses given as arguments
	 * 
	 * @throws InvalidUserInputException if user fails to provide valid input
	 */
	public static String promptUser(String prompt, int maxTries, @Nonnull String @Nonnull... validResponses) 
		throws EOFRuntimeException, IllegalStateException, InvalidUserInputException
	{
		return promptUser(false, prompt, maxTries, validResponses);
	}
	
	/**
	 * Prompts user to enter some command (from console).
	 * If entered command is not valid, retries up to specified number of times.
	 * 
	 * @param prompt this is shown to user using System.out each time s/he is
	 * 		(re)prompted; it is shown as is (i.e. new line is NOT appended)
	 * 
	 * @return user selection which is one of the valid responses given as arguments
	 * 
	 * @throws InvalidUserInputException if user fails to provide valid input
	 */
	public static String promptUser(boolean caseSensitive, String prompt, int maxTries, @Nonnull String @Nonnull... validResponses) 
		throws EOFRuntimeException, IllegalStateException, InvalidUserInputException
	{
		for (int i = 0; i < maxTries; i++)
		{
			System.out.print(prompt);
			String line = readConsoleLine();
			for (@Nonnull String option : validResponses)
			{
				if (caseSensitive)
				{
					if (option.equals(line))
						return option;
				}
				else
				{
					if (option.equalsIgnoreCase(line))
						return option;
				}
			}
			
			System.out.println("Invalid input!\n");
		}
		
		throw new InvalidUserInputException("User failed to provide valid input given the choice of: " + Arrays.toString(validResponses));
	}
	
	/**
	 * Reads a password from console if available.
	 * Result can be empty but cannot be null
	 * 
	 * @param allowInsecure if true, then if System.console() is not present
	 * 		(e.g. under Eclipse) will fall back to reading line (password)
	 * 		without hiding it; if false, then will fail if System.console() is
	 * 		not present
	 * 
	 * @throws IllegalStateException if exception occurs or console is not available
	 */
	public static String readPassword(boolean allowInsecure) throws IllegalStateException
	{
		try
		{
			java.io.Console console = System.console();
			if (console != null)
			{
				char[] result = console.readPassword();
				if (result == null)
					throw new IllegalStateException("Failed to read password from console: null received (possibly console is not available or end-of-stream reached)");
				return new String(result);
			}
			else
			{
				if (allowInsecure)
					return readConsoleLineUntrimmed();
				else
					throw new IllegalStateException("Unable to read password: console is not available and insecure read is not allowed.");
			}
		} catch (Exception e)
		{
			if (e instanceof IllegalStateException)
				throw (IllegalStateException)e;
			throw new IllegalStateException("Failed to read password from console: " + e, e);
		}
	}
	
	/**
	 * Destroys given process and all its children.
	 */
	public static void destroyProcessAndChildren(Process process)
	{
		if (isWindows)
		{
			new WinProcess(process).killRecursively();
		}
		else
		{
			process.destroy(); // I think on UNIX should work automatically.
		}
	}
	
	/**
	 * Prepares command and its arguments so that it can work cross-platform
	 * (specifically on windows prepends cmd /x /c so that .bat files work).
	 */
	public static String[] convertCommand(final String... cmd)
	{
		if (!isWindows)
			return cmd;
		
		String[] result = nnc(new String[cmd.length + 3]);
		result[0] = "cmd";
		result[1] = "/X";
		result[2] = "/C";
		
		for (int i = 0; i < cmd.length; i++)
			result[i+3] = cmd[i];
		
		return result;
	}
}
