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
package io.github.solf.extra2.kryo;

/**
 * Marker interface for classes that want to be notified when database
 * upgrade occurs.
 * 
 * If implementing this interface, then class must provide an implementation
 * of method:
 * void kryoUpgrade(int fromVersion)
 * Method is not declared in interface since it's likely that you don't want to
 * make this method public.
 * 
 * NOTE: framework will invoke all versions of method for specific object/class
 * instance (i.e. if method is defined in parent and child, both parent and child 
 * versions will be invoked). If there are multiple method instances, then the
 * 'parent' one will be invoked before 'child' one.
 * 
 * NOTE2: invocation happens after everything was read from storage and initialized.
 *
 * @author Sergey Olefir
 */
public interface KryoUpgradeListener
{
	// Nothing in marker interface.
}
