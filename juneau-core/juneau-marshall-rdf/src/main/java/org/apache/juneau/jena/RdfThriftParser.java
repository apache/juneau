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
 * Stream-based RDF parser for RDF/THRIFT binary format.
 *
 * <p>
 * Parses compact binary RDF (Apache Thrift encoding) into POJOs. Accepts byte arrays and input streams.
 * Semantically equivalent to RDF/XML parsing—produces the same Java objects.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse RDF/THRIFT bytes into a bean.</jc>
 * 	<jk>byte</jk>[] <jv>thriftBytes</jv> = RdfThrift.of(<jv>person</jv>);
 * 	Person <jv>person</jv> = RdfThriftParser.<jsf>DEFAULT</jsf>.parse(<jv>thriftBytes</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the RdfThrift marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = RdfThrift.to(<jv>thriftBytes</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse into a map with key/value types.</jc>
 * 	Map&lt;String, String&gt; <jv>map</jv> = RdfThriftParser.<jsf>DEFAULT</jsf>.parse(<jv>thriftBytes</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse from InputStream.</jc>
 * 	Person <jv>person</jv> = RdfThriftParser.<jsf>DEFAULT</jsf>.parse(<jv>inputStream</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	RdfThriftParser <jv>p</jv> = RdfThriftParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>thriftBytes</jv>, MyBean.<jk>class</jk>);
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
public class RdfThriftParser extends RdfStreamParser {

	/** Default RDF/THRIFT parser.*/
	public static final RdfThriftParser DEFAULT = new RdfThriftParser(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static RdfStreamParser.Builder create() {
		return RdfStreamParser.create()
			.language(Constants.LANG_RDFTHRIFT)
			.consumes("application/vnd.apache.thrift.binary");
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public RdfThriftParser(RdfStreamParser.Builder builder) {
		super(builder);
	}
}
