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

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.parser.*;

/**
 * Identifies a REST PATH operation Java method on a {@link RestServlet} implementation class.
 *
 * <p>
 * This is a specialized subtype of <c><ja>{@link RestOp @RestOp}(method=<jsf>PATCH</jsf>)</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
@ContextApply(RestPatchAnnotation.RestOpContextApply.class)
@AnnotationGroup(RestOp.class)
public @interface RestPatch {

	/**
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link Rest#clientVersionHeader() @Rest(clientVersionHeader)} which by default is <js>"Client-Version"</js>.
	 *
	 * <p>
	 * This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same
	 * method/path based on the client version.
	 *
	 * <p>
	 * The format of the client version range is similar to that of OSGi versions.
	 *
	 * <p>
	 * In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
	 * <p class='bjava'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bjava'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<ja>@BeanConfig</ja>(swaps=NewToOldSwap.<jk>class</jk>)
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod();
	 * 	}
	 *
	 * <p>
	 * Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into
	 * an older form.
	 * The old method could also just return back a completely different object.
	 * The range can be any of the following:
	 * <ul>
	 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
	 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
	 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#clientVersionHeader(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String clientVersion() default "";

	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#consumes(MediaType...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] consumes() default {};

	/**
	 * Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with this method.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#converters()} - Registering converters with REST resources.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Enable debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * 	<li>
	 * 		Request/response messages are automatically logged.
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> - Debug is enabled for all requests.
	 * 	<li><js>"false"</js> - Debug is disabled for all requests.
	 * 	<li><js>"conditional"</js> - Debug is enabled only for requests that have a <c class='snippet'>Debug: true</c> header.
	 * 	<li><js>""</js> (or anything else) - Debug mode is inherited from class.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#debugEnablement()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String debug() default "";

	/**
	 * Default <c>Accept</c> header.
	 *
	 * <p>
	 * The default value for the <c>Accept</c> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * @return The annotation value.
	 */
	String defaultAccept() default "";

	/**
	 * Default character encoding.
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#defaultCharset(Charset)}
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#defaultCharset(Charset)}
	 * 	<li class='ja'>{@link Rest#defaultCharset}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String defaultCharset() default "";

	/**
	 * Default <c>Content-Type</c> header.
	 *
	 * <p>
	 * The default value for the <c>Content-Type</c> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * @return The annotation value.
	 */
	String defaultContentType() default "";

	/**
	 * Specifies default values for form-data parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getFormParam(String)} when the parameter is not present on the
	 * request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(path=<js>"/*"</js>, defaultRequestFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li class='note'>
	 * 		Key and value is trimmed of whitespace.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestFormData() default {};

	/**
	 * Specifies default values for query parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQueryParam(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(path=<js>"/*"</js>, defaultRequestQueryData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doPost(<ja>@Query</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li class='note'>
	 * 		Key and value is trimmed of whitespace.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestQueryData() default {};

	/**
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <p>
	 * Affects values returned by the following methods:
	 * 	<ul>
	 * 		<li class='jm'>{@link RestRequest#getAttribute(String)}.
	 * 		<li class='jm'>{@link RestRequest#getAttributes()}.
	 * 	</ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(defaultRequestAttributes={<js>"Foo=bar"</js>, <js>"Baz: $C{REST/myAttributeValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestGet</ja>(defaultRequestAttributes={<js>"Foo: bar"</js>})
	 * 		<jk>public</jk> Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * </ul>
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#defaultRequestAttributes(NamedAttribute...)}
	 * 	<li class='ja'>{@link Rest#defaultRequestAttributes()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestAttributes() default {};

	/**
	 * Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doPost()  {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#defaultRequestHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not overwritten during the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/*"</js>, defaultResponseHeaders={<js>"Content-Type: text/json"</js>})
	 * 	<jk>public</jk> String doPost()  {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#defaultResponseHeaders(org.apache.http.Header...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Optional description for the exposed API.
	 *
	 * <p>
	 * This description is used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The value returned by {@link Operation#getDescription()} in the auto-generated swagger.
	 * 	<li>
	 * 		The <js>"$RS{operationDescription}"</js> variable.
	 * 	<li>
	 * 		The description of the method in the Swagger page.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Corresponds to the swagger field <c>/paths/{path}/{method}/description</c>.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Specifies the compression encoders for this method.
	 *
	 * <p>
	 * Encoders are used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p>
	 * This value overrides encoders specified at the class level using {@link Rest#encoders()}.
	 * The {@link org.apache.juneau.encoders.EncoderSet.Inherit} class can be used to include values from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that handles GZIP compression.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		encoders={
	 * 			GzipEncoder.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Define a REST method that can also use a custom encoder.</jc>
	 * 		<ja>@RestPatch</ja>(
	 * 			encoders={
	 * 				EncoderSet.Inherit.<jk>class</jk>, MyEncoder.<jk>class</jk>
	 * 			}
	 * 		)
	 * 		<jk>public void</jk> doPost(MyBean <jv>bean</jv>) {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is:
	 * <p class='bjava'>
	 * 	RestOpContext.Builder <jv>builder</jv> = RestOpContext.<jsm>create</jsm>(<jv>method</jv>,<jv>restContext</jv>);
	 * 	<jv>builder</jv>.getEncoders().set(<jv>classes</jv>);
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Encoders">Encoders</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Method-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with this method.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#guards()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Method matchers.
	 *
	 * <p>
	 * Associates one more more {@link RestMatcher RestMatchers} with this method.
	 *
	 * <p>
	 * Matchers are used to allow multiple Java methods to handle requests assigned to the same URL path pattern, but
	 * differing based on some request attribute, such as a specific header value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jac'>{@link RestMatcher}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestMatcher>[] matchers() default {};

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(
	 * 		maxInput=<js>"100M"</js>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#maxInput(String)}
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#maxInput(String)}
	 * 	<li class='ja'>{@link Rest#maxInput}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maxInput() default "";

	/**
	 * Dynamically apply this annotation to the specified methods.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Specifies the parsers for converting HTTP request bodies into POJOs for this method.
	 *
	 * <p>
	 * Parsers are used to convert the content of HTTP requests into POJOs.
	 * <br>Any of the Juneau framework parsers can be used in this setting.
	 * <br>The parser selected is based on the request <c>Content-Type</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Parser#getMediaTypes()}
	 * </ul>
	 *
	 * <p>
	 * This value overrides parsers specified at the class level using {@link Rest#parsers()}.
	 * The {@link org.apache.juneau.parser.ParserSet.Inherit} class can be used to include values from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that can consume JSON and HTML.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		parsers={
	 * 			JsonParser.<jk>class</jk>,
	 * 			HtmlParser.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Define a REST method that can also consume XML.</jc>
	 * 		<ja>@RestPatch</ja>(
	 * 			parsers={
	 * 				ParserSet.Inherit.<jk>class</jk>, XmlParser.<jk>class</jk>
	 * 			}
	 * 		)
	 * 		<jk>public void</jk> doPost(MyBean <jv>bean</jv>) {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is:
	 * <p class='bjava'>
	 * 	RestOpContext.Builder <jv>builder</jv> = RestOpContext.<jsm>create</jsm>(<jv>method</jv>,<jv>restContext</jv>);
	 * 	<jv>builder</jv>.getParsers().set(<jv>classes</jv>);
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Marshalling">Marshalling</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] parsers() default {};

	/**
	 * Optional path pattern for the specified method.
	 *
	 * <p>
	 * Appending <js>"/*"</js> to the end of the path pattern will make it match any remainder too.
	 * <br>Not appending <js>"/*"</js> to the end of the pattern will cause a 404 (Not found) error to occur if the exact
	 * pattern is not found.
	 *
	 * <p>
	 * The path can contain variables that get resolved to {@link org.apache.juneau.http.annotation.Path @Path} parameters.
	 *
	 * <h5 class='figure'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
	 * </p>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(path=<js>"/myurl/{0}/{1}/{2}/*"</js>)
	 * </p>
	 *
	 * <p>
	 * If you do not specify a path name, then the path name is inferred from the Java method name.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Path is assumed to be "/foo".</jc>
	 * 	<ja>@RestPatch</ja>
	 * 	<jk>public void</jk> foo() {...}
	 * </p>
	 *
	 * <p>
	 * Note that you can also use {@link #value()} to specify the path.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link org.apache.juneau.http.annotation.Path}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] path() default {};

	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#produces(MediaType...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] produces() default {};

	/**
	 * Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access this method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<ja>@RestPatch</ja>(
	 * 			path=<js>"/foo"</js>,
	 * 			roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 		)
	 * 		<jk>public</jk> Object doPost() {
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | baz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || baz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li class='note'>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li class='note'>
	 * 		Whitespace is ignored.
	 * 	<li class='note'>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li class='note'>
	 * 		If patterns are used, you must specify the list of declared roles using {@link #rolesDeclared()} or {@link org.apache.juneau.rest.RestOpContext.Builder#rolesDeclared(String...)}.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		When defined on parent/child classes and methods, ALL guards within the hierarchy must pass.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#roleGuard(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String roleGuard() default "";

	/**
	 * Declared roles.
	 *
	 * <p>
	 * A comma-delimited list of all possible user roles.
	 *
	 * <p>
	 * Used in conjunction with {@link #roleGuard()} is used with patterns.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<ja>@RestPatch</ja>(
	 * 			path=<js>"/foo"</js>,
	 * 			rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 			roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 		)
	 * 		<jk>public</jk> Object doPost() {
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#rolesDeclared(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String rolesDeclared() default "";

	/**
	 * Specifies the serializers for marshalling POJOs into response bodies for this method.
	 *
	 * <p>
	 * Serializer are used to convert POJOs to HTTP response bodies.
	 * <br>Any of the Juneau framework serializers can be used in this setting.
	 * <br>The serializer selected is based on the request <c>Accept</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
	 * </ul>
	 *
	 * <p>
	 * This value overrides serializers specified at the class level using {@link Rest#serializers()}.
	 * The {@link org.apache.juneau.serializer.SerializerSet.Inherit} class can be used to include values from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that can produce JSON and HTML.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		serializers={
	 * 			JsonParser.<jk>class</jk>,
	 * 			HtmlParser.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Define a REST method that can also produce XML.</jc>
	 * 		<ja>@RestPatch</ja>(
	 * 			parsers={
	 * 				SerializerSet.Inherit.<jk>class</jk>, XmlParser.<jk>class</jk>
	 * 			}
	 * 		)
	 * 		<jk>public</jk> MyBean doPost() {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is:
	 * <p class='bjava'>
	 * 	RestOpContext.Builder <jv>builder</jv> = RestOpContext.<jsm>create</jsm>(<jv>method</jv>,<jv>restContext</jv>);
	 * 	<jv>builder</jv>.getSerializers().set(<jv>classes</jv>);
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Marshalling">Marshalling</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Optional summary for the exposed API.
	 *
	 * <p>
	 * This summary is used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The value returned by {@link Operation#getSummary()} in the auto-generated swagger.
	 * 	<li>
	 * 		The <js>"$RS{operationSummary}"</js> variable.
	 * 	<li>
	 * 		The summary of the method in the Swagger page.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Corresponds to the swagger field <c>/paths/{path}/{method}/summary</c>.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String summary() default "";

	/**
	 * Provides swagger-specific metadata on this method.
	 *
	 * <p>
	 * Used to populate the auto-generated OPTIONS swagger documentation.
	 *
	 * <p>
	 * The format of this annotation is JSON when all individual parts are concatenated.
	 * <br>The starting and ending <js>'{'</js>/<js>'}'</js> characters around the entire value are optional.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPatch</ja>(
	 * 		path=<js>"/{propertyName}"</js>,
	 *
	 * 		<jc>// Swagger info.</jc>
	 * 		swagger={
	 * 			<js>"parameters:["</js>,
	 * 				<js>"{name:'propertyName',in:'path',description:'The system property name.'},"</js>,
	 * 				<js>"{in:'body',description:'The new system property value.'}"</js>,
	 * 			<js>"],"</js>,
	 * 			<js>"responses:{"</js>,
	 * 				<js>"302: {headers:{Location:{description:'The root URL of this resource.'}}},"</js>,
	 * 				<js>"403: {description:'User is not an admin.'}"</js>,
	 * 			<js>"}"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a>.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		The starting and ending <js>'{'</js>/<js>'}'</js> characters around the entire value are optional.
	 * 	<li class='note'>
	 * 		These values are superimposed on top of any Swagger JSON file present for the resource in the classpath.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link OpSwagger}
	 * 	<li class='jc'>{@link SwaggerProvider}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	OpSwagger swagger() default @OpSwagger;

	/**
	 * REST method path.
	 *
	 * <p>
	 * Can be used to provide a shortened form for the {@link #path()} value.
	 *
	 * <p>
	 * The following examples are considered equivalent.
	 * <p class='bjava'>
	 * 	<jc>// Normal form</jc>
	 * 	<ja>@RestPatch</ja>(path=<js>"/{propertyName}"</js>)
	 *
	 * 	<jc>// Shortened form</jc>
	 * 	<ja>@RestPatch</ja>(<js>"/{propertyName}"</js>)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
