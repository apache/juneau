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
package org.apache.juneau.marshall.jena.marshaller;

import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;
import java.lang.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link JsonLdSerializer} and {@link JsonLdParser} into a single class with convenience to/of methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for serializing and parsing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	JsonLd <jv>jsonLd</jv> = <jk>new</jk> JsonLd();
 * 	MyPojo <jv>myPojo</jv> = <jv>jsonLd</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>jsonLd</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bcode'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = JsonLd.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = JsonLd.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, JSON-LD format):</h5>
 * <p class='bjson'>
 * 	{ <js>"@context"</js>: {...}, <js>"@id"</js>: <js>"..."</js>,
 * 		<js>"@type"</js>: <js>"Person"</js>, <js>"name"</js>: <js>"Alice"</js>, <js>"age"</js>: 30 }
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bjson'>
 * 	{ <js>"@context"</js>: {...}, <js>"@id"</js>: <js>"..."</js>, <js>"@type"</js>: <js>"Person"</js>,
 * 		<js>"name"</js>: <js>"Alice"</js>, <js>"age"</js>: 30,
 * 		<js>"address"</js>: { <js>"@id"</js>: <js>".../address"</js>, <js>"street"</js>: <js>"123 Main St"</js>,
 * 			<js>"city"</js>: <js>"Boston"</js>, <js>"state"</js>: <js>"MA"</js> },
 * 		<js>"tags"</js>: [<js>"a"</js>,<js>"b"</js>,<js>"c"</js>] }
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = JsonLd.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = JsonLd.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class JsonLd extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final JsonLd DEFAULT = new JsonLd();

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
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link JsonLdSerializer#DEFAULT} and {@link JsonLdParser#DEFAULT}.
	 */
	public JsonLd() {
		this(JsonLdSerializer.DEFAULT, JsonLdParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public JsonLd(JsonLdSerializer s, JsonLdParser p) {
		super(s, p);
	}
}
