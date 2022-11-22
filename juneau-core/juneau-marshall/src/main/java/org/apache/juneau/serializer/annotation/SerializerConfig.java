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
package org.apache.juneau.serializer.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Annotation for specifying config properties defined in {@link Serializer}, {@link OutputStreamSerializer}, and {@link WriterSerializer}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({SerializerConfigAnnotation.SerializerApply.class,SerializerConfigAnnotation.OutputStreamSerializerApply.class,SerializerConfigAnnotation.WriterSerializerApply.class})
public @interface SerializerConfig {

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
	// OutputStreamSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Binary output format.
	 *
	 * <p>
	 * When using the {@link OutputStreamSerializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <ul class='values'>
	 * 	<li><js>"SPACED_HEX"</js>
	 * 	<li><js>"HEX"</js> (default)
	 * 	<li><js>"BASE64"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.OutputStreamSerializer.Builder#binaryFormat(BinaryFormat)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String binaryFormat() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// Serializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * <br>For example, when serializing a <c>Map&lt;String,Object&gt;</c> field where the bean class cannot be determined from
	 * the type of the values.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addBeanTypes() default "";

	/**
	 * Add type attribute to root nodes.
	 *
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a top-level POJO with a {@link Bean#typeName() @Bean(typeName)} value, a
	 * <js>'_type'</js> attribute will only be added when this setting is enabled.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addRootType()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addRootType() default "";

	/**
	 * Don't trim null bean property values.
	 *
	 * <p>
	 * If <js>"true"</js>, null bean values will be serialized to the output.
	 *
	 * <p>
	 * Note that not enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with <jk>null</jk> values will be lost.
	 * </ul>
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#keepNullProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String keepNullProperties() default "";

	/**
	 * Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#listener(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends SerializerListener> listener() default SerializerListener.Void.class;

	/**
	 * Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortCollections()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String sortCollections() default "";

	/**
	 * Sort maps alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of maps by their keys before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortMaps()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String sortMaps() default "";

	/**
	 * Trim empty lists and arrays.
	 *
	 * <p>
	 * If <js>"true"</js>, empty lists and arrays will not be serialized.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with empty list values will be lost.
	 * 	<li>
	 * 		Bean properties with empty list values will not be set.
	 * </ul>
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyCollections()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String trimEmptyCollections() default "";

	/**
	 * Trim empty maps.
	 *
	 * <p>
	 * If <js>"true"</js>, empty map values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Bean properties with empty map values will not be set.
	 * </ul>
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyMaps()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String trimEmptyMaps() default "";

	/**
	 * Trim strings.
	 *
	 * <p>
	 * If <js>"true"</js>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimStrings()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String trimStrings() default "";

	/**
	 * URI context bean.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: JSON object representing a {@link UriContext}
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriContext(UriContext)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriContext() default "";

	/**
	 * URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"RESOURCE"</js> (default) - Relative URIs should be considered relative to the servlet URI.
	 * 	<li><js>"PATH_INFO"</js> - Relative URIs should be considered relative to the request URI.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriRelativity(UriRelativity)}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriRelativity() default "";

	/**
	 * URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"ABSOLUTE"</js> - Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li><js>"ROOT_RELATIVE"</js> - Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li><js>"NONE"</js> (default) - Don't do any URL resolution.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriResolution(UriResolution)}
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriResolution() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// WriterSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * <p>
	 * The character set to use for writing Files to the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: string
	 * 	<li class='note'>
	 * 		"DEFAULT" can be used to indicate the JVM default file system charset.
	 * 	<li class='note'>
	 * 		Default: JVM system default.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#fileCharset(java.nio.charset.Charset)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String fileCharset() default "";

	/**
	 * Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 * 	<li class='note'>
	 * 		Default: 100
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#maxIndent(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maxIndent() default "";

	/**
	 * Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Default: <c>"</c>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#quoteChar(char)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String quoteChar() default "";

	/**
	 * Output stream charset.
	 *
	 * <p>
	 * The character set to use when writing to OutputStreams.
	 *
	 * <p>
	 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: string
	 * 	<li class='note'>
	 * 		Default: <js>"utf-8"</js>.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#streamCharset(Charset)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String streamCharset() default "";

	/**
	 * Use whitespace.
	 *
	 * <p>
	 * If <js>"true"</js>, whitespace is added to the output to improve readability.
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
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useWhitespace() default "";

	//-----------------------------------------------------------------------------------------------------------------
	// BeanTraverseContext
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Automatically detect POJO recursions.
	 *
	 * <p>
	 * Specifies that recursions should be checked for during traversal.
	 *
	 * <p>
	 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
	 * <br>In general, unchecked recursions cause stack-overflow-errors.
	 * <br>These show up as {@link ParseException ParseExceptions} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>BEANTRAVERSE_ignoreRecursions</jsf> is <jk>true</jk>...
	 *
	 * <p class='bjson'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='warn'>
	 * 		Checking for recursion can cause a small performance penalty.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String detectRecursions() default "";

	/**
	 * Ignore recursion errors.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}.
	 * <br>Setting is ignored if <jsf>BEANTRAVERSE_detectRecursions</jsf> is <js>"false"</js>.
	 *
	 * <p>
	 * If <js>"true"</js>, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * <br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
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
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreRecursions() default "";

	/**
	 * Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 * <br>Useful when constructing document fragments that need to be indented at a certain level.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 *	<li class='note'>
	 * 		Default value: <js>"0"</js>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#initialDepth(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String initialDepth() default "";

	/**
	 * Max traversal depth.
	 *
	 * <p>
	 * Abort traversal if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 * 	<li class='note'>
	 * 		Default value: <js>"100"</js>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#maxDepth(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maxDepth() default "";
}
