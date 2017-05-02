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

import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Builder class for building instances of RDF serializers.
 */
public class RdfSerializerBuilder extends SerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public RdfSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public RdfSerializerBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializer build() {
		return new RdfSerializer(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  RDF language.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.language"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"RDF/XML-ABBREV"</js>
	 * </ul>
	 * <p>
	 * Can be any of the following:
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
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_language</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfCommonContext#RDF_language
	 */
	public RdfSerializerBuilder language(String value) {
		return property(RDF_language, value);
	}
	
	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder xml() {
		return language(Constants.LANG_RDF_XML);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder xmlabbrev() {
		return language(Constants.LANG_RDF_XML_ABBREV);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder ntriple() {
		return language(Constants.LANG_NTRIPLE);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder n3() {
		return language(Constants.LANG_N3);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder turtle() {
		return language(Constants.LANG_TURTLE);
	}

	/**
	 * <b>Configuration property:</b>  XML namespace for Juneau properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.juneauNs"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_juneauNs</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_juneauNs
	 */
	public RdfSerializerBuilder juneauNs(Namespace value) {
		return property(RDF_juneauNs, value);
	}

	/**
	 * <b>Configuration property:</b>  Default XML namespace for bean properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.juneauBpNs"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_juneauBpNs</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_juneauBpNs
	 */
	public RdfSerializerBuilder juneauBpNs(Namespace value) {
		return property(RDF_juneauBpNs, value);
	}

	/**
	 * <b>Configuration property:</b>  Reuse XML namespaces when RDF namespaces not specified.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.useXmlNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs} and {@link Xml} will be inherited by the RDF serializers.
	 * Otherwise, namespaces will be defined using {@link RdfNs} and {@link Rdf}.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_useXmlNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public RdfSerializerBuilder useXmlNamespaces(boolean value) {
		return property(RDF_useXmlNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Add XSI data types to non-<code>String</code> literals.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addLiteralTypes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_addLiteralTypes</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_addLiteralTypes
	 */
	public RdfSerializerBuilder addLiteralTypes(boolean value) {
		return property(RDF_addLiteralTypes, value);
	}

	/**
	 * <b>Configuration property:</b>  Add RDF root identifier property to root node.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addRootProperty"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without
	 * 	incoming predicates to identify root notes, which can introduce a considerable performance
	 * 	degradation.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_addRootProperty</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_addRootProperty
	 */
	public RdfSerializerBuilder addRootProperty(boolean value) {
		return property(RDF_addRootProperty, value);
	}

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_autoDetectNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_autoDetectNamespaces
	 */
	public RdfSerializerBuilder autoDetectNamespaces(boolean value) {
		return property(RDF_autoDetectNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.namespaces.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_namespaces</jsf>, values)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfSerializerContext#RDF_namespaces
	 */
	public RdfSerializerBuilder namespaces(Namespace...values) {
		return property(RDF_namespaces, values);
	}

	/**
	 * <b>Configuration property:</b>  RDF format for representing collections and arrays.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.collectionFormat"</js>
	 * 	<li><b>Data type:</b> <code>RdfCollectionFormat</code>
	 * 	<li><b>Default:</b> <js>"DEFAULT"</js>
	 * </ul>
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
	 * 	<li><js>"SEQ"</js> - RDF Sequence container.
	 * 	<li><js>"BAG"</js> - RDF Bag container.
	 * 	<li><js>"LIST"</js> - RDF List container.
	 * 	<li><js>"MULTI_VALUED"</js> - Multi-valued properties.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get lost.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_collectionFormat</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfCommonContext#RDF_collectionFormat
	 */
	public RdfSerializerBuilder collectionFormat(RdfCollectionFormat value) {
		return property(RDF_collectionFormat, value);
	}

	/**
	 * <b>Configuration property:</b>  Collections should be serialized and parsed as loose collections.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.looseCollections"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
	 * resources that are children of an RDF collection (e.g. Sequence, Bag).
	 * <p>
	 * Note that this setting is specialized for RDF syntax, and is incompatible with the concept of
	 * losslessly representing POJO models, since the tree structure of these POJO models are lost
	 * when serialized as loose collections.
	 * <p>
	 * This setting is typically only useful if the beans being parsed into do not have a bean property
	 * annotated with {@link Rdf#beanUri @Rdf(beanUri=true)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	WriterSerializer s = <jk>new</jk> RdfSerializerBuilder().xmlabbrev().looseCollections(<jk>true</jk>).build();
	 * 	ReaderParser p = <jk>new</jk> RdfParserBuilder().xml().looseCollections(<jk>true</jk>).build();
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
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_looseCollections</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfCommonContext#RDF_looseCollections
	 */
	public RdfSerializerBuilder looseCollections(boolean value) {
		return property(RDF_looseCollections, value);
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder sq() {
		super.sq();
		return this;
	}
	
	@Override /* SerializerBuilder */
	public RdfSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder relativeUriBase(String value) {
		super.relativeUriBase(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder absolutePathUriBase(String value) {
		super.absolutePathUriBase(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}
	
	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> RdfSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder debug(boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}