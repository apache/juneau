// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.jena;

import static org.apache.juneau.jena.Constants.*;

import org.apache.juneau.*;

/**
 * Subclass of {@link RdfParser} for parsing RDF in standard XML notation.
 */
public class RdfXmlParser extends RdfParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default XML parser, all default settings.*/
	public static final RdfXmlParser DEFAULT = new RdfXmlParser(ContextProperties.DEFAULT);

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Instantiates a new clean-slate {@link RdfParserBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link RdfParserBuilder} object.
	 */
	public static RdfParserBuilder create() {
		return new RdfParserBuilder().xml();
	}

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 */
	public RdfXmlParser(ContextProperties cp) {
		super(
			cp.builder()
				.set(RDF_language, LANG_RDF_XML)
				.build(),
			"text/xml+rdf"
		);
	}
}