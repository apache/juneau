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

import org.apache.juneau.jsonschema.annotation.Schema;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;

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
 * <p class='bcode w800'>
 * 	<jc>// Used on parameter</jc>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(<ja>@Body</ja> Pet pet) {...}
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on class</jc>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(Pet pet) {...}
 *
 * 	<ja>@Body</ja>
 * 	<jk>public class</jk> Pet {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(RestRequest <jv>req</jv>) {
 * 		Pet <jv>pet</jv> = <jv>req</jv>.getBody().asType(Pet.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * Also used to populate the auto-generated Swagger documentation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>(<js>"/pets"</js>)
 * 	<jk>public void</jk> addPet(Pet <jv>pet</jv>) {...}
 *
 * 	<ja>@Body</ja>(
 * 		description=<js>"Pet object to add to the store"</js>,
 * 		required=<jk>true</jk>,
 * 		example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
 * 	)
 * 	<jk>public class</jk> Pet {...}
 * </p>
 *
 * <p>
 * Swagger documentation values are coalesced from multiple sources in the following order of precedence:
 * <ol>
 * 	<li><ja>@Body</ja> annotation on parameter.
 * 	<li><ja>@Body</ja> annotation on parameter class.
 * 	<li><ja>@Body</ja> annotation on parent classes and interfaces.
 * 	<li><ja>@OpSwagger(value)</ja> annotation.
 * 	<li>Localized resource bundle property <js>"[method-name].produces"</js>.
 * 	<li><ja>@Swagger(value)</ja> annotation.
 * 	<li>Localized classpath resource file <js>"[enclosing-class].[simple-class-name]_[locale].json"</js> (if it's an inner or member class).
 * 	<li>Default classpath resource file <js>"[enclosing-class].[simple-class-name].json"</js> (if it's an inner or member class).
 * 	<li>Localized classpath resource file <js>"[simple-class-name]_[locale].json"</js>.
 * 	<li>Default classpath resource file <js>"[simple-class-name].json"</js>.
 * </ol>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestBodyAnnotation}
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerParameterObject}
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcBody}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestRequestAnnotation}
 * 	<li class='link'>{@doc RestcRequest}
 * </ul>
 *
 * <div class='warn'>
 * 	If using this annotation on a Spring bean, note that you are likely to encounter issues when using on parameterized
 * 	types such as <code>List&lt;MyBean&gt;</code>.  This is due to the fact that Spring uses CGLIB to recompile classes
 * 	at runtime, and CGLIB was written before generics were introduced into Java and is a virtually-unsupported library.
 * 	Therefore, parameterized types will often be stripped from class definitions and replaced with unparameterized types
 *	(e.g. <code>List</code>).  Under these circumstances, you are likely to get <code>ClassCastExceptions</code>
 *	when trying to access generalized <code>OMaps</code> as beans.  The best solution to this issue is to either
 *	specify the parameter as a bean array (e.g. <code>MyBean[]</code>) or declare the method as final so that CGLIB
 *	will not try to recompile it.
 * </div>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(BodyAnnotation.Array.class)
@ContextApply(BodyAnnotation.Applier.class)
public @interface Body {

	/**
	 * Equivalent to {@link #value()}.
	 *
	 * <p>
	 * The following are entirely equivalent:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@Body</ja>({
	 * 		<js>"description: 'Pet object to add to the store',"</js>,
	 * 		<js>"required: true,"</js>,
	 * 		<js>"example: {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 	})
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Body</ja>(api={
	 * 		<js>"description: 'Pet object to add to the store',"</js>,
	 * 		<js>"required: true,"</js>,
	 * 		<js>"example: {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 	})
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
	 * 		If you specify both {@link #value()} and {@link #api()}, {@link #value()} will be ignored.
	 * </ul>
	 */
	String[] api() default {};

	/**
	 * Synonym for {@link #description()}.
	 */
	String[] d() default {};

	/**
	 * <mk>description</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A brief description of the body. This could contain examples of use.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>)
	 * 	<jk>public class</jk> Pet {...}
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
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * Synonym for {@link #example()}.
	 */
	String[] ex() default {};

	/**
	 * A serialized example of the body of a request.
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
	 * 	<ja>@Body</ja>(
	 * 		example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 	)
	 * </p>
	 * <p>
	 * <img class='bordered' src='doc-files/Body_Example.png' style='width:860px'>
	 *
	 * <p>
	 * There are several other options for defining this example:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Defining an <js>"example"</js> field in the inherited Swagger JSON body field (classpath file or <code><ja>@Swagger</ja>(value)</code>/<code><ja>@OpSwagger</ja>(value)</code>).
	 * 	<li>
	 * 		Defining an <js>"example"</js> field in the Swagger Schema Object for the body (including referenced <js>"$ref"</js> schemas).
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
	 * Examples can also be specified via generic properties as well using the {@link Marshalled#example() @Marshalled(example)} annotation.
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
	 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
	 * 	<li class='jc'>{@link JsonSchemaGeneratorBuilder}
	 * 	<ul>
	 * 		<li class='jm'>{@link JsonSchemaGeneratorBuilder#addExamplesTo(TypeCategory...) addExamplesTo(TypeCategory...)}
	 * 		<li class='jm'>{@link JsonSchemaGeneratorBuilder#allowNestedExamples() allowNestedExamples()}
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
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * Serialized examples of the body of a request.
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
	 * 	<ja>@Body</ja>(
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
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
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
	 * Synonym for {@link #required()}.
	 */
	boolean r() default false;

	/**
	 * <mk>required</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Determines whether the body is mandatory.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(required=<jk>true</jk>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Body</ja>(required=<jk>true</jk>)
	 * 	<jk>public class</jk> Pet {...}
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
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean required() default false;

	/**
	 * <mk>schema</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * <p>
	 * This is a required attribute per the swagger definition.
	 * However, if not explicitly specified, the value will be auto-generated using {@link JsonSchemaSerializer}.
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	Schema schema() default @Schema;

	/**
	 * Free-form value for the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * This is a {@doc SimplifiedJson} object that makes up the swagger information for this parameter-info.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the body:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(
	 * 			description=<js>"Pet object to add to the store"</js>,
	 * 			required=<jk>true</jk>,
	 * 			example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 		) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>({
	 * 			<js>"description: 'Pet object to add to the store',"</js>,
	 * 			<js>"required: true,"</js>,
	 * 			<js>"example: {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 		}) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(<js>"$L{petObjectSwagger}"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>petObjectSwagger</mk> = <mv>{ description: "Pet object to add to the store", required: true, example: {name:"Doggie",price:9.99,species:"Dog",tags:["friendly","cute"]} }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this body from an external source such as a properties file.
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
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		Schema-based serialization is NOT affected by values defined in this annotation.
	 * 		<br>It only affects the generated Swagger documentation.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Body</ja>(<js>"{description: 'Pet object to add to the store'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Body</ja>(<js>"description: 'Pet object to add to the store'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
