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

import org.apache.juneau.marshall.protobuf.*;

/**
 * A pairing of a {@link ProtobufSerializer} and {@link ProtobufParser} into a single class with convenience read/write
 * methods.
 *
 * <p>
 * Output is binary (<code><jk>byte</jk>[]</code>) in the Protocol Buffers <b>binary</b> wire format.
 *
 * <p>
 * Distinct from the text-format {@link Prototext} marshaller (<c>text/protobuf</c>):  this marshaller emits the compact,
 * non-self-describing protobuf binary wire format and requires the target type on read.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using static convenience methods</jc>
 * 	byte[] <jv>protobuf</jv> = Protobuf.<jsm>of</jsm>(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = Protobuf.<jsm>to</jsm>(<jv>protobuf</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using the DEFAULT instance</jc>
 * 	byte[] <jv>protobuf</jv> = Protobuf.<jsf>DEFAULT</jsf>.of(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = Protobuf.<jsf>DEFAULT</jsf>.to(<jv>protobuf</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * </ul>
 */
public class Protobuf extends StreamMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Protobuf DEFAULT = new Protobuf();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link ProtobufSerializer#DEFAULT} and {@link ProtobufParser#DEFAULT}.
	 */
	public Protobuf() {
		this(ProtobufSerializer.DEFAULT, ProtobufParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use for serializing output.
	 * @param p The parser to use for parsing input.
	 */
	public Protobuf(ProtobufSerializer s, ProtobufParser p) {
		super(s, p);
	}
}
