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

import org.apache.juneau.marshall.hjson.*;

/**
 * A pairing of a {@link HjsonSerializer} and {@link HjsonParser} into a single class with convenience to/of methods.
 *
 * <p>
 * The general idea is to combine a single serializer and parser inside a simplified API for serializing and parsing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Hjson <jv>hjson</jv> = <jk>new</jk> Hjson();
 * 	MyPojo <jv>myPojo</jv> = <jv>hjson</jv>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>hjson</jv>.of(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Hjson.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Hjson.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output - readable mode (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example output - compact mode ({@link #DEFAULT_COMPACT}):</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
public class Hjson extends CharMarshaller {

	/** Default reusable instance. */
	public static final Hjson DEFAULT = new Hjson();

	/** Default reusable instance, compact format. */
	public static final Hjson DEFAULT_COMPACT = new Hjson(HjsonSerializer.DEFAULT_COMPACT, HjsonParser.DEFAULT);

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link HjsonSerializer#DEFAULT} and {@link HjsonParser#DEFAULT}.
	 */
	public Hjson() {
		this(HjsonSerializer.DEFAULT, HjsonParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public Hjson(HjsonSerializer s, HjsonParser p) {
		super(s, p);
	}
}
