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
import static org.apache.juneau.jena.RdfParser.*;
import static org.apache.juneau.jena.RdfSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RdfConfig @RdfConfig} annotation.
 */
public class RdfConfigAnnotation {

	/**
	 * Applies {@link RdfConfig} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<RdfConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<RdfConfig> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<RdfConfig> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			RdfConfig a = ai.getAnnotation();
			psb.setIfNotEmpty(RDF_language, string(a.language()));
			psb.setIfNotEmpty(RDF_juneauNs, string(a.juneauNs()));
			psb.setIfNotEmpty(RDF_juneauBpNs, string(a.juneauBpNs()));
			psb.setIfNotEmpty(RDF_disableUseXmlNamespaces, bool(a.disableUseXmlNamespaces()));
			psb.setIfNotEmpty(RDF_arp_iriRules, string(a.arp_iriRules()));
			psb.setIfNotEmpty(RDF_arp_errorMode, string(a.arp_errorMode()));
			psb.setIfNotEmpty(RDF_arp_embedding, bool(a.arp_embedding()));
			psb.setIfNotEmpty(RDF_rdfxml_xmlBase, string(a.rdfxml_xmlBase()));
			psb.setIfNotEmpty(RDF_rdfxml_longId, bool(a.rdfxml_longId()));
			psb.setIfNotEmpty(RDF_rdfxml_allowBadUris, bool(a.rdfxml_allowBadUris()));
			psb.setIfNotEmpty(RDF_rdfxml_relativeUris, string(a.rdfxml_relativeUris()));
			psb.setIfNotEmpty(RDF_rdfxml_showXmlDeclaration, string(a.rdfxml_showXmlDeclaration()));
			psb.setIfNotEmpty(RDF_rdfxml_disableShowDoctypeDeclaration, bool(a.rdfxml_disableShowDoctypeDeclaration()));
			psb.setIfNotEmpty(RDF_rdfxml_tab, integer(a.rdfxml_tab(), "rdfxml_tab"));
			psb.setIfNotEmpty(RDF_rdfxml_attributeQuoteChar, string(a.rdfxml_attributeQuoteChar()));
			psb.setIfNotEmpty(RDF_rdfxml_blockRules, string(a.rdfxml_blockRules()));
			psb.setIfNotEmpty(RDF_n3_minGap, integer(a.n3_minGap(), "n3_minGap"));
			psb.setIfNotEmpty(RDF_n3_disableObjectLists, bool(a.n3_disableObjectLists()));
			psb.setIfNotEmpty(RDF_n3_subjectColumn, integer(a.n3_subjectColumn(), "n3_subjectColumn"));
			psb.setIfNotEmpty(RDF_n3_propertyColumn, integer(a.n3_propertyColumn(), "n3_propertyColumn"));
			psb.setIfNotEmpty(RDF_n3_indentProperty, integer(a.n3_indentProperty(), "n3_indentProperty"));
			psb.setIfNotEmpty(RDF_n3_widePropertyLen, integer(a.n3_widePropertyLen(), "n3_widePropertyLen"));
			psb.setIfNotEmpty(RDF_n3_disableAbbrevBaseUri, bool(a.n3_disableAbbrevBaseUri()));
			psb.setIfNotEmpty(RDF_n3_disableUsePropertySymbols, bool(a.n3_disableUsePropertySymbols()));
			psb.setIfNotEmpty(RDF_n3_disableUseTripleQuotedStrings, bool(a.n3_disableUseTripleQuotedStrings()));
			psb.setIfNotEmpty(RDF_n3_disableUseDoubles, bool(a.n3_disableUseDoubles()));
			psb.setIfNotEmpty(RDF_collectionFormat, string(a.collectionFormat()));
			psb.setIfNotEmpty(RDF_looseCollections, bool(a.looseCollections()));
			psb.setIfNotEmpty(RDF_addBeanTypes, bool(a.addBeanTypes()));
			psb.setIfNotEmpty(RDF_addLiteralTypes, bool(a.addLiteralTypes()));
			psb.setIfNotEmpty(RDF_addRootProperty, bool(a.addRootProperty()));
			psb.setIfNotEmpty(RDF_disableAutoDetectNamespaces, bool(a.disableAutoDetectNamespaces()));
			psb.setIfNotEmpty(RDF_namespaces, stringList(a.namespaces()));
			psb.setIfNotEmpty(RDF_trimWhitespace, bool(a.trimWhitespace()));
		}
	}
}