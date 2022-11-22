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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJOs to RDF.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for language-specific serializers:
 * <ul>
 * 	<li>{@link RdfXmlSerializer} - RDF/XML.
 * 	<li>{@link RdfXmlAbbrevSerializer} - RDF/XML-ABBREV.
 * 	<li>{@link NTripleSerializer} - N-TRIPLE.
 * 	<li>{@link TurtleSerializer} - TURTLE.
 * 	<li>{@link N3Serializer} - N3.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfSerializer extends WriterSerializer implements RdfMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final Namespace[] EMPTY_NAMESPACE_ARRAY = new Namespace[0];

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,RdfSerializer> CACHE = Cache.of(HashKey.class, RdfSerializer.class).build();

		private static final Namespace
			DEFAULT_JUNEAU_NS = Namespace.of("j", "http://www.apache.org/juneau/"),
			DEFAULT_JUNEAUBP_NS = Namespace.of("jp", "http://www.apache.org/juneaubp/");

		boolean addBeanTypesRdf, addLiteralTypes, addRootProperty, disableAutoDetectNamespaces, disableUseXmlNamespaces, looseCollections;
		String language;
		Namespace juneauNs, juneauBpNs;
		RdfCollectionFormat collectionFormat;
		List<Namespace> namespaces;
		Map<String,Object> jenaSettings;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			addBeanTypesRdf = env("Rdf.addBeanTypesRdf", false);
			addLiteralTypes = env("Rdf.addLiteralTypes", false);
			addRootProperty = env("Rdf.addRootProperty", false);
			disableAutoDetectNamespaces = env("Rdf.disableAutoDetectNamespaces", false);
			disableUseXmlNamespaces = env("Rdf.disableUseXmlNamespaces", false);
			looseCollections = env("Rdf.looseCollections", false);
			language = env("Rdf.language", "RDF/XML-ABBREV");
			collectionFormat = env("Rdf.collectionFormat", RdfCollectionFormat.DEFAULT);
			juneauNs = DEFAULT_JUNEAU_NS;
			juneauBpNs = DEFAULT_JUNEAUBP_NS;
			namespaces = null;
			jenaSettings = new TreeMap<>();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(RdfSerializer copyFrom) {
			super(copyFrom);
			addBeanTypesRdf = copyFrom.addBeanTypesRdf;
			addLiteralTypes = copyFrom.addLiteralTypes;
			addRootProperty = copyFrom.addRootProperty;
			disableAutoDetectNamespaces = ! copyFrom.autoDetectNamespaces;
			disableUseXmlNamespaces = ! copyFrom.useXmlNamespaces;
			looseCollections = copyFrom.looseCollections;
			language = copyFrom.language;
			collectionFormat = copyFrom.collectionFormat;
			juneauNs = copyFrom.juneauNs;
			juneauBpNs = copyFrom.juneauBpNs;
			namespaces = copyFrom.namespaces.length == 0 ? null : list(copyFrom.namespaces);
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			addBeanTypesRdf = copyFrom.addBeanTypesRdf;
			addLiteralTypes = copyFrom.addLiteralTypes;
			addRootProperty = copyFrom.addRootProperty;
			disableAutoDetectNamespaces = copyFrom.disableAutoDetectNamespaces;
			disableUseXmlNamespaces = copyFrom.disableUseXmlNamespaces;
			looseCollections = copyFrom.looseCollections;
			language = copyFrom.language;
			collectionFormat = copyFrom.collectionFormat;
			juneauNs = copyFrom.juneauNs;
			juneauBpNs = copyFrom.juneauBpNs;
			namespaces = copyFrom.namespaces;
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public RdfSerializer build() {
			return cache(CACHE).build(RdfSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				addBeanTypesRdf,
				addLiteralTypes,
				addRootProperty,
				disableAutoDetectNamespaces,
				disableUseXmlNamespaces,
				looseCollections,
				language,
				collectionFormat,
				juneauNs,
				juneauBpNs,
				namespaces,
				jenaSettings
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		Builder jena(String key, Object value) {
			jenaSettings.put(key, value);
			return this;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypesRdf() {
			return addBeanTypesRdf(true);
		}

		/**
		 * Same as {@link #addBeanTypesRdf()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypesRdf(boolean value) {
			addBeanTypesRdf = value;
			return this;
		}

		/**
		 * RDF/XML property: <c>iri_rules</c>.
		 *
		 * <p>
		 * Set the engine for checking and resolving.
		 *
		 * <ul class='values spaced-list'>
		 * 	<li>
		 * 		<js>"lax"</js> - The rules for RDF URI references only, which does permit spaces although the use of spaces
		 * 		is not good practice.
		 * 	<li>
		 * 		<js>"strict"</js> - Sets the IRI engine with rules for valid IRIs, XLink and RDF; it does not permit spaces
		 * 		in IRIs.
		 * 	<li>
		 * 		<js>"iri"</js> - Sets the IRI engine to IRI
		 * 		({doc http://www.ietf.org/rfc/rfc3986.txt RFC 3986},
		 * 		{doc http://www.ietf.org/rfc/rfc3987.txt RFC 3987}).
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_iriRules(String value) {
			return jena("rdfXml.iri-rules", value);
		}

		/**
		 * RDF/XML ARP property: <c>error-mode</c>.
		 *
		 * <p>
		 * This allows a coarse-grained approach to control of error handling.
		 *
		 * <ul class='values'>
		 * 	<li><js>"default"</js>
		 * 	<li><js>"lax"</js>
		 * 	<li><js>"strict"</js>
		 * 	<li><js>"strict-ignore"</js>
		 * 	<li><js>"strict-warning"</js>
		 * 	<li><js>"strict-error"</js>
		 * 	<li><js>"strict-fatal"</js>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setDefaultErrorMode() ARPOptions.setDefaultErrorMode()}
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setLaxErrorMode() ARPOptions.setLaxErrorMode()}
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setStrictErrorMode() ARPOptions.setStrictErrorMode()}
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setStrictErrorMode(int) ARPOptions.setStrictErrorMode(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_errorMode(String value) {
			return jena("rdfXml.error-mode", value);
		}

		/**
		 * RDF/XML ARP property: <c>error-mode</c>.
		 *
		 * <p>
		 * Sets ARP to look for RDF embedded within an enclosing XML document.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_embedding() {
			return rdfxml_embedding(true);
		}

		/**
		 * RDF/XML ARP property: <c>error-mode</c>.
		 *
		 * <p>
		 * Sets ARP to look for RDF embedded within an enclosing XML document.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li>
		 * 		{doc ext.ARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_embedding(boolean value) {
			return jena("rdfXml.embedding", value);
		}

		/**
		 * RDF/XML property: <c>xmlbase</c>.
		 *
		 * <p>
		 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_xmlbase(String value) {
			return jena("rdfXml.xmlbase", value);
		}

		/**
		 * RDF/XML property: <c>longId</c>.
		 *
		 * <p>
		 * Whether to use long ID's for anon resources.
		 * Short ID's are easier to read, but can run out of memory on very large models.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_longId() {
			return rdfxml_longId(true);
		}

		/**
		 * RDF/XML property: <c>longId</c>.
		 *
		 * <p>
		 * Whether to use long ID's for anon resources.
		 * Short ID's are easier to read, but can run out of memory on very large models.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_longId(boolean value) {
			return jena("rdfXml.longId", value);
		}

		/**
		 * RDF/XML property: <c>allowBadURIs</c>.
		 *
		 * <p>
		 * URIs in the graph are, by default, checked prior to serialization.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_allowBadUris() {
			return rdfxml_allowBadUris(true);
		}

		/**
		 * RDF/XML property: <c>allowBadURIs</c>.
		 *
		 * <p>
		 * URIs in the graph are, by default, checked prior to serialization.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_allowBadUris(boolean value) {
			return jena("rdfXml.allowBadURIs", value);
		}

		/**
		 * RDF/XML property: <c>relativeURIs</c>.
		 *
		 * <p>
		 * What sort of relative URIs should be used.
		 *
		 * <p>
		 * A comma separate list of options:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"same-document"</js> - Same-document references (e.g. <js>""</js> or <js>"#foo"</js>)
		 * 	<li>
		 * 		<js>"network"</js>  - Network paths (e.g. <js>"//example.org/foo"</js> omitting the URI scheme)
		 * 	<li>
		 * 		<js>"absolute"</js> - Absolute paths (e.g. <js>"/foo"</js> omitting the scheme and authority)
		 * 	<li>
		 * 		<js>"relative"</js> - Relative path not beginning in <js>"../"</js>
		 * 	<li>
		 * 		<js>"parent"</js> - Relative path beginning in <js>"../"</js>
		 * 	<li>
		 * 		<js>"grandparent"</js> - Relative path beginning in <js>"../../"</js>
		 * </ul>
		 *
		 * <p>
		 * The default value is <js>"same-document, absolute, relative, parent"</js>.
		 * To switch off relative URIs use the value <js>""</js>.
		 * Relative URIs of any of these types are output where possible if and only if the option has been specified.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_relativeUris(String value) {
			return jena("rdfXml.relativeURIs", value);
		}

		/**
		 * RDF/XML property: <c>showXmlDeclaration</c>.
		 *
		 * <ul class='values spaced-list'>
		 * 	<li>
		 * 		<js>"true"</js> - Add XML Declaration to the output.
		 * 	<li>
		 * 		<js>"false"</js> - Don't add XML Declaration to the output.
		 * 	<li>
		 * 		<js>"default"</js> - Only add an XML Declaration when asked to write to an <c>OutputStreamWriter</c>
		 * 		that uses some encoding other than <c>UTF-8</c> or <c>UTF-16</c>.
		 * 		In this case the encoding is shown in the XML declaration.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_showXmlDeclaration(String value) {
			return jena("rdfXml.showXmlDeclaration", value);
		}

		/**
		 * RDF/XML property: <c>disableShowDoctypeDeclaration</c>.
		 *
		 * <p>
		 * If disabled, an XML doctype declaration isn't included in the output.
		 * This declaration includes a <c>!ENTITY</c> declaration for each prefix mapping in the model, and any
		 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
		 * invocation.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_disableShowDoctypeDeclaration() {
			return rdfxml_disableShowDoctypeDeclaration(true);
		}

		/**
		 * RDF/XML property: <c>disableShowDoctypeDeclaration</c>.
		 *
		 * <p>
		 * If disabled, an XML doctype declaration isn't included in the output.
		 * This declaration includes a <c>!ENTITY</c> declaration for each prefix mapping in the model, and any
		 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
		 * invocation.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_disableShowDoctypeDeclaration(boolean value) {
			return jena("rdfXml.disableShowDoctypeDeclaration", value);
		}

		/**
		 * RDF/XML property: <c>tab</c>.
		 *
		 * <p>
		 * The number of spaces with which to indent XML child elements.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_tab(int value) {
			return jena("rdfXml.tab", value);
		}

		/**
		 * RDF/XML property: <c>attributeQuoteChar</c>.
		 *
		 * <p>
		 * The XML attribute quote character.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_attributeQuoteChar(char value) {
			return jena("rdfXml.attributeQuoteChar", value);
		}

		/**
	 	 * RDF/XML property: <c>blockRules</c>.
		 *
		 * <p>
		 * A list of <c>Resource</c> or a <c>String</c> being a comma separated list of fragment IDs from
		 * {doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
		 * rules that will not be used.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rdfxml_blockRules(String value) {
			return jena("rdfXml.blockRules", value);
		}

		/**
		 * N3/Turtle property: <c>minGap</c>.
		 *
		 * <p>
		 * Minimum gap between items on a line.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_minGap(int value) {
			return jena("n3.minGap", value);
		}

		/**
		 * N3/Turtle property: <c>disableObjectLists</c>.
		 *
		 * <p>
		 * Don't print object lists as comma separated lists.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableObjectLists() {
			return n3_disableObjectLists(true);
		}

		/**
		 * N3/Turtle property: <c>disableObjectLists</c>.
		 *
		 * <p>
		 * Don't print object lists as comma separated lists.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableObjectLists(boolean value) {
			return jena("n3.disableObjectLists", value);
		}

		/**
		 * N3/Turtle property: <c>subjectColumn</c>.
		 *
		 * <p>
		 * If the subject is shorter than this value, the first property may go on the same line.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_subjectColumn(int value) {
			return jena("n3.subjectColumn", value);
		}

		/**
		 * N3/Turtle property: <c>propertyColumn</c>.
		 *
		 * <p>
		 * Width of the property column.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_propertyColumn(int value) {
			return jena("n3.propertyColumn", value);
		}

		/**
		 * N3/Turtle property: <c>indentProperty</c>.
		 *
		 * <p>
		 * Width to indent properties.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_indentProperty(int value) {
			return jena("n3.indentProperty", value);
		}

		/**
		 * N3/Turtle property: <c>widePropertyLen</c>.
		 *
		 * <p>
		 * Width of the property column.
		 * Must be longer than <c>propertyColumn</c>.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_widePropertyLen(int value) {
			return jena("n3.widePropertyLen", value);
		}

		/**
		 * N3/Turtle property: <c>disableAbbrevBaseURI</c>.
		 *
		 * <p>
		 * Controls whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableAbbrevBaseUri() {
			return n3_disableAbbrevBaseUri(true);
		}

		/**
		 * N3/Turtle property: <c>disableAbbrevBaseURI</c>.
		 *
		 * <p>
		 * Controls whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableAbbrevBaseUri(boolean value) {
			return jena("n3.disableAbbrevBaseUri", value);
		}

		/**
		 * N3/Turtle property: <c>disableUsePropertySymbols</c>.
		 *
		 * <p>
		 * Controls whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUsePropertySymbols() {
			return n3_disableUsePropertySymbols(true);
		}

		/**
		 * N3/Turtle property: <c>disableUsePropertySymbols</c>.
		 *
		 * <p>
		 * Controls whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUsePropertySymbols(boolean value) {
			return jena("n3.disableUsePropertySymbols", value);
		}

		/**
		 * N3/Turtle property: <c>disableUseTripleQuotedStrings</c>.
		 *
		 * <p>
		 * Disallow the use of <c>"""</c> to delimit long strings.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUseTripleQuotedStrings() {
			return n3_disableUseTripleQuotedStrings(true);
		}

		/**
		 * N3/Turtle property: <c>disableUseTripleQuotedStrings</c>.
		 *
		 * <p>
		 * Disallow the use of <c>"""</c> to delimit long strings.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUseTripleQuotedStrings(boolean value) {
			return jena("n3.disableUseTripleQuotedStrings", value);
		}

		/**
		 * N3/Turtle property: <c>disableUseDoubles</c>.
		 *
		 * <p>
		 * Disallow the use of doubles as <c>123.456</c>.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUseDoubles() {
			return n3_disableUseDoubles(true);
		}

		/**
		 * N3/Turtle property: <c>disableUseDoubles</c>.
		 *
		 * <p>
		 * Disallow the use of doubles as <c>123.456</c>.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3_disableUseDoubles(boolean value) {
			return jena("n3.disableUseDoubles", value);
		}

		/**
		 * Add XSI data types to non-<c>String</c> literals.
		 *
		 * <p>
		 * Shortcut for calling <code>addLiteralTypes(<jk>true</jk>)</code>.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addLiteralTypes() {
			return addLiteralTypes(true);
		}

		/**
		 * Add XSI data types to non-<c>String</c> literals.
		 *
		 * <p>
		 * Shortcut for calling <code>addLiteralTypes(<jk>true</jk>)</code>.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addLiteralTypes(boolean value) {
			addLiteralTypes = value;
			return this;
		}

		/**
		 * Add RDF root identifier property to root node.
		 *
		 * <p>
		 * When enabled an RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
		 * to identify the root node in the graph.
		 * <br>This helps locate the root node during parsing.
		 *
		 * <p>
		 * If disabled, the parser has to search through the model to find any resources without incoming predicates to
		 * identify root notes, which can introduce a considerable performance degradation.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addRootProperty() {
			return addRootProperty(true);
		}

		/**
		 * Add RDF root identifier property to root node.
		 *
		 * <p>
		 * When enabled an RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
		 * to identify the root node in the graph.
		 * <br>This helps locate the root node during parsing.
		 *
		 * <p>
		 * If disabled, the parser has to search through the model to find any resources without incoming predicates to
		 * identify root notes, which can introduce a considerable performance degradation.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addRootProperty(boolean value) {
			addRootProperty = value;
			return this;
		}

		/**
		 * Disable auto-detect namespace usage.
		 *
		 * <p>
		 * Don't detect namespace usage before serialization.
		 *
		 * <p>
		 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
		 * the root element is serialized.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableAutoDetectNamespaces() {
			return disableAutoDetectNamespaces(true);
		}

		/**
		 * Disable auto-detect namespace usage.
		 *
		 * <p>
		 * Don't detect namespace usage before serialization.
		 *
		 * <p>
		 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
		 * the root element is serialized.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableAutoDetectNamespaces(boolean value) {
			disableAutoDetectNamespaces = value;
			return this;
		}

		/**
		 * RDF format for representing collections and arrays.
		 *
		 * <ul class='values'>
		 * 	<li><js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
		 * 	<li><js>"SEQ"</js> - RDF Sequence container.
		 * 	<li><js>"BAG"</js> - RDF Bag container.
		 * 	<li><js>"LIST"</js> - RDF List container.
		 * 	<li><js>"MULTI_VALUED"</js> - Multi-valued properties.
		 * </ul>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
		 * 		lost.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder collectionFormat(RdfCollectionFormat value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Default XML namespace for bean properties.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder juneauBpNs(Namespace value) {
			juneauBpNs = value;
			return this;
		}

		/**
		 * XML namespace for Juneau properties.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder juneauNs(Namespace value) {
			juneauNs = value;
			return this;
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Can be any of the following:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"RDF/XML"</js>
		 * 	<li>
		 * 		<js>"RDF/XML-ABBREV"</js> (default)
		 * 	<li>
		 * 		<js>"N-TRIPLE"</js>
		 * 	<li>
		 * 		<js>"N3"</js> - General name for the N3 writer.
		 * 		Will make a decision on exactly which writer to use (pretty writer, plain writer or simple writer) when
		 * 		created.
		 * 		Default is the pretty writer but can be overridden with system property
		 * 		<c>org.apache.jena.n3.N3JenaWriter.writer</c>.
		 * 	<li>
		 * 		<js>"N3-PP"</js> - Name of the N3 pretty writer.
		 * 		The pretty writer uses a frame-like layout, with prefixing, clustering like properties and embedding
		 * 		one-referenced bNodes.
		 * 	<li>
		 * 		<js>"N3-PLAIN"</js> - Name of the N3 plain writer.
		 * 		The plain writer writes records by subject.
		 * 	<li>
		 * 		<js>"N3-TRIPLES"</js> - Name of the N3 triples writer.
		 * 		This writer writes one line per statement, like N-Triples, but does N3-style prefixing.
		 * 	<li>
		 * 		<js>"TURTLE"</js> -  Turtle writer.
		 * 		http://www.dajobe.org/2004/01/turtle/
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link #n3()}
		 * 	<li class='jm'>{@link #ntriple()}
		 * 	<li class='jm'>{@link #turtle()}
		 * 	<li class='jm'>{@link #xml()}
		 * 	<li class='jm'>{@link #xmlabbrev()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder language(String value) {
			language = value;
			return this;
		}

		/**
		 * Collections should be serialized and parsed as loose collections.
		 *
		 * <p>
		 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
		 * resources that are children of an RDF collection (e.g. Sequence, Bag).
		 *
		 * <p>
		 * Note that this setting is specialized for RDF syntax, and is incompatible with the concept of
		 * losslessly representing POJO models, since the tree structure of these POJO models are lost
		 * when serialized as loose collections.
		 *
		 * <p>
		 * This setting is typically only useful if the beans being parsed into do not have a bean property
		 * annotated with {@link Rdf#beanUri @Rdf(beanUri=true)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = RdfSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.xmlabbrev()
		 * 		.looseCollections()
		 * 		.build();
		 *
		 * 	ReaderParser <jv>parser</jv> = RdfParser
		 * 		.<jsm>create</jsm>()
		 * 		.xml()
		 * 		.looseCollections()
		 * 		.build();
		 *
		 * 	List&lt;MyBean&gt; <jv>list</jv> = <jsm>createListOfMyBeans</jsm>();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	String <jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>list</jv>);
		 *
		 * 	<jc>// Parse back into a Java collection</jc>
		 * 	<jv>list</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
		 *
		 * 	MyBean[] <jv>beans</jv> = <jsm>createArrayOfMyBeans</jsm>();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	<jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>beans</jv>);
		 *
		 * 	<jc>// Parse back into a bean array</jc>
		 * 	<jv>beans</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, MyBean[].<jk>class</jk>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder looseCollections() {
			return looseCollections(true);
		}

		/**
		 * Collections should be serialized and parsed as loose collections.
		 *
		 * <p>
		 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
		 * resources that are children of an RDF collection (e.g. Sequence, Bag).
		 *
		 * <p>
		 * Note that this setting is specialized for RDF syntax, and is incompatible with the concept of
		 * losslessly representing POJO models, since the tree structure of these POJO models are lost
		 * when serialized as loose collections.
		 *
		 * <p>
		 * This setting is typically only useful if the beans being parsed into do not have a bean property
		 * annotated with {@link Rdf#beanUri @Rdf(beanUri=true)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = RdfSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.xmlabbrev()
		 * 		.looseCollections()
		 * 		.build();
		 *
		 * 	ReaderParser <jv>parser</jv> = RdfParser
		 * 		.<jsm>create</jsm>()
		 * 		.xml()
		 * 		.looseCollections()
		 * 		.build();
		 *
		 * 	List&lt;MyBean&gt; <jv>list</jv> = <jsm>createListOfMyBeans</jsm>();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	String <jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>list</jv>);
		 *
		 * 	<jc>// Parse back into a Java collection</jc>
		 * 	<jv>list</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
		 *
		 * 	MyBean[] <jv>beans</jv> = <jsm>createArrayOfMyBeans</jsm>();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	<jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>beans</jv>);
		 *
		 * 	<jc>// Parse back into a bean array</jc>
		 * 	<jv>beans</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, MyBean[].<jk>class</jk>);
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder looseCollections(boolean value) {
			looseCollections = value;
			return this;
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder n3() {
			return language(Constants.LANG_N3);
		}

		/**
		 * Default namespaces.
		 *
		 * <p>
		 * The default list of namespaces associated with this serializer.
		 *
		 * @param values The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder namespaces(Namespace...values) {
			namespaces = addAll(namespaces, values);
			return this;
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ntriple() {
			return language(Constants.LANG_NTRIPLE);
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder turtle() {
			return language(Constants.LANG_TURTLE);
		}

		/**
		 * Disable reuse of XML namespaces when RDF namespaces not specified.
		 *
		 * <p>
		 * When enabled, namespaces defined using {@link XmlNs @XmlNs} and {@link org.apache.juneau.xml.annotation.Xml Xml} will not be
		 * inherited by the RDF serializers.
		 * Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableUseXmlNamespaces() {
			return disableUseXmlNamespaces(true);
		}

		/**
		 * Disable reuse of XML namespaces when RDF namespaces not specified.
		 *
		 * <p>
		 * When enabled, namespaces defined using {@link XmlNs @XmlNs} and {@link org.apache.juneau.xml.annotation.Xml Xml} will not be
		 * inherited by the RDF serializers.
		 * Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableUseXmlNamespaces(boolean value) {
			disableUseXmlNamespaces = value;
			return this;
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder xml() {
			return language(Constants.LANG_RDF_XML);
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder xmlabbrev() {
			return language(Constants.LANG_RDF_XML_ABBREV);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		addLiteralTypes,
		addRootProperty,
		useXmlNamespaces,
		looseCollections,
		autoDetectNamespaces,
		addBeanTypesRdf;

	final String language;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings;
	final Namespace[] namespaces;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,RdfClassMeta> rdfClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,RdfBeanMeta> rdfBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,RdfBeanPropertyMeta> rdfBeanPropertyMetas = new ConcurrentHashMap<>();

	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	public RdfSerializer(Builder builder) {
		super(builder.produces(getProduces(builder)).accept(getAccept(builder)));
		addLiteralTypes = builder.addLiteralTypes;
		addRootProperty = builder.addRootProperty;
		useXmlNamespaces = ! builder.disableUseXmlNamespaces;
		looseCollections = builder.looseCollections;
		autoDetectNamespaces = ! builder.disableAutoDetectNamespaces;
		language = builder.language;
		juneauNs = builder.juneauNs;
		juneauBpNs = builder.juneauBpNs;
		collectionFormat = builder.collectionFormat;
		namespaces = builder.namespaces != null ? builder.namespaces.toArray(EMPTY_NAMESPACE_ARRAY) : EMPTY_NAMESPACE_ARRAY;
		addBeanTypesRdf = builder.addBeanTypesRdf;
		jenaSettings = new TreeMap<>(builder.jenaSettings);

		addBeanTypes = addBeanTypesRdf || super.isAddBeanTypes();
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public  RdfSerializerSession.Builder createSession() {
		return RdfSerializerSession.create(this);
	}

	@Override /* Context */
	public RdfSerializerSession getSession() {
		return createSession().build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RdfMetaProvider */
	public RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		RdfClassMeta m = rdfClassMetas.get(cm);
		if (m == null) {
			m = new RdfClassMeta(cm, this);
			rdfClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* RdfMetaProvider */
	public RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		RdfBeanMeta m = rdfBeanMetas.get(bm);
		if (m == null) {
			m = new RdfBeanMeta(bm, this);
			rdfBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* RdfMetaProvider */
	public RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		RdfBeanPropertyMeta m = rdfBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new RdfBeanPropertyMeta(bpm.getDelegateFor(), this);
			rdfBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		XmlClassMeta m = xmlClassMetas.get(cm);
		if (m == null) {
			m = new XmlClassMeta(cm, this);
			xmlClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		XmlBeanMeta m = xmlBeanMetas.get(bm);
		if (m == null) {
			m = new XmlBeanMeta(bm, this);
			xmlBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		XmlBeanPropertyMeta m = xmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new XmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			xmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Common properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see RdfSerializer.Builder#collectionFormat(RdfCollectionFormat)
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see RdfSerializer.Builder#juneauBpNs(Namespace)
	 * @return
	 * 	The XML namespace to use for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return juneauBpNs;
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see RdfSerializer.Builder#juneauNs(Namespace)
	 * @return
	 * 	The XML namespace to use for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * RDF language.
	 *
	 * @see RdfSerializer.Builder#language(String)
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return language;
	}

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see RdfSerializer.Builder#looseCollections()
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() {
		return looseCollections;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Jena properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() {
		return jenaSettings;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see RdfSerializer.Builder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add XSI data types to non-<c>String</c> literals.
	 *
	 * @see RdfSerializer.Builder#addLiteralTypes()
	 * @return
	 * 	<jk>true</jk> if XSI data types should be added to string literals.
	 */
	protected final boolean isAddLiteralTypes() {
		return addLiteralTypes;
	}

	/**
	 * Add RDF root identifier property to root node.
	 *
	 * @see RdfSerializer.Builder#addRootProperty()
	 * @return
	 * 	<jk>true</jk> if RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 */
	protected final boolean isAddRootProp() {
		return addRootProperty;
	}

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see RdfSerializer.Builder#disableAutoDetectNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespaces usage should be detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Default namespaces.
	 *
	 * @see RdfSerializer.Builder#namespaces(Namespace...)
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	public Namespace[] getNamespaces() {
		return namespaces;
	}

	/**
	 * Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * @see RdfSerializer.Builder#disableUseXmlNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * 	<br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 */
	protected final boolean isUseXmlNamespaces() {
		return useXmlNamespaces;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private static String getProduces(Builder builder) {
		if (builder.getProduces() != null)
			return builder.getProduces();
		switch(builder.language) {
			case "RDF/XML": return "text/xml+rdf+abbrev";
			case "RDF/XML-ABBREV": return "text/xml+rdf";
			case "N-TRIPLE": return "text/n-triple";
			case "N3": return "text/n3";
			case "N3-PP": return "text/n3-pp";
			case "N3-PLAIN": return "text/n3-plain";
			case "N3-TRIPLES": return "text/n3-triples";
			case "TURTLE": return "text/turtle";
			default: return "text/xml+rdf";
		}
	}

	private static String getAccept(Builder builder) {
		if (builder.getAccept() != null)
			return builder.getAccept();
		switch(builder.language) {
			case "RDF/XML": return "text/xml+rdf+abbrev";
			case "RDF/XML-ABBREV": return "text/xml+rdf+abbrev,text/xml+rdf;q=0.9";
			case "N-TRIPLE": return "text/n-triple";
			case "N3": return "text/n3";
			case "N3-PP": return "text/n3-pp";
			case "N3-PLAIN": return "text/n3-plain";
			case "N3-TRIPLES": return "text/n3-triples";
			case "TURTLE": return "text/turtle";
			default: return "text/xml+rdf";
		}
	}

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("addLiteralTypes", addLiteralTypes)
			.append("addRootProperty", addRootProperty)
			.append("useXmlNamespaces", useXmlNamespaces)
			.append("looseCollections", looseCollections)
			.append("autoDetectNamespaces", autoDetectNamespaces)
			.append("language", language)
			.append("juneauNs", juneauNs)
			.append("juneauBpNs", juneauBpNs)
			.append("collectionFormat", collectionFormat)
			.append("namespaces", namespaces)
			.append("addBeanTypes", addBeanTypes);
	}
}
