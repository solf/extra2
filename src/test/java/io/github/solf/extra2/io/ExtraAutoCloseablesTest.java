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
package io.github.solf.extra2.io;

import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static io.github.solf.extra2.testutil.AssertExtra.assertPasses;
import static org.testng.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

/**
 * Tests for {@link AutoCloseables}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraAutoCloseablesTest
{
	/**
	 * Standard test.
	 */
	@SuppressWarnings("resource")
	@Test
	public void test()
	{
		{
			// No exceptions AutoCloseable test
			AtomicInteger c1 = new AtomicInteger(0);
			AtomicInteger c2 = new AtomicInteger(0);
			AtomicInteger c3 = new AtomicInteger(0);
			
			AutoCloseable ac1 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c1.incrementAndGet();
				}
			};
			AutoCloseable ac2 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c2.incrementAndGet();
				}
			};
			AutoCloseable ac3 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c3.incrementAndGet();
				}
			};
			
			assertPasses(() -> {
				try (AutoCloseable allC = AutoCloseables.of(ac1, ac2, ac3))
				{
					// empty
				}
			});
			
			assertEquals(c1.get(), 1);
			assertEquals(c2.get(), 1);
			assertEquals(c3.get(), 1);
		}

		{
			// With exceptions AutoCloseable test
			AtomicInteger c1 = new AtomicInteger(0);
			AtomicInteger c2 = new AtomicInteger(0);
			AtomicInteger c3 = new AtomicInteger(0);
			
			AutoCloseable ac1 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c1.incrementAndGet();
					throw new Exception("1");
				}
			};
			AutoCloseable ac2 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c2.incrementAndGet();
					throw new Exception("2");
				}
			};
			AutoCloseable ac3 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c3.incrementAndGet();
					throw new Exception("3");
				}
			};
			
			assertFailsWithSubstring(() -> {
				try (AutoCloseable allC = AutoCloseables.of(ac1, ac2, ac3))
				{
					// empty
				}
			}, "java.lang.Exception: 3");
			
			assertEquals(c1.get(), 1);
			assertEquals(c2.get(), 1);
			assertEquals(c3.get(), 1);
		}
		
		{
			// No exceptions AutoCloseableUnchecked test
			AtomicInteger c1 = new AtomicInteger(0);
			AtomicInteger c2 = new AtomicInteger(0);
			AtomicInteger c3 = new AtomicInteger(0);
			
			AutoCloseable ac1 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c1.incrementAndGet();
				}
			};
			AutoCloseable ac2 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c2.incrementAndGet();
				}
			};
			AutoCloseable ac3 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c3.incrementAndGet();
				}
			};
			
			try (AutoCloseableUnchecked allC = AutoCloseables.ofUnchecked(ac1, ac2, ac3))
			{
				// empty
			}
			
			assertEquals(c1.get(), 1);
			assertEquals(c2.get(), 1);
			assertEquals(c3.get(), 1);
		}

		{
			// With exceptions AutoCloseableUnchecked test
			AtomicInteger c1 = new AtomicInteger(0);
			AtomicInteger c2 = new AtomicInteger(0);
			AtomicInteger c3 = new AtomicInteger(0);
			
			AutoCloseable ac1 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c1.incrementAndGet();
					throw new Exception("21");
				}
			};
			AutoCloseable ac2 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c2.incrementAndGet();
					throw new Exception("22");
				}
			};
			AutoCloseable ac3 = new AutoCloseable()
			{
				@Override
				public void close()
					throws Exception
				{
					c3.incrementAndGet();
					throw new Exception("23");
				}
			};
			
			assertFailsWithSubstring(() -> {
				try (AutoCloseableUnchecked allC = AutoCloseables.ofUnchecked(ac1, ac2, ac3))
				{
					// empty
				}
			}, "java.lang.Exception: 23");
			
			assertEquals(c1.get(), 1);
			assertEquals(c2.get(), 1);
			assertEquals(c3.get(), 1);
		}
	}
}
