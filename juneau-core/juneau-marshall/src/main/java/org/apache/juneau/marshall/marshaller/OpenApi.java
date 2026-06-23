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

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link OpenApiSerializer} and {@link OpenApiParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	OpenApi <jv>oapi</jv> = <jk>new</jk> OpenApi();
 * 	MyPojo <jv>myPojo</jv> = <jv>oapi</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>oapi</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = OpenApi.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = OpenApi.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class OpenApi extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final OpenApi DEFAULT = new OpenApi();

	/**
	 * Serializes a Java object to an OpenApi output.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.of(<jv>output</jv>)</c>.
	 * @param schema The part schema.  Can be <jk>null</jk>.
	 * @param object The object to serialize.
	 * @return The output object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public static String of(HttpPartSchema schema, Object object) throws SerializeException {
		return DEFAULT.s.serialize(HttpPartType.ANY, schema, object);
	}

	/**
	 * Parses an OpenApi input object to the specified Java type.
	 *
	 * <p>
	 * A shortcut for calling <c><jsf>DEFAULT</jsf>.to(<jv>input</jv>, <jv>type</jv>)</c>.
	 *
	 * @param <T> The class type of the object being created.
	 * @param schema The part type schema.  Can be <jk>null</jk>.
	 * @param input
	 * 	The input.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> T to(HttpPartSchema schema, String input, Class<T> type) throws ParseException {
		return DEFAULT.p.parse(HttpPartType.ANY, schema, input, type);
	}

	private final OpenApiSerializer s;

	private final OpenApiParser p;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link OpenApiSerializer#DEFAULT} and {@link OpenApiParser#DEFAULT}.
	 */
	public OpenApi() {
		this(OpenApiSerializer.DEFAULT, OpenApiParser.DEFAULT);
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
	public OpenApi(OpenApiSerializer s, OpenApiParser p) {
		super(s, p);
		this.s = s;
		this.p = p;
	}
}