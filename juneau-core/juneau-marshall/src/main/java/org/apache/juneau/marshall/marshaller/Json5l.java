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

import org.apache.juneau.marshall.json5l.*;

/**
 * Pairs {@link Json5lSerializer} and {@link Json5lParser} into a single class with convenience
 * read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize to JSON5L using instance</jc>
 * 	Json5l <jv>json5l</jv> = <jk>new</jk> Json5l();
 * 	String <jv>out</jv> = <jv>json5l</jv>.write(<jv>myList</jv>);
 * 	List&lt;MyBean&gt; <jv>in</jv> = <jv>json5l</jv>.read(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Serialize to JSON5L using DEFAULT instance</jc>
 * 	String <jv>out</jv> = Json5l.<jsm>of</jsm>(<jv>myList</jv>);
 * 	List&lt;MyBean&gt; <jv>in</jv> = Json5l.<jsm>to</jsm>(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (List of beans, strict-per-line default):</h5>
 * <p class='bjson'>
 * {"name":"Alice","age":30}
 * {"name":"Bob","age":25}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Json5l extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Json5l DEFAULT = new Json5l();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link Json5lSerializer#DEFAULT} and {@link Json5lParser#DEFAULT}.
	 */
	public Json5l() {
		this(Json5lSerializer.DEFAULT, Json5lParser.DEFAULT);
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
	public Json5l(Json5lSerializer s, Json5lParser p) {
		super(s, p);
	}
}
