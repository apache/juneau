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

/**
 * A pairing of {@link RdfProtoSerializer} and {@link RdfProtoParser} for RDF/PROTO binary format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to RDF/PROTO bytes</jc>
 * 	byte[] <jv>bytes</jv> = RdfProto.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse RDF/PROTO bytes into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = RdfProto.<jsm>to</jsm>(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	RdfProto <jv>m</jv> = RdfProto.<jsf>DEFAULT</jsf>;
 * 	<jv>bytes</jv> = <jv>m</jv>.of(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.to(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <p>Output is binary (<jk>byte</jk>[]), Protocol Buffers format.</p>
 *
 * <p>Complex structures (nested objects, arrays) serialize to equivalent RDF triples in binary form.</p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class RdfProto extends StreamMarshaller {

	/** Default reusable instance.*/
	public static final RdfProto DEFAULT = new RdfProto();

	/** Constructor using defaults.*/
	public RdfProto() {
		this(RdfProtoSerializer.DEFAULT, RdfProtoParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public RdfProto(RdfProtoSerializer s, RdfProtoParser p) {
		super(s, p);
	}
}
