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

import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * REST request header annotation.
 *
 * <p>
 * Identifies a POJO to be used as the header of an HTTP request.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Arguments and argument-types of client-side <ja>@RemoteResource</ja>-annotated interfaces.
 * 	<li>Methods and return types of server-side and client-side <ja>@Request</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of server-side @RestOp-annotated methods</h5>
 *
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a HTTP
 * request header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(<ja>@Header</ja>(<js>"ETag"</js>) UUID <jv>etag</jv>) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		UUID <jv>etag</jv> = <jv>req</jv>.getHeader(UUID.<jk>class</jk>, <js>"ETag"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerParameterObject}
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 *
 * Annotation applied to Java method arguments of interface proxies to denote that they are serialized as an HTTP
 * header value.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcHeader}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcRequest}
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Inherited
@Repeatable(HeaderAnnotation.Array.class)
@ContextApply(HeaderAnnotation.Applier.class)
public @interface Header {

	/**
	 * Denotes a multi-part parameter (e.g. multiple entries with the same name).
	 *
	 * <h5 class='figure'>Example</h5>
	 * 	<jk>public void</jk> doPost(
	 * 		<ja>@Header</ja>(name=<js>"Beans"</js>, multi=<jk>true</jk>) MyBean[] <jv>beans</jv>
	 * 	) {
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Meant to be used on multi-part parameters (e.g. <js>"Header: h1"</js>, <js>"Header: h2"</js> instead of <js>"Header: @(h1,h2)"</js>)
	 * 	<li>
	 * 		The data type must be a collection or array type.
	 * </ul>
	 */
	boolean multi() default false;

	/**
	 * HTTP header name.
	 * <p>
	 * A blank value (the default) indicates to reuse the bean property name when used on a request bean property.
	 *
	 * <p>
	 * The value should be either a valid HTTP header name, or <js>"*"</js> to represent multiple name/value pairs
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
	 * 	<ja>@RestPost</ja>(<js>"/addPet"</js>)
	 * 	<jk>public void</jk> addPet(<ja>@Header</ja> OMap <jv>allHeaderParameters</jv>) {...}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@RemoteGet</ja>(<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Header</ja> Map&lt;String,Object&gt; <jv>allHeaderParameters</jv>);
	 * 	}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@Header</ja>
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
	 * 		<jc>// Equivalent to @Header("Foo")</jc>
	 * 		<ja>@Header</ja>
	 * 		<ja>@Beanp</ja>(<js>"Foo"</js>)
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 */
	String name() default "";

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	Class<?>[] onClass() default {};

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;

	/**
	 * <mk>schema</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * The schema defining the type used for parameter.
	 *
	 * <p>
	 * The {@link Schema @Schema} annotation can also be used standalone on the parameter or type.
	 * Values specified on this field override values specified on the type, and values specified on child types override values
	 * specified on parent types.
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
	Schema schema() default @Schema;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a header entry:
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Header</ja>(name=<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Header</ja>(<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 */
	String value() default "";
}
