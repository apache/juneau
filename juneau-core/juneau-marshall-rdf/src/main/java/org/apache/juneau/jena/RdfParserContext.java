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

import static org.apache.juneau.jena.RdfParser.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link RdfParser} class.
 */
public final class RdfParserContext extends ParserContext implements RdfCommon {
	
	final boolean trimWhitespace, looseCollections;
	final String rdfLanguage;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings = new HashMap<>();

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public RdfParserContext(PropertyStore ps) {
		super(ps);
		trimWhitespace = ps.getProperty(RDF_trimWhitespace, boolean.class, false);
		looseCollections = ps.getProperty(RDF_looseCollections, boolean.class, false);
		rdfLanguage = ps.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = ps.getProperty(RDF_juneauNs, Namespace.class, new Namespace("j", "http://www.apache.org/juneau/"));
		juneauBpNs = ps.getProperty(RDF_juneauBpNs, Namespace.class, new Namespace("j", "http://www.apache.org/juneaubp/"));
		collectionFormat = ps.getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("RdfParserContext", new ObjectMap()
				.append("trimWhitespace", trimWhitespace)
				.append("looseCollections", looseCollections)
				.append("rdfLanguage", rdfLanguage)
				.append("juneauNs", juneauNs)
				.append("juneauBpNs", juneauBpNs)
				.append("collectionFormat", collectionFormat)
			);
	}
}