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
import org.apache.juneau.encoders.*;
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
	 *
	 * 	<jc>// Example 2</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{0}/{1}/{2}/*"</js>)
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
	 * Method guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with a method call.
	 * These guards get called immediately before execution of the REST method.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Method response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter RestConverters} with a method call.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * Default converters are available in the <a class='doclink'
	 * href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
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
	 * Overrides the list of serializers assigned at the method level.
	 *
	 * <p>
	 * Use this annotation when the list of serializers assigned to a method differs from the list of serializers
	 * assigned at the servlet level.
	 *
	 * <p>
	 * To append to the list of serializers assigned at the servlet level, use
	 * <code>serializersInherit=<jsf>SERIALIZERS</jsf></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"GET"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			serializers=MySpecialSerializer.<jk>class</jk>,
	 * 			serializersInherit=<jsf>SERIALIZERS</jsf>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Used in conjunction with {@link #serializers()} to identify what class-level settings are inherited by the method
	 * serializer group.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link Inherit#SERIALIZERS} - Inherit class-level serializers.
	 * 	<li>{@link Inherit#PROPERTIES} - Inherit class-level properties.
	 * 	<li>{@link Inherit#TRANSFORMS} - Inherit class-level transforms.
	 * </ul>
	 *
	 * <p>
	 * For example, to inherit all serializers, properties, and transforms from the servlet class:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		serializers=MySpecialSerializer.<jk>class</jk>,
	 * 		serializersInherit={<jsf>SERIALIZERS</jsf>,<jsf>PROPERTIES</jsf>,<jsf>TRANSFORMS</jsf>}
	 * 	)
	 * </p>
	 */
	Inherit[] serializersInherit() default {};

	/**
	 * Overrides the list of parsers assigned at the method level.
	 *
	 * <p>
	 * Use this annotation when the list of parsers assigned to a method differs from the list of parsers assigned at
	 * the servlet level.
	 *
	 * <p>
	 * To append to the list of serializers assigned at the servlet level, use
	 * <code>serializersInherit=<jsf>SERIALIZERS</jsf></code>.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"PUT"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			parsers=MySpecialParser.<jk>class</jk>,
	 * 			parsersInherit=<jsf>PARSERS</jsf>
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialAcceptType() {
	 * 			<jc>// Handle request for special Accept type</jc>
	 * 		}
	 * 	}
	 * </p>
	 */
	Class<? extends Parser>[] parsers() default {};

	/**
	 * Used in conjunction with {@link #parsers()} to identify what class-level settings are inherited by the method
	 * parser group.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link Inherit#PARSERS} - Inherit class-level parsers.
	 * 	<li>{@link Inherit#PROPERTIES} - Inherit class-level properties.
	 * 	<li>{@link Inherit#TRANSFORMS} - Inherit class-level transforms.
	 * </ul>
	 *
	 * <p>
	 * For example, to inherit all parsers, properties, and transforms from the servlet class:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		parsers=MySpecialParser.<jk>class</jk>,
	 * 		parsersInherit={<jsf>PARSERS</jsf>,<jsf>PROPERTIES</jsf>,<jsf>TRANSFORMS</jsf>}
	 * 	)
	 * </p>
	 */
	Inherit[] parsersInherit() default {};

	/**
	 * Appends to the list of {@link Encoder encoders} specified on the servlet.
	 *
	 * <p>
	 * Use this annotation when the list of encoders assigned to a method differs from the list of encoders assigned at
	 * the servlet level.
	 *
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@RestMethod</ja>(
	 * 			name=<js>"PUT"</js>,
	 * 			path=<js>"/foo"</js>,
	 * 			encoders={GzipEncoder.<jk>class</jk>}
	 * 		)
	 * 		<jk>public</jk> Object doGetWithSpecialEncoding() {
	 * 			<jc>// Handle request with special encoding</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * If you want to OVERRIDE the set of encoders specified by the servlet, combine this annotation with
	 * <code><ja>@RestMethod</ja>(inheritEncoders=<jk>false</jk>)</code>.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Specifies whether the method should inherit encoders from the servlet.
	 */
	boolean inheritEncoders() default true;

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
	 */
	Class<?>[] beanFilters() default {};

	/**
	 * Appends the specified POJO swaps to all serializers and parsers used by this method.
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/mybeans"</js>, bpIncludes=<js>"{MyBean:'id'}"</js>)
	 * 	<jk>public</jk> List&lt;MyBean&gt; getBeanSummary();
	 *
	 *	<jc>// Only render "a" and "b" properties.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/mybeans/{id}"</js>, bpIncludes=<js>"{MyBean:'a,b'}"</js>)
	 * 	<jk>public</jk> MyBean getBeanDetails(<ja>@Path</ja> String id);
	 * </p>
	 *
	 * <p>
	 * The format of this value is a lax JSON object.
	 * <br>Keys can be fully-qualified or short class names or <js>"*"</js> to represent all classes.
	 * <br>Values are comma-delimited lists of bean property names.
	 * <br>Properties apply to specified class and all subclasses.
	 */
	String bpIncludes() default "";

	/**
	 * Shortcut for specifying the {@link BeanContext#BEAN_excludeProperties} property on all serializers.
	 *
	 * <p>
	 * Same as {@link #bpIncludes()} except you specify a list of bean property names that you want to exclude from
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/mybeans"</js>, bpExcludes=<js>"{MyBean:'a,b'}"</js>)
	 * 	<jk>public</jk> List&lt;MyBean&gt; getBeanSummary();
	 *
	 *	<jc>// Render all properties.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/mybeans/{id}"</js>)
	 * 	<jk>public</jk> MyBean getBeanDetails(<ja>@Path</ja> String id);
	 * </p>
	 *
	 * <p>
	 * The format of this value is a lax JSON object.
	 * <br>Keys can be fully-qualified or short class names or <js>"*"</js> to represent all classes.
	 * <br>Values are comma-delimited lists of bean property names.
	 * <br>Properties apply to specified class and all subclasses.
	 */
	String bpExcludes() default "";

	/**
	 * Specifies default values for request headers.
	 *
	 * <p>
	 * Strings are of the format <js>"Header-Name: header-value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 *
	 * <p>
	 * The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * specified so that a particular default {@link Serializer} is picked.
	 *
	 * <p>
	 * Only one header value can be specified per entry (i.e. it's not a delimited list of header entries).
	 *
	 * <p>
	 * Header values specified at the method level override header values specified at the servlet level.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet() {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * You can use either <js>':'</js> or <js>'='</js> as the key/value delimiter.
	 * Key and value is trimmed of whitespace.
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/*"</js>, defaultQuery={<js>"foo=bar"</js>})
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
	 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>, path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
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
	 * See {@link RestContext#getVarResolver()} for the list of supported variables.
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
	 * See {@link RestContext#getVarResolver()} for the list of supported variables.
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
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
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
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
}
