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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use {@link Rest}
 * </div>
 *
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as
 * a REST resource.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestAnnotation}
 * </ul>
 */
@SuppressWarnings("deprecation")
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(RestResourceConfigApply.class)
@Deprecated
public @interface RestResource {

	/**
	 * Allow body URL parameter.
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
	 * 	<li class='jf'>{@link RestContext#REST_allowBodyParam}
	 * </ul>
	 */
	String allowBodyParam() default "";

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
	 * 	<li class='jf'>{@link RestContext#REST_allowedHeaderParams}
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
	 * 	<li class='jf'>{@link RestContext#REST_allowedMethodParams}
	 * </ul>
	 */
	String allowedMethodParams() default "";

	/**
	 * Allow header URL parameters.
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
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
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowHeaderParams}
	 * </ul>
	 */
	String allowHeaderParams() default "";

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
	 * 	<li class='jf'>{@link RestContext#REST_reqAttrs}
	 * </ul>
	 */
	String[] attrs() default {};

	/**
	 * Class-level bean filters.
	 *
	 * <p>
	 * Shortcut to add bean filters to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 */
	Class<?>[] beanFilters() default {};

	/**
	 * REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callHandler}
	 * </ul>
	 */
	Class<? extends RestCallHandler> callHandler() default RestCallHandler.Null.class;

	/**
	 * REST children.
	 *
	 * <p>
	 * Defines children of this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 */
	Class<?>[] children() default {};

	/**
	 * Classpath resource finder.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_classpathResourceFinder}
	 * </ul>
	 */
	Class<? extends ClasspathResourceFinder> classpathResourceFinder() default ClasspathResourceFinder.Null.class;

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
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
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
	 * 	<li class='jf'>{@link RestContext#REST_defaultCharset}
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
	 * 	<li class='jf'>{@link RestContext#REST_reqHeaders}
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
	 * 	<li class='jf'>{@link RestContext#REST_resHeaders}
	 * </ul>
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Optional servlet description.
	 *
	 * <p>
	 * It is used to populate the Swagger description field.
	 * <br>This value can be retrieved programmatically through the {@link RestRequest#getResourceDescription()} method.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		The format is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestInfoProvider#getDescription(RestRequest)}
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
	 * Shortcut for setting {@link #properties()} of simple boolean types.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Setting a flag is equivalent to setting the same property to <js>"true"</js>.
	 * </ul>
	 */
	String[] flags() default {};

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
	 * Provides HTML-doc-specific metadata on this method.
	 *
	 * <p>
	 * Used to customize the output from the HTML Doc serializer.
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/addressBook"</js>,
	 *
	 * 		<jc>// Links on the HTML rendition page.
	 * 		// "request:/..." URIs are relative to the request URI.
	 * 		// "servlet:/..." URIs are relative to the servlet URI.
	 * 		// "$C{...}" variables are pulled from the config file.</jc>
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			<jc>// Widgets for $W variables.</jc>
	 * 			widgets={
	 * 				PoweredByJuneau.<jk>class</jk>,
	 * 				ContentTypeLinks.<jk>class</jk>
	 * 			}
	 * 			navlinks={
	 * 				<js>"up: request:/.."</js>,
	 * 				<js>"options: servlet:/?method=OPTIONS"</js>,
	 * 				<js>"stats: servlet:/stats"</js>,
	 * 				<js>"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/addressbook/AddressBookResource.java"</js>,
	 * 			},
	 * 			aside={
	 * 				<js>"&lt;div style='max-width:400px;min-width:200px'&gt;"</js>,
	 * 				<js>"	&lt;p&gt;Proof-of-concept resource that shows off the capabilities of working with POJO resources.&lt;/p&gt;"</js>,
	 * 				<js>"	&lt;p&gt;Provides examples of: &lt;/p&gt;"</js>,
	 * 				<js>"		&lt;ul&gt;"</js>,
	 * 				<js>"			&lt;li&gt;XML and RDF namespaces"</js>,
	 * 				<js>"			&lt;li&gt;Swagger documentation"</js>,
	 * 				<js>"			&lt;li&gt;Widgets"</js>,
	 * 				<js>"		&lt;/ul&gt;"</js>,
	 * 				<js>"	&lt;p style='text-weight:bold;text-decoration:underline;'&gt;Available Content Types&lt;/p&gt;"</js>,
	 * 				<js>"	$W{ContentTypeLinks}"</js>,
	 * 				<js>"&lt;/div&gt;"</js>
	 * 			},
	 * 			footer=<js>"$W{PoweredByJuneau}"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestHtmlDocAnnotation}
	 * </ul>
	 */
	HtmlDoc htmldoc() default @HtmlDoc;

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
	 * </ul>
	 */
	Class<? extends RestInfoProvider> infoProvider() default RestInfoProvider.Null.class;

	/**
	 * REST logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_logger}
	 * </ul>
	 */
	Class<? extends RestLogger> logger() default RestLogger.Null.class;

	/**
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callLogger}
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * </ul>
	 */
	Class<? extends RestCallLogger> callLogger() default RestCallLogger.Null.class;

	/**
	 * Specifies rules on how to handle logging of HTTP requests/responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_callLoggerConfig}
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * </ul>
	 */
	Logging logging() default @Logging;

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
	 * 	<li class='jf'>{@link RestContext#REST_maxInput}
	 * </ul>
	 */
	String maxInput() default "";

	/**
	 * Messages.
	 *
	 * Identifies the location of the resource bundle for this class.
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
	 * Configuration property:  MIME types.
	 *
	 * <p>
	 * Defines MIME-type file type mappings.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_mimeTypes}
	 * </ul>
	 */
	String[] mimeTypes() default {};

	/**
	 * Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * <br>This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_paramResolvers}
	 * </ul>
	 */
	Class<? extends RestMethodParam>[] paramResolvers() default {};

	/**
	 * Parser listener.
	 *
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 */
	Class<? extends ParserListener> parserListener() default ParserListener.Null.class;

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
	 * <p class='bpcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		children={ChildResource.<jk>class</jk>}
	 * 	)
	 * 	<jk>public class</jk> TopLevelResource <jk>extends</jk> BasicRestServlet {...}
	 *
	 * 	<ja>@RestResource</ja>(
	 *		path=<js>"/child"</js>,
	 *		children={GrandchildResource.<jk>class</jk>}
	 *	)
	 *	<jk>public class</jk> ChildResource {...}
	 *
	 *	<ja>@RestResource</ja>(
	 *		path=<js>"/grandchild"</js>
	 *	)
	 *	<jk>public class</jk> GrandchildResource {
	 *		<ja>@RestMethod</ja>(
	 *			path=<js>"/"</js>
	 *		)
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
	 * <p class='bpcode'>
	 * 	<ja>@SpringBootApplication</ja>
	 * 	<ja>@Controller</ja>
	 * 	<jk>public class</jk> App {
	 *
	 *		<jc>// Our entry-point method.</jc>
	 * 		<jk>public static void</jk> main(String[] args) {
	 * 			<jk>new</jk> SpringApplicationBuilder(App.<jk>class</jk>)
	 * 				.initializers(<jk>new</jk> JuneauRestInitializer(App.<jk>class</jk>))
	 * 				.run(args);
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
	 * 	<ja>@RestResource</ja>(
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
	 * or access through the {@link RestRequest#getPathMatch()} method.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bpcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myResource/{foo}/{bar}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 *		<ja>@RestMethod</ja>(
	 *			path=<js>"/{baz}"</js>
	 *		)
	 *		<jk>public void</jk> String doX(<ja>@Path</ja> String foo, <ja>@Path</ja> <jk>int</jk> bar, <ja>@Path</ja> MyPojo baz) {
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
	 * 	<li class='jf'>{@link RestContext#REST_path}
	 * </ul>
	 */
	String path() default "";

	/**
	 * Class-level POJO swaps.
	 *
	 * <p>
	 * Shortcut to add POJO swaps to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_swaps}
	 * </ul>
	 */
	Class<?>[] pojoSwaps() default {};

	/**
	 * Class-level properties.
	 *
	 * <p>
	 * Shortcut to add properties to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#set(String,Object)}
	 * 	<li class='jm'>{@link RestContextBuilder#set(java.util.Map)}
	 * </ul>
	 */
	Property[] properties() default {};

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
	 * REST resource resolver.
	 *
	 * <p>
	 * The resolver used for resolving child resources.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resourceResolver}
	 * </ul>
	 */
	Class<? extends RestResourceResolver> resourceResolver() default RestResourceResolver.Null.class;

	/**
	 * Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseHandlers}
	 * </ul>
	 */
	Class<? extends ResponseHandler>[] responseHandlers() default {};

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
	 * 	<ja>@RestResource</ja>(
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
	 * Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
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
	 * Serializer listener.
	 *
	 * <p>
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 */
	Class<? extends SerializerListener> serializerListener() default SerializerListener.Null.class;

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
	 * This value can be retrieved programmatically through the {@link RestRequest#getSiteName()} method.
	 *
	 * <p>
	 * One possible use is if you want to add the same title to the top of all pages by defining a header on a
	 * common parent class like so:
	 * <p class='bcode w800'>
	 * 	htmldoc=<ja>@HtmlDoc</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$R{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$R{resourceTitle}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestInfoProvider#getSiteName(RestRequest)}
	 * </ul>
	 */
	String siteName() default "";

	/**
	 * Static file response headers.
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFileResponseHeaders={
	 * 			<js>"Cache-Control: $C{REST/cacheControl,nocache}"</js>,
	 * 			<js>"My-Header: $C{REST/myHeaderValue}"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.staticFileResponseHeader(<js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 				.staticFileResponseHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder
	 * 				.addTo(<jsf>REST_staticFileResponseHeaders</jsf>, <js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 				.addTo(<jsf>REST_staticFileResponseHeaders</jsf>, <js>"My-Header"</js>, <js>"foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.staticFileResponseHeader(<js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Note that headers can also be specified per path-mapping via the {@link RestResource#staticFiles() @RestResource(staticFiles)} annotation.
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFiles={
	 * 			<js>"htdocs:docs:{'Cache-Control':'max-age=86400, public'}"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 */
	String[] staticFileResponseHeaders() default {};

	/**
	 * Static file mappings.
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents
	 * from the classpath or file system.
	 *
	 * <p>
	 * The format of the value is one of the following:
	 * <ol class='spaced-list'>
	 * 	<li><js>"path:location"</js>
	 * 	<li><js>"path:location:headers"</js>
	 * </ol>
	 *
	 * <p>
	 * An example where this class is used is in the {@link RestResource#staticFiles} annotation:
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles={
	 * 			<js>"htdocs:docs"</js>,
	 * 			<js>"styles:styles"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {...}
	 * </p>
	 *
	 * <p>
	 * In the example above, given a GET request to the following URL...
	 * <p class='bcode w800'>
	 *  	/myresource/htdocs/foobar.html
	 * </p>
	 * <br>...the servlet will attempt to find the <c>foobar.html</c> file in the following location:
	 * <ol class='spaced-list'>
	 * 	<li><c>com.foo.mypackage.docs</c> package.
	 * </ol>
	 *
	 * <p>
	 * The location is interpreted as an absolute path if it starts with <js>'/'</js>.
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFiles={
	 * 			<js>"htdocs:/docs"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p>
	 * In the example above, given a GET request to the following URL...
	 * <p class='bcode w800'>
	 *  	/myresource/htdocs/foobar.html
	 * </p>
	 * <br>...the servlet will attempt to find the <c>foobar.html</c> file in the following location:
	 * <ol class='spaced-list'>
	 * 	<li><c>docs</c> package (typically under <c>src/main/resources/docs</c> in your workspace).
	 * 	<li><c>[working-dir]/docs</c> directory at runtime.
	 * </ol>
	 *
	 * <p>
	 * Response headers can be specified for served files by adding a 3rd section that consists of a {@doc SimplifiedJson} object.
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFiles={
	 * 			<js>"htdocs:docs:{'Cache-Control':'max-age=86400, public'}"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * The same path can map to multiple locations.  Files are searched in the order
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFiles={
	 * 			<jc>// Search in absolute location '/htdocs/folder' before location 'htdocs.package' relative to servlet package.</jc>
	 * 			<js>"htdocs:/htdocs/folder,htdocs:htdocs.package"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Mappings are cumulative from super classes.
	 * 	<li>
	 * 		Child resources can override mappings made on parent class resources.
	 * 		<br>When both parent and child resources map against the same path, files will be search in the child location
	 * 		and then the parent location.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 */
	String[] staticFiles() default {};

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
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 */
	String[] produces() default {};

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
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 */
	String[] consumes() default {};

	/**
	 * Provides swagger-specific metadata on this resource.
	 *
	 * <p>
	 * Used to populate the auto-generated OPTIONS swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/addressBook"</js>,
	 *
	 * 		<jc>// Swagger info.</jc>
	 * 		swagger=@ResourceSwagger({
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
	 * 	<li class='ja'>{@link ResourceSwagger}
	 * 	<li class='jm'>{@link RestInfoProvider#getSwagger(RestRequest)}
	 * </ul>
	 */
	ResourceSwagger swagger() default @ResourceSwagger;

	/**
	 * Optional servlet title.
	 *
	 * <p>
	 * It is used to populate the Swagger title field.
	 * <br>This value can be retrieved programmatically through the {@link RestRequest#getResourceTitle()} method.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <c>/info/title</c>.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestInfoProvider#getTitle(RestRequest)}
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
	 * 	<li class='jf'>{@link RestContext#REST_uriAuthority}
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
	 * 	<li class='jf'>{@link RestContext#REST_uriContext}
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
	 * 	<li class='jf'>{@link RestContext#REST_uriRelativity}
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
	 * 	<li class='jf'>{@link RestContext#REST_uriResolution}
	 * </ul>
	 */
	String uriResolution() default "";

	/**
	 * Configuration property:  Use classpath resource caching.
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestRequest#getClasspathHttpResource(String, boolean)} (and related
	 * methods) will be cached in memory to speed subsequent lookups.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_useClasspathResourceCaching}
	 * </ul>
	 */
	String useClasspathResourceCaching() default "";

	/**
	 * Use stack trace hashes.
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_useStackTraceHashes}
	 * </ul>
	 */
	String useStackTraceHashes() default "";

	/**
	 * Enable debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * </ul>
	 *
	 * <p>
	 * Possible values (case insensitive):
	 * <ul>
	 * 	<li><js>"true"</js> - Debug is enabled for all requests.
	 * 	<li><js>"false"</js> - Debug is disabled for all requests.
	 * 	<li><js>"per-request"</js> - Debug is enabled only for requests that have a <c class='snippet'>X-Debug: true</c> header.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * </ul>
	 */
	String debug() default "";
}
