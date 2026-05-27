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
package io.github.solf.extra2.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

/**
 * Tests for {@link DirManager}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class TestDirManager
{
	/**
	 * Test everything.
	 */
	@Test
	public void test() throws IOException
	{
		final String testDir = "target/test-data/dirmanager/";
		final File fTestDir = new File(testDir);
		
		FileUtils.deleteDirectory(testDir);
		
		fTestDir.mkdirs();
		
		try (FileOutputStream fos1 = new FileOutputStream(new File(fTestDir, "test")))
		{
			fos1.write(123);
			try (FileOutputStream fos2 = new FileOutputStream(new File(fTestDir, "test")))
			{
				fos2.write(05);
			}
		}
		
		System.out.println(new File(".").getAbsolutePath());
	}
}
