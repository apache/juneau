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
 * Subclass of {@link RdfParser} for parsing Turtle into POJOs.
 *
 * <p>
 * Accepts RDF in W3C Turtle format and converts it to Java beans, maps, collections, and primitive
 * types. Date/time literals are parsed via ISO 8601.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse Turtle string into a bean.</jc>
 * 	String <jv>turtle</jv> = ...;  <jc>// Turtle content</jc>
 * 	Person <jv>person</jv> = TurtleParser.<jsf>DEFAULT</jsf>.parse(<jv>turtle</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the Turtle marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = Turtle.<jsf>DEFAULT</jsf>.read(<jv>turtle</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse into a list of beans.</jc>
 * 	List&lt;Person&gt; <jv>people</jv> = TurtleParser.<jsf>DEFAULT</jsf>.parse(<jv>turtle</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	TurtleParser <jv>p</jv> = TurtleParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>turtle</jv>, MyBean.<jk>class</jk>);
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
public class TurtleParser extends RdfParser {

	/** Default Turtle parser, all default settings.*/
	public static final TurtleParser DEFAULT = new TurtleParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfParser.Builder create() {
		return RdfParser.create().turtle();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public TurtleParser(RdfParser.Builder builder) {
		super(builder.turtle().consumes("text/turtle"));
	}
}