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
package io.github.solf.extra2.config;

import java.util.MissingResourceException;

import javax.annotation.NonNullByDefault;

/**
 * Represents a sectioned configuration, that is configuration that:
 * - has 'global' section where keys & values belong to no specific section;
 * - zero or more 'sections' with their specific keys & values.
 * 
 * Note that the same key name can be used in different sections and have
 * different values.
 * 
 * The logic of this class is very similar to .ini files on Windows.
 * 
 * NOTE: instances are created via {@link Configuration} class.
 * 
 * NOTE: all implementations must be thread-safe.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public interface SectionedConfiguration
{
	/**
	 * Gets global section -- that is section containing keys & values that belong to
	 * no specific section.
	 * 
	 * NOTE: global section is always available -- but it might be empty (contain
	 * no configuration keys/values).
	 */
	public FlatConfiguration getGlobalSection();
	
	/**
	 * Gets specific section.
	 * 
	 * @param sectionKey the key for the desired section
	 * @exception NullPointerException if <code>sectionKey</code> is <code>null</code>
	 * @exception MissingResourceException if no section for the given key can be found
	 */
	public FlatConfiguration getSection(String sectionKey) throws MissingResourceException, NullPointerException;
	
	/**
	 * Iterable for all section keys in this configuration (excluding global section).
	 */
	public Iterable<String> getAllSectionKeys();
}
