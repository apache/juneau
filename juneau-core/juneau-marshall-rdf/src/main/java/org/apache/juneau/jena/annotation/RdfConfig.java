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

import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Annotation for specifying config properties defined in {@link XmlSerializer}, {@link XmlDocSerializer}, and {@link XmlParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({RdfConfigAnnotation.SerializerApply.class,RdfConfigAnnotation.ParserApply.class})
public @interface RdfConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// RdfCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * RDF language.
	 *
	 * <p>
	 * 	The RDF language to use.
	 *
	 * <ul class='values spaced-list'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#language(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#language(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String language() default "";

	/**
	 * XML namespace for Juneau properties.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#juneauNs(Namespace)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#juneauNs(Namespace)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String juneauNs() default "";

	/**
	 * Default XML namespace for bean properties.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#juneauBpNs(Namespace)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#juneauBpNs(Namespace)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String juneauBpNs() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_iriRules() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
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
	 * @return The annotation value.
	 */
	String rdfxml_errorMode() default "";

	/**
	 * RDF/XML ARP property: <c>embedding</c>.
	 *
	 * <p>
	 * Sets ARP to look for RDF embedded within an enclosing XML document.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li>
	 * 		{doc ext.ARP/ARPOptions.html#setEmbedding(boolean) ARPOptions.setEmbedding(boolean)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_embedding() default "";

	/**
	 * RDF/XML property: <c>xmlbase</c>.
	 *
	 * <p>
	 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_xmlbase(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_xmlbase(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_xmlBase() default "";

	/**
	 * RDF/XML property: <c>longId</c>.
	 *
	 * <p>
	 * Whether to use long ID's for anon resources.
	 * <br>Short ID's are easier to read, but can run out of memory on very large models.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_longId()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_longId()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_longId() default "";

	/**
	 * RDF/XML property: <c>allowBadURIs</c>.
	 *
	 * <p>
	 * URIs in the graph are, by default, checked prior to serialization.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_allowBadUris()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_allowBadUris()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_allowBadUris() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_relativeUris(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_relativeUris(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_relativeUris() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_showXmlDeclaration(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_showXmlDeclaration(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_showXmlDeclaration() default "";

	/**
	 * RDF/XML property: <c>disableShowDoctypeDeclaration</c>.
	 *
	 * <p>
	 * If true, an XML doctype declaration isn't included in the output.
	 * <br>This declaration includes a <c>!ENTITY</c> declaration for each prefix mapping in the model, and any
	 * attribute value that starts with the URI of that mapping is written as starting with the corresponding entity
	 * invocation.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_disableShowDoctypeDeclaration()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_disableShowDoctypeDeclaration()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_disableShowDoctypeDeclaration() default "";

	/**
	 * RDF/XML property: <c>tab</c>.
	 *
	 * <p>
	 * The number of spaces with which to indent XML child elements.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_tab(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_tab(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_tab() default "";

	/**
	 * RDF/XML property: <c>attributeQuoteChar</c>.
	 *
	 * <p>
	 * The XML attribute quote character.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_attributeQuoteChar(char)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_attributeQuoteChar(char)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_attributeQuoteChar() default "";

	/**
	 * RDF/XML property: <c>blockRules</c>.
	 *
	 * <p>
	 * A list of <c>Resource</c> or a <c>String</c> being a comma separated list of fragment IDs from
	 * {doc http://www.w3.org/TR/rdf-syntax-grammar RDF Syntax Grammar} indicating grammar
	 * rules that will not be used.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#rdfxml_blockRules(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#rdfxml_blockRules(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rdfxml_blockRules() default "";

	/**
	 * N3/Turtle property: <c>minGap</c>.
	 *
	 * <p>
	 * Minimum gap between items on a line.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_minGap(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_minGap(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_minGap() default "";

	/**
	 * N3/Turtle property: <c>disableObjectLists</c>.
	 *
	 * <p>
	 * Don't print object lists as comma separated lists.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_disableObjectLists()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_disableObjectLists()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_disableObjectLists() default "";

	/**
	 * N3/Turtle property: <c>subjectColumn</c>.
	 *
	 * <p>
	 * If the subject is shorter than this value, the first property may go on the same line.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_subjectColumn(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_subjectColumn(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_subjectColumn() default "";

	/**
	 * N3/Turtle property: <c>propertyColumn</c>.
	 *
	 * <p>
	 * Width of the property column.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_propertyColumn(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_propertyColumn(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_propertyColumn() default "";

	/**
	 * N3/Turtle property: <c>indentProperty</c>.
	 *
	 * <p>
	 * Width to indent properties.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_indentProperty(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_indentProperty(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_indentProperty() default "";

	/**
	 * N3/Turtle property: <c>widePropertyLen</c>.
	 *
	 * <p>
	 * Width of the property column.
	 * <br>Must be longer than <c>propertyColumn</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_widePropertyLen(int)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_widePropertyLen(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_widePropertyLen() default "";

	/**
	 * N3/Turtle property: <c>disableAbbrevBaseURI</c>.
	 *
	 * <p>
	 * Controls whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_disableAbbrevBaseUri()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_disableAbbrevBaseUri()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_disableAbbrevBaseUri() default "";

	/**
	 * N3/Turtle property: <c>disableUsePropertySymbols</c>.
	 *
	 * <p>
	 * Controls whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_disableUsePropertySymbols()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_disableUsePropertySymbols()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_disableUsePropertySymbols() default "";

	/**
	 * N3/Turtle property: <c>disableUseTripleQuotedStrings</c>.
	 *
	 * <p>
	 * Disallow the use of <c>"""</c> to delimit long strings.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_disableUseTripleQuotedStrings()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_disableUseTripleQuotedStrings()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_disableUseTripleQuotedStrings() default "";

	/**
	 * N3/Turtle property: <c>disableUseDoubles</c>.
	 *
	 * <p>
	 * Disallow the use doubles as <c>123.456</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#n3_disableUseDoubles()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#n3_disableUseDoubles()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String n3_disableUseDoubles() default "";

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * <ul class='values spaced-list'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get
	 * 		lost.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#collectionFormat(RdfCollectionFormat)}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#collectionFormat(RdfCollectionFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String collectionFormat() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#looseCollections()}
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#looseCollections()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String looseCollections() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// RdfParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Trim whitespace from text elements.
	 *
	 * <p>
	 * If <js>"true"</js>, whitespace in text elements will be automatically trimmed.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfParser.Builder#trimWhitespace()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String trimWhitespace() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// RdfSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#addBeanTypes()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addBeanTypes() default "";

	/**
	 * Add XSI data types to non-<c>String</c> literals.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#addLiteralTypes()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addLiteralTypes() default "";

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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#addRootProperty()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addRootProperty() default "";

	/**
	 * Disable auto-detect namespace usage.
	 *
	 * <p>
	 * Don't detect namespace usage before serialization.
	 *
	 * <p>
	 * If disabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#disableAutoDetectNamespaces()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableAutoDetectNamespaces() default "";

	/**
	 * Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#namespaces(Namespace...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] namespaces() default {};

	/**
	 * Disable reuse of XML namespaces when RDF namespaces not specified.
	 *
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * <br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jena.RdfSerializer.Builder#disableUseXmlNamespaces()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableUseXmlNamespaces() default "";
}
