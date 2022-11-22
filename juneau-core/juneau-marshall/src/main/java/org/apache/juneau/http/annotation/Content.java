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
import org.apache.juneau.json.*;

/**
 * REST request body annotation.
 *
 * <p>
 * Identifies a POJO to be used as the body of an HTTP request.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Arguments and argument-types of client-side <ja>@RemoteOp</ja>-annotated interfaces.
 * 	<li>Methods and return types of server-side and client-side <ja>@Request</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of server-side @RestOp-annotated methods</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters or parameter classes to identify them as the body of an HTTP request.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Used on parameter</jc>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(<ja>@Content</ja> Pet <jv>pet</jv>) {...}
 * </p>
 * <p class='bjava'>
 * 	<jc>// Used on class</jc>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(Pet <jv>pet</jv>) {...}
 *
 * 	<ja>@Content</ja>
 * 	<jk>public class</jk> Pet {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(RestRequest <jv>req</jv>) {
 * 		Pet <jv>pet</jv> = <jv>req</jv>.getContent().as(Pet.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Content">@Content</a>
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Request">@Request</a>
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Annotation parameter values will be aggregated when used on POJO parent and child classes.
 * 		<br>Values on child classes override values on parent classes.
 * 	<li class='note'>
 * 		Annotation parameter values will be aggregated when used on both POJOs and REST methods.
 * 		<br>Values on methods override values on POJO classes.
 * 	<li class='warn'>
 * 		If using this annotation on a Spring bean, note that you are likely to encounter issues when using on parameterized
 * 		types such as <code>List&lt;MyBean&gt;</code>.  This is due to the fact that Spring uses CGLIB to recompile classes
 * 		at runtime, and CGLIB was written before generics were introduced into Java and is a virtually-unsupported library.
 * 		Therefore, parameterized types will often be stripped from class definitions and replaced with unparameterized types
 *		(e.g. <code>List</code>).  Under these circumstances, you are likely to get <code>ClassCastExceptions</code>
 *		when trying to access generalized <code>JsonMaps</code> as beans.  The best solution to this issue is to either
 *		specify the parameter as a bean array (e.g. <code>MyBean[]</code>) or declare the method as final so that CGLIB
 *		will not try to recompile it.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(ContentAnnotation.Array.class)
@ContextApply(ContentAnnotation.Applier.class)
public @interface Content {

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
	 * <mk>schema</mk> field of the <a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Parameter Object</a>.
	 *
	 * <p>
	 * The schema defining the type used for parameter.
	 *
	 * <p>
	 * This is a required attribute per the swagger definition.
	 * However, if not explicitly specified, the value will be auto-generated using {@link JsonSchemaSerializer}.
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
}
