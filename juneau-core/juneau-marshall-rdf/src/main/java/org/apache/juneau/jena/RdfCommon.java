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

import java.util.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Configurable properties common to both the {@link RdfSerializer} and {@link RdfParser} classes.
 */
public interface RdfCommon {

	/**
	 * Property prefix.
	 */
	static final String PREFIX = "RdfCommon.";

	/**
	 * Maps RDF writer names to property prefixes that apply to them.
	 */
	static final Map<String,String> LANG_PROP_MAP = new AMap<String,String>()
		.append("RDF/XML","rdfXml.")
		.append("RDF/XML-ABBREV","rdfXml.")
		.append("N3","n3.")
		.append("N3-PP","n3.")
		.append("N3-PLAIN","n3.")
		.append("N3-TRIPLES","n3.")
		.append("TURTLE","n3.")
		.append("N-TRIPLE","ntriple.");

	/**
	 * Configuration property:  RDF language.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.language.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"RDF/XML-ABBREV"</js>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#language(String)}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#n3()}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#ntriple()}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#turtle()}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#xml()}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#xmlabbrev()}
	 * 			<li class='jm'>{@link RdfParserBuilder#language(String)}
	 * 			<li class='jm'>{@link RdfParserBuilder#n3()}
	 * 			<li class='jm'>{@link RdfParserBuilder#ntriple()}
	 * 			<li class='jm'>{@link RdfParserBuilder#turtle()}
	 * 			<li class='jm'>{@link RdfParserBuilder#xml()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * 	The RDF language to use.
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
	 * 		<code>com.hp.hpl.jena.n3.N3JenaWriter.writer</code>.
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
	 */
	public static final String RDF_language = PREFIX + "language.s";

	/**
	 * Configuration property:  XML namespace for Juneau properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.juneauNs.s"</js>
	 * 	<li><b>Data type:</b>  {@link Namespace}
	 * 	<li><b>Default:</b>  <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#juneauNs(Namespace)}
	 * 			<li class='jm'>{@link RdfParserBuilder#juneauNs(Namespace)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_juneauNs = PREFIX + "juneauNs.s";

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.juneauBpNs.s"</js>
	 * 	<li><b>Data type:</b>  {@link Namespace}
	 * 	<li><b>Default:</b>  <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#juneauBpNs(Namespace)}
	 * 			<li class='jm'>{@link RdfParserBuilder#juneauBpNs(Namespace)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_juneauBpNs = PREFIX + "juneauBpNs.s";

	/**
	 * Configuration property:  Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.useXmlNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#useXmlNamespaces(boolean)}
	 * 			<li class='jm'>{@link RdfParserBuilder#useXmlNamespaces(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * <br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 */
	public static final String RDF_useXmlNamespaces = PREFIX + "useXmlNamespaces.b";

	/**
	 * Configuration property:  RDF/XML property: <code>iri_rules</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.iri-rules.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"lax"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 */
	public static final String RDF_arp_iriRules = PREFIX + "jena.rdfXml.iri-rules.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>error-mode</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.error-mode.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"lax"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * <p>
	 * See also:
	 * <ul class='spaced-list'>
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
	public static final String RDF_arp_errorMode = PREFIX + "jena.rdfXml.error-mode.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>embedding</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.embedding.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Sets ARP to look for RDF embedded within an enclosing XML document.
	 *
	 * <p>
	 * See also:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
	 * </ul>
	 */
	public static final String RDF_arp_embedding = PREFIX + "jena.rdfXml.embedding.b";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>ERR_xxx</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.ERR_"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Provides fine-grained control over detected error conditions.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"EM_IGNORE"</js>
	 * 	<li><js>"EM_WARNING"</js>
	 * 	<li><js>"EM_ERROR"</js>
	 * 	<li><js>"EM_FATAL"</js>
	 * </ul>
	 *
	 * <p>
	 * See also:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@doc ARP/ARPErrorNumbers.html ARPErrorNumbers}
	 * 	<li>
	 * 		{@doc ARP/ARPOptions.html#setErrorMode(int,%20int) ARPOptions.setErrorMode(int, int)}
	 * </ul>
	 */
	public static final String RDF_arp_err_ = PREFIX + "jena.rdfXml.ERR_";

	/**
	 * Configuration property:  RDF/XML ARP property: <code>WARN_xxx</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.WARN_"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_warn_ = PREFIX + "jena.rdfXml.WARN_";

	/**
	 * RDF/XML ARP property: <code>IGN_xxx</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.IGN_"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_ign_ = PREFIX + "jena.rdfXml.IGN_";

	/**
	 * Configuration property:  RDF/XML property: <code>xmlbase</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.xmlbase.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 */
	public static final String RDF_rdfxml_xmlBase = PREFIX + "jena.rdfXml.xmlbase.s";

	/**
	 * Configuration property:  RDF/XML property: <code>longId</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.longId.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Whether to use long ID's for anon resources.
	 * Short ID's are easier to read, but can run out of memory on very large models.
	 */
	public static final String RDF_rdfxml_longId = PREFIX + "jena.rdfXml.longId.b";

	/**
	 * Configuration property:  RDF/XML property: <code>allowBadURIs</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.allowBadURIs.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
	 */
	public static final String RDF_rdfxml_allowBadUris = PREFIX + "jena.rdfXml.allowBadURIs.b";

	/**
	 * Configuration property:  RDF/XML property: <code>relativeURIs</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.relativeURIs.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"same-document, absolute, relative, parent"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 */
	public static final String RDF_rdfxml_relativeUris = PREFIX + "jena.rdfXml.relativeURIs.s";

	/**
	 * Configuration property:  RDF/XML property: <code>showXmlDeclaration</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.showXmlDeclaration.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"default"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 */
	public static final String RDF_rdfxml_showXmlDeclaration = PREFIX + "jena.rdfXml.showXmlDeclaration.s";

	/**
	 * Configuration property:  RDF/XML property: <code>showDoctypeDeclaration</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.showDoctypeDeclaration.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If true, an XML doctype declaration is included in the output.
	 * This declaration includes a <code>!ENTITY</code> declaration for each prefix mapping in the model, and any
	 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
	 * invocation.
	 */
	public static final String RDF_rdfxml_showDoctypeDeclaration = PREFIX + "jena.rdfXml.showDoctypeDeclaration.b";

	/**
	 * Configuration property:  RDF/XML property: <code>tab</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.tab.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>2</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The number of spaces with which to indent XML child elements.
	 */
	public static final String RDF_rdfxml_tab = PREFIX + "jena.rdfXml.tab.i";

	/**
	 * Configuration property:  RDF/XML property: <code>attributeQuoteChar</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.attributeQuoteChar.s"</js>
	 * 	<li><b>Data type:</b>  <code>Character</code>
	 * 	<li><b>Default:</b>  <js>'"'</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The XML attribute quote character.
	 */
	public static final String RDF_rdfxml_attributeQuoteChar = PREFIX + "jena.rdfXml.attributeQuoteChar.s";

	/**
	 * Configuration property:  RDF/XML property: <code>blockRules</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.rdfXml.blockRules.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>""</js>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * A list of <code>Resource</code> or a <code>String</code> being a comma separated list of fragment IDs from
	 * <{@doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
	 * rules that will not be used.
	 */
	public static final String RDF_rdfxml_blockRules = PREFIX + "jena.rdfXml.blockRules.s";

	/**
	 * Configuration property:  N3/Turtle property: <code>minGap</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.minGap.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>1</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Minimum gap between items on a line.
	 */
	public static final String RDF_n3_minGap = PREFIX + "jena.n3.minGap.i";

	/**
	 * Configuration property:  N3/Turtle property: <code>objectLists</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.objectLists.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Print object lists as comma separated lists.
	 */
	public static final String RDF_n3_objectLists = PREFIX + "jena.n3.objectLists.b";

	/**
	 * Configuration property:  N3/Turtle property: <code>subjectColumn</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.subjectColumn.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  indentProperty
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
	 */
	public static final String RDF_n3_subjectColumn = PREFIX + "jena.n3.subjectColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <code>propertyColumn</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.propertyColumn.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>8</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width of the property column.
	 */
	public static final String RDF_n3_propertyColumn = PREFIX + "jena.n3.propertyColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <code>indentProperty</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.indentProperty.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>6</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width to indent properties.
	 */
	public static final String RDF_n3_indentProperty = PREFIX + "jena.n3.indentProperty.i";

	/**
	 * Configuration property:  N3/Turtle property: <code>widePropertyLen</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.widePropertyLen.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>20</code>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width of the property column.
	 * Must be longer than <code>propertyColumn</code>.
	 */
	public static final String RDF_n3_widePropertyLen = PREFIX + "jena.n3.widePropertyLen.i";

	/**
	 * Configuration property:  N3/Turtle property: <code>abbrevBaseURI</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.abbrevBaseURI.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Control whether to use abbreviations <code>&lt;&gt;</code> or <code>&lt;#&gt;</code>.
	 */
	public static final String RDF_n3_abbrevBaseUri = PREFIX + "jena.n3.abbrevBaseURI.b";

	/**
	 * Configuration property:  N3/Turtle property: <code>usePropertySymbols</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.usePropertySymbols.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Control whether to use <code>a</code>, <code>=</code> and <code>=&gt;</code> in output
	 */
	public static final String RDF_n3_usePropertySymbols = PREFIX + "jena.n3.usePropertySymbols.b";

	/**
	 * Configuration property:  N3/Turtle property: <code>useTripleQuotedStrings</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.useTripleQuotedStrings.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allow the use of <code>"""</code> to delimit long strings.
	 */
	public static final String RDF_n3_useTripleQuotedStrings = PREFIX + "jena.n3.useTripleQuotedStrings.b";

	/**
	 * Configuration property:  N3/Turtle property: <code>useDoubles</code>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.jena.n3.useDoubles.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allow the use doubles as <code>123.456</code>.
	 */
	public static final String RDF_n3_useDoubles = PREFIX + "jena.n3.useDoubles.b";

	/**
	 * Configuration property:  RDF format for representing collections and arrays.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.collectionFormat.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"DEFAULT"</js>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#collectionFormat(RdfCollectionFormat)}
	 * 			<li class='jm'>{@link RdfParserBuilder#collectionFormat(RdfCollectionFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 */
	public static final String RDF_collectionFormat = PREFIX + "collectionFormat.s";

	/**
	 * Configuration property:  Collections should be serialized and parsed as loose collections.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Rdf.looseCollections.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#looseCollections(boolean)}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#looseCollections()}
	 * 			<li class='jm'>{@link RdfParserBuilder#looseCollections(boolean)}
	 * 			<li class='jm'>{@link RdfParserBuilder#looseCollections()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	 * 	WriterSerializer s = RdfSerializer.<jsm>create</jsm>().xmlabbrev().looseCollections(<jk>true</jk>).build();
	 * 	ReaderParser p = RdfParser.<jsm>create</jsm>().xml().looseCollections(<jk>true</jk>).build();
	 *
	 * 	List&lt;MyBean&gt; l = createListOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(l);
	 *
	 * 	<jc>// Parse back into a Java collection</jc>
	 * 	l = p.parse(rdfXml, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	MyBean[] b = createArrayOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(b);
	 *
	 * 	<jc>// Parse back into a bean array</jc>
	 * 	b = p.parse(rdfXml, MyBean[].<jk>class</jk>);
	 * </p>
	 */
	public static final String RDF_looseCollections = PREFIX + "looseCollections.b";
}
