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
package io.github.solf.extra2.util;

/**
 * Utility for helping with managing Sonar / SonarQube code scan issues.
 *
 * @author Sergey Olefir
 */
//@NonNullByDefault // not-type-ized intentionally so types can be anything.
public class SonarUtil
{
	/**
	 * This method can be used when some value needs to be 'un-tainted' in Sonar
	 * code analysis.
	 * <p>
	 * All this does is return the original value passed. However since Sonar
	 * 'doesn't know' what this method does, it will consider returned value
	 * as not-tainted.
	 * <p>
	 * Sonar does taint code analysis where any values that are user-entered
	 * are marked as 'tainted' and this taint propagates to other places/variables
	 * they are used in -- this is in order to detect when un-sanitized user-entered
	 * values are used somewhere where they shouldn't -- such as in SQL string
	 * (SQL injection attacks).
	 * <p>
	 * Unfortunately Sonar doesn't provide explicit mechanism to mark values
	 * 'un-tainted', so this method is added to provide this kind of support,
	 * e.g.:
	 * <p>
	 * this.loginToken = SonarUtil.untaint(userProvidedToken);
	 * <p>
	 * One example where Sonar doesn't notice that value is 'safe' is when
	 * the value is first used to e.g. do database lookup (such as with
	 * loginToken) -- if something is found, then we are sure that token is valid.
	 * However Sonar will still complain if original token is used somewhere, 
	 * such as outputted to HTML or something.
	 */
	public <T> T untaint(T taintedValue)
	{
		return taintedValue;
	}
}
