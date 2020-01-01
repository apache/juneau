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
package org.apache.juneau.jena.annotation;

import static org.apache.juneau.jena.RdfCommon.*;
import static org.apache.juneau.jena.RdfSerializer.*;
import static org.apache.juneau.jena.RdfParser.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link RdfConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class RdfConfigApply extends ConfigApply<RdfConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public RdfConfigApply(Class<RdfConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(AnnotationInfo<RdfConfig> ai, PropertyStoreBuilder psb) {
		RdfConfig a = ai.getAnnotation();
		if (! a.language().isEmpty())
			psb.set(RDF_language, string(a.language()));
		if (! a.juneauNs().isEmpty())
			psb.set(RDF_juneauNs, string(a.juneauNs()));
		if (! a.juneauBpNs().isEmpty())
			psb.set(RDF_juneauBpNs, string(a.juneauBpNs()));
		if (! a.useXmlNamespaces().isEmpty())
			psb.set(RDF_useXmlNamespaces, bool(a.useXmlNamespaces()));
		if (! a.arp_iriRules().isEmpty())
			psb.set(RDF_arp_iriRules, string(a.arp_iriRules()));
		if (! a.arp_errorMode().isEmpty())
			psb.set(RDF_arp_errorMode, string(a.arp_errorMode()));
		if (! a.arp_embedding().isEmpty())
			psb.set(RDF_arp_embedding, bool(a.arp_embedding()));
		if (! a.rdfxml_xmlBase().isEmpty())
			psb.set(RDF_rdfxml_xmlBase, string(a.rdfxml_xmlBase()));
		if (! a.rdfxml_longId().isEmpty())
			psb.set(RDF_rdfxml_longId, bool(a.rdfxml_longId()));
		if (! a.rdfxml_allowBadUris().isEmpty())
			psb.set(RDF_rdfxml_allowBadUris, bool(a.rdfxml_allowBadUris()));
		if (! a.rdfxml_relativeUris().isEmpty())
			psb.set(RDF_rdfxml_relativeUris, string(a.rdfxml_relativeUris()));
		if (! a.rdfxml_showXmlDeclaration().isEmpty())
			psb.set(RDF_rdfxml_showXmlDeclaration, string(a.rdfxml_showXmlDeclaration()));
		if (! a.rdfxml_showDoctypeDeclaration().isEmpty())
			psb.set(RDF_rdfxml_showDoctypeDeclaration, bool(a.rdfxml_showDoctypeDeclaration()));
		if (! a.rdfxml_tab().isEmpty())
			psb.set(RDF_rdfxml_tab, integer(a.rdfxml_tab(), "rdfxml_tab"));
		if (! a.rdfxml_attributeQuoteChar().isEmpty())
			psb.set(RDF_rdfxml_attributeQuoteChar, string(a.rdfxml_attributeQuoteChar()));
		if (! a.rdfxml_blockRules().isEmpty())
			psb.set(RDF_rdfxml_blockRules, string(a.rdfxml_blockRules()));
		if (! a.n3_minGap().isEmpty())
			psb.set(RDF_n3_minGap, integer(a.n3_minGap(), "n3_minGap"));
		if (! a.n3_objectLists().isEmpty())
			psb.set(RDF_n3_objectLists, bool(a.n3_objectLists()));
		if (! a.n3_subjectColumn().isEmpty())
			psb.set(RDF_n3_subjectColumn, integer(a.n3_subjectColumn(), "n3_subjectColumn"));
		if (! a.n3_propertyColumn().isEmpty())
			psb.set(RDF_n3_propertyColumn, integer(a.n3_propertyColumn(), "n3_propertyColumn"));
		if (! a.n3_indentProperty().isEmpty())
			psb.set(RDF_n3_indentProperty, integer(a.n3_indentProperty(), "n3_indentProperty"));
		if (! a.n3_widePropertyLen().isEmpty())
			psb.set(RDF_n3_widePropertyLen, integer(a.n3_widePropertyLen(), "n3_widePropertyLen"));
		if (! a.n3_abbrevBaseUri().isEmpty())
			psb.set(RDF_n3_abbrevBaseUri, bool(a.n3_abbrevBaseUri()));
		if (! a.n3_usePropertySymbols().isEmpty())
			psb.set(RDF_n3_usePropertySymbols, bool(a.n3_usePropertySymbols()));
		if (! a.n3_useTripleQuotedStrings().isEmpty())
			psb.set(RDF_n3_useTripleQuotedStrings, bool(a.n3_useTripleQuotedStrings()));
		if (! a.n3_useDoubles().isEmpty())
			psb.set(RDF_n3_useDoubles, bool(a.n3_useDoubles()));
		if (! a.collectionFormat().isEmpty())
			psb.set(RDF_collectionFormat, string(a.collectionFormat()));
		if (! a.looseCollections().isEmpty())
			psb.set(RDF_looseCollections, bool(a.looseCollections()));

		if (! a.addBeanTypes().isEmpty())
			psb.set(RDF_addBeanTypes, bool(a.addBeanTypes()));
		if (! a.addLiteralTypes().isEmpty())
			psb.set(RDF_addLiteralTypes, bool(a.addLiteralTypes()));
		if (! a.addRootProperty().isEmpty())
			psb.set(RDF_addRootProperty, bool(a.addRootProperty()));
		if (! a.autoDetectNamespaces().isEmpty())
			psb.set(RDF_autoDetectNamespaces, bool(a.autoDetectNamespaces()));
		if (a.namespaces().length > 0)
			psb.set(RDF_namespaces, strings(a.namespaces()));

		if (! a.trimWhitespace().isEmpty())
			psb.set(RDF_trimWhitespace, bool(a.trimWhitespace()));

		if (a.annotateRdf().length > 0)
			psb.addTo(CONTEXT_annotations, a.annotateRdf());
	}
}
