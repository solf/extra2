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
package io.github.solf.extra2.objectgraph;

/**
 * Parent for test object.
 *
 * @author Sergey Olefir
 */
//@ParametersAreNonnullByDefault // No annotations because they make writing test code annoying.
/*package*/ abstract class OGTOParent
{
	public transient String transientName = null; // For testing field hiding & transients
	
	private OGTestObject[] array = null; // For testing field 'hiding'
	
	/**
	 * Gets array.
	 */
	public OGTestObject[] getParentArray()
	{
		return array;
	}
	
	/**
	 * Sets array.
	 */
	public OGTOParent setParentArray(OGTestObject[] array)
	{
		this.array = array;
		
		return this;
	}

}
