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

import org.apache.juneau.marshall.hjson.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A pairing of a {@link HjsonSerializer} and {@link HjsonParser} into a single class with convenience to/of methods.
 *
 * <p>
 * The general idea is to combine a single serializer and parser inside a simplified API for serializing and parsing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Hjson <jv>hjson</jv> = <jk>new</jk> Hjson();
 * 	MyPojo <jv>myPojo</jv> = <jv>hjson</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>hjson</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Hjson.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Hjson.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output - readable mode (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example output - compact mode ({@link HjsonC}):</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = Hjson.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Hjson.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class Hjson extends CharMarshaller {

	/** Default reusable instance. */
	public static final Hjson DEFAULT = new Hjson();

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
	 *
	 * <p>
	 * Uses {@link HjsonSerializer#DEFAULT} and {@link HjsonParser#DEFAULT}.
	 */
	public Hjson() {
		this(HjsonSerializer.DEFAULT, HjsonParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public Hjson(HjsonSerializer s, HjsonParser p) {
		super(s, p);
	}
}
