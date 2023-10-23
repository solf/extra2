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
package io.github.solf.extra2.pool;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nn;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.concurrent.Latch;
import io.github.solf.extra2.pool.SimpleObjectPool.PoolObjectWrapper;
import io.github.solf.extra2.util.TypeUtil;

/**
 * Tests for {@link SimpleObjectPool}
 * 
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraSimpleObjectPoolTest
{
	/**
	 * Increment for instance identity
	 */
	private static final int INCR_IDENTITY = 1 * 1000 * 1000;
	
	/**
	 * Increment for 'finished processing'
	 */
	private static final int INCR_FINISHED = 1 * 1000; 
	
	/**
	 * Increment for 'started processing'
	 */
	private static final int INCR_STARTED = 1; 

	/**
	 * Field for accessing internal pool queue.
	 */
	private static final Field poolQueueField;
	
	/**
	 * Field for accessing internal pool scheduler (pool maintenance)
	 */
	private static final Field schedulerField;
	
	static
	{
		try
		{
			poolQueueField = SimpleObjectPool.class.getDeclaredField("pool");
			poolQueueField.setAccessible(true);
			schedulerField = SimpleObjectPool.class.getDeclaredField("executorService");
			schedulerField.setAccessible(true);
		} catch (NoSuchFieldException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Tests {@link SimpleObjectPool}
	 */
	@Test
	public void testWithMaintenance() throws IllegalAccessException, InterruptedException
	{
		final long SLEEP_TIME = 1377; // must be longer than pool maintenance window.
		
		final ConcurrentLinkedQueue<AtomicInteger> allCreatedPoolObjects = new ConcurrentLinkedQueue<>();
		final AtomicInteger instanceCounter = new AtomicInteger(0);
		
		SimpleObjectPool<AtomicInteger> pool = new SimpleObjectPool<>("test atomic ints", 4, 10, 1, 
			() -> {
				int count = INCR_IDENTITY * instanceCounter.incrementAndGet(); // skip zero
				AtomicInteger inst = new AtomicInteger(count);
				allCreatedPoolObjects.add(inst);
				return inst;
			});
		Latch latch = new Latch(false);
		
		ConcurrentLinkedQueue<PoolObjectWrapper<AtomicInteger>> queue = TypeUtil.coerceForceNonnull(poolQueueField.get(pool));
		ScheduledExecutorService scheduler = TypeUtil.coerceForceNonnull(schedulerField.get(pool));
		
		assert queue.size() == 4 : queue;
		assert scheduler != null;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		
		// Borrow two objects.
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		// Wait for pool maintenance
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 4 : queue;
		assert allCreatedPoolObjects.size() == 6 : allCreatedPoolObjects;
		assert instanceCounter.get() == 6 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_000_001, 2_000_001, 3_000_000, 4_000_000, 5_000_000, 6_000_000);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 6 : queue;
		assert allCreatedPoolObjects.size() == 6 : allCreatedPoolObjects;
		assert instanceCounter.get() == 6 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_000_000, 4_000_000, 5_000_000, 6_000_000);
		
		// Borrow 2 from pool (should not create new ones)
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 4 : queue;
		assert allCreatedPoolObjects.size() == 6 : allCreatedPoolObjects;
		assert instanceCounter.get() == 6 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_000_001, 4_000_001, 5_000_000, 6_000_000);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 6 : queue;
		assert allCreatedPoolObjects.size() == 6 : allCreatedPoolObjects;
		assert instanceCounter.get() == 6 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_001_001, 4_001_001, 5_000_000, 6_000_000);
		
		// Borrow 8 from pool (should create 2 new ones)
		latch.close();
		for (int i = 0; i < 8; i++)
			runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance (should create 4 more new ones)
		assert queue.size() == 4 : queue;
		assert allCreatedPoolObjects.size() == 12 : allCreatedPoolObjects;
		assert instanceCounter.get() == 12 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_001_002, 2_001_002, 3_001_002,  4_001_002,  5_000_001,  6_000_001,
			7_000_001, 8_000_001, 9_000_000, 10_000_000, 11_000_000, 12_000_000);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance (should discard 2 extras)
		assert queue.size() == 10 : queue;
		assert allCreatedPoolObjects.size() == 12 : allCreatedPoolObjects;
		assert instanceCounter.get() == 12 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_002_002, 2_002_002, 3_002_002,  4_002_002,  5_001_001,  6_001_001,
			7_001_001, 8_001_001, 9_000_000, 10_000_000, 11_000_000, 12_000_000);
	}
	
	/**
	 * Tests {@link SimpleObjectPool}
	 */
	@Test
	public void testNoMaintenance() throws IllegalAccessException, InterruptedException
	{
		final long SLEEP_TIME = 1377; // must be longer than pool maintenance window.
		
		final ConcurrentLinkedQueue<AtomicInteger> allCreatedPoolObjects = new ConcurrentLinkedQueue<>();
		final AtomicInteger instanceCounter = new AtomicInteger(0);
		
		SimpleObjectPool<AtomicInteger> pool = new SimpleObjectPool<>("test atomic ints (no maint)", 4, 0, 0, 
			() -> {
				int count = INCR_IDENTITY * instanceCounter.incrementAndGet(); // skip zero
				AtomicInteger inst = new AtomicInteger(count);
				allCreatedPoolObjects.add(inst);
				return inst;
			});
		Latch latch = new Latch(false);
		
		ConcurrentLinkedQueue<PoolObjectWrapper<AtomicInteger>> queue = TypeUtil.coerceForceNonnull(poolQueueField.get(pool));
		ScheduledExecutorService scheduler = TypeUtil.coerceNullable(schedulerField.get(pool));
		
		assert queue.size() == 4 : queue;
		assert scheduler == null;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		
		// Borrow two objects (should not create more).
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		// Wait for pool maintenance
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 2 : queue;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_000_001, 2_000_001, 3_000_000, 4_000_000);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 4 : queue;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_000_000, 4_000_000);
		
		// Borrow 2 from pool (should not create new ones)
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 2 : queue;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_000_001, 4_000_001);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 4 : queue;
		assert allCreatedPoolObjects.size() == 4 : allCreatedPoolObjects;
		assert instanceCounter.get() == 4 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001, 3_001_001, 4_001_001);
		
		// Borrow 8 from pool (should create 4 new ones)
		latch.close();
		for (int i = 0; i < 8; i++)
			runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance
		assert queue.size() == 0 : queue;
		assert allCreatedPoolObjects.size() == 8 : allCreatedPoolObjects;
		assert instanceCounter.get() == 8 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_001_002, 2_001_002, 3_001_002,  4_001_002,  5_000_001,  6_000_001,
			7_000_001, 8_000_001);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance
		assert queue.size() == 8 : queue;
		assert allCreatedPoolObjects.size() == 8 : allCreatedPoolObjects;
		assert instanceCounter.get() == 8 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_002_002, 2_002_002, 3_002_002,  4_002_002,  5_001_001,  6_001_001,
			7_001_001, 8_001_001);
	}
	
	/**
	 * Tests {@link SimpleObjectPool}
	 */
	@Test
	public void testNoMaintenanceNoInitialSize() throws IllegalAccessException, InterruptedException
	{
		final long SLEEP_TIME = 1377; // must be longer than pool maintenance window.
		
		final ConcurrentLinkedQueue<AtomicInteger> allCreatedPoolObjects = new ConcurrentLinkedQueue<>();
		final AtomicInteger instanceCounter = new AtomicInteger(0);
		
		SimpleObjectPool<AtomicInteger> pool = new SimpleObjectPool<>("test atomic ints (no maint, no initial)", 0, 0, 0, 
			() -> {
				int count = INCR_IDENTITY * instanceCounter.incrementAndGet(); // skip zero
				AtomicInteger inst = new AtomicInteger(count);
				allCreatedPoolObjects.add(inst);
				return inst;
			});
		Latch latch = new Latch(false);
		
		ConcurrentLinkedQueue<PoolObjectWrapper<AtomicInteger>> queue = TypeUtil.coerceForceNonnull(poolQueueField.get(pool));
		ScheduledExecutorService scheduler = TypeUtil.coerceNullable(schedulerField.get(pool));
		
		assert queue.size() == 0 : queue;
		assert scheduler == null;
		assert allCreatedPoolObjects.size() == 0 : allCreatedPoolObjects;
		assert instanceCounter.get() == 0 : instanceCounter.get();
		
		// Borrow two objects (should not create more).
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		// Wait for pool maintenance
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 0 : queue;
		assert allCreatedPoolObjects.size() == 2 : allCreatedPoolObjects;
		assert instanceCounter.get() == 2 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_000_001, 2_000_001);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 2 : queue;
		assert allCreatedPoolObjects.size() == 2 : allCreatedPoolObjects;
		assert instanceCounter.get() == 2 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_001, 2_001_001);
		
		// Borrow 2 from pool (should not create new ones)
		latch.close();
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 0 : queue;
		assert allCreatedPoolObjects.size() == 2 : allCreatedPoolObjects;
		assert instanceCounter.get() == 2 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_001_002, 2_001_002);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME);
		assert queue.size() == 2 : queue;
		assert allCreatedPoolObjects.size() == 2 : allCreatedPoolObjects;
		assert instanceCounter.get() == 2 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 1_002_002, 2_002_002);
		
		// Borrow 8 from pool (should create 6 new ones)
		latch.close();
		for (int i = 0; i < 8; i++)
			runWorker(pool, latch);
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance
		assert queue.size() == 0 : queue;
		assert allCreatedPoolObjects.size() == 8 : allCreatedPoolObjects;
		assert instanceCounter.get() == 8 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_002_003, 2_002_003, 3_000_001,  4_000_001,  5_000_001,  6_000_001,
			7_000_001, 8_000_001);
		
		latch.open(); // Release resources
		
		Thread.sleep(SLEEP_TIME); // Wait for pool maintenance
		assert queue.size() == 8 : queue;
		assert allCreatedPoolObjects.size() == 8 : allCreatedPoolObjects;
		assert instanceCounter.get() == 8 : instanceCounter.get();
		checkList(allCreatedPoolObjects, 
			1_003_003, 2_003_003, 3_001_001,  4_001_001,  5_001_001,  6_001_001,
			7_001_001, 8_001_001);
	}	
	
	/**
	 * Tests maintenance thread timing.
	 */
	@Test
	public void testMaintenanceTiming() throws IllegalAccessException, InterruptedException
	{
		SimpleObjectPool<AtomicInteger> pool = new SimpleObjectPool<>("test maintenance", 4, 10, 3, 
			() -> new AtomicInteger());
		Latch latch = new Latch(false);
		
		ConcurrentLinkedQueue<PoolObjectWrapper<AtomicInteger>> queue = TypeUtil.coerceForceNonnull(poolQueueField.get(pool));
		ScheduledExecutorService scheduler = TypeUtil.coerceForceNonnull(schedulerField.get(pool));
		
		assert scheduler != null : scheduler;
		assert queue.size() == 4 : queue;
		
		// Borrow two
		runWorker(pool, latch);
		runWorker(pool, latch);
		
		// Wait a bit (less than maintenance period)
		Thread.sleep(1000);
		assert queue.size() == 2 : queue;
		
		// Wait longer than maintenance period
		Thread.sleep(3000);
		assert queue.size() == 4 : queue; // Must've grown back to minimum.
		
		latch.open(); // release waiting threads.
	}
	
	/**
	 * Checks list for exact match with given values (order is irrelevant)
	 * asserts if something is wrong
	 */
	protected void checkList(ConcurrentLinkedQueue<AtomicInteger> argList, int... values)
	{
		ArrayList<Integer> list = new ArrayList<>(argList.size());
		for (@Nonnull AtomicInteger ai : argList)
			list.add(ai.get());
		ArrayList<Integer> remaining = new ArrayList<>(list);		
		
		for (int v : values)
			assert remaining.remove(Integer.valueOf(v)) : "Missing [" + v + "] in " + list + " vs " + Arrays.toString(values);

		assert remaining.size() == 0 : "Extra items [" + remaining + "] in " + list + " vs " + Arrays.toString(values);
	}
	
	/**
	 * Creates&starts a worker thread for testing pool
	 */
	protected Thread runWorker(SimpleObjectPool<AtomicInteger> pool, Latch latch)
	{
		Thread thread = new Thread("SimpleObjectPool-worker")
		{
			/* (non-Javadoc)
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run()
			{
				try (@Nonnull PoolObjectWrapper<@Nonnull AtomicInteger> w = pool.borrow())
				{
					@Nonnull AtomicInteger item = w.getObject();
					item.addAndGet(INCR_STARTED);
					
					latch.awaitNoException();
					
					item.addAndGet(INCR_FINISHED);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
		
		return thread;
	}
	
	/**
	 * Tests what happens if factory fails.
	 */
	@Test
	public void testFactoryFailures() throws InterruptedException, IllegalAccessException
	{
		final long SLEEP_TIME = 1377; // must be longer than pool maintenance window.
		
		final AtomicBoolean failOnFactory = new AtomicBoolean(true);
		
		{
			// Test factory failing in constructor
			try
			{
				@SuppressWarnings("unused")
				SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("fail1", 4, 10, 1, () ->
					{
						if (failOnFactory.get())
							throw new IllegalStateException("Factory fail on purpose");
						
						return "ok";
					}
				);
				assert false;
			} catch (IllegalStateException e)
			{
				assert nn(e.getMessage()).contains("Factory fail on purpose") : e.getMessage();
			}
		}
		
		{
			// Test factory failing in borrow
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("fail2", 0, 10, 1, () ->
				{
					if (failOnFactory.get())
						throw new IllegalStateException("Factory fail on purpose");
					
					return "ok";
				}
			);
			try
			{
				try (@Nonnull PoolObjectWrapper<@Nonnull String> w = pool.borrow())
				{
					// nothing
				}
				assert false;
			} catch (IllegalStateException e)
			{
				assert nn(e.getMessage()).contains("Factory fail on purpose") : e.getMessage();
			}
		}
		
		
		{
			// Test factory failing in maintenance thread
			failOnFactory.set(false); // allow creation
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("fail3", 4, 10, 1, () ->
				{
					if (failOnFactory.get())
						throw new IllegalStateException("Factory fail on purpose");
					
					return "ok";
				}
			);
			ConcurrentLinkedQueue<PoolObjectWrapper<AtomicInteger>> queue = TypeUtil.coerceForceNonnull(poolQueueField.get(pool));
			
			// Borrow a resource so that maintenance needs to create another one.
			failOnFactory.set(true);
			pool.borrow();
			
			Thread.sleep(SLEEP_TIME);
			assert queue.size() == 3 : queue; // Maintenance shouldn't have been able to create a new instance
			failOnFactory.set(false); // Now allow creation
			
			Thread.sleep(SLEEP_TIME);
			assert queue.size() == 4 : queue; // Now maintenance should've been able to create new instance
		}
	}
	
	/**
	 * Shutdown test.
	 */
	@Test
	public void testShutdown() throws IllegalAccessException, InterruptedException
	{
		SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("test-shutdown", 4, 10, 10, () -> "object");
		ScheduledExecutorService scheduler = TypeUtil.coerceNullable(schedulerField.get(pool));
		
		assert scheduler != null;
		assert !scheduler.isShutdown();
		assert !scheduler.isTerminated();
		
		pool.shutdown();
		assert scheduler.isShutdown();
		Thread.sleep(200); // Minor sleep to let 'terminated' flag update
		assert scheduler.isTerminated();
	}
	
	/**
	 * Constructor arg validation tests.
	 */
	@Test
	public void testConstructorArgs()
	{
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>(fakeNonNull(), 4, 10, 10, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("poolName must not be null") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("", 4, 10, 10, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("poolName must not be empty") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("name", -1, 10, 10, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("minIdle must be positive or 0") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("name", 0, -1, 10, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("maxIdle must be positive or 0") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("name", 0, 1, -1, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("validationIntervalSeconds must be positive or 0") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("name", 0, 0, 1, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("When maxIdle is 0, validationIntervalSeconds must be 0 too") : e.getMessage();
		}
		
		try
		{
			@SuppressWarnings("unused")
			SimpleObjectPool<@Nonnull String> pool = new SimpleObjectPool<String>("name", 0, 1, 0, () -> "object");
			assert false;
		} catch (IllegalArgumentException e)
		{
			assert nn(e.getMessage()).contains("When validationIntervalSeconds is 0, maxIdle must be 0 too") : e.getMessage();
		}
	}
}
