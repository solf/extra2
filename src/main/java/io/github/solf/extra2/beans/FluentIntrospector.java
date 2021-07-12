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
package io.github.solf.extra2.beans;

import static io.github.solf.extra2.util.NullUtil.nn;
import static io.github.solf.extra2.util.NullUtil.nullable;
import static java.util.Locale.ENGLISH;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.collection.ConcurrentWeakIdentityHashMap;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * {@link Introspector} version that supports fluent setters (those that return
 * 'this' instead of void).
 * <p>
 * This is done by wrapping {@link BeanInfo} from {@link Introspector} and
 * overriding property set methods where appropriate. The result is then
 * cached in the weak map.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class FluentIntrospector
{
	/**
	 * Weak cache of {@link BeanInfo} per class. 
	 */
	private static final ConcurrentWeakIdentityHashMap<Class<?>, FluentIntrospectorBeanInfo> cache = new ConcurrentWeakIdentityHashMap<>();
	
	/**
	 * Delegates all {@link BeanInfo} operations to the given delegate.
	 */
	@RequiredArgsConstructor
	public static class FluentIntrospectorBeanInfoDelegator implements BeanInfo
	{
		@Delegate
		private final BeanInfo delegate;
	}
	
	/**
	 * Class that delegates all {@link BeanInfo} operations to the given delegate
	 * EXCEPT for bean properties (which are specified separately).
	 */
	public static class FluentIntrospectorBeanInfo extends FluentIntrospectorBeanInfoDelegator
	{
		/**
		 * Properties array (overriddes whatever there is in delegate).
		 */
		private final PropertyDescriptor @Nullable [] properties;
		
		/**
		 * Constructor.
		 */
		public FluentIntrospectorBeanInfo(BeanInfo delegate, PropertyDescriptor @Nullable [] properties)
		{
			super(delegate);
			this.properties = properties;
		}

		/* (non-Javadoc)
		 * @see FluentIntrospectorBeanInfoDelegator#getPropertyDescriptors() 
		 */
		@SuppressWarnings("null") // something doesn't seem to work with generated delegate
		@Override
		public PropertyDescriptor @Nullable [] getPropertyDescriptors()
		{
			return properties;
		}
	}
	
	/**
     * Introspect on a Java Bean and learn about all its properties, exposed
     * methods, and events.
     * <p>
     * If the BeanInfo class for a Java Bean has been previously Introspected
     * then the BeanInfo class is retrieved from the BeanInfo cache.
     * <p>
     * Unlike {@link Introspector} this implementation allows fluent setters
     * (specifically it'll add write methods to data returned from {@link Introspector}
     * where applicable).
     *
     * @param beanClass  The bean class to be analyzed.
     * @return  A BeanInfo object describing the target bean.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
	 */
	public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException
	{
		FluentIntrospectorBeanInfo result = cache.get(beanClass);
		if (result != null)
			return result;
		
		BeanInfo baseBeanInfo = Introspector.getBeanInfo(beanClass);
		
		PropertyDescriptor[] basePds = baseBeanInfo.getPropertyDescriptors();
		if (basePds == null)
		{
			result = new FluentIntrospectorBeanInfo(baseBeanInfo, null);
		}
		else
		{
			PropertyDescriptor[] newPds = new @Nonnull PropertyDescriptor[basePds.length];
			
			for (int i = 0; i < basePds.length; i++)
			{
				PropertyDescriptor bpd = basePds[i];
				PropertyDescriptor npd;
				if (bpd.getWriteMethod() != null)
				{
					npd = bpd;
				}
				else
				{
					try
					{
						Method writeMethod = beanClass.getMethod("set" + capitalize(nn(bpd.getName())), bpd.getPropertyType());
						npd = new PropertyDescriptor(bpd.getName(), bpd.getReadMethod(), writeMethod);
					} catch (NoSuchMethodException e)
					{
						npd = bpd; // No write method, just use original property descriptor
					}
				}
				
				newPds[i] = npd;
			}
			
			result = new FluentIntrospectorBeanInfo(baseBeanInfo, newPds);
		}
		
		cache.put(beanClass, result);
		
		return result;
	}
	

    /**
     * Returns a String which capitalizes the first letter of the string.
     * <p>
     * From Java NameGenerator#capitalize()
     */
    public static String capitalize(String name) {
    	if (nullable(name) == null)
    		throw new NullPointerException();
    	
        if (name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }
	
}
