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
package org.apache.juneau.marshall.marshaller;

import org.apache.juneau.marshall.json5.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A {@link Json5} variant that produces <b>readable</b> (indented, multi-line) JSON5 output.
 *
 * <p>
 * 	This is the first-class facade form of the readable JSON5 variant.  It pairs
 * 	{@link Json5Serializer#DEFAULT_READABLE} with {@link Json5Parser#DEFAULT} and exposes the full
 * 	static shortcut surface bound to its own {@link #DEFAULT} instance.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	String <jv>json</jv> = Json5R.<jsm>of</jsm>(<jv>myPojo</jv>);
 * 	MyPojo <jv>myPojo</jv> = Json5R.<jsm>to</jsm>(<jv>json</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Because Java <i>hides</i> (does not override) static members, this class redeclares the
 * 		entire static shortcut surface of {@link Json5} so that every shortcut is bound to this class's
 * 		readable {@link #DEFAULT} rather than the compact {@link Json5#DEFAULT}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class Json5R extends Json5 {

	/**
	 * Default reusable instance, readable format.
	 */
	public static final Json5R DEFAULT = new Json5R();

	/**
	 * Serializes a POJO to a <c>String</c> using the {@link #DEFAULT} marshaller.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(Object object) throws SerializeException {
		return DEFAULT.write(object);
	}

	/**
	 * Parses an input into the specified object type using the {@link #DEFAULT} marshaller.
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
	 * Parses an input into the specified parameterized object type using the {@link #DEFAULT} marshaller.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(String input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Parses the contents of a {@link Reader} into the specified object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * Catches any {@link IOException} from the underlying stream and rethrows it as an unchecked
	 * {@link ParseException}.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input reader.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered or an I/O error occurred on the underlying stream.
	 */
	public static <T> T to(Reader input, Class<T> type) throws ParseException {
		try {
			return DEFAULT.read(input, type);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Parses the contents of a {@link Reader} into the specified parameterized object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * Catches any {@link IOException} from the underlying stream and rethrows it as an unchecked
	 * {@link ParseException}.
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input reader.
	 * @param type The object type to create.
	 * @param args The type arguments of the class if it's a collection or map.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered or an I/O error occurred on the underlying stream.
	 */
	public static <T> T to(Reader input, Type type, Type... args) throws ParseException {
		try {
			return DEFAULT.read(input, type, args);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Serializes a POJO to the specified {@link Writer} using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * Catches any {@link IOException} from the underlying stream and rethrows it as an unchecked
	 * {@link SerializeException}.
	 *
	 * @param object The object to serialize.
	 * @param output The writer to serialize to.
	 * @throws SerializeException If a problem occurred trying to convert the output or an I/O error occurred on the underlying stream.
	 */
	public static void of(Object object, Writer output) throws SerializeException {
		try {
			DEFAULT.write(object, output);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Opens a low-level {@link TokenReader} cursor over the specified input using the {@link #DEFAULT} marshaller.
	 *
	 * @param input The input.
	 * @return A new {@link TokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	public static TokenReader toTokens(Object input) throws IOException {
		return DEFAULT.readTokens(input);
	}

	/**
	 * Opens a low-level {@link TokenWriter} generator over the specified output using the {@link #DEFAULT} marshaller.
	 *
	 * @param output The output.
	 * @return A new {@link TokenWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	public static TokenWriter ofTokens(Object output) throws IOException {
		return DEFAULT.writeTokens(output);
	}

	/**
	 * Opens a low-level {@link RecordReader} cursor over the specified input using the {@link #DEFAULT} marshaller.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	public static RecordReader toRecords(Object input) throws IOException {
		return DEFAULT.readRecords(input);
	}

	/**
	 * Opens a low-level {@link RecordWriter} cursor over the specified output using the {@link #DEFAULT} marshaller.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	public static RecordWriter ofRecords(Object output) throws IOException {
		return DEFAULT.writeRecords(output);
	}

	/**
	 * Opens a {@link RecordReader} that yields each element of a top-level wire array using the {@link #DEFAULT} marshaller.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	public static RecordReader toArrayRecords(Object input) throws IOException {
		return DEFAULT.readArrayRecords(input);
	}

	/**
	 * Opens a {@link RecordWriter} that wraps each written value as one element of a top-level wire array using the {@link #DEFAULT} marshaller.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	public static RecordWriter ofArrayRecords(Object output) throws IOException {
		return DEFAULT.writeArrayRecords(output);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link Json5Serializer#DEFAULT_READABLE} and {@link Json5Parser#DEFAULT}.
	 */
	public Json5R() {
		super(Json5Serializer.DEFAULT_READABLE, Json5Parser.DEFAULT);
	}
}
