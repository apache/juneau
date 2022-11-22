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
 * REST request path annotation.
 *
 * <p>
 * Identifies a POJO to be used as a path entry on an HTTP request.
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
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a variable
 * in a URL path pattern.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(
 * 			<ja>@Path</ja>(<js>"foo"</js>) String <jv>foo</jv>,
 * 			<ja>@Path</ja>(<js>"bar"</js>) <jk>int</jk> <jv>bar</jv>,
 * 			<ja>@Path</ja>(<js>"baz"</js>) UUID <jv>baz</jv>,
 * 			<ja>@Path</ja>(<js>"/*"</js>) String <jv>remainder</jv>,
 * 		) {...}
 * </p>
 *
 * <p>
 * The special name <js>"/*"</js> is used to retrieve the path remainder after the path match (i.e. the part that matches <js>"/*"</js>).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * 	<li class='extlink'><a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Parameter Object</a>
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 * <p>
 * Annotation applied to Java method arguments of interface proxies to denote that they are path variables on the request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Path">@Path</a>
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Request">@Request</a>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Inherited
@Repeatable(PathAnnotation.Array.class)
@ContextApply(PathAnnotation.Applier.class)
public @interface Path {

	/**
	 * Default value for this parameter.
	 *
	 * @return The annotation value.
	 */
	String def() default "";

	/**
	 * URL path variable name.
	 *
	 * <p>
	 * The path remainder after the path match can be referenced using the name <js>"/*"</js>.
	 * <br>The non-URL-decoded path remainder after the path match can be referenced using the name <js>"/**"</js>.
	 *
	 * <p>
	 * The value should be either a valid path parameter name, or <js>"*"</js> to represent multiple name/value pairs
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <c>NameValuePairs</c>, <c>Map</c>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be treated as name/value pairs.
	 *
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a REST method</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(<ja>@Path</ja> JsonMap <jv>allPathParameters</jv>) {...}
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Path("*")</jc>
	 * 		<ja>@RemoteGet</ja>(<js>"/mymethod/{foo}/{bar}"</js>)
	 * 		String myProxyMethod1(<ja>@Path</ja> Map&lt;String,Object&gt; <jv>allPathParameters</jv>);
	 * 	}
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Path("*")</jc>
	 * 		<ja>@Path</ja>
	 * 		Map&lt;String,Object&gt; getPathVars();
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
	 * 		<jc>// Equivalent to @Path("foo")</jc>
	 * 		<ja>@Path</ja>
	 * 		String getFoo();
	 * 	}
	 * </ul>
	 *
	 * <p>
	 * The name field MUST correspond to the associated <a class="doclink" href="https://swagger.io/specification/v2#pathsPath">path</a> segment from the path field in the <a class="doclink" href="https://swagger.io/specification/v2#pathsObject">Paths Object</a>.
	 * See <a class="doclink" href="https://swagger.io/specification/v2#pathTemplating">Path Templating</a> for further information.
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
	 * The following are completely equivalent ways of defining a path entry:
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(<js>"/pet/{petId}"</js>)
	 * 	<jk>public</jk> Pet getPet(<ja>@Path</ja>(name=<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) { ... }
	 * </p>
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(<js>"/pet/{petId}"</js>)
	 * 	<jk>public</jk> Pet getPet(<ja>@Path</ja>(<js>"petId"</js>) <jk>long</jk> <jv>petId</jv>) { ... }
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
