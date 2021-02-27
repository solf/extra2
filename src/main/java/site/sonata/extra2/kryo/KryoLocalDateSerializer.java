/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.kryo;

import org.joda.time.LocalDate;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo serialized for {@link LocalDate}
 *
 * @author Sergey Olefir
 */
public class KryoLocalDateSerializer extends Serializer<LocalDate>
{

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.Serializer#write(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, java.lang.Object)
	 */
	@Override
	public void write(Kryo kryo, Output output, LocalDate object)
	{
		output.writeString(object.toString());
	}

	/* (non-Javadoc)
	 * @see com.esotericsoftware.kryo.Serializer#read(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
	 */
	@Override
	public LocalDate read(Kryo kryo, Input input, Class<LocalDate> type)
	{
		return LocalDate.parse(input.readString());
	}

	
}
