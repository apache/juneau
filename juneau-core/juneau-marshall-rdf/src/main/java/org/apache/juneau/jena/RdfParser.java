/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.commons.bean.BeanMeta;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Parses RDF into POJOs.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}

 * </ul>
 */
@SuppressWarnings({
	"java:S100", // RDF method names (getRdfXml, isXmlLang, etc.) use domain-specific casing
	"java:S115"  // Constants use UPPER_snakeCase convention
})
public class RdfParser extends ReaderParser implements RdfMetaProvider {

	// Property name constants
	private static final String PROP_collectionFormat = "collectionFormat";
	private static final String PROP_juneauBpNs = "juneauBpNs";
	private static final String PROP_juneauNs = "juneauNs";
	private static final String PROP_language = "language";
	private static final String PROP_looseCollections = "looseCollections";
	private static final String PROP_trimWhitespace = "trimWhitespace";

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends ReaderParser.Builder<SELF> {

		private static final Cache<HashKey,RdfParser> CACHE = Cache.of(HashKey.class, RdfParser.class).build();

		private static final Namespace DEFAULT_JUNEAU_NS = Namespace.of("j", "http://www.apache.org/juneau/");
		private static final Namespace DEFAULT_JUNEAUBP_NS = Namespace.of("jp", "http://www.apache.org/juneaubp/");

		boolean trimWhitespace;
		boolean looseCollections;
		String language;
		Namespace juneauNs;
		Namespace juneauBpNs;
		RdfCollectionFormat collectionFormat;
		Map<String,Object> jenaSettings = new TreeMap<>();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
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
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			collectionFormat = copyFrom.collectionFormat;
			juneauBpNs = copyFrom.juneauBpNs;
			juneauNs = copyFrom.juneauNs;
			language = copyFrom.language;
			looseCollections = copyFrom.looseCollections;
			trimWhitespace = copyFrom.trimWhitespace;
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(RdfParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			collectionFormat = copyFrom.collectionFormat;
			juneauBpNs = copyFrom.juneauBpNs;
			juneauNs = copyFrom.juneauNs;
			language = copyFrom.language;
			looseCollections = copyFrom.looseCollections;
			trimWhitespace = copyFrom.trimWhitespace;
			jenaSettings = new TreeMap<>(copyFrom.jenaSettings);
		}

		@Override /* Overridden from Context.Builder<?> */
		public RdfParser build() {
			return cache(CACHE).build(RdfParser.class);
		}

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
		 * @param value The new value for this property.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF collectionFormat(RdfCollectionFormat value) {
			collectionFormat = assertArgNotNull(ARG_value, value);
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				trimWhitespace,
				looseCollections,
				language,
				collectionFormat,
				juneauNs,
				juneauBpNs,
				jenaSettings
			);
			// @formatter:on
		}

		/**
		 * Default XML namespace for bean properties.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF juneauBpNs(Namespace value) {
			juneauBpNs = assertArgNotNull(ARG_value, value);
			return self();
		}

		/**
		 * XML namespace for Juneau properties.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF juneauNs(Namespace value) {
			juneauNs = assertArgNotNull(ARG_value, value);
			return self();
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
		 * 	<li class='jm'>{@link RdfParser.Builder#n3()}
		 * 	<li class='jm'>{@link RdfParser.Builder#ntriple()}
		 * 	<li class='jm'>{@link RdfParser.Builder#turtle()}
		 * 	<li class='jm'>{@link RdfParser.Builder#xml()}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF language(String value) {
			language = assertArgNotNull(ARG_value, value);
			return self();
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
		 * 	MyBean[] <jv>bean</jv> = <jsm>createArrayOfMyBeans</jsm>();
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
		public SELF looseCollections() {
			return looseCollections(true);
		}

		/**
		 * Same as {@link #looseCollections()} but explicitly specifies the setting value.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public SELF looseCollections(boolean value) {
			looseCollections = value;
			return self();
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF n3() {
			return language(Constants.LANG_N3);
		}

		/**
		 * N3/Turtle property: <c>disableAbbrevBaseURI</c>.
		 *
		 * <p>
		 * Controls whether to use abbreviations <c>&lt;&gt;</c> or <c>&lt;#&gt;</c>.
		 *
		 * @return This object.
		 */
		public SELF n3_disableAbbrevBaseUri() {
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
		public SELF n3_disableAbbrevBaseUri(boolean value) {
			return jena("n3.disableAbbrevBaseUri", value);
		}

		/**
		 * N3/Turtle property: <c>disableObjectLists</c>.
		 *
		 * <p>
		 * Don't print object lists as comma separated lists.
		 *
		 * @return This object.
		 */
		public SELF n3_disableObjectLists() {
			return n3_disableObjectLists(true);
		}

		/**
		 * N3/Turtle property: <c>disableObjectLists</c>.
		 *
		 * <p>
		 * Don't print object lists as comma separated lists.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF n3_disableObjectLists(boolean value) {
			return jena("n3.disableObjectLists", value);
		}

		/**
		 * N3/Turtle property: <c>disableUseDoubles</c>.
		 *
		 * <p>
		 * Disallow the use of doubles as <c>123.456</c>.
		 *
		 * @return This object.
		 */
		public SELF n3_disableUseDoubles() {
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
		public SELF n3_disableUseDoubles(boolean value) {
			return jena("n3.disableUseDoubles", value);
		}

		/**
		 * N3/Turtle property: <c>disableUsePropertySymbols</c>.
		 *
		 * <p>
		 * Controls whether to use <c>a</c>, <c>=</c> and <c>=&gt;</c> in output
		 *
		 * @return This object.
		 */
		public SELF n3_disableUsePropertySymbols() {
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
		public SELF n3_disableUsePropertySymbols(boolean value) {
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
		public SELF n3_disableUseTripleQuotedStrings() {
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
		public SELF n3_disableUseTripleQuotedStrings(boolean value) {
			return jena("n3.disableUseTripleQuotedStrings", value);
		}

		/**
		 * N3/Turtle property: <c>indentProperty</c>.
		 *
		 * <p>
		 * Width to indent properties.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF n3_indentProperty(int value) {
			return jena("n3.indentProperty", value);
		}

		/**
		 * N3/Turtle property: <c>minGap</c>.
		 *
		 * <p>
		 * Minimum gap between items on a line.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF n3_minGap(int value) {
			return jena("n3.minGap", value);
		}

		/**
		 * N3/Turtle property: <c>propertyColumn</c>.
		 *
		 * <p>
		 * Width of the property column.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF n3_propertyColumn(int value) {
			return jena("n3.propertyColumn", value);
		}

		/**
		 * N3/Turtle property: <c>subjectColumn</c>.
		 *
		 * <p>
		 * If the subject is shorter than this value, the first property may go on the same line.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF n3_subjectColumn(int value) {
			return jena("n3.subjectColumn", value);
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
		 * @return This object.
		 */
		public SELF n3_widePropertyLen(int value) {
			return jena("n3.widePropertyLen", value);
		}

		/**
		 * RDF language.
		 *
		 * <p>
		 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF ntriple() {
			return language(Constants.LANG_NTRIPLE);
		}

		/**
		 * RDF/XML property: <c>allowBadURIs</c>.
		 *
		 * <p>
		 * URIs in the graph are, by default, checked prior to serialization.
		 *
		 * @return This object.
		 */
		public SELF rdfxml_allowBadUris() {
			return rdfxml_allowBadUris(true);
		}

		/**
		 * RDF/XML property: <c>allowBadURIs</c>.
		 *
		 * <p>
		 * URIs in the graph are, by default, checked prior to serialization.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_allowBadUris(boolean value) {
			return jena("rdfXml.allowBadURIs", value);
		}

		/**
		 * RDF/XML property: <c>attributeQuoteChar</c>.
		 *
		 * <p>
		 * The XML attribute quote character.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_attributeQuoteChar(char value) {
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_blockRules(String value) {
			return jena("rdfXml.blockRules", value);
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
		public SELF rdfxml_disableShowDoctypeDeclaration() {
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_disableShowDoctypeDeclaration(boolean value) {
			return jena("rdfXml.disableShowDoctypeDeclaration", value);
		}

		/**
		 * RDF/XML ARP property: <c>embedding</c>.
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
		public SELF rdfxml_embedding() {
			return rdfxml_embedding(true);
		}

		/**
		 * RDF/XML ARP property: <c>embedding</c>.
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_embedding(boolean value) {
			return jena("rdfXml.embedding", value);
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_errorMode(String value) {
			return jena("rdfXml.error-mode", value);
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
		 *
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_iriRules(String value) {
			return jena("rdfXml.iri-rules", value);
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
		public SELF rdfxml_longId() {
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_longId(boolean value) {
			return jena("rdfXml.longId", value);
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
		 * @return This object.
		 */
		public SELF rdfxml_relativeUris(String value) {
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
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_showXmlDeclaration(String value) {
			return jena("rdfXml.showXmlDeclaration", value);
		}

		/**
		 * RDF/XML property: <c>tab</c>.
		 *
		 * <p>
		 * The number of spaces with which to indent XML child elements.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_tab(int value) {
			return jena("rdfXml.tab", value);
		}

		/**
		 * RDF/XML property: <c>xmlbase</c>.
		 *
		 * <p>
		 * The value to be included for an <xa>xml:base</xa> attribute on the root element in the file.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public SELF rdfxml_xmlbase(String value) {
			return jena("rdfXml.xmlbase", value);
		}

		/**
		 * Trim whitespace from text elements.
		 *
		 * <p>
		 * When enabled, whitespace in text elements will be automatically trimmed.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create an RDF parser that trims whitespace.</jc>
		 * 	ReaderParser <jv>parser</jv> = RdfParser
		 * 		.<jsm>create</jsm>()
		 * 		.xml()
		 * 		.trimWhitespace()
		 * 		.build();
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF trimWhitespace() {
			return trimWhitespace(true);
		}

		/**
		 * Same as {@link #trimWhitespace()} but allows you to explicitly specify the value.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		public SELF trimWhitespace(boolean value) {
			trimWhitespace = value;
			return self();
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF turtle() {
			return language(Constants.LANG_TURTLE);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_JSONLD</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF jsonLd() {
			return language(Constants.LANG_JSONLD);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_NQUADS</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF nQuads() {
			return language(Constants.LANG_NQUADS);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_TRIG</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF triG() {
			return language(Constants.LANG_TRIG);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_TRIX</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF triX() {
			return language(Constants.LANG_TRIX);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_RDFJSON</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF rdfJson() {
			return language(Constants.LANG_RDFJSON);
		}


		/**
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF xml() {
			return language(Constants.LANG_RDF_XML);
		}

		/**
		 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public SELF xmlabbrev() {
			return language(Constants.LANG_RDF_XML_ABBREV);
		}

		SELF jena(String key, Object value) {
			jenaSettings.put(key, value);
			return self();
		}
	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link RdfParser#create()} / {@link RdfParser#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(RdfParser copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	private static String getConsumes(Builder<?> builder) {
		if (nn(builder.getConsumes()))
			return builder.getConsumes();
		return switch (builder.language) {
			case "RDF/XML", "RDF/XML-ABBREV" -> "text/xml+rdf";
			case "N-TRIPLE" -> "text/n-triple";
			case "N3" -> "text/n3";
			case "N3-PP" -> "text/n3-pp";
			case "N3-PLAIN" -> "text/n3-plain";
			case "N3-TRIPLES" -> "text/n3-triples";
			case "TURTLE" -> "text/turtle";
			case "JSON-LD" -> "application/ld+json";
			case "N-QUADS" -> "application/n-quads";
			case "TRIG" -> "application/trig";
			case "TRIX" -> "application/trix+xml";
			case "RDF/JSON" -> "application/rdf+json";
			default -> "text/xml+rdf";
		};
	}

	protected final boolean looseCollections;
	protected final boolean trimWhitespace;
	protected final Namespace juneauBpNs;
	protected final Namespace juneauNs;
	protected final RdfCollectionFormat collectionFormat;
	protected final String language;

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
	 * @param builder The builder for this object.
	 */
	public RdfParser(Builder<?> builder) {
		super(builder.consumes(getConsumes(builder)));

		collectionFormat = builder.collectionFormat;
		juneauBpNs = builder.juneauBpNs;
		juneauNs = builder.juneauNs;
		language = builder.language;
		looseCollections = builder.looseCollections;
		trimWhitespace = builder.trimWhitespace;
		jenaSettings = new TreeMap<>(builder.jenaSettings);
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public RdfParserSession.Builder createSession() {
		return RdfParserSession.create(this);
	}

	@Override /* Overridden from RdfMetaProvider */
	public RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		return rdfBeanMetas.computeIfAbsent(bm, k -> new RdfBeanMeta(k, this));
	}

	@Override /* Overridden from RdfMetaProvider */
	public RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		return rdfBeanPropertyMetas.computeIfAbsent(bpm, k -> new RdfBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from RdfMetaProvider */
	public RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return rdfClassMetas.computeIfAbsent(cm, k -> new RdfClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public RdfParserSession getSession() { return createSession().build(); }

	@Override /* Overridden from XmlMetaProvider */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		return xmlBeanMetas.computeIfAbsent(bm, k -> new XmlBeanMeta(k, this));
	}

	@Override /* Overridden from XmlMetaProvider */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return xmlBeanPropertyMetas.computeIfAbsent(bpm, k -> new XmlBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from XmlMetaProvider */
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		return xmlClassMetas.computeIfAbsent(cm, k -> new XmlClassMeta(k, this));
	}

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see RdfParser.Builder#collectionFormat(RdfCollectionFormat)
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() { return collectionFormat; }

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() { return jenaSettings; }

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see RdfParser.Builder#juneauBpNs(Namespace)
	 * @return
	 * 	Default XML namespace for bean properties.
	 */
	protected final Namespace getJuneauBpNs() { return juneauBpNs; }

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see RdfParser.Builder#juneauNs(Namespace)
	 * @return
	 * 	XML namespace for Juneau properties.
	 */
	protected final Namespace getJuneauNs() { return juneauNs; }

	/**
	 * RDF language.
	 *
	 * @see RdfParser.Builder#language(String)
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() { return language; }

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see RdfParser.Builder#looseCollections()
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() { return looseCollections; }

	/**
	 * Trim whitespace from text elements.
	 *
	 * @see RdfParser.Builder#trimWhitespace()
	 * @return
	 * 	<jk>true</jk> if whitespace in text elements will be automatically trimmed.
	 */
	protected final boolean isTrimWhitespace() { return trimWhitespace; }

	@Override /* Overridden from ReaderParser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_collectionFormat, collectionFormat)
			.a(PROP_juneauBpNs, juneauBpNs)
			.a(PROP_juneauNs, juneauNs)
			.a(PROP_language, language)
			.a(PROP_looseCollections, looseCollections)
			.a(PROP_trimWhitespace, trimWhitespace);
	}
}