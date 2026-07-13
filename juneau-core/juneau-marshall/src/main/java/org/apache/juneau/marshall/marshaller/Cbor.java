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

import org.apache.juneau.marshall.cbor.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A pairing of a {@link CborSerializer} and {@link CborParser} into a single class with convenience read/write methods.
 *
 * <p>
 * Output is binary (<code><jk>byte</jk>[]</code>) in RFC 8949 CBOR format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using static convenience methods</jc>
 * 	byte[] <jv>cbor</jv> = Cbor.<jsm>of</jsm>(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = Cbor.<jsm>to</jsm>(<jv>cbor</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using the DEFAULT instance</jc>
 * 	byte[] <jv>cbor</jv> = Cbor.<jsf>DEFAULT</jsf>.write(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = Cbor.<jsf>DEFAULT</jsf>.read(<jv>cbor</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = Cbor.<jsm>to</jsm>(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * 	<jk>byte</jk>[] <jv>bytes</jv> = Cbor.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Cbor">CBOR Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class Cbor extends StreamMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Cbor DEFAULT = new Cbor();

	/**
	 * Serializes a POJO to a <code><jk>byte</jk>[]</code> using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
	 *
	 * @param object The object to serialize.
	 * @return The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static byte[] of(Object object) throws SerializeException {
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
	public static <T> T to(byte[] input, Class<T> type) throws ParseException {
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
	public static <T> T to(byte[] input, Type type, Type... args) throws ParseException {
		return DEFAULT.read(input, type, args);
	}

	/**
	 * Opens a low-level {@link TokenReader} cursor over the specified input using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.readTokens(<jv>input</jv>)</c>.
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
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.writeTokens(<jv>output</jv>)</c>.
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
	 * Opens a {@link RecordReader} that yields each element of a top-level wire array using the {@link #DEFAULT} marshaller.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.readArrayRecords(<jv>input</jv>)</c>.
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
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.writeArrayRecords(<jv>output</jv>)</c>.
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
	 * Uses {@link CborSerializer#DEFAULT} and {@link CborParser#DEFAULT}.
	 */
	public Cbor() {
		this(CborSerializer.DEFAULT, CborParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use for serializing output.
	 * @param p The parser to use for parsing input.
	 */
	public Cbor(CborSerializer s, CborParser p) {
		super(s, p);
	}
}
