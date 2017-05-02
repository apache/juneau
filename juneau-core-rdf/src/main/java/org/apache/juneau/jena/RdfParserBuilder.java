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
import static org.apache.juneau.jena.RdfParserContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Builder class for building instances of RDF parsers.
 */
public class RdfParserBuilder extends ParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public RdfParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public RdfParserBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public RdfParser build() {
		return new RdfParser(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Trim whitespace from text elements.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfParser.trimWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_trimWhitespace</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfParserContext#RDF_trimWhitespace
	 */
	public RdfParserBuilder trimWhitespace(boolean value) {
		return property(RDF_trimWhitespace, value);
	}

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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfParserContext#RDF_language
	 */
	public RdfParserBuilder language(String value) {
		return property(RDF_language, value);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfParserBuilder xml() {
		return language(Constants.LANG_RDF_XML);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_RDF_XML_ABBREV</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfParserBuilder xmlabbrev() {
		return language(Constants.LANG_RDF_XML_ABBREV);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_NTRIPLE</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfParserBuilder ntriple() {
		return language(Constants.LANG_NTRIPLE);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_N3</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfParserBuilder n3() {
		return language(Constants.LANG_N3);
	}

	/**
	 * Shortcut for calling <code>language(<jsf>LANG_TURTLE</jsf>)</code>
	 * @return This object (for method chaining).
	 */
	public RdfParserBuilder turtle() {
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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfParserContext#RDF_juneauNs
	 */
	public RdfParserBuilder juneauNs(Namespace value) {
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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfParserContext#RDF_juneauBpNs
	 */
	public RdfParserBuilder juneauBpNs(Namespace value) {
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
	 * When specified, namespaces defined using {@link XmlNs} and {@link Xml} will be inherited by the RDF parsers.
	 * Otherwise, namespaces will be defined using {@link RdfNs} and {@link Rdf}.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>RDF_useXmlNamespaces</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see RdfParserContext#RDF_useXmlNamespaces
	 */
	public RdfParserBuilder useXmlNamespaces(boolean value) {
		return property(RDF_useXmlNamespaces, value);
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
	public RdfParserBuilder collectionFormat(RdfCollectionFormat value) {
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
	 * <jc>// Parse back into a Java collection</jc>
	 * 	l = p.parse(rdfXml, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	MyBean[] b = createArrayOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(b);
	 *
	 * <jc>// Parse back into a bean array</jc>
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
	public RdfParserBuilder looseCollections(boolean value) {
		return property(RDF_looseCollections, value);
	}

	@Override /* ParserBuilder */
	public RdfParserBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* ParserBuilder */
	public RdfParserBuilder strict(boolean value) {
		super.strict(value);
		return this;
	}

	@Override /* ParserBuilder */
	public RdfParserBuilder strict() {
		super.strict();
		return this;
	}
	
	@Override /* ParserBuilder */
	public RdfParserBuilder inputStreamCharset(String value) {
		super.inputStreamCharset(value);
		return this;
	}

	@Override /* ParserBuilder */
	public RdfParserBuilder fileCharset(String value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> RdfParserBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder debug(boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public RdfParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}