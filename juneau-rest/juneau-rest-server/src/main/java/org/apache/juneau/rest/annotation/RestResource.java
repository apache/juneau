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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.response.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
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
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * This annotation is used to provide localized messages for the following methods:
	 * <ul>
	 * 	<li>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li>{@link RestContext#getMessages()}
	 * </ul>
	 *
	 * <p>
	 * Refer to the {@link MessageBundle} class for a description of the message key formats used in the properties file.
	 *
	 * <p>
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
	 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
	 * absolute path, like <js>"com.foo.sample.nls.Messages"</js>
	 */
	String messages() default "";

	/**
	 * Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_guards}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#guards()}
	 * 			<li>{@link RestMethod#guards()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}
	 * 			<li>{@link RestContextBuilder#guards(RestGuard...)}
	 * 		</ul>
	 * 	<li>{@link RestGuard} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * Default converter implementations are provided in the <a class='doclink'
	 * href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_converters}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#converters()}
	 * 			<li>{@link RestMethod#converters()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}
	 * 			<li>{@link RestContextBuilder#converters(RestConverter...)}
	 * 		</ul>
	 * 	<li>{@link RestConverter} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	Class<? extends RestConverter>[] converters() default {};

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
	 * If the specified class is an instance of {@link BeanFilterBuilder}, then a filter built from that builder is added.
	 * Any other classes are wrapped in a {@link InterfaceBeanFilterBuilder} to indicate that subclasses should be
	 * treated as the specified class type.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link BeanContext#BEAN_beanFilters}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#beanFilters()}
	 * 			<li>{@link RestMethod#beanFilters()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#beanFilters(Class...)}
	 * 			<li>{@link RestContextBuilder#beanFilters(Collection)}
	 * 			<li>{@link RestContextBuilder#beanFilters(boolean, Class...)}
	 * 			<li>{@link RestContextBuilder#beanFilters(boolean, Collection)}
	 * 			<li>{@link RestContextBuilder#beanFiltersRemove(Class...)}
	 * 			<li>{@link RestContextBuilder#beanFiltersRemove(Collection)}
	 * 		</ul>
	 *	</ul>
	 */
	Class<?>[] beanFilters() default {};

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
	 * If the specified class is an instance of {@link PojoSwap}, then that swap is added.
	 * Any other classes are wrapped in a {@link SurrogateSwap}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link BeanContext#BEAN_pojoSwaps}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#pojoSwaps()}
	 * 			<li>{@link RestMethod#pojoSwaps()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#pojoSwaps(Class...)}
	 * 			<li>{@link RestContextBuilder#pojoSwaps(Collection)}
	 * 			<li>{@link RestContextBuilder#pojoSwaps(boolean, Class...)}
	 * 			<li>{@link RestContextBuilder#pojoSwaps(boolean, Collection)}
	 * 			<li>{@link RestContextBuilder#pojoSwapsRemove(Class...)}
	 * 			<li>{@link RestContextBuilder#pojoSwapsRemove(Collection)}
	 * 		</ul>
	 *	</ul>
	 */
	Class<?>[] pojoSwaps() default {};

	/**
	 * Java method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
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
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_paramResolvers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#paramResolvers()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#paramResolvers(Class...)}
	 * 		</ul>
	 * 	<li>{@link RestParam} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	Class<? extends RestParam>[] paramResolvers() default {};

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
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#setProperty(String, Object)}/
	 * {@link RestContextBuilder#setProperties(java.util.Map)} methods.
	 */
	Property[] properties() default {};

	/**
	 * Shortcut for setting {@link #properties()} of simple boolean types.
	 *
	 * <p>
	 * Setting a flag is equivalent to setting the same property to <js>"true"</js>.
	 */
	String[] flags() default {};

	/**
	 * Specifies a list of {@link Serializer} classes to add to the list of serializers available for this servlet.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#serializers(Class...)}/
	 * {@link RestContextBuilder#serializers(Serializer...)} methods.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Specifies a list of {@link Parser} classes to add to the list of parsers available for this servlet.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#parsers(Class...)}/
	 * {@link RestContextBuilder#parsers(Parser...)} methods.
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#partSerializer(Class)}/
	 * {@link RestContextBuilder#partSerializer(HttpPartSerializer)} methods.
	 */
	Class<? extends HttpPartSerializer> partSerializer() default SimpleUonPartSerializer.class;
	
	/**
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#partParser(Class)}/
	 * {@link RestContextBuilder#partParser(HttpPartParser)} methods.
	 */
	Class<? extends HttpPartParser> partParser() default UonPartParser.class;

	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * 
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedAcceptTypes()} and the supported accept
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedAcceptTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedAcceptTypes()}
	 * 			<li>{@link RestMethod#supportedAcceptTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 */
	String[] supportedAcceptTypes() default {};
	
	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedContentTypes()} and the supported content
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedContentTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedContentTypes()}
	 * 			<li>{@link RestMethod#supportedContentTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 */
	String[] supportedContentTypes() default {};
	
	/**
	 * Response handlers.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * By default, the following response handlers are provided out-of-the-box:
	 * <ul>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_responseHandlers}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#responseHandlers()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseHandlers(Class...)}
	 * 			<li>{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 		</ul>
	 * 	<li>{@link ResponseHandler} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	Class<? extends ResponseHandler>[] responseHandlers() default {};

	/**
	 * Specifies a list of {@link Encoder} to associate with this servlet.
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
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
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#encoders(Class...)}/
	 * {@link RestContextBuilder#encoders(Encoder...)} methods.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Default request headers.
	 * 
	 * <p>
	 * Specifies default values for request headers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestResource</ja>(defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultRequestHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultRequestHeaders()}
	 * 			<li>{@link RestMethod#defaultRequestHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 		</ul>
	 * 	<li>Strings are of the format <js>"Header-Name: header-value"</js>.
	 * 	<li>You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li>Key and value is trimmed of whitespace.
	 * 	<li>Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 * 	<li>Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 *	</ul>
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Add a version header attribute to all responses</jc>
	 * 	<ja>@RestResource</ja>(defaultResponseHeaders={<js>"X-Version: 1.0"</js>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultResponseHeaders}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		</ul>
	 * 	<li>Strings are of the format <js>"Header-Name: header-value"</js>.
	 * 	<li>You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * 	<li>Key and value is trimmed of whitespace.
	 * 	<li>Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 * 	<li>This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of 
	 * 		the Java methods.
	 * 	<li>The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 *	</ul>
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Defines children of this resource.
	 *
	 * <p>
	 * A REST child resource is simply another servlet that is initialized as part of the parent resource and has a
	 * servlet path directly under the parent servlet path.
	 * The main advantage to defining servlets as REST children is that you do not need to define them in the
	 * <code>web.xml</code> file of the web application.
	 * This can cut down on the number of entries that show up in the <code>web.xml</code> file if you are defining
	 * large numbers of servlets.
	 *
	 * <p>
	 * Child resources must specify a value for {@link #path()} that identifies the subpath of the child resource
	 * relative to the parent path.
	 *
	 * <p>
	 * It should be noted that servlets can be nested arbitrarily deep using this technique (i.e. children can also have
	 * children).
	 *
	 * <dl>
	 * 	<dt>Servlet initialization:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			A child resource will be initialized immediately after the parent servlet is initialized.
	 * 			The child resource receives the same servlet config as the parent resource.
	 * 			This allows configuration information such as servlet initialization parameters to filter to child
	 * 			resources.
	 * 		</p>
	 * 	</dd>
	 * 	<dt>Runtime behavior:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			As a rule, methods defined on the <code>HttpServletRequest</code> object will behave as if the child
	 * 			servlet were deployed as a top-level resource under the child's servlet path.
	 * 			For example, the <code>getServletPath()</code> and <code>getPathInfo()</code> methods on the
	 * 			<code>HttpServletRequest</code> object will behave as if the child resource were deployed using the
	 * 			child's servlet path.
	 * 			Therefore, the runtime behavior should be equivalent to deploying the child servlet in the
	 * 			<code>web.xml</code> file of the web application.
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 *
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestContextBuilder#childResource(String, Object)}/
	 * {@link RestContextBuilder#childResources(Class...)}/{@link RestContextBuilder#childResources(Object...)} methods.
	 */
	Class<?>[] children() default {};

	/**
	 * Resource path.   
	 *
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_path}
	 * 	<li>Annotation:  {@link RestResource#path()} 
	 * 	<li>Method: {@link RestContextBuilder#path(String)} 
	 * 	<li>This annotation is ignored on top-level servlets (i.e. servlets defined in <code>web.xml</code> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>Typically, this setting is only applicable to resources defined as children through the 
	 * 		{@link RestResource#children()} annotation.
	 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 *	</ul>
	 */
	String path() default "";

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
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getSiteName(RestRequest)} method.
	 */
	String siteName() default "";

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
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/info/title</code>.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getTitle(RestRequest)} method.
	 */
	String title() default "";

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
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/info/description</code>.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestInfoProvider#getDescription(RestRequest)} method.
	 */
	String description() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 *
	 * <p>
	 * The configuration file .
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * The programmatic equivalent to this annotation is the {@link RestContextBuilder#configFile(ConfigFile)} method.
	 */
	String config() default "";

	/**
	 * Static file mappings. 
	 *
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 * 
	 * <p>
	 * Static files are found using the registered  {@link ClasspathResourceFinder} for locating files on the classpath 
	 * (or other location).
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 * 
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles=<js>"htdocs:docs"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {...}
	 * </p>
	 * 
	 * <p>
	 * In the example above, given a GET request to <l>/myresource/htdocs/foobar.html</l>, the servlet will attempt to find 
	 * the <l>foobar.html</l> file in the following ordered locations:
	 * <ol>
	 * 	<li><l>com.foo.mypackage.docs</l> package.
	 * 	<li><l>org.apache.juneau.rest.docs</l> package (since <l>RestServletDefault</l> is in <l>org.apache.juneau.rest</l>).
	 * 	<li><l>[working-dir]/docs</l> directory.
	 * </ol>
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFiles}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFiles()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFiles(String)},
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(String,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String,String)} 
	 * 			<li>{@link RestContextBuilder#staticFiles(StaticFileMapping...)} 
	 * 		</ul>
	 * 	<li>Mappings are cumulative from parent to child.  
	 * 	<li>Child resources can override mappings made on parent resources.
	 * 	<li>The resource finder is configured via the {@link RestContext#REST_classpathResourceFinder} setting, and can be
	 * 		overridden to provide customized handling of resource retrieval.
	 * 	<li>The {@link RestContext#REST_useClasspathResourceCaching} setting can be used to cache static files in memory
	 * 		to improve performance.
	 * </ul>
	 */
	String[] staticFiles() default {};
	
	/**
	 * Static file response headers. 
	 *
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFileResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFileResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(boolean,Map)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 		</ul>
	 * 	<li>The default values is <code>{<js>'Cache-Control'</js>: <js>'max-age=86400, public</js>}</code>.
	 * </ul>
	 */
	String[] staticFileResponseHeaders() default {};
	
	/**
	 * Classpath resource finder. 
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_classpathResourceFinder}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#classpathResourceFinder()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(Class)}
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(ClasspathResourceFinder)}
	 * 		</ul>
	 * 	<li>
	 * 		The default value is {@link ClasspathResourceFinderBasic} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * 		<br>The {@link ClasspathResourceFinderRecursive} is another option that also recursively searches for resources
	 * 		up the parent class hierarchy.
	 * 		<br>Each of these classes can be extended to provide customized handling of resource retrieval.
	 * </ul>
	 */
	Class<? extends ClasspathResourceFinder> classpathResourceFinder() default ClasspathResourceFinder.Null.class;

	/**
	 * <b>Configuration property:</b>  Use classpath resource caching. 
	 *
	 * <p>
	 * When enabled, resources retrieved via {@link RestRequest#getClasspathReaderResource(String, boolean)} (and related 
	 * methods) will be cached in memory to speed subsequent lookups.
	 * 
	 * <h6 class='topic'>Notes:</h6>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useClasspathResourceCaching}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#useClasspathResourceCaching()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useClasspathResourceCaching(boolean)}
	 * 		</ul>
	 * </ul>
	 */
	String useClasspathResourceCaching() default "";
	
	/**
	 * Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestMethod#clientVersion()} annotation.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_clientVersionHeader}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#clientVersionHeader()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#clientVersionHeader(String)}
	 * 		</ul>
	 * 	<li>The default value is <js>"X-Client-Version"</js>.
	 *	</ul>
	 */
	String clientVersionHeader() default "";

	/**
	 * REST resource resolver.
	 * 
	 * <p>
	 * The resolver used for resolving child resources.
	 * 
	 * <p>
	 * Can be used to provide customized resolution of REST resource class instances (e.g. resources retrieve from Spring).
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_resourceResolver}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#resourceResolver()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#resourceResolver(Class)}
	 * 			<li>{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
	 * 		</ul>
	 * 	<li>Unless overridden, resource resolvers are inherited from parent resources.
	 *	</ul>
	 */
	Class<? extends RestResourceResolver> resourceResolver() default RestResourceResolverSimple.class;

	/**
	 * REST logger.
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_logger}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#logger()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#logger(Class)}
	 * 			<li>{@link RestContextBuilder#logger(RestLogger)} 
	 * 		</ul>
	 * 	<li>The {@link org.apache.juneau.rest.RestLogger.Normal} logger can be used to provide basic error logging to the Java logger.
	 *	</ul>
	 */
	Class<? extends RestLogger> logger() default RestLogger.Normal.class;

	/**
	 * REST call handler.
	 *
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_callHandler}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#callHandler()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callHandler(Class)}
	 * 			<li>{@link RestContextBuilder#callHandler(RestCallHandler)} 
	 * 		</ul>
	 *	</ul>
	 */
	Class<? extends RestCallHandler> callHandler() default RestCallHandler.class;

	/**
	 * <b>Configuration property:</b>  REST info provider. 
	 * 
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_infoProvider}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#infoProvider()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#infoProvider(Class)}
	 * 			<li>{@link RestContextBuilder#infoProvider(RestInfoProvider)} 
	 * 		</ul>
	 *	</ul>
	 */
	Class<? extends RestInfoProvider> infoProvider() default RestInfoProvider.class;

	/**
	 * Serializer listener.
	 * 
	 * <p>
	 * Specifies the serializer listener class to use for listening to non-fatal serialization errors.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link Serializer#SERIALIZER_listener}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#serializerListener()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializerListener(Class)} 
	 * 		</ul>
	 *	</ul>
	 */
	Class<? extends SerializerListener> serializerListener() default SerializerListener.Null.class;

	/**
	 * Parser listener.
	 * 
	 * <p>
	 * Specifies the parser listener class to use for listening to non-fatal parsing errors.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link Parser#PARSER_listener}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#parserListener()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parserListener(Class)} 
	 * 		</ul>
	 *	</ul>
	 */
	Class<? extends ParserListener> parserListener() default ParserListener.Null.class;

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
	 * Resource context path. 
	 * 
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <p>
	 * This setting is useful if you want to use <js>"context:/child/path"</js> URLs in child resource POJOs but
	 * the context path is not actually specified on the servlet container.
	 * The net effect is that the {@link RestRequest#getContextPath()} and {@link RestRequest#getServletPath()} methods
	 * will return this value instead of the actual context path of the web app.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_contextPath}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#contextPath()} 
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#contextPath(String)} 
	 * 		</ul>
	 *	</ul>
	 */
	String contextPath() default "";

	/**
	 * Allow header URL parameters.
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowHeaderParams}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowHeaderParams()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowHeaderParams(boolean)}
	 * 		</ul>
	 * 	<li>Boolean value.
	 * 	<li>Can contain variables.
	 *		<li>Parameter names are case-insensitive.
	 * 	<li>Useful for debugging REST interface using only a browser.
	 *	</ul>
	 */
	String allowHeaderParams() default "";

	/**
	 * Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowedMethodParams}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowedMethodParams()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowedMethodParams(String...)}
	 * 		</ul>
	 * 	<li>Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Can contain variables.
	 * 	<li>Use "*" to represent all methods.
	 *	</ul>
	 *
	 * <p>
	 * Note that per the <a class="doclink"
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 */
	String allowedMethodParams() default "";

	/**
	 * Allow body URL parameter.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.allowBodyParam.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowBodyParam}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#allowBodyParam()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowBodyParam(boolean)}
	 * 		</ul>
	 * 	<li>Boolean value.
	 * 	<li>Can contain variables.
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging PUT and POST methods using only a browser.
	 *	</ul>
	 */
	String allowBodyParam() default "";

	/**
	 * Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_renderResponseStackTraces}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 		</ul>
	 * 	<li>Boolean value.
	 * 	<li>Can contain variables.
	 * 	<li>Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 *	</ul>
	 */
	String renderResponseStackTraces() default "";

	/**
	 * Use stack trace hashes.
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useStackTraceHashes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#useStackTraceHashes()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useStackTraceHashes(boolean)}
	 * 		</ul>
	 * 	<li>Boolean value.
	 * 	<li>Can contain variables.
	 *	</ul>
	 */
	String useStackTraceHashes() default "";

	/**
	 * Default character encoding.
	 * 
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultCharset}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultCharset()}
	 * 			<li>{@link RestMethod#defaultCharset()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultCharset(String)}
	 * 		</ul>
	 * 	<li>String value.
	 * 	<li>Can contain variables.
	 *	</ul>
	 */
	String defaultCharset() default "";

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		maxInput=<js>"100M"</js>
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_maxInput}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#maxInput()}
	 * 			<li>{@link RestMethod#maxInput()}
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#maxInput(String)}
	 * 		</ul>
	 * 	<li>String value that gets resolved to a <jk>long</jk>.
	 * 	<li>Can contain variables.
	 * 	<li>Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:  
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>A value of <js>"-1"</js> can be used to represent no limit.
	 *	</ul>
	 */
	String maxInput() default "";
}
