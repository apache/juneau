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
import org.apache.juneau.jena.RdfSerializer;

/**
 * TODO
 */
public class RdfExample {

	/**
	 * TODO
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Pojo pojo = new Pojo("rdf","This is RDF format.");
		// this creates an RDF serializer with the default XML structure
		RdfSerializer rdfSerializer = RdfSerializer.DEFAULT_XML;
		// This will show the final output from the bean
		System.out.println(rdfSerializer.serialize(pojo));
	}
}
