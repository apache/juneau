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
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of a {@link MsgPackSerializer} and {@link MsgPackParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	MsgPack <jv>msgPack</jv> = <jk>new</jk> MsgPack();
 * 	MyPojo <jv>myPojo</jv> = <jv>msgPack</jv>.read(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * 	<jk>byte</jk>[] <jv>bytes</jv> = <jv>msgPack</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = MsgPack.<jsf>DEFAULT</jsf>.read(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * 	<jk>byte</jk>[] <jv>bytes</jv> = MsgPack.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Marshallers">Marshallers</a>
 * </ul>
 */
public class MsgPack extends StreamMarshaller {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Default reusable instance.
	 */
	public static final MsgPack DEFAULT = new MsgPack();

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

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
	public MsgPack(MsgPackSerializer s, MsgPackParser p) {
		super(s, p);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link MsgPackSerializer#DEFAULT} and {@link MsgPackParser#DEFAULT}.
	 */
	public MsgPack() {
		this(MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT);
	}

	/**
	 * Parses a JSON input string to the specified type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(byte[] input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses a JSON input object to the specified Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link org.apache.juneau.parser.InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Class<T> type) throws ParseException, IOException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses a JSON input string to the specified Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c>.
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
	public static <T> T to(byte[] input, Type type, Type...args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses a JSON input object to the specified Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c>.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input
	 * 	The input.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link org.apache.juneau.parser.InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public static <T> T to(Object input, Type type, Type...args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Serializes a Java object to a JSON string.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return
	 * 	The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to an output.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>output</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @param output
	 * 	The output object.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}
}
