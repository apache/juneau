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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;

/**
  * REST request form-data annotation.
 *
 * <p>
 * Identifies a POJO to be used as a form-data entry on an HTTP request.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestMethod</ja>-annotated methods.
 * 	<li>Arguments and argument-types of client-side <ja>@RemoteResource</ja>-annotated interfaces.
 * 	<li>Methods and return types of server-side and client-side <ja>@Request</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of server-side @RestMethod-annotated methods</h5>
 *
 * Annotation that can be applied to a parameter of a <ja>@RestMethod</ja>-annotated method to identify it as a URL query parameter.
 *
 * <p>
 * Unlike {@link FormData @FormData}, using this annotation does not result in the servlet reading the contents of
 * URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Body @Body} annotation or
 * <c>RestRequest.getBody()</c> method for <c>application/x-www-form-urlencoded POST</c> calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public void</jk> doGet(
 * 			<ja>@Query</ja>(<js>"p1"</js>) <jk>int</jk> p1,
 * 			<ja>@Query</ja>(<js>"p2"</js>) String p2,
 * 			<ja>@Query</ja>(<js>"p3"</js>) UUID p3
 * 		) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>int</jk> p1 = req.getQueryParameter(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String p2 = req.getQueryParameter(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID p3 = req.getQueryParameter(UUID.<jk>class</jk>, <js>"p3"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.Query}
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
 * 	<li class='extlink'>{@doc SwaggerParameterObject}
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies.Query}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.Request}
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies.Request}
 * </ul>
 */
@Documented
@Target({PARAMETER,FIELD,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Query {

	/**
	 * Skips this value during serialization if it's an empty string or empty collection/array.
	 *
	 * <p>
	 * Note that <jk>null</jk> values are already ignored.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;

	/**
	 * Denotes a multi-part parameter (e.g. multiple entries with the same name).
	 *
	 * <h5 class='figure'>Example</h5>
	 * 	<jk>public void</jk> doPost(
	 * 		<ja>@Query</ja>(name=<js>"beans"</js>, multi=<jk>true</jk>) MyBean[] beans
	 * 	) {
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Meant to be used on multi-part parameters (e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=@(1,2,3)"</js>)
	 * 	<li>
	 * 		The data type must be a collection or array type.
	 * </ul>
	 */
	boolean multi() default false;

	//=================================================================================================================
	// Attributes common to all Swagger Parameter objects
	//=================================================================================================================

	/**
	 * URL query parameter name.
	 *
	 * Required. The name of the parameter. Parameter names are case sensitive.
	 *
	 * <p>
	 * The value should be either a valid query parameter name, or <js>"*"</js> to represent multiple name/value pairs
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <c>NameValuePairs</c>, <c>Map</c>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 *
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a REST method</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/addPet"</js>)
	 * 	<jk>public void</jk> addPet(<ja>@Query</ja> OMap allQueryParameters) {...}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Query("*")</jc>
	 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Query</ja> Map&lt;String,Object&gt; allQueryParameters);
	 * 	}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Query("*")</jc>
	 * 		<ja>@Query</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * 	<li>
	 * 		If used on a request bean method, uses the bean property name.
	 *
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Query("foo")</jc>
	 * 		<ja>@Query</ja>
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain-text.
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the existence of a query entry:
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Query</ja>(name=<js>"petId"</js>) <jk>long</jk> petId) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Query</ja>(<js>"petId"</js>) <jk>long</jk> petId) {...}
	 * </p>
	 */
	String value() default "";

	/**
	 * <mk>description</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * A brief description of the parameter. This could contain examples of use.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>required</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Determines whether the parameter is mandatory.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean required() default false;

	//=================================================================================================================
	// Attributes specific to parameters other than body
	//=================================================================================================================

	/**
	 * <mk>type</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * The type of the parameter.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"string"</js>
	 * 		<br>Parameter must be a string or a POJO convertible from a string.
	 * 	<li>
	 * 		<js>"number"</js>
	 * 		<br>Parameter must be a number primitive or number object.
	 * 		<br>If parameter is <c>Object</c>, creates either a <c>Float</c> or <c>Double</c> depending on the size of the number.
	 * 	<li>
	 * 		<js>"integer"</js>
	 * 		<br>Parameter must be a integer/long primitive or integer/long object.
	 * 		<br>If parameter is <c>Object</c>, creates either a <c>Short</c>, <c>Integer</c>, or <c>Long</c> depending on the size of the number.
	 * 	<li>
	 * 		<js>"boolean"</js>
	 * 		<br>Parameter must be a boolean primitive or object.
	 * 	<li>
	 * 		<js>"array"</js>
	 * 		<br>Parameter must be an array or collection.
	 * 		<br>Elements must be strings or POJOs convertible from strings.
	 * 		<br>If parameter is <c>Object</c>, creates an {@link OList}.
	 * 	<li>
	 * 		<js>"object"</js>
	 * 		<br>Parameter must be a map or bean.
	 * 		<br>If parameter is <c>Object</c>, creates an {@link OMap}.
	 * 		<br>Note that this is an extension of the OpenAPI schema as Juneau allows for arbitrarily-complex POJOs to be serialized as HTTP parts.
	 * 	<li>
	 * 		<js>"file"</js>
	 * 		<br>This type is currently not supported.
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link ParameterType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 */
	String type() default "";

	/**
	 * <mk>format</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * The extending format for the previously mentioned {@doc SwaggerParameterTypes parameter type}.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"int32"</js> - Signed 32 bits.
	 * 		<br>Only valid with type <js>"integer"</js>.
	 * 	<li>
	 * 		<js>"int64"</js> - Signed 64 bits.
	 * 		<br>Only valid with type <js>"integer"</js>.
	 * 	<li>
	 * 		<js>"float"</js> - 32-bit floating point number.
	 * 		<br>Only valid with type <js>"number"</js>.
	 * 	<li>
	 * 		<js>"double"</js> - 64-bit floating point number.
	 * 		<br>Only valid with type <js>"number"</js>.
	 * 	<li>
	 * 		<js>"byte"</js> - BASE-64 encoded characters.
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 		<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
	 * 	<li>
	 * 		<js>"binary"</js> - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 		<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
	 * 	<li>
	 * 		<js>"date"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 	<li>
	 * 		<js>"date-time"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 	<li>
	 * 		<js>"password"</js> - Used to hint UIs the input needs to be obscured.
	 * 		<br>This format does not affect the serialization or parsing of the parameter.
	 * 	<li>
	 * 		<js>"uon"</js> - UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>).
	 * 		<br>Only valid with type <js>"object"</js>.
	 * 		<br>If not specified, then the input is interpreted as plain-text and is converted to a POJO directly.
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link FormatType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypeFormats}
	 * </ul>
	 */
	String format() default "";

	/**
	 * <mk>allowEmptyValue</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * <br>This is valid only for either query or formData parameters and allows you to send a parameter with a name only or an empty value.
	 *
	 * <p>
	 * The default value is <jk>false</jk>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean allowEmptyValue() default false;

	/**
	 * <mk>items</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * <p>
	 * Required if <c>type</c> is <js>"array"</js>.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing and parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing and serializing validation.
	 * </ul>
	 */
	Items items() default @Items;

	/**
	 * <mk>collectionFormat</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
	 *
	 * <br>Possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"csv"</js> (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 	<li>
	 * 		<js>"ssv"</js> - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 	<li>
	 * 		<js>"tsv"</js> - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 	<li>
	 * 		<js>"pipes</js> - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 	<li>
	 * 		<js>"multi"</js> - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 			<br>Note: This is not supported by {@link OpenApiSerializer}.
	 * 	<li>
	 * 		<js>"uon"</js> - UON notation (e.g. <js>"@(foo,bar)"</js>).
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link CollectionFormatType}.
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	String collectionFormat() default "";

	/**
	 * <mk>default</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a "count" to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * <br>(Note: "default" has no meaning for required parameters.)
	 *
	 * <p>
	 * Additionally, this value is used to create instances of POJOs that are then serialized as language-specific examples in the generated Swagger documentation
	 * if the examples are not defined in some other way.
	 *
	 * <p>
	 * The format of this value is a string.
	 * <br>Multiple lines are concatenated with newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(
	 * 		<ja>@Query</ja>(name=<js>"petId"</js>, _default=<js>"100"</js>) <jk>long</jk> petId,
	 * 		<ja>@Query</ja>(name=<js>"additionalInfo"</js>, format=<js>"uon"</js>, _default=<js>"(rushOrder=false)"</js>) AdditionalInfo additionalInfo,
	 * 		<ja>@Query</ja>(name=<js>"flags"</js>, collectionFormat=<js>"uon"</js>, _default=<js>"@(new-customer)"</js>) String[] flags
	 * 	) {...}
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	String[] _default() default {};

	/**
	 * <mk>maximum</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Defines the maximum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String maximum() default "";

	/**
	 * <mk>exclusiveMaximum</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Defines whether the maximum is matched exclusively.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <c>maximum</c>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>minimum</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Defines the minimum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>exclusiveMinimum</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * Defines whether the minimum is matched exclusively.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, Must be accompanied with <c>minimum</c>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * <mk>maxLength</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <c>-1</c> is always ignored.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long maxLength() default -1;

	/**
	 * <mk>minLength</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <c>-1</c> is always ignored.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long minLength() default -1;

	/**
	 * <mk>pattern</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * A string input is valid if it matches the specified regular expression pattern.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * <mk>maxItems</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * An array or collection is valid if its size is less than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long maxItems() default -1;

	/**
	 * <mk>minItems</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * An array or collection is valid if its size is greater than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long minItems() default -1;

	/**
	 * <mk>uniqueItems</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * If <jk>true</jk> the input validates successfully if all of its elements are unique.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * If the parameter type is a subclass of {@link Set}, this validation is skipped (since a set can only contain unique items anyway).
	 * <br>Otherwise, the collection or array is checked for duplicate items.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean uniqueItems() default false;

	/**
	 * <mk>enum</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * If specified, the input validates successfully if it is equal to one of the elements in this array.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * The format is a {@doc SimpleJson} array or comma-delimited list.
	 * <br>Multiple lines are concatenated with newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Comma-delimited list</jc>
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Query</ja>(
	 * 			name=<js>"status"</js>,
	 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
	 * 		) PetStatus status
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// JSON array</jc>
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Query</ja>(
	 * 			name=<js>"status"</js>,
	 * 			_enum=<js>"['AVAILABLE','PENDING','SOLD']"</js>,
	 * 		) PetStatus status
	 * 	) {...}
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String[] _enum() default {};

	/**
	 * <mk>multipleOf</mk> field of the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * A numeric instance is valid if the result of the division of the instance by this keyword's value is an integer.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String multipleOf() default "";

	//=================================================================================================================
	// Other
	//=================================================================================================================

	/**
	 * A serialized example of the parameter.
	 *
	 * <p>
	 * This attribute defines a representation of the value that is used by <c>BasicRestInfoProvider</c> to construct
	 * an example of parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>,
	 * 		type=<js>"array"</js>,
	 * 		collectionFormat=<js>"csv"</js>,
	 * 		example=<js>"AVAILABLE,PENDING"</js>
	 * 	)
	 * 	PetStatus[] status
	 * </p>
	 *
	 * <p>
	 * If not specified, Juneau will automatically create examples from sample POJOs serialized using the registered {@link HttpPartSerializer}.
	 * <br>
	 *
	 * </p>
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='jc'>{@link BeanContext}
	 * 	<ul>
	 * 		<li class='jf'>{@link BeanContext#BEAN_examples BEAN_examples}
	 * 	</ul>
	 * 	<li class='jc'>{@link JsonSchemaSerializer}
	 * 	<ul>
	 * 		<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_addExamplesTo JSONSCHEMA_addExamplesTo}
	 * 		<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_allowNestedExamples JSONSCHEMA_allowNestedExamples}
	 * 	</ul>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimpleJson} object or plain text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * Free-form value for the {@doc SwaggerParameterObject}.
	 *
	 * <p>
	 * This is a {@doc SimpleJson} object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the Query object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>,
	 * 		description=<js>"Status values that need to be considered for filter."</js>,
	 * 		required=<jk>true</jk>,
	 * 		type=<js>"array"</js>,
	 * 		items=<ja>@Items</ja>(
	 * 			type=<js>"string"</js>,
	 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
	 * 			_default=<js>"AVAILABLE"</js>
	 * 		),
	 * 		example=<js>"['AVAILABLE','PENDING']"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>,
	 * 		api={
	 * 			<js>"description: 'Status values that need to be considered for filter.',"</js>,
	 * 			<js>"required: true,"</js>,
	 * 			<js>"type: 'array',"</js>,
	 * 			<js>"items: {"</js>,
	 * 				<js>"type: 'string',"</js>,
	 * 				<js>"enum: 'AVAILABLE,PENDING,SOLD',"</js>,
	 * 				<js>"default: 'AVAILABLE'"</js>,
	 * 			<js>"}"</js>,
	 * 			<js>"example: "['AVAILABLE','PENDING']"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@Query</ja>(
	 * 		name=<js>"status"</js>,
	 * 		api=<js>"$L{statusSwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>statusSwagger</mk> = <mv>{ description: "Status values that need to be considered for filter.", required: true, type: "array", items: {type: "string", enum: "AVAILABLE,PENDING,SOLD", default: "AVAILABLE"}, example: "['AVAILABLE','PENDING']" } }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this field from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		Automatic validation is NOT performed on input based on attributes in this value.
	 * 	<li>
	 * 		The format is a {@doc SimpleJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Query</ja>(api=<js>"{description: 'ID of order to fetch'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Query</ja>(api=<js>"description: 'ID of order to fetch''"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
