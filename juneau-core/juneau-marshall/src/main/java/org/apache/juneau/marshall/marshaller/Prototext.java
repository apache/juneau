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

import org.apache.juneau.marshall.prototext.*;

/**
 * A pairing of a {@link PrototextSerializer} and {@link PrototextParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Prototext <jv>proto</jv> = <jk>new</jk> Prototext();
 * 	MyPojo <jv>myPojo</jv> = <jv>proto</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>proto</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Prototext.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Prototext.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * 	address {
 * 	  street: "123 Main St"
 * 	  city: "Boston"
 * 	  state: "MA"
 * 	}
 * 	tags: ["a", "b", "c"]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * </ul>
 */
public class Prototext extends CharMarshaller {

	/** Default marshaller instance. */
	public static final Prototext DEFAULT = new Prototext();

	/** Creates using default serializer and parser. */
	public Prototext() {
		this(PrototextSerializer.DEFAULT, PrototextParser.DEFAULT);
	}

	/**
	 * Creates with custom serializer and parser.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Prototext(PrototextSerializer s, PrototextParser p) {
		super(s, p);
	}
}
