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

import org.apache.juneau.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Utility classes and methods for the {@link RdfConfig @RdfConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfConfigAnnotation {

	/**
	 * Applies {@link RdfConfig} annotations to a {@link org.apache.juneau.jena.RdfSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<RdfConfig,RdfSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(RdfConfig.class, RdfSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RdfConfig> ai, RdfSerializer.Builder b) {
			RdfConfig a = ai.inner();

			string(a.language()).ifPresent(x -> b.language(x));
			string(a.juneauNs()).map(Namespace::of).ifPresent(x -> b.juneauNs(x));
			string(a.juneauBpNs()).map(Namespace::of).ifPresent(x -> b.juneauBpNs(x));
			bool(a.disableUseXmlNamespaces()).ifPresent(x -> b.disableUseXmlNamespaces(x));
			string(a.collectionFormat()).map(RdfCollectionFormat::valueOf).ifPresent(x -> b.collectionFormat(x));
			bool(a.looseCollections()).ifPresent(x -> b.looseCollections(x));
			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesRdf(x));
			bool(a.addLiteralTypes()).ifPresent(x -> b.addLiteralTypes(x));
			bool(a.addRootProperty()).ifPresent(x -> b.addRootProperty(x));
			bool(a.disableAutoDetectNamespaces()).ifPresent(x -> b.disableAutoDetectNamespaces(x));
			strings(a.namespaces()).map(Namespace::createArray).ifPresent(x -> b.namespaces(x));
			string(a.rdfxml_iriRules()).ifPresent(x -> b.rdfxml_iriRules(x));
			string(a.rdfxml_errorMode()).ifPresent(x -> b.rdfxml_errorMode(x));
			bool(a.rdfxml_embedding()).ifPresent(x -> b.rdfxml_embedding(x));
			string(a.rdfxml_xmlBase()).ifPresent(x -> b.rdfxml_xmlbase(x));
			bool(a.rdfxml_longId()).ifPresent(x -> b.rdfxml_longId(x));
			bool(a.rdfxml_allowBadUris()).ifPresent(x -> b.rdfxml_allowBadUris(x));
			string(a.rdfxml_relativeUris()).ifPresent(x -> b.rdfxml_relativeUris(x));
			string(a.rdfxml_showXmlDeclaration()).ifPresent(x -> b.rdfxml_showXmlDeclaration(x));
			bool(a.rdfxml_disableShowDoctypeDeclaration()).ifPresent(x -> b.rdfxml_disableShowDoctypeDeclaration(x));
			integer(a.rdfxml_tab(), "rdfxml_tab").ifPresent(x -> b.rdfxml_tab(x));
			string(a.rdfxml_attributeQuoteChar()).map(x -> x.charAt(0)).ifPresent(x -> b.rdfxml_attributeQuoteChar(x));
			string(a.rdfxml_blockRules()).ifPresent(x -> b.rdfxml_blockRules(x));
			integer(a.n3_minGap(), "n3_minGap").ifPresent(x -> b.n3_minGap(x));
			bool(a.n3_disableObjectLists()).ifPresent(x -> b.n3_disableObjectLists(x));
			integer(a.n3_subjectColumn(), "n3_subjectColumn").ifPresent(x -> b.n3_subjectColumn(x));
			integer(a.n3_propertyColumn(), "n3_propertyColumn").ifPresent(x -> b.n3_propertyColumn(x));
			integer(a.n3_indentProperty(), "n3_indentProperty").ifPresent(x -> b.n3_indentProperty(x));
			integer(a.n3_widePropertyLen(), "n3_widePropertyLen").ifPresent(x -> b.n3_widePropertyLen(x));
			bool(a.n3_disableAbbrevBaseUri()).ifPresent(x -> b.n3_disableAbbrevBaseUri(x));
			bool(a.n3_disableUsePropertySymbols()).ifPresent(x -> b.n3_disableUsePropertySymbols(x));
			bool(a.n3_disableUseTripleQuotedStrings()).ifPresent(x -> b.n3_disableUseTripleQuotedStrings(x));
			bool(a.n3_disableUseDoubles()).ifPresent(x -> b.n3_disableUseDoubles(x));
		}
	}

	/**
	 * Applies {@link RdfConfig} annotations to a {@link org.apache.juneau.jena.RdfParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<RdfConfig,RdfParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(RdfConfig.class, RdfParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RdfConfig> ai, RdfParser.Builder b) {
			RdfConfig a = ai.inner();

			string(a.language()).ifPresent(x -> b.language(x));
			string(a.juneauNs()).map(Namespace::of).ifPresent(x -> b.juneauNs(x));
			string(a.juneauBpNs()).map(Namespace::of).ifPresent(x -> b.juneauBpNs(x));
			string(a.collectionFormat()).map(RdfCollectionFormat::valueOf).ifPresent(x -> b.collectionFormat(x));
			bool(a.looseCollections()).ifPresent(x -> b.looseCollections(x));
			bool(a.trimWhitespace()).ifPresent(x -> b.trimWhitespace(x));
			string(a.rdfxml_iriRules()).ifPresent(x -> b.rdfxml_iriRules(x));
			string(a.rdfxml_errorMode()).ifPresent(x -> b.rdfxml_errorMode(x));
			bool(a.rdfxml_embedding()).ifPresent(x -> b.rdfxml_embedding(x));
			string(a.rdfxml_xmlBase()).ifPresent(x -> b.rdfxml_xmlbase(x));
			bool(a.rdfxml_longId()).ifPresent(x -> b.rdfxml_longId(x));
			bool(a.rdfxml_allowBadUris()).ifPresent(x -> b.rdfxml_allowBadUris(x));
			string(a.rdfxml_relativeUris()).ifPresent(x -> b.rdfxml_relativeUris(x));
			string(a.rdfxml_showXmlDeclaration()).ifPresent(x -> b.rdfxml_showXmlDeclaration(x));
			bool(a.rdfxml_disableShowDoctypeDeclaration()).ifPresent(x -> b.rdfxml_disableShowDoctypeDeclaration(x));
			integer(a.rdfxml_tab(), "rdfxml_tab").ifPresent(x -> b.rdfxml_tab(x));
			string(a.rdfxml_attributeQuoteChar()).map(x -> x.charAt(0)).ifPresent(x -> b.rdfxml_attributeQuoteChar(x));
			string(a.rdfxml_blockRules()).ifPresent(x -> b.rdfxml_blockRules(x));
			integer(a.n3_minGap(), "n3_minGap").ifPresent(x -> b.n3_minGap(x));
			bool(a.n3_disableObjectLists()).ifPresent(x -> b.n3_disableObjectLists(x));
			integer(a.n3_subjectColumn(), "n3_subjectColumn").ifPresent(x -> b.n3_subjectColumn(x));
			integer(a.n3_propertyColumn(), "n3_propertyColumn").ifPresent(x -> b.n3_propertyColumn(x));
			integer(a.n3_indentProperty(), "n3_indentProperty").ifPresent(x -> b.n3_indentProperty(x));
			integer(a.n3_widePropertyLen(), "n3_widePropertyLen").ifPresent(x -> b.n3_widePropertyLen(x));
			bool(a.n3_disableAbbrevBaseUri()).ifPresent(x -> b.n3_disableAbbrevBaseUri(x));
			bool(a.n3_disableUsePropertySymbols()).ifPresent(x -> b.n3_disableUsePropertySymbols(x));
			bool(a.n3_disableUseTripleQuotedStrings()).ifPresent(x -> b.n3_disableUseTripleQuotedStrings(x));
			bool(a.n3_disableUseDoubles()).ifPresent(x -> b.n3_disableUseDoubles(x));
		}
	}
}