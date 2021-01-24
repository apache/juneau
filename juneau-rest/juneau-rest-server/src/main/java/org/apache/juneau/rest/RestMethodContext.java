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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.StringUtils.firstNonEmpty;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import javax.servlet.*;

import org.apache.http.*;
import org.apache.http.ParseException;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.guards.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
@ConfigurableContext(nocache=true)
public class RestMethodContext extends BeanContext implements Comparable<RestMethodContext>  {

	/** Represents a null value for the {@link RestMethod#contextClass()} annotation.*/
	@SuppressWarnings("javadoc")
	public static final class Null extends RestMethodContext {
		public Null(RestMethodContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestMethodContext";

	/**
	 * Configuration property:  Client version pattern matcher.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_clientVersion RESTMETHOD_clientVersion}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.clientVersion.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.clientVersion</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_CLIENTVERSION</c>
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#clientVersion()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#clientVersion(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link Rest#clientVersionHeader() @Rest(clientVersionHeader)} which by default is <js>"X-Client-Version"</js>.
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
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 */
	public static final String RESTMETHOD_clientVersion = PREFIX + ".clientVersion.s";

	/**
	 * Configuration property:  REST method context class.
	 *
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_contextClass RESTMETHOD_contextClass}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.contextClass.c"</js>
	 * 	<li><b>Data type:</b>  <c>Class&lt;? extends {@link org.apache.juneau.rest.RestMethodContext}&gt;</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.RestMethodContext}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#methodContextClass()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#contextClass()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#methodContextClass(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#contextClass(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to extend the {@link RestMethodContext} class to modify how any of the functions are implemented.
	 *
	 * <p>
	 * The subclass must have a public constructor that takes in any of the following arguments:
	 * <ul>
	 * 	<li>{@link RestMethodContextBuilder} - The builder for the object.
	 * 	<li>Any beans found in the specified {@link #REST_beanFactory bean factory}.
	 * 	<li>Any {@link Optional} beans that may or may not be found in the specified {@link #REST_beanFactory bean factory}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our extended context class that adds a request attribute to all requests.</jc>
	 * 	<jc>// The attribute value is provided by an injected spring bean.</jc>
	 * 	<jk>public</jk> MyRestMethodContext <jk>extends</jk> RestMethodContext {
	 *
	 * 		<jk>private final</jk> Optional&lt;? <jk>extends</jk> Supplier&lt;Object&gt;&gt; <jf>fooSupplier</jf>;
	 *
	 * 		<jc>// Constructor that takes in builder and optional injected attribute provider.</jc>
	 * 		<jk>public</jk> MyRestMethodContext(RestMethodContextBuilder <jv>builder</jv>, Optional&lt;AnInjectedFooSupplier&gt; <jv>fooSupplier</jv>) {
	 * 			<jk>super</jk>(<jv>builder</jv>);
	 * 			<jk>this</jk>.<jf>fooSupplier</jf> = <jv>fooSupplier</jv>.orElseGet(()-><jk>null</jk>);
	 * 		}
	 *
	 * 		<jc>// Override the method used to create default request attributes.</jc>
	 * 		<ja>@Override</ja>
	 * 		<jk>protected</jk> NamedAttributeList createDefaultRequestAttributes(Object <jv>resource</jv>, BeanFactory <jv>beanFactory</jv>, Method <jv>method</jv>, RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return super</jk>
	 * 				.createDefaultRequestAttributes(<jv>resource</jv>, <jv>beanFactory</jv>, <jv>method</jv>, <jv>context</jv>)
	 * 				.append(NamedAttribute.<jsm>of</jsm>(<js>"foo"</js>, ()-><jf>fooSupplier</jf>.get());
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 		<ja>@RestMethod</ja>(contextClass=MyRestMethodContext.<jk>class</jk>)
	 * 		<jk>public</jk> Object getFoo(RequestAttributes <jv>attributes</jv>) {
	 * 			<jk>return</jk> <jv>attributes</jv>.get(<js>"foo"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String RESTMETHOD_contextClass = PREFIX + ".contextClass.c";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_debug RESTMETHOD_debug}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Enablement}
	 * 	<li><b>System property:</b>  <c>RestMethodContext.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#debug(Enablement)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * </ul>
	 */
	public static final String RESTMETHOD_debug = PREFIX + ".debug.s";

	/**
	 * Configuration property:  Default form data.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultFormData RESTMETHOD_defaultFormData}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultFormData.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NameValuePair}[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultFormData</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTFORMDATA</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultFormData()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultFormData(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultFormData(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultFormData(NameValuePair...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for form-data parameters.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getFormData(String)} when the parameter is not present on the
	 * request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(method=<jsf>POST</jsf>, path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String foo)  {...}
	 * </p>
	 */
	public static final String RESTMETHOD_defaultFormData = PREFIX + ".defaultFormData.lo";

	/**
	 * Configuration property:  Default query parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultQuery RESTMETHOD_defaultQuery}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultQuery.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NameValuePair}[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultQuery</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTQUERY</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultQuery()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultQuery(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultQuery(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultQuery(NameValuePair...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for query parameters.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQuery(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultQuery={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"foo"</js>) String foo)  {...}
	 * </p>
	 */
	public static final String RESTMETHOD_defaultQuery = PREFIX + ".defaultQuery.lo";

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultRequestAttributes RESTMETHOD_defaultRequestAttributes}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.reqAttrs.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NamedAttribute}[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultRequestAttributes</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTREQUESTATTRIBUTES</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultRequestAttributes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestAttribute(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestAttribute(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestAttributes(NamedAttribute...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they are not already set on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultRequestAttributes={<js>"Foo=bar"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 */
	public static final String RESTMETHOD_defaultRequestAttributes = PREFIX + ".defaultRequestAttributes.lo";

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultRequestHeaders RESTMETHOD_defaultRequestHeaders}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultRequestHeaders.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.http.Header}[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultRequestHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTREQUESTHEADERS</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultRequestHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultAccept()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultContentType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestHeader(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultRequestHeaders(org.apache.http.Header...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 */
	public static final String RESTMETHOD_defaultRequestHeaders = PREFIX + ".defaultRequestHeaders.lo";

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultResponseHeaders RESTMETHOD_defaultResponseHeaders}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultResponseHeaders.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.http.Header}[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultResponseHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTRESPONSEHEADERS</c>
	 * 	<li><b>Default:</b>  empty list.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultRequestHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultAccept()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultContentType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultResponseHeader(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#defaultResponseHeaders(org.apache.http.Header...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not overwritten during the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultResponseHeaders={<js>"Content-Type: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 */
	public static final String RESTMETHOD_defaultResponseHeaders = PREFIX + ".defaultResponseHeaders.lo";

	/**
	 * Configuration property:  HTTP method name.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_httpMethod RESTMETHOD_httpMethod}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.httpMethod.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.httpMethod</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_HTTPMETHOD</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#method()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#httpMethod(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * REST method name.
	 *
	 * <p>
	 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 *
	 * <p>
	 * Method names are case-insensitive (always folded to upper-case).
	 *
	 * <p>
	 * Note that you can use {@link org.apache.juneau.http.HttpMethod} for constant values.
	 *
	 * <p>
	 * Besides the standard HTTP method names, the following can also be specified:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"*"</js>
	 * 		- Denotes any method.
	 * 		<br>Use this if you want to capture any HTTP methods in a single Java method.
	 * 		<br>The {@link org.apache.juneau.rest.annotation.Method @Method} annotation and/or {@link RestRequest#getMethod()} method can be used to
	 * 		distinguish the actual HTTP method name.
	 * 	<li>
	 * 		<js>""</js>
	 * 		- Auto-detect.
	 * 		<br>The method name is determined based on the Java method name.
	 * 		<br>For example, if the method is <c>doPost(...)</c>, then the method name is automatically detected
	 * 		as <js>"POST"</js>.
	 * 		<br>Otherwise, defaults to <js>"GET"</js>.
	 * 	<li>
	 * 		<js>"RRPC"</js>
	 * 		- Remote-proxy interface.
	 * 		<br>This denotes a Java method that returns an object (usually an interface, often annotated with the
	 * 		{@link Remote @Remote} annotation) to be used as a remote proxy using
	 * 		<c>RestClient.getRemoteInterface(Class&lt;T&gt; interfaceClass, String url)</c>.
	 * 		<br>This allows you to construct client-side interface proxies using REST as a transport medium.
	 * 		<br>Conceptually, this is simply a fancy <c>POST</c> against the url <js>"/{path}/{javaMethodName}"</js>
	 * 		where the arguments are marshalled from the client to the server as an HTTP body containing an array of
	 * 		objects, passed to the method as arguments, and then the resulting object is marshalled back to the client.
	 * 	<li>
	 * 		Anything else
	 * 		- Overloaded non-HTTP-standard names that are passed in through a <c>&amp;method=methodName</c> URL
	 * 		parameter.
	 * </ul>
	 */
	public static final String RESTMETHOD_httpMethod = PREFIX + ".httpMethod.s";

	/**
	 * Configuration property:  Method-level matchers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_matchers RESTMETHOD_matchers}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.matchers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestMatcher}|Class&lt;{@link org.apache.juneau.rest.RestMatcher}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#matchers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#matchers(RestMatcher...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <p>
	 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
	 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
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
	 * 	<li class='link'>{@doc RestmMatchers}
	 * </ul>
	 */
	public static final String RESTMETHOD_matchers = PREFIX + ".matchers.lo";

	/**
	 * Configuration property:  Resource method paths.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_path RESTMETHOD_path}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.path.ls"</js>
	 * 	<li><b>Data type:</b>  <c>String[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.path</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_PATH</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#path()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#path(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the URL subpath relative to the servlet class.
	 *
	 * <p>
	 * <ul class='notes'>
	 * 	<li>
	 * 		This method is only applicable for Java methods.
	 * 	<li>
	 * 		Slashes are trimmed from the path ends.
	 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
	 * </ul>
	 */
	public static final String RESTMETHOD_path = PREFIX + ".path.ls";

	/**
	 * Configuration property:  Priority.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_priority RESTMETHOD_priority}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.priority.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.priority</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_PRIORITY</c>
	 * 	<li><b>Default:</b>  <c>0</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#priority()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestMethodContextBuilder#priority(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * URL path pattern priority.
	 *
	 * <p>
	 * To force path patterns to be checked before other path patterns, use a higher priority number.
	 *
	 * <p>
	 * By default, it's <c>0</c>, which means it will use an internal heuristic to determine a best match.
	 */
	public static final String RESTMETHOD_priority = PREFIX + ".priority.i";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String httpMethod;
	private final UrlPathMatcher[] pathMatchers;
	final RestParam[] methodParams;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final Integer priority;
	private final RestContext context;
	private final BeanFactory beanFactory;
	final Method method;
	final MethodInvoker methodInvoker;
	final MethodInfo mi;
	final SerializerGroup serializers;
	final ParserGroup parsers;
	final EncoderGroup encoders;
	final HttpPartSerializer partSerializer;
	final HttpPartParser partParser;
	final JsonSchemaGenerator jsonSchemaGenerator;
	final org.apache.http.Header[] defaultRequestHeaders, defaultResponseHeaders;
	final NameValuePair[] defaultRequestQuery, defaultRequestFormData;
	final NamedAttribute[] defaultRequestAttributes;
	final String defaultCharset;
	final long maxInput;
	final List<MediaType>
		supportedAcceptTypes,
		supportedContentTypes;
	final RestLogger callLogger;

	final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> bodyPartMetas = new ConcurrentHashMap<>();
	final ResponseBeanMeta responseMeta;

	final Map<Integer,AtomicInteger> statusCodes = new ConcurrentHashMap<>();

	final Enablement debug;
	final int hierarchyDepth;

	/**
	 * Creator.
	 *
	 * @param method The Java method this context belongs to.
	 * @param context The Java class context.
	 * @return A new builder.
	 */
	public static RestMethodContextBuilder create(java.lang.reflect.Method method, RestContext context) {
		return new RestMethodContextBuilder(method, context);
	}

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	public RestMethodContext(RestMethodContextBuilder builder) throws ServletException {
		super(builder.getPropertyStore());

		try {
			context = builder.restContext;
			method = builder.restMethod;

			PropertyStore ps = getPropertyStore();

			methodInvoker = new MethodInvoker(method, context.getMethodExecStats(method));
			mi = MethodInfo.of(method).accessible();
			Object r = context.getResource();

			beanFactory = BeanFactory.of(context.rootBeanFactory, r)
				.addBean(RestMethodContext.class, this)
				.addBean(Method.class, method)
				.addBean(PropertyStore.class, ps);
			beanFactory.addBean(BeanFactory.class, beanFactory);

			serializers = createSerializers(r, beanFactory, ps);
			beanFactory.addBean(SerializerGroup.class, serializers);

			parsers = createParsers(r, beanFactory, ps);
			beanFactory.addBean(ParserGroup.class, parsers);

			partSerializer = createPartSerializer(r, beanFactory, ps);
			beanFactory.addBean(HttpPartSerializer.class, partSerializer);

			partParser = createPartParser(r, beanFactory, ps);
			beanFactory.addBean(HttpPartParser.class, partParser);

			converters = createConverters(r, beanFactory).asArray();
			beanFactory.addBean(RestConverter[].class, converters);

			guards = createGuards(r, beanFactory).asArray();
			beanFactory.addBean(RestGuard[].class, guards);

			RestMatcherList matchers = createMatchers(r, beanFactory);
 			requiredMatchers = matchers.stream().filter(x -> x.required()).toArray(RestMatcher[]::new);
			optionalMatchers = matchers.stream().filter(x -> ! x.required()).toArray(RestMatcher[]::new);

			pathMatchers = createPathMatchers(r, beanFactory).asArray();
			beanFactory.addBean(UrlPathMatcher[].class, pathMatchers);
			beanFactory.addBean(UrlPathMatcher.class, pathMatchers.length > 0 ? pathMatchers[0] : null);

			encoders = createEncoders(r, beanFactory);
			beanFactory.addBean(EncoderGroup.class, encoders);

			jsonSchemaGenerator = createJsonSchemaGenerator(r, beanFactory, ps);
			beanFactory.addBean(JsonSchemaGenerator.class, jsonSchemaGenerator);

			supportedAcceptTypes = getListProperty(REST_produces, MediaType.class, serializers.getSupportedMediaTypes());
			supportedContentTypes = getListProperty(REST_consumes, MediaType.class, parsers.getSupportedMediaTypes());

			defaultRequestHeaders = createDefaultRequestHeaders(r, beanFactory, method, context).asArray();
			defaultResponseHeaders = createDefaultResponseHeaders(r, beanFactory, method, context).asArray();
			defaultRequestQuery = createDefaultRequestQuery(r, beanFactory, method).asArray();
			defaultRequestFormData = createDefaultRequestFormData(r, beanFactory, method).asArray();
			defaultRequestAttributes = createDefaultRequestAttributes(r, beanFactory, method, context).asArray();

			int _hierarchyDepth = 0;
			Class<?> sc = method.getDeclaringClass().getSuperclass();
			while (sc != null) {
				_hierarchyDepth++;
				sc = sc.getSuperclass();
			}
			hierarchyDepth = _hierarchyDepth;

			String _httpMethod = getProperty(RESTMETHOD_httpMethod, String.class, null);
			if (_httpMethod == null)
				_httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(_httpMethod))
				_httpMethod = "*";
			httpMethod = _httpMethod.toUpperCase(Locale.ENGLISH);

			defaultCharset = getProperty(REST_defaultCharset, String.class, "utf-8");

			maxInput = StringUtils.parseLongWithSuffix(getProperty(REST_maxInput, String.class, "100M"));

			responseMeta = ResponseBeanMeta.create(mi, ps);

			methodParams = context.findRestMethodParams(mi.inner(), beanFactory);

			this.priority = getIntegerProperty(RESTMETHOD_priority, 0);

			this.debug = context.getDebug(method);
			this.callLogger = context.getCallLogger();
		} catch (ServletException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private String joinnlFirstNonEmptyArray(String[]...s) {
		for (String[] ss : s)
			if (ss.length > 0)
				return joinnl(ss);
		return null;
	}

	ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, serializers.getPropertyStore());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Instantiates the result converters for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_converters} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}/{@link RestContextBuilder#converters(RestConverter...)}
	 * 			<li>{@link RestMethod#converters()}.
	 * 			<li>{@link Rest#converters()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createConverters()</> method that returns <c>{@link RestConverter}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestConverter[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The result converters for this REST resource method.
	 * @throws Exception If result converters could not be instantiated.
	 * @seealso #REST_converters
	 */
	protected RestConverterList createConverters(Object resource, BeanFactory beanFactory) throws Exception {

		RestConverterList x = RestConverterList.create();

		x.append(getInstanceArrayProperty(REST_converters, RestConverter.class, new RestConverter[0], beanFactory));

		if (x.isEmpty())
			x = beanFactory.getBean(RestConverterList.class).orElse(x);

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(RestConverterList.class, x)
			.beanCreateMethodFinder(RestConverterList.class, resource)
			.find("createConverters", Method.class)
			.thenFind("createConverters")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the guards for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_guards} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}/{@link RestContextBuilder#guards(RestGuard...)}
	 * 			<li>{@link RestMethod#guards()}.
	 * 			<li>{@link Rest#guards()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createGuards()</> method that returns <c>{@link RestGuard}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestGuard[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The guards for this REST resource method.
	 * @throws Exception If guards could not be instantiated.
	 * @seealso #REST_guards
	 */
	protected RestGuardList createGuards(Object resource, BeanFactory beanFactory) throws Exception {

		RestGuardList x = RestGuardList.create();

		x.append(getInstanceArrayProperty(REST_guards, RestGuard.class, new RestGuard[0], beanFactory));

		if (x.isEmpty())
			x = beanFactory.getBean(RestGuardList.class).orElse(x);

		Set<String> rolesDeclared = getSetProperty(REST_rolesDeclared, String.class, null);
		Set<String> roleGuard = getSetProperty(REST_roleGuard, String.class, Collections.emptySet());

		for (String rg : roleGuard) {
			try {
				x.add(new RoleBasedRestGuard(rolesDeclared, rg));
			} catch (java.text.ParseException e1) {
				throw new ServletException(e1);
			}
		}

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(RestGuardList.class, x)
			.beanCreateMethodFinder(RestGuardList.class, resource)
			.find("createGuards", Method.class)
			.thenFind("createGuards")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the method matchers for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #RESTMETHOD_matchers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestMethod#matchers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createMatchers()</> method that returns <c>{@link RestMatcher}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link java.lang.reflect.Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestMatcher[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The method matchers for this REST resource method.
	 * @throws Exception If method matchers could not be instantiated.
	 * @seealso #RESTMETHOD_matchers
	 */
	protected RestMatcherList createMatchers(Object resource, BeanFactory beanFactory) throws Exception {

		RestMatcherList x = RestMatcherList.create();

		x.append(getInstanceArrayProperty(RESTMETHOD_matchers, RestMatcher.class, new RestMatcher[0], beanFactory));

		if (x.isEmpty())
			x = beanFactory.getBean(RestMatcherList.class).orElse(x);

		String clientVersion = getProperty(RESTMETHOD_clientVersion, String.class, null);
		if (clientVersion != null)
			x.add(new ClientVersionMatcher(context.getClientVersionHeader(), mi));

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(RestMatcherList.class, x)
			.beanCreateMethodFinder(RestMatcherList.class, resource)
			.find("createMatchers", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the encoders for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_encoders} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}/{@link RestContextBuilder#encoders(Encoder...)}
	 * 			<li>{@link RestMethod#encoders()}.
	 * 			<li>{@link Rest#encoders()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createEncoders()</> method that returns <c>{@link Encoder}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Encoder[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The encoders for this REST resource method.
	 * @throws Exception If encoders could not be instantiated.
	 * @seealso #REST_encoders
	 */
	protected EncoderGroup createEncoders(Object resource, BeanFactory beanFactory) throws Exception {

		Encoder[] x = getInstanceArrayProperty(REST_encoders, Encoder.class, null, beanFactory);

		if (x == null)
			x = beanFactory.getBean(Encoder[].class).orElse(null);

		if (x == null)
			x = new Encoder[0];

		EncoderGroup g = EncoderGroup
			.create()
			.append(IdentityEncoder.INSTANCE)
			.append(x)
			.build();

		g = BeanFactory
			.of(beanFactory, resource)
			.addBean(EncoderGroup.class, g)
			.beanCreateMethodFinder(EncoderGroup.class, resource)
			.find("createEncoders", Method.class)
			.thenFind("createEncoders")
			.withDefault(g)
			.run();

		return g;
	}

	/**
	 * Instantiates the serializers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_serializers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializers(Class...)}/{@link RestContextBuilder#serializers(Serializer...)}
	 * 			<li>{@link Rest#serializers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createSerializers()</> method that returns <c>{@link Serializer}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Serializer[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The serializers for this REST resource.
	 * @throws Exception If serializers could not be instantiated.
	 * @seealso #REST_serializers
	 */
	protected SerializerGroup createSerializers(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {

		SerializerGroup g = beanFactory.getBean(SerializerGroup.class).orElse(null);

		if (g == null) {
			Object[] x = getArrayProperty(REST_serializers, Object.class);

			if (x == null)
				x = beanFactory.getBean(Serializer[].class).orElse(null);

			if (x == null)
				x = new Serializer[0];

			g = SerializerGroup
				.create()
				.append(x)
				.apply(ps)
				.build();
		}

		g = BeanFactory
			.of(beanFactory, resource)
			.addBean(SerializerGroup.class, g)
			.beanCreateMethodFinder(SerializerGroup.class, resource)
			.find("createSerializers", Method.class)
			.thenFind("createSerializers")
			.withDefault(g)
			.run();

		return g;
	}

	/**
	 * Instantiates the parsers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_parsers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parsers(Class...)}/{@link RestContextBuilder#parsers(Parser...)}
	 * 			<li>{@link Rest#parsers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createParsers()</> method that returns <c>{@link Parser}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Parser[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The parsers for this REST resource.
	 * @throws Exception If parsers could not be instantiated.
	 * @seealso #REST_parsers
	 */
	protected ParserGroup createParsers(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {

		ParserGroup g = beanFactory.getBean(ParserGroup.class).orElse(null);

		if (g == null) {
			Object[] x = getArrayProperty(REST_parsers, Object.class);

			if (x == null)
				x = beanFactory.getBean(Parser[].class).orElse(null);

			if (x == null)
				x = new Parser[0];

			g = ParserGroup
				.create()
				.append(x)
				.apply(ps)
				.build();
		}

		g = BeanFactory
			.of(beanFactory, resource)
			.addBean(ParserGroup.class, g)
			.beanCreateMethodFinder(ParserGroup.class, resource)
			.find("createParsers", Method.class)
			.thenFind("createParsers")
			.withDefault(g)
			.run();

		return g;
	}

	/**
	 * Instantiates the HTTP part serializer for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
	 * 	<li>Looks for {@link #REST_partSerializer} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}/{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 			<li>{@link Rest#partSerializer()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The HTTP part serializer for this REST resource.
	 * @throws Exception If serializer could not be instantiated.
	 * @seealso #REST_partSerializer
	 */
	protected HttpPartSerializer createPartSerializer(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {

		HttpPartSerializer x = null;

		if (resource instanceof HttpPartSerializer)
			x = (HttpPartSerializer)resource;

		if (x == null)
			x = getInstanceProperty(REST_partSerializer, HttpPartSerializer.class, null, beanFactory);

		if (x == null)
			x = beanFactory.getBean(HttpPartSerializer.class).orElse(null);

		if (x == null)
			x = new OpenApiSerializer(ps);

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(HttpPartSerializer.class, x)
			.beanCreateMethodFinder(HttpPartSerializer.class, resource)
			.find("createPartSerializer", Method.class)
			.thenFind("createPartSerializer")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the HTTP part parser for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartParser}.
	 * 	<li>Looks for {@link #REST_partParser} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}/{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 			<li>{@link Rest#partParser()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartParser()</> method that returns <c>{@link HttpPartParser}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 * @seealso #REST_partParser
	 */
	protected HttpPartParser createPartParser(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {

		HttpPartParser x = null;

		if (resource instanceof HttpPartParser)
			x = (HttpPartParser)resource;

		if (x == null)
			x = getInstanceProperty(REST_partParser, HttpPartParser.class, null, beanFactory);

		if (x == null)
			x = beanFactory.getBean(HttpPartParser.class).orElse(null);

		if (x == null)
			x = new OpenApiParser(ps);

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(HttpPartParser.class, x)
			.beanCreateMethodFinder(HttpPartParser.class, resource)
			.find("createPartParser", Method.class)
			.thenFind("createPartParser")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the path matchers for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 * @seealso #RESTMETHOD_paths
	 */
	protected UrlPathMatcherList createPathMatchers(Object resource, BeanFactory beanFactory) throws Exception {

		UrlPathMatcherList x = UrlPathMatcherList.create();
		boolean dotAll = getBooleanProperty("RestMethodContext.dotAll.b", false);

		for (String p : getArrayProperty(RESTMETHOD_path, String.class)) {
			if (dotAll && ! p.endsWith("/*"))
				p += "/*";
			x.add(UrlPathMatcher.of(p));
		}

		if (x.isEmpty()) {
			String p = HttpUtils.detectHttpPath(method, true);
			if (dotAll && ! p.endsWith("/*"))
				p += "/*";
			x.add(UrlPathMatcher.of(p));
		}

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(UrlPathMatcherList.class, x)
			.beanCreateMethodFinder(UrlPathMatcherList.class, resource)
			.find("createPathMatchers", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the JSON-schema generator for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The JSON-schema generator for this method.
	 * @throws Exception If schema generator could not be instantiated.
	 */
	protected JsonSchemaGenerator createJsonSchemaGenerator(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {

		JsonSchemaGenerator x = null;

		if (resource instanceof JsonSchemaGenerator)
			x = (JsonSchemaGenerator)resource;

		if (x == null)
			x = beanFactory.getBean(JsonSchemaGenerator.class).orElse(null);

		if (x == null)
			x = JsonSchemaGenerator.create().apply(ps).build();

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(JsonSchemaGenerator.class, x)
			.beanCreateMethodFinder(JsonSchemaGenerator.class, resource)
			.find("createJsonSchemaGenerator", Method.class)
			.thenFind("createJsonSchemaGenerator")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request headers for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request headers for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected HeaderList createDefaultRequestHeaders(Object resource, BeanFactory beanFactory, Method method, RestContext context) throws Exception {

		HeaderList x = HeaderList.create();

		x.appendUnique(context.defaultRequestHeaders);

		x.appendUnique(getInstanceArrayProperty(RESTMETHOD_defaultRequestHeaders, org.apache.http.Header.class, new org.apache.http.Header[0], beanFactory));

		for (Annotation[] aa : method.getParameterAnnotations()) {
			for (Annotation a : aa) {
				if (a instanceof Header) {
					Header h = (Header)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							x.appendUnique(BasicHeader.of(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Header annotation");
						}
					}
				}
			}
		}

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(HeaderList.class, x)
			.beanCreateMethodFinder(HeaderList.class, resource)
			.find("createDefaultRequestHeaders", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request headers for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request headers for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected HeaderList createDefaultResponseHeaders(Object resource, BeanFactory beanFactory, Method method, RestContext context) throws Exception {

		HeaderList x = HeaderList.create();

		x.appendUnique(context.defaultResponseHeaders);

		x.appendUnique(getInstanceArrayProperty(RESTMETHOD_defaultResponseHeaders, org.apache.http.Header.class, new org.apache.http.Header[0], beanFactory));

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(HeaderList.class, x)
			.beanCreateMethodFinder(HeaderList.class, resource)
			.find("createDefaultResponseHeaders", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request attributes for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request attributes for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected NamedAttributeList createDefaultRequestAttributes(Object resource, BeanFactory beanFactory, Method method, RestContext context) throws Exception {
		NamedAttributeList x = NamedAttributeList.create();

		x.appendUnique(context.defaultRequestAttributes);

		x.appendUnique(getInstanceArrayProperty(RESTMETHOD_defaultRequestAttributes, NamedAttribute.class, new NamedAttribute[0], beanFactory));

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(NamedAttributeList.class, x)
			.beanCreateMethodFinder(NamedAttributeList.class, resource)
			.find("createDefaultRequestAttributes", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default query parameters for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @return The default request query parameters for this method.
	 * @throws Exception If default request query parameters could not be instantiated.
	 */
	protected NameValuePairList createDefaultRequestQuery(Object resource, BeanFactory beanFactory, Method method) throws Exception {

		NameValuePairList x = NameValuePairList.create();

		x.appendUnique(getInstanceArrayProperty(RESTMETHOD_defaultQuery, NameValuePair.class, new NameValuePair[0], beanFactory));

		for (Annotation[] aa : method.getParameterAnnotations()) {
			for (Annotation a : aa) {
				if (a instanceof Query) {
					Query h = (Query)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							x.appendUnique(BasicNameValuePair.of(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Query annotation");
						}
					}
				}
			}
		}

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(NameValuePairList.class, x)
			.beanCreateMethodFinder(NameValuePairList.class, resource)
			.find("createDefaultRequestQuery", Method.class)
			.thenFind("createDefaultRequestQuery")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default form-data parameters for this method.
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @return The default request form-data parameters for this method.
	 * @throws Exception If default request form-data parameters could not be instantiated.
	 */
	protected NameValuePairList createDefaultRequestFormData(Object resource, BeanFactory beanFactory, Method method) throws Exception {

		NameValuePairList x = NameValuePairList.create();

		x.appendUnique(getInstanceArrayProperty(RESTMETHOD_defaultFormData, NameValuePair.class, new NameValuePair[0], beanFactory));

		for (Annotation[] aa : method.getParameterAnnotations()) {
			for (Annotation a : aa) {
				if (a instanceof FormData) {
					FormData h = (FormData)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							x.appendUnique(BasicNameValuePair.of(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @FormData annotation");
						}
					}
				}
			}
		}

		x = BeanFactory
			.of(beanFactory, resource)
			.addBean(NameValuePairList.class, x)
			.beanCreateMethodFinder(NameValuePairList.class, resource)
			.find("createDefaultRequestFormData", Method.class)
			.thenFind("createDefaultRequestFormData")
			.withDefault(x)
			.run();

		return x;
	}

	ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = headerPartMetas.get(c);
		if (pm == null) {
			ResponseHeader a = c.getAnnotation(ResponseHeader.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(HEADER, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			headerPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	ResponsePartMeta getResponseBodyMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = bodyPartMetas.get(c);
		if (pm == null) {
			ResponseBody a = c.getAnnotation(ResponseBody.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(BODY, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			bodyPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	/**
	 * Returns <jk>true</jk> if this Java method has any guards or matchers.
	 */
	boolean hasGuardsOrMatchers() {
		return (guards.length != 0 || requiredMatchers.length != 0 || optionalMatchers.length != 0);
	}

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 */
	String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 */
	String getPathPattern() {
		return pathMatchers[0].toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified request object can call this method.
	 */
	boolean isRequestAllowed(RestRequest req) {
		for (RestGuard guard : guards) {
			req.setJavaMethod(method);
			if (! guard.isRequestAllowed(req))
				return false;
		}
		return true;
	}

	boolean matches(UrlPath urlPath) {
		for (UrlPathMatcher p : pathMatchers)
			if (p.match(urlPath) != null)
				return true;
		return false;
	}

	/**
	 * Identifies if this method can process the specified call.
	 *
	 * <p>
	 * To process the call, the following must be true:
	 * <ul>
	 * 	<li>Path pattern must match.
	 * 	<li>Matchers (if any) must match.
	 * </ul>
	 *
	 * @param call The call to check.
	 * @return
	 * 	One of the following values:
	 * 	<ul>
	 * 		<li><c>0</c> - Path doesn't match.
	 * 		<li><c>1</c> - Path matched but matchers did not.
	 * 		<li><c>2</c> - Matches.
	 * 	</ul>
	 */
	protected int match(RestCall call) {

		UrlPathMatch pm = matchPattern(call);

		if (pm == null)
			return 0;

		if (requiredMatchers.length == 0 && optionalMatchers.length == 0) {
			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		}

		try {
			RestRequest req = call.getRestRequest();
			RestResponse res = call.getRestResponse();

			req.init(this);
			res.init(this);

			// If the method implements matchers, test them.
			for (RestMatcher m : requiredMatchers)
				if (! m.matches(req))
					return 1;
			if (optionalMatchers.length > 0) {
				boolean matches = false;
				for (RestMatcher m : optionalMatchers)
					matches |= m.matches(req);
				if (! matches)
					return 1;
			}

			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	private UrlPathMatch matchPattern(RestCall call) {
		UrlPathMatch pm = null;
		for (UrlPathMatcher pp : pathMatchers)
			if (pm == null)
				pm = pp.match(call.getUrlPath());
		return pm;
	}

	/**
	 * Workhorse method.
	 *
	 * @param call Invokes the specified call against this Java method.
	 * @throws Throwable Typically an HTTP exception.  Anything else will result in an HTTP 500.
	 */
	protected void invoke(RestCall call) throws Throwable {

		UrlPathMatch pm = call.getUrlPathMatch();
		if (pm == null)
			pm = matchPattern(call);

		if (pm == null)
			throw new NotFound();

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		RequestPath rp = req.getPathMatch();
		for (Map.Entry<String,String> e : pm.getVars().entrySet())
			rp.put(e.getKey(), e.getValue());
		if (pm.getRemainder() != null)
			rp.remainder(pm.getRemainder());

		req.init(this);
		res.init(this);

		context.preCall(call);

		call.logger(callLogger);

		if (debug == ALWAYS) {
			call.debug(true);
		} else if (debug == NEVER) {
			call.debug(false);
		} else if (debug == CONDITIONAL) {
			boolean b = "true".equalsIgnoreCase(req.getHeader("X-Debug"));
			if (b) {
				call.debug(true);
			} else {
				call.debug(false);
			}
		}

		Object[] args = new Object[methodParams.length];
		for (int i = 0; i < methodParams.length; i++) {
			ParamInfo pi = methodInvoker.inner().getParam(i);
			try {
				args[i] = methodParams[i].resolve(call);
			} catch (Exception e) {
				throw toHttpException(e, BadRequest.class, "Could not convert resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), mi.getFullName());
			}
		}

		try {

			for (RestGuard guard : guards)
				if (! guard.guard(req, res))
					return;

			Object output;
			try {
				output = methodInvoker.invoke(context.getResource(), args);

				// Handle manual call to req.setDebug().
				Boolean debug = ObjectUtils.castOrNull(req.getAttribute("Debug"), Boolean.class);
				if (debug == Boolean.TRUE) {
					call.debug(true);
				} else if (debug == Boolean.FALSE) {
					call.debug(false);
				}

				if (res.getStatus() == 0)
					res.setStatus(200);
				if (! method.getReturnType().equals(Void.TYPE)) {
					if (output != null || ! res.getOutputStreamCalled())
						res.setOutput(output);
				}
			} catch (ExecutableException e) {
				Throwable e2 = e.unwrap();		// Get the throwable thrown from the doX() method.
				res.setStatus(500);
				ResponsePartMeta rpm = getResponseBodyMeta(e2);
				ResponseBeanMeta rbm = getResponseBeanMeta(e2);
				if (rpm != null || rbm != null) {
					res.setOutput(e2);
					res.setResponseMeta(rbm);
				} else {
					throw e;
				}
			}

			context.postCall(call);

			if (res.hasOutput())
				for (RestConverter converter : converters)
					res.setOutput(converter.convert(req, res.getOutput()));

		} catch (IllegalArgumentException e) {
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				mi.toString(), mi.getFullName()
			);
		} catch (ExecutableException e) {
			throw e.unwrap();
		}
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestMethodContext o) {
		int c;

		c = priority.compareTo(o.priority);
		if (c != 0)
			return c;

		for (int i = 0; i < Math.min(pathMatchers.length, o.pathMatchers.length); i++) {
			c = pathMatchers[i].compareTo(o.pathMatchers[i]);
			if (c != 0)
				return c;
		}

		c = compare(o.hierarchyDepth, hierarchyDepth);
		if (c != 0)
			return c;

		c = compare(o.requiredMatchers.length, requiredMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.optionalMatchers.length, optionalMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.guards.length, guards.length);
		if (c != 0)
			return c;

		c = compare(method.getName(), o.method.getName());
		if (c != 0)
			return c;

		c = compare(method.getParameterCount(), o.method.getParameterCount());
		if (c != 0)
			return c;

		for (int i = 0; i < method.getParameterCount(); i++) {
			c = compare(method.getParameterTypes()[i].getName(), o.method.getParameterTypes()[i].getName());
			if (c != 0)
				return c;
		}

		c = compare(method.getReturnType().getName(), o.method.getReturnType().getName());
		if (c != 0)
			return c;

		return 0;
	}

	/**
	 * Bean property getter:  <property>serializers</property>.
	 *
	 * @return The value of the <property>serializers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Bean property getter:  <property>parsers</property>.
	 *
	 * @return The value of the <property>parsers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the JSON-Schema generator applicable to this Java method.
	 *
	 * @return The JSON-Schema generator applicable to this Java method.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	Enablement getDebug() {
		return debug;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof RestMethodContext) && eq(this, (RestMethodContext)o, (x,y)->x.method.equals(y.method));
	}

	@Override /* Object */
	public int hashCode() {
		return method.hashCode();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//-----------------------------------------------------------------------------------------------------------------

	static String[] resolveVars(VarResolver vr, String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = vr.resolve(in[i]);
		return out;
	}

	static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, PropertyStore ps, HttpPartSerializer _default) {
		HttpPartSerializer hps = castOrCreate(HttpPartSerializer.class, c, true, ps);
		return hps == null ? _default : hps;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("RestMethodContext", new DefaultFilteringOMap()
				.a("defaultRequestFormData", defaultRequestFormData)
				.a("defaultRequestHeaders", defaultRequestHeaders)
				.a("defaultRequestQuery", defaultRequestQuery)
				.a("httpMethod", httpMethod)
				.a("priority", priority)
			);
	}
}
