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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.jena.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Annotation for specifying config properties defined in {@link XmlSerializer}, {@link XmlDocSerializer}, and {@link XmlParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface RdfConfig {

	//-------------------------------------------------------------------------------------------------------------------
	// RdfCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  RDF language.
	 *
	 * <p>
	 * 	The RDF language to use.
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
	 * 		<code>org.apache.jena.n3.N3JenaWriter.writer</code>.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfCommon#RDF_language}
	 * </ul>
	 */
	String language() default "";

	/**
	 * Configuration property:  XML namespace for Juneau properties.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfCommon#RDF_juneauNs}
	 * </ul>
	 */
	String juneauNs() default "";

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfCommon#RDF_juneauBpNs}
	 * </ul>
	 */
	String juneauBpNs() default "";

	/**
	 * Configuration property:  Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * <br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfCommon#RDF_useXmlNamespaces}
	 * </ul>
	 */
	String useXmlNamespaces() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>iri_rules</code>.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfCommon#RDF_arp_iriRules}
	 * </ul>
	 */
	String arp_iriRules() default "";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>error-mode</code>.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_arp_errorMode}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setDefaultErrorMode() ARPOptions.setDefaultErrorMode()}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setLaxErrorMode() ARPOptions.setLaxErrorMode()}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setStrictErrorMode() ARPOptions.setStrictErrorMode()}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setStrictErrorMode(int) ARPOptions.setStrictErrorMode(int)}
	 * </ul>
	 */
	String arp_errorMode() default "";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>embedding</code>.
	 *
	 * <p>
	 * Sets ARP to look for RDF embedded within an enclosing XML document.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_arp_embedding}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
	 * </ul>
	 */
	String arp_embedding() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>xmlbase</code>.
	 *
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_xmlBase}
	 * </ul>
	 */
	String rdfxml_xmlBase() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>longId</code>.
	 *
	 * <p>
	 * Whether to use long ID's for anon resources.
	 * Short ID's are easier to read, but can run out of memory on very large models.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_longId}
	 * </ul>
	 */
	String rdfxml_longId() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>allowBadURIs</code>.
	 *
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_allowBadUris}
	 * </ul>
	 */
	String rdfxml_allowBadUris() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>relativeURIs</code>.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_relativeUris}
	 * </ul>
	 */
	String rdfxml_relativeUris() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>showXmlDeclaration</code>.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"true"</js> - Add XML Declaration to the output.
	 * 	<li>
	 * 		<js>"false"</js> - Don't add XML Declaration to the output.
	 * 	<li>
	 * 		<js>"default"</js> - Only add an XML Declaration when asked to write to an <code>OutputStreamWriter</code>
	 * 		that uses some encoding other than <code>UTF-8</code> or <code>UTF-16</code>.
	 * 		In this case the encoding is shown in the XML declaration.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_showXmlDeclaration}
	 * </ul>
	 */
	String rdfxml_showXmlDeclaration() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>showDoctypeDeclaration</code>.
	 *
	 * <p>
	 * If true, an XML doctype declaration is included in the output.
	 * This declaration includes a <code>!ENTITY</code> declaration for each prefix mapping in the model, and any
	 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
	 * invocation.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_showDoctypeDeclaration}
	 * </ul>
	 */
	String rdfxml_showDoctypeDeclaration() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>tab</code>.
	 *
	 * <p>
	 * The number of spaces with which to indent XML child elements.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_tab}
	 * </ul>
	 */
	String rdfxml_tab() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>attributeQuoteChar</code>.
	 *
	 * <p>
	 * The XML attribute quote character.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_attributeQuoteChar}
	 * </ul>
	 */
	String rdfxml_attributeQuoteChar() default "";

	/**
	 * Configuration property:  RDF/XML property: <code>blockRules</code>.
	 *
	 * <p>
	 * A list of <code>Resource</code> or a <code>String</code> being a comma separated list of fragment IDs from
	 * {@doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
	 * rules that will not be used.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_rdfxml_blockRules}
	 * </ul>
	 */
	String rdfxml_blockRules() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>minGap</code>.
	 *
	 * <p>
	 * Minimum gap between items on a line.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_minGap}
	 * </ul>
	 */
	String n3_minGap() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>objectLists</code>.
	 *
	 * <p>
	 * Print object lists as comma separated lists.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_objectLists}
	 * </ul>
	 */
	String n3_objectLists() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>subjectColumn</code>.
	 *
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_subjectColumn}
	 * </ul>
	 */
	String n3_subjectColumn() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>propertyColumn</code>.
	 *
	 * <p>
	 * Width of the property column.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_propertyColumn}
	 * </ul>
	 */
	String n3_propertyColumn() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>indentProperty</code>.
	 *
	 * <p>
	 * Width to indent properties.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_indentProperty}
	 * </ul>
	 */
	String n3_indentProperty() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>widePropertyLen</code>.
	 *
	 * <p>
	 * Width of the property column.
	 * <br>Must be longer than <code>propertyColumn</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_widePropertyLen}
	 * </ul>
	 */
	String n3_widePropertyLen() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>abbrevBaseURI</code>.
	 *
	 * <p>
	 * Control whether to use abbreviations <code>&lt;&gt;</code> or <code>&lt;#&gt;</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_abbrevBaseUri}
	 * </ul>
	 */
	String n3_abbrevBaseUri() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>usePropertySymbols</code>.
	 *
	 * <p>
	 * Control whether to use <code>a</code>, <code>=</code> and <code>=&gt;</code> in output
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_usePropertySymbols}
	 * </ul>
	 */
	String n3_usePropertySymbols() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>useTripleQuotedStrings</code>.
	 *
	 * <p>
	 * Allow the use of <code>"""</code> to delimit long strings.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_useTripleQuotedStrings}
	 * </ul>
	 */
	String n3_useTripleQuotedStrings() default "";

	/**
	 * Configuration property:  N3/Turtle property: <code>useDoubles</code>.
	 *
	 * <p>
	 * Allow the use doubles as <code>123.456</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_n3_useDoubles}
	 * </ul>
	 */
	String n3_useDoubles() default "";

	/**
	 * Configuration property:  RDF format for representing collections and arrays.
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
	 * 		lost.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_collectionFormat}
	 * </ul>
	 */
	String collectionFormat() default "";

	/**
	 * Configuration property:  Collections should be serialized and parsed as loose collections.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfCommon#RDF_looseCollections}
	 * </ul>
	 */
	String looseCollections() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// RdfParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Trim whitespace from text elements.
	 *
	 * <p>
	 * If <js>"true"</js>, whitespace in text elements will be automatically trimmed.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfParser#RDF_trimWhitespace}
	 * </ul>
	 */
	String trimWhitespace() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// RdfSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link Serializer#SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addBeanTypes}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Add XSI data types to non-<code>String</code> literals.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addLiteralTypes}
	 * </ul>
	 */
	String addLiteralTypes() default "";

	/**
	 * Configuration property:  Add RDF root identifier property to root node.
	 *
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * to identify the root node in the graph.
	 * <br>This helps locate the root node during parsing.
	 *
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without incoming predicates to
	 * identify root notes, which can introduce a considerable performance degradation.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addRootProperty}
	 * </ul>
	 */
	String addRootProperty() default "";

	/**
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * <p>
	 * Detect namespace usage before serialization.
	 *
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_autoDetectNamespaces}
	 * </ul>
	 */
	String autoDetectNamespaces() default "";

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_namespaces}
	 * </ul>
	 */
	String[] namespaces() default {};
}
