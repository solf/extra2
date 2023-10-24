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

import static io.github.solf.extra2.util.NullUtil.isNull;
import static io.github.solf.extra2.util.NullUtil.nullable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.concurrent.WAExecutors;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple object pool implementation. Completely thread-safe.
 * <p>
 * Supports optional min/max size limits.
 * 
 * <p>
 * Sample pool creation:
 * <pre>
 * SimpleObjectPool<byte[]> pool = new SimpleObjectPool<>("byte array pool", 0, 4, 30, () -> new byte[2 * 1024 * 1024]);
 * </pre>
 * 
 * It is intended to be used with try-with-resources, i.e.:
 * <pre>
 * 		try (PoolObjectWrapper<byte[]> w = pool.borrow())
		{
			byte[] bytes = w.getObject();
			...
		}
 * </pre>
 * 
 */
@NonNullByDefault
@Slf4j
public class SimpleObjectPool<T>
{
	/**
	 * Internal queue for holding idle instances.
	 */
    private final ConcurrentLinkedQueue<PoolObjectWrapper<T>> pool = new ConcurrentLinkedQueue<>();

    /**
     * Executor service used for scheduling/running maintenance thread.
     */
    @Nullable
    private final ScheduledExecutorService executorService;
    
    /**
     * Future representing maintenance thread (for cancelation during shutdown)
     */
    @Nullable
    private final ScheduledFuture<?> executorFuture; 
    
    /**
     * Factory for creating new instances.
     */
    private final Supplier<T> factory;
    
    /**
     * Full pool name (for e.g. {@link #toString()} ) 
     */
    private final String fullPoolName;
    
    /**
     * AutoCloseable wrapper for pool objects -- close() returns wrapper to the pool.
     */
    @NonNullByDefault
    public static class PoolObjectWrapper<O> implements AutoCloseable
    {
    	/**
    	 * Parent pool.
    	 */
    	private final SimpleObjectPool<O> pool;
    	
    	/**
    	 * Underlying object.
    	 */
    	private final O object;
    	
    	
    	/**
		 * Constructor.
		 */
		public PoolObjectWrapper(SimpleObjectPool<O> pool, O object)
		{
			this.object = object;
			this.pool = pool;
		}

		/* (non-Javadoc)
		 * @see java.io.Closeable#close()
		 */
		@Override
		public void close()
		{
			pool.returnObject(this);
		}

		/**
		 * Gets actual object.
		 */
		public O getObject()
		{
			return object;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public @Nonnull String toString()
		{
			return PoolObjectWrapper.class.getSimpleName() + '[' + object + ']';
		}
    }

    /**
     * Creates the pool.
     * <p>
     * WARNING: using minIdle greater than zero is somewhat dangerous as maintenance
     * thread may attempt to create 'missing' instances indefinitely (if there are
     * not enough idle objects) -- which may be a problem in case of 'limited' 
     * and/or 'heavy' resources -- such as database connections.
     * <p>
     * ATTENTION: if maintenance thread encounters exception while trying to
     * create new instances, the error message and exception will be logged
     * via SLF4J
     * <p>
     * NOTE: Will fail if minIdle > 0 and factory fails
     *
     * @param poolName used as a part of toString and maintenance thread name
     * 		(SimpleObjectPool moniker is added automatically, so no need to add
     * 		that to pool name)
     * @param minIdle minimum number of objects residing in the pool, this many
     * 		objects are created initially and this number is maintained over time
     * 		unless maxIdle is zero
     * @param maxIdle maximum number of 'idle' objects residing in the pool;
     * 		a background thread takes care of ensuring that there are 'not too
     * 		many' and 'not too few' objects in the pool; set this value to 0
     * 		if you don't want to have background maintenance
     * @param validationIntervalSeconds time in seconds for periodical ensuring of 
     * 		minIdle / maxIdle conditions in a separate thread; if maxIdle is
     * 		set to 0, this parameter must also be 0
     */
    @SuppressWarnings("resource")
	public SimpleObjectPool(String poolName, final int minIdle, final int maxIdle, final int validationIntervalSeconds,
    	final Supplier<T> factory)
    	throws IllegalArgumentException
    {
    	if (nullable(poolName) == null)
    		throw new IllegalArgumentException("poolName must not be null");
    	if (poolName.isEmpty())
    		throw new IllegalArgumentException("poolName must not be empty");
    	
    	if (minIdle < 0)
    		throw new IllegalArgumentException("minIdle must be positive or 0");
    	if (maxIdle < 0)
    		throw new IllegalArgumentException("maxIdle must be positive or 0");
    	if (validationIntervalSeconds < 0)
    		throw new IllegalArgumentException("validationIntervalSeconds must be positive or 0");
    	if (maxIdle == 0)
    		if (validationIntervalSeconds != 0)
        		throw new IllegalArgumentException("When maxIdle is 0, validationIntervalSeconds must be 0 too.");
		if (validationIntervalSeconds == 0)
	    	if (maxIdle != 0)
	    		throw new IllegalArgumentException("When validationIntervalSeconds is 0, maxIdle must be 0 too.");
		
		this.factory = factory;
		
        // initialize pool
        for (int i = 0; i < minIdle; i++)
        {
        	PoolObjectWrapper<T> o = createObject(); // extract to var to make warning-compatible with old Eclipse
            pool.add(o);
        }
		

        // note: getClass().getSimpleName() returns empty string at least in some of the cases
        fullPoolName = SimpleObjectPool.class.getSimpleName() + '[' + poolName + ']' + '@' + Integer.toHexString(hashCode());
        
        if (maxIdle > 0)
        {
        	if (maxIdle < minIdle)
        		throw new IllegalArgumentException("maxIdle[" + maxIdle + "] < minIdle[" + minIdle + "]");
        	
	        // check pool conditions in a separate thread
	        // note: getClass().getSimpleName() returns empty string at least in some of the cases
	        String maintenanceThreadName = "pool-maintenance-" + fullPoolName;
	        executorService = WAExecutors.newSingleThreadScheduledExecutor(maintenanceThreadName, true);
	        executorFuture = executorService.scheduleWithFixedDelay(new Runnable()
	        {
				@Override
				public void run()
				{
					try
					{
						int size = pool.size();
						if( size < minIdle )
						{
							int sizeToBeAdded = minIdle - size;
							for( int i = 0; i < sizeToBeAdded; i++ )
								pool.add(createObject());
						}
						else if( size > maxIdle )
						{
							int sizeToBeRemoved = size - maxIdle;
							for( int i = 0; i < sizeToBeRemoved; i++ )
								pool.poll();
						}
					} catch (Exception e)
					{
						log.error(Thread.currentThread().getName() + " error while managing pool size: " + e, e);
					}
				}
	        }, validationIntervalSeconds, validationIntervalSeconds, TimeUnit.SECONDS);
        }
        else
        {
        	executorService = null;
        	executorFuture = null;
        }
    }

    /**
     * Gets the next free wrapped object from the pool. If the pool doesn't contain 
     * any objects, a new object will be created and returned.
     * <p>
     * This is intended to be used with try-with-resource paradigm, i.e.:
     * <pre>
 		try (PoolObjectWrapper<byte[]> w = pool.borrow())
		{
			byte[] bytes = w.getObject();
			...
		}
     * </pre>
     * 
     * NOTE: this method may fail if new instance creation is required and
     * underlying factory fails
     *
     * @return borrowed object wrapper
     */
    @SuppressWarnings("resource")
	public PoolObjectWrapper<T> borrow() 
    {
    	PoolObjectWrapper<T> object;
        if ((object = pool.poll()) == null) 
            object = createObject();

        return object;
    }

    /**
     * Returns object back to the pool.
     * Not public because client code is expected to use try-with-resource
     *
     * @param object object to be returned
     * 
     * @throws NullPointerException if argument is null
     */
    protected void returnObject(PoolObjectWrapper<T> object) throws NullPointerException 
    {
        if (isNull(object))
            throw new NullPointerException("Argument may not be null.");

        this.pool.offer(object);
    }

    /**
     * Shutdown this pool.
     * The only thing this method does is to shutdown background maintenance
     * thread (if any).
     */
    public void shutdown() {
    	if (executorFuture != null)
    		executorFuture.cancel(true);
        if (executorService != null) 
            executorService.shutdown();
    }

    /**
     * Creates a new wrapped object.
     *
     * @return T new object
     */
    protected PoolObjectWrapper<T> createObject()
    {
    	return new PoolObjectWrapper<T>(this, factory.get());
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public @Nonnull String toString()
	{
		return fullPoolName + '(' + pool.size() + ')';
	}
}
