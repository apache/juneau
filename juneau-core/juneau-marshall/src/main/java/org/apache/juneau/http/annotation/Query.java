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
  * REST request form-data annotation.
 *
 * <p>
 * Identifies a POJO to be used as a form-data entry on an HTTP request.
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
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a URL query parameter.
 *
 * <p>
 * Unlike {@link FormData @FormData}, using this annotation does not result in the servlet reading the contents of
 * URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Body @Body} annotation or
 * <c>RestRequest.getBody()</c> method for <c>application/x-www-form-urlencoded POST</c> calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(
 * 			<ja>@Query</ja>(<js>"p1"</js>) <jk>int</jk> <jv>p1</jv>,
 * 			<ja>@Query</ja>(<js>"p2"</js>) String <jv>p2</jv>,
 * 			<ja>@Query</ja>(<js>"p3"</js>) UUID <jv>p3</jv>
 * 		) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		<jk>int</jk> <jv>p1</jv> = <jv>req</jv>.getQueryParameter(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String <jv>p2</jv> = <jv>req</jv>.getQueryParameter(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID <jv>p3</jv> = <jv>req</jv>.getQueryParameter(UUID.<jk>class</jk>, <js>"p3"</js>);
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcQuery}
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
@Repeatable(QueryAnnotation.Array.class)
@ContextApply(QueryAnnotation.Applier.class)
public @interface Query {

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
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(<ja>@Query</ja> OMap <jv>allQueryParameters</jv>) {...}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Query("*")</jc>
	 * 		<ja>@RemoteGet</ja>(<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Query</ja> Map&lt;String,Object&gt; <jv>allQueryParameters</jv>);
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
	 * The {@link Schema @Schema} annotation can also be used standalone on the parameter or type.
	 * Values specified on this field override values specified on the type, and values specified on child types override values
	 * specified on parent types.
	 *
	 * <p>
	 * The schema defining the type used for parameter.
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
	 * The following are completely equivalent ways of defining the existence of a query entry:
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Query</ja>(name=<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Query</ja>(<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) {...}
	 * </p>
	 */
	String value() default "";
}
