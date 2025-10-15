/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * REST request path remainder annotation.
 *
 * <p>
 * Identifies a POJO to be used as the path remainder (the part after the path match) on an HTTP request.
 *
 * <p>
 * This is a specialized shortcut for <c><ja>@Path</ja>(<js>"/*"</js>)</c>.
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
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as the 
 * path remainder after the path pattern match.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/myurl/{foo}/{bar}/*"</js>)
 * 	<jk>public void</jk> doGet(
 * 			<ja>@Path</ja>(<js>"foo"</js>) String <jv>foo</jv>,
 * 			<ja>@Path</ja>(<js>"bar"</js>) <jk>int</jk> <jv>bar</jv>,
 * 			<ja>@PathRemainder</ja> String <jv>remainder</jv>,  <jc>// Equivalent to @Path("/*")</jc>
 * 		) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to using <c><ja>@Path</ja>(<js>"/*"</js>)</c>, but provides a more intuitive name.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Path">@Path</a>
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 * <p>
 * Annotation applied to Java method arguments of interface proxies to denote that they represent the path remainder on the request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/PathRemainder">@PathRemainder</a>
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Request">@Request</a>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Path}
 * </ul>
 *
 * @since 9.2.0
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Inherited
@Repeatable(PathRemainderAnnotation.Array.class)
@ContextApply(PathRemainderAnnotation.Applier.class)
public @interface PathRemainder {

	/**
	 * Default value for this parameter.
	 *
	 * @return The annotation value.
	 */
	String def() default "";

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
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
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
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
}

