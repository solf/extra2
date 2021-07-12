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

import static io.github.solf.extra2.util.NullUtil.nnChecked;
import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

import io.github.solf.extra2.testutil.TestUtil;
import io.github.solf.extra2.testutil.TestUtil.AsyncTestRunner;
import lombok.Getter;

/**
 * Tests for {@link TestUtil}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestTestUtil
{
	/**
	 * Test value class.
	 */
	private static class TestValue
	{
		@Getter
		private int value;
		
		/**
		 * Constructor.
		 */
		public TestValue(int value)
		{
			this.value = value;
		}
	}
	
	/**
	 * Exception subclass that cannot be de-serialized -- to test exception cloning etc.
	 */
	private static class TestNotSerializableException extends Exception
	{

		/**
		 * 
		 */
		@SuppressWarnings("unused")
		public TestNotSerializableException()
		{
			super();
		}

		/**
		 * @param message
		 * @param cause
		 * @param enableSuppression
		 * @param writableStackTrace
		 */
		@SuppressWarnings("unused")
		public TestNotSerializableException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace)
		{
			super(message, cause, enableSuppression, writableStackTrace);
		}

		/**
		 * @param message
		 * @param cause
		 */
		public TestNotSerializableException(String message, Throwable cause)
		{
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public TestNotSerializableException(String message)
		{
			super(message);
		}

		/**
		 * @param cause
		 */
		@SuppressWarnings("unused")
		public TestNotSerializableException(Throwable cause)
		{
			super(cause);
		}
		
		/**
		 * Custom de-serialization method.
		 */
		private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream)
		{
			throw new IllegalStateException("de-clone failed");
		}
	}
	
	/**
	 * Testing inaccessible method invocation.
	 */
	@Test
	public void testMethodInvocation()
	{
		HashMap<String, String> map = new HashMap<>();
		map.put("key", "value");
		
		int hash = TestUtil.invokeInaccessibleMethod(HashMap.class, "hash", null, Object.class, "key");
		Entry<String,String> entry = TestUtil.invokeInaccessibleMethod(HashMap.class, "getNode", map, int.class, hash, Object.class, "key");

		assertEquals(entry.getKey(), "key");
		assertEquals(entry.getValue(), "value");
	}
	
	/**
	 * Testing inaccessible method invocation.
	 */
	@Test
	public void testMethodInvocationWrongArgCount()
	{
		HashMap<String, String> map = new HashMap<>();
		map.put("key", "value");
		
		int hash = TestUtil.invokeInaccessibleMethod(HashMap.class, "hash", null, Object.class, "key");
		try
		{
			@SuppressWarnings("unused") Entry<String,String> entry = 
				TestUtil.invokeInaccessibleMethod(HashMap.class, "getNode", map, int.class, hash, Object.class);
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.toString().contains("Args must be even-length array of type + value pairs") : e;
		}
	}
	
	/**
	 * Test inaccessible fields read/write.
	 */
	@Test
	public void testInaccessibleFieldAccess()
	{
		TestValue value = new TestValue(3);
		
		assertEquals((int)TestUtil.getInaccessibleFieldValue(TestValue.class, "value", value), 3);
		TestUtil.setInaccessibleFieldValue(TestValue.class, "value", value, 5);
		assertEquals((int)TestUtil.getInaccessibleFieldValue(TestValue.class, "value", value), 5);
	}
	
	/** self-documenting */
	@Test
	public void testCallWithTimeLimitTimeout() throws Exception
	{
		long start = System.currentTimeMillis();
		try
		{
			TestUtil.callWithTimeLimit(2000, () -> {Thread.sleep(3000);return null;});
			assert false;
		} catch (ExecutionException e)
		{
			assert e.toString().contains("sleep interrupted") : e;
		}
		
		long duration = System.currentTimeMillis() - start;
		assert duration > 1000 : duration;
		assert duration < 3000 : duration;
	}
	
	/** self-documenting */
	@Test
	public void testCallWithTimeLimitNonInterruptible() throws Exception
	{
		long start = System.currentTimeMillis();
		try
		{
			TestUtil.callWithTimeLimit(2000, () -> {return new BufferedReader(new InputStreamReader(System.in)).readLine();});
			assert false;
		} catch (TimeoutException e)
		{
			assert e.toString().contains("or even after interrupt") : e;
		}
		
		long duration = System.currentTimeMillis() - start;
		assert duration > 3000 : duration;
		assert duration < 5000 : duration;
	}
	
	/** self-documenting */
	@Test
	public void testCallWithTimeLimitOk() throws Exception
	{
		long start = System.currentTimeMillis();
		String result = TestUtil.callWithTimeLimit(3000, () -> {Thread.sleep(2000);return "done";});
		assertEquals(result, "done");
		
		long duration = System.currentTimeMillis() - start;
		assert duration > 1000 : duration;
		assert duration < 3000 : duration;
	}
	
	/** self-documenting */
	@Test
	public void testRunWithTimeLimitOk() throws Exception
	{
		long start = System.currentTimeMillis();
		TestUtil.runWithTimeLimit(3000, () -> {Thread.sleep(2000);});
		
		long duration = System.currentTimeMillis() - start;
		assert duration > 1000 : duration;
		assert duration < 3000 : duration;
	}
	
	
	/** self-documenting */
	@Test
	public void testCallAsynchronouslyTimeoutWithoutInterrupt() throws Exception
	{
		long start = System.currentTimeMillis();
		AsyncTestRunner<String> task = TestUtil.callAsynchronously(() -> {Thread.sleep(3000); return "done";});
		
		try
		{
			task.getResult(2000, false);
			assert false;
		} catch (TimeoutException e)
		{
			assert e.toString().contains("Asyncronous execution didn't finish") : e;
		}
		
		// Retry -- should complete
		String result = task.getResult(2000, false);
		assertEquals(result, "done");
		
		long duration = System.currentTimeMillis() - start;
		assert duration > 2900 : duration;
		assert duration < 5000 : duration;
	}
	
	/** self-documenting */
	@Test
	public void testSimpleExceptionClone()
	{
		Exception toClone = new Exception("to clone");
		
		Exception clone = TestUtil.cloneThrowable(toClone);
		
		assert clone != toClone;
		assertEquals(clone.toString(), toClone.toString());
		assertEquals(clone.getStackTrace()[0].toString(), toClone.getStackTrace()[0].toString());
	}
	
	/** self-documenting */
	@Test
	public void testSimpleExceptionCloneFail()
	{
		Exception toClone = new TestNotSerializableException("to clone");
		
		try
		{
			@SuppressWarnings("unused") Exception clone = TestUtil.cloneThrowable(toClone);
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.toString().contains("Clone failed for:") : e;
			assert nnChecked(e.getCause()).toString().contains("de-clone failed") : e;
		}
	}
	
	/** self-documenting */
	@Test
	public void testSimpleExceptionCloneNoFail()
	{
		Exception toClone = new Exception("to clone");
		
		Exception clone = TestUtil.cloneThrowableNoFailFallbackToOriginal(toClone);
		
		assert clone != toClone;
		assertEquals(clone.toString(), toClone.toString());
		assertEquals(clone.getStackTrace()[0].toString(), toClone.getStackTrace()[0].toString());
	}
	
	/** self-documenting */
	@Test
	public void testSimpleExceptionCloneNoFailWithFail()
	{
		Exception toClone = new TestNotSerializableException("to clone");
		
		Exception clone = TestUtil.cloneThrowableNoFailFallbackToOriginal(toClone);
		
		assertEquals(clone, toClone);
	}

	/** self-documenting */
	@Test
	public void testNestedExceptionClone()
	{
		IOException nested = generateException();
		
		Exception toClone = new Exception("to clone", nested);
		
		Exception clone = TestUtil.cloneThrowable(toClone);
		
		assert clone != toClone;
		assertEquals(clone.toString(), toClone.toString());
		assertEquals(clone.getStackTrace()[0].toString(), toClone.getStackTrace()[0].toString());
		assert clone.getCause() != toClone.getCause();
		assertEquals(nnChecked(clone.getCause()).toString(), nnChecked(toClone.getCause()).toString());
		assertEquals(nnChecked(clone.getCause()).getStackTrace()[0].toString(), nnChecked(toClone.getCause()).getStackTrace()[0].toString());
	}
	
	/**
	 * Simple method to generate exception.
	 */
	private IOException generateException()
	{
		return new IOException("generated");
	}

	/** self-documenting */
	@Test
	public void testNestedExceptionCloneAddCurrentStack()
	{
		IOException nested = generateException();
		
		Exception toClone = new Exception("to clone", nested);
		
		Exception clone = TestUtil.cloneThrowableAddCurrentStack(toClone);
		
		assert clone != toClone;
		assertEquals(clone.toString(), toClone.toString()); // top level description should match
		// Top level should contain current stack
		assert clone.getStackTrace()[0].toString().contains("cloneThrowableAddCurrentStack") : clone;
		
		// Cause should be the original exception
		assert clone.getCause() == toClone : clone;
	}

	/** self-documenting */
	@Test
	public void testNestedExceptionCloneAddCurrentStackNoFailWithFail()
	{
		IOException nested = generateException();
		
		TestNotSerializableException toClone = new TestNotSerializableException("to clone", nested);
		
		TestNotSerializableException clone = TestUtil.cloneThrowableAddCurrentStackNoFailFallbackToOriginal(toClone);
		
		assert clone == toClone;
	}
}
