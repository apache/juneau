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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * REST response header annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response header.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Methods and return types of server-side and client-side <ja>@Response</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments of server-side <ja>@RestOp</ja>-annotated methods</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters to identify them as an HTTP response header.
 * <br>In this case, the annotation can only be applied to subclasses of type {@link Value}.
 *
 * <p>
 * The following examples show 3 different ways of accomplishing the same task of setting an HTTP header
 * on a response:
 *
 * <p class='bcode w800'>
 * 	<jc>// Example #1 - Setting header directly on RestResponse object.</jc>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public void</jk> login(RestResponse <jv>res</jv>) {
 * 		<jv>res</jv>.setHeader(<js>"X-Rate-Limit"</js>, 1000);
 * 		...
 * 	}
 *
 *	<jc>// Example #2 - Use on parameter.</jc>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public void</jk> login(
 * 			<ja>@ResponseHeader</ja>(
 * 				name=<js>"X-Rate-Limit"</js>,
 * 				type=<js>"integer"</js>,
 * 				format=<js>"int32"</js>,
 * 				description=<js>"Calls per hour allowed by the user."</js>,
 * 				example=<js>"123"</js>
 * 			)
 * 			Value&lt;Integer&gt; <jv>rateLimit</jv>
 *		) {
 *		<jv>rateLimit</jv>.set(1000);
 *		...
 * 	}
 *
 *	<jc>// Example #3 - Use on type.</jc>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public void</jk> login(Value&lt;RateLimit&gt; <jv>rateLimit</jv>) {
 * 		<jv>rateLimit</jv>.set(<jk>new</jk> RateLimit(1000));
 * 		...
 * 	}
 *
 * 	<ja>@ResponseHeader</ja>(
 * 		name=<js>"X-Rate-Limit"</js>,
 * 		type=<js>"integer"</js>,
 * 		format=<js>"int32"</js>,
 * 		description=<js>"Calls per hour allowed by the user."</js>,
 * 		example=<js>"123"</js>
 * 	)
 * 	<jk>public class</jk> RateLimit {
 * 		<jc>// OpenApiPartSerializer knows to look for this method based on format/type.</jc>
 * 		<jk>public</jk> Integer toInteger() {
 * 			<jk>return</jk> 1000;
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Public methods of @Response-annotated types</h5>
 *
 * <p>
 * On server-side REST, this annotation can also be applied to public methods of {@link Response}-annotated methods.
 *
 * <p class='bcode w800'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> AddPetSuccess {
 *
 * 		<ja>@ResponseHeader</ja>(
 * 			name=<js>"X-PetId"</js>,
 * 			type=<js>"integer"</js>,
 * 			format=<js>"int32"</js>,
 * 			description=<js>"ID of added pet."</js>,
 * 			example=<js>"123"</js>
 * 		)
 * 		<jk>public int</jk> getPetId() {...}
 * 	}
 * </p>
 *
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerHeaderObject}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Response-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcResponse}
 * </ul>
*/
@Documented
@Target({PARAMETER,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(ResponseHeaderAnnotation.Array.class)
@ContextApply(ResponseHeaderAnnotation.Applier.class)
public @interface ResponseHeader {

	/**
	 * The HTTP status (or statuses) of the response.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The is a comma-delimited list of HTTP status codes that this header applies to.
	 * 	<li>
	 * 		The default value is <js>"200"</js>.
	 * </ul>
	 */
	int[] code() default {};

	/**
	 * Synonym for {@link #name()}.
	 */
	String n() default "";

	/**
	 * The HTTP header name.
	 *
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
	 * Overrides for this part the part serializer defined on the REST resource which by default is {@link OpenApiSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a response header:
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(name=<js>"X-Rate-Limit"</js>) Value&lt;Integer&gt; <jv>rateLimit</jv>)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(<js>"X-Rate-Limit"</js>) Value&lt;Integer&gt; <jv>rateLimit</jv>)
	 * </p>
	 */
	String value() default "";
}
