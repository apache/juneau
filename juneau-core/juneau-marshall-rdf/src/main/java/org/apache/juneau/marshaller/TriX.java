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

import org.apache.juneau.jena.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A pairing of a {@link TriXSerializer} and {@link TriXParser} into a single class with convenience read/write methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class TriX extends CharMarshaller {

	/** Default reusable instance.*/
	public static final TriX DEFAULT = new TriX();

	/**
	 * Serializes a Java object to a TriX string.
	 *
	 * @param object The object to serialize.
	 * @return The serialized string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to TriX output.
	 *
	 * @param object The object to serialize.
	 * @param output The output (Writer, OutputStream, etc.).
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}

	/**
	 * Parses a TriX input object to the specified Java type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input (Reader, InputStream, etc.).
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Class<T> type) throws ParseException, IOException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses a TriX input object to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input (Reader, InputStream, etc.).
	 * @param type The object type to create.
	 * @param args The type arguments if a generic type.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses a TriX input string to the specified type.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input string.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Class<T> type) throws ParseException {
		return DEFAULT.read(input, type);
	}

	/**
	 * Parses a TriX input string to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input string.
	 * @param type The object type to create.
	 * @param args The type arguments if a generic type.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/** Constructor using defaults.*/
	public TriX() {
		this(TriXSerializer.DEFAULT, TriXParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public TriX(TriXSerializer s, TriXParser p) {
		super(s, p);
	}
}
