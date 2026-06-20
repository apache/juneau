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

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link Json5Serializer} and {@link Json5Parser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Json5 <jv>json</jv> = <jk>new</jk> Json5();
 * 	MyPojo <jv>myPojo</jv> = <jv>json</jv>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>json</jv>.of(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Json5.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Json5.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age, JSON5 allows unquoted keys):</h5>
 * <p class='bjson'>
 * 	{name:<js>"Alice"</js>,age:30}
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bjson'>
 * 	{name:<js>"Alice"</js>,age:30,address:{street:<js>"123 Main St"</js>,city:<js>"Boston"</js>,state:<js>"MA"</js>},tags:[<js>"a"</js>,<js>"b"</js>,<js>"c"</js>]}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Json5 extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Json5 DEFAULT = new Json5();

	/**
	 * Default reusable instance, readable format.
	 */
	public static final Json5 DEFAULT_READABLE = new Json5(Json5Serializer.DEFAULT_READABLE, Json5Parser.DEFAULT);

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link Json5Serializer#DEFAULT} and {@link Json5Parser#DEFAULT}.
	 */
	public Json5() {
		this(Json5Serializer.DEFAULT, Json5Parser.DEFAULT);
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
	public Json5(Json5Serializer s, Json5Parser p) {
		super(s, p);
	}
}