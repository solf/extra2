/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.kryo;

/**
 * Marker interface for ROOT classes (those that are given directly / received
 * directly from {@link KryoDB} -- i.e. only one instance will receive this
 * event) that want to be notified AFTER database upgrade occurs.
 * 
 * This is intended to allow for the need to do structural changes after 
 * per-entity changes are already carried out.
 * 
 * If implementing this interface, then class must provide an implementation
 * of method:
 * void kryoPostUpgrade(int fromVersion)
 * Method is not declared in interface since it's likely that you don't want to
 * make this method public.
 * 
 * NOTE: framework will invoke all versions of method for specific object/class
 * instance (i.e. if method is defined in parent and child, both parent and child 
 * versions will be invoked). If there are multiple method instances, then the
 * 'parent' one will be invoked before 'child' one.
 * 
 * NOTE2: invocation happens after everything was read from storage and initialized
 * AND after upgrade is finished / upgrade listeners (if any) have completed
 *
 * @author Sergey Olefir
 */
public interface KryoPostUpgradeListener
{
	// Nothing in marker interface.
}
