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

import org.apache.juneau.collections.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;

/**
 * Configurable properties common to both the {@link RdfSerializer} and {@link RdfParser} classes.
 */
public interface RdfCommon {

	/**
	 * Property prefix.
	 */
	static final String PREFIX = "RdfCommon";

	/**
	 * Maps RDF writer names to property prefixes that apply to them.
	 */
	static final Map<String,String> LANG_PROP_MAP = AMap.of("RDF/XML","rdfXml.","RDF/XML-ABBREV","rdfXml.","N3","n3.","N3-PP","n3.","N3-PLAIN","n3.","N3-TRIPLES","n3.","TURTLE","n3.","N-TRIPLE","ntriple.");

	/**
	 * Configuration property:  RDF language.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_language RDF_language}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.language.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.language</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_LANGUAGE</c>
	 * 	<li><b>Default:</b>  <js>"RDF/XML-ABBREV"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#language()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#language(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#ntriple()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#turtle()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#xml()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#xmlabbrev()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#language(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#ntriple()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#turtle()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#xml()}
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
	 */
	public static final String RDF_language = PREFIX + ".language.s";

	/**
	 * Configuration property:  XML namespace for Juneau properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_juneauNs RDF_juneauNs}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.juneauNs.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.xml.Namespace}
	 * 	<li><b>System property:</b>  <c>RdfCommon.juneauNs</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JUNEAUNS</c>
	 * 	<li><b>Default:</b>  <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#juneauNs()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#juneauNs(Namespace)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#juneauNs(Namespace)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_juneauNs = PREFIX + ".juneauNs.s";

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_juneauBpNs RDF_juneauBpNs}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.juneauBpNs.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.xml.Namespace}
	 * 	<li><b>System property:</b>  <c>RdfCommon.juneauBpNs</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JUNEAUBPNS</c>
	 * 	<li><b>Default:</b>  <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#juneauBpNs()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#juneauBpNs(Namespace)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#juneauBpNs(Namespace)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_juneauBpNs = PREFIX + ".juneauBpNs.s";

	/**
	 * Configuration property:  RDF/XML property: <c>iri_rules</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_iriRules RDF_arp_iriRules}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.iri-rules.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.iri-rules</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_IRI_RULES</c>
	 * 	<li><b>Default:</b>  <js>"lax"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#arp_iriRules()}
	 * 		</ul>
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
	public static final String RDF_arp_iriRules = PREFIX + ".jena.rdfXml.iri-rules.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>error-mode</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_errorMode RDF_arp_errorMode}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.error-mode.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.error-mode</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_ERROR_MODE</c>
	 * 	<li><b>Default:</b>  <js>"lax"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#arp_errorMode()}
	 * 		</ul>
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
	 */
	public static final String RDF_arp_errorMode = PREFIX + ".jena.rdfXml.error-mode.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>embedding</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_embedding RDF_arp_embedding}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.embedding.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.embedding</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_EMBEDDING</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#arp_embedding()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Sets ARP to look for RDF embedded within an enclosing XML document.
	 *
	 * <ul class='seealso'>
	 * 	<li>
	 * 		{@doc ExtARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
	 * </ul>
	 */
	public static final String RDF_arp_embedding = PREFIX + ".jena.rdfXml.embedding.b";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>ERR_xxx</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_err_ RDF_arp_err_}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.ERR_"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.ERR_</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_ERR_</c>
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
	 * <ul class='seealso'>
	 * 	<li>
	 * 		{@doc ExtARP/ARPErrorNumbers.html ARPErrorNumbers}
	 * 	<li>
	 * 		{@doc ExtARP/ARPOptions.html#setErrorMode(int,%20int) ARPOptions.setErrorMode(int, int)}
	 * </ul>
	 */
	public static final String RDF_arp_err_ = PREFIX + ".jena.rdfXml.ERR_";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>WARN_xxx</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_warn_ RDF_arp_warn_}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.WARN_"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.WARN_</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_WARN_</c>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_warn_ = PREFIX + ".jena.rdfXml.WARN_";

	/**
	 * RDF/XML ARP property: <c>IGN_xxx</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_ign_ RDF_arp_ign_}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.IGN_"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.IGN_</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_IGN_</c>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 */
	public static final String RDF_arp_ign_ = PREFIX + ".jena.rdfXml.IGN_";

	/**
	 * Configuration property:  RDF/XML property: <c>xmlbase</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_xmlBase RDF_rdfxml_xmlBase}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.xmlbase.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.xmlbase</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_XMLBASE</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_xmlBase()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 */
	public static final String RDF_rdfxml_xmlBase = PREFIX + ".jena.rdfXml.xmlbase.s";

	/**
	 * Configuration property:  RDF/XML property: <c>longId</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_longId RDF_rdfxml_longId}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.longId.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.longId</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_LONGID</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_longId()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Whether to use long ID's for anon resources.
	 * Short ID's are easier to read, but can run out of memory on very large models.
	 */
	public static final String RDF_rdfxml_longId = PREFIX + ".jena.rdfXml.longId.b";

	/**
	 * Configuration property:  RDF/XML property: <c>allowBadURIs</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_allowBadUris RDF_rdfxml_allowBadUris}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.allowBadURIs.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.allowBadURIs</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_ALLOWBADURIS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_allowBadUris()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
	 */
	public static final String RDF_rdfxml_allowBadUris = PREFIX + ".jena.rdfXml.allowBadURIs.b";

	/**
	 * Configuration property:  RDF/XML property: <c>relativeURIs</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_relativeUris RDF_rdfxml_relativeUris}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.relativeURIs.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.relativeURIs</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_RELATIVEURIS</c>
	 * 	<li><b>Default:</b>  <js>"same-document, absolute, relative, parent"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_relativeUris()}
	 * 		</ul>
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
	public static final String RDF_rdfxml_relativeUris = PREFIX + ".jena.rdfXml.relativeURIs.s";

	/**
	 * Configuration property:  RDF/XML property: <c>showXmlDeclaration</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_showXmlDeclaration RDF_rdfxml_showXmlDeclaration}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.showXmlDeclaration.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.showXmlDeclaration</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_SHOWXMLDECLARATION</c>
	 * 	<li><b>Default:</b>  <js>"default"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_showXmlDeclaration()}
	 * 		</ul>
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
	 * 		<js>"default"</js> - Only add an XML Declaration when asked to write to an <c>OutputStreamWriter</c>
	 * 		that uses some encoding other than <c>UTF-8</c> or <c>UTF-16</c>.
	 * 		In this case the encoding is shown in the XML declaration.
	 * </ul>
	 */
	public static final String RDF_rdfxml_showXmlDeclaration = PREFIX + ".jena.rdfXml.showXmlDeclaration.s";

	/**
	 * Configuration property:  RDF/XML property: <c>disableShowDoctypeDeclaration</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_disableShowDoctypeDeclaration RDF_rdfxml_disableShowDoctypeDeclaration}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.disableShowDoctypeDeclaration.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.disableShowDoctypeDeclaration</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_DISABLESHOWDOCTYPEDECLARATION</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_disableShowDoctypeDeclaration()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If true, an XML doctype declaration is included in the output.
	 * This declaration includes a <c>!ENTITY</c> declaration for each prefix mapping in the model, and any
	 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
	 * invocation.
	 */
	public static final String RDF_rdfxml_disableShowDoctypeDeclaration = PREFIX + ".jena.rdfXml.disableShowDoctypeDeclaration.b";

	/**
	 * Configuration property:  RDF/XML property: <c>tab</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_tab RDF_rdfxml_tab}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.tab.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.tab</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_TAB</c>
	 * 	<li><b>Default:</b>  <c>2</c>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_tab()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The number of spaces with which to indent XML child elements.
	 */
	public static final String RDF_rdfxml_tab = PREFIX + ".jena.rdfXml.tab.i";

	/**
	 * Configuration property:  RDF/XML property: <c>attributeQuoteChar</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_attributeQuoteChar RDF_rdfxml_attributeQuoteChar}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.attributeQuoteChar.s"</js>
	 * 	<li><b>Data type:</b>  <c>Character</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.attributeQuoteChar</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_ATTRIBUTEQUOTECHAR</c>
	 * 	<li><b>Default:</b>  <js>'"'</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_attributeQuoteChar()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The XML attribute quote character.
	 */
	public static final String RDF_rdfxml_attributeQuoteChar = PREFIX + ".jena.rdfXml.attributeQuoteChar.s";

	/**
	 * Configuration property:  RDF/XML property: <c>blockRules</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_rdfxml_blockRules RDF_rdfxml_blockRules}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.blockRules.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.blockRules</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_BLOCKRULES</c>
	 * 	<li><b>Default:</b>  <js>""</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#rdfxml_blockRules()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * A list of <c>Resource</c> or a <c>String</c> being a comma separated list of fragment IDs from
	 * {@doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
	 * rules that will not be used.
	 */
	public static final String RDF_rdfxml_blockRules = PREFIX + ".jena.rdfXml.blockRules.s";

	/**
	 * Configuration property:  N3/Turtle property: <c>minGap</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_minGap RDF_n3_minGap}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.minGap.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.minGap</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_MINGAP</c>
	 * 	<li><b>Default:</b>  <c>1</c>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_minGap()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Minimum gap between items on a line.
	 */
	public static final String RDF_n3_minGap = PREFIX + ".jena.n3.minGap.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableObjectLists</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_disableObjectLists RDF_n3_disableObjectLists}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.disableObjectLists.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.disableObjectLists</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_DISABLEOBJECTLISTS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_disableObjectLists()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Print object lists as comma separated lists.
	 */
	public static final String RDF_n3_disableObjectLists = PREFIX + ".jena.n3.disableObjectLists.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>subjectColumn</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_subjectColumn RDF_n3_subjectColumn}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.subjectColumn.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.subjectColumn</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_SUBJECTCOLUMN</c>
	 * 	<li><b>Default:</b>  indentProperty
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_subjectColumn()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
	 */
	public static final String RDF_n3_subjectColumn = PREFIX + ".jena.n3.subjectColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>propertyColumn</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_propertyColumn RDF_n3_propertyColumn}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.propertyColumn.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.propertyColumn</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_PROPERTYCOLUMN</c>
	 * 	<li><b>Default:</b>  <c>8</c>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_propertyColumn()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width of the property column.
	 */
	public static final String RDF_n3_propertyColumn = PREFIX + ".jena.n3.propertyColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>indentProperty</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_indentProperty RDF_n3_indentProperty}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.indentProperty.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.indentProperty</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_INDENTPROPERTY</c>
	 * 	<li><b>Default:</b>  <c>6</c>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_indentProperty()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width to indent properties.
	 */
	public static final String RDF_n3_indentProperty = PREFIX + ".jena.n3.indentProperty.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>widePropertyLen</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_widePropertyLen RDF_n3_widePropertyLen}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.widePropertyLen.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.widePropertyLen</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_WIDEPROPERTYLEN</c>
	 * 	<li><b>Default:</b>  <c>20</c>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_widePropertyLen()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Width of the property column.
	 * Must be longer than <c>propertyColumn</c>.
	 */
	public static final String RDF_n3_widePropertyLen = PREFIX + ".jena.n3.widePropertyLen.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableAbbrevBaseUri</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_disableAbbrevBaseUri RDF_n3_disableAbbrevBaseUri}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.disableAbbrevBaseUri.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.disableAbbrevBaseUri</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_DISABLEABBREVBASEURI</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_disableAbbrevBaseUri()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Control whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
	 */
	public static final String RDF_n3_disableAbbrevBaseUri = PREFIX + ".jena.n3.disableAbbrevBaseUuri.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUsePropertySymbols</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_disableUsePropertySymbols RDF_n3_disableUsePropertySymbols}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.disableUsePropertySymbols.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.disableUsePropertySymbols</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_DISABLEUSEPROPERTYSYMBOLS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_disableUsePropertySymbols()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Control whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output
	 */
	public static final String RDF_n3_disableUsePropertySymbols = PREFIX + ".jena.n3.disableUsePropertySymbols.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUseTripleQuotedStrings</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_disableUseTripleQuotedStrings RDF_n3_disableUseTripleQuotedStrings}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.disableUseTripleQuotedStrings.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.disableUseTripleQuotedStrings</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_DISABLEUSETRIPLEQUOTEDSTRINGS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_disableUseTripleQuotedStrings()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allow the use of <c>"""</c> to delimit long strings.
	 */
	public static final String RDF_n3_disableUseTripleQuotedStrings = PREFIX + ".jena.n3.disableUseTripleQuotedStrings.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUseDoubles</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_n3_disableUseDoubles RDF_n3_disableUseDoubles}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.n3.disableUseDoubles.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.n3.disableUseDoubles</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_N3_DISABLEUSEDOUBLES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#n3_disableUseDoubles()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allow the use doubles as <c>123.456</c>.
	 */
	public static final String RDF_n3_disableUseDoubles = PREFIX + ".jena.n3.disableUseDoubles.b";

	/**
	 * Configuration property:  RDF format for representing collections and arrays.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_collectionFormat RDF_collectionFormat}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.collectionFormat.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.collectionFormat</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_COLLECTIONFORMAT</c>
	 * 	<li><b>Default:</b>  <js>"DEFAULT"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#collectionFormat()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#collectionFormat(RdfCollectionFormat)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#collectionFormat(RdfCollectionFormat)}
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
	 * 		lost.
	 * </ul>
	 */
	public static final String RDF_collectionFormat = PREFIX + ".collectionFormat.s";

	/**
	 * Configuration property:  Collections should be serialized and parsed as loose collections.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_looseCollections RDF_looseCollections}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.looseCollections.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfCommon.looseCollections</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_LOOSECOLLECTIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#looseCollections()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#looseCollections(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#looseCollections()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#looseCollections(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#looseCollections()}
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
	public static final String RDF_looseCollections = PREFIX + ".looseCollections.b";
}
