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
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static java.util.Collections.*;
import static org.apache.juneau.http.header.StandardHttpHeaders.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import javax.servlet.*;
import javax.servlet.http.*;

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
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.guards.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestOp @RestOp}.
 */
@ConfigurableContext(nocache=true)
public class RestOperationContext extends BeanContext implements Comparable<RestOperationContext>  {

	/** Represents a null value for the {@link RestOp#contextClass()} annotation.*/
	@SuppressWarnings("javadoc")
	public static final class Null extends RestOperationContext {
		public Null(RestOperationContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestOperationContext";

	/**
	 * Configuration property:  Client version pattern matcher.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_clientVersion RESTMETHOD_clientVersion}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.clientVersion.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.clientVersion</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_CLIENTVERSION</c>
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#clientVersion()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#clientVersion(String)}
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
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
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
	public static final String RESTOP_clientVersion = PREFIX + ".clientVersion.s";

	/**
	 * Configuration property:  REST method context class.
	 *
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_contextClass RESTMETHOD_contextClass}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.contextClass.c"</js>
	 * 	<li><b>Data type:</b>  <c>Class&lt;? extends {@link org.apache.juneau.rest.RestOperationContext}&gt;</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.RestOperationContext}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#restOperationContextClass()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#contextClass()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#restOperationContextClass(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#contextClass(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to extend the {@link RestOperationContext} class to modify how any of the functions are implemented.
	 *
	 * <p>
	 * The subclass must have a public constructor that takes in any of the following arguments:
	 * <ul>
	 * 	<li>{@link RestOperationContextBuilder} - The builder for the object.
	 * 	<li>Any beans found in the specified {@link #REST_beanStore bean store}.
	 * 	<li>Any {@link Optional} beans that may or may not be found in the specified {@link #REST_beanStore bean store}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our extended context class that adds a request attribute to all requests.</jc>
	 * 	<jc>// The attribute value is provided by an injected spring bean.</jc>
	 * 	<jk>public</jk> MyRestOperationContext <jk>extends</jk> RestOperationContext {
	 *
	 * 		<jk>private final</jk> Optional&lt;? <jk>extends</jk> Supplier&lt;Object&gt;&gt; <jf>fooSupplier</jf>;
	 *
	 * 		<jc>// Constructor that takes in builder and optional injected attribute provider.</jc>
	 * 		<jk>public</jk> MyRestOperationContext(RestOperationContextBuilder <jv>builder</jv>, Optional&lt;AnInjectedFooSupplier&gt; <jv>fooSupplier</jv>) {
	 * 			<jk>super</jk>(<jv>builder</jv>);
	 * 			<jk>this</jk>.<jf>fooSupplier</jf> = <jv>fooSupplier</jv>.orElseGet(()-><jk>null</jk>);
	 * 		}
	 *
	 * 		<jc>// Override the method used to create default request attributes.</jc>
	 * 		<ja>@Override</ja>
	 * 		<jk>protected</jk> NamedAttributeList createDefaultRequestAttributes(Object <jv>resource</jv>, BeanStore <jv>beanStore</jv>, Method <jv>method</jv>, RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return super</jk>
	 * 				.createDefaultRequestAttributes(<jv>resource</jv>, <jv>beanStore</jv>, <jv>method</jv>, <jv>context</jv>)
	 * 				.append(NamedAttribute.<jsm>of</jsm>(<js>"foo"</js>, ()-><jf>fooSupplier</jf>.get());
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 		<ja>@RestGet</ja>(contextClass=MyRestOperationContext.<jk>class</jk>)
	 * 		<jk>public</jk> Object foo(RequestAttributes <jv>attributes</jv>) {
	 * 			<jk>return</jk> <jv>attributes</jv>.get(<js>"foo"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String RESTOP_contextClass = PREFIX + ".contextClass.c";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_debug RESTMETHOD_debug}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Enablement}
	 * 	<li><b>System property:</b>  <c>RestOperationContext.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#debug(Enablement)}
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
	public static final String RESTOP_debug = PREFIX + ".debug.s";

	/**
	 * Configuration property:  Default form data.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_defaultFormData RESTMETHOD_defaultFormData}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.defaultFormData.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NameValuePair}[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.defaultFormData</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEFAULTFORMDATA</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultFormData()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultFormData(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultFormData(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultFormData(NameValuePair...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for form-data parameters.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getFormParam(String)} when the parameter is not present on the
	 * request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>
	 */
	public static final String RESTOP_defaultFormData = PREFIX + ".defaultFormData.lo";

	/**
	 * Configuration property:  Default query parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_defaultQuery RESTMETHOD_defaultQuery}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.defaultQuery.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NameValuePair}[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.defaultQuery</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEFAULTQUERY</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultQuery()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultQuery(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultQuery(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultQuery(NameValuePair...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for query parameters.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQueryParam(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultQuery={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>
	 */
	public static final String RESTOP_defaultQuery = PREFIX + ".defaultQuery.lo";

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_defaultRequestAttributes RESTMETHOD_defaultRequestAttributes}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.reqAttrs.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link NamedAttribute}[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.defaultRequestAttributes</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEFAULTREQUESTATTRIBUTES</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultRequestAttributes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestAttribute(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestAttribute(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestAttributes(NamedAttribute...)}
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
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultRequestAttributes={<js>"Foo=bar"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestAttributes}
	 * </ul>
	 */
	public static final String RESTOP_defaultRequestAttributes = PREFIX + ".defaultRequestAttributes.lo";

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_defaultRequestHeaders RESTMETHOD_defaultRequestHeaders}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.defaultRequestHeaders.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.http.Header}[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.defaultRequestHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEFAULTREQUESTHEADERS</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultRequestHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultAccept()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultContentType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestHeader(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultRequestHeaders(org.apache.http.Header...)}
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
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 */
	public static final String RESTOP_defaultRequestHeaders = PREFIX + ".defaultRequestHeaders.lo";

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_defaultResponseHeaders RESTMETHOD_defaultResponseHeaders}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.defaultResponseHeaders.lo"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.http.Header}[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.defaultResponseHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_DEFAULTRESPONSEHEADERS</c>
	 * 	<li><b>Default:</b>  empty list.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultRequestHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultAccept()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#defaultContentType()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultResponseHeader(String,Supplier)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#defaultResponseHeaders(org.apache.http.Header...)}
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
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultResponseHeaders={<js>"Content-Type: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 */
	public static final String RESTOP_defaultResponseHeaders = PREFIX + ".defaultResponseHeaders.lo";

	/**
	 * Configuration property:  HTTP method name.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_httpMethod RESTMETHOD_httpMethod}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.httpMethod.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.httpMethod</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_HTTPMETHOD</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#method()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#httpMethod(String)}
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
	public static final String RESTOP_httpMethod = PREFIX + ".httpMethod.s";

	/**
	 * Configuration property:  Method-level matchers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_matchers RESTMETHOD_matchers}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.matchers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestMatcher}|Class&lt;{@link org.apache.juneau.rest.RestMatcher}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#matchers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#matchers(RestMatcher...)}
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
	public static final String RESTOP_matchers = PREFIX + ".matchers.lo";

	/**
	 * Configuration property:  Resource method paths.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestOperationContext#RESTOP_path RESTMETHOD_path}
	 * 	<li><b>Name:</b>  <js>"RestOperationContext.path.ls"</js>
	 * 	<li><b>Data type:</b>  <c>String[]</c>
	 * 	<li><b>System property:</b>  <c>RestOperationContext.path</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTOPERATIONCONTEXT_PATH</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestOp#path()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestOperationContextBuilder#path(String...)}
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
	public static final String RESTOP_path = PREFIX + ".path.ls";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String httpMethod;
	private final UrlPathMatcher[] pathMatchers;
	private final RestOperationArg[] opArgs;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final RestContext context;
	private final Method method;
	private final MethodInvoker methodInvoker;
	private final MethodInfo mi;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final EncoderGroup encoders;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final JsonSchemaGenerator jsonSchemaGenerator;
	private final HeaderGroup defaultRequestHeaders, defaultResponseHeaders;
	private final List<NameValuePair> defaultRequestQuery, defaultRequestFormData;
	private final List<NamedAttribute> defaultRequestAttributes;
	private final Charset defaultCharset;
	private final long maxInput;
	private final List<MediaType>
		supportedAcceptTypes,
		supportedContentTypes;
	private final RestLogger callLogger;

	private final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	private final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	private final Map<Class<?>,ResponsePartMeta> bodyPartMetas = new ConcurrentHashMap<>();
	private final ResponseBeanMeta responseMeta;
	private final int hierarchyDepth;

	/**
	 * Creator.
	 *
	 * @param method The Java method this context belongs to.
	 * @param context The Java class context.
	 * @return A new builder.
	 */
	public static RestOperationContextBuilder create(java.lang.reflect.Method method, RestContext context) {
		return new RestOperationContextBuilder(method, context);
	}

	/**
	 * Context constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws ServletException If context could not be created.
	 */
	public RestOperationContext(RestOperationContextBuilder builder) throws ServletException {
		super(builder.getContextProperties());

		try {
			context = builder.restContext;
			method = builder.restMethod;

			ContextProperties cp = getContextProperties();

			methodInvoker = new MethodInvoker(method, context.getMethodExecStats(method));
			mi = MethodInfo.of(method).accessible();
			Object r = context.getResource();

			BeanStore bs = BeanStore.of(context.getRootBeanStore(), r)
				.addBean(RestOperationContext.class, this)
				.addBean(Method.class, method)
				.addBean(ContextProperties.class, cp);
			bs.addBean(BeanStore.class, bs);

			serializers = createSerializers(r, cp, bs);
			bs.addBean(SerializerGroup.class, serializers);

			parsers = createParsers(r, cp, bs);
			bs.addBean(ParserGroup.class, parsers);

			partSerializer = createPartSerializer(r, cp, bs);
			bs.addBean(HttpPartSerializer.class, partSerializer);

			partParser = createPartParser(r, cp, bs);
			bs.addBean(HttpPartParser.class, partParser);

			converters = createConverters(r, cp, bs).asArray();
			bs.addBean(RestConverter[].class, converters);

			guards = createGuards(r, cp, bs).asArray();
			bs.addBean(RestGuard[].class, guards);

			RestMatcherList matchers = createMatchers(r, cp, bs);
 			requiredMatchers = matchers.stream().filter(x -> x.required()).toArray(RestMatcher[]::new);
			optionalMatchers = matchers.stream().filter(x -> ! x.required()).toArray(RestMatcher[]::new);

			pathMatchers = createPathMatchers(r, cp, bs).asArray();
			bs.addBean(UrlPathMatcher[].class, pathMatchers);
			bs.addBean(UrlPathMatcher.class, pathMatchers.length > 0 ? pathMatchers[0] : null);

			encoders = createEncoders(r, cp, bs);
			bs.addBean(EncoderGroup.class, encoders);

			jsonSchemaGenerator = createJsonSchemaGenerator(r, cp, bs);
			bs.addBean(JsonSchemaGenerator.class, jsonSchemaGenerator);

			supportedAcceptTypes = unmodifiableList(cp.getList(REST_produces, MediaType.class).orElse(serializers.getSupportedMediaTypes()));
			supportedContentTypes = unmodifiableList(cp.getList(REST_consumes, MediaType.class).orElse(parsers.getSupportedMediaTypes()));

			defaultRequestHeaders = createDefaultRequestHeaders(r, cp, bs, method, context).build();
			defaultResponseHeaders = createDefaultResponseHeaders(r, cp, bs, method, context).build();
			defaultRequestQuery = unmodifiableList(createDefaultRequestQuery(r, cp, bs, method));
			defaultRequestFormData = unmodifiableList(createDefaultRequestFormData(r, cp, bs, method));
			defaultRequestAttributes = unmodifiableList(createDefaultRequestAttributes(r, cp, bs, method, context));

			int _hierarchyDepth = 0;
			Class<?> sc = method.getDeclaringClass().getSuperclass();
			while (sc != null) {
				_hierarchyDepth++;
				sc = sc.getSuperclass();
			}
			hierarchyDepth = _hierarchyDepth;

			String _httpMethod = cp.get(RESTOP_httpMethod, String.class).orElse(null);
			if (_httpMethod == null)
				_httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(_httpMethod))
				_httpMethod = "*";
			httpMethod = _httpMethod.toUpperCase(Locale.ENGLISH);

			defaultCharset = Charset.forName(cp.getString(REST_defaultCharset).orElse("utf-8"));

			maxInput = StringUtils.parseLongWithSuffix(cp.get(REST_maxInput, String.class).orElse("100M"));

			responseMeta = ResponseBeanMeta.create(mi, cp);

			opArgs = context.findRestOperationArgs(mi.inner(), bs);

			this.callLogger = context.getCallLogger();
		} catch (ServletException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
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
	 * 			<li>{@link RestOp#converters()}.
	 * 			<li>{@link Rest#converters()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createConverters()</> method that returns <c>{@link RestConverter}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>RestConverter[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The result converters for this REST resource method.
	 * @throws Exception If result converters could not be instantiated.
	 * @seealso #REST_converters
	 */
	protected RestConverterList createConverters(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		RestConverterList x = RestConverterList.create();

		x.append(properties.getInstanceArray(REST_converters, RestConverter.class, beanStore).orElse(new RestConverter[0]));

		if (x.isEmpty())
			x = beanStore.getBean(RestConverterList.class).orElse(x);

		x = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link RestOp#guards()}.
	 * 			<li>{@link Rest#guards()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createGuards()</> method that returns <c>{@link RestGuard}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>RestGuard[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The guards for this REST resource method.
	 * @throws Exception If guards could not be instantiated.
	 * @seealso #REST_guards
	 */
	protected RestGuardList createGuards(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		RestGuardList x = RestGuardList.create();

		x.append(properties.getInstanceArray(REST_guards, RestGuard.class, beanStore).orElse(new RestGuard[0]));

		if (x.isEmpty())
			x = beanStore.getBean(RestGuardList.class).orElse(x);

		Set<String> rolesDeclared = properties.getSet(REST_rolesDeclared, String.class).orElse(null);
		Set<String> roleGuard = properties.getSet(REST_roleGuard, String.class).orElse(Collections.emptySet());

		for (String rg : roleGuard) {
			try {
				x.add(new RoleBasedRestGuard(rolesDeclared, rg));
			} catch (java.text.ParseException e1) {
				throw new ServletException(e1);
			}
		}

		x = BeanStore
			.of(beanStore, resource)
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
	 * 	<li>Looks for {@link #RESTOP_matchers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestOp#matchers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createMatchers()</> method that returns <c>{@link RestMatcher}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link java.lang.reflect.Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>RestMatcher[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The method matchers for this REST resource method.
	 * @throws Exception If method matchers could not be instantiated.
	 * @seealso #RESTMETHOD_matchers
	 */
	protected RestMatcherList createMatchers(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		RestMatcherList x = RestMatcherList.create();

		x.append(properties.getInstanceArray(RESTOP_matchers, RestMatcher.class, beanStore).orElse(new RestMatcher[0]));

		if (x.isEmpty())
			x = beanStore.getBean(RestMatcherList.class).orElse(x);

		String clientVersion = properties.get(RESTOP_clientVersion, String.class).orElse(null);
		if (clientVersion != null)
			x.add(new ClientVersionMatcher(context.getClientVersionHeader(), mi));

		x = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link RestOp#encoders()}.
	 * 			<li>{@link Rest#encoders()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createEncoders()</> method that returns <c>{@link Encoder}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>Encoder[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The encoders for this REST resource method.
	 * @throws Exception If encoders could not be instantiated.
	 * @seealso #REST_encoders
	 */
	protected EncoderGroup createEncoders(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		Encoder[] x = properties.getInstanceArray(REST_encoders, Encoder.class, beanStore).orElse(null);

		if (x == null)
			x = beanStore.getBean(Encoder[].class).orElse(null);

		if (x == null)
			x = new Encoder[0];

		EncoderGroup g = EncoderGroup
			.create()
			.append(IdentityEncoder.INSTANCE)
			.append(x)
			.build();

		g = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>Serializer[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties The property store of this method.
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The serializers for this REST resource.
	 * @throws Exception If serializers could not be instantiated.
	 * @seealso #REST_serializers
	 */
	protected SerializerGroup createSerializers(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		SerializerGroup g = beanStore.getBean(SerializerGroup.class).orElse(null);

		if (g == null) {
			Object[] x = properties.getArray(REST_serializers, Object.class).orElse(null);

			if (x == null)
				x = beanStore.getBean(Serializer[].class).orElse(null);

			if (x == null)
				x = new Serializer[0];

			g = SerializerGroup
				.create()
				.append(x)
				.apply(properties)
				.build();
		}

		g = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>Parser[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties The property store of this method.
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The parsers for this REST resource.
	 * @throws Exception If parsers could not be instantiated.
	 * @seealso #REST_parsers
	 */
	protected ParserGroup createParsers(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		ParserGroup g = beanStore.getBean(ParserGroup.class).orElse(null);

		if (g == null) {
			Object[] x = properties.getArray(REST_parsers, Object.class).orElse(null);

			if (x == null)
				x = beanStore.getBean(Parser[].class).orElse(null);

			if (x == null)
				x = new Parser[0];

			g = ParserGroup
				.create()
				.append(x)
				.apply(properties)
				.build();
		}

		g = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties The property store of this method.
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The HTTP part serializer for this REST resource.
	 * @throws Exception If serializer could not be instantiated.
	 * @seealso #REST_partSerializer
	 */
	protected HttpPartSerializer createPartSerializer(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		HttpPartSerializer x = null;

		if (resource instanceof HttpPartSerializer)
			x = (HttpPartSerializer)resource;

		if (x == null)
			x = properties.getInstance(REST_partSerializer, HttpPartSerializer.class, beanStore).orElse(null);

		if (x == null)
			x = beanStore.getBean(HttpPartSerializer.class).orElse(null);

		if (x == null)
			x = new OpenApiSerializer(properties);

		x = BeanStore
			.of(beanStore, resource)
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
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param properties The property store of this method.
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 * @seealso #REST_partParser
	 */
	protected HttpPartParser createPartParser(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		HttpPartParser x = null;

		if (resource instanceof HttpPartParser)
			x = (HttpPartParser)resource;

		if (x == null)
			x = properties.getInstance(REST_partParser, HttpPartParser.class, beanStore).orElse(null);

		if (x == null)
			x = beanStore.getBean(HttpPartParser.class).orElse(null);

		if (x == null)
			x = new OpenApiParser(properties);

		x = BeanStore
			.of(beanStore, resource)
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
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 * @seealso #RESTMETHOD_paths
	 */
	protected UrlPathMatcherList createPathMatchers(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		UrlPathMatcherList x = UrlPathMatcherList.create();
		boolean dotAll = properties.getBoolean("RestOperationContext.dotAll.b").orElse(false);

		for (String p : properties.getArray(RESTOP_path, String.class).orElse(new String[0])) {
			if (dotAll && ! p.endsWith("/*"))
				p += "/*";
			x.add(UrlPathMatcher.of(p));
		}

		if (x.isEmpty()) {
			MethodInfo mi = MethodInfo.of(method);
			String p = null;
			String httpMethod = null;
			if (mi.hasAnnotation(RestGet.class))
				httpMethod = "get";
			else if (mi.hasAnnotation(RestPut.class))
				httpMethod = "put";
			else if (mi.hasAnnotation(RestPost.class))
				httpMethod = "post";
			else if (mi.hasAnnotation(RestDelete.class))
				httpMethod = "delete";
			else if (mi.hasAnnotation(RestOp.class))
				httpMethod = mi.getAnnotations(RestOp.class).stream().map(y -> y.method()).filter(y -> ! y.isEmpty()).findFirst().orElse(null);

			p = HttpUtils.detectHttpPath(method, httpMethod);

			if (dotAll && ! p.endsWith("/*"))
				p += "/*";

			x.add(UrlPathMatcher.of(p));
		}

		x = BeanStore
			.of(beanStore, resource)
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
	 * @param properties The property store of this method.
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @return The JSON-schema generator for this method.
	 * @throws Exception If schema generator could not be instantiated.
	 */
	protected JsonSchemaGenerator createJsonSchemaGenerator(Object resource, ContextProperties properties, BeanStore beanStore) throws Exception {

		JsonSchemaGenerator x = null;

		if (resource instanceof JsonSchemaGenerator)
			x = (JsonSchemaGenerator)resource;

		if (x == null)
			x = beanStore.getBean(JsonSchemaGenerator.class).orElse(null);

		if (x == null)
			x = JsonSchemaGenerator.create().apply(properties).build();

		x = BeanStore
			.of(beanStore, resource)
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
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request headers for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected HeaderGroupBuilder createDefaultRequestHeaders(Object resource, ContextProperties properties, BeanStore beanStore, Method method, RestContext context) throws Exception {

		HeaderGroupBuilder x = HeaderGroup.create();

		x.appendUnique(context.getDefaultRequestHeaders().getAllHeaders());

		x.appendUnique(properties.getInstanceArray(RESTOP_defaultRequestHeaders, org.apache.http.Header.class, beanStore).orElse(new org.apache.http.Header[0]));

		for (Annotation[] aa : method.getParameterAnnotations()) {
			for (Annotation a : aa) {
				if (a instanceof Header) {
					Header h = (Header)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							x.appendUnique(basicHeader(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Header annotation");
						}
					}
				}
			}
		}

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HeaderGroupBuilder.class, x)
			.beanCreateMethodFinder(HeaderGroupBuilder.class, resource)
			.find("createDefaultRequestHeaders", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request headers for this method.
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request headers for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected HeaderGroupBuilder createDefaultResponseHeaders(Object resource, ContextProperties properties, BeanStore beanStore, Method method, RestContext context) throws Exception {

		HeaderGroupBuilder x = HeaderGroup.create();

		x.appendUnique(context.getDefaultResponseHeaders().getAllHeaders());

		x.appendUnique(properties.getInstanceArray(RESTOP_defaultResponseHeaders, org.apache.http.Header.class, beanStore).orElse(new org.apache.http.Header[0]));

		x = BeanStore
			.of(beanStore, resource)
			.addBean(HeaderGroupBuilder.class, x)
			.beanCreateMethodFinder(HeaderGroupBuilder.class, resource)
			.find("createDefaultResponseHeaders", Method.class)
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Instantiates the default request attributes for this method.
	 *
	 * @param resource The REST resource object.
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @param context The REST class context.
	 * @return The default request attributes for this method.
	 * @throws Exception If default request headers could not be instantiated.
	 */
	protected NamedAttributeList createDefaultRequestAttributes(Object resource, ContextProperties properties, BeanStore beanStore, Method method, RestContext context) throws Exception {
		NamedAttributeList x = NamedAttributeList.create();

		x.appendUnique(context.getDefaultRequestAttributes());

		x.appendUnique(properties.getInstanceArray(RESTOP_defaultRequestAttributes, NamedAttribute.class, beanStore).orElse(new NamedAttribute[0]));

		x = BeanStore
			.of(beanStore, resource)
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
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @return The default request query parameters for this method.
	 * @throws Exception If default request query parameters could not be instantiated.
	 */
	protected NameValuePairList createDefaultRequestQuery(Object resource, ContextProperties properties, BeanStore beanStore, Method method) throws Exception {

		NameValuePairList x = NameValuePairList.create();

		x.appendUnique(properties.getInstanceArray(RESTOP_defaultQuery, NameValuePair.class, beanStore).orElse(new NameValuePair[0]));

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

		x = BeanStore
			.of(beanStore, resource)
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
	 * @param properties xxx
	 * @param beanStore The bean store to use for retrieving and creating beans.
	 * @param method This Java method.
	 * @return The default request form-data parameters for this method.
	 * @throws Exception If default request form-data parameters could not be instantiated.
	 */
	protected NameValuePairList createDefaultRequestFormData(Object resource, ContextProperties properties, BeanStore beanStore, Method method) throws Exception {

		NameValuePairList x = NameValuePairList.create();

		x.appendUnique(properties.getInstanceArray(RESTOP_defaultFormData, NameValuePair.class, beanStore).orElse(new NameValuePair[0]));

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

		x = BeanStore
			.of(beanStore, resource)
			.addBean(NameValuePairList.class, x)
			.beanCreateMethodFinder(NameValuePairList.class, resource)
			.find("createDefaultRequestFormData", Method.class)
			.thenFind("createDefaultRequestFormData")
			.withDefault(x)
			.run();

		return x;
	}

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link Response @Response}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link Response @Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, serializers.getContextProperties());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link ResponseHeader @ResponseHeader}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link ResponseHeader @ResponseHeader}.
	 */
	public ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = headerPartMetas.get(c);
		if (pm == null) {
			ResponseHeader a = c.getAnnotation(ResponseHeader.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getContextProperties(), partSerializer);
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

	/**
	 * Returns metadata about the specified response object if it's annotated with {@link ResponseBody @ResponseBody}.
	 *
 	 * @param o The response POJO.
	 * @return Metadata about the specified response object, or <jk>null</jk> if it's not annotated with {@link ResponseBody @ResponseBody}.
	 */
	public ResponsePartMeta getResponseBodyMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = bodyPartMetas.get(c);
		if (pm == null) {
			ResponseBody a = c.getAnnotation(ResponseBody.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getContextProperties(), partSerializer);
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
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 *
	 * @return The HTTP method name.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 *
	 * @return The path pattern.
	 */
	public String getPathPattern() {
		return pathMatchers[0].toString();
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
	 * Bean property getter:  <property>encoders</property>.
	 *
	 * @return The value of the <property>encoders</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public EncoderGroup getEncoders() {
		return encoders;
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

	/**
	 * Returns the underlying Java method that this context belongs to.
	 *
	 * @return The underlying Java method that this context belongs to.
	 */
	public Method getJavaMethod() {
		return method;
	}

	/**
	 * Returns the default request headers.
	 *
	 * @return The default request headers.  Never <jk>null</jk>.
	 */
	public HeaderGroup getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default response headers.
	 *
	 * @return The default response headers.  Never <jk>null</jk>.
	 */
	public HeaderGroup getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the default request query parameters.
	 *
	 * @return The default request query parameters.  Never <jk>null</jk>.
	 */
	public List<NameValuePair> getDefaultRequestQuery() {
		return defaultRequestQuery;
	}

	/**
	 * Returns the default form data parameters.
	 *
	 * @return The default form data parameters.  Never <jk>null</jk>.
	 */
	public List<NameValuePair> getDefaultRequestFormData() {
		return defaultRequestFormData;
	}

	/**
	 * Returns the default request attributes.
	 *
	 * @return The default request attributes.  Never <jk>null</jk>.
	 */
	public List<NamedAttribute> getDefaultRequestAttributes() {
		return defaultRequestAttributes;
	}

	/**
	 * Returns the default charset.
	 *
	 * @return The default charset.  Never <jk>null</jk>.
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Returns the max number of bytes to process in the input body.
	 *
	 * @return The max number of bytes to process in the input body.
	 */
	public long getMaxInput() {
		return maxInput;
	}

	/**
	 * Returns the list of supported content types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/**
	 * Returns a list of supported accept types.
	 *
	 * @return An unmodifiable list.
	 */
	public List<MediaType> getSupportedAcceptTypes() {
		return supportedAcceptTypes;
	}

	/**
	 * Returns the response bean meta if this method returns a {@link Response}-annotated bean.
	 *
	 * @return The response bean meta or <jk>null</jk> if it's not a {@link Response}-annotated bean.
	 */
	public ResponseBeanMeta getResponseMeta() {
		return responseMeta;
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
			HttpServletRequest req = call.getRequest();

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

	/**
	 * Workhorse method.
	 *
	 * @param call Invokes the specified call against this Java method.
	 * @throws Throwable Typically an HTTP exception.  Anything else will result in an HTTP 500.
	 */
	protected void invoke(RestCall call) throws Throwable {

		call.restOperationContext(this);

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		context.preCall(call);

		call.logger(callLogger);

		call.debug(context.getDebugEnablement().isDebug(this, call.getRequest()));

		Object[] args = new Object[opArgs.length];
		for (int i = 0; i < opArgs.length; i++) {
			ParamInfo pi = methodInvoker.inner().getParam(i);
			try {
				args[i] = opArgs[i].resolve(call);
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
				Boolean debug = req.getAttribute("Debug").asType(Boolean.class).orElse(null);
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

			Optional<Optional<Object>> o = res.getOutput();
			if (o.isPresent())
				for (RestConverter converter : converters)
					res.setOutput(converter.convert(req, o.get().orElse(null)));

		} catch (IllegalArgumentException e) {
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				mi.toString(), mi.getFullName()
			);
		} catch (ExecutableException e) {
			throw e.unwrap();
		}
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestOperationContext o) {
		int c;

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

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof RestOperationContext) && eq(this, (RestOperationContext)o, (x,y)->x.method.equals(y.method));
	}

	@Override /* Object */
	public int hashCode() {
		return method.hashCode();
	}
	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RestOperationContext",
				OMap
				.create()
				.filtered()
				.a("defaultRequestFormData", defaultRequestFormData)
				.a("defaultRequestHeaders", defaultRequestHeaders)
				.a("defaultRequestQuery", defaultRequestQuery)
				.a("httpMethod", httpMethod)
			);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, ContextProperties cp, HttpPartSerializer _default) {
		HttpPartSerializer hps = castOrCreate(HttpPartSerializer.class, c, true, cp);
		return hps == null ? _default : hps;
	}

	private String joinnlFirstNonEmptyArray(String[]...s) {
		for (String[] ss : s)
			if (ss.length > 0)
				return joinnl(ss);
		return null;
	}

	private UrlPathMatch matchPattern(RestCall call) {
		UrlPathMatch pm = null;
		for (UrlPathMatcher pp : pathMatchers)
			if (pm == null)
				pm = pp.match(call.getUrlPath());
		return pm;
	}
}
