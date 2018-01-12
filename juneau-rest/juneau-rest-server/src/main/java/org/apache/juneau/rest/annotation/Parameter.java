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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation used in conjunction with {@link MethodSwagger#parameters() @MethodSwagger.parameters()} to identify 
 * content and header descriptions on specific method requests.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"*"</js>,
 * 		swagger=@MethodSwagger(
 * 			parameters={
 * 				<ja>@Parameter</ja>(in=<js>"header"</js>, name=<js>"Range"</js>, description=<js>"$L{ContentRange.description}"</js>)
 * 			}
 * 		)
 * 	)
 * 	<jk>public void</jk> doAnything(RestRequest req, RestResponse res, <ja>@Method</ja> String method) {
 * 		...
 * 	}
 * </p>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Parameter {

	/**
	 * Declares the value of the parameter that the server will use if none is provided.
	 *
	 * <p>
	 * For example a "count" to control the number of results per page might default to 100 if not supplied by the
	 * client in the request.
	 * (Note: "default" has no meaning for required parameters.)
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">
	 * http://json-schema.org/latest/json-schema-validation.html#anchor101</a>.
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 */
	String _default() default "";

	/**
	 * Sets the ability to pass empty-valued parameters.
	 *
	 * <p>
	 * This is valid only for either <code>query</code> or <code>formData</code> parameters and allows you to send a
	 * parameter with a name only or an empty value.
	 * Default value is <jk>false</jk>.
	 */
	boolean allowEmptyValue() default false;

	/**
	 * Determines the format of the array if type array is used.
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><js>"csv"</js> - comma separated values <js>"foo,bar"</js>.
	 * 	<li><js>"ssv"</js> - space separated values <js>"foo bar"</js>.
	 * 	<li><js>"tsv"</js> - tab separated values <js>"foo\tbar"</js>.
	 * 	<li><js>"pipes"</js> - pipe separated values <js>"foo|bar"</js>.
	 * 	<li><js>"multi"</js> - corresponds to multiple parameter instances instead of multiple values for a single
	 * 		instance <js>"foo=bar&amp;foo=baz"</js>.
	 * 		This is valid only for parameters <code>in</code> <js>"query"</js> or <js>"formData"</js>.
	 * </ul>
	 * Default value is <js>"csv"</js>.
	 */
	String collectionFormat() default "";

	/**
	 * Parameter description (e.g. <js>"Indicates the range returned when Range header is present in the request"</js>).
	 *
	 * <p>
	 * A brief description of the parameter.
	 * This could contain examples of use.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used
	 * for rich text representation.
	 *
	 * <p>
	 * The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * (e.g. <js>"myMethod.res.[code].[category].[name] = foo"</js> or
	 * <js>"MyServlet.myMethod.res.[code].[category].[name] = foo"</js>).
	 */
	String description() default "";

	/**
	 * The extending format for the previously mentioned <code>type</code>.
	 *
	 * <p>
	 * See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further
	 * details.
	 */
	String format() default "";

	/**
	 * The location of the parameter.
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><js>"query"</js>
	 * 	<li><js>"header"</js>
	 * 	<li><js>"path"</js>
	 * 	<li><js>"formData"</js>
	 * 	<li><js>"body"</js>
	 * </ul>
	 */
	String in() default "";

	/**
	 * Required if <code>type</code> is <js>"array"</js>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		parameters={
	 * 			<ja>@Parameter</ja>(
	 * 				in=<js>"header"</js>,
	 * 				name=<js>"Foo"</js>,
	 * 				type=<js>"array"</js>,
	 * 				items=<js>"{type:'string',collectionFormat:'csv'}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public void</jk> doAnything() {
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="http://swagger.io/specification/#itemsObject">Items Object</a> for further details.
	 */
	String items() default "";

	/**
	 * The name of the parameter (e.g. <js>"Content-Range"</js>).
	 *
	 * <p>
	 * Parameter names are case sensitive.
	 * If <code>in</code> is <js>"path"</js>, the name field MUST correspond to the associated path segment from the
	 * <code>path</code> field in the <a class="doclink"
	 * href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * See <a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a> for further
	 * information.
	 * For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 */
	String name() default "";

	/**
	 * Determines whether this parameter is mandatory.
	 *
	 * <p>
	 * If the parameter is <code>in</code> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 */
	boolean required() default false;

	/**
	 * The schema defining the type used for the body parameter.
	 *
	 * <p>
	 * Only applicable for <code>in</code> of type <js>"body"</js>.
	 *
	 * <p>
	 * The schema is a JSON object specified <a class="doclink" href="http://swagger.io/specification/#schemaObject">here</a>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		parameters={
	 * 			<ja>@Parameter</ja>(
	 * 				in=<js>"header"</js>,
	 * 				name=<js>"Foo"</js>,
	 * 				schema=<js>"{format:'string',title:'Foo header',description:'Header that contains the Foo value.'}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public void</jk> doAnything() {
	 * </p>
	 */
	String schema() default "";

	/**
	 * The type of the parameter.
	 *
	 * <p>
	 * The value MUST be one of <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>,
	 * <js>"array"</js> or <js>"file"</js>.
	 * If type is <js>"file"</js>, the consumes MUST be either <js>"multipart/form-data"</js>,
	 * <js>"application/x-www-form-urlencoded"</js> or both and the parameter MUST be in <js>"formData"</js>.
	 */
	String type() default "string";
}
