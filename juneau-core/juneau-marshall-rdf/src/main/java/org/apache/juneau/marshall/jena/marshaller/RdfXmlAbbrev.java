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
package org.apache.juneau.marshall.jena.marshaller;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link RdfXmlAbbrevSerializer} and {@link RdfXmlParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	RdfXmlAbbrev <jv>rdfXmlAbbrev</jv> = <jk>new</jk> RdfXmlAbbrev();
 * 	MyPojo <jv>myPojo</jv> = <jv>rdfXmlAbbrev</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>rdfXmlAbbrev</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = RdfXmlAbbrev.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = RdfXmlAbbrev.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, abbreviated RDF/XML):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;rdf:RDF&gt;</xt>
 * 		<xt>&lt;j:Person rdf:about=</xt><xs>"..."</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:name&gt;</xt>Alice<xt>&lt;/j:name&gt;</xt>
 * 			<xt>&lt;j:age&gt;</xt>30<xt>&lt;/j:age&gt;</xt>
 * 		<xt>&lt;/j:Person&gt;</xt>
 * 	<xt>&lt;/rdf:RDF&gt;</xt>
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;rdf:RDF&gt;</xt>
 * 		<xt>&lt;j:Person rdf:about=</xt><xs>"..."</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:name&gt;</xt>Alice<xt>&lt;/j:name&gt;</xt>
 * 			<xt>&lt;j:age&gt;</xt>30<xt>&lt;/j:age&gt;</xt>
 * 			<xt>&lt;j:address rdf:resource=</xt><xs>".../address"</xs><xt>/&gt;</xt>
 * 		<xt>&lt;/j:Person&gt;</xt>
 * 		<xt>&lt;j:Address rdf:about=</xt><xs>".../address"</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:street&gt;</xt>123 Main St<xt>&lt;/j:street&gt;</xt>
 * 			<xt>&lt;j:city&gt;</xt>Boston<xt>&lt;/j:city&gt;</xt>
 * 			<xt>&lt;j:state&gt;</xt>MA<xt>&lt;/j:state&gt;</xt>
 * 		<xt>&lt;/j:Address&gt;</xt>
 * 	<xt>&lt;/rdf:RDF&gt;</xt>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class RdfXmlAbbrev extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final RdfXmlAbbrev DEFAULT = new RdfXmlAbbrev();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link RdfXmlAbbrevSerializer#DEFAULT} and {@link RdfXmlParser#DEFAULT}.
	 */
	public RdfXmlAbbrev() {
		this(RdfXmlAbbrevSerializer.DEFAULT, RdfXmlParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public RdfXmlAbbrev(RdfXmlAbbrevSerializer s, RdfXmlParser p) {
		super(s, p);
	}
}