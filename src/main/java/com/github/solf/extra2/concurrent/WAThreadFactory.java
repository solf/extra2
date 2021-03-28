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
package com.github.solf.extra2.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Thread factory for creating threads for executors.
 * 
 * Threads names are generated based on specified group name:
 * pool-[groupName]-thread-XX
 *
 * @author Sergey Olefir
 */
public class WAThreadFactory implements ThreadFactory
{
	/**
	 * Thread group.
	 */
	private final ThreadGroup threadGroup;
	
	/**
	 * Thread name prefix.
	 */
	private final String threadNamePrefix;
	
	/**
	 * Thread number.
	 */
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    
    /**
     * Threads daemon flag.
     */
    private final boolean threadDaemonFlag;
    
    /**
     * Threads priority.
     */
    private final int threadPriority;
	
	/**
	 * Constructor.
	 * Priority is set to {@link Thread#NORM_PRIORITY}
	 * 
	 * Threads names are generated based on specified group name:
	 * pool-[groupName]-thread-XX
	 * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 */
	public WAThreadFactory(String groupName, boolean daemon) throws IllegalArgumentException
	{
		this(groupName, daemon, Thread.NORM_PRIORITY);
	}

	/**
	 * Constructor.
	 * 
	 * Threads names are generated based on specified group name:
	 * pool-[groupName]-thread-XX
	 * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 * @param priority what priority threads should have, e.g. {@link Thread#NORM_PRIORITY}
	 */
	public WAThreadFactory(String groupName, boolean daemon, int priority) throws IllegalArgumentException
	{
		this(groupName, daemon, priority, null);
	}

	/**
	 * Constructor.
	 * 
	 * Threads names are generated based on specified group name:
	 * pool-[groupName]-thread-XX
	 * 
	 * @param groupName group name to be used for threads and also prefix for every thread name
	 * @param daemon whether threads should be daemon
	 * @param priority what priority threads should have, e.g. {@link Thread#NORM_PRIORITY}
	 * @param parentThreadGroup if not null, then this thread group is used as
	 * 		a parent for this factory's thread group (factory always creates
	 * 		new threads in its own {@link ThreadGroup})
	 */
	public WAThreadFactory(String groupName, boolean daemon, int priority, 
		@Nullable ThreadGroup parentThreadGroup) 
			throws IllegalArgumentException
	{
		if ((groupName == null) || (groupName.trim().isEmpty()))
			throw new IllegalArgumentException("Group name must be specified and non-empty.");
		if ((priority < Thread.MIN_PRIORITY) || (priority > Thread.MAX_PRIORITY))
			throw new IllegalArgumentException("Priority must be within allowed range " + Thread.MIN_PRIORITY + "-" + Thread.MAX_PRIORITY + ", got: " + priority);
		
		if (parentThreadGroup != null)
			threadGroup = new ThreadGroup(parentThreadGroup, groupName);
		else
			threadGroup = new ThreadGroup(groupName);
		threadNamePrefix = "pool-" + groupName + "-thread-";
		threadDaemonFlag = daemon;
		threadPriority = priority;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = new Thread(threadGroup, r, threadNamePrefix + threadNumber.incrementAndGet(), 0);
		if (t.isDaemon() != threadDaemonFlag)
			t.setDaemon(threadDaemonFlag);
		if (t.getPriority() != threadPriority)
			t.setPriority(threadPriority);
		return t;
	}

}
