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
import org.apache.juneau.xml.*;

/**
 * Configurable properties common to both the {@link RdfSerializer} and {@link RdfParser} classes.
 * {@review}
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
	 * <p>
	 * 	The RDF language to use.
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
	 * <p>
	 * Set the engine for checking and resolving.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#arp_iriRules(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#arp_iriRules(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_arp_iriRules = PREFIX + ".jena.rdfXml.iri-rules.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>error-mode</c>.
	 *
	 * <p>
	 * This allows a coarse-grained approach to control of error handling.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#arp_errorMode(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#arp_errorMode(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_arp_errorMode = PREFIX + ".jena.rdfXml.error-mode.s";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>embedding</c>.
	 *
	 * <p>
	 * Sets ARP to look for RDF embedded within an enclosing XML document.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#arp_embedding()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#arp_embedding()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 */
	public static final String RDF_arp_embedding = PREFIX + ".jena.rdfXml.embedding.b";

	/**
	 * Configuration property:  RDF/XML ARP property: <c>ERR_xxx</c>.
	 *
	 * <p>
	 * Provides fine-grained control over detected error conditions.
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
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_warn_ RDF_arp_warn_}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.WARN_"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.WARN_</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_WARN_</c>
	 * </ul>
	 */
	public static final String RDF_arp_warn_ = PREFIX + ".jena.rdfXml.WARN_";

	/**
	 * RDF/XML ARP property: <c>IGN_xxx</c>.
	 *
	 * <p>
	 * See {@link #RDF_arp_err_} for details.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfCommon#RDF_arp_ign_ RDF_arp_ign_}
	 * 	<li><b>Name:</b>  <js>"RdfCommon.jena.rdfXml.IGN_"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RdfCommon.jena.rdfXml.IGN_</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFCOMMON_JENA_RDFXML_IGN_</c>
	 * </ul>
	 */
	public static final String RDF_arp_ign_ = PREFIX + ".jena.rdfXml.IGN_";

	/**
	 * Configuration property:  RDF/XML property: <c>xmlbase</c>.
	 *
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_xmlBase(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_xmlBase(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_xmlBase = PREFIX + ".jena.rdfXml.xmlbase.s";

	/**
	 * Configuration property:  RDF/XML property: <c>longId</c>.
	 *
	 * <p>
	 * Whether to use long ID's for anon resources.
	 * Short ID's are easier to read, but can run out of memory on very large models.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_longId()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_longId()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_longId = PREFIX + ".jena.rdfXml.longId.b";

	/**
	 * Configuration property:  RDF/XML property: <c>allowBadURIs</c>.
	 *
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_allowBadUris()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_allowBadUris()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_allowBadUris = PREFIX + ".jena.rdfXml.allowBadURIs.b";

	/**
	 * Configuration property:  RDF/XML property: <c>relativeURIs</c>.
	 *
	 * <p>
	 * What sort of relative URIs should be used.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_relativeUris(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_relativeUris(String)}
	 * 		</ul>
	 * </ul>
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_showXmlDeclaration(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_showXmlDeclaration(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_showXmlDeclaration = PREFIX + ".jena.rdfXml.showXmlDeclaration.s";

	/**
	 * Configuration property:  RDF/XML property: <c>disableShowDoctypeDeclaration</c>.
	 *
	 * <p>
	 * If <jk>true</jk>, an XML doctype declaration is not included in the output.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_disableShowDoctypeDeclaration()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_disableShowDoctypeDeclaration()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_disableShowDoctypeDeclaration = PREFIX + ".jena.rdfXml.disableShowDoctypeDeclaration.b";

	/**
	 * Configuration property:  RDF/XML property: <c>tab</c>.
	 *
	 * <p>
	 * The number of spaces with which to indent XML child elements.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_tab(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_tab(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_tab = PREFIX + ".jena.rdfXml.tab.i";

	/**
	 * Configuration property:  RDF/XML property: <c>attributeQuoteChar</c>.
	 *
	 * <p>
	 * The XML attribute quote character.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_attributeQuoteChar(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_attributeQuoteChar(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_attributeQuoteChar = PREFIX + ".jena.rdfXml.attributeQuoteChar.s";

	/**
	 * Configuration property:  RDF/XML property: <c>blockRules</c>.
	 *
	 * <p>
	 * A list of <c>Resource</c> or a <c>String</c> being a comma separated list of fragment IDs from
	 * {@doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
	 * rules that will not be used.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#rdfxml_blockRules(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#rdfxml_blockRules(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_rdfxml_blockRules = PREFIX + ".jena.rdfXml.blockRules.s";

	/**
	 * Configuration property:  N3/Turtle property: <c>minGap</c>.
	 *
	 * <p>
	 * Minimum gap between items on a line.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_minGap(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_minGap(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_minGap = PREFIX + ".jena.n3.minGap.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableObjectLists</c>.
	 *
	 * <p>
	 * Don't print object lists as comma separated lists.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_disableObjectLists()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_disableObjectLists()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_disableObjectLists = PREFIX + ".jena.n3.disableObjectLists.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>subjectColumn</c>.
	 *
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_subjectColumn(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_subjectColumn(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_subjectColumn = PREFIX + ".jena.n3.subjectColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>propertyColumn</c>.
	 *
	 * <p>
	 * Width of the property column.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_propertyColumn(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_propertyColumn(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_propertyColumn = PREFIX + ".jena.n3.propertyColumn.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>indentProperty</c>.
	 *
	 * <p>
	 * Width to indent properties.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_indentProperty(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_indentProperty(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_indentProperty = PREFIX + ".jena.n3.indentProperty.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>widePropertyLen</c>.
	 *
	 * <p>
	 * Width of the property column.
	 * Must be longer than <c>propertyColumn</c>.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_widePropertyLen(int)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_widePropertyLen(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_widePropertyLen = PREFIX + ".jena.n3.widePropertyLen.i";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableAbbrevBaseUri</c>.
	 *
	 * <p>
	 * Control whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_disableAbbrevBaseUri()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_disableAbbrevBaseUri()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_disableAbbrevBaseUri = PREFIX + ".jena.n3.disableAbbrevBaseUuri.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUsePropertySymbols</c>.
	 *
	 * <p>
	 * Control whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_disableUsePropertySymbols()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_disableUsePropertySymbols()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_disableUsePropertySymbols = PREFIX + ".jena.n3.disableUsePropertySymbols.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUseTripleQuotedStrings</c>.
	 *
	 * <p>
	 * Disallow the use of <c>"""</c> to delimit long strings.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_disableUseTripleQuotedStrings()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_disableUseTripleQuotedStrings()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_n3_disableUseTripleQuotedStrings = PREFIX + ".jena.n3.disableUseTripleQuotedStrings.b";

	/**
	 * Configuration property:  N3/Turtle property: <c>disableUseDoubles</c>.
	 *
	 * <p>
	 * Disallow the use doubles as <c>123.456</c>.
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
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#n3_disableUseDoubles()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#n3_disableUseDoubles()}
	 * 		</ul>
	 * </ul>
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
	 */
	public static final String RDF_collectionFormat = PREFIX + ".collectionFormat.s";

	/**
	 * Configuration property:  Collections should be serialized and parsed as loose collections.
	 *
	 * <p>
	 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
	 * resources that are children of an RDF collection (e.g. Sequence, Bag).
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
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#looseCollections()}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfParserBuilder#looseCollections()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_looseCollections = PREFIX + ".looseCollections.b";
}
