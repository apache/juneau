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
 * Subclass of {@link RdfParser} for parsing RDF/XML into POJOs.
 *
 * <p>
 * Accepts RDF in standard XML notation (including RDF/XML-Abbrev output) and converts it to Java beans,
 * maps, collections, and primitive types. Date/time literals are parsed via ISO 8601.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse RDF/XML string into a bean.</jc>
 * 	String <jv>rdfXml</jv> = ...;  <jc>// RDF/XML content</jc>
 * 	Person <jv>person</jv> = RdfXmlParser.<jsf>DEFAULT</jsf>.parse(<jv>rdfXml</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the RdfXml marshaller for convenience.</jc>
 * 	Person <jv>person</jv> = RdfXml.to(<jv>rdfXml</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Parse into a map.</jc>
 * 	Map&lt;String, String&gt; <jv>map</jv> = RdfXmlParser.<jsf>DEFAULT</jsf>.parse(<jv>rdfXml</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom parser with swaps.</jc>
 * 	RdfXmlParser <jv>p</jv> = RdfXmlParser.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	MyBean <jv>bean</jv> = <jv>p</jv>.parse(<jv>rdfXml</jv>, MyBean.<jk>class</jk>);
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
public class RdfXmlParser extends RdfParser {

	/** Default XML parser, all default settings.*/
	public static final RdfXmlParser DEFAULT = new RdfXmlParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfParser.Builder create() {
		return RdfParser.create().xml();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public RdfXmlParser(RdfParser.Builder builder) {
		super(builder.xml().consumes("text/xml+rdf"));
	}
}