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
 * Subclass of {@link RdfParser} for parsing JSON-LD into POJOs.
 *
 * <p>
 * Accepts RDF in W3C JSON-LD format and converts it to Java beans, maps, collections, and primitive
 * types. Date/time literals are parsed via ISO 8601.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse JSON-LD string into a bean.</jc>
 * 	String <jv>jsonLd</jv> = ...;  <jc>// JSON-LD content</jc>
 * 	Person <jv>person</jv> = JsonLdParser.<jsf>DEFAULT</jsf>.parse(<jv>jsonLd</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the JsonLd marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = JsonLd.<jsf>DEFAULT</jsf>.read(<jv>jsonLd</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse into a list of beans.</jc>
 * 	List&lt;Person&gt; <jv>people</jv> = JsonLdParser.<jsf>DEFAULT</jsf>.parse(<jv>jsonLd</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	JsonLdParser <jv>p</jv> = JsonLdParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>jsonLd</jv>, MyBean.<jk>class</jk>);
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
public class JsonLdParser extends RdfParser {

	/** Default JSON-LD parser, all default settings.*/
	public static final JsonLdParser DEFAULT = new JsonLdParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfParser.Builder create() {
		return RdfParser.create().jsonLd();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonLdParser(RdfParser.Builder builder) {
		super(builder.jsonLd().consumes("application/ld+json"));
	}
}
