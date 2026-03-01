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
 * Stream-based RDF serializer for RDF/THRIFT binary format.
 *
 * <p>
 * Serializes POJOs to compact binary RDF using Apache Thrift encoding, ideal for efficient storage
 * and network transfer. Semantically equivalent to RDF/XML and other RDF text formats.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to RDF/THRIFT bytes.</jc>
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Alice"</js>, 30);
 * 	<jk>byte</jk>[] <jv>thriftBytes</jv> = RdfThriftSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the RdfThrift marshaller for convenience.</jc>
 * 	<jk>byte</jk>[] <jv>thriftBytes</jv> = RdfThrift.of(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Serialize to OutputStream.</jc>
 * 	RdfThriftSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>, <jv>outputStream</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom serializer with swaps.</jc>
 * 	RdfThriftSerializer <jv>s</jv> = RdfThriftSerializer.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	<jk>byte</jk>[] <jv>bytes</jv> = <jv>s</jv>.serialize(<jv>bean</jv>);
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
public class RdfThriftSerializer extends RdfStreamSerializer {

	/** Default RDF/THRIFT serializer.*/
	public static final RdfThriftSerializer DEFAULT = new RdfThriftSerializer(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static RdfStreamSerializer.Builder create() {
		return RdfStreamSerializer.create()
			.language(Constants.LANG_RDFTHRIFT)
			.produces("application/vnd.apache.thrift.binary")
			.accept("application/vnd.apache.thrift.binary");
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public RdfThriftSerializer(RdfStreamSerializer.Builder builder) {
		super(builder);
	}
}
