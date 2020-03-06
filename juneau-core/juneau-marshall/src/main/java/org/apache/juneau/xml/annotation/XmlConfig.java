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
package org.apache.juneau.xml.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xmlschema.*;

/**
 * Annotation for specifying config properties defined in {@link XmlSerializer}, {@link XmlDocSerializer}, and {@link XmlParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(XmlConfigApply.class)
public @interface XmlConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// XmlCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Dynamically applies {@link Xml @Xml} annotations to specified classes/methods/fields.
	 *
	 * <p>
	 * Provides an alternate approach for applying annotations using {@link Xml#on() @Xml.on} to specify the names
	 * to apply the annotation to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	Xml[] applyXml() default {};

	//-------------------------------------------------------------------------------------------------------------------
	// XmlParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  XML event allocator.
	 *
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_eventAllocator}
	 * </ul>
	 */
	Class<? extends XMLEventAllocator> eventAllocator() default XmlEventAllocator.Null.class;

	/**
	 * Configuration property:  Preserve root element during generalized parsing.
	 *
	 * <p>
	 * If <js>"true"</js>, when parsing into a generic {@link ObjectMap}, the map will contain a single entry whose key
	 * is the root element name.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_preserveRootElement}
	 * </ul>
	 */
	String preserveRootElement() default "";

	/**
	 * Configuration property:  XML reporter.
	 *
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Reporters are not copied to new parsers during a clone.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_reporter}
	 * </ul>
	 */
	Class<? extends XMLReporter> reporter() default XmlReporter.Null.class;

	/**
	 * Configuration property:  XML resolver.
	 *
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_resolver}
	 * </ul>
	 */
	Class<? extends XMLResolver> resolver() default XmlResolver.Null.class;

	/**
	 * Configuration property:  Enable validation.
	 *
	 * <p>
	 * If <js>"true"</js>, XML document will be validated.
	 *
	 * <p>
	 * See {@link XMLInputFactory#IS_VALIDATING} for more info.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_validating}
	 * </ul>
	 */
	String validating() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// XmlSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link Serializer#SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_addBeanTypes}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Add namespace URLs to the root element.
	 *
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root element for the default and all mapped namespaces.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		This setting is ignored if {@link XmlSerializer#XML_enableNamespaces} is not enabled.
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_addNamespaceUrisToRoot}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	String addNamespaceUrisToRoot() default "";

	/**
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * <p>
	 * Detect namespace usage before serialization.
	 *
	 * <p>
	 * Used in conjunction with {@link XmlSerializer#XML_addNamespaceUrisToRoot} to reduce the list of namespace URLs appended to the
	 * root element to only those that will be used in the resulting document.
	 *
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <p>
	 * This setting is ignored if {@link XmlSerializer#XML_enableNamespaces} is not enabled.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Auto-detection of namespaces can be costly performance-wise.
	 * 		<br>In high-performance environments, it's recommended that namespace detection be
	 * 		disabled, and that namespaces be manually defined through the {@link XmlSerializer#XML_namespaces} property.
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_autoDetectNamespaces}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	String autoDetectNamespaces() default "";

	/**
	 * Configuration property:  Default namespace.
	 *
	 * <p>
	 * Specifies the default namespace URI for this document.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_defaultNamespace}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	String defaultNamespace() default "";

	/**
	 * Configuration property:  Enable support for XML namespaces.
	 *
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 *		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_enableNamespaces}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	String enableNamespaces() default "";

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_namespaces}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	String[] namespaces() default {};

	/**
	 * Configuration property:  XMLSchema namespace.
	 *
	 * <p>
	 * Specifies the namespace for the <c>XMLSchema</c> namespace, used by the schema generated by the
	 * {@link XmlSchemaSerializer} class.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlSerializer#XML_xsNamespace}
	 * 	<li class='link'>{@doc juneau-marshall.XmlDetails.Namespaces}
	 * </ul>
	 */
	@Deprecated
	String xsNamespace() default "";
}
