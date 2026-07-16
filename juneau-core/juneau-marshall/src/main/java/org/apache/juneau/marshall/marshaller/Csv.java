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

import org.apache.juneau.marshall.csv.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A pairing of a {@link CsvSerializer} and {@link CsvParser} into a single class with convenience to/of methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to CSV</jc>
 * 	List&lt;Person&gt; <jv>people</jv> = List.of(<jk>new</jk> Person(<js>"Alice"</js>, 30), <jk>new</jk> Person(<js>"Bob"</js>, 25));
 * 	String <jv>csv</jv> = Csv.<jsm>of</jsm>(<jv>people</jv>);
 *
 * 	<jc>// Parse CSV into a list of beans</jc>
 * 	List&lt;Person&gt; <jv>parsed</jv> = Csv.<jsm>to</jsm>(<jv>csv</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 *
 * 	<jc>// Parse CSV into a list of maps</jc>
 * 	List&lt;Map&lt;String,String&gt;&gt; <jv>rows</jv> = Csv.<jsm>to</jsm>(<jv>csv</jv>, List.<jk>class</jk>, Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Csv <jv>m</jv> = Csv.<jsf>DEFAULT</jsf>;
 * 	<jv>csv</jv> = <jv>m</jv>.write(<jv>people</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>csv</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (list of maps with a,b):</h5>
 * <p class='bcode'>
 * 	a,b
 * 	foo,bar
 * </p>
 *
 * <h5 class='figure'>Complex (list of beans with nested address, flattened):</h5>
 * <p class='bcode'>
 * 	name,age,address_street,address_city,address_state,tags
 * 	Alice,30,123 Main St,Boston,MA,"a,b,c"
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = Csv.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Csv.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor shortcut methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class Csv extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Csv DEFAULT = new Csv();

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
	 * Uses {@link CsvSerializer#DEFAULT} and {@link CsvParser#DEFAULT}.
	 */
	public Csv() {
		this(CsvSerializer.DEFAULT, CsvParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public Csv(CsvSerializer s, CsvParser p) {
		super(s, p);
	}
}