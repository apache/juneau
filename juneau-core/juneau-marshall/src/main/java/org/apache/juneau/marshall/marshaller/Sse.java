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

import org.apache.juneau.marshall.sse.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

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
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = Sse.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Sse.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class Sse extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Sse DEFAULT = new Sse();

	/**
	 * Serializes a POJO to a <c>String</c> using the {@link #DEFAULT} marshaller.
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
	 * Parses an input into the specified object type using the {@link #DEFAULT} marshaller.
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
	 * Parses an input into the specified parameterized object type using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c>.
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
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link ParseException}, so the
	 * caller is not burdened with a checked exception.
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
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>, <jv>args</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link ParseException}, so the
	 * caller is not burdened with a checked exception.
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
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>, <jv>output</jv>)</c> that catches any
	 * {@link IOException} from the underlying stream and rethrows it as an unchecked {@link SerializeException}, so the
	 * caller is not burdened with a checked exception.
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
	 * Opens a low-level {@link RecordReader} cursor over the specified input using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.readRecords(<jv>input</jv>)</c>.
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
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.writeRecords(<jv>output</jv>)</c>.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	public static RecordWriter ofRecords(Object output) throws IOException {
		return DEFAULT.writeRecords(output);
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
