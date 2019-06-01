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

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 *
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as
 * a REST resource.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.RestResource}
 * </ul>
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(RestResourceConfigApply.class)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_allowBodyParam}
	 * </ul>
	 */
	String allowBodyParam() default "";

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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_allowHeaderParams}
	 * </ul>
	 */
	String allowHeaderParams() default "";

	/**
	 * Class-level bean filters.
	 *
	 * <p>
	 * Shortcut to add bean filters to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @deprecated Use {@link BeanConfig#beanFilters()}
	 */
	@Deprecated
	Class<?>[] beanFilters() default {};

	/**
	 * REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Use the keyword <code>SYSTEM_DEFAULT</code> to refer to the system default configuration
	 * 		returned by the {@link Config#getSystemDefault()}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_converters}
	 * </ul>
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Default <code>Accept</code> header.
	 *
	 * <p>
	 * The default value for the <code>Accept</code> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_defaultCharset}
	 * </ul>
	 */
	String defaultCharset() default "";

	/**
	 * Default <code>Content-Type</code> header.
	 *
	 * <p>
	 * The default value for the <code>Content-Type</code> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		The format is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_encoders}
	 * </ul>
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Shortcut for setting {@link #properties()} of simple boolean types.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation}
	 * </ul>
	 *
	 * @deprecated Use {@link HtmlDocConfig}
	 */
	@Deprecated
	HtmlDoc htmldoc() default @HtmlDoc;

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_logger}
	 * </ul>
	 */
	Class<? extends RestLogger> logger() default RestLogger.Null.class;

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_maxInput}
	 * </ul>
	 */
	String maxInput() default "";

	/**
	 * Messages.
	 *
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_mimeTypes}
	 * </ul>
	 */
	String[] mimeTypes() default {};

	/**
	 * Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * <br>This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 *
	 * @deprecated Use {@link ParserConfig#listener()}
	 */
	@Deprecated
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 */
	Class<? extends HttpPartSerializer> partSerializer() default HttpPartSerializer.Null.class;

	/**
	 * Resource path.
	 *
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_path}
	 * </ul>
	 */
	String path() default "";

	/**
	 * Class-level POJO swaps.
	 *
	 * <p>
	 * Shortcut to add POJO swaps to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @deprecated {@link BeanConfig#pojoSwaps()}
	 */
	@Deprecated
	Class<?>[] pojoSwaps() default {};

	/**
	 * Class-level properties.
	 *
	 * <p>
	 * Shortcut for specifying class-level properties on this servlet to the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 *
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <p>
	 * In some cases, properties can be overridden at runtime through the
	 * {@link RestResponse#prop(String, Object)} method or through an {@link RequestProperties}
	 * method parameter.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE && ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE && ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* & *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* && *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
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
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @deprecated Use {@link SerializerConfig#listener()}
	 */
	@Deprecated
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_staticFileResponseHeaders}
	 * </ul>
	 */
	String[] staticFileResponseHeaders() default {};

	/**
	 * Static file mappings.
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/info/title</code>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_uriResolution}
	 * </ul>
	 */
	String uriResolution() default "";

	/**
	 * Configuration property:  Use classpath resource caching.
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestRequest#getClasspathReaderResource(String, boolean)} (and related
	 * methods) will be cached in memory to speed subsequent lookups.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_useStackTraceHashes}
	 * </ul>
	 */
	String useStackTraceHashes() default "";

	/**
	 * Enable debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul>
	 * 	<li>A message and stack trace is printed to STDERR when {@link BasicRestCallHandler#handleError(HttpServletRequest, HttpServletResponse, Throwable)} is called.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * </ul>
	 */
	String debug() default "";
}
