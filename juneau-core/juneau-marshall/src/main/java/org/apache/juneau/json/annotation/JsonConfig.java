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
package org.apache.juneau.json.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Annotation for specifying config properties defined in {@link JsonSerializer} and {@link JsonParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({JsonConfigAnnotation.SerializerApply.class,JsonConfigAnnotation.ParserApply.class})
public @interface JsonConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// JsonCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// JsonParser
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Validate end.
	 *
	 * <p>
	 * If <js>"true"</js>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.json.JsonParser.Builder#validateEnd()}
	 * </ul>
	 */
	String validateEnd() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// JsonSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <js>"true"</js>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.json.JsonSerializer.Builder#addBeanTypesJson()}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * <p>
	 * If <js>"true"</js>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * <br>However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
	 * <xt>&lt;\/script&gt;</xt>.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.json.JsonSerializer.Builder#escapeSolidus()}
	 * </ul>
	 */
	String escapeSolidus() default "";

	/**
	 * Configuration property:  Simple JSON mode.
	 *
	 * <p>
	 * If <js>"true"</js>, JSON attribute names will only be quoted when necessary.
	 * <br>Otherwise, they are always quoted.
	 *
	 * <p>
	 * Attributes do not need to be quoted when they conform to the following:
	 * <ol class='spaced-list'>
	 * 	<li>They start with an ASCII character or <js>'_'</js>.
	 * 	<li>They contain only ASCII characters or numbers or <js>'_'</js>.
	 * 	<li>They are not one of the following reserved words:
	 * 		<p class='bcode w800'>
	 * 	arguments, break, case, catch, class, const, continue, debugger, default,
	 * 	delete, do, else, enum, eval, export, extends, false, finally, for, function,
	 * 	if, implements, import, in, instanceof, interface, let, new, null, package,
	 * 	private, protected, public, return, static, super, switch, this, throw,
	 * 	true, try, typeof, var, void, while, with, undefined, yield
	 * 		</p>
	 * </ol>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver} (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.json.JsonSerializer.Builder#simpleMode()}
	 * </ul>
	 */
	String simpleMode() default "";
}
