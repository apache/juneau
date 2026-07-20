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
 * A pairing of a {@link NQuadsSerializer} and {@link NQuadsParser} into a single class with convenience to/of methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to N-Quads</jc>
 * 	String <jv>nquads</jv> = NQuads.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse N-Quads into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = NQuads.<jsm>to</jsm>(<jv>nquads</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	NQuads <jv>m</jv> = NQuads.<jsf>DEFAULT</jsf>;
 * 	<jv>nquads</jv> = <jv>m</jv>.write(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>nquads</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, N-Triples with graph):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" &lt;...&gt; .
 * 	&lt;...&gt; &lt;.../age&gt; "30"^^&lt;http://www.w3.org/2001/XMLSchema#int&gt; &lt;...&gt; .
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" &lt;graph&gt; .
 * 	&lt;...&gt; &lt;.../address&gt; &lt;.../address&gt; &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../street&gt; "123 Main St" &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../city&gt; "Boston" &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../state&gt; "MA" &lt;graph&gt; .
 * </p>
 *
 * <p class='bjava'>
 *	<jc>// Using static shortcuts.</jc>
 * 	MyPojo <jv>myPojo</jv> = NQuads.<jsm>to</jsm>(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = NQuads.<jsm>of</jsm>(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class NQuads extends CharMarshaller {

	/** Default reusable instance.*/
	public static final NQuads DEFAULT = new NQuads();

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
	public NQuads() {
		this(NQuadsSerializer.DEFAULT, NQuadsParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p The parser to use.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public NQuads(NQuadsSerializer s, NQuadsParser p) {
		super(s, p);
	}
}
