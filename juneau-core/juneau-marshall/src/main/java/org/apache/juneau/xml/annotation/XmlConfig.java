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

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Annotation for specifying config properties defined in {@link XmlSerializer}, {@link XmlDocSerializer}, and {@link XmlParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({XmlConfigAnnotation.SerializerApply.class,XmlConfigAnnotation.ParserApply.class})
public @interface XmlConfig {

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
	// XmlCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// XmlParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * XML event allocator.
	 *
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlParser.Builder#eventAllocator(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends XMLEventAllocator> eventAllocator() default XmlEventAllocator.Void.class;

	/**
	 * Preserve root element during generalized parsing.
	 *
	 * <p>
	 * If <js>"true"</js>, when parsing into a generic {@link JsonMap}, the map will contain a single entry whose key
	 * is the root element name.
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
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlParser.Builder#preserveRootElement()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String preserveRootElement() default "";

	/**
	 * XML reporter.
	 *
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Reporters are not copied to new parsers during a clone.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlParser.Builder#reporter(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends XMLReporter> reporter() default XmlReporter.Void.class;

	/**
	 * XML resolver.
	 *
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlParser.Builder#resolver(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends XMLResolver> resolver() default XmlResolver.Void.class;

	/**
	 * Enable validation.
	 *
	 * <p>
	 * If <js>"true"</js>, XML document will be validated.
	 *
	 * <p>
	 * See {@link XMLInputFactory#IS_VALIDATING} for more info.
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
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlParser.Builder#validating()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String validating() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// XmlSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
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
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#addBeanTypesXml()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addBeanTypes() default "";

	/**
	 * Add namespace URLs to the root element.
	 *
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root element for the default and all mapped namespaces.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This setting is ignored if {@link org.apache.juneau.xml.XmlSerializer.Builder#enableNamespaces()} is not enabled.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#addNamespaceUrisToRoot()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlNamespaces">Namespaces</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addNamespaceUrisToRoot() default "";

	/**
	 * Don't auto-detect namespace usage.
	 *
	 * <p>
	 * Don't detect namespace usage before serialization.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.xml.XmlSerializer.Builder#addNamespaceUrisToRoot()} to reduce the list of namespace URLs appended to the
	 * root element to only those that will be used in the resulting document.
	 *
	 * <p>
	 * If disabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <p>
	 * This setting is ignored if {@link org.apache.juneau.xml.XmlSerializer.Builder#enableNamespaces()} is not enabled.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='warn'>
	 * 		Auto-detection of namespaces can be costly performance-wise.
	 * 		<br>In high-performance environments, it's recommended that namespace detection be
	 * 		disabled, and that namespaces be manually defined through the {@link org.apache.juneau.xml.XmlSerializer.Builder#namespaces(Namespace...)} property.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#disableAutoDetectNamespaces()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlNamespaces">Namespaces</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableAutoDetectNamespaces() default "";

	/**
	 * Default namespace.
	 *
	 * <p>
	 * Specifies the default namespace URI for this document.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#defaultNamespace(Namespace)}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlNamespaces">Namespaces</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String defaultNamespace() default "";

	/**
	 * Enable support for XML namespaces.
	 *
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
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
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#enableNamespaces()}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlNamespaces">Namespaces</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String enableNamespaces() default "";

	/**
	 * Default namespaces.
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.xml.XmlSerializer.Builder#namespaces(Namespace...)}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlNamespaces">Namespaces</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] namespaces() default {};
}
