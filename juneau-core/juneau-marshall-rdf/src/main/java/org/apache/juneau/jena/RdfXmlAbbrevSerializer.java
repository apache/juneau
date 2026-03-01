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
 * Subclass of {@link RdfSerializer} for serializing POJOs to RDF/XML-Abbrev (abbreviated XML notation).
 *
 * <p>
 * Produces more compact RDF/XML output than {@link RdfXmlSerializer} by using XML abbreviation syntax.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to abbreviated RDF/XML.</jc>
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"Alice"</js>, 30);
 * 	String <jv>rdfXml</jv> = RdfXmlAbbrevSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Or use the RdfXmlAbbrev marshaller for convenience.</jc>
 * 	String <jv>rdfXml</jv> = RdfXmlAbbrev.<jsf>DEFAULT</jsf>.write(<jv>person</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom serializer.</jc>
 * 	RdfXmlAbbrevSerializer <jv>s</jv> = RdfXmlAbbrevSerializer.create().swaps(DateSwap.<jk>class</jk>).build();
 * 	String <jv>rdfXml</jv> = <jv>s</jv>.serialize(<jv>bean</jv>);
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
public class RdfXmlAbbrevSerializer extends RdfSerializer {

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfXmlAbbrevSerializer DEFAULT = new RdfXmlAbbrevSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static RdfSerializer.Builder create() {
		return RdfSerializer.create().xmlabbrev();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public RdfXmlAbbrevSerializer(RdfSerializer.Builder builder) {
		super(builder.xmlabbrev());
	}
}