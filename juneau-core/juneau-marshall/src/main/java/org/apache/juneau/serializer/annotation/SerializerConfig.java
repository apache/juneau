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
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// OutputStreamSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Binary output format.
	 *
	 * <p>
	 * When using the {@link OutputStreamSerializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"SPACED_HEX"</js>
	 * 			<li><js>"HEX"</js> (default)
	 * 			<li><js>"BASE64"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.OutputStreamSerializer.Builder#binaryFormat(BinaryFormat)}
	 * </ul>
	 */
	String binaryFormat() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// Serializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Add type attribute to root nodes.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addRootType()}
	 * </ul>
	 */
	String addRootType() default "";

	/**
	 * Configuration property:  Don't trim null bean property values.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#keepNullProperties()}
	 * </ul>
	 */
	String keepNullProperties() default "";

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#listener(Class)}
	 * </ul>
	 */
	Class<? extends SerializerListener> listener() default SerializerListener.Null.class;

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortCollections()}
	 * </ul>
	 */
	String sortCollections() default "";

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of maps by their keys before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortMaps()}
	 * </ul>
	 */
	String sortMaps() default "";

	/**
	 * Configuration property:  Trim empty lists and arrays.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyCollections()}
	 * </ul>
	 */
	String trimEmptyCollections() default "";

	/**
	 * Configuration property:  Trim empty maps.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyMaps()}
	 * </ul>
	 */
	String trimEmptyMaps() default "";

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * If <js>"true"</js>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimStrings()}
	 * </ul>
	 */
	String trimStrings() default "";

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: JSON object representing a {@link UriContext}
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriContext(UriContext)}
	 * </ul>
	 */
	String uriContext() default "";

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"RESOURCE"</js> (default) - Relative URIs should be considered relative to the servlet URI.
	 * 			<li><js>"PATH_INFO"</js> - Relative URIs should be considered relative to the request URI.
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriRelativity(UriRelativity)}
	 * 	<li class='link'>{@doc jm.MarshallingUris}
	 * </ul>
	 */
	String uriRelativity() default "";

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"ABSOLUTE"</js> - Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 			<li><js>"ROOT_RELATIVE"</js> - Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 			<li><js>"NONE"</js> (default) - Don't do any URL resolution.
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriResolution(UriResolution)}
	 * 	<li class='link'>{@doc jm.MarshallingUris}
	 * </ul>
	 */
	String uriResolution() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// WriterSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for writing Files to the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: string
	 * 	<li>
	 * 		"DEFAULT" can be used to indicate the JVM default file system charset.
	 * 	<li>
	 * 		Default: JVM system default.
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#fileCharset(java.nio.charset.Charset)}
	 * </ul>
	 */
	String fileCharset() default "";

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: integer
	 * 	<li>
	 * 		Default: 100
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#maxIndent(int)}
	 * </ul>
	 */
	String maxIndent() default "";

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Default: <c>"</c>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#quoteChar(char)}
	 * </ul>
	 */
	String quoteChar() default "";

	/**
	 * Configuration property:  Output stream charset.
	 *
	 * <p>
	 * The character set to use when writing to OutputStreams.
	 *
	 * <p>
	 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: string
	 * 	<li>
	 * 		Default: <js>"utf-8"</js>.
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#streamCharset(Charset)}
	 * </ul>
	 */
	String streamCharset() default "";

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * If <js>"true"</js>, whitespace is added to the output to improve readability.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace()}
	 * </ul>
	 */
	String useWhitespace() default "";

	//-----------------------------------------------------------------------------------------------------------------
	// BeanTraverseContext
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Automatically detect POJO recursions.
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
	 * <p class='bcode w800'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * 	<li>
	 *		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}
	 * </ul>
	 */
	String detectRecursions() default "";

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}.
	 * <br>Setting is ignored if <jsf>BEANTRAVERSE_detectRecursions</jsf> is <js>"false"</js>.
	 *
	 * <p>
	 * If <js>"true"</js>, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * <br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}
	 * </ul>
	 */
	String ignoreRecursions() default "";

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 * <br>Useful when constructing document fragments that need to be indented at a certain level.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: integer
	 *	<li>
	 * 		Default value: <js>"0"</js>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#initialDepth(int)}
	 * </ul>
	 */
	String initialDepth() default "";

	/**
	 * Configuration property:  Max traversal depth.
	 *
	 * <p>
	 * Abort traversal if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: integer
	 * 	<li>
	 * 		Default value: <js>"100"</js>
	 * 	<li>
	 * 		Supports {@doc jm.DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#maxDepth(int)}
	 * </ul>
	 */
	String maxDepth() default "";
}
