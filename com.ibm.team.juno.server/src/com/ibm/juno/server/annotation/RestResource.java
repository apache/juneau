/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.servlet.http.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.encoders.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.server.*;

/**
 * Optionally used to associate metadata on an instance of {@link RestServlet}.
 * <p>
 *		Refer to {@link com.ibm.juno.server} doc for information on using this class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface RestResource {

	/**
	 * Identifies the location of the resource bundle for this class.
	 * <p>
	 * This annotation is used to provide localized messages for the following methods:
	 * <ul>
	 * 	<li>{@link RestServlet#getMessage(java.util.Locale, String, Object...)}
	 * 	<li>{@link RestServlet#getMethodDescriptions(RestRequest)}
	 * 	<li>{@link RestServlet#getLabel(RestRequest)}
	 * 	<li>{@link RestServlet#getDescription(RestRequest)}
	 * </ul>
	 * <p>
	 * Refer to the {@link MessageBundle} class for a description of the message key formats
	 * 	used in the properties file.
	 * <p>
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the
	 * 	resource bundle <js>"com.ibm.sample.nls.Messages"</js> if the resource class
	 * 	is in <js>"com.ibm.sample"</js>, or it can be an absolute path, like <js>"com.ibm.sample.nls.Messages"</js>
	 */
	String messages() default "";

	/**
	 * Class-level guards.
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined
	 * 	in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request,
	 * 	but it can also be used for other purposes like pre-call validation of a request.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Class-level converters.
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same
	 * 	order specified in the annotation.
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 * <p>
	 * Default converter implementations are provided in the {@link com.ibm.juno.server.converters} package.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Class-level POJO filters.
	 * <p>
	 * Shortcut to add POJO filters to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestServlet#getBeanContext()}
	 * 	<li>{@link RestServlet#getSerializers()}
	 * 	<li>{@link RestServlet#getParsers()}
	 * </ul>
	 * <p>
	 * If the specified class is an instance of {@link Filter}, then that filter is added.
	 * Any other classes are wrapped in a {@link BeanFilter} to indicate that subclasses should
	 * 	be treated as the specified class type.
	 */
	Class<?>[] filters() default {};

	/**
	 * Class-level properties.
	 * <p>
	 * 	Shortcut for specifying class-level properties on this servlet to the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestServlet#getBeanContext()}
	 * 	<li>{@link RestServlet#getSerializers()}
	 * 	<li>{@link RestServlet#getParsers()}
	 * </ul>
	 * <p>
	 * 	Any of the following property names can be specified:
	 * <ul>
	 * 	<li>{@link RestServletProperties}
	 * 	<li>{@link BeanContextProperties}
	 * 	<li>{@link SerializerProperties}
	 * 	<li>{@link ParserProperties}
	 * 	<li>{@link JsonSerializerProperties}
	 * 	<li>{@link RdfSerializerProperties}
	 * 	<li>{@link RdfParserProperties}
	 * 	<li>{@link RdfProperties}
	 * 	<li>{@link XmlSerializerProperties}
	 * 	<li>{@link XmlParserProperties}
	 * </ul>
	 * <p>
	 * Property values will be converted to the appropriate type.
	 * <p>
	 * In some cases, properties can be overridden at runtime through the {@link RestResponse#setProperty(String, Object)} method
	 * 	or through a {@link Properties @Properties} annotated method parameter.
	 */
	Property[] properties() default {};

	/**
	 * Specifies a list of {@link Serializer} classes to add to the list of serializers available for this servlet.
	 * <p>
	 * This annotation can only be used on {@link Serializer} classes that have no-arg constructors.
	 */
	Class<? extends Serializer<?>>[] serializers() default {};

	/**
	 * Specifies a list of {@link Parser} classes to add to the list of parsers available for this servlet.
	 * <p>
	 * This annotation can only be used on {@link Parser} classes that have no-arg constructors.
	 */
	Class<? extends Parser<?>>[] parsers() default {};

	/**
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned
	 * 	by REST methods or set via {@link RestResponse#setOutput(Object)} into appropriate
	 * 	HTTP responses.
	 * See {@link ResponseHandler} for details.
	 */
	Class<? extends ResponseHandler>[] responseHandlers() default {};

	/**
	 * Specifies a list of {@link Encoder} to associate with this servlet.
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 * <p>
	 * This annotation can only be used on {@link Encoder} classes that have no-arg constructors.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jc>// Servlet with automated support for GZIP compression</jc>
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Specifies default values for request headers.
	 * <p>
	 * Strings are of the format <js>"Header-Name: header-value"</js>.
	 * <p>
	 * Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * <p>
	 * The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not specified
	 * 	so that a particular default {@link Serializer} is picked.
	 * <p>
	 * Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestResource</ja>(defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Specifies default values for response headers.
	 * <p>
	 * Strings are of the format <js>"Header-Name: header-value"</js>.
	 * <p>
	 * This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of the Java methods.
	 * <p>
	 * The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 * <p>
	 * Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jc>// Add a version header attribute to all responses</jc>
	 * 	<ja>@RestResource</ja>(defaultResponseHeaders={<js>"X-Version: 1.0"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Defines children of this resource.
	 * <p>
	 * A REST child resource is simply another servlet that is initialized as part of the parent
	 * 	resource and has a servlet path directly under the parent servlet path.
	 * The main advantage to defining servlets as REST children is that you do not need
	 * 	to define them in the <code>web.xml</code> file of the web application.
	 * This can cut down on the number of entries that show up in the <code>web.xml</code> file
	 * 	if you are defining large numbers of servlets.
	 * <p>
	 * Child resources must specify a value for {@link #path()} that identifies the subpath of the
	 * 	child resource relative to the parent path.
	 * <p>
	 * It should be noted that servlets can be nested arbitrarily deep using this technique (i.e. children can also have children).
	 *
	 * <dl>
	 * 	<dt>Servlet initialization:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			A child resource will be initialized immediately after the parent servlet is initialized.  The child resource
	 * 			receives the same servlet config as the parent resource.  This allows configuration information such as
	 * 			servlet initialization parameters to filter to child resources.
	 * 		</p>
	 * 	</dd>
	 * 	<dt>Runtime behavior:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			As a rule, methods defined on the <code>HttpServletRequest</code> object will behave as if
	 * 			the child servlet were deployed as a top-level resource under the child's servlet path.
	 * 			For example, the <code>getServletPath()</code> and <code>getPathInfo()</code> methods on the
	 * 			<code>HttpServletRequest</code> object will behave as if the child resource were deployed
	 * 			using the child's servlet path.
	 * 			Therefore, the runtime behavior should be equivalent to deploying the child servlet in
	 * 			the <code>web.xml</code> file of the web application.
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 */
	Class<?>[] children() default {};

	/**
	 * Identifies the URL subpath relative to the parent resource.
	 * <p>
	 * Typically, this annotation is only applicable to resources defined as children through the {@link #children()}
	 * 	annotation.  However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 * <p>
	 * This annotation is ignored on top-level servlets (i.e. servlets defined in <code>web.xml</code> files).
	 * Therefore, implementers can optionally specify a path value for documentation purposes.
	 */
	String path() default "";

	/**
	 * Optional servlet label.
	 * <p>
	 * 	The default value pulls the label from the <code>label</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"label = foo"</js> or <js>"MyServlet.label = foo"</js>).
	 * <p>
	 * 	This field can contain variables (e.g. "$L{my.localized.variable}").
	 * 	See {@link RestServlet#createRequestVarResolver(RestRequest)}.
	 */
	String label() default "";

	/**
	 * Optional servlet description.
	 * <p>
	 * 	The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"description = foo"</js> or <js>"MyServlet.description = foo"</js>).
	 * <p>
	 * 	This field can contain variables (e.g. "$L{my.localized.variable}").
	 * 	See {@link RestServlet#createRequestVarResolver(RestRequest)}.
	 */
	String description() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 * <p>
	 * 	The configuration file .
	 * <p>
	 * 	This field can contain variables (e.g. "$L{my.localized.variable}").
	 * 	See {@link RestServlet#createRequestVarResolver(RestRequest)}.
	 */
	String config() default "";

	/**
	 * The stylesheet to use for HTML views.
	 * <p>
	 * 	The name is a path to a stylesheet located in either the classpath or working directory.
	 * 	The resulting stylesheet becomes available through the servlet via the URL <js>"[servletpath]/style.css"</js>.
	 * <p>
	 * 	The default set of styles located in the <code>com.ibm.juno.server.styles</code> package are:
	 * <ul>
	 * 	<li><js>"styles/juno.css"</js> - Theme based on Jazz look-and-feel.
	 * 	<li><js>"styles/devops.css"</js> - Theme based on IBM DevOps look-and-feel.
	 * </ul>
	 * <p>
	 * 	The classpath search starts with the child servlet class and proceeds up the class hierarchy
	 * 	chain.  Since the {@link RestServlet} class is in the <code>com.ibm.juno.server</code> package
	 * 	and the predefined styles are in the <code>com.ibm.juno.server.styles</code> package, the paths to
	 * 	the predefined styles are prefixed with <js>"styles/"</js>.
	 * <p>
	 * 	If the stylesheet cannot be found on the classpath, an attempt to look in the working directory
	 * 	for it will be made.  This allows for stylesheets to be placed on the file system in the working
	 * 	directory.
	 * <p>
	 * 	If the file cannot be located, the request to <js>"[servletpath]/style.css"</js> will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>package</jk> com.ibm.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		stylesheet=<js>"mystyles/mycss.css"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			In this example, the servlet will attempt to find the <code>mycss.css</code> file in the following ordered locations:
	 * 		</p>
	 * 		<ol>
	 * 			<li><code>com.ibm.mypackage.mystyles</code> package.
	 * 			<li><code>com.ibm.juno.server.mystyles</code> package (since <code>RestServletDefault</code> is in <code>com.ibm.juno.server</code>).
	 * 			<li><code>[working-dir]/mystyles</code> directory.
	 * 		</ol>
	 * 	</dd>
	 * </dl>
	 */
	String stylesheet() default "";

	/**
	 * The favicon to use for HTML views.
	 * <p>
	 * 	The name is a path to an icon file located in either the classpath or working directory in a similar way
	 * 	to how the {@link #stylesheet()} stylesheet is resolved.
	 * 	The resulting favicon becomes available in the servlet via the URL <js>"[servletpath]/favicon.ico"</js>.
	 * <p>
	 * 	If the file cannot be located, the request to <js>"[servletpath]/favicon.ico"</js> will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>package</jk> com.ibm.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		favicon=<js>"mydocs/myicon.ico"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			In this example, the servlet will attempt to find the <code>myicon.ico</code> file in the following ordered locations:
	 * 		</p>
	 * 		<ol>
	 * 			<li><code>com.ibm.mypackage.mydocs</code> package.
	 * 			<li><code>com.ibm.juno.server.mydocs</code> package (since <code>RestServletDefault</code> is in <code>com.ibm.juno.server</code>).
	 * 			<li><code>[working-dir]/mydocs</code> directory.
	 * 		</ol>
	 * 	</dd>
	 * </dl>
	 */
	String favicon() default "";

	/**
	 * Defines paths and locations of statically served files.
	 * <p>
	 * 	This is a JSON map of paths to packages/directories located on either the classpath or working directory.
	 * <p>
	 * 	Mappings are cumulative from parent to child.  Child resources can override mappings made on parent resources.
	 * <p>
	 * 	If the file cannot be located, the request will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 * <p>
	 * 	The media type on the response is determined by the {@link RestServlet#getMimetypesFileTypeMap()} method.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jk>package</jk> com.ibm.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles=<js>"{htdocs:'docs'}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * 		</p>
	 * 		<p>
	 * 			In this example, given a GET request to <code>/myresource/htdocs/foobar.html</code>, the servlet will attempt to find the <code>foobar.html</code> file
	 * 			in the following ordered locations:
	 * 		</p>
	 * 		<ol>
	 * 			<li><code>com.ibm.mypackage.docs</code> package.
	 * 			<li><code>com.ibm.juno.server.docs</code> package (since <code>RestServletDefault</code> is in <code>com.ibm.juno.server</code>).
	 * 			<li><code>[working-dir]/docs</code> directory.
	 * 		</ol>
	 * 	</dd>
	 * </dl>
	 */
	String staticFiles() default "";
}
