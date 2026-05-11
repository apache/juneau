/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshaller;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parquet.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of {@link ParquetSerializer} and {@link ParquetParser} for Apache Parquet binary format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to Parquet bytes</jc>
 * 	List&lt;MyBean&gt; <jv>beans</jv> = List.<jsm>of</jsm>(new MyBean(...), new MyBean(...));
 * 	byte[] <jv>bytes</jv> = Parquet.<jsm>of</jsm>(<jv>beans</jv>);
 *
 * 	<jc>// Parse Parquet bytes back into a list of beans</jc>
 * 	List&lt;MyBean&gt; <jv>parsed</jv> = Parquet.<jsm>to</jsm>(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Parquet <jv>m</jv> = Parquet.<jsf>DEFAULT</jsf>;
 * 	<jv>bytes</jv> = <jv>m</jv>.write(<jv>beans</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>Output is binary (<jk>byte</jk>[]), Apache Parquet columnar format.</p>
 *
 * <p class='warnbox'>
 * 	<b>Note:</b> Parquet is <b>collection-oriented</b>. The serializer accepts a
 * 	{@link java.util.Collection} or array of beans (each bean is one row). A single
 * 	bean is automatically wrapped in a one-element list. The parser <em>always</em>
 * 	returns a {@code List<T>}, never a bare {@code T}.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParquetBasics">Parquet Basics</a>
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class Parquet extends StreamMarshaller {

	/** Default reusable instance. */
	public static final Parquet DEFAULT = new Parquet();

	/**
	 * Serializes a Java object to Parquet bytes.
	 *
	 * @param object The object to serialize (Collection, array, or single bean).
	 * @return The serialized Parquet bytes.
	 * @throws SerializeException If a problem occurred.
	 */
	public static byte[] of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to a Parquet output stream.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>, <jv>output</jv>)</c>.
	 *
	 * @param object The object to serialize (Collection, array, or single bean).
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

	/**
	 * Parses a Parquet input into the specified type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, List.<jk>class</jk>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The element type.
	 * @param input
	 * 	The input.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type The bean class (parser returns {@code List<T>}).
	 * @return The parsed list of beans.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> to(Object input, Class<T> type) throws ParseException, IOException {
		return (List<T>) DEFAULT.read(input, List.class, type);
	}

	/**
	 * Parses a Parquet input into the specified type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, List.<jk>class</jk>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The element type.
	 * @param input
	 * 	The input.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type The element type (parser returns {@code List<T>}).
	 * @return The parsed list.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> to(Object input, Type type) throws ParseException, IOException {
		return (List<T>) DEFAULT.read(input, List.class, type);
	}

	/**
	 * Parses Parquet bytes into the specified type.
	 *
	 * @param <T> The element type.
	 * @param input The Parquet bytes.
	 * @param type The bean class (parser returns {@code List<T>}).
	 * @return The parsed list of beans.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> to(byte[] input, Class<T> type) throws ParseException {
		return (List<T>) DEFAULT.read(input, List.class, type);
	}

	/**
	 * Parses Parquet bytes into the specified type.
	 *
	 * @param <T> The element type.
	 * @param input The Parquet bytes.
	 * @param type The element type (parser returns {@code List<T>}).
	 * @return The parsed list.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> to(byte[] input, Type type) throws ParseException {
		return (List<T>) DEFAULT.read(input, List.class, type);
	}

	/**
	 * Parses Parquet bytes to the specified parameterized Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c>.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The Parquet bytes.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @see MarshallingSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public static <T> T to(byte[] input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Constructor using default serializer and parser.
	 */
	public Parquet() {
		this(ParquetSerializer.DEFAULT, ParquetParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Parquet(ParquetSerializer s, ParquetParser p) {
		super(s, p);
	}
}
