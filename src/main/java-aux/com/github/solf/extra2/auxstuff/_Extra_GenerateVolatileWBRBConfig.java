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
package com.github.solf.extra2.auxstuff;

import static com.github.solf.extra2.util.NullUtil.nnChecked;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import org.codehaus.plexus.util.StringUtils;

import com.google.common.io.Files;
import com.github.solf.extra2.cache.wbrb.GeneratedVolatileWBRBConfig;
import com.github.solf.extra2.cache.wbrb.VolatileWBRBConfig;
import com.github.solf.extra2.file.FileEditor;

/**
 * Generates {@link GeneratedVolatileWBRBConfig} on top of which {@link VolatileWBRBConfig}
 * is built -- which is useful to be able to adjust
 * cache configuration at runtime (though at the performance cost of constant
 * volatile variable accesses).
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class _Extra_GenerateVolatileWBRBConfig
{
	/**
	 * Entry point.
	 */
	public static void main(String[] args) throws IOException
	{
		System.out.println("Generation running...");
		
		final String targetClassName = "GeneratedVolatileWBRBConfig";
		final String targetFileName = "src/main/java-generated/com/github/solf/extra2/cache/wbrb/" + targetClassName + ".java";
		
		nnChecked(new File(targetFileName).getParentFile()).mkdirs();
		
		File targetDir = nnChecked(new File(targetFileName).getParentFile());
		if ((!targetDir.exists()) || (!targetDir.isDirectory()))
			throw new IllegalStateException("Target dir must exist and be directory: " + targetDir);
		
		Files.copy(new File("src/main/java/com/github/solf/extra2/cache/wbrb/WBRBConfig.java"), new File(targetFileName));
		
		new FileEditor(targetFileName).replaceAllAndCommit("Configuration for \\{@link WriteBehindResyncInBackgroundCache\\}", "Configuration for {@link WriteBehindResyncInBackgroundCache} that allows changing the values by storing everything in volatile fields and providing setters");
		new FileEditor(targetFileName).replaceAllAndCommit("WBRBConfig", targetClassName);
		new FileEditor(targetFileName).replaceAllAndCommit("extends BaseDelegatingOptions", "extends WBRBConfig");
		new FileEditor(targetFileName).replaceAllAndCommit("private final", "private volatile");
		new FileEditor(targetFileName).replaceAllAndCommit("@ParametersAreNonnullByDefault", "@SuppressWarnings(\"unused\")\n@ParametersAreNonnullByDefault");
		
		{
			FileEditor fe = new FileEditor(targetFileName);
			
			fe.find(targetClassName); // find where class declaration starts
			fe.readLine(); // skip one line, e.g. opening brace
			
			final Pattern fieldPattern = Pattern.compile("^([ \t]+)private volatile (.*) ([A-Za-z]+)( =.*;|;) *");
			while (true)
			{
				String line = fe.readLine();
				if (line == null)
					throw new IllegalStateException("Unexpected EOF");
				if (line.contains(targetClassName))
					break; // done
				
				if (line.contains("private volatile"))
				{
					Matcher matcher = fieldPattern.matcher(line);
					if (!matcher.matches())
						throw new IllegalStateException("Failed to parse field line: " + line);
					
					final String spacer = matcher.group(1);
					final String type = matcher.group(2);
					final String fieldName = matcher.group(3);
//					final String initializer = matcher.group(4);
					
					String capitalizedFieldName = StringUtils.capitalise(fieldName);
					String getterName = (line.contains("boolean") ? "is" : "get") + capitalizedFieldName;
					
					// Field
					fe.writeLine(spacer + "private volatile " + type + " v_" + fieldName + " = super." + getterName + "();"); // replace field line
					
					// Getter
					fe.writeLine(spacer + "@Override");
					fe.writeLine(spacer + "public " + type + " " + getterName + "() {return v_" + fieldName + ";}");
					
					// Setter
					fe.writeLine(spacer + "public void set" + capitalizedFieldName + "(" + type + " newValue) {v_" + fieldName + " = newValue;}");
				}
				else if (line.trim().isEmpty())
				{
					// pass empty lines
				}
				else if (line.contains("/*") || line.contains("*/") || line.trim().startsWith("*"))
				{
					// pass comments
				}
				else
				{
					fe.removeReadLine(); // remove non-matching lines
				}
			}
			
			fe.spoolAndCommit();
		}
		
		{
			// Need to cut out everything after constructors (e.g. custom methods in config java file).
			FileEditor fe = new FileEditor(targetFileName);
			
			int validClassNameMatchesCount = 0;
			while (true)
			{
				try
				{
					fe.find(targetClassName);
					validClassNameMatchesCount++;
				} catch (RuntimeException e)
				{
					break; // no more matches
				}
			}
			
			fe = fe.recreate(); // reset editor (create new one)
			
			// Find where last constructor starts
			for (int i = 0; i < validClassNameMatchesCount; i++)
				fe.find(targetClassName);
			
			while(true)
			{
				String line = nnChecked(fe.readLine());
				if (line.trim().equals("}")) // closing brace of the constructor
					break;
			}
			
			fe.readLine();
			fe.writeLine("}"); // replace whatever after constructor with class-closing brace
			
			fe.truncateAndCommit(); // cut the rest of the file and save
		}
		
		System.out.println("Done.");
	}
}
