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
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.httppart.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 *
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as
 * a REST resource.
 *
 * Refer to <a class='doclink' href='../package-summary.html#TOC'>org.apache.juneau.rest</a> doc for information on
 * using this class.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface RestResource {

	/**
	 * Allow body URL parameter.
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowBodyParam} for more information.
	 */
	String allowBodyParam() default "";

	/**
	 * Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowedMethodParams} for more information.
	 */
	String allowedMethodParams() default "";

	/**
	 * Allow header URL parameters.
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_allowHeaderParams} for more information.
	 */
	String allowHeaderParams() default "";

	/**
	 * Class-level bean filters.
	 *
	 * <p>
	 * Shortcut to add bean filters to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 *
	 * <p>
	 * See {@link BeanContext#BEAN_beanFilters} for more information.
	 */
	Class<?>[] beanFilters() default {};

	/**
	 * REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * 
	 * <p>
	 * See {@link RestContext#REST_callHandler} for more information.
	 */
	Class<? extends RestCallHandler> callHandler() default RestCallHandler.class;

	/**
	 *	Children.
	 *
	 * <p>
	 * Defines children of this resource.
	 *
	 * <p>
	 * See {@link RestContext#REST_children} for more information.
	 */
	Class<?>[] children() default {};

	/**
	 * Classpath resource finder. 
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 * 
	 * <p>
	 * See {@link RestContext#REST_classpathResourceFinder} for more information.
	 */
	Class<? extends ClasspathResourceFinder> classpathResourceFinder() default ClasspathResourceFinder.Null.class;

	/**
	 * Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 *
	 * <p>
	 * See {@link RestContext#REST_clientVersionHeader} for more information.
	 */
	String clientVersionHeader() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 *
	 * <p>
	 * The configuration file .
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestContextBuilder#configFile(ConfigFile)} method.
	 */
	String config() default "";

	/**
	 * Resource context path. 
	 * 
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_contextPath} for more information.
	 */
	String contextPath() default "";

	/**
	 * Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * See {@link RestContext#REST_converters} for more information.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Default character encoding.
	 * 
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 *
	 * <p>
	 * See {@link RestContext#REST_defaultCharset} for more information.
	 */
	String defaultCharset() default "";

	/**
	 * Default request headers.
	 * 
	 * <p>
	 * Specifies default values for request headers.
	 *
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_defaultRequestHeaders} for more information.
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_defaultResponseHeaders} for more information.
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Optional servlet description.
	 *
	 * <p>
	 * It is used to populate the Swagger description field.
	 * This value can be retrieved programmatically through the {@link RestRequest#getServletDescription()} method.
	 *
	 * <p>
	 * The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * (e.g. <js>"description = foo"</js> or <js>"MyServlet.description = foo"</js>).
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/info/description</code>.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getDescription(RestRequest)} method.
	 */
	String description() default "";

	/**
	 * Compression encoders. 
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p>
	 * See {@link RestContext#REST_encoders} for more information.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Shortcut for setting {@link #properties()} of simple boolean types.
	 *
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * Setting a flag is equivalent to setting the same property to <js>"true"</js>.
	 */
	String[] flags() default {};

	/**
	 * Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * See {@link RestContext#REST_guards} for more information.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Provides HTML-doc-specific metadata on this method.
	 *
	 * <p>
	 * Used to customize the output from the HTML Doc serializer.
	 * <p class='bcode'>
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
	 */
	HtmlDoc htmldoc() default @HtmlDoc;

	/**
	 * Configuration property:  REST info provider. 
	 * 
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * See {@link RestContext#REST_infoProvider} for more information.
	 */
	Class<? extends RestInfoProvider> infoProvider() default RestInfoProvider.class;

	/**
	 * REST logger.
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * See {@link RestContext#REST_logger} for more information.
	 */
	Class<? extends RestLogger> logger() default RestLogger.Normal.class;

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_maxInput} for more information.
	 */
	String maxInput() default "";
	
	/**
	 * Messages. 
	 * 
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_messages} for more information.
	 */
	String messages() default "";

	/**
	 * Configuration property:  MIME types. 
	 *
	 * <p>
	 * Defines MIME-type file type mappings.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_mimeTypes} for more information.
	 */
	String[] mimeTypes() default {};

	/**
	 * Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * See {@link RestContext#REST_paramResolvers} for more information.
	 */
	Class<? extends RestParam>[] paramResolvers() default {};

	/**
	 * Parser listener.
	 * 
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <p>
	 * See {@link Parser#PARSER_listener} for more information.
	 */
	Class<? extends ParserListener> parserListener() default ParserListener.Null.class;

	/**
	 * Parsers. 
	 *
	 * <p>
	 * Adds class-level parsers to this resource.
	 * 
	 * <p>
	 * See {@link RestContext#REST_parsers} for more information.
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * HTTP part parser. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 * 
	 * <p>
	 * See {@link RestContext#REST_partParser} for more information.
	 */
	Class<? extends HttpPartParser> partParser() default UonPartParser.class;

	/**
	 * HTTP part serializer. 
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * See {@link RestContext#REST_partSerializer} for more information.
	 */
	Class<? extends HttpPartSerializer> partSerializer() default SimpleUonPartSerializer.class;
	
	/**
	 * Resource path.   
	 *
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <p>
	 * See {@link RestContext#REST_path} for more information.
	 */
	String path() default "";

	/**
	 * Class-level POJO swaps.
	 *
	 * <p>
	 * Shortcut to add POJO swaps to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 *
	 * <p>
	 * See {@link BeanContext#BEAN_pojoSwaps} for more information.
	 */
	Class<?>[] pojoSwaps() default {};

	/**
	 * Class-level properties.
	 *
	 * <p>
	 * Shortcut for specifying class-level properties on this servlet to the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <p>
	 * In some cases, properties can be overridden at runtime through the
	 * {@link RestResponse#setProperty(String, Object)} method or through a {@link Properties @Properties} annotated
	 * method parameter.
	 *
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#setProperty(String, Object)}/
	 * {@link RestContextBuilder#setProperties(java.util.Map)} methods.
	 */
	Property[] properties() default {};

	/**
	 * Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 *
	 * <p>
	 * See {@link RestContext#REST_renderResponseStackTraces} for more information.
	 */
	String renderResponseStackTraces() default "";

	/**
	 * REST resource resolver.
	 * 
	 * <p>
	 * The resolver used for resolving child resources.
	 * 
	 * <p>
	 * See {@link RestContext#REST_resourceResolver} for more information.
	 */
	Class<? extends RestResourceResolver> resourceResolver() default RestResourceResolverSimple.class;

	/**
	 * Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * See {@link RestContext#REST_responseHandlers} for more information.
	 */
	Class<? extends ResponseHandler>[] responseHandlers() default {};

	/**
	 * Serializer listener.
	 * 
	 * <p>
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 *
	 * <p>
	 * See {@link Serializer#SERIALIZER_listener} for more information.
	 */
	Class<? extends SerializerListener> serializerListener() default SerializerListener.Null.class;

	/**
	 * Serializers. 
	 *
	 * <p>
	 * Adds class-level serializers to this resource.
	 * 
	 * <p>
	 * See {@link RestContext#REST_serializers} for more information.
	 */
	Class<? extends Serializer>[] serializers() default {};

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
	 * <p class='bcode'>
	 * 	htmldoc=<ja>@HtmlDoc</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$R{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$R{servletTitle}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 * 
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getSiteName(RestRequest)} method.
	 */
	String siteName() default "";

	/**
	 * Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_staticFileResponseHeaders} for more information.
	 */
	String[] staticFileResponseHeaders() default {};
	
	/**
	 * Static file mappings. 
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 * 
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_staticFiles} for more information.
	 */
	String[] staticFiles() default {};
	
	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * 
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_supportedAcceptTypes} for more information.
	 */
	String[] supportedAcceptTypes() default {};
	
	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * Values can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_supportedContentTypes} for more information.
	 */
	String[] supportedContentTypes() default {};
	
	/**
	 * Provides swagger-specific metadata on this resource.
	 *
	 * <p>
	 * Used to populate the auto-generated OPTIONS swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/addressBook"</js>,
	 *
	 * 		<jc>// Swagger info.</jc>
	 * 		swagger=<ja>@ResourceSwagger</ja>(
	 * 			contact=<js>"{name:'John Smith',email:'john@smith.com'}"</js>,
	 * 			license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>,
	 * 			version=<js>"2.0"</js>,
	 * 			termsOfService=<js>"You're on your own."</js>,
	 * 			tags=<js>"[{name:'Java',description:'Java utility',externalDocs:{description:'Home page',url:'http://juneau.apache.org'}}]"</js>,
	 * 			externalDocs=<js>"{description:'Home page',url:'http://juneau.apache.org'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 */
	ResourceSwagger swagger() default @ResourceSwagger;

	/**
	 * Optional servlet title.
	 *
	 * <p>
	 * It is used to populate the Swagger title field.
	 * This value can be retrieved programmatically through the {@link RestRequest#getServletTitle()} method.
	 *
	 * <p>
	 * The default value pulls the label from the <code>label</code> entry in the servlet resource bundle.
	 * (e.g. <js>"title = foo"</js> or <js>"MyServlet.title = foo"</js>).
	 *
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link FileVar $F} 
	 * {@link ServletInitParamVar $I},
	 * {@link IfVar $IF}
	 * {@link LocalizationVar $L}
	 * {@link RequestAttributeVar $RA} 
	 * {@link RequestFormDataVar $RF} 
	 * {@link RequestHeaderVar $RH} 
	 * {@link RequestPathVar $RP} 
	 * {@link RequestQueryVar $RQ} 
	 * {@link RequestVar $R} 
	 * {@link SystemPropertiesVar $S}
	 * {@link SerializedRequestAttrVar $SA}
	 * {@link SwitchVar $SW}
	 * {@link UrlVar $U}
	 * {@link UrlEncodeVar $UE}
	 * {@link WidgetVar $W}
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/info/title</code>.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTitle(RestRequest)} method.
	 */
	String title() default "";

	/**
	 * Configuration property:  Use classpath resource caching. 
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestRequest#getClasspathReaderResource(String, boolean)} (and related 
	 * methods) will be cached in memory to speed subsequent lookups.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 * 
	 * <p>
	 * See {@link RestContext#REST_useClasspathResourceCaching} for more information.
	 */
	String useClasspathResourceCaching() default "";
	
	/**
	 * Use stack trace hashes.
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 * 
	 * <p>
	 * Value can contain any of the following variables:  
	 * {@link ConfigFileVar $C} 
	 * {@link CoalesceVar $CO}
	 * {@link CoalesceAndRecurseVar $CR}
	 * {@link EnvVariablesVar $E} 
	 * {@link IfVar $IF}
	 * {@link SystemPropertiesVar $S}
	 * {@link SwitchVar $SW}
	 *
	 * <p>
	 * See {@link RestContext#REST_useStackTraceHashes} for more information.
	 */
	String useStackTraceHashes() default "";
}
