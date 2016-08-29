/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.jena;

import java.util.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Configurable properties common to both the {@link RdfSerializer} and {@link RdfParser} classes.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings("serial")
public interface RdfCommonContext {
// TODO - RENAME?
	/**
	 * Maps RDF writer names to property prefixes that apply to them.
	 */
	final static Map<String,String> LANG_PROP_MAP = new HashMap<String,String>() {{
		put("RDF/XML","rdfXml.");
		put("RDF/XML-ABBREV","rdfXml.");
		put("N3","n3.");
		put("N3-PP","n3.");
		put("N3-PLAIN","n3.");
		put("N3-TRIPLES","n3.");
		put("TURTLE","n3.");
		put("N-TRIPLE","ntriple.");
	}};

	/**
	 * The RDF language to serialize to ({@link String}, default=<js>"RDF/XML-ABBREV"</js>).
	 * <p>
	 * 	Can be any of the following:
	 * <ul class='spaced-list'>
	 * 	<li><js>"RDF/XML"</js>
	 * 	<li><js>"RDF/XML-ABBREV"</js>
	 * 	<li><js>"N-TRIPLE"</js>
	 * 	<li><js>"N3"</js> - General name for the N3 writer.
	 * 		Will make a decision on exactly which writer to use (pretty writer, plain writer or simple writer) when created.
	 * 		Default is the pretty writer but can be overridden with system property	<code>com.hp.hpl.jena.n3.N3JenaWriter.writer</code>.
	 * 	<li><js>"N3-PP"</js> - Name of the N3 pretty writer.
	 * 		The pretty writer uses a frame-like layout, with prefixing, clustering like properties and embedding one-referenced bNodes.
	 * 	<li><js>"N3-PLAIN"</js> - Name of the N3 plain writer.
	 * 		The plain writer writes records by subject.
	 * 	<li><js>"N3-TRIPLES"</js> - Name of the N3 triples writer.
	 * 		This writer writes one line per statement, like N-Triples, but does N3-style prefixing.
	 * 	<li><js>"TURTLE"</js> -  Turtle writer.
	 * 		http://www.dajobe.org/2004/01/turtle/
	 * </ul>
	 */
	public static final String RDF_language = "Rdf.language";

	/**
	 * The XML namespace for Juneau properties ({@link Namespace}, default=<js>{j:'http://www.ibm.com/juneau/'}</js>).
	 */
	public static final String RDF_juneauNs = "Rdf.juneauNs";

	/**
	 * The default XML namespace for bean properties ({@link Namespace}, default=<js>{j:'http://www.ibm.com/juneaubp/'}</js>).
	 */
	public static final String RDF_juneauBpNs = "Rdf.juneauBpNs";

	/**
	 * Reuse XML namespaces when RDF namespaces not specified ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	When specified, namespaces defined using {@link XmlNs} and {@link Xml} will be inherited by the RDF serializers.
	 * 	Otherwise, namespaces will be defined using {@link RdfNs} and {@link Rdf}.
	 */
	public static final String RDF_useXmlNamespaces = "Rdf.useXmlNamespaces";

	/**
	 * RDF/XML property: <code>iri_rules</code> ({@link String}, default=<js>"lax"</js>).
	 * <p>
	 *  	Set the engine for checking and resolving.
	 * <p>
	 * 	Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"lax"</js> - The rules for RDF URI references only, which does permit spaces although the use of spaces is not good practice.
	 * 	<li><js>"strict"</js> - Sets the IRI engine with rules for valid IRIs, XLink and RDF; it does not permit spaces in IRIs.
	 * 	<li><js>"iri"</js> - Sets the IRI engine to IRI (<a href='http://www.ietf.org/rfc/rfc3986.txt'>RFC 3986</a>, <a href='http://www.ietf.org/rfc/rfc3987.txt'>RFC 3987</a>).
	 * </ul>
	 */
	public static final String RDF_arp_iriRules = "Rdf.jena.rdfXml.iri-rules";

	/**
	 * RDF/XML ARP property: <code>error-mode</code> ({@link String}, default=<js>"lax"</js>).
	 * <p>
	 * 	This allows a coarse-grained approach to control of error handling.
	 * <p>
	 * 	Possible values:
	 * <ul>
	 * 	<li><js>"default"</js>
	 * 	<li><js>"lax"</js>
	 * 	<li><js>"strict"</js>
	 * 	<li><js>"strict-ignore"</js>
	 * 	<li><js>"strict-warning"</js>
	 * 	<li><js>"strict-error"</js>
	 * 	<li><js>"strict-fatal"</js>
	 * </ul>
	 * <p>
	 * 	See also:
	 * <ul class='spaced-list'>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setDefaultErrorMode()'>ARPOptions.setDefaultErrorMode()</a>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setLaxErrorMode()'>ARPOptions.setLaxErrorMode()</a>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setStrictErrorMode()'>ARPOptions.setStrictErrorMode()</a>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setStrictErrorMode(int)'>ARPOptions.setStrictErrorMode(int)</a>
	 * </ul>
	 */
	public static final String RDF_arp_errorMode = "Rdf.jena.rdfXml.error-mode";

	/**
	 * RDF/XML ARP property: <code>embedding</code> ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * 	Sets ARP to look for RDF embedded within an enclosing XML document.
	 * <p>
	 * 	See also:
	 * <ul class='spaced-list'>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setEmbedding(boolean)'>ARPOptions.setEmbedding(boolean)</a>
	 * </ul>
	 */
	public static final String RDF_arp_embedding = "Rdf.jena.rdfXml.embedding";

	/**
	 * RDF/XML ARP property: <code>ERR_xxx</code> ({@link String}).
	 * <p>
	 * 	Provides fine-grained control over detected error conditions.
	 * <p>
	 * 	Possible values:
	 * <ul>
	 * 	<li><js>"EM_IGNORE"</js>
	 * 	<li><js>"EM_WARNING"</js>
	 * 	<li><js>"EM_ERROR"</js>
	 * 	<li><js>"EM_FATAL"</js>
	 * </ul>
	 * <p>
	 * 	See also:
	 * <ul class='spaced-list'>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPErrorNumbers.html'>ARPErrorNumbers</a>
	 * 	<li><a href='http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/ARPOptions.html#setErrorMode(int,%20int)'>ARPOptions.setErrorMode(int, int)</a>
	 * </ul>
	 */
	public static final String RDF_arp_err_ = "Rdf.jena.rdfXml.ERR_";

	/**
	 * RDF/XML ARP property: <code>WARN_xxx</code> ({@link String}).
	 * <p>
	 * 	See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_warn_ = "Rdf.jena.rdfXml.WARN_";

	/**
	 * RDF/XML ARP property: <code>IGN_xxx</code> ({@link String}).
	 * <p>
	 * 	See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_ign_ = "Rdf.jena.rdfXml.IGN_";

	/**
	 * RDF/XML property: <code>xmlbase</code> ({@link String}, default=<jk>null</jk>).
	 * <p>
	 * 	The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 */
	public static final String RDF_rdfxml_xmlBase = "Rdf.jena.rdfXml.xmlbase";

	/**
	 * RDF/XML property: <code>longId</code> ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 *  	Whether to use long ID's for anon resources.
	 *  	Short ID's are easier to read, but can run out of memory on very large models.
	 */
	public static final String RDF_rdfxml_longId = "Rdf.jena.rdfXml.longId";

	/**
	 * RDF/XML property: <code>allowBadURIs</code> ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * 	URIs in the graph are, by default, checked prior to serialization.
	 */
	public static final String RDF_rdfxml_allowBadUris = "Rdf.jena.rdfXml.allowBadURIs";

	/**
	 * RDF/XML property: <code>relativeURIs</code> ({@link String}).
	 * <p>
	 * 	What sort of relative URIs should be used.
	 * <p>
	 * 	A comma separate list of options:
	 * <ul class='spaced-list'>
	 * 	<li><js>"same-document"</js> - Same-document references (e.g. <js>""</js> or <js>"#foo"</js>)
	 * 	<li><js>"network"</js>  - Network paths (e.g. <js>"//example.org/foo"</js> omitting the URI scheme)
	 * 	<li><js>"absolute"</js> - Absolute paths (e.g. <js>"/foo"</js> omitting the scheme and authority)
	 * 	<li><js>"relative"</js> - Relative path not begining in <js>"../"</js>
	 * 	<li><js>"parent"</js> - Relative path begining in <js>"../"</js>
	 * 	<li><js>"grandparent"</js> - Relative path begining in <js>"../../"</js>
	 * </ul>
	 * <p>
	 * 	The default value is <js>"same-document, absolute, relative, parent"</js>.
	 * 	To switch off relative URIs use the value <js>""</js>.
	 * 	Relative URIs of any of these types are output where possible if and only if the option has been specified.
	 */
	public static final String RDF_rdfxml_relativeUris = "Rdf.jena.rdfXml.relativeURIs";

	/**
	 * RDF/XML property: <code>showXmlDeclaration</code> ({@link String}, default=<js>"default"</js>).
	 * <p>
	 * 	Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"true"</js> - Add XML Declaration to the output.
	 * 	<li><js>"false"</js> - Don't add XML Declaration to the output.
	 * 	<li><js>"default"</js> - Only add an XML Declaration when asked to write to an <code>OutputStreamWriter</code> that uses some encoding other than <code>UTF-8</code> or <code>UTF-16</code>.
	 * 		In this case the encoding is shown in the XML declaration.
	 * </ul>
	 */
	public static final String RDF_rdfxml_showXmlDeclaration = "Rdf.jena.rdfXml.showXmlDeclaration";

	/**
	 * RDF/XML property: <code>showDoctypeDeclaration</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	If true, an XML Doctype declaration is included in the output.
	 * 	This declaration includes a <code>!ENTITY</code> declaration for each prefix mapping in the model, and any attribute value that starts with the URI of that mapping is written as starting with the corresponding entity invocation.
	 */
	public static final String RDF_rdfxml_showDoctypeDeclaration = "Rdf.jena.rdfXml.showDoctypeDeclaration";

	/**
	 * RDF/XML property: <code>tab</code> ({@link Integer}, default=<code>2</code>).
	 * <p>
	 * 	The number of spaces with which to indent XML child elements.
	 */
	public static final String RDF_rdfxml_tab = "Rdf.jena.rdfXml.tab";

	/**
	 * RDF/XML property: <code>attributeQuoteChar</code> ({@link Character}, default=<js>'"'</js>).
	 * <p>
	 * 	The XML attribute quote character.
	 */
	public static final String RDF_rdfxml_attributeQuoteChar = "Rdf.jena.rdfXml.attributeQuoteChar";

	/**
	 * RDF/XML property: <code>blockRules</code> ({@link String}, default=<js>""</js>).
	 * <p>
	 * 	A list of <code>Resource</code> or a <code>String</code> being a comma separated list of fragment IDs from <a href='http://www.w3.org/TR/rdf-syntax-grammar'>RDF Syntax Grammar</a> indicating grammar rules that will not be used.
	 */
	public static final String RDF_rdfxml_blockRules = "Rdf.jena.rdfXml.blockRules";

	/**
	 * N3/Turtle property: <code>minGap</code> ({@link Integer}, default=<code>1</code>).
	 * <p>
	 * 	Minimum gap between items on a line.
	 */
	public static final String RDF_n3_minGap = "Rdf.jena.n3.minGap";

	/**
	 * N3/Turtle property: <code>objectLists</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	Print object lists as comma separated lists.
	 */
	public static final String RDF_n3_objectLists = "Rdf.jena.n3.objectLists";

	/**
	 * N3/Turtle property: <code>subjectColumn</code> ({@link Integer}, default=indentProperty).
	 * <p>
	 * 	If the subject is shorter than this value, the first property may go on the same line.
	 */
	public static final String RDF_n3_subjectColumn = "Rdf.jena.n3.subjectColumn";

	/**
	 * N3/Turtle property: <code>propertyColumn</code> ({@link Integer}, default=<code>8</code>).
	 * <p>
	 *  	Width of the property column.
	 */
	public static final String RDF_n3_propertyColumn = "Rdf.jena.n3.propertyColumn";

	/**
	 * N3/Turtle property: <code>indentProperty</code> ({@link Integer}, default=<code>6</code>).
	 * <p>
	 * 	Width to indent properties.
	 */
	public static final String RDF_n3_indentProperty = "Rdf.jena.n3.indentProperty";

	/**
	 * N3/Turtle property: <code>widePropertyLen</code> ({@link Integer}, default=<code>20</code>).
	 * <p>
	 * 	Width of the property column.
	 * 	Must be longer than <code>propertyColumn</code>.
	 */
	public static final String RDF_n3_widePropertyLen = "Rdf.jena.n3.widePropertyLen";

	/**
	 * N3/Turtle property: <code>abbrevBaseURI</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	Control whether to use abbreviations <code>&lt;&gt;</code> or <code>&lt;#&gt;</code>.
	 */
	public static final String RDF_n3_abbrevBaseUri = "Rdf.jena.n3.abbrevBaseURI";

	/**
	 * N3/Turtle property: <code>usePropertySymbols</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	Control whether to use <code>a</code>, <code>=</code> and <code>=&gt;</code> in output
	 */
	public static final String RDF_n3_usePropertySymbols = "Rdf.jena.n3.usePropertySymbols";

	/**
	 * N3/Turtle property: <code>useTripleQuotedStrings</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	Allow the use of <code>"""</code> to delimit long strings.
	 */
	public static final String RDF_n3_useTripleQuotedStrings = "Rdf.jena.n3.useTripleQuotedStrings";

	/**
	 * N3/Turtle property: <code>useDoubles</code> ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * 	Allow the use doubles as <code>123.456</code>.
	 */
	public static final String RDF_n3_useDoubles = "Rdf.jena.n3.useDoubles";

	/**
	 * The RDF format for representing collections and arrays.  ({@link String}, default=<js>"DEFAULT"</js>).
	 * <p>
	 * 	Possible values:
	 * 	<ul class='spaced-list'>
	 * 		<li><js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
	 * 		<li><js>"SEQ"</js> - RDF Sequence container.
	 * 		<li><js>"BAG"</js> - RDF Bag container.
	 * 		<li><js>"LIST"</js> - RDF List container.
	 * 		<li><js>"MULTI_VALUED"</js> - Multi-valued properties.
	 * 	</ul>
	 *	</p>
	 *	<p>
	 *		Important Note:  If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements
	 *		in the collection will get lost.
	 */
	public static final String RDF_collectionFormat = "Rdf.collectionFormat";

	/**
	 * Specifies that collections should be serialized and parsed as loose collections.  ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
	 * resources that are children of an RDF collection (e.g. Sequence, Bag).
	 * <p>
	 * Note that this setting is specialized for RDF syntax, and is incompatible with the concept of
	 * losslessly representing POJO models, since the tree structure of these POJO models are lost
	 * when serialized as loose collections.
	 *	<p>
	 *	This setting is typically only useful if the beans being parsed into do not have a bean property
	 *	annotated with {@link Rdf#beanUri @Rdf(beanUri=true)}.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	WriterSerializer s = <jk>new</jk> RdfSerializer.XmlAbbrev().setProperty(<jsf>RDF_looseCollection</jsf>, <jk>true</jk>);
	 * 	ReaderParser p = <jk>new</jk> RdfParser.Xml().setProperty(<jsf>RDF_looseCollection</jsf>, <jk>true</jk>);
	 *
	 * 	List&lt;MyBean&gt; l = createListOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(l);
	 *
	 *		<jc>// Parse back into a Java collection</jc>
	 * 	l = p.parseCollection(rdfXml, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	MyBean[] b = createArrayOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(b);
	 *
	 *		<jc>// Parse back into a bean array</jc>
	 * 	b = p.parse(rdfXml, MyBean[].<jk>class</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 */
	public static final String RDF_looseCollection = "Rdf.looseCollection";
}
