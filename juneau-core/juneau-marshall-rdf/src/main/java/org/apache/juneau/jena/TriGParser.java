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
 * Subclass of {@link RdfParser} for parsing TriG into POJOs.
 *
 * <p>
 * Accepts RDF datasets in TriG format (Turtle with named graphs) and converts them to Java beans,
 * maps, collections, and primitive types. Date/time literals are parsed via ISO 8601.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse TriG string into a bean.</jc>
 * 	String <jv>triG</jv> = ...;  <jc>// TriG content</jc>
 * 	Person <jv>person</jv> = TriGParser.<jsf>DEFAULT</jsf>.parse(<jv>triG</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the TriG marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = TriG.to(<jv>triG</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	TriGParser <jv>p</jv> = TriGParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>triG</jv>, MyBean.<jk>class</jk>);
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
public class TriGParser extends RdfParser {

	/** Default TriG parser, all default settings.*/
	public static final TriGParser DEFAULT = new TriGParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfParser.Builder create() {
		return RdfParser.create().triG();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public TriGParser(RdfParser.Builder builder) {
		super(builder.triG().consumes("application/trig"));
	}
}
