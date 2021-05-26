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

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer;

import io.github.solf.extra2.collection.TreeMapExt;

/**
 * A version of Kryo's {@link TreeMapSerializer} that is able to handle
 * {@link TreeMapExt}.
 *
 * @author Sergey Olefir
 */
public class KryoTreeMapExtSerializer extends TreeMapSerializer
{

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer#create(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected Map create(Kryo kryo, Input input, Class<Map> type)
	{
		return instantiate(type, (Comparator<?>)kryo.readClassAndObject(input));
	}

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer#createCopy(com.esotericsoftware.kryo.Kryo, java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected Map createCopy(Kryo kryo, Map original)
	{
		return instantiate(original.getClass(), ((TreeMap)original).comparator());
	}
	
	
	/**
	 * Instantiates required type based on given type and comparator.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected TreeMap<?, ?> instantiate(Class type, Comparator<?> comparator)
	{
		if (type.isAssignableFrom(TreeMapExt.class))
			return new TreeMapExt(comparator);
		
		return new TreeMap(comparator);
	}
}
