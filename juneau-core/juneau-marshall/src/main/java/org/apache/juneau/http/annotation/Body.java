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

import java.io.*;
import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.serializer.*;

/**
 * REST request body annotation.
 *
 * <p>
 * Identifies a POJO to be used as the body of an HTTP request.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Java method arguments of client-side REST interface proxies.
 * 	<li>Java method arguments of server-side REST Java methods and/or their class types.
 * </ul>
 *
 * <h5 class='topic'>Server-side REST</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters or parameter classes to identify them as the body of an HTTP request.
 *
 * <p>
 * This annotation can be applied to the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Parameters on a <ja>@RestMethod</ja>-annotated method.
 * 	<li>
 * 		POJO classes used as parameters on a <ja>@RestMethod</ja>-annotated method.
 * </ul>
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
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Swagger values are coalesced from multiple sources in the following order of precedence:
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
 *
 * <h5 class='topic'>Client-side REST</h5>
 *
 * Annotation applied to Java method arguments of interface proxies to denote that they are the HTTP body of the request.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
 * 		String myProxyMethod(<ja>@Body</ja> MyPojo pojo);
 * 	}
 * </p>
 *
 * <p>
 * The argument can be any of the following types:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Any serializable POJO - Converted to text using the {@link Serializer} registered with the
 * 		<code>RestClient</code>.
 * 	<li>
 * 		{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
 * 	<li>
 * 		{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
 * 	<li>
 * 		<code>HttpEntity</code> - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
 * 	<li>
 * 		<code>NameValuePairs</code> - Converted to a URL-encoded FORM post.
 * </ul>
 *
 * <p>
 * The annotation can also be applied to a bean property field or getter when the argument is annotated with
 * {@link RequestBean @RequestBean}:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 *
 * 	<jk>public interface</jk> MyRequestBean {
 * 		<ja>@Body</ja>
 * 		MyPojo getMyPojo();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies.Body'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces &gt; Body</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,FIELD,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Body {

	//=================================================================================================================
	// Attributes common to all Swagger Parameter objects
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
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>required</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 *
	 * <p>
	 * Determines whether the body is mandatory.
	 *
	 * <p>
	 * If validation is not met during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
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
	boolean required() default false;

	//=================================================================================================================
	// Attributes specific to in=body
	//=================================================================================================================

	/**
	 * <mk>schema</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 *
	 * <p>
	 * The schema defining the type used for the body parameter.
	 *
	 * <p>
	 * This is a required attribute per the swagger definition.
	 * However, if not explicitly specified, the value will be auto-generated using {@link JsonSchemaSerializer}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object.
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

	//=================================================================================================================
	// Other
	//=================================================================================================================

	/**
	 * A serialized example of the body of a request.
	 *
	 * <p>
	 * This is the <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> or String representation of an example of the body.
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
	 * 		The format is any <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> if the object can be converted to a POJO using {@link JsonParser#DEFAULT} or a simple String if the object
	 * 		can be converted from a String.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * Serialized examples of the body of a request.
	 *
	 * <p>
	 * This is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object whose keys are media types and values are string representations of that value.
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
	 * 		The format is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object with string keys (media type) and string values (example for that media type) .
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
	 * This is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object that makes up the swagger information for this parameter-info.
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
	 * 		The format is a <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.JsonDetails.SimplifiedJson'>Simplified JSON</a> object.
	 * 	<li>
	 * 		Automatic validation is NOT performed on input based on attributes in this value.
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

	/**
	 * Equivalent to {@link #value()}.
	 */
	String[] api() default {};

	/**
	 * TODO
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing values from strings.
	 *
	 * <p>
	 * The default value for this parser is inherited from the servlet/method which defaults to {@link OpenApiPartParser}.
	 * <br>You can use {@link SimplePartParser} to parse POJOs that are directly convertible from <code>Strings</code>.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;
}
