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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.vars.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 *
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as
 * a REST resource.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestAnnotation}
 * </ul>
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
@ContextApply({RestAnnotation.RestContextApply.class,RestAnnotation.RestOpContextApply.class})
public @interface Rest {

	/**
	 * Disable body URL parameter.
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?body=(name='John%20Smith',age=45)
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#disableBodyParam()}
	 * </ul>
	 */
	String disableBodyParam() default "";

	/**
	 * Configuration property:  Allowed header URL parameters.
	 *
	 * <p>
	 * When specified, allows headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?Accept=text/json&amp;Content-Type=text/json
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#allowedHeaderParams(String)}
	 * </ul>
	 */
	String allowedHeaderParams() default "";

	/**
	 * Configuration property:  Allowed method headers.
	 *
	 * <p>
	 * A comma-delimited list of HTTP method names that are allowed to be passed as values in an <c>X-Method</c> HTTP header
	 * to override the real HTTP method name.
	 * <p>
	 * Allows you to override the actual HTTP method with a simulated method.
	 * <br>For example, if an HTTP Client API doesn't support <c>PATCH</c> but does support <c>POST</c> (because
	 * <c>PATCH</c> is not part of the original HTTP spec), you can add a <c>X-Method: PATCH</c> header on a normal
	 * <c>HTTP POST /foo</c> request call which will make the HTTP call look like a <c>PATCH</c> request in any of the REST APIs.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Method names are case-insensitive.
	 * 	<li>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 */
	String allowedMethodHeaders() default "";

	/**
	 * Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?method=OPTIONS
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#allowedMethodParams(String)}
	 * </ul>
	 */
	String allowedMethodParams() default "";

	/**
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The default call logger if not specified is {@link BasicRestLogger}.
	 * 	<li>
	 * 		The resource class itself will be used if it implements the {@link RestLogger} interface and not
	 * 		explicitly overridden via this annotation.
	 * 	<li>
	 * 		The implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * </ul>
	 */
	Class<? extends RestLogger> callLogger() default RestLogger.Null.class;

	/**
	 * The resolver used for resolving instances of child resources and various other beans including:
	 * <ul>
	 * 	<li>{@link RestLogger}
	 * 	<li>{@link SwaggerProvider}
	 * 	<li>{@link FileFinder}
	 * 	<li>{@link StaticFiles}
	 * </ul>
	 *
	 * <p>
	 * Note that the <c>SpringRestServlet</c> classes uses the <c>SpringBeanStore</c> class to allow for any
	 * Spring beans to be injected into your REST resources.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_beanStore}
	 * 	<li class='link'>{@doc RestInjection}
	 * </ul>
	 */
	Class<? extends BeanStore> beanStore() default BeanStore.Null.class;

	/**
	 * REST children.
	 *
	 * <p>
	 * Defines children of this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#children}
	 * </ul>
	 */
	Class<?>[] children() default {};

	/**
	 * Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#clientVersionHeader(String)}
	 * </ul>
	 */
	String clientVersionHeader() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 *
	 * <p>
	 * The configuration file .
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Use the keyword <c>SYSTEM_DEFAULT</c> to refer to the system default configuration
	 * 		returned by the {@link Config#getSystemDefault()}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#config(Config)}
	 * </ul>
	 */
	String config() default "";

	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#consumes(MediaType...)}
	 * </ul>
	 */
	String[] consumes() default {};

	/**
	 * REST context class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestContext} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#contextClass(Class)}
	 * </ul>
	 */
	Class<? extends RestContext> contextClass() default RestContext.Null.class;

	/**
	 * Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_converters}
	 * </ul>
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
	 * 		HTTP requests/responses are logged to the registered {@link RestLogger}.
	 * </ul>
	 *
	 * <p>
	 * Possible values (case insensitive):
	 * <ul>
	 * 	<li><js>"true"</js> - Debug is enabled for all requests.
	 * 	<li><js>"false"</js> - Debug is disabled for all requests.
	 * 	<li><js>"conditional"</js> - Debug is enabled only for requests that have a <c class='snippet'>X-Debug: true</c> header.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		These debug settings can be overridden by the {@link Rest#debugOn()} annotation or at runtime by directly
	 * 		calling {@link RestRequest#setDebug()}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * </ul>
	 */
	String debug() default "";

	/**
	 * Debug enablement bean.
	 *
	 * TODO
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_debugEnablement}
	 * </ul>
	 */
	Class<? extends DebugEnablement> debugEnablement() default DebugEnablement.Null.class;

	/**
	 * Enable debug mode on specified classes/methods.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes on matching classes and methods.
	 * 	<li>
	 * 		HTTP requests/responses are logged to the registered {@link RestLogger}.
	 * </ul>
	 *
	 * <p>
	 * Consists of a comma-delimited list of strings of the following forms:
	 * <ul>
	 * 	<li><js>"class-identifier"</js> - Enable debug on the specified class.
	 * 	<li><js>"class-identifier=[true|false|per-request]"</js> - Explicitly enable debug on the specified class.
	 * 	<li><js>"method-identifier"</js> - Enable debug on the specified class.
	 * 	<li><js>"method-identifier=[true|false|per-request]"</js> - Explicitly enable debug on the specified class.
	 * </ul>
	 *
	 * <p>
	 * Class identifiers can be any of the following forms:
	 * <ul>
	 * 	<li>Fully qualified:
	 * 		<ul>
	 * 			<li><js>"com.foo.MyClass"</js>
	 * 		</ul>
	 * 	<li>Fully qualified inner class:
	 * 		<ul>
	 * 			<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 		</ul>
	 * 	<li>Simple:
	 * 		<ul>
	 * 			<li><js>"MyClass"</js>
	 * 		</ul>
	 * 	<li>Simple inner:
	 * 		<ul>
	 * 			<li><js>"MyClass$Inner1$Inner2"</js>
	 * 			<li><js>"Inner1$Inner2"</js>
	 * 			<li><js>"Inner2"</js>
	 * 		</ul>
	 * </ul>
	 *
	 * <p>
	 * Method identifiers can be any of the following forms:
	 * <ul>
	 * 	<li>Fully qualified with args:
	 * 		<ul>
	 * 			<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 			<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 			<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 		</ul>
	 * 	<li>Fully qualified:
	 * 		<ul>
	 * 			<li><js>"com.foo.MyClass.myMethod"</js>
	 * 		</ul>
	 * 	<li>Simple with args:
	 * 		<ul>
	 * 			<li><js>"MyClass.myMethod(String,int)"</js>
	 * 			<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 			<li><js>"MyClass.myMethod()"</js>
	 * 		</ul>
	 * 	<li>Simple:
	 * 		<ul>
	 * 			<li><js>"MyClass.myMethod"</js>
	 * 		</ul>
	 * 	<li>Simple inner class:
	 * 		<ul>
	 * 			<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 			<li><js>"Inner1$Inner2.myMethod"</js>
	 * 			<li><js>"Inner2.myMethod"</js>
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Turn on debug per-request on the class and always on the doX() method</jc>.
	 * 	<ja>@Rest</ja>(
	 * 		debugOn=<js>"MyResource=per-request,Mysource.doX=true"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 *		<ja>@RestGet</ja>
	 *		<jk>public void</jk> String getX() {
	 *			...
	 *		}
	 * </p>
	 *
	 * <p>
	 * A more-typical scenario is to pull this setting from an external source such as system property or environment
	 * variable:
	 *
	 * <h5 class='figure'>Example:</h5
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		debugOn=<js>"$E{DEBUG_ON_SETTINGS}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		These debug settings override the settings define via {@link Rest#debug()} and {@link RestOp#debug()}.
	 * 	<li>
	 * 		These debug settings can be overridden at runtime by directly calling {@link RestRequest#setDebug()}.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_debugOn}
	 * </ul>
	 */
	String debugOn() default "";

	/**
	 * Default <c>Accept</c> header.
	 *
	 * <p>
	 * The default value for the <c>Accept</c> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String defaultAccept() default "";

	/**
	 * Default character encoding.
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#defaultCharset(Charset)}
	 * 	<li class='jm'>{@link RestOpContextBuilder#defaultCharset(Charset)}
	 * 	<li class='ja'>{@link RestOp#defaultCharset}
	 * 	<li class='ja'>{@link RestGet#defaultCharset}
	 * 	<li class='ja'>{@link RestPut#defaultCharset}
	 * 	<li class='ja'>{@link RestPost#defaultCharset}
	 * 	<li class='ja'>{@link RestDelete#defaultCharset}
	 * </ul>
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String defaultContentType() default "";

	/**
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 */
	String[] defaultRequestAttributes() default {};

	/**
	 * Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Optional servlet description.
	 *
	 * <p>
	 * It is used to populate the Swagger description field.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		The format is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * Compression encoders.
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_encoders}
	 * </ul>
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * File finder.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes including:
	 * <ul>
	 * 	<li>Resolution of {@link FileVar $F} variable contents.
	 * </ul>
	 *
	 * <p>
	 * The file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getFileFinder()}
	 * 	<li class='jm'>{@link RestRequest#getFileFinder()}
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 */
	Class<? extends FileFinder> fileFinder() default FileFinder.Null.class;

	/**
	 * Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_guards}
	 * </ul>
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#maxInput(String)}
	 * 	<li class='jm'>{@link RestOpContextBuilder#maxInput(String)}
	 * 	<li class='ja'>{@link RestOp#maxInput}
	 * 	<li class='ja'>{@link RestPost#maxInput}
	 * 	<li class='ja'>{@link RestPut#maxInput}
	 * </ul>
	 */
	String maxInput() default "";

	/**
	 * Messages.
	 *
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * There are two possible formats:
	 * <ul>
	 * 	<li>A simple string - Represents the {@link MessagesBuilder#name(String) name} of the resource bundle.
	 * 		<br><br><i>Example:</i>
	 * 		<p class='bcode w800'>
	 * 	<jc>// Bundle name is Messages.properties.</jc>
	 * 	<ja>@Rest</ja>(messages=<js>"Messages"</js>)
	 * 		</p>
	 * 	<li>Simplified JSON - Represents parameters for the {@link MessagesBuilder} class.
	 * 		<br><br><i>Example:</i>
	 * 		<p class='bcode w800'>
	 * 	<jc>// Bundles can be found in two packages.</jc>
	 * 	<ja>@Rest</ja>(messages=<js>"{name:'Messages',baseNames:['{package}.{name}','{package}.i18n.{name}']"</js>)
	 * 		</p>
	 * </ul>
	 *
	 * <p>
	 * If the bundle name is not specified, the class name of the resource object is used.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_messages}
	 * </ul>
	 */
	String messages() default "";

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
	 * Parsers.
	 *
	 * <p>
	 * If no value is specified, the parsers are inherited from parent class.
	 * <br>Otherwise, this value overrides the parsers defined on the parent class.
	 *
	 * <p>
	 * Use {@link Inherit} to inherit parsers defined on the parent class.
	 *
	 * <p>
	 * Use {@link None} to suppress inheriting parsers defined on the parent class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_parsers}
	 * </ul>
	 */
	Class<?>[] parsers() default {};

	/**
	 * HTTP part parser.
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 */
	Class<? extends HttpPartParser> partParser() default HttpPartParser.Null.class;

	/**
	 * HTTP part serializer.
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 */
	Class<? extends HttpPartSerializer> partSerializer() default HttpPartSerializer.Null.class;

	/**
	 * Resource path.
	 *
	 * <p>
	 * Used in the following situations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		On child resources (resource classes attached to parents via the {@link #children()} annotation) to identify
	 * 		the subpath used to access the child resource relative to the parent.
	 * 	<li>
	 * 		On top-level {@link RestServlet} classes deployed as Spring beans when <c>JuneauRestInitializer</c> is being used.
	 * </ul>
	 *
	 * <h5 class='topic'>On child resources</h5>
	 * <p>
	 * The typical usage is to define a path to a child resource relative to the parent resource.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		children={ChildResource.<jk>class</jk>}
	 * 	)
	 * 	<jk>public class</jk> TopLevelResource <jk>extends</jk> BasicRestServlet {...}
	 *
	 * 	<ja>@Rest</ja>(
	 *		path=<js>"/child"</js>,
	 *		children={GrandchildResource.<jk>class</jk>}
	 *	)
	 *	<jk>public class</jk> ChildResource {...}
	 *
	 *	<ja>@Rest</ja>(
	 *		path=<js>"/grandchild"</js>
	 *	)
	 *	<jk>public class</jk> GrandchildResource {
	 *		<ja>@RestGet</ja>(js>"/"</js>)
	 *		<jk>public</jk> String sayHello() {
	 *			<jk>return</jk> <js>"Hello!"</js>;
	 *		}
	 *	}
	 * </p>
	 * <p>
	 * In the example above, assuming the <c>TopLevelResource</c> servlet is deployed to path <c>/myContext/myServlet</c>,
	 * then the <c>sayHello</c> method is accessible through the URI <c>/myContext/myServlet/child/grandchild</c>.
	 *
	 * <p>
	 * Note that in this scenario, the <c>path</c> attribute is not defined on the top-level resource.
	 * Specifying the path on the top-level resource has no effect, but can be used for readability purposes.
	 *
	 * <h5 class='topic'>On top-level resources deployed as Spring beans</h5>
	 * <p>
	 * The path can also be used on top-level resources deployed as Spring beans when used with the <c>JuneauRestInitializer</c>
	 * Spring Boot initializer class:
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@SpringBootApplication</ja>
	 * 	<ja>@Controller</ja>
	 * 	<jk>public class</jk> App {
	 *
	 *		<jc>// Our entry-point method.</jc>
	 * 		<jk>public static void</jk> main(String[] <jv>args</jv>) {
	 * 			<jk>new</jk> SpringApplicationBuilder(App.<jk>class</jk>)
	 * 				.initializers(<jk>new</jk> JuneauRestInitializer(App.<jk>class</jk>))
	 * 				.run(<jv>args</jv>);
	 * 		}
	 *
	 * 		<jc>// Our top-level servlet.</jc>
	 * 		<ja>@Bean</ja>
	 * 		<ja>@JuneauRestRoot</ja>
	 * 		<jk>public</jk> MyResource getMyResource() {
	 * 			<jk>return new</jk> MyResource();
	 * 		}
	 * 	}
	 *
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/myResource"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {...}
	 * </p>
	 *
	 * <p>
	 * In this case, the servlet will get registered using the path defined on the resource class.
	 *
	 * <h5 class='topic'>Path variables</h5>
	 * <p>
	 * The path can contain variables that get resolved to {@link org.apache.juneau.http.annotation.Path @Path} parameters
	 * or access through the {@link RestRequest#getPathParams()} method.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/myResource/{foo}/{bar}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 *		<ja>@RestGet</ja>(<js>"/{baz}"</js>)
	 *		<jk>public void</jk> String doX(<ja>@Path</ja> String <jv>foo</jv>, <ja>@Path</ja> <jk>int</jk> <jv>bar</jv>, <ja>@Path</ja> MyPojo <jv>baz</jv>) {
	 *			...
	 *		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Variables can be used on either top-level or child resources and can be defined on multiple levels.
	 *
	 * <p>
	 * All variables in the path must be specified or else the target will not resolve and a <c>404</c> will result.
	 *
	 * <p>
	 * When variables are used on a path of a top-level resource deployed as a Spring bean in a Spring Boot application,
	 * the first part of the URL must be a literal which will be used as the servlet path of the registered servlet.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The leading slash is optional.  <js>"/myResource"</js> and <js>"myResource"</js> is equivalent.
	 * 	<li>
	 * 		The paths <js>"/myResource"</js> and <js>"/myResource/*"</js> are equivalent.
	 * 	<li>
	 * 		Paths must not end with <js>"/"</js> (per the servlet spec).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#path(String)}
	 * </ul>
	 */
	String path() default "";

	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#produces(MediaType...)}
	 * </ul>
	 */
	String[] produces() default {};

	/**
	 * Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_renderResponseStackTraces}
	 * </ul>
	 */
	String renderResponseStackTraces() default "";

	/**
	 * Response processors.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseProcessor} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseProcessors}
	 * </ul>
	 */
	Class<? extends ResponseProcessor>[] responseProcessors() default {};

	/**
	 * REST children class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestChildren} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#restChildrenClass(Class)}
	 * </ul>
	 */
	Class<? extends RestChildren> restChildrenClass() default RestChildren.Null.class;

	/**
	 * REST methods class.
	 *
	 * <p>
	 * Allows you to extend the {@link RestOperations} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#restOperationsClass(Class)}
	 * </ul>
	 */
	Class<? extends RestOperations> restOperationsClass() default RestOperations.Null.class;

	/**
	 * Allows you to extend the {@link RestOpContext} class to modify how any of the methods are implemented.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#restOpContextClass(Class)}
	 * </ul>
	 */
	Class<? extends RestOpContext> restOpContextClass() default RestOpContext.Null.class;

	/**
	 * Java REST operation method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * <br>This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_restOperationArgs}
	 * </ul>
	 */
	Class<? extends RestOpArg>[] restOpArgs() default {};

	/**
	 * Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
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
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link #rolesDeclared()} or {@link RestContext#REST_rolesDeclared}.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_roleGuard}
	 * </ul>
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
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_rolesDeclared}
	 * </ul>
	 */
	String rolesDeclared() default "";

	/**
	 * Serializers.
	 *
	 * <p>
	 * If no value is specified, the serializers are inherited from parent class.
	 * <br>Otherwise, this value overrides the serializers defined on the parent class.
	 *
	 * <p>
	 * Use {@link Inherit} to inherit serializers defined on the parent class.
	 *
	 * <p>
	 * Use {@link None} to suppress inheriting serializers defined on the parent class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * </ul>
	 */
	Class<?>[] serializers() default {};

	/**
	 * Optional site name.
	 *
	 * <p>
	 * The site name is intended to be a title that can be applied to the entire site.
	 *
	 * <p>
	 * One possible use is if you want to add the same title to the top of all pages by defining a header on a
	 * common parent class like so:
	 * <p class='bcode w800'>
	 *  <ja>@HtmlDocConfig</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$RS{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$RS{title}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String siteName() default "";

	/**
	 * Static files.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRestObject#getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <p>
	 * The static file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getStaticFiles()}
	 * 	<li class='jm'>{@link RestRequest#getStaticFiles()}
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 */
	Class<? extends StaticFiles> staticFiles() default StaticFiles.Null.class;

	/**
	 * Provides swagger-specific metadata on this resource.
	 *
	 * <p>
	 * Used to populate the auto-generated OPTIONS swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/addressBook"</js>,
	 *
	 * 		<jc>// Swagger info.</jc>
	 * 		swagger=@Swagger({
	 * 			<js>"contact:{name:'John Smith',email:'john@smith.com'},"</js>,
	 * 			<js>"license:{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'},"</js>,
	 * 			<js>"version:'2.0',</js>,
	 * 			<js>"termsOfService:'You are on your own.',"</js>,
	 * 			<js>"tags:[{name:'Java',description:'Java utility',externalDocs:{description:'Home page',url:'http://juneau.apache.org'}}],"</js>,
	 * 			<js>"externalDocs:{description:'Home page',url:'http://juneau.apache.org'}"</js>
	 * 		})
	 * 	)
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Swagger}
	 * </ul>
	 */
	Swagger swagger() default @Swagger;

	/**
	 * Configuration property:  Swagger provider.
	 *
	 * TODO
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_swaggerProvider}
	 * </ul>
	 */
	Class<? extends SwaggerProvider> swaggerProvider() default SwaggerProvider.Null.class;

	/**
	 * Optional servlet title.
	 *
	 * <p>
	 * It is used to populate the Swagger title field.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <c>/info/title</c>.
	 * </ul>
	 */
	String[] title() default {};

	/**
	 * Resource authority path.
	 *
	 * <p>
	 * Overrides the authority path value for this resource and any child resources.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriAuthority(String)}
	 * </ul>
	 */
	String uriAuthority() default "";

	/**
	 * Resource context path.
	 *
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriContext(String)}
	 * </ul>
	 */
	String uriContext() default "";

	/**
	 * URI-resolution relativity.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriRelativity(UriRelativity)}
	 * </ul>
	 */
	String uriRelativity() default "";

	/**
	 * URI-resolution.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#uriResolution(UriResolution)}
	 * </ul>
	 */
	String uriResolution() default "";
}
