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

import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * A pairing of a {@link RdfXmlSerializer} and {@link RdfXmlParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	RdfXml <jv>rdfXml</jv> = <jk>new</jk> RdfXml();
 * 	MyPojo <jv>myPojo</jv> = <jv>rdfXml</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>rdfXml</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = RdfXml.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = RdfXml.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;rdf:RDF&gt;</xt>
 * 		<xt>&lt;rdf:Description rdf:about=</xt><xs>"..."</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:name&gt;</xt>Alice<xt>&lt;/j:name&gt;</xt>
 * 			<xt>&lt;j:age&gt;</xt>30<xt>&lt;/j:age&gt;</xt>
 * 		<xt>&lt;/rdf:Description&gt;</xt>
 * 	<xt>&lt;/rdf:RDF&gt;</xt>
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array of tags):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;rdf:RDF&gt;</xt>
 * 		<xt>&lt;rdf:Description rdf:about=</xt><xs>"..."</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:name&gt;</xt>Alice<xt>&lt;/j:name&gt;</xt>
 * 			<xt>&lt;j:age&gt;</xt>30<xt>&lt;/j:age&gt;</xt>
 * 			<xt>&lt;j:address rdf:resource=</xt><xs>".../address"</xs><xt>/&gt;</xt>
 * 			<xt>&lt;j:tags rdf:resource=</xt><xs>".../tags"</xs><xt>/&gt;</xt>
 * 		<xt>&lt;/rdf:Description&gt;</xt>
 * 		<xt>&lt;rdf:Description rdf:about=</xt><xs>".../address"</xs><xt>&gt;</xt>
 * 			<xt>&lt;j:street&gt;</xt>123 Main St<xt>&lt;/j:street&gt;</xt>
 * 			<xt>&lt;j:city&gt;</xt>Boston<xt>&lt;/j:city&gt;</xt>
 * 			<xt>&lt;j:state&gt;</xt>MA<xt>&lt;/j:state&gt;</xt>
 * 		<xt>&lt;/rdf:Description&gt;</xt>
 * 	<xt>&lt;/rdf:RDF&gt;</xt>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class RdfXml extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final RdfXml DEFAULT = new RdfXml();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link RdfXmlSerializer#DEFAULT} and {@link RdfXmlParser#DEFAULT}.
	 */
	public RdfXml() {
		this(RdfXmlSerializer.DEFAULT, RdfXmlParser.DEFAULT);
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
	public RdfXml(RdfXmlSerializer s, RdfXmlParser p) {
		super(s, p);
	}
}