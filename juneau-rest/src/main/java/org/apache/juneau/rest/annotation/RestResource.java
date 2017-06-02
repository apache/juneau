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
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.html.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as a REST resource.
 *
 * Refer to <a class='doclink' href='../package-summary.html#TOC'>org.apache.juneau.rest</a> doc for information on using this class.
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
	 * 	<li>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li>{@link RestContext#getMessages()}
	 * </ul>
	 * <p>
	 * Refer to the {@link MessageBundle} class for a description of the message key formats
	 * 	used in the properties file.
	 * <p>
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the
	 * 	resource bundle <js>"com.foo.sample.nls.Messages"</js> if the resource class
	 * 	is in <js>"com.foo.sample"</js>, or it can be an absolute path, like <js>"com.foo.sample.nls.Messages"</js>
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
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addGuards(Class...)}/{@link RestConfig#addGuards(RestGuard...)} methods.
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
	 * Default converter implementations are provided in the <a class='doclink' href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addConverters(Class...)}/{@link RestConfig#addConverters(RestConverter...)} methods.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Class-level bean filters.
	 * <p>
	 * Shortcut to add bean filters to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 * <p>
	 * If the specified class is an instance of {@link BeanFilterBuilder}, then a filter built from that builder is added.
	 * Any other classes are wrapped in a {@link InterfaceBeanFilterBuilder} to indicate that subclasses should
	 * 	be treated as the specified class type.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#addBeanFilters(Class...)} method.
	 */
	Class<?>[] beanFilters() default {};

	/**
	 * Class-level POJO swaps.
	 * <p>
	 * Shortcut to add POJO swaps to the bean contexts of the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 * <p>
	 * If the specified class is an instance of {@link PojoSwap}, then that swap is added.
	 * Any other classes are wrapped in a {@link SurrogateSwap}.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#addPojoSwaps(Class...)} method.
	 */
	Class<?>[] pojoSwaps() default {};

	/**
	 * Class-level Java method parameter resolvers.
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 * <p>
	 * For example, if you want to pass in instances of <code>MySpecialObject</code> to your Java method, define
	 * the following resolver:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyRestParam <jk>extends</jk> RestParam {
	 *
	 * 		<jc>// Must have no-arg constructor!</jc>
	 * 		<jk>public</jk> MyRestParam() {
	 * 			<jc>// First two parameters help with Swagger doc generation.</jc>
	 * 			<jk>super</jk>(<jsf>QUERY</jsf>, <js>"myparam"</js>, MySpecialObject.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// The method that creates our object.
	 * 		// In this case, we're taking in a query parameter and converting it to our object.</jc>
	 * 		<jk>public</jk> Object resolve(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MySpecialObject(req.getQuery().get(<js>"myparam"</js>));
	 * 		}
	 * 	}
	 * </p>
	 * <p>
	 * <b>Note:</b>{@link RestParam} classes must have no-arg constructors.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#addParamResolvers(Class...)} method.
	 */
	Class<? extends RestParam>[] paramResolvers() default {};

	/**
	 * Class-level properties.
	 * <p>
	 * Shortcut for specifying class-level properties on this servlet to the objects returned by the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#getBeanContext()}
	 * 	<li>{@link RestContext#getSerializers()}
	 * 	<li>{@link RestContext#getParsers()}
	 * </ul>
	 * <p>
	 * Any of the following property names can be specified:
	 * <ul>
	 * 	<li>{@link RestContext}
	 * 	<li>{@link BeanContext}
	 * 	<li>{@link SerializerContext}
	 * 	<li>{@link ParserContext}
	 * 	<li>{@link JsonSerializerContext}
	 * 	<li>{@link RdfSerializerContext}
	 * 	<li>{@link RdfParserContext}
	 * 	<li>{@link RdfCommonContext}
	 * 	<li>{@link XmlSerializerContext}
	 * 	<li>{@link XmlParserContext}
	 * </ul>
	 * <p>
	 * Property values will be converted to the appropriate type.
	 * <p>
	 * In some cases, properties can be overridden at runtime through the {@link RestResponse#setProperty(String, Object)} method
	 * 	or through a {@link Properties @Properties} annotated method parameter.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setProperty(String, Object)}/{@link RestConfig#setProperties(java.util.Map)} methods.
	 */
	Property[] properties() default {};

	/**
	 * Specifies a list of {@link Serializer} classes to add to the list of serializers available for this servlet.
	 * <p>
	 * This annotation can only be used on {@link Serializer} classes that have no-arg constructors.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addSerializers(Class...)}/{@link RestConfig#addSerializers(Serializer...)} methods.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Specifies a list of {@link Parser} classes to add to the list of parsers available for this servlet.
	 * <p>
	 * This annotation can only be used on {@link Parser} classes that have no-arg constructors.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addParsers(Class...)}/{@link RestConfig#addParsers(Parser...)} methods.
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned
	 * 	by REST methods or set via {@link RestResponse#setOutput(Object)} into appropriate
	 * 	HTTP responses.
	 * <p>
	 * See {@link ResponseHandler} for details.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addResponseHandlers(Class...)}/{@link RestConfig#addResponseHandlers(ResponseHandler...)} methods.
	 */
	Class<? extends ResponseHandler>[] responseHandlers() default {};

	/**
	 * Specifies a list of {@link Encoder} to associate with this servlet.
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 * <p>
	 * This annotation can only be used on {@link Encoder} classes that have no-arg constructors.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Servlet with automated support for GZIP compression</jc>
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addEncoders(Class...)}/{@link RestConfig#addEncoders(Encoder...)} methods.
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestResource</ja>(defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addDefaultRequestHeader(String, Object)}/{@link RestConfig#addDefaultRequestHeaders(String...)} methods.
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Add a version header attribute to all responses</jc>
	 * 	<ja>@RestResource</ja>(defaultResponseHeaders={<js>"X-Version: 1.0"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addDefaultResponseHeader(String, Object)}/{@link RestConfig#addDefaultResponseHeaders(String...)} methods.
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
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addChildResource(String, Object)}/{@link RestConfig#addChildResources(Class...)}/{@link RestConfig#addChildResources(Object...)} methods.
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
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#setPath(String)} method.
	 */
	String path() default "";

	/**
	 * Optional servlet title.
	 * <p>
	 * It is used to populate the Swagger title field and as a default value for the {@link #pageTitle()} value.
	 * This value can be retrieved programmatically through the {@link RestRequest#getServletTitle()} method.
	 * <p>
	 * The default value pulls the label from the <code>label</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"title = foo"</js> or <js>"MyServlet.title = foo"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/title</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTitle(RestRequest)} method.
	 */
	String title() default "";

	/**
	 * Optional servlet description.
	 * <p>
	 * It is used to populate the Swagger description field and as a default value for the {@link #pageText()} value.
	 * This value can be retrieved programmatically through the {@link RestRequest#getServletDescription()} method.
	 * <p>
	 * The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"description = foo"</js> or <js>"MyServlet.description = foo"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/description</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getDescription(RestRequest)} method.
	 */
	String description() default "";

	/**
	 * Optional servlet terms-of-service for this API.
	 * <p>
	 * It is used to populate the Swagger terms-of-service field.
	 * <p>
	 * The default value pulls the description from the <code>termsOfService</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"termsOfService = foo"</js> or <js>"MyServlet.termsOfService = foo"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/termsOfService</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTermsOfService(RestRequest)} method.
	 */
	String termsOfService() default "";

	/**
	 * Optional contact information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger contact field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string,
	 * 		email: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>contact</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js> or <js>"MyServlet.contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(contact=<js>"{name:'John Smith',email:'john.smith@foo.bar'}"</js>)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/contact</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getContact(RestRequest)} method.
	 */
	String contact() default "";

	/**
	 * Optional license information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger license field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		name: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>license</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js> or <js>"MyServlet.license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/license</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getLicense(RestRequest)} method.
	 */
	String license() default "";

	/**
	 * Provides the version of the application API (not to be confused with the specification version).
	 * <p>
	 * It is used to populate the Swagger version field and to display on HTML pages.
	 * <p>
	 * The default value pulls the description from the <code>version</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"version = 2.0"</js> or <js>"MyServlet.version = 2.0"</js>).
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/info/version</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getVersion(RestRequest)} method.
	 */
	String version() default "";

	/**
	 * Optional tagging information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger tags field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	[
	 * 		{
	 * 			name: string,
	 * 			description: string,
	 * 			externalDocs: {
	 * 				description: string,
	 * 				url: string
	 * 			}
	 * 		}
	 * 	]
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>tags</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"tags = [{name:'Foo',description:'Foobar'}]"</js> or <js>"MyServlet.tags = [{name:'Foo',description:'Foobar'}]"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(tags=<js>"[{name:'Foo',description:'Foobar'}]"</js>)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTags(RestRequest)} method.
	 */
	String tags() default "";

	/**
	 * Optional external documentation information for the exposed API.
	 * <p>
	 * It is used to populate the Swagger external documentation field and to display on HTML pages.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		description: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>externalDocs</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"externalDocs = {url:'http://juneau.apache.org'}"</js> or <js>"MyServlet.externalDocs = {url:'http://juneau.apache.org'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(externalDocs=<js>"{url:'http://juneau.apache.org'}"</js>)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/tags</code>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getExternalDocs(RestRequest)} method.
	 */
	String externalDocs() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 * <p>
	 * The configuration file .
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#setConfigFile(ConfigFile)} method.
	 */
	String config() default "";

	/**
	 * The stylesheet to use for HTML views.
	 * <p>
	 * The name is a path to a stylesheet located in either the classpath or working directory.
	 * The resulting stylesheet becomes available through the servlet via the URL <js>"[servletpath]/style.css"</js>.
	 * <p>
	 * The default set of styles located in the <code>org.apache.juneau.rest.styles</code> package are:
	 * <ul class='spaced-list'>
	 * 	<li><js>"styles/juneau.css"</js> - Theme based on Jazz look-and-feel.
	 * 	<li><js>"styles/devops.css"</js> - Theme based on IBM DevOps look-and-feel.
	 * </ul>
	 * <p>
	 * The classpath search starts with the child servlet class and proceeds up the class hierarchy
	 * 	chain.  Since the {@link RestServlet} class is in the <code>org.apache.juneau.rest</code> package
	 * 	and the predefined styles are in the <code>org.apache.juneau.rest.styles</code> package, the paths to
	 * 	the predefined styles are prefixed with <js>"styles/"</js>.
	 * <p>
	 * If the stylesheet cannot be found on the classpath, an attempt to look in the working directory
	 * 	for it will be made.  This allows for stylesheets to be placed on the file system in the working
	 * 	directory.
	 * <p>
	 * If the file cannot be located, the request to <js>"[servletpath]/style.css"</js> will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		stylesheet=<js>"mystyles/mycss.css"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * </p>
	 * <p>
	 * In this example, the servlet will attempt to find the <code>mycss.css</code> file in the following ordered locations:
	 * </p>
	 * <ol>
	 * 	<li><code>com.foo.mypackage.mystyles</code> package.
	 * 	<li><code>org.apache.juneau.rest.mystyles</code> package (since <code>RestServletDefault</code> is in <code>org.apache.juneau.rest</code>).
	 * 	<li><code>[working-dir]/mystyles</code> directory.
	 * </ol>
	 * <p>
	 * Multiple stylesheets can be specified as a comma-delimited list.
	 * When multiple stylesheets are specified, their contents will be concatenated and return in the order specified
	 * in the list.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#addStyleSheet(Object...)}/{@link RestConfig#addStyleSheet(Class, String)} methods.
	 */
	String stylesheet() default "";

	/**
	 * The favicon to use for HTML views.
	 * <p>
	 * The name is a path to an icon file located in either the classpath or working directory in a similar way
	 * 	to how the {@link #stylesheet()} stylesheet is resolved.
	 * The resulting favicon becomes available in the servlet via the URL <js>"[servletpath]/favicon.ico"</js>.
	 * <p>
	 * If the file cannot be located, the request to <js>"[servletpath]/favicon.ico"</js> will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		favicon=<js>"mydocs/myicon.ico"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * </p>
	 * <p>
	 * In this example, the servlet will attempt to find the <code>myicon.ico</code> file in the following ordered locations:
	 * </p>
	 * <ol>
	 * 	<li><code>com.foo.mypackage.mydocs</code> package.
	 * 	<li><code>org.apache.juneau.rest.mydocs</code> package (since <code>RestServletDefault</code> is in <code>org.apache.juneau.rest</code>).
	 * 	<li><code>[working-dir]/mydocs</code> directory.
	 * </ol>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setFavIcon(Object)}/{@link RestConfig#setFavIcon(Class, String)} methods.
	 */
	String favicon() default "";

	/**
	 * Defines paths and locations of statically served files.
	 * <p>
	 * This is a JSON map of paths to packages/directories located on either the classpath or working directory.
	 * <p>
	 * Mappings are cumulative from parent to child.  Child resources can override mappings made on parent resources.
	 * <p>
	 * If the file cannot be located, the request will return {@link HttpServletResponse#SC_NOT_FOUND}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles=<js>"{htdocs:'docs'}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * 	}
	 * </p>
	 * <p>
	 * In this example, given a GET request to <code>/myresource/htdocs/foobar.html</code>, the servlet will attempt to find the <code>foobar.html</code> file
	 * 	in the following ordered locations:
	 * </p>
	 * <ol>
	 * 	<li><code>com.foo.mypackage.docs</code> package.
	 * 	<li><code>org.apache.juneau.rest.docs</code> package (since <code>RestServletDefault</code> is in <code>org.apache.juneau.rest</code>).
	 * 	<li><code>[working-dir]/docs</code> directory.
	 * </ol>
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#addStaticFiles(Class, String)} method.
	 */
	String staticFiles() default "";

	/**
	 * Specifies the HTTP header name used to identify the client version.
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface
	 * 	changes.  Used in conjunction with {@link RestMethod#clientVersion()} annotation.
	 * <p>
	 * If not specified, uses <js>"X-Client-Version"</js>.
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestConfig#setClientVersionHeader(String)} method.
	 */
	String clientVersionHeader() default "";

	/**
	 * Specifies the resolver class to use for resolving child resources by class name.
	 * <p>
	 * The default implementation simply instantiates the class using one of the following constructors:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(RestConfig)</code>
	 * 	<li><code><jk>public</jk> T()</code>
	 * </ul>
	 * The former constructor can be used to get access to the {@link RestConfig} object to get access to the
	 * config file and initialization information or make programmatic modifications to the resource before
	 * full initialization.
	 * <p>
	 * Non-<code>RestServlet</code> classes can also add the following two methods to get access to the
	 * {@link RestConfig} and {@link RestContext} objects:
	 * <ul>
	 * 	<li><jk>public void</jk> init(RestConfig);</code>
	 * 	<li><jk>public void</jk> init(RestContext);</code>
	 * </ul>
	 * <p>
	 * Subclasses can be used to provide customized resolution of REST resource class instances.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setResourceResolver(Class)}/{@link RestConfig#setResourceResolver(RestResourceResolver)} methods.
	 */
	Class<? extends RestResourceResolver> resourceResolver() default RestResourceResolver.class;

	/**
	 * Specifies the logger class to use for logging.
	 * <p>
	 * The default logger performs basic error logging to the Java logger.
	 * Subclasses can be used to customize logging behavior on the resource.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setLogger(Class)}/{@link RestConfig#setLogger(RestLogger)} methods.
	 */
	Class<? extends RestLogger> logger() default RestLogger.Normal.class;

	/**
	 * Specifies the REST call handler class.
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * Subclasses can be used to customize how these HTTP calls are handled.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setCallHandler(Class)}/{@link RestConfig#setCallHandler(RestCallHandler)} methods.
	 */
	Class<? extends RestCallHandler> callHandler() default RestCallHandler.class;

	/**
	 * Specifies the class used to retrieve title/description/swagger information about a resource.
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setInfoProvider(Class)}/{@link RestConfig#setInfoProvider(RestInfoProvider)} methods.
	 */
	Class<? extends RestInfoProvider> infoProvider() default RestInfoProvider.class;

	/**
	 * Specifies the page title to use on the HTML view of all pages produced by this resource.
	 * <p>
	 * This annotation has no effect on any serializers other than {@link HtmlDocSerializer} and is a shorthand method
	 * for setting the {@link HtmlDocSerializerContext#HTMLDOC_title} property:
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<jsf>HTMLDOC_title</jsf>, value=<js>"My Resource Page"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 * <p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageTitle</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageTitle</code> resource bundle value.
	 * 	<li><code><ja>@RestResource</ja>(title)</code> annotation.
	 * 	<li><code>{servletClass}.title</code> resource bundle value.
	 * 	<li><code>info/title</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setPageTitle(String)}/{@link RestResponse#setPageTitle(Object)} methods.
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(title)</code> annotation to specify the page title.
	 * 		However, this annotation is provided in cases where you want the page title to be different that the one
	 * 		shown in the swagger document.
	 * </ul>
	 */
	String pageTitle() default "";

	/**
	 * Specifies the page text to use on the HTML view of all pages produced by this resource.
	 * <p>
	 * The page text is portion of the page immediately under the title and above the links.
	 * <p>
	 * This annotation has no effect on any serializers other than {@link HtmlDocSerializer} and is a shorthand method
	 * for setting the {@link HtmlDocSerializerContext#HTMLDOC_text} property:
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<jsf>HTMLDOC_text</jsf>, value=<js>"This is my awesome resource page"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageText</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageText</code> resource bundle value.
	 * 	<li><code><ja>@RestMethod</ja>(summary)</code> annotation.
	 * 	<li><code>{servletClass}.{methodName}.summary</code> resource bundle value.
	 * 	<li><code>summary</code> entry in swagger file for method.
	 * 	<li><code>{servletClass}.description</code> resource bundle value.
	 * 	<li><code>info/description</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setPageText(String)}/{@link RestResponse#setPageText(Object)} methods.
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(description)</code> or <code>@RestMethod(summary)</code> annotations to specify the page text.
	 * 		However, this annotation is provided in cases where you want the text to be different that the values shown in the swagger document.
	 * </ul>
	 */
	String pageText() default "";

	/**
	 * Specifies the page hyperlinks to use on the HTML view of all pages produced by this resource.
	 * <p>
	 * The page links is positioned immediately under the title and text.
	 * <p>
	 * This annotation has no effect on any serializers other than {@link HtmlDocSerializer} and is a shorthand method
	 * for setting the {@link HtmlDocSerializerContext#HTMLDOC_text} property:
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<jsf>HTMLDOC_links</jsf>, value=<js>"{up:'request:/..',options:'servlet:/?method=OPTIONS'}"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 * <p>
	 * The format of this value is a lax-JSON string of key/value pairs where the keys are the link text and the values are relative (to the servlet) or
	 * absolute URLs.
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageLinks</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageLinks</code> resource bundle value.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * This field can also use URIs of any support type in {@link UriResolver}.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setPageLinks(String)}/{@link RestResponse#setPageLinks(Object)} methods.
	 */
	String pageLinks() default "";

	/**
	 * Specifies the serializer listener class to use for listening for non-fatal errors.
	 */
	Class<? extends SerializerListener> serializerListener() default SerializerListener.class;

	/**
	 * Specifies the parser listener class to use for listening for non-fatal errors.
	 */
	Class<? extends ParserListener> parserListener() default ParserListener.class;
}
