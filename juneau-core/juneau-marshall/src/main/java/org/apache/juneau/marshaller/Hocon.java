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

import org.apache.juneau.hocon.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of a {@link HoconSerializer} and {@link HoconParser} into a single class with convenience read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Instance usage</jc>
 * 	Hocon <jv>hocon</jv> = <jk>new</jk> Hocon();
 * 	MyBean <jv>bean</jv> = <jv>hocon</jv>.read(<jv>hoconString</jv>, MyBean.<jk>class</jk>);
 * 	String <jv>hoconOut</jv> = <jv>hocon</jv>.write(<jv>bean</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Static DEFAULT instance</jc>
 * 	MyBean <jv>bean</jv> = Hocon.<jsf>DEFAULT</jsf>.read(<jv>hoconString</jv>, MyBean.<jk>class</jk>);
 * 	String <jv>hoconOut</jv> = Hocon.<jsf>DEFAULT</jsf>.write(<jv>bean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean):</h5>
 * <p class='bjson'>
 * name = myapp
 * port = 8080
 * debug = true
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">marshallers</a>
 * </ul>
 */
public class Hocon extends CharMarshaller {

	/** Default reusable instance. */
	public static final Hocon DEFAULT = new Hocon();

	/**
	 * Serializes a Java object to a HOCON string.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to HOCON output.
	 *
	 * @param object The object to serialize.
	 * @param output The output (Writer, OutputStream, File, StringBuilder).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}

	/**
	 * Parses HOCON input to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Class<T> type) throws ParseException, IOException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses HOCON input to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments for maps and collections.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses a HOCON string to the specified type.
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
	 * Parses a HOCON string to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments for maps and collections.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link HoconSerializer#DEFAULT} and {@link HoconParser#DEFAULT}.
	 */
	public Hocon() {
		this(HoconSerializer.DEFAULT, HoconParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public Hocon(HoconSerializer s, HoconParser p) {
		super(s, p);
	}
}
