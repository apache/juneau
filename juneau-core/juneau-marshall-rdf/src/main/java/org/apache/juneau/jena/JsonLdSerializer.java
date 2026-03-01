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
 * Subclass of {@link RdfSerializer} for serializing POJOs to JSON-LD format.
 *
 * <p>
 * Produces RDF in W3C JSON-LD syntax, a JSON-based format for linked data. JSON-LD is widely used
 * for web APIs and integrates well with JSON tooling.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to JSON-LD string.</jc>
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Alice"</js>, 30);
 * 	String <jv>jsonLd</jv> = JsonLdSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the JsonLd marshaller for convenience.</jc>
 * 	String <jv>jsonLd</jv> = JsonLd.<jsf>DEFAULT</jsf>.write(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom serializer with swaps.</jc>
 * 	JsonLdSerializer <jv>s</jv> = JsonLdSerializer.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	String <jv>jsonLd</jv> = <jv>s</jv>.serialize(<jv>bean</jv>);
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
public class JsonLdSerializer extends RdfSerializer {

	/** Default JSON-LD serializer, all default settings.*/
	public static final JsonLdSerializer DEFAULT = new JsonLdSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfSerializer.Builder create() {
		return RdfSerializer.create().jsonLd();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonLdSerializer(RdfSerializer.Builder builder) {
		super(builder.jsonLd());
	}
}
