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
 * Subclass of {@link RdfParser} for parsing N-Quads into POJOs.
 *
 * <p>
 * Accepts RDF in N-Quads format (quads with optional graph component) and converts it to Java beans,
 * maps, collections, and primitive types. Date/time literals are parsed via ISO 8601.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse N-Quads string into a bean.</jc>
 * 	String <jv>nQuads</jv> = ...;  <jc>// N-Quads content</jc>
 * 	Person <jv>person</jv> = NQuadsParser.<jsf>DEFAULT</jsf>.parse(<jv>nQuads</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the NQuads marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = NQuads.to(<jv>nQuads</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	NQuadsParser <jv>p</jv> = NQuadsParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>nQuads</jv>, MyBean.<jk>class</jk>);
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
public class NQuadsParser extends RdfParser {

	/** Default N-Quads parser, all default settings.*/
	public static final NQuadsParser DEFAULT = new NQuadsParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfParser.Builder create() {
		return RdfParser.create().nQuads();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public NQuadsParser(RdfParser.Builder builder) {
		super(builder.nQuads().consumes("application/n-quads"));
	}
}
