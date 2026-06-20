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

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of {@link RdfThriftSerializer} and {@link RdfThriftParser} for RDF/THRIFT binary format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to RDF/THRIFT bytes</jc>
 * 	byte[] <jv>bytes</jv> = RdfThrift.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse RDF/THRIFT bytes into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = RdfThrift.<jsm>to</jsm>(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	RdfThrift <jv>m</jv> = RdfThrift.<jsf>DEFAULT</jsf>;
 * 	<jv>bytes</jv> = <jv>m</jv>.write(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>bytes</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <p>Output is binary (<jk>byte</jk>[]), Apache Thrift format.</p>
 *
 * <p>Complex structures (nested objects, arrays) serialize to equivalent RDF triples in binary form.</p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class RdfThrift extends StreamMarshaller {

	/** Default reusable instance.*/
	public static final RdfThrift DEFAULT = new RdfThrift();

	/** Constructor using defaults.*/
	public RdfThrift() {
		this(RdfThriftSerializer.DEFAULT, RdfThriftParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public RdfThrift(RdfThriftSerializer s, RdfThriftParser p) {
		super(s, p);
	}
}
