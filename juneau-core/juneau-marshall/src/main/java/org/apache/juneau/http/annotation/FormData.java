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
 * <p>
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a form-data parameter.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> doPost(
 * 			<ja>@FormData</ja>(<js>"p1"</js>) <jk>int</jk> <jv>p1</jv>,
 * 			<ja>@FormData</ja>(<js>"p2"</js>) String <jv>p2</jv>,
 * 			<ja>@FormData</ja>(<js>"p3"</js>) UUID <jv>p3</jv>
 * 		) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
 * 		<jk>int</jk> <jv>p1</jv> = <jv>req</jv>.getFormParam(<js>"p1"</js>).as(<jk>int</jk>.<jk>class</jk>).orElse(0);
 * 		String <jv>p2</jv> = <jv>req</jv>.getFormParam(<js>"p2"</js>).asString().orElse(<jk>null</jk>);
 * 		UUID <jv>p3</jv> = <jv>req</jv>.getFormParam(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
 * 		...
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * 	<li class='extlink'><a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Parameter Object</a>
 * </ul>
 *
 * <h5 class='topic'>Important note concerning FORM posts</h5>
 * <p>
 * This annotation should not be combined with the {@link Content @Content} annotation or <c>RestRequest.getContent()</c> method
 * for <c>application/x-www-form-urlencoded POST</c> posts, since it will trigger the underlying servlet
 * API to parse the body content as key-value pairs resulting in empty content.
 *
 * <p>
 * The {@link Query @Query} annotation can be used to retrieve a URL parameter in the URL string without triggering the
 * servlet to drain the body content.
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 * <p>
 * Annotation applied to Java method arguments of interface proxies to denote that they are FORM post parameters on the
 * request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.FormData">@FormData</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Request">@Request</a>
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Request">@Request</a>
 * </ul>
 *
 * <div class='warn'>
 * 	This annotation should not be combined with the {@link Content @Content} annotation or <c>RestRequest#getContent()</c> method
 * 	for <c>application/x-www-form-urlencoded POST</c> posts, since it will trigger the underlying servlet
 * 	API to parse the body content as key-value pairs resulting in empty content.
 * 	<br>The {@link Query @Query} annotation can be used to retrieve a URL parameter in the URL string without triggering the
 * 	servlet to drain the body content.
 * </div>
 * <div class='warn'>
 * 	If using this annotation on a Spring bean, note that you are likely to encounter issues when using on parameterized
 * 	types such as <code>List&lt;MyBean&gt;</code>.  This is due to the fact that Spring uses CGLIB to recompile classes
 * 	at runtime, and CGLIB was written before generics were introduced into Java and is a virtually-unsupported library.
 * 	Therefore, parameterized types will often be stripped from class definitions and replaced with unparameterized types
 *	(e.g. <code>List</code>).  Under these circumstances, you are likely to get <code>ClassCastExceptions</code>
 *	when trying to access generalized <code>JsonMaps</code> as beans.  The best solution to this issue is to either
 *	specify the parameter as a bean array (e.g. <code>MyBean[]</code>) or declare the method as final so that CGLIB
 *	will not try to recompile it.
 * </div>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Inherited
@Repeatable(FormDataAnnotation.Array.class)
@ContextApply(FormDataAnnotation.Applier.class)
public @interface FormData {

	/**
	 * Default value for this parameter.
	 *
	 * @return The annotation value.
	 */
	String def() default "";

	/**
	 * FORM parameter name.
	 *
	 * <p>
	 * The name of the parameter (required).
	 *
	 * <p>
	 * The value should be either a valid form parameter name, or <js>"*"</js> to represent multiple name/value pairs
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <c>NameValuePairs</c>, <c>Map</c>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 *
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a REST method</jc>
	 * 	<ja>@RestPost</ja>(<js>"/addPet"</js>)
	 * 	<jk>public void</jk> addPet(<ja>@FormData</ja> JsonMap <jv>allFormDataParameters</jv>) {...}
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @FormData("*")</jc>
	 * 		<ja>@RemotePost</ja>(<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@FormData</ja> Map&lt;String,Object&gt; <jv>allFormDataParameters</jv>);
	 * 	}
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @FormData("*")</jc>
	 * 		<ja>@FormData</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * 	<li>
	 * 		If used on a request bean method, uses the bean property name.
	 *
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bjava'>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @FormData("foo")</jc>
	 * 		<ja>@FormData</ja>
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain-text.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Void.class;

	/**
	 * <mk>schema</mk> field of the <a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Parameter Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	Schema schema() default @Schema;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiSerializer}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Void.class;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a form post entry:
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@FormData</ja>(name=<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@FormData</ja>(<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
