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
package org.apache.juneau.jena;

/**
 * Stream-based RDF parser for RDF/PROTO binary format.
 *
 * <p>
 * Parses compact binary RDF (Protocol Buffers encoding) into POJOs. Accepts byte arrays and input streams.
 * Semantically equivalent to RDF/XML parsing—produces the same Java objects.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse RDF/PROTO bytes into a bean.</jc>
 * 	<jk>byte</jk>[] <jv>protoBytes</jv> = RdfProto.of(<jv>person</jv>);
 * 	Person <jv>person</jv> = RdfProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>protoBytes</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the RdfProto marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = RdfProto.to(<jv>protoBytes</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse into a map with key/value types.</jc>
 * 	Map&lt;String, String&gt; <jv>map</jv> = RdfProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>protoBytes</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse from InputStream.</jc>
 * 	Person <jv>person</jv> = RdfProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>inputStream</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	RdfProtoParser <jv>p</jv> = RdfProtoParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>protoBytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfProtoParser extends RdfStreamParser {

	/** Default RDF/PROTO parser.*/
	public static final RdfProtoParser DEFAULT = new RdfProtoParser(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static RdfStreamParser.Builder create() {
		return RdfStreamParser.create()
			.language(Constants.LANG_RDFPROTO)
			.consumes("application/vnd.apache.protobuf");
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public RdfProtoParser(RdfStreamParser.Builder builder) {
		super(builder);
	}
}
