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
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcResponse}
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerResponseObject}
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
	 * The HTTP response code.
	 *
	 * The default value is <c>500</c> for exceptions and <c>200</c> for return types.
	 */
	int[] code() default {};

	/**
	 * Synonym for {@link #example()}.
	 */
	String[] ex() default {};

	/**
	 * A serialized example of the body of a response.
	 *
	 * <p>
	 * This is the {@doc SimplifiedJson} of an example of the body.
	 *
	 * <p>
	 * This value is converted to a POJO and then serialized to all the registered serializers on the REST method to produce examples for all
	 * supported language types.
	 * <br>These values are then used to automatically populate the {@link #examples} field.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A JSON representation of a PetCreate object.</jc>
	 * 	<ja>@Response</ja>(
	 * 		example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 	)
	 * </p>
	 *
	 * <p>
	 * There are several other options for defining this example:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Defining an <js>"example"</js> field in the inherited Swagger JSON response object (classpath file or <code><ja>@Swagger</ja>(value)</code>/<code><ja>@OpSwagger</ja>(value)</code>).
	 * 	<li>
	 * 		Defining an <js>"example"</js> field in the Swagger Schema Object for the response object (including referenced <js>"$ref"</js> schemas).
	 * 	<li>
	 * 		Allowing Juneau to auto-generate a code example.
	 * </ul>
	 *
	 * <p>
	 * The latter is important because Juneau also supports auto-generation of JSON-Schema from POJO classes using {@link JsonSchemaSerializer} which has several of it's own
	 * options for auto-detecting and calculation POJO examples.
	 *
	 * <p>
	 * In particular, examples can be defined via static methods, fields, and annotations on the classes themselves.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Annotation on class.</jc>
	 * 	<ja>@Example</ja>(<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>)
	 * 	<jk>public class</jk> PetCreate {
	 * 		...
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Annotation on static method.</jc>
	 * 	<jk>public class</jk> PetCreate {
	 *
	 * 		<ja>@Example</ja>
	 * 		<jk>public static</jk> PetCreate <jsm>sample</jsm>() {
	 * 			<jk>return new</jk> PetCreate(<js>"Doggie"</js>, 9.99f, <js>"Dog"</js>, <jk>new</jk> String[] {<js>"friendly"</js>,<js>"cute"</js>});
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Static method with specific name 'example'.</jc>
	 * 	<jk>public class</jk> PetCreate {
	 *
	 * 		<jk>public static</jk> PetCreate <jsm>example</jsm>() {
	 * 			<jk>return new</jk> PetCreate(<js>"Doggie"</js>, 9.99f, <js>"Dog"</js>, <jk>new</jk> String[] {<js>"friendly"</js>,<js>"cute"</js>});
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Static field.</jc>
	 * 	<jk>public class</jk> PetCreate {
	 *
	 * 		<ja>@Example</ja>
	 * 		<jk>public static</jk> PetCreate <jsf>EXAMPLE</jsf> = <jk>new</jk> PetCreate(<js>"Doggie"</js>, 9.99f, <js>"Dog"</js>, <jk>new</jk> String[] {<js>"friendly"</js>,<js>"cute"</js>});
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Examples can also be specified via generic properties as well using the {@link Marshalled#example() @Marshalled(example)} annotation at either the class or method level.
	 * <p class='bcode w800'>
	 * 	<jc>// Examples defined at class level.</jc>
	 * 	<ja>@Rest</ja>
	 * 	<ja>@BeanConfig</ja>(
	 * 		applyMarshalled={
	 * 			<ja>@Marshalled</ja>(on=<js>"PetCreate"</js>,example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>)
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
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='ja'>{@link Bean#example() Bean(example)}
	 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
	 * 	<li class='jc'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder}
	 * 	<ul>
	 * 		<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...) addExamplesTo(TypeCategory...)}
	 * 		<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples() allowNestedExamples()}
	 * 	</ul>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is any {@doc SimplifiedJson} if the object can be converted to a POJO using {@link JsonParser#DEFAULT} or a simple String if the object
	 * 		has a schema associated with it meancan be converted from a String.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		The format of this object can also be a simple String if the body has a schema associated with it, meaning it's meant to be treated as an HTTP part.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables} (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * Serialized examples of the body of a response.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object whose keys are media types and values are string representations of that value.
	 *
	 * <p>
	 * In general you won't need to populate this value directly since it will automatically be calculated based on the value provided in the {@link #example()} field.
	 * <br>However, this field allows you to override the behavior and show examples for only specified media types or different examples for different media types.
	 *
	 * <p class='bcode w800'>
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object with string keys (media type) and string values (example for that media type) .
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable:
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables} (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * 	<li>
	 * 		Resolution of variables is delayed until request time and occurs before parsing.
	 * 		<br>This allows you to, for example, pull in a JSON construct from a properties file based on the locale of the HTTP request.
	 * </ul>
	 */
	String[] examples() default {};

	/**
	 * Synonym for {@link #examples()}.
	 */
	String[] exs() default {};

	/**
	 * <mk>headers</mk> field of the {@doc ExtSwaggerResponseObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	ResponseHeader[] headers() default {};

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
	 * <mk>schema</mk> field of the {@doc ExtSwaggerResponseObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based serializing and serializing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
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
	 * A synonym for {@link #code()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the code.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the response code:
	 * <p class='bcode w800'>
	 * 	<ja>@Response</ja>(code=404)
	 * 	<jk>public class</jk> NotFound <jk>extends</jk> RestException {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Response</ja>(404)
	 * 	<jk>public class</jk> NotFound <jk>extends</jk> RestException {...}
	 * </p>
	 */
	int[] value() default {};
}
