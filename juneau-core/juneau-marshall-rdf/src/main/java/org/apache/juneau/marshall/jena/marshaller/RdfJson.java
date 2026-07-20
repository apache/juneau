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
 * A pairing of a {@link RdfJsonSerializer} and {@link RdfJsonParser} into a single class with convenience to/of methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to RDF/JSON</jc>
 * 	String <jv>rdfJson</jv> = RdfJson.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse RDF/JSON into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = RdfJson.<jsm>to</jsm>(<jv>rdfJson</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	RdfJson <jv>m</jv> = RdfJson.<jsf>DEFAULT</jsf>;
 * 	<jv>rdfJson</jv> = <jv>m</jv>.write(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>rdfJson</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, RDF/JSON format):</h5>
 * <p class='bjson'>
 * 	{ <js>"&lt;subject&gt;"</js>: { <js>"&lt;.../name&gt;"</js>: [{ <js>"value"</js>: <js>"Alice"</js> }],
 * 		<js>"&lt;.../age&gt;"</js>: [{ <js>"value"</js>: 30, <js>"type"</js>: <js>"literal"</js> }] } }
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bjson'>
 * 	{ <js>"&lt;...&gt;"</js>: { <js>"&lt;.../name&gt;"</js>: [{ <js>"value"</js>: <js>"Alice"</js> }],
 * 		<js>"&lt;.../address&gt;"</js>: [{ <js>"type"</js>: <js>"uri"</js>, <js>"value"</js>: <js>".../address"</js> }] },
 * 	  <js>"&lt;.../address&gt;"</js>: { <js>"&lt;.../street&gt;"</js>: [{ <js>"value"</js>: <js>"123 Main St"</js> }],
 * 		<js>"&lt;.../city&gt;"</js>: [{ <js>"value"</js>: <js>"Boston"</js> }], <js>"&lt;.../state&gt;"</js>: [{ <js>"value"</js>: <js>"MA"</js> }] } }
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = RdfJson.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = RdfJson.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class RdfJson extends CharMarshaller {

	/** Default reusable instance.*/
	public static final RdfJson DEFAULT = new RdfJson();

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

	/** Constructor using defaults.*/
	public RdfJson() {
		this(RdfJsonSerializer.DEFAULT, RdfJsonParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p The parser to use.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public RdfJson(RdfJsonSerializer s, RdfJsonParser p) {
		super(s, p);
	}
}
