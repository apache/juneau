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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;

/**
 * Annotation applied to Java methods on interface proxy classes.
 *
 * <p>
 * The return type on the Java method can be any of the following:
 * <ul>
 * 	<li><jk>void</jk> - Don't parse any response.  Note that the method will still throw an exception if an error
 * 		HTTP status is returned.
 * 	<li>Any parsable POJO - The body of the response will be converted to the POJO using the parser defined on the
 * 		<code>RestClient</code>.
 * 	<li><code>HttpResponse</code> - Returns the raw <code>HttpResponse</code> returned by the inner
 * 		<code>HttpClient</code>.
 * 	<li>{@link Reader} - Returns access to the raw reader of the response.
 * 	<li>{@link InputStream} - Returns access to the raw input stream of the response.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface RemoteMethod {

	/**
	 * The path to the REST service for this Java method relative to the parent proxy interface URL.
	 *
	 * <p>
	 * The default value is the Java method name (e.g. <js>"http://localhost/root-url/org.foo.MyInterface/myMethod"</js>)
	 * if {@link Remoteable#methodPaths() @Remoteable.methodPaths()} is <js>"NAME"</js>, or the Java method signature
	 * (e.g. <js>"http://localhost/root-url/org.foo.MyInterface/myMethod(int,boolean,java.lang.String)"</js>) if
	 * it's <js>"SIGNATURE"</js>.
	 */
	String path() default "";

	/**
	 * Defines the HTTP method to use for REST calls.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><jsf>POST</jsf> (default) - Parameters are serialized using the serializer registered with the RestClient.
	 * 	<li><jsf>GET</jsf> - Parameters are serialized using the UrlEncodingSerializer registered with the RestClient.
	 * </ul>
	 *
	 * <p>
	 * The default value is <js>"POST"</js>.
	 */
	String httpMethod() default "POST";

	/**
	 * The value the remoteable method returns.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>
	 * 		{@link ReturnValue#BODY} (default) - The body of the HTTP response converted to a POJO.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul>
	 * 			<li><jk>void</jk> - Don't parse any response.  Note that the method will still throw an exception if an
	 * 					error HTTP status is returned.
	 * 			<li>Any parsable POJO - The body of the response will be converted to the POJO using the parser defined
	 * 					on the <code>RestClient</code>.
	 * 			<li><code>HttpResponse</code> - Returns the raw <code>HttpResponse</code> returned by the inner
	 * 					<code>HttpClient</code>.
	 * 			<li>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 		</ul>
	 * 	<li>
	 * 		{@link ReturnValue#HTTP_STATUS} - The HTTP status code on the response.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul>
	 * 			<li><jk>int</jk>/<code>Integer</code> - The HTTP response code.
	 * 			<li><jk>boolean</jk>/<code>Boolean</code> - <jk>true</jk> if the response code is <code>&lt;400</code>
	 * 		</ul>
	 * </ul>
	 */
	ReturnValue returns() default ReturnValue.BODY;
}
