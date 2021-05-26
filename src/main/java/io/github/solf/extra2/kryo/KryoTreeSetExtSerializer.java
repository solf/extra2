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

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeSetSerializer;

import io.github.solf.extra2.collection.TreeSetExt;

/**
 * A version of Kryo's {@link TreeSetSerializer} that can handle {@link TreeSetExt}
 *
 * @author Sergey Olefir
 */
public class KryoTreeSetExtSerializer extends TreeSetSerializer
{

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeSetSerializer#create(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected TreeSet create(Kryo kryo, Input input, Class<Collection> type)
	{
		return instantiate(type, (Comparator<?>)kryo.readClassAndObject(input));
	}

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeSetSerializer#createCopy(com.esotericsoftware.kryo.Kryo, java.util.Collection)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected TreeSet createCopy(Kryo kryo, Collection original)
	{
		return instantiate(original.getClass(), ((TreeSet)original).comparator());
	}
	
	
	/**
	 * Instantiates required type based on given type and comparator.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected TreeSet<?> instantiate(Class type, Comparator<?> comparator)
	{
		if (type.isAssignableFrom(TreeSetExt.class))
			return new TreeSetExt(comparator);
		
		return new TreeSet(comparator);
	}
}
