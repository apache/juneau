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

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.toml.*;

/**
 * TOML marshaller combining {@link TomlSerializer} and {@link TomlParser}.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a config map to TOML</jc>
 * 	Map&lt;String, Object&gt; <jv>config</jv> = Map.of(<js>"name"</js>, <js>"myapp"</js>, <js>"port"</js>, 8080);
 * 	String <jv>toml</jv> = Toml.<jsm>of</jsm>(<jv>config</jv>);
 *
 * 	<jc>// Parse TOML into JsonMap</jc>
 * 	JsonMap <jv>parsed</jv> = Toml.<jsm>to</jsm>(<jv>toml</jv>, JsonMap.<jk>class</jk>);
 *
 * 	<jc>// Round-trip a bean</jc>
 * 	MyConfig <jv>config</jv> = <jk>new</jk> MyConfig(<js>"myapp"</js>, 8080);
 * 	<jv>toml</jv> = Toml.<jsm>of</jsm>(<jv>config</jv>);
 * 	MyConfig <jv>restored</jv> = Toml.<jsm>to</jsm>(<jv>toml</jv>, MyConfig.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Toml <jv>m</jv> = Toml.<jsf>DEFAULT</jsf>;
 * 	String <jv>toml</jv> = <jv>m</jv>.write(<jv>config</jv>);
 * 	JsonMap <jv>parsed</jv> = <jv>m</jv>.read(<jv>toml</jv>, JsonMap.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name = "Alice"
 * 	age = 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested table + array):</h5>
 * <p class='bcode'>
 * 	name = "Alice"
 * 	age = 30
 * 	tags = ["a", "b", "c"]
 *
 * 	[address]
 * 	street = "123 Main St"
 * 	city = "Boston"
 * 	state = "MA"
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a href="https://toml.io/en/v1.0.0">TOML v1.0.0 Specification</a>
 * </ul>
 */
public class Toml extends CharMarshaller {

	/** Default marshaller instance. */
	public static final Toml DEFAULT = new Toml();

	/**
	 * Serializes the object to TOML string.
	 *
	 * @param object The object to serialize.
	 * @return The TOML string.
	 * @throws SerializeException Serialization error.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes the object to the output and returns the output.
	 *
	 * @param object The object to serialize.
	 * @param output The output target.
	 * @return The output.
	 * @throws SerializeException Serialization error.
	 * @throws IOException I/O error.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}

	/**
	 * Parses input to the specified type.
	 *
	 * @param input The input to parse.
	 * @param type The target class.
	 * @return The parsed object.
	 * @throws ParseException Parse error.
	 * @throws IOException I/O error.
	 */
	public static <T> T to(Object input, Class<T> type) throws ParseException, IOException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses input to the specified parameterized type.
	 *
	 * @param input The input to parse.
	 * @param type The target type.
	 * @param args Type arguments for parameterized types.
	 * @return The parsed object.
	 * @throws ParseException Parse error.
	 * @throws IOException I/O error.
	 */
	public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses TOML string to the specified class.
	 *
	 * @param input The TOML string.
	 * @param type The target class.
	 * @return The parsed object.
	 * @throws ParseException Parse error.
	 */
	public static <T> T to(String input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses TOML string to the specified parameterized type.
	 *
	 * @param input The TOML string.
	 * @param type The target type.
	 * @param args Type arguments for parameterized types.
	 * @return The parsed object.
	 * @throws ParseException Parse error.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/** Creates using default serializer and parser. */
	public Toml() {
		this(TomlSerializer.DEFAULT, TomlParser.DEFAULT);
	}

	/**
	 * Creates with custom serializer and parser.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Toml(TomlSerializer s, TomlParser p) {
		super(s, p);
	}
}
