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

import static org.apache.juneau.internal.SystemEnv.env;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.MediaType;
import org.apache.juneau.internal.FluentSetter;
import org.apache.juneau.internal.FluentSetters;
import org.apache.juneau.jena.annotation.Rdf;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Parses RDF into POJOs.
 * {@review}
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 *
 * The following direct subclasses are provided for language-specific parsers:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link RdfXmlParser} - RDF/XML and RDF/XML-ABBREV.
 * 	<li>
 * 		{@link NTripleParser} - N-TRIPLE.
 * 	<li>
 * 		{@link TurtleParser} - TURTLE.
 * 	<li>
 * 		{@link N3Parser} - N3.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall-rdf}
 * </ul>
 */
public class RdfParser extends ReaderParser implements RdfMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ReaderParser.Builder {

		private static final Namespace
			DEFAULT_JUNEAU_NS = Namespace.of("j", "http://www.apache.org/juneau/"),
			DEFAULT_JUNEAUBP_NS = Namespace.of("jp", "http://www.apache.org/juneaubp/");

		boolean trimWhitespace, looseCollections;
		String language;
		Namespace juneauNs, juneauBpNs;
		RdfCollectionFormat collectionFormat;
		Map<String,Object> jenaSettings = new TreeMap<String,Object>();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			type(RdfParser.class);
			trimWhitespace = env("Rdf.trimWhitespace", false);
			looseCollections = env("Rdf.looseCollections", false);
			language = env("Rdf.language", "RDF/XML-ABBREV");
			collectionFormat = env("Rdf.collectionFormat", RdfCollectionFormat.DEFAULT);
			juneauNs = DEFAULT_JUNEAU_NS;
			juneauBpNs = DEFAULT_JUNEAUBP_NS;
			jenaSettings = new TreeMap<>();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(RdfParser copyFrom) {
			super(copyFrom);
			trimWhitespace = copyFrom.trimWhitespace;
			looseCollections = copyFrom.looseCollections;
			language = copyFrom.language;
			collectionFormat = copyFrom.collectionFormat;
			juneauNs = copyFrom.juneauNs;
			juneauBpNs = copyFrom.juneauBpNs;
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			trimWhitespace = copyFrom.trimWhitespace;
			looseCollections = copyFrom.looseCollections;
			language = copyFrom.language;
			collectionFormat = copyFrom.collectionFormat;
			juneauNs = copyFrom.juneauNs;
			juneauBpNs = copyFrom.juneauBpNs;
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public RdfParser build() {
			return (RdfParser)super.build();
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		@Override
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}

		Builder jena(String key, Object value) {
			jenaSettings.put(key, value);
			return this;
		}

		/**
		 * RDF/XML property: <c>iri_rules</c>.
		 *
		 * <p>
		 * Set the engine for checking and resolving.
		 *
		 * <p>
		 * Possible values:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"lax"</js> - The rules for RDF URI references only, which does permit spaces although the use of spaces
		 * 		is not good practice.
		 * 	<li>
		 * 		<js>"strict"</js> - Sets the IRI engine with rules for valid IRIs, XLink and RDF; it does not permit spaces
		 * 		in IRIs.
		 * 	<li>
		 * 		<js>"iri"</js> - Sets the IRI engine to IRI
		 * 		({@doc http://www.ietf.org/rfc/rfc3986.txt RFC 3986},
		 * 		{@doc http://www.ietf.org/rfc/rfc3987.txt RFC 3987}).
		 *
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * <p>
		 * Possible values:
		 * <ul>
		 * 	<li><js>"default"</js>
		 * 	<li><js>"lax"</js>
		 * 	<li><js>"strict"</js>
		 * 	<li><js>"strict-ignore"</js>
		 * 	<li><js>"strict-warning"</js>
		 * 	<li><js>"strict-error"</js>
		 * 	<li><js>"strict-fatal"</js>
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li>
		 * 		{@doc ExtARP/ARPOptions.html#setDefaultErrorMode() ARPOptions.setDefaultErrorMode()}
		 * 	<li>
		 * 		{@doc ExtARP/ARPOptions.html#setLaxErrorMode() ARPOptions.setLaxErrorMode()}
		 * 	<li>
		 * 		{@doc ExtARP/ARPOptions.html#setStrictErrorMode() ARPOptions.setStrictErrorMode()}
		 * 	<li>
		 * 		{@doc ExtARP/ARPOptions.html#setStrictErrorMode(int) ARPOptions.setStrictErrorMode(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_errorMode(String value) {
			return jena("rdfXml.error-mode", value);
		}

		/**
		 * RDF/XML ARP property: <c>embedding</c>.
		 *
		 * <p>
		 * Sets ARP to look for RDF embedded within an enclosing XML document.
		 *
		 * <ul class='seealso'>
		 * 	<li>
		 * 		{@doc ExtARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_embedding() {
			return rdfxml_embedding(true);
		}

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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_longId() {
			return rdfxml_longId(true);
		}

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
		 * @param value
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_allowBadUris() {
			return rdfxml_allowBadUris(true);
		}

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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_relativeUris(String value) {
			return jena("rdfXml.relativeURIs", value);
		}

		/**
		 * RDF/XML property: <c>showXmlDeclaration</c>.
		 *
		 * <p>
		 * Possible values:
		 * <ul class='spaced-list'>
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder rdfxml_disableShowDoctypeDeclaration() {
			return rdfxml_disableShowDoctypeDeclaration(true);
		}

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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * {@doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
		 * rules that will not be used.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3_disableObjectLists() {
			return n3_disableObjectLists(true);
		}

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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * 	The new value for this property.
		 * @return This object (for method chaining).
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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3_disableAbbrevBaseUri() {
			return n3_disableAbbrevBaseUri(true);
		}

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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3_disableUsePropertySymbols() {
			return n3_disableUsePropertySymbols(true);
		}

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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3_disableUseTripleQuotedStrings() {
			return n3_disableUseTripleQuotedStrings(true);
		}

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
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3_disableUseDoubles() {
			return n3_disableUseDoubles(true);
		}

		@FluentSetter
		public Builder n3_disableUseDoubles(boolean value) {
			return jena("n3.disableUseDoubles", value);
		}

		/**
		 * RDF format for representing collections and arrays.
		 *
		 * <p>
		 * Possible values:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
		 * 	<li>
		 * 		<js>"SEQ"</js> - RDF Sequence container.
		 * 	<li>
		 * 		<js>"BAG"</js> - RDF Bag container.
		 * 	<li>
		 * 		<js>"LIST"</js> - RDF List container.
		 * 	<li>
		 * 		<js>"MULTI_VALUED"</js> - Multi-valued properties.
		 * </ul>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_collectionFormat}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder collectionFormat(RdfCollectionFormat value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Default XML namespace for bean properties.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_juneauBpNs}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder juneauBpNs(Namespace value) {
			juneauBpNs = value;
			return this;
		}

		/**
		 * XML namespace for Juneau properties.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_juneauNs}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>.
		 * @return This object (for method chaining).
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
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * 	<li class='jm'>{@link org.apache.juneau.jena.Builder#n3()}
		 * 	<li class='jm'>{@link org.apache.juneau.jena.Builder#ntriple()}
		 * 	<li class='jm'>{@link org.apache.juneau.jena.Builder#turtle()}
		 * 	<li class='jm'>{@link org.apache.juneau.jena.Builder#xml()}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
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
		 * <p class='bcode w800'>
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
		 * 	List&lt;MyBean&gt; <jv>myList</jv> = createListOfMyBeans();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	String <jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>myList</jv>);
		 *
		 * 	<jc>// Parse back into a Java collection</jc>
		 * 	<jv>myList</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
		 *
		 * 	MyBean[] <jv>myBeans</jv> = createArrayOfMyBeans();
		 *
		 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
		 * 	<jv>rdfXml</jv> = <jv>serializer</jv>.serialize(<jv>myBeans</jv>);
		 *
		 * 	<jc>// Parse back into a bean array</jc>
		 * 	<jv>myBeans</jv> = <jv>parser</jv>.parse(<jv>rdfXml</jv>, MyBean[].<jk>class</jk>);
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_looseCollections}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder looseCollections() {
			return looseCollections(true);
		}

		@FluentSetter
		public Builder looseCollections(boolean value) {
			looseCollections = value;
			return this;
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder n3() {
			return language(Constants.LANG_N3);
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder ntriple() {
			return language(Constants.LANG_NTRIPLE);
		}

		/**
		 * Trim whitespace from text elements.
		 *
		 * <p>
		 * When enabled, whitespace in text elements will be automatically trimmed.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bcode w800'>
		 * 	<jc>// Create an RDF parser that trims whitespace.</jc>
		 * 	ReaderParser <jv>parser</jv> = RdfParser
		 * 		.<jsm>create</jsm>()
		 * 		.xml()
		 * 		.trimWhitespace()
		 * 		.build();
		 * </p>
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_trimWhitespace}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder trimWhitespace() {
			return trimWhitespace(true);
		}

		@FluentSetter
		public Builder trimWhitespace(boolean value) {
			trimWhitespace = value;
			return this;
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder turtle() {
			return language(Constants.LANG_TURTLE);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder xml() {
			return language(Constants.LANG_RDF_XML);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>.
		 *
		 * <ul class='seealso'>
		 * 	<li class='jf'>{@link RdfParser#RDF_language}
		 * </ul>
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder xmlabbrev() {
			return language(Constants.LANG_RDF_XML_ABBREV);
		}

		// <FluentSetters>

		@Override /* Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* Context.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Context.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Context.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder beanDictionary(Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder notBeanClasses(Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder swaps(Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - BeanContext.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder autoCloseStreams() {
			super.autoCloseStreams();
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder debugOutputLines(int value) {
			super.debugOutputLines(value);
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder strict() {
			super.strict();
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - Parser.Builder */
		public Builder unbuffered() {
			super.unbuffered();
			return this;
		}

		@Override /* GENERATED - ReaderParser.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - ReaderParser.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean trimWhitespace, looseCollections;
	final String language;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings;

	private final Map<ClassMeta<?>,RdfClassMeta> rdfClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,RdfBeanMeta> rdfBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,RdfBeanPropertyMeta> rdfBeanPropertyMetas = new ConcurrentHashMap<>();

	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	protected RdfParser(Builder builder) {
		super((Builder) builder.consumes(getConsumes(builder)));

		trimWhitespace = builder.trimWhitespace;
		looseCollections = builder.looseCollections;
		language = builder.language;
		juneauNs = builder.juneauNs;
		juneauBpNs = builder.juneauBpNs;
		collectionFormat = builder.collectionFormat;
		jenaSettings = new TreeMap<>(builder.jenaSettings);
	}

	private static String getConsumes(Builder builder) {
		if (builder.getConsumes() != null)
			return builder.getConsumes();
		switch(builder.language) {
			case "RDF/XML":
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

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public RdfParserSession.Builder createSession() {
		return RdfParserSession.create(this);
	}

	@Override /* Context */
	public RdfParserSession getSession() {
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
	 * @see #RDF_collectionFormat
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see #RDF_juneauBpNs
	 * @return
	 * 	Default XML namespace for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return juneauBpNs;
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see #RDF_juneauNs
	 * @return
	 * 	XML namespace for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * RDF language.
	 *
	 * @see #RDF_language
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return language;
	}

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see #RDF_looseCollections
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
	 * Trim whitespace from text elements.
	 *
	 * @see #RDF_trimWhitespace
	 * @return
	 * 	<jk>true</jk> if whitespace in text elements will be automatically trimmed.
	 */
	protected final boolean isTrimWhitespace() {
		return trimWhitespace;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RdfParser",
				OMap
					.create()
					.filtered()
					.a("trimWhitespace", trimWhitespace)
					.a("looseCollections", looseCollections)
					.a("language", language)
					.a("juneauNs", juneauNs)
					.a("juneauBpNs", juneauBpNs)
					.a("collectionFormat", collectionFormat)
			);
	}
}
