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
 * Subclass of {@link RdfSerializer} for serializing POJOs to N-Triples notation.
 *
 * <p>
 * Produces one RDF triple per line in W3C N-Triples format. Simple, line-oriented, and easy to parse.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to N-Triples string.</jc>
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Alice"</js>, 30);
 * 	String <jv>nTriples</jv> = NTripleSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the NTriple marshaller for convenience.</jc>
 * 	String <jv>nTriples</jv> = NTriple.<jsf>DEFAULT</jsf>.write(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom serializer with swaps.</jc>
 * 	NTripleSerializer <jv>s</jv> = NTripleSerializer.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	String <jv>nTriples</jv> = <jv>s</jv>.serialize(<jv>bean</jv>);
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
public class NTripleSerializer extends RdfSerializer {

	/** Default N-Triple serializer, all default settings.*/
	public static final NTripleSerializer DEFAULT = new NTripleSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfSerializer.Builder create() {
		return RdfSerializer.create().ntriple();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public NTripleSerializer(RdfSerializer.Builder builder) {
		super(builder.ntriple());
	}
}