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
import org.apache.juneau.jena.RdfParserBuilder;
import org.apache.juneau.jena.RdfSerializerBuilder;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RdfConfig @RdfConfig} annotation.
 */
public class RdfConfigAnnotation {

	/**
	 * Applies {@link RdfConfig} annotations to a {@link RdfSerializerBuilder}.
	 */
	public static class SerializerApplier extends AnnotationApplier<RdfConfig,RdfSerializerBuilder> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApplier(VarResolverSession vr) {
			super(RdfConfig.class, RdfSerializerBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RdfConfig> ai, RdfSerializerBuilder b) {
			RdfConfig a = ai.getAnnotation();

			string(a.language()).ifPresent(x -> b.set(RDF_language, x));
			string(a.juneauNs()).ifPresent(x -> b.set(RDF_juneauNs, x));
			string(a.juneauBpNs()).ifPresent(x -> b.set(RDF_juneauBpNs, x));
			bool(a.disableUseXmlNamespaces()).ifPresent(x -> b.set(RDF_disableUseXmlNamespaces, x));
			string(a.arp_iriRules()).ifPresent(x -> b.set(RDF_arp_iriRules, x));
			string(a.arp_errorMode()).ifPresent(x -> b.set(RDF_arp_errorMode, x));
			bool(a.arp_embedding()).ifPresent(x -> b.set(RDF_arp_embedding, x));
			string(a.rdfxml_xmlBase()).ifPresent(x -> b.set(RDF_rdfxml_xmlBase, x));
			bool(a.rdfxml_longId()).ifPresent(x -> b.set(RDF_rdfxml_longId, x));
			bool(a.rdfxml_allowBadUris()).ifPresent(x -> b.set(RDF_rdfxml_allowBadUris, x));
			string(a.rdfxml_relativeUris()).ifPresent(x -> b.set(RDF_rdfxml_relativeUris, x));
			string(a.rdfxml_showXmlDeclaration()).ifPresent(x -> b.set(RDF_rdfxml_showXmlDeclaration, x));
			bool(a.rdfxml_disableShowDoctypeDeclaration()).ifPresent(x -> b.set(RDF_rdfxml_disableShowDoctypeDeclaration, x));
			integer(a.rdfxml_tab(), "rdfxml_tab").ifPresent(x -> b.set(RDF_rdfxml_tab, x));
			string(a.rdfxml_attributeQuoteChar()).ifPresent(x -> b.set(RDF_rdfxml_attributeQuoteChar, x));
			string(a.rdfxml_blockRules()).ifPresent(x -> b.set(RDF_rdfxml_blockRules, x));
			integer(a.n3_minGap(), "n3_minGap").ifPresent(x -> b.set(RDF_n3_minGap, x));
			bool(a.n3_disableObjectLists()).ifPresent(x -> b.set(RDF_n3_disableObjectLists, x));
			integer(a.n3_subjectColumn(), "n3_subjectColumn").ifPresent(x -> b.set(RDF_n3_subjectColumn, x));
			integer(a.n3_propertyColumn(), "n3_propertyColumn").ifPresent(x -> b.set(RDF_n3_propertyColumn, x));
			integer(a.n3_indentProperty(), "n3_indentProperty").ifPresent(x -> b.set(RDF_n3_indentProperty, x));
			integer(a.n3_widePropertyLen(), "n3_widePropertyLen").ifPresent(x -> b.set(RDF_n3_widePropertyLen, x));
			bool(a.n3_disableAbbrevBaseUri()).ifPresent(x -> b.set(RDF_n3_disableAbbrevBaseUri, x));
			bool(a.n3_disableUsePropertySymbols()).ifPresent(x -> b.set(RDF_n3_disableUsePropertySymbols, x));
			bool(a.n3_disableUseTripleQuotedStrings()).ifPresent(x -> b.set(RDF_n3_disableUseTripleQuotedStrings, x));
			bool(a.n3_disableUseDoubles()).ifPresent(x -> b.set(RDF_n3_disableUseDoubles, x));
			string(a.collectionFormat()).ifPresent(x -> b.set(RDF_collectionFormat, x));
			bool(a.looseCollections()).ifPresent(x -> b.set(RDF_looseCollections, x));
			bool(a.addBeanTypes()).ifPresent(x -> b.set(RDF_addBeanTypes, x));
			bool(a.addLiteralTypes()).ifPresent(x -> b.set(RDF_addLiteralTypes, x));
			bool(a.addRootProperty()).ifPresent(x -> b.set(RDF_addRootProperty, x));
			bool(a.disableAutoDetectNamespaces()).ifPresent(x -> b.set(RDF_disableAutoDetectNamespaces, x));
			b.setIfNotEmpty(RDF_namespaces, stringList(a.namespaces()));
			bool(a.trimWhitespace()).ifPresent(x -> b.set(RDF_trimWhitespace, x));
		}
	}

	/**
	 * Applies {@link RdfConfig} annotations to a {@link RdfParserBuilder}.
	 */
	public static class ParserApplier extends AnnotationApplier<RdfConfig,RdfParserBuilder> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApplier(VarResolverSession vr) {
			super(RdfConfig.class, RdfParserBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RdfConfig> ai, RdfParserBuilder b) {
			RdfConfig a = ai.getAnnotation();

			string(a.language()).ifPresent(x -> b.set(RDF_language, x));
			string(a.juneauNs()).ifPresent(x -> b.set(RDF_juneauNs, x));
			string(a.juneauBpNs()).ifPresent(x -> b.set(RDF_juneauBpNs, x));
			bool(a.disableUseXmlNamespaces()).ifPresent(x -> b.set(RDF_disableUseXmlNamespaces, x));
			string(a.arp_iriRules()).ifPresent(x -> b.set(RDF_arp_iriRules, x));
			string(a.arp_errorMode()).ifPresent(x -> b.set(RDF_arp_errorMode, x));
			bool(a.arp_embedding()).ifPresent(x -> b.set(RDF_arp_embedding, x));
			string(a.rdfxml_xmlBase()).ifPresent(x -> b.set(RDF_rdfxml_xmlBase, x));
			bool(a.rdfxml_longId()).ifPresent(x -> b.set(RDF_rdfxml_longId, x));
			bool(a.rdfxml_allowBadUris()).ifPresent(x -> b.set(RDF_rdfxml_allowBadUris, x));
			string(a.rdfxml_relativeUris()).ifPresent(x -> b.set(RDF_rdfxml_relativeUris, x));
			string(a.rdfxml_showXmlDeclaration()).ifPresent(x -> b.set(RDF_rdfxml_showXmlDeclaration, x));
			bool(a.rdfxml_disableShowDoctypeDeclaration()).ifPresent(x -> b.set(RDF_rdfxml_disableShowDoctypeDeclaration, x));
			integer(a.rdfxml_tab(), "rdfxml_tab").ifPresent(x -> b.set(RDF_rdfxml_tab, x));
			string(a.rdfxml_attributeQuoteChar()).ifPresent(x -> b.set(RDF_rdfxml_attributeQuoteChar, x));
			string(a.rdfxml_blockRules()).ifPresent(x -> b.set(RDF_rdfxml_blockRules, x));
			integer(a.n3_minGap(), "n3_minGap").ifPresent(x -> b.set(RDF_n3_minGap, x));
			bool(a.n3_disableObjectLists()).ifPresent(x -> b.set(RDF_n3_disableObjectLists, x));
			integer(a.n3_subjectColumn(), "n3_subjectColumn").ifPresent(x -> b.set(RDF_n3_subjectColumn, x));
			integer(a.n3_propertyColumn(), "n3_propertyColumn").ifPresent(x -> b.set(RDF_n3_propertyColumn, x));
			integer(a.n3_indentProperty(), "n3_indentProperty").ifPresent(x -> b.set(RDF_n3_indentProperty, x));
			integer(a.n3_widePropertyLen(), "n3_widePropertyLen").ifPresent(x -> b.set(RDF_n3_widePropertyLen, x));
			bool(a.n3_disableAbbrevBaseUri()).ifPresent(x -> b.set(RDF_n3_disableAbbrevBaseUri, x));
			bool(a.n3_disableUsePropertySymbols()).ifPresent(x -> b.set(RDF_n3_disableUsePropertySymbols, x));
			bool(a.n3_disableUseTripleQuotedStrings()).ifPresent(x -> b.set(RDF_n3_disableUseTripleQuotedStrings, x));
			bool(a.n3_disableUseDoubles()).ifPresent(x -> b.set(RDF_n3_disableUseDoubles, x));
			string(a.collectionFormat()).ifPresent(x -> b.set(RDF_collectionFormat, x));
			bool(a.looseCollections()).ifPresent(x -> b.set(RDF_looseCollections, x));
			bool(a.addBeanTypes()).ifPresent(x -> b.set(RDF_addBeanTypes, x));
			bool(a.addLiteralTypes()).ifPresent(x -> b.set(RDF_addLiteralTypes, x));
			bool(a.addRootProperty()).ifPresent(x -> b.set(RDF_addRootProperty, x));
			bool(a.disableAutoDetectNamespaces()).ifPresent(x -> b.set(RDF_disableAutoDetectNamespaces, x));
			b.setIfNotEmpty(RDF_namespaces, stringList(a.namespaces()));
			bool(a.trimWhitespace()).ifPresent(x -> b.set(RDF_trimWhitespace, x));
		}
	}
}