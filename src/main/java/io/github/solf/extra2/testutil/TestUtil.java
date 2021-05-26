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
package io.github.solf.extra2.testutil;

import static io.github.solf.extra2.util.NullUtil.fakeNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;

import io.github.solf.extra2.concurrent.RunnableWithException;
import io.github.solf.extra2.io.BAOSInputStream;
import io.github.solf.extra2.util.TypeUtil;
import junit.framework.AssertionFailedError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Utilities for writing tests.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TestUtil
{
	/**
	 * Counter to give each async thread unique name.
	 */
	private static final AtomicInteger uniqueInstanceCounter = new AtomicInteger(0);
	
	/**
	 * Data for the asynchronous test runner.
	 * 
	 * @param <V> type of value returned by the asynchronous execution
	 */
	@RequiredArgsConstructor
	public static class AsyncTestRunner<V>
	{
		/**
		 * Thread actually executing the asynchronous operation.
		 */
		@Getter
		private final Thread thread;
		
		/**
		 * Communication queue used to report asynchronous operation result --
		 * it is expected that this queue will contain either Throwable that
		 * occurred in execution thread or an {@link AtomicReference} holding
		 * the result of asynchronous operation.
		 */
		@Getter
		private final BlockingQueue<Object> commQueue;

		/**
		 * tryInterruptIfNoResult is set to true when calling this method
		 * 
		 * @see #getResult(long, boolean)
		 */
		public V getResult(long timeLimit) throws TimeoutException, ExecutionException, InterruptedException
		{
			return getResult(timeLimit, true);
		}
		
		/**
		 * Attempts to retrieve asynchronous execution result for up to the given
		 * amount of time.
		 * <p>
		 * If tryInterruptIfNoResult is true and there's no result after the 
		 * given time limit, then an attempt is made to interrupt the asynchronous
		 * task -- so that it might be possible to ascertain where it is 'stuck'.
		 * There's an additional up to 2 seconds wait in this case to let
		 * asynchronous task finish interruption processing.
		 * 
		 * @return asynchronous execution result (if succeeded)
		 * 
		 * @throws TimeoutException if processing didn't finish in the given
		 * 		time limit
		 * @throws ExecutionException any throwable that might've occurred in the
		 * 		asynchronous thread is re-thrown for the calling thread
		 */
		public V getResult(long timeLimit, boolean tryInterruptIfNoResult) throws TimeoutException, ExecutionException, InterruptedException
		{
			
			Object result = commQueue.poll(timeLimit, TimeUnit.MILLISECONDS);
			if (result == null)
			{
				if (!tryInterruptIfNoResult)
					throw new TimeoutException("Asyncronous execution didn't finish after given time limit: " + timeLimit);
				
				thread.interrupt();
				result = commQueue.poll(2000, TimeUnit.MILLISECONDS);
			}
			
			if (result == null)
				throw new TimeoutException("Asyncronous execution didn't finish after given time limit [" + timeLimit + "] or even after interrupt.");
			
			if (result instanceof Throwable)
			{
				throw new ExecutionException("Throwable encountered in async processing thread [" + thread.getName() + "]: " + result, (Throwable)result);
			}
			else if (result instanceof AtomicReference)
			{
				return TypeUtil.coerceUnknown(((AtomicReference<?>)result).get());
			}
			else
				throw new AssertionFailedError("Unexpected object: " + result);
		}
	}
	
	/**
	 * Gets the value of a private (or otherwise inaccessible) field declared in some class from the given
	 * instance.
	 * <p>
	 * For convenience, checked reflection exceptions do not actually require
	 * checking (although they can be thrown by this method) -- via {@link SneakyThrows}
	 * 
	 * @throws IllegalAccessException if reflection fails
	 * @throws NoSuchFieldException if reflection fails
	 */
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public static <T, V> V getInaccessibleFieldValue(Class<T> clazz, String fieldName, T instance)
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (V)field.get(instance);
	}

	/**
	 * Sets the value of a private (or otherwise inaccessible) field declared in some class into 
	 * the given instance.
	 * <p>
	 * For convenience, checked reflection exceptions do not actually require
	 * checking (although they can be thrown by this method) -- via {@link SneakyThrows}
	 * 
	 * @throws IllegalAccessException if reflection fails
	 * @throws NoSuchFieldException if reflection fails
	 */
	@SneakyThrows
	public static <T, V> void setInaccessibleFieldValue(Class<T> clazz, String fieldName, T instance, V value)
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(instance, value);
	}


	/**
	 * Invokes private (or otherwise inaccessible) method declared in given class and returns the resulting
	 * value.
	 * <p>
	 * Args is an array of pairs [type]:[instance], e.g.:
	 * <p>
	 * int hash = TestUtil.invokeInaccessibleMethod(HashMap.class, "hash", null, Object.class, "key");
	 * Entry<String,String> entry = TestUtil.invokeInaccessibleMethod(HashMap.class, "getNode", map, int.class, hash, Object.class, "key");
	 * <p>
	 * For convenience, checked reflection exceptions do not actually require
	 * checking (although they can be thrown by this method) -- via {@link SneakyThrows}
	 * 
	 * @param instance may be null if invoking static method
	 * @param args pairs of Class + value, e.g.: [String.class, "asd"] -- those
	 * 		are used to find method and invoke it with these arguments
	 * 
	 * @throws IllegalAccessException if reflection fails
	 * @throws NoSuchFieldException if reflection fails
	 * @throws InvocationTargetException if reflection fails
	 * @throws IllegalArgumentException if e.g. Class<?> for argument type is null
	 */
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static <T, V> V invokeInaccessibleMethod(Class<T> clazz, String methodName, @Nullable T instance, @Nullable Object ... args)
	{
		if ((args.length % 2) != 0)
			throw new IllegalStateException("Args must be even-length array of type + value pairs, got: " + Arrays.toString(args));

		Class<?>[] argTypes = new @Nonnull Class<?>[args.length / 2];
		@Nullable Object[] argValues = new @Nullable Object[args.length / 2];
		for (int i = 0; i < argTypes.length; i++)
		{
			Class<?> c = (Class<?>)args[i * 2];
			if (c == null)
				throw new IllegalArgumentException("Null Class<?> given for argument type at position: " + i);
			argTypes[i] = c;
			argValues[i] = args[i * 2 + 1];
		}
		
		Method method = clazz.getDeclaredMethod(methodName, argTypes);
		method.setAccessible(true);
		return (V)method.invoke(instance, argValues);
	}
	
	/**
	 * Executes given code -- there's time limit on execution (as specified).
	 * <p>
	 * The code is executed in a separate thread; if code doesn't complete in the
	 * given time limit, then the executor thread is interrupted; after interruption
	 * there's an additional wait time (2000ms) to see if thread will exit.
	 * <p>
	 * If thread doesn't exit after interruption -- {@link TimeoutException} is
	 * thrown in the calling thread.
	 * 
	 * @throws TimeoutException if processing didn't finish in the given
	 * 		time limit
	 * @throws ExecutionException any throwable that might've occurred in the
	 * 		asynchronous thread is re-thrown for the calling thread
	 */
	public static void runWithTimeLimit(long timeLimit, final RunnableWithException runnable) throws TimeoutException, ExecutionException, InterruptedException
	{
		callWithTimeLimit(timeLimit, () -> {runnable.run(); return null;});
	}
	
	/**
	 * Executes given code and returns the result -- there's time limit on execution
	 * (as specified).
	 * <p>
	 * The code is executed in a separate thread; if code doesn't complete in the
	 * given time limit, then the executor thread is interrupted; after interruption
	 * there's an additional wait time (2000ms) to see if thread will exit.
	 * <p>
	 * If thread doesn't exit after interruption -- {@link TimeoutException} is
	 * thrown in the calling thread.
	 * 
	 * @return resulting value (if code completes normally)
	 * 
	 * @throws TimeoutException if processing didn't finish in the given
	 * 		time limit
	 * @throws ExecutionException any throwable that might've occurred in the
	 * 		asynchronous thread is re-thrown for the calling thread
	 */
	@ParametersAreNonnullByDefault({})
	public static <V> V callWithTimeLimit(long timeLimit, final @Nonnull Callable<V> callable) throws TimeoutException, ExecutionException, InterruptedException
	{
		AsyncTestRunner<V> runner = callAsynchronously(callable);
		
		return runner.getResult(timeLimit);
	}	
	
	/**
	 * Starts given code asynchronously in a daemon thread that contains 
	 * current thread name in its thread name -- so that e.g. it is possible
	 * to grep logs by the calling thread name and find records from the
	 * spawned threads.
	 * 
	 * @return {@link AsyncTestRunner} that can be used to monitor state and
	 * 		retrieve execution result
	 */
	@ParametersAreNonnullByDefault({})
	public static <V> @Nonnull AsyncTestRunner<V> callAsynchronously(final @Nonnull Callable<V> callable)
	{
		final @Nonnull ArrayBlockingQueue<@Nonnull Object> commQueue = new ArrayBlockingQueue<>(5);
		
		Thread thread = new Thread(() -> {
			try
			{
				V result = callable.call();
				commQueue.put(new AtomicReference<V>(result));
			} catch (Throwable t)
			{
				try
				{
					commQueue.put(t);
				} catch (InterruptedException e)
				{
					// nothing else to do
				}
			}
		}, "async-test-runner-" + Thread.currentThread().getName() + "-" + uniqueInstanceCounter.incrementAndGet());
		thread.setDaemon(true);
		
		thread.start();
		
		return new AsyncTestRunner<V>(thread, commQueue);
	}	
	
	/**
	 * Starts given code asynchronously in a daemon thread that contains 
	 * current thread name in its thread name -- so that e.g. it is possible
	 * to grep logs by the calling thread name and find records from the
	 * spawned threads.
	 * 
	 * @return {@link AsyncTestRunner} that can be used to monitor state and
	 * 		determine when thread has finished / any exceptions
	 */
	public static AsyncTestRunner<Void> runAsynchronously(final RunnableWithException runnable)
	{
		return callAsynchronously(() -> {runnable.run(); return fakeNonNull();});
	}
	
	/**
	 * Creates byte pipe (InputStream, OutputStream) that is very useful for 
	 * sending/receiving data from methods that deal with streams.
	 * <p>
	 * This pipe is guaranteed to react to Thread.interrupt() unlike blocking
	 * reads in some other cases in Java.
	 * <p>
	 * Additionally {@link RevivableInputStream} may be used to (temporarily)
	 * signal 'end of file' to the reader by using {@link RevivableInputStream#kill()}
	 * 
	 * @param bufferSize buffer size for internal buffers -- note that there are
	 * 		at least two different buffers + data that is in-flight, so the
	 * 		actual size of data that can be 'in the pipes' might be roughly
	 * 		3 times as much as this size
	 */
	@SuppressWarnings("resource")
	public static Pair<RevivableInputStream, RevivableOutputStream> createKillableBytePipe(int bufferSize) throws IOException
	{
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos, bufferSize);
		RevivableInputStream is = new RevivableInputStream(pis);
		RevivableOutputStream os = new RevivableOutputStream(pos, bufferSize);
		
		return new Pair<RevivableInputStream, RevivableOutputStream>(is, os);
	}
	
	/**
	 * Creates character pipe (Reader, Writer) that is very useful for 
	 * sending/receiving data from methods that deal with readers/writers.
	 * <p>
	 * This pipe is guaranteed to react to Thread.interrupt() unlike blocking
	 * reads in some other cases in Java.
	 * 
	 * @param bufferSize buffer size for the pipe -- i.e. how much data can be
	 * 		stored in the pipe before reading
	 */
	@SuppressWarnings("resource")
	public static Pair<PipedReader, PipedWriter> createCharPipe(int bufferSize) throws IOException
	{
		PipedWriter writer = new PipedWriter();
		PipedReader reader = new PipedReader(writer, bufferSize);
		
		return new Pair<>(reader, writer);
	}

	
	/**
	 * Factory for creating mock sockets.
	 * 
	 * @param bufferSize buffer size for internal buffers -- note that there are
	 * 		at least two different buffers + data that is in-flight, so the
	 * 		actual size of data that can be 'in the pipes' might be roughly
	 * 		3 times as much as this size
	 */
	public static MockSocketData createMockSocket(int bufferSize) throws IOException
	{
		return MockSocketData.createSocket(bufferSize);
	}
	
	/**
	 * Clones a {@link Throwable} (via serializing + deserializing).
	 * 
	 * @throws IllegalStateException if cloning fails
	 */
	public static <T extends Throwable> T cloneThrowable(T throwable) throws IllegalStateException
	{
		try
		{
			try (
				ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
				ObjectOutputStream oos = new ObjectOutputStream(bos);
			)
			{
				oos.writeObject(throwable);
				oos.flush();
				
				try (ObjectInputStream ois = new ObjectInputStream(new BAOSInputStream(bos)))
				{
					return TypeUtil.coerceForceNonnull(ois.readObject());
				}
			}
		} catch (Exception e)
		{
			throw new IllegalStateException("Clone failed for: " + throwable, e);
		}
	}
	
	/**
	 * Clones a {@link Throwable} (via serializing + deserializing).
	 * <p>
	 * In case of any failure -- the original exception is returned instead.
	 * Any {@link Throwable} is never thrown.
	 */
	public static <T extends Throwable> T cloneThrowableNoFailFallbackToOriginal(T exception)
	{
		try
		{
			return cloneThrowable(exception);
		} catch (Throwable e)
		{
			return exception;
		}
	}
	
	/**
	 * Clones given {@link Throwable} (to keep message and type), assigns it the
	 * current stack (so that it is visible where it originated from), then
	 * (via reflection hack) modifies cause to point to the original exception.
	 * <p>
	 * This helps to create an exception copy that 'looks like' the original
	 * exception, but has current stack trace and references original exception
	 * as the cause.
	 * 
	 * @throws IllegalAccessException if reflection fails
	 * @throws NoSuchFieldException if reflection fails
	 * @throws IllegalStateException if cloning fails
	 */
	public static <T extends Throwable> T cloneThrowableAddCurrentStack(T throwable) 
		throws IllegalStateException
	{
		T clone = cloneThrowable(throwable);
		
		// Setting cause to 'this' allows using initCause below
		TestUtil.setInaccessibleFieldValue(Throwable.class, "cause", clone, clone);
		
		clone.fillInStackTrace(); // Update stack trace in our clone.
		
		clone.initCause(throwable); // And reference original exception...
		
		return clone;
	}
	
	/**
	 * Clones given {@link Throwable} (to keep message and type), assigns it the
	 * current stack (so that it is visible where it originated from), then
	 * (via reflection hack) modifies cause to point to the original exception.
	 * <p>
	 * This helps to create an exception copy that 'looks like' the original
	 * exception, but has current stack trace and references original exception
	 * as the cause.
	 * <p>
	 * In case of any failure -- the original exception is returned instead.
	 * Any {@link Throwable} is never thrown.
	 */
	public static <T extends Throwable> T cloneThrowableAddCurrentStackNoFailFallbackToOriginal(T throwable) 
		throws IllegalStateException
	{
		try
		{
			return cloneThrowableAddCurrentStack(throwable);
		} catch (Throwable e)
		{
			return throwable;
		}
	}
}
