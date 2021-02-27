/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.util;

import java.lang.management.ManagementFactory;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility for (system)process-related stuff.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ProcessUtil
{
	/**
	 * Current JVM process pid.
	 * Only makes sense if {@link #jvmPidExceptionMessage} is null
	 */
	private static final int jvmPid;
	
	/**
	 * Message of the problem that happened while retrieving {@link #jvmPid}
	 * null if everything is okay.
	 */
	@Nullable private static final String jvmPidExceptionMessage;
	
	static
	{
		{
			// Initialize JVM pid.
			int pid = -1;
			String msg = null;
			try
			{
				String name = ManagementFactory.getRuntimeMXBean().getName();
				if (name == null)
				{
					pid = -1;
					msg = "JVM pid cannot be obtained: null process name";
				}
				else
				{
					String pidString = name.split("@")[0];
					
					try
					{
						pid = Integer.parseInt(pidString);
					} catch (NumberFormatException e)
					{
						pid = -1;
						msg = "JVM pid cannot be obtained: unable to parse pid from process name (expected 'pid@hostname'): " + name;
					}
				}
			} catch (Throwable e)
			{
				pid = -1;
				msg = "JVM pid cannot be obtained: " + e.toString();
			}
			
			jvmPid = pid;
			jvmPidExceptionMessage = msg;
		}
	}
	
	/**
	 * Tries to get current JVM process pid.
	 * It should *probably* work, it does:
	 * ManagementFactory.getRuntimeMXBean().getName() and expects something like
	 * 12345@localhost in return.
	 * 
	 * @throws IllegalStateException if for some reason pid cannot be obtained
	 */
	public static int getJVMPid() throws IllegalStateException
	{
		if (jvmPidExceptionMessage != null)
			throw new IllegalStateException(jvmPidExceptionMessage);
		
		return jvmPid;
	}
}
