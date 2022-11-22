// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.marshaller;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A subclass of {@link Marshaller} for stream-based serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Marshallers">Marshallers</a>
 * </ul>
 */
public class StreamMarshaller extends Marshaller {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final OutputStreamSerializer s;
	private final InputStreamParser p;

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public StreamMarshaller(OutputStreamSerializer s, InputStreamParser p) {
		super(s, p);
		this.s = s;
		this.p = p;
	}

	/**
	 * Same as {@link #read(Object,Class)} but reads from a byte array and thus doesn't throw an <c>IOException</c>.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Marshaller <jv>marshaller</jv>  = Json.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String <jv>string</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] <jv>beanArray</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List <jv>list</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>marshaller</jv> .read(<jv>json</jv>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public final <T> T read(byte[] input, Class<T> type) throws ParseException {
		try {
			return p.parse(input, type);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Same as {@link #read(Object,Type,Type...)} but reads from a byte array and thus doesn't throw an <c>IOException</c>.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T read(byte[] input, Type type, Type...args) throws ParseException {
		try {
			return p.parse(input, type, args);
		} catch (IOException e) { throw new ParseException(e); }
	}

	/**
	 * Serializes a POJO directly to a <code><jk>byte</jk>[]</code>.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final byte[] write(Object o) throws SerializeException {
		return s.serialize(o);
	}
}
