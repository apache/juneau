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

import org.apache.juneau.ini.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of an {@link IniSerializer} and {@link IniParser} into a single class with convenience read/write methods.
 *
 * <p>
 * The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Ini <jv>ini</jv> = <jk>new</jk> Ini();
 * 	MyPojo <jv>myPojo</jv> = <jv>ini</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>ini</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Ini.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Ini.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bini'>
 * 	<ck>name</ck> = <cv>Alice</cv>
 * 	<ck>age</ck> = <cv>30</cv>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bini'>
 * 	<ck>name</ck> = <cv>Alice</cv>
 * 	<ck>age</ck> = <cv>30</cv>
 * 	<ck>tags</ck> = <cv>['a','b','c']</cv>
 *
 * 	<cs>[address]</cs>
 * 	<ck>street</ck> = <cv>123 Main St</cv>
 * 	<ck>city</ck> = <cv>Boston</cv>
 * 	<ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>The top-level object must be a bean or <c>Map&lt;String,?&gt;</c>.
 * 	<li class='note'>Collections and complex map values are embedded as JSON5 inline strings.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Ini extends CharMarshaller {

	/** Default reusable instance. */
	public static final Ini DEFAULT = new Ini();

	/** Default reusable instance, readable format. */
	public static final Ini DEFAULT_READABLE = new Ini(IniSerializer.DEFAULT_READABLE, IniParser.DEFAULT);

	/**
	 * Serializes a Java object to an INI string.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to INI output.
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
	 * Parses INI input to the specified Java type.
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
	 * Parses INI input to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments for maps and collections.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Type type, Type...args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses an INI string to the specified type.
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
	 * Parses an INI string to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments for maps and collections.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type...args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link IniSerializer#DEFAULT} and {@link IniParser#DEFAULT}.
	 */
	public Ini() {
		this(IniSerializer.DEFAULT, IniParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public Ini(IniSerializer s, IniParser p) {
		super(s, p);
	}
}
