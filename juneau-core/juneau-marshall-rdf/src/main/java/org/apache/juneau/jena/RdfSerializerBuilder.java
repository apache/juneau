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
	 * 
	 * @param ps The initial configuration settings for this builder.
	 */
	public RdfSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public RdfSerializer build() {
		return build(RdfSerializer.class);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add XSI data types to non-<code>String</code> literals.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_addLiteralTypes</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addLiteralTypes}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder addLiteralTypes(boolean value) {
		return set(RDF_addLiteralTypes, value);
	}

	/**
	 * Configuration property:  Add RDF root identifier property to root node.
	 * 
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of 
	 * <js>"true"</js> to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * 
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without incoming predicates to 
	 * identify root notes, which can introduce a considerable performance degradation.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_addRootProperty}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder addRootProperty(boolean value) {
		return set(RDF_addRootProperty, value);
	}

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
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_autoDetectNamespaces}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder autoDetectNamespaces(boolean value) {
		return set(RDF_autoDetectNamespaces, value);
	}

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
	 * <ul>
	 * 	<li>If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get 
	 * 		lost.
	 * </ul>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_collectionFormat}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder collectionFormat(RdfCollectionFormat value) {
		return set(RDF_collectionFormat, value);
	}

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_juneauBpNs}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder juneauBpNs(Namespace value) {
		return set(RDF_juneauBpNs, value);
	}

	/**
	 * Configuration property:  XML namespace for Juneau properties.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_juneauNs}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder juneauNs(Namespace value) {
		return set(RDF_juneauNs, value);
	}

	/**
	 * Configuration property:  RDF language.
	 * 
	 * <p>
	 * Can be any of the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"RDF/XML"</js>
	 * 	<li>
	 * 		<js>"RDF/XML-ABBREV"</js>
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
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder language(String value) {
		return set(RDF_language, value);
	}

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
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_looseCollections}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder looseCollections(boolean value) {
		return set(RDF_looseCollections, value);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder n3() {
		return language(Constants.LANG_N3);
	}

	/**
	 * Configuration property:  Default namespaces.
	 * 
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_namespaces</jsf>, values)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_namespaces}
	 * </ul>
	 * 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder namespaces(Namespace...values) {
		return set(RDF_namespaces, values);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder ntriple() {
		return language(Constants.LANG_NTRIPLE);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder turtle() {
		return language(Constants.LANG_TURTLE);
	}

	/**
	 * Configuration property:  Reuse XML namespaces when RDF namespaces not specified.
	 * 
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs} and {@link org.apache.juneau.xml.annotation.Xml} will be 
	 * inherited by the RDF serializers.
	 * Otherwise, namespaces will be defined using {@link RdfNs} and {@link Rdf}.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_useXmlNamespaces}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder useXmlNamespaces(boolean value) {
		return set(RDF_useXmlNamespaces, value);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder xml() {
		return language(Constants.LANG_RDF_XML);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RdfSerializer#RDF_language}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public RdfSerializerBuilder xmlabbrev() {
		return language(Constants.LANG_RDF_XML_ABBREV);
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
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
	public RdfSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
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
	public RdfSerializerBuilder sq() {
		super.sq();
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
	public RdfSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public RdfSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
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

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RdfSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RdfSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RdfSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}