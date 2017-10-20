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

import static org.apache.juneau.jena.RdfSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link RdfSerializer} class.
 */
public final class RdfSerializerContext extends SerializerContext implements RdfCommon {
	
	final boolean
		addLiteralTypes,
		addRootProperty,
		useXmlNamespaces,
		looseCollections,
		autoDetectNamespaces,
		addBeanTypeProperties;
	final String rdfLanguage;
	final Namespace juneauNs;
	final Namespace juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings = new HashMap<String,Object>();
	final Namespace[] namespaces;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public RdfSerializerContext(PropertyStore ps) {
		super(ps);
		addLiteralTypes = ps.getProperty(RDF_addLiteralTypes, boolean.class, false);
		addRootProperty = ps.getProperty(RDF_addRootProperty, boolean.class, false);
		useXmlNamespaces = ps.getProperty(RDF_useXmlNamespaces, boolean.class, true);
		looseCollections = ps.getProperty(RDF_looseCollections, boolean.class, false);
		autoDetectNamespaces = ps.getProperty(RDF_autoDetectNamespaces, boolean.class, true);
		rdfLanguage = ps.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = ps.getProperty(RDF_juneauNs, Namespace.class, new Namespace("j", "http://www.apache.org/juneau/"));
		juneauBpNs = ps.getProperty(RDF_juneauBpNs, Namespace.class, new Namespace("jp", "http://www.apache.org/juneaubp/"));
		collectionFormat = ps.getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
		namespaces = ps.getProperty(RDF_namespaces, Namespace[].class, new Namespace[0]);
		addBeanTypeProperties = ps.getProperty(RDF_addBeanTypeProperties, boolean.class, ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("RdfSerializerContext", new ObjectMap()
				.append("addLiteralTypes", addLiteralTypes)
				.append("addRootProperty", addRootProperty)
				.append("useXmlNamespaces", useXmlNamespaces)
				.append("looseCollections", looseCollections)
				.append("autoDetectNamespaces", autoDetectNamespaces)
				.append("rdfLanguage", rdfLanguage)
				.append("juneauNs", juneauNs)
				.append("juneauBpNs", juneauBpNs)
				.append("collectionFormat", collectionFormat)
				.append("namespaces", namespaces)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
