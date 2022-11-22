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
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of a {@link OpenApiSerializer} and {@link OpenApiParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	OpenApi <jv>oapi</jv> = <jk>new</jk> OpenApi();
 * 	MyPojo <jv>myPojo</jv> = <jv>oapi</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>oapi</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = OpenApi.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = OpenApi.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Marshallers">Marshallers</a>
 * </ul>
 */
public class OpenApi extends CharMarshaller {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Default reusable instance.
	 */
	public static final OpenApi DEFAULT = new OpenApi();

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final OpenApiSerializer s;
	private final OpenApiParser p;

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
	public OpenApi(OpenApiSerializer s, OpenApiParser p) {
		super(s, p);
		this.s = s;
		this.p = p;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link OpenApiSerializer#DEFAULT} and {@link OpenApiParser#DEFAULT}.
	 */
	public OpenApi() {
		this(OpenApiSerializer.DEFAULT, OpenApiParser.DEFAULT);
	}

	/**
	 * Parses an OpenApi input string to the specified type.
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
	public static <T> T to(String input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses an OpenApi input object to the specified Java type.
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
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#fileCharset(Charset)} property value).
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
	 * Parses an OpenApi input object to the specified Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param schema The part type schema.  Can be <jk>null</jk>.
	 * @param input
	 * 	The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(HttpPartSchema schema, String input, Class<T> type) throws ParseException, IOException {
		return DEFAULT.p.parse(HttpPartType.ANY, schema, input, type);
	}

	/**
	 * Parses an OpenApi input string to the specified Java type.
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
	public static <T> T to(String input, Type type, Type...args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses an OpenApi input object to the specified Java type.
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
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link org.apache.juneau.parser.ReaderParser.Builder#fileCharset(Charset)} property value).
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
	 * Serializes a Java object to an OpenApi string.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return
	 * 	The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to an OpenApi output.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>output</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @param output
	 * 	The output object.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 		<li>{@link StringBuilder} - Output will be written to the specified string builder.
	 * 	</ul>
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}

	/**
	 * Serializes a Java object to an OpenApi output.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>output</jv>)</c>.
	 * @param schema The part schema.  Can be <jk>null</jk>.
	 * @param object The object to serialize.
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(HttpPartSchema schema, Object object) throws SerializeException {
		return DEFAULT.s.serialize(HttpPartType.ANY, schema, object);
	}
}
