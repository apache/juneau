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
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;

/**
 * Identifies a REST Java method on a {@link RestServlet} implementation class.
 * <p>
 * Refer to <a class='doclink' href='../package-summary.html#TOC'>org.apache.juneau.rest</a> doc for information on using this class.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface RestMethod {

	/**
	 * REST method name.
	 *
	 * <p>
	 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 *
	 * <p>
	 * Method names are case-insensitive (always folded to upper-case).
	 *
	 * <p>
	 * Note that you can use {@link org.apache.juneau.http.HttpMethodName} for constant values.
	 *
	 * <p>
	 * Besides the standard HTTP method names, the following can also be specified:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"*"</js>
	 * 		- Denotes any method.
	 * 		<br>Use this if you want to capture any HTTP methods in a single Java method.
	 * 		<br>The {@link Method @Method} annotation and/or {@link RestRequest#getMethod()} method can be used to
	 * 		distinguish the actual HTTP method name.
	 * 	<li>
	 * 		<js>""</js>
	 * 		- Auto-detect.
	 * 		<br>The method name is determined based on the Java method name.
	 * 		<br>For example, if the method is <code>doPost(...)</code>, then the method name is automatically detected
	 * 		as <js>"POST"</js>.
	 * 		<br>Otherwise, defaults to <js>"GET"</js>.
	 * 	<li>
	 * 		<js>"PROXY"</js>
	 * 		- Remote-proxy interface.
	 * 		<br>This denotes a Java method that returns an object (usually an interface, often annotated with the
	 * 		{@link Remoteable @Remoteable} annotation) to be used as a remote proxy using
	 * 		<code>RestClient.getRemoteableProxy(Class&lt;T&gt; interfaceClass, String url)</code>.
	 * 		<br>This allows you to construct client-side interface proxies using REST as a transport medium.
	 * 		<br>Conceptually, this is simply a fancy <code>POST</code> against the url <js>"/{path}/{javaMethodName}"</js>
	 * 		where the arguments are marshalled from the client to the server as an HTTP body containing an array of
	 * 		objects, passed to the method as arguments, and then the resulting object is marshalled back to the client.
	 * 	<li>
	 * 		Anything else
	 * 		- Overloaded non-HTTP-standard names that are passed in through a <code>&amp;method=methodName</code> URL
	 * 		parameter.
	 * </ul>
	 */
	String name() default "";

	/**
	 * Optional path pattern for the specified method.
	 *
	 * <p>
	 * Appending <js>"/*"</js> to the end of the path pattern will make it match any remainder too.
	 * <br>Not appending <js>"/*"</js> to the end of the pattern will cause a 404 (Not found) error to occur if the exact
	 * pattern is not found.
	 *
	 * <p>
	 * The path can contain variables that get resolved to {@link Path @Path} parameters:
	 * <p class='bcode'>
	 * 	<jc>// Example 1</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
	 *
	 * 	<jc>// Example 2</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{0}/{1}/{2}/*"</js>)
	 * </p>
	 *
	 * <p>
	 * Refer to {@link Path @Path} on how path variables get resolved.
	 */
	String path() default "/*";

	/**
	 * URL path pattern priority.
	 *
	 * <p>
	 * To force path patterns to be checked before other path patterns, use a higher priority number.
	 *
	 * <p>
	 * By default, it's <code>0</code>, which means it will use an internal heuristic to determine a best match.
	 */
	int priority() default 0;

	/**
	 * Method-level guards.
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
	 * Method-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource method.
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
	 * Method matchers.
	 *
	 * <p>
	 * Associates one more more {@link RestMatcher RestMatchers} with this method.
	 *
	 * <p>
	 * Matchers are used to allow multiple Java methods to handle requests assigned to the same URL path pattern, but
	 * differing based on some request attribute, such as a specific header value.
	 *
	 * <p>
	 * See {@link RestMatcher} for details.
	 */
	Class<? extends RestMatcher>[] matchers() default {};

	/**
	 * Serializers. 
	 *
	 * <p>
	 * Overrides the list of serializers assigned at the method level.
	 *
	 * <p>
	 * Use this annotation when the list of serializers assigned to a method differs from the list of serializers
	 * assigned at the servlet level.
	 * 
	 * <p>
	 * To append to the list of serializers assigned at the servlet level, use <code>inherit=<js>"SERIALIZERS"</js></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<jsf>GET</jsf>,
	 * 			path=<js>"/foo"</js>,
	 * 			serializers=MySpecialSerializer.<jk>class</jk>,
	 * 			inherit=<js>"SERIALIZERS"</js>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_serializers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#serializers()} 
	 * 			<li>{@link RestMethod#serializers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializers(Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(Serializer...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Serializer...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Parsers. 
	 *
	 * <p>
	 * Overrides the list of parsers assigned at the method level.
	 *
	 * <p>
	 * Use this annotation when the list of parsers assigned to a method differs from the list of parsers assigned at
	 * the servlet level.
	 *
	 * <p>
	 * To append to the list of parsers assigned at the servlet level, use
	 * <code>inherit=<js>"PARSERS"</js></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<jsf>PUT</jsf>,
	 * 			path=<js>"/foo"</js>,
	 * 			parsers=MySpecialParser.<jk>class</jk>,
	 * 			inherit=<js>"PARSERS"</js>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_parsers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#parsers()} 
	 * 			<li>{@link RestMethod#parsers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parsers(Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(Parser...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Parser...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>Values are added AFTER those found in the annotation and therefore take precedence over those defined via the
	 * 		annotation.
	 * </ul>
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * Identifies what class-level properties are inherited by the serializers and parsers defined on the method.
	 * 
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>"SERIALIZERS" - Inherit class-level serializers.
	 * 	<li>"PARSERS" - Inherit class-level parsers.
	 * 	<li>"TRANSFORMS" - Inherit class-level bean properties and pojo-swaps.
	 * 	<li>"PROPERTIES" - Inherit class-level properties (other than transforms).
	 * 	<li>"ENCODERS" - Inherit class-level encoders.
	 * 	<li>"*" - Inherit everything.
	 * </ul>
	 *
	 * <p>
	 * For example, to inherit all parsers, properties, and transforms from the servlet class:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		parsers=MySpecialParser.<jk>class</jk>,
	 * 		inherit=<js>"PARSERS,PROPERTIES,TRANSFORMS"</js>
	 * 	)
	 * </p>
	 */
	String inherit() default "";
	
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
	 * Compression encoders. 
	 *
	 * <p>
	 * Use this annotation when the list of encoders assigned to a method differs from the list of encoders assigned at
	 * the servlet level.
	 * 
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_encoders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#encoders()} 
	 * 			<li>{@link RestMethod#encoders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}
	 * 			<li>{@link RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * 	<li>Instance classes must provide a public no-arg constructor, or a public constructor that takes in a
	 * 		{@link PropertyStore} object.
	 * 	<li>Instance class can be defined as an inner class of the REST resource class.
	 * 	<li>Use <code>inherit={<js>"ENCODERS"</js>}</code> to inherit encoders from the resource class.
	 * </ul>
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Same as {@link RestResource#properties()}, except defines property values by default when this method is called.
	 *
	 * <p>
	 * This is equivalent to simply calling <code>res.addProperties()</code> in the Java method, but is provided for
	 * convenience.
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
	 * Appends the specified bean filters to all serializers and parsers used by this method.
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
	 * Appends the specified POJO swaps to all serializers and parsers used by this method.
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
	 * Shortcut for specifying the {@link BeanContext#BEAN_includeProperties} property on all serializers.
	 *
	 * <p>
	 * The typical use case is when you're rendering summary and details views of the same bean in a resource and
	 * you want to expose or hide specific properties depending on the level of detail you want.
	 *
	 * <p>
	 * In the example below, our 'summary' view is a list of beans where we only want to show the ID property,
	 * and our detail view is a single bean where we want to expose different fields:
	 * <p class='bcode'>
	 *	<jc>// Our bean</jc>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Summary properties</jc>
	 * 		<ja>@Html</ja>(link=<js>"servlet:/mybeans/{id}"</js>)
	 * 		<jk>public</jk> String <jf>id</jf>;
	 *
	 * 		<jc>// Detail properties</jc>
	 * 		<jk>public</jk> String <jf>a</jf>, <jf>b</jf>;
	 * 	}
	 *
	 *	<jc>// Only render "id" property.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/mybeans"</js>, bpi=<js>"MyBean: id"</js>)
	 * 	<jk>public</jk> List&lt;MyBean&gt; getBeanSummary();
	 *
	 *	<jc>// Only render "a" and "b" properties.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/mybeans/{id}"</js>, bpi=<js>"MyBean: a,b"</js>)
	 * 	<jk>public</jk> MyBean getBeanDetails(<ja>@Path</ja> String id);
	 * </p>
	 *
	 * <p>
	 * The format of each value is: <js>"Key: comma-delimited-tokens"</js>.
	 * <br>Keys can be fully-qualified or short class names or <js>"*"</js> to represent all classes.
	 * <br>Values are comma-delimited lists of bean property names.
	 * <br>Properties apply to specified class and all subclasses.
	 *
	 * <p>
	 * Semicolons can be used as an additional separator for multiple values:
	 * <p class='bcode'>
	 * 	<jc>// Equivalent</jc>
	 * 	bpi={<js>"Bean1: foo"</js>,<js>"Bean2: bar,baz"</js>}
	 * 	bpi=<js>"Bean1: foo; Bean2: bar,baz"</js>
	 * </p>
	 */
	String[] bpi() default {};

	/**
	 * Shortcut for specifying the {@link BeanContext#BEAN_excludeProperties} property on all serializers.
	 *
	 * <p>
	 * Same as {@link #bpi()} except you specify a list of bean property names that you want to exclude from
	 * serialization.
	 *
	 * <p>
	 * In the example below, our 'summary' view is a list of beans where we want to exclude some properties:
	 * <p class='bcode'>
	 *	<jc>// Our bean</jc>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Summary properties</jc>
	 * 		<ja>@Html</ja>(link=<js>"servlet:/mybeans/{id}"</js>)
	 * 		<jk>public</jk> String <jf>id</jf>;
	 *
	 * 		<jc>// Detail properties</jc>
	 * 		<jk>public</jk> String <jf>a</jf>, <jf>b</jf>;
	 * 	}
	 *
	 *	<jc>// Don't show "a" and "b" properties.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/mybeans"</js>, bpx=<js>"MyBean: a,b"</js>)
	 * 	<jk>public</jk> List&lt;MyBean&gt; getBeanSummary();
	 *
	 *	<jc>// Render all properties.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/mybeans/{id}"</js>)
	 * 	<jk>public</jk> MyBean getBeanDetails(<ja>@Path</ja> String id);
	 * </p>
	 *
	 * <p>
	 * The format of each value is: <js>"Key: comma-delimited-tokens"</js>.
	 * <br>Keys can be fully-qualified or short class names or <js>"*"</js> to represent all classes.
	 * <br>Values are comma-delimited lists of bean property names.
	 * <br>Properties apply to specified class and all subclasses.
	 *
	 * <p>
	 * Semicolons can be used as an additional separator for multiple values:
	 * <p class='bcode'>
	 * 	<jc>// Equivalent</jc>
	 * 	bpx={<js>"Bean1: foo"</js>,<js>"Bean2: bar,baz"</js>}
	 * 	bpx=<js>"Bean1: foo; Bean2: bar,baz"</js>
	 * </p>
	 */
	String[] bpx() default {};

	/**
	 * Default request headers.
	 * 
	 * <p>
	 * Specifies default values for request headers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet() {
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
	 * Specifies default values for query parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQuery(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultQuery={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"foo"</js>) String foo) {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * Key and value is trimmed of whitespace.
	 */
	String[] defaultQuery() default {};

	/**
	 * Specifies default values for form-data parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getFormData(String)} when the parameter is not present on the
	 * request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>, path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String foo) {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * Key and value is trimmed of whitespace.
	 */
	String[] defaultFormData() default {};

	/**
	 * Optional summary for the exposed API.
	 *
	 * <p>
	 * This summary is used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The value returned by {@link RestRequest#getMethodSummary()}.
	 * 	<li>
	 * 		The <js>"$R{methodSummary}"</js> variable.
	 * 	<li>
	 * 		The summary of the method in the Swagger page.
	 * </ul>
	 *
	 * <p>
	 * The default value pulls the description from the <code>(className.?)[javaMethodName].summary</code> entry in the
	 * servlet resource bundle. (e.g. <js>"MyClass.myMethod.summary = foo"</js> or <js>"myMethod.summary = foo"</js>).
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/summary</code>.
	 */
	String summary() default "";

	/**
	 * Optional description for the exposed API.
	 *
	 * <p>
	 * This description is used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The value returned by {@link RestRequest#getMethodDescription()}.
	 * 	<li>
	 * 		The <js>"$R{methodDescription}"</js> variable.
	 * 	<li>
	 * 		The description of the method in the Swagger page.
	 * </ul>
	 *
	 * <p>
	 * The default value pulls the description from the <code>(className.?)[javaMethodName].description</code> entry in
	 * the servlet resource bundle. (e.g. <js>"MyClass.myMethod.description = foo"</js> or
	 * <js>"myMethod.description = foo"</js>).
	 *
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/description</code>.
	 */
	String description() default "";

	/**
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link RestResource#clientVersionHeader()} which by default is <js>"X-Client-Version"</js>.
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
	 * <p class='bcode'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3() {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod()
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
	 */
	String clientVersion() default "";

	/**
	 * Provides swagger-specific metadata on this method.
	 */
	MethodSwagger swagger() default @MethodSwagger;

	/**
	 * Provides HTML-doc-specific metadata on this method.
	 *
	 * <p>
	 * Information provided here overrides information provided in the servlet-level annotation.
	 */
	HtmlDoc htmldoc() default @HtmlDoc;

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
	 * 	<ja>@RestMethod</ja>(
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
