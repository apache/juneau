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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;

/**
 * REST request body annotation.
 * 
 * <p>
 * Identifies a POJO to be used as the body of an HTTP request.
 * 
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Used on parameter</jc>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(<ja>@Body</ja> Pet pet) {...}
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on class</jc>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(Pet pet) {...}
 * 
 * 	<ja>@Body</ja>
 * 	<jk>public class</jk> Pet {...}
 * </p>
 * 
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(RestRequest req) {
 * 		Pet pet = req.getBody().asType(Pet.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 * 
 * <p>
 * This annotation is also used for supplying swagger information about the body of the request.
 * 
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Normal</jc>
 * 	<ja>@Body</ja>(
 * 		description=<js>"Pet object to add to the store"</js>,
 * 		required=<js>"true"</js>,
 * 		example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Free-form</jc>
 * 	<ja>@Body</ja>({
 * 		<js>"description: 'Pet object to add to the store',"</js>,
 * 		<js>"required: true,"</js>,
 * 		<js>"example: {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']},"</js>
 * 		<js>"x-extra: 'extra field'"</js>
 * 	})
 * </p>
 * 
 * <p>
 * This is used to populate the auto-generated Swagger documentation and UI:
 * 
 * <p>
 * <img class='bordered' src='doc-files/Body_Swagger.png' style='width:860px'>
 * 
 * <p>
 * This annotation can be applied to the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Parameters on a {@link RestMethod @RestMethod}-annotated method.
 * 	<li>
 * 		POJO classes used as parameters on a {@link RestMethod @RestMethod}-annotated method.
 * </ul>
 * 
 * <p>
 * Any of the following types can be used (matched in the specified order):
 * <ol class='spaced-list'>
 * 	<li>
 * 		{@link Reader}
 * 		<br><ja>@Body</ja> annotation is optional (it's inferred from the class type).
 * 		<br><code>Content-Type</code> is always ignored.
 * 	<li>
 * 		{@link InputStream} 
 * 		<br><ja>@Body</ja> annotation is optional (it's inferred from the class type).
 * 		<br><code>Content-Type</code> is always ignored.
 * 	<li>
 * 		Any <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.PojoCategories'>parsable</a> POJO type.
 * 		<br><code>Content-Type</code> is required to identify correct parser.
 * 	<li>
 * 		Objects convertible from {@link Reader} by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(Reader in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(Reader in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromReader</jsm>(Reader in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * 	<li>
 * 		Objects convertible from {@link InputStream} by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(InputStream in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(InputStream in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromInputStream</jsm>(InputStream in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * 	<li>
 * 		Objects convertible from {@link String} (including <code>String</code> itself) by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(String in) {...}</code> (e.g. {@link Integer}, {@link Boolean})
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(String in) {...}</code> 
 * 			<li><code><jk>public static</jk> T <jsm>fromString</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromValue</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>valueOf</jsm>(String in) {...}</code> (e.g. enums)
 * 			<li><code><jk>public static</jk> T <jsm>parse</jsm>(String in) {...}</code> (e.g. {@link Level})
 * 			<li><code><jk>public static</jk> T <jsm>parseString</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>forName</jsm>(String in) {...}</code> (e.g. {@link Class}, {@link Charset})
 * 			<li><code><jk>public static</jk> T <jsm>forString</jsm>(String in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * </ol>
 * 
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Annotation values are coalesced from multiple sources in the following order of precedence:
 * 		<ol>
 * 			<li><ja>@Body</ja> annotation on parameter.
 * 			<li><ja>@Body</ja> annotation on parameter class.
 * 			<li><ja>@Body</ja> annotation on parent classes and interfaces.
 * 			<li><ja>@MethodSwagger(value)</ja> annotation.
 * 			<li>Localized resource bundle property <js>"[method-name].produces"</js>.
 * 			<li><ja>@ResourceSwagger(value)</ja> annotation.
 * 			<li>Localized classpath resource file <js>"[enclosing-class].[simple-class-name]_[locale].json"</js> (if it's an inner or member class).
 * 			<li>Default classpath resource file <js>"[enclosing-class].[simple-class-name].json"</js> (if it's an inner or member class).
 * 			<li>Localized classpath resource file <js>"[simple-class-name]_[locale].json"</js>.
 * 			<li>Default classpath resource file <js>"[simple-class-name].json"</js>.
 * 		</ol>
 * </ul>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Body">Overview &gt; juneau-rest-server &gt; @Body</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Swagger Specification &gt; Parameter Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Body {
	
	//=================================================================================================================
	// Attributes common to all ParameterInfos
	//=================================================================================================================
	
	/**
	 * <mk>description</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A brief description of the body. This could contain examples of use.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(Pet input) {...}
	 * 
	 * 	<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		TODO - Future support for MarkDown.
	 * </ul>
	 */
	String[] description() default {};
	
	/**
	 * <mk>required</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * 	Determines whether this parameter is mandatory. 
	 *  <br>The property MAY be included and its default value is false.
	 *  
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(required=<js>"true"</js>) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(Pet input) {...}
	 * 
	 * 	<ja>@Body</ja>(required=<js>"true"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is boolean.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String required() default "";
	
	//=================================================================================================================
	// Attributes specific to in=body
	//=================================================================================================================

	/**
	 * <mk>schema</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<ul>
	 * 			<li><code>schema=<js>"{type:'string',format:'binary'}"</js></code>
	 * 			<li><code>schema=<js>"type:'string',format:'binary'"</js></code>
	 * 		</ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	Schema schema() default @Schema;
	
	/**
	 * TODO
	 * 
	 * <p>
	 * This is the JSON or String representation of an example of the body.
	 * 
	 * <p>
	 * This value is converted to a POJO and then serialized to all the registered serializers on the REST method to produce examples for all
	 * supported language types.
	 * <br>These values are then used to automatically populate the {@link #examples} field.
	 * 
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
	 * 		Defining an <js>"x-example"</js> field in the inherited Swagger JSON body field (classpath file or <code><ja>@ResourceSwagger</ja>(value)</code>/<code><ja>@MethodSwagger</ja>(value)</code>).
	 * 	<li>
	 * 		Defining an <js>"x-example"</js> field in the Swagger Schema Object for the body (including referenced <js>"$ref"</js> schemas).
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
	 * Examples can also be specified via generic properties as well using the {@link BeanContext#BEAN_examples} property at either the class or method level.
	 * <p class='bcode w800'>
	 * 	<jc>// Examples defined at class level.</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(
	 * 				name=<jsf>BEAN_examples</jsf>, 
	 * 				value=<js>"{'org.apache.juneau.examples.rest.petstore.PetCreate': {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}}"</js>
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is any JSON if the object can be converted to a POJO using {@link JsonParser#DEFAULT} or a simple String if the object
	 * 		can be converted from a String.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};
	
	/**
	 * TODO
	 * 
	 * <p>
	 * This is a JSON object whose keys are media types and values are string representations of that value.
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a Simplified JSON object with string keys (media type) and string values (example for that media type) .
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable:
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Resolution of variables is delayed until request time and occurs before parsing.
	 * 		<br>This allows you to, for example, pull in a JSON construct from a properties file based on the locale of the HTTP request.
	 * </ul>
	 */
	String[] examples() default {};

	/**
	 * Free-form value for the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this parameter-info.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the body:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(
	 * 			description=<js>"Pet object to add to the store"</js>,
	 * 			required=<js>"true"</js>,
	 * 			example=<js>"{name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 		) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>({
	 * 			<js>"description: 'Pet object to add to the store',"</js>,
	 * 			<js>"required: true,"</js>,
	 * 			<js>"example: {name:'Doggie',price:9.99,species:'Dog',tags:['friendly','cute']}"</js>
	 * 		}) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(<js>"$L{petObjectSwagger}"</js>) Pet input
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a Simplified JSON object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
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
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};
}
