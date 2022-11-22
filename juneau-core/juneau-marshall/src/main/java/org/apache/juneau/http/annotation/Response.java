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
 * REST response annotation.
 *
 * <p>
 * Identifies an interface to use to interact with HTTP parts of an HTTP response through a bean.
 *
 * <p>
 * Can be used in the following locations:
 *  <ul>
 * 	<li>Exception classes thrown from server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Return type classes of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Return type classes of server-side <ja>@RemoteOp</ja>-annotated methods.
 * 	<li>Client-side <ja>@RemoteOp</ja>-annotated methods.
 * 	<li>Return type interfaces of client-side <ja>@RemoteOp</ja>-annotated methods.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Response">@Response</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#responseObject">Swagger Response Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@Repeatable(ResponseAnnotation.Array.class)
@ContextApply(ResponseAnnotation.Applier.class)
public @interface Response {

	/**
	 * Serialized examples of the body of a response.
	 *
	 * <p>
	 * This is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object whose keys are media types and values are string representations of that value.
	 *
	 * <p class='bjava'>
	 * 	<jc>// A JSON representation of a PetCreate object.</jc>
	 * 	<ja>@Response</ja>(
	 * 		examples={
	 * 			<js>"'application/json':'{name:\\'Doggie\\',species:\\'Dog\\'}',"</js>,
	 * 			<js>"'text/uon':'(name:Doggie,species=Dog)'"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object with string keys (media type) and string values (example for that media type) .
	 * 	<li class='note'>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 	<li class='note'>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable:
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * 	<li class='note'>
	 * 		Resolution of variables is delayed until request time and occurs before parsing.
	 * 		<br>This allows you to, for example, pull in a JSON construct from a properties file based on the locale of the HTTP request.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] examples() default {};

	/**
	 * <mk>headers</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#responseObject">Swagger Response Object</a>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Header[] headers() default {};

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
	 * <mk>schema</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#responseObject">Swagger Response Object</a>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based serializing and serializing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Schema schema() default @Schema;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST resource which by default is {@link OpenApiSerializer}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Void.class;
}
