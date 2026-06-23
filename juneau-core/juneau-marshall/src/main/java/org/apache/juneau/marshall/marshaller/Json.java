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

import org.apache.juneau.marshall.json.*;

/**
 * A pairing of a {@link JsonSerializer} and {@link JsonParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Json <jv>json</jv> = <jk>new</jk> Json();
 * 	MyPojo <jv>myPojo</jv> = <jv>json</jv>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>json</jv>.of(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Json.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Json.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30}
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30,<js>"address"</js>:{<js>"street"</js>:<js>"123 Main St"</js>,<js>"city"</js>:<js>"Boston"</js>,<js>"state"</js>:<js>"MA"</js>},<js>"tags"</js>:[<js>"a"</js>,<js>"b"</js>,<js>"c"</js>]}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Json extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Json DEFAULT = new Json();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link JsonSerializer#DEFAULT} and {@link JsonParser#DEFAULT}.
	 */
	public Json() {
		this(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
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
	public Json(JsonSerializer s, JsonParser p) {
		super(s, p);
	}
}