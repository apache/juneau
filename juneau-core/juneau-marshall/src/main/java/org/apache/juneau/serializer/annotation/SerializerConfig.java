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

import static org.apache.juneau.serializer.Serializer.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Annotation for specifying config properties defined in {@link Serializer}, {@link OutputStreamSerializer}, and {@link WriterSerializer}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(SerializerConfigApply.class)
public @interface SerializerConfig {

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
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"SPACED_HEX"</js>
	 * 	<li><js>"HEX"</js> (default)
	 * 	<li><js>"BASE64"</js>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link OutputStreamSerializer#OSSERIALIZER_binaryFormat}
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
	 * <br>For example, when serializing a <code>Map&lt;String,Object&gt;</code> field where the bean class cannot be determined from
	 * the type of the values.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul>
	 * 	<li class='jf'>{@link #SERIALIZER_addRootType} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypes} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
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
	 * 	<li class='jf'>{@link #SERIALIZER_addRootType} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #SERIALIZER_addBeanTypes} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 */
	String addRootType() default "";

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
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
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
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
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
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
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
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
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 */
	String trimEmptyMaps() default "";

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * <p>
	 * If <js>"true"</js>, null bean values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
	 * </ul>
	 */
	String trimNullProperties() default "";

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * If <js>"true"</js>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
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
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><js>"RESOURCE"</js>
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li><js>"PATH_INFO"</js>
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
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
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><js>"ABSOLUTE"</js>
	 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li><js>"ROOT_RELATIVE"</js>
	 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li><js>"NONE"</js>
	 * 		- Don't do any URL resolution.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 */
	String uriResolution() default "";

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * If <js>"true"</js>, whitespace is added to the output to improve readability.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 */
	String useWhitespace() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// WriterSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <p>
	 * This setting does not apply to the RDF serializers.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
	 * </ul>
	 */
	String maxIndent() default "";

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <p>
	 * This setting does not apply to the RDF serializers.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 */
	String quoteChar() default "";
}
