/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.juneau.examples.core.rdf;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.jena.*;

/**
 *	Sample class which shows the simple usage of RdfXmlSerializer.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RdfExample {

	/**
	 * Serializing Pojo bean into RDF XML format.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {
		Pojo pojo = new Pojo("rdf","This is RDF format.");
		// this creates an RDF serializer with the default XML structure
		/**Produces
		 * <rdf:RDF
		 * xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		 * xmlns:j="http://www.apache.org/juneau/"
		 * xmlns:jp="http://www.apache.org/juneaubp/" >
		 * <rdf:Description rdf:nodeID="A0">
		 * <jp:name>This is RDF format.</jp:name>
		 * <jp:id>rdf</jp:id>
		 * </rdf:Description>
		 * </rdf:RDF>
		 */
		RdfSerializer rdfSerializer = RdfXmlSerializer.DEFAULT;
		// This will show the final output from the bean
		System.out.println(rdfSerializer.serialize(pojo));

		/**Produces
		 * <rdf:RDF
		 * xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		 * xmlns:j="http://www.apache.org/juneau/"
		 * xmlns:jp="http://www.apache.org/juneaubp/">
		 * <rdf:Description>
		 * <jp:name>This is RDF format.</jp:name>
		 * <jp:id>rdf</jp:id>
		 * </rdf:Description>
		 * </rdf:RDF>
		 */
		String rdfXml = RdfXmlAbbrevSerializer.DEFAULT.serialize(pojo);
		System.out.println(rdfXml);

		// Deserialize back to Pojo instance type.
		Pojo xmlAbParsed = RdfXmlParser.DEFAULT.parse(rdfXml,Pojo.class);
		assert xmlAbParsed.getClass().equals(pojo.getClass());
		assert xmlAbParsed.getId().equals(pojo.getId());

		/**Produces
		 * @prefix jp:      <http://www.apache.org/juneaubp/> .
		 * @prefix j:       <http://www.apache.org/juneau/> .
		 *
		 * []    jp:id   "rdf" ;
		 * jp:name "This is RDF format." .
		 */
		String rdfN3 = N3Serializer.DEFAULT.serialize(pojo);
		System.out.println(rdfN3);

		// Deserialize back to Pojo instance type.
		Pojo n3parsed = N3Parser.DEFAULT.parse(rdfN3,Pojo.class);
		assert n3parsed.getClass().equals(pojo.getClass());
		assert n3parsed.getId().equals(pojo.getId());

		/**Produces
		 *_:A5ecded4fX3aX167a62fdefeX3aXX2dX7ffc <http://www.apache.org/juneaubp/name> "This is RDF format." .
		 *_:A5ecded4fX3aX167a62fdefeX3aXX2dX7ffc <http://www.apache.org/juneaubp/id> "rdf" .
		 */
		String rdfNTriple = NTripleSerializer.DEFAULT.serialize(pojo);
		System.out.println(rdfNTriple);

		// Deserialize back to Pojo instance type.
		Pojo nTripleparsed = NTripleParser.DEFAULT.parse(rdfNTriple,Pojo.class);
		assert nTripleparsed.getClass().equals(pojo.getClass());
		assert nTripleparsed.getId().equals(pojo.getId());

		/**
		 * @prefix jp:      <http://www.apache.org/juneaubp/> .
		 * @prefix j:       <http://www.apache.org/juneau/> .
		 *
		 * []    jp:id   "rdf" ;
		 * jp:name "This is RDF format." .
		 */
		String rdfTurtle = TurtleSerializer.DEFAULT.serialize(pojo);
		System.out.println(rdfTurtle);

		// Deserialize back to Pojo instance type.
		Pojo turtleparsed = TurtleParser.DEFAULT.parse(rdfTurtle,Pojo.class);
		assert turtleparsed.getClass().equals(pojo.getClass());
		assert turtleparsed.getId().equals(pojo.getId());


	}
}
