/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.kryo;

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
