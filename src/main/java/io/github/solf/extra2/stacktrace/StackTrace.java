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
package io.github.solf.extra2.stacktrace;

import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Provides certain utilities for call stack trace investigation. 
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class StackTrace
{
	/**
	 * List of common classes prefixes that are skipped in stack trace if the
	 * appropriate flag is set.
	 */
	public static final List<String> COMMON_CLASSES_PREFIXES;
	static
	{
		ArrayList<String> list = new ArrayList<String>();
		list.add("java.");
		list.add("javax.");
		list.add("jdk.");
		list.add("sun.");
		list.add("org.testng.");
		list.add("org.junit.");
		list.add("org.apache.");
		list.add("org.restlet.");
		list.add("org.eclipse.");
		
		COMMON_CLASSES_PREFIXES = Collections.unmodifiableList(list);
	}
	
	/**
	 * Self class name.
	 */
	private static final String selfClassName = StackTrace.class.getName();
	
	/**
	 * Checks whether stack trace invocation printing is requested on JVM level
	 * via -Dio.github.solf.extra2.debug.showInvocationTraces=true
	 */
	public static boolean isShowInvocationTrace()
	{
		return "true".equals(System.getProperty("io.github.solf.extra2.debug.showInvocationTraces"));
	}
	
	/**
	 * Dumps short invocation trace to System.out if enabled via {@link #isShowInvocationTrace()}
	 * Trace doesn't include this method or the line directly invoking this method.
	 * Trace skips common classes.
	 * 
	 * @see #getShortExceptionStackTrace(Throwable, int, boolean)
	 * 
	 * @param skipClasses if not null, then initial stack frames up and including
	 * 		these class(es) appearance will be skipped -- useful if e.g. you want to
	 * 		show stack trace until it crosses your own class boundary, e.g. in
	 * 		a logging method; multiple classes are supported to cover the case
	 * 		when both 'class' and 'superclass' methods are used in the stack trace
	 */
	public static void dumpShortInvocationTraceIfEnabled(Class<?>... skipClasses)
	{
		if (isShowInvocationTrace())
			dumpShortInvocationTrace(skipClasses);
	}
	
	/**
	 * Dumps short invocation trace to System.out
	 * Trace doesn't include this method or the line directly invoking this method.
	 * Trace skips common classes.
	 * 
	 * @see #getShortExceptionStackTrace(Throwable, int, boolean)
	 * 
	 * @param skipClasses if not null, then initial stack frames up and including
	 * 		these class(es) appearance will be skipped -- useful if e.g. you want to
	 * 		show stack trace until it crosses your own class boundary, e.g. in
	 * 		a logging method; multiple classes are supported to cover the case
	 * 		when both 'class' and 'superclass' methods are used in the stack trace
	 */
	public static void dumpShortInvocationTrace(Class<?>... skipClasses)
	{
		System.out.println(getShortExceptionStackTrace(null, true, skipClasses));
	}
	
	/**
	 * Gets short invocation trace
	 * Trace doesn't include this method or the line directly invoking this method.
	 * Trace skips common classes.
	 * 
	 * @see #getShortExceptionStackTrace(Throwable, int, boolean)
	 * 
	 * @param skipClasses if not null, then initial stack frames up and including
	 * 		these class(es) appearance will be skipped -- useful if e.g. you want to
	 * 		show stack trace until it crosses your own class boundary, e.g. in
	 * 		a logging method; multiple classes are supported to cover the case
	 * 		when both 'class' and 'superclass' methods are used in the stack trace
	 */
	public static String getShortInvocationTrace(Class<?>... skipClasses)
	{
		return getShortExceptionStackTrace(null, true, skipClasses);
	}
	
	/**
	 * Gets short invocation trace
	 * SKIPS this method and all initial stack frames from the invoking class
	 * (i.e. starts with the first line that is NOT in the invoking class)
	 * Trace skips common classes.
	 * 
	 * @see #getShortExceptionStackTrace(Throwable, int, boolean)
	 */
	public static String getShortInvocationTraceSkipSelf()
	{
		// Need to figure out what to skip.
		Throwable e = new Throwable(); 
		String callerClassName = e.getStackTrace()[1].getClassName();
		
		return getShortExceptionStackTrace(null, true, callerClassName);
	}

	
	/**
	 * Empty string array for reuse.
	 */
	private static String[] EMPTY_STRING_ARRAY = {};
	
	/**
	 * Gets condensed (one-line) stack trace for the exception.
	 * Format is something like:
	 * io.github.solf.extra2.tests.SomeTest.innerTest(SomeTest.java:268), 321, 4; io.github.solf.extra2.tests.SomeTest.test(SomeTest.java:22)
	 * (i.e. it only lists line numbers while they are in the same class).
	 * <p>
	 * This skips common classes.
	 * 
	 * @param e exception to prepare stack trace for
	 * 
	 * @return short exception stack trace
	 */
	public static String getShortExceptionStackTrace(final Throwable e)
			throws IllegalArgumentException, NullPointerException
	{
		return getShortExceptionStackTrace(e, true, EMPTY_STRING_ARRAY);
	}	
	
	/**
	 * Gets condensed (one-line) stack trace for the exception.
	 * Format is something like:
	 * io.github.solf.extra2.tests.SomeTest.innerTest(SomeTest.java:268), 321, 4; io.github.solf.extra2.tests.SomeTest.test(SomeTest.java:22)
	 * (i.e. it only lists line numbers while they are in the same class).
	 * 
	 * @param e exception to prepare stack trace for, if null, it'll create new
	 * 		exception internally and such stack trace will skip initial frames
	 * 		that are inside this ({@link StackTrace}) class
	 * @param skipCommonClasses if true, stack trace will silently exclude 'common'
	 * 		classes, particularly java.*, javax.*, jdk.*, sun.*, org.testng.*, 
	 * 		org.junit.*, org.apache.*, org.restlet.*, org.eclipse.*
	 * @param skipClasses if not null, then initial stack frames up and including
	 * 		these class(es) appearance will be skipped -- useful if e.g. you want to
	 * 		show stack trace until it crosses your own class boundary, e.g. in
	 * 		a logging method; multiple classes are supported to cover the case
	 * 		when both 'class' and 'superclass' methods are used in the stack trace
	 * 
	 * @return short exception stack trace
	 */
	public static String getShortExceptionStackTrace(@Nullable final Throwable e, 
		final boolean skipCommonClasses, final Class<?>... skipClasses)
			throws IllegalArgumentException, NullPointerException
	{
		String[] skipClassNames = EMPTY_STRING_ARRAY;
		if ((nullable(skipClasses) != null) && (skipClasses.length > 0))
		{
			skipClassNames = new @Nonnull String[skipClasses.length];
			for (int i = 0; i < skipClasses.length; i++)
				skipClassNames[i] = skipClasses[i].getName();
		}
		
		return getShortExceptionStackTrace(e, skipCommonClasses, skipClassNames);
	}	
	
	/**
	 * Gets condensed (one-line) stack trace for the exception.
	 * Format is something like:
	 * io.github.solf.extra2.tests.SomeTest.innerTest(SomeTest.java:268), 321, 4; io.github.solf.extra2.tests.SomeTest.test(SomeTest.java:22)
	 * (i.e. it only lists line numbers while they are in the same class).
	 * 
	 * @param e exception to prepare stack trace for, if null, it'll create new
	 * 		exception internally and such stack trace will skip initial frames
	 * 		that are inside this ({@link StackTrace}) class
	 * @param skipCommonClasses if true, stack trace will silently exclude 'common'
	 * 		classes, particularly java.*, javax.*, jdk.*, sun.*, org.testng.*, 
	 * 		org.junit.*, org.apache.*, org.restlet.*, org.eclipse.*
	 * @param skipClasses if not null, then initial stack frames up and including
	 * 		these class(es) appearance will be skipped -- useful if e.g. you want to
	 * 		show stack trace until it crosses your own class boundary, e.g. in
	 * 		a logging method; multiple classes are supported to cover the case
	 * 		when both 'class' and 'superclass' methods are used in the stack trace
	 * 
	 * @return short exception stack trace
	 */
	public static String getShortExceptionStackTrace(@Nullable final Throwable e, 
		final boolean skipCommonClasses, final String... skipClasses)
			throws IllegalArgumentException, NullPointerException
	{
		StringBuilder sb = new StringBuilder(2048);
		
		boolean skipSelf = false;
		Throwable exception = e;
		if (exception == null)
		{
			exception = new Throwable();
			skipSelf = true;
		}
		
		StackTraceElement[] stack = exception.getStackTrace();
		
		String[] skipClassNames = nullable(skipClasses) == null ? null : skipClasses.length == 0 ? null : skipClasses; 
		boolean foundSkipClass = false;
		String currentClassName = null;
		frameCycle:
		for (int i = 0; i < stack.length; i++)
		{
			StackTraceElement item = stack[i];
			
			String className = item.getClassName();
			
			if (skipSelf)
			{
				if (className.equals(selfClassName))
					continue;
				else
					skipSelf = false; // Only skip own initial frames.
			}
			
			if (skipClassNames != null)
			{
				for (String skipName : skipClassNames)
				{
					if (className.equals(skipName))
					{
						foundSkipClass = true;
						continue frameCycle;
					}
				}
				
				// Class doesn't match skip class name.
				if (foundSkipClass)
				{
					skipClassNames = null; // Skipped enough, stop skipping.
				}
				else
				{
					continue; // Still looking for class to skip.
				}
			}
			
			if (skipCommonClasses)
			{
				for (String prefix : COMMON_CLASSES_PREFIXES)
				{
					if (className.startsWith(prefix))
						continue frameCycle;
				}
			}
			
			if (className.equals(currentClassName))
			{
				// Only append line number.
				sb.append(", ");
				sb.append(item.getLineNumber());
			}
			else
			{
				// Append entire invocation trace.
				if (sb.length() != 0)
					sb.append("; ");
				sb.append(item.toString());
				currentClassName = className;
			}
		}
		
		if (sb.length() == 0)
		{
			if (e == null)
				throw new IllegalArgumentException("Empty short stack trace for arguments: exception=null, skipClass=" + Arrays.toString(skipClasses) + ", skipCommonClasses=" + skipCommonClasses);
			else
				throw new IllegalArgumentException("Empty short stack trace for arguments: exception=<see cause>, skipClass=" + Arrays.toString(skipClasses) + ", skipCommonClasses=" + skipCommonClasses, e);
		}
		
		return sb.toString();
	}
	
	/**
	 * Gets current line number in the Java class.
	 * 
	 * @return could return negative number (e.g. for native methods)
	 */
	public static int getCurrentLineNumber()
	{
		return getCurrentLineNumber(1);
	}
	
	/**
	 * Gets current line number in the Java class.
	 * 
	 * @param how many additional frames to skip; 0 will give you the invoking
	 * 		line, 1 will give the previous invoking like and so on
	 * 
	 * @throws IllegalArgumentException if skipFrames is negative
	 * 
	 * @return could return negative number (e.g. for native methods)
	 */
	public static int getCurrentLineNumber(int skipFrames) throws IllegalArgumentException
	{
		if (skipFrames < 0)
			throw new IllegalArgumentException("skipFrames may not be negative: " + skipFrames);
		
		return Thread.currentThread().getStackTrace()[2 + skipFrames].getLineNumber();
	}
	
	/**
	 * Gets current Java class (fully-qualified).
	 */
	public static String getCurrentJavaClassFullName()
	{
		return getCurrentJavaClassFullName(1);
	}
	
	/**
	 * Gets current Java class (fully-qualified).
	 * 
	 * @param how many additional frames to skip; 0 will give you the invoking
	 * 		class, 1 will give the previous invoking place (could be the same class
	 * 		or other) and so on
	 * 
	 * @throws IllegalArgumentException if skipFrames is negative
	 */
	public static String getCurrentJavaClassFullName(int skipFrames)
	{
		if (skipFrames < 0)
			throw new IllegalArgumentException("skipFrames may not be negative: " + skipFrames);
		
		return Thread.currentThread().getStackTrace()[2 + skipFrames].getClassName();
	}
	
	/**
	 * Gets current Java class (short name).
	 * 
	 * @throws IllegalStateException if something goes wrong (e.g. can't figure
	 * 		out short name).
	 */
	public static String getCurrentJavaClassShortName() throws IllegalStateException
	{
		return getCurrentJavaClassShortName(1);
	}
	
	/**
	 * Gets current Java class (short name).
	 * 
	 * @param how many additional frames to skip; 0 will give you the invoking
	 * 		class, 1 will give the previous invoking place (could be the same class
	 * 		or other) and so on
	 * 
	 * @throws IllegalArgumentException if skipFrames is negative
	 * @throws IllegalStateException if something goes wrong (e.g. can't figure
	 * 		out short name).
	 */
	public static String getCurrentJavaClassShortName(int skipFrames) throws IllegalStateException
	{
		if (skipFrames < 0)
			throw new IllegalArgumentException("skipFrames may not be negative: " + skipFrames);
		
		String fqn = getCurrentJavaClassFullName(1 + skipFrames); // +1 to account for this method
		
		int lastDot = fqn.lastIndexOf('.');
		if (lastDot < 0)
			return fqn; // Possibly class is in default package.
		
		int pos = lastDot + 1;
		if (pos >= fqn.length())
			throw new IllegalStateException("Trailing dot in FQN: " + fqn);
		
		return fqn.substring(pos);
	}
}
