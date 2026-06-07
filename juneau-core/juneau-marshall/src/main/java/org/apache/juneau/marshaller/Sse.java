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

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.sse.*;

/**
 * Pairs {@link SseSerializer} and {@link SseParser} into a single class with convenience
 * read/write methods for the <c>text/event-stream</c> wire format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of events using the DEFAULT instance</jc>
 * 	List&lt;SseEvent&gt; <jv>events</jv> = List.<jsm>of</jsm>(
 * 		<jk>new</jk> SseEvent(<js>"progress"</js>, <js>"step 1"</js>),
 * 		<jk>new</jk> SseEvent(<js>"progress"</js>, <js>"step 2"</js>)
 * 	);
 * 	String <jv>wire</jv> = Sse.<jsm>of</jsm>(<jv>events</jv>);
 *
 * 	<jc>// Parse a wire string back into a list of events</jc>
 * 	List&lt;SseEvent&gt; <jv>parsed</jv> = Sse.<jsm>to</jsm>(<jv>wire</jv>, List.<jk>class</jk>, SseEvent.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Sse extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Sse DEFAULT = new Sse();

	/**
	 * Serializes a Java object to an SSE wire string.
	 *
	 * @param object The object to serialize.
	 * @return The serialized output.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Serializes a Java object to an SSE output.
	 *
	 * @param object The object to serialize.
	 * @param output The output object.
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static Object of(Object object, Object output) throws SerializeException, IOException {
		DEFAULT.write(object, output);
		return output;
	}

	/**
	 * Parses an SSE input object to the specified Java type.
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
	 * Parses an SSE input object to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @see MarshallingSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public static <T> T to(Object input, Type type, Type...args) throws ParseException, IOException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses an SSE input string to the specified type.
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
	 * Parses an SSE input string to the specified Java type.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @see MarshallingSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public static <T> T to(String input, Type type, Type...args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Constructor.
	 */
	public Sse() {
		this(SseSerializer.DEFAULT, SseParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Sse(SseSerializer s, SseParser p) {
		super(s, p);
	}
}
