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
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Pairs {@link JsonlSerializer} and {@link JsonlParser} into a single class with convenience
 * read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize to JSONL using instance</jc>
 * 	Jsonl <jv>jsonl</jv> = <jk>new</jk> Jsonl();
 * 	String <jv>out</jv> = <jv>jsonl</jv>.write(<jv>myList</jv>);
 * 	List&lt;MyBean&gt; <jv>in</jv> = <jv>jsonl</jv>.read(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Serialize to JSONL using DEFAULT instance</jc>
 * 	String <jv>out</jv> = Jsonl.<jsm>of</jsm>(<jv>myList</jv>);
 * 	List&lt;MyBean&gt; <jv>in</jv> = Jsonl.<jsm>to</jsm>(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (List of beans):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30}
 * {"name":"Bob","age":25}
 * {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='figure'>Complex (bean with nested object and array):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30,"address":{"street":"123 Main St","city":"Boston"},"tags":["a","b","c"]}
 * {"name":"Bob","age":25,"address":{"street":"456 Oak Ave","city":"Portland"},"tags":["d","e"]}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Jsonl extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Jsonl DEFAULT = new Jsonl();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link JsonlSerializer#DEFAULT} and {@link JsonlParser#DEFAULT}.
	 */
	public Jsonl() {
		this(JsonlSerializer.DEFAULT, JsonlParser.DEFAULT);
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
	public Jsonl(JsonlSerializer s, JsonlParser p) {
		super(s, p);
	}
}
