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

import static org.apache.juneau.jena.RdfCommon.*;
import static org.apache.juneau.jena.RdfSerializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Builder class for building instances of RDF serializers.
 * {@review}
 */
@FluentSetters
public class RdfSerializerBuilder extends WriterSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	protected RdfSerializerBuilder() {
		super();
		contextClass(RdfSerializer.class);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected RdfSerializerBuilder(RdfSerializer copyFrom) {
		super(copyFrom);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected RdfSerializerBuilder(RdfSerializerBuilder copyFrom) {
		super(copyFrom);
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder copy() {
		return new RdfSerializerBuilder(this);
	}

	@Override /* ContextBuilder */
	public RdfSerializer build() {
		return (RdfSerializer)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder juneauNs(String value) {
		return set(RDF_juneauNs, value);
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder juneauBpNs(String value) {
		return set(RDF_juneauBpNs, value);
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
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder arp_iriRules(String value) {
		return set(RDF_arp_iriRules, value);
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
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder arp_errorMode(String value) {
		return set(RDF_arp_errorMode, value);
	}

	/**
	 * RDF/XML ARP property: <c>error-mode</c>.
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
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder arp_embedding() {
		return set(RDF_arp_embedding);
	}

	/**
	 * RDF/XML property: <c>xmlbase</c>.
	 *
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_xmlBase(String value) {
		return set(RDF_rdfxml_xmlBase, value);
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
	public RdfSerializerBuilder rdfxml_longId() {
		return set(RDF_rdfxml_longId);
	}

	/**
	 * RDF/XML property: <c>allowBadURIs</c>.
	 *
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_allowBadUris() {
		return set(RDF_rdfxml_allowBadUris);
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_relativeUris(String value) {
		return set(RDF_rdfxml_relativeUris, value);
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
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_showXmlDeclaration(String value) {
		return set(RDF_rdfxml_showXmlDeclaration, value);
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
	public RdfSerializerBuilder rdfxml_disableShowDoctypeDeclaration() {
		return set(RDF_rdfxml_disableShowDoctypeDeclaration);
	}

	/**
	 * RDF/XML property: <c>tab</c>.
	 *
	 * <p>
	 * The number of spaces with which to indent XML child elements.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_tab(int value) {
		return set(RDF_rdfxml_tab, value);
	}

	/**
	 * RDF/XML property: <c>attributeQuoteChar</c>.
	 *
	 * <p>
	 * The XML attribute quote character.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_attributeQuoteChar(String value) {
		return set(RDF_rdfxml_attributeQuoteChar, value);
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
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder rdfxml_blockRules(String value) {
		return set(RDF_rdfxml_blockRules, value);
	}

	/**
	 * N3/Turtle property: <c>minGap</c>.
	 *
	 * <p>
	 * Minimum gap between items on a line.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3_minGap(int value) {
		return set(RDF_n3_minGap, value);
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
	public RdfSerializerBuilder n3_disableObjectLists() {
		return set(RDF_n3_disableObjectLists);
	}

	/**
	 * N3/Turtle property: <c>subjectColumn</c>.
	 *
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3_subjectColumn(int value) {
		return set(RDF_n3_subjectColumn, value);
	}

	/**
	 * N3/Turtle property: <c>propertyColumn</c>.
	 *
	 * <p>
	 * Width of the property column.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3_propertyColumn(int value) {
		return set(RDF_n3_propertyColumn, value);
	}

	/**
	 * N3/Turtle property: <c>indentProperty</c>.
	 *
	 * <p>
	 * Width to indent properties.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3_indentProperty(int value) {
		return set(RDF_n3_indentProperty, value);
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3_widePropertyLen(int value) {
		return set(RDF_n3_widePropertyLen, value);
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
	public RdfSerializerBuilder n3_disableAbbrevBaseUri() {
		return set(RDF_n3_disableAbbrevBaseUri);
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
	public RdfSerializerBuilder n3_disableUsePropertySymbols() {
		return set(RDF_n3_disableUsePropertySymbols);
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
	public RdfSerializerBuilder n3_disableUseTripleQuotedStrings() {
		return set(RDF_n3_disableUseTripleQuotedStrings);
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
	public RdfSerializerBuilder n3_disableUseDoubles() {
		return set(RDF_n3_disableUseDoubles);
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
	 * 		lost.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder collectionFormat(String value) {
		return set(RDF_collectionFormat, value);
	}

	/**
	 * Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder namespaces(String[] value) {
		return set(RDF_namespaces, value);
	}

	/**
	 * Add XSI data types to non-<c>String</c> literals.
	 *
	 * <p>
	 * Shortcut for calling <code>addLiteralTypes(<jk>true</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder addLiteralTypes() {
		return set(RDF_addLiteralTypes);
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addRootProperty}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder addRootProperty() {
		return set(RDF_addRootProperty);
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_disableAutoDetectNamespaces}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder disableAutoDetectNamespaces() {
		return set(RDF_disableAutoDetectNamespaces);
	}

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * <p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
	 * 		lost.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_collectionFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li><js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
	 * 		<li><js>"SEQ"</js> - RDF Sequence container.
	 * 		<li><js>"BAG"</js> - RDF Bag container.
	 * 		<li><js>"LIST"</js> - RDF List container.
	 * 		<li><js>"MULTI_VALUED"</js> - Multi-valued properties.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder collectionFormat(RdfCollectionFormat value) {
		return set(RDF_collectionFormat, value);
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_juneauBpNs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder juneauBpNs(Namespace value) {
		return set(RDF_juneauBpNs, value);
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_juneauNs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder juneauNs(Namespace value) {
		return set(RDF_juneauNs, value);
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
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * 	<li class='jm'>{@link #n3()}
	 * 	<li class='jm'>{@link #ntriple()}
	 * 	<li class='jm'>{@link #turtle()}
	 * 	<li class='jm'>{@link #xml()}
	 * 	<li class='jm'>{@link #xmlabbrev()}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder language(String value) {
		return set(RDF_language, value);
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
	 * 	<li class='jf'>{@link RdfSerializer#RDF_looseCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder looseCollections() {
		return set(RDF_looseCollections);
	}

	/**
	 * RDF language.
	 *
	 * <p>
	 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder n3() {
		return language(Constants.LANG_N3);
	}

	/**
	 * Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_namespaces}
	 * </ul>
	 *
	 * @param values The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder namespaces(Namespace...values) {
		return set(RDF_namespaces, values);
	}

	/**
	 * RDF language.
	 *
	 * <p>
	 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder ntriple() {
		return language(Constants.LANG_NTRIPLE);
	}

	/**
	 * RDF language.
	 *
	 * <p>
	 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder turtle() {
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_disableUseXmlNamespaces}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder disableUseXmlNamespaces() {
		return set(RDF_disableUseXmlNamespaces);
	}

	/**
	 * RDF language.
	 *
	 * <p>
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder xml() {
		return language(Constants.LANG_RDF_XML);
	}

	/**
	 * RDF language.
	 *
	 * <p>
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RdfSerializerBuilder xmlabbrev() {
		return language(Constants.LANG_RDF_XML_ABBREV);
	}

	// <FluentSetters>

	@Override
	public RdfSerializerBuilder produces(String value) {
		super.produces(value);
		return this;
	}

	@Override
	public RdfSerializerBuilder accept(String value) {
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder apply(List<AnnotationWork> work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RdfSerializerBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RdfSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RdfSerializerBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RdfSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public RdfSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public RdfSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public RdfSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public RdfSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public RdfSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public RdfSerializerBuilder ws() {
		super.ws();
		return this;
	}

	// </FluentSetters>
}
