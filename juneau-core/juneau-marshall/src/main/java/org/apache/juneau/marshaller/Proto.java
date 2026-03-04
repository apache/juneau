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
import org.apache.juneau.proto.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of a {@link ProtoSerializer} and {@link ProtoParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Proto <jv>proto</jv> = <jk>new</jk> Proto();
 * 	MyPojo <jv>myPojo</jv> = <jv>proto</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>proto</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Proto.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Proto.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * 	address {
 * 	  street: "123 Main St"
 * 	  city: "Boston"
 * 	  state: "MA"
 * 	}
 * 	tags: ["a", "b", "c"]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * </ul>
 */
public class Proto extends CharMarshaller {

	/** Default marshaller instance. */
	public static final Proto DEFAULT = new Proto();

	/**
	 * Serializes a Java object to a Protobuf Text Format string.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to the output and returns the output.
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
	 * Parses a Protobuf Text Format string to the specified type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input to parse.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
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
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException I/O error.
	 */
	public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses a Protobuf Text Format string to the specified class.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The Protobuf Text Format string.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses a Protobuf Text Format string to the specified parameterized type.
	 *
	 * @param input The Protobuf Text Format string.
	 * @param type The target type.
	 * @param args Type arguments for parameterized types.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/** Creates using default serializer and parser. */
	public Proto() {
		this(ProtoSerializer.DEFAULT, ProtoParser.DEFAULT);
	}

	/**
	 * Creates with custom serializer and parser.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Proto(ProtoSerializer s, ProtoParser p) {
		super(s, p);
	}
}
