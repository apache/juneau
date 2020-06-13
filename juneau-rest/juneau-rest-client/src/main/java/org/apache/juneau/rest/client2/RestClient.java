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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.AddFlag.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.client2.RestClientUtils.*;
import static java.util.logging.Level.*;
import static org.apache.juneau.internal.StateMachineState.*;
import static java.lang.Character.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.URI;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.regex.*;
import java.util.stream.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.remote.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.http.client.CookieStore;

/**
 * Utility class for interfacing with remote REST interfaces.
 *
 * <p class='w900'>
 * Built upon the feature-rich Apache HttpClient library, the Juneau RestClient API adds support for fluent-style
 * REST calls and the ability to perform marshalling of POJOs to and from HTTP parts.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a basic REST client with JSON support and download a bean.</jc>
 * 	<jk>try</jk> (RestClient c = RestClient.<jsm>create</jsm>().build()) {
 * 		MyBean b = c.get(<jsf>URL</jsf>)
 * 			.run()
 * 			.assertStatus().is(200)
 * 			.getBody().as(MyBean.<jk>class</jk>);
 * 	}
 * </p>
 *
 * <p class='w900'>
 * Breaking apart the fluent call, we can see the classes being used:
 * <p class='bcode w800'>
 * 	<jc>// Create a basic REST client with JSON support and download a bean.</jc>
 * 	<jk>try</jk> (RestClient c = RestClient.<jsm>create</jsm>().build()) {
 * 		RestRequest req = c.get(<jsf>URL</jsf>);
 * 		RestResponse res = req.run();
 * 		res.assertStatus().is(200);
 * 		RestResponseBody body = res.getBody();
 * 		MyBean b = body.as(MyBean.<jk>class</jk>);
 * 	}
 * </p>
 *
 * <p class='w900'>
 * It additionally provides support for creating remote proxy interfaces using REST as the transport medium.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Define a Remote proxy for interacting with a REST interface.</jc>
 * 	<ja>@Remote</ja>(path=<js>"/petstore"</js>)
 * 	<jk>public interface</jk> PetStore {
 * 		<ja>@RemoteMethod</ja>(httpMethod=<jsf>POST</jsf>, path=<js>"/pets"</js>)
 * 		Pet addPet(
 * 			<ja>@Body</ja> CreatePet pet,
 * 			<ja>@Header</ja>(<js>"E-Tag"</js>) UUID etag,
 * 			<ja>@Query</ja>(<js>"debug"</js>) <jk>boolean</jk> debug
 * 		);
 * 	}
 *
 * 	<jc>// Use a RestClient with default Simple JSON support.</jc>
 * 	<jk>try</jk> (RestClient c = RestClient.<jsm>create</jsm>().simpleJson().build()) {
 * 		PetStore store = c.getRemote(PetStore.<jk>class</jk>, <js>"http://localhost:10000"</js>);
 * 		CreatePet cp = <jk>new</jk> CreatePet(<js>"Fluffy"</js>, 9.99);
 * 		Pet p = store.addPet(cp, UUID.<jsm>randomUUID</jsm>(), <jk>true</jk>);
 * 	}
 * </p>
 *
 * <p class='w900'>
 * The classes are closely tied to Apache HttpClient, yet provide lots of additional functionality:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient} <jk>extends</jk> {@link HttpClient}, creates {@link RestRequest} objects.
 * 	<li class='jc'>{@link RestRequest} <jk>extends</jk> {@link HttpUriRequest}, creates {@link RestResponse} objects.
 * 	<li class='jc'>{@link RestResponse} <jk>extends</jk> {@link HttpResponse}, creates {@link RestResponseBody} and {@link RestResponseHeader} objects.
 * 	<li class='jc'>{@link RestResponseBody} <jk>extends</jk> {@link HttpEntity}
 * 	<li class='jc'>{@link RestResponseHeader} <jk>extends</jk> {@link Header}
 * </ul>
 *
 *
 * <h4 class='topic'>Instantiation and Lifecycle</h4>
 *
 * <p class='w900'>
 * Instances of this class are built using the {@link RestClientBuilder} class which can be constructed using
 * the {@link #create() RestClient.create()} method as shown above.
 *
 * <p class='w900'>
 * Clients are typically created with a root URL so that relative URLs can be used when making requests.
 * This is done using the {@link RestClientBuilder#rootUrl(Object)} method.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a client where all URLs are relative to localhost.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().rootUrl(<js>"http://localhost:5000"</js>).build();
 *
 * 	<jc>// Use relative paths.</jc>
 * 	String body = c.get(<js>"/subpath"</js>).run().getBody().asString();
 * </p>
 *
 * <p class='w900'>
 * The {@link RestClient} class creates {@link RestRequest} objects using the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'>{@link RestClient#get(Object) get(Object url)}
 * 		<li class='jm'>{@link RestClient#put(Object,Object) put(Object url, Object body)}
 * 		<li class='jm'>{@link RestClient#put(Object) put(Object url)}
 * 		<li class='jm'>{@link RestClient#post(Object) post(Object url, Object body)}
 * 		<li class='jm'>{@link RestClient#post(Object) post(Object url)}
 * 		<li class='jm'>{@link RestClient#patch(Object,Object) patch(Object url, Object body)}
 * 		<li class='jm'>{@link RestClient#patch(Object) patch(Object url)}
 * 		<li class='jm'>{@link RestClient#delete(Object) delete(Object url)}
 * 		<li class='jm'>{@link RestClient#options(Object) options(Object url)}
 * 		<li class='jm'>{@link RestClient#formPost(Object,Object) formPost(Object url, Object body)}
 * 		<li class='jm'>{@link RestClient#formPost(Object) formPost(Object url)}
 * 		<li class='jm'>{@link RestClient#formPostPairs(Object,Object...) formPost(Object url, Object...parameters)}
 * 		<li class='jm'>{@link RestClient#request(HttpMethod,Object,Object) request(HttpMethod method, Object url, Object body)}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The {@link RestRequest} class creates {@link RestResponse} objects using the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#run() run()}
 * 		<li class='jm'>{@link RestRequest#complete() complete()}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The distinction between the two methods is that {@link RestRequest#complete() complete()} automatically consumes the response body and
 * {@link RestRequest#run() run()} does not.  Note that you must consume response bodies in order for HTTP connections to be freed up
 * for reuse!  The {@link InputStream InputStreams} returned by the {@link RestResponseBody} object are auto-closing once
 * they are exhausted, so it is often not necessary to explicitly close them.
 *
 * <p class='w900'>
 * The following examples show the distinction between the two calls:
 *
 * <p class='bcode w800'>
 * 	<jc>// Consuming the response, so use run().</jc>
 * 	String body = client.get(<jsf>URL</jsf>).run().getBody().asString();
 *
 * 	<jc>// Only interested in response status code, so use complete().</jc>
 *   <jk>int</jk> status = client.get(<jsf>URL</jsf>).complete().getStatusCode();
 * </p>
 *
 *
 * <h4 class='topic'>POJO Marshalling</h4>
 *
 * <p class='w900'>
 * By default, JSON support is provided for HTTP request and response bodies.
 * Other languages can be specified using any of the following builder methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#json() json()}
 * 		<li class='jm'>{@link RestClientBuilder#simpleJson() simpleJson()}
 * 		<li class='jm'>{@link RestClientBuilder#xml() xml()}
 * 		<li class='jm'>{@link RestClientBuilder#html() html()}
 * 		<li class='jm'>{@link RestClientBuilder#plainText() plainText()}
 * 		<li class='jm'>{@link RestClientBuilder#msgPack() msgPack()}
 * 		<li class='jm'>{@link RestClientBuilder#uon() uon()}
 * 		<li class='jm'>{@link RestClientBuilder#urlEnc() urlEnc()}
 * 		<li class='jm'>{@link RestClientBuilder#openApi() openApi()}
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a basic REST client with Simplified-JSON support.</jc>
 * 	<jc>// Typically easier to use when performing unit tests.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().simpleJson().build();
 * </p>
 *
 * <p class='w900'>
 * 	Other methods are also provided for specifying the serializers and parsers used for lower-level marshalling support:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#serializer(Serializer) serializer(Serializer)}
 * 		<li class='jm'>{@link RestClientBuilder#serializer(Class) serializer(Class&lt;? extends Serializer>)}
 * 		<li class='jm'>{@link RestClientBuilder#parser(Parser) parser(Parser)}
 * 		<li class='jm'>{@link RestClientBuilder#parser(Class) parser(Class&lt;? extends Parser>)}
 * 		<li class='jm'>{@link RestClientBuilder#marshall(Marshall) marshall(Marshall)}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * HTTP parts (headers, query parameters, form data...) are serialized and parsed using the {@link HttpPartSerializer}
 * and {@link HttpPartParser} APIs.  By default, clients are configured to use {@link OpenApiSerializer} and
 * {@link OpenApiParser}.  These can be overridden using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#partSerializer(HttpPartSerializer) partSerializer(HttpPartSerializer)}
 * 		<li class='jm'>{@link RestClientBuilder#partSerializer(Class) partSerializer(Class&lt;? extends HttpPartSerializer>)}
 * 		<li class='jm'>{@link RestClientBuilder#partParser(HttpPartParser) partParser(HttpPartParser)}
 * 		<li class='jm'>{@link RestClientBuilder#partParser(Class) partParser(Class&lt;? extends HttpPartParser>)}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The {@link RestClientBuilder} class also provides convenience methods for setting common serializer and parser
 * settings.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a basic REST client with JSON support.</jc>
 * 	<jc>// Use single-quotes and whitespace.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().json().sq().ws().build();
 * </p>
 *
 *
 * <h4 class='topic'>Request Headers</h4>
 * <p class='w900'>
 * Per-client or per-request headers can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#header(String,Object) header(String,Object)}
 * 		<li class='jm'>{@link RestClientBuilder#header(String,Object,HttpPartSchema,HttpPartSerializer) header(String,Object,HttpPartSerializer,HttpPartSchema)}
 * 		<li class='jm'>{@link RestClientBuilder#header(Object) header(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#headers(Object...) headers(Object...)}
 * 		<li class='jm'>{@link RestClientBuilder#headerPairs(Object...) headerPairs(Object...)}
 * 		<li class='jm'>{@link RestClientBuilder#accept(Object) accept(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#acceptCharset(Object) acceptCharset(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#acceptEncoding(Object) acceptEncoding(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#acceptLanguage(Object) acceptLanguage(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#authorization(Object) authorization(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#cacheControl(Object) cacheControl(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#clientVersion(Object) clientVersion(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#connection(Object) connection(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#contentLength(Object) contentLength(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#contentType(Object) contentType(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#date(Object) date(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#expect(Object) expect(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#forwarded(Object) forwarded(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#from(Object) from(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#host(Object) host(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#ifMatch(Object) ifMatch(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#ifModifiedSince(Object) ifModifiedSince(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#ifNoneMatch(Object) ifNoneMatch(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#ifRange(Object) ifRange(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#ifUnmodifiedSince(Object) ifUnmodifiedSince(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#maxForwards(Object) maxForwards(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#origin(Object) origin(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#pragma(Object) pragma(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#proxyAuthorization(Object) proxyAuthorization(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#range(Object) proxyAuthorization(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#referer(Object) referer(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#te(Object) te(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#userAgent(Object) userAgent(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#upgrade(Object) upgrade(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#via(Object) via(Object)}
 * 		<li class='jm'>{@link RestClientBuilder#warning(Object) warning(Object)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#header(String,Object) header(String,Object)}
 * 		<li class='jm'>{@link RestRequest#header(EnumSet,String,Object) header(EnumSet&gt;AddFlag&gt;,String,Object)}
 * 		<li class='jm'>{@link RestRequest#header(Object) header(Object)}
 * 		<li class='jm'>{@link RestRequest#header(EnumSet,Object) header(EnumSet&gt;AddFlag&gt;,Object)}
 * 		<li class='jm'>{@link RestRequest#headers(Object...) headers(Object...)}
 * 		<li class='jm'>{@link RestRequest#headers(EnumSet,Object...) headers(EnumSet&gt;AddFlag&gt;Object...)}
 * 		<li class='jm'>{@link RestRequest#headerPairs(Object...) headers(Object...)}
 *		<li class='jm'>{@link RestRequest#accept(Object) accept(Object)}
 * 		<li class='jm'>{@link RestRequest#acceptCharset(Object) acceptCharset(Object)}
 * 		<li class='jm'>{@link RestRequest#acceptEncoding(Object) acceptEncoding(Object)}
 * 		<li class='jm'>{@link RestRequest#acceptLanguage(Object) acceptLanguage(Object)}
 * 		<li class='jm'>{@link RestRequest#authorization(Object) authorization(Object)}
 * 		<li class='jm'>{@link RestRequest#cacheControl(Object) cacheControl(Object)}
 * 		<li class='jm'>{@link RestRequest#clientVersion(Object) clientVersion(Object)}
 * 		<li class='jm'>{@link RestRequest#connection(Object) connection(Object)}
 * 		<li class='jm'>{@link RestRequest#contentLength(Object) contentLength(Object)}
 * 		<li class='jm'>{@link RestRequest#contentType(Object) contentType(Object)}
 * 		<li class='jm'>{@link RestRequest#date(Object) date(Object)}
 * 		<li class='jm'>{@link RestRequest#expect(Object) expect(Object)}
 * 		<li class='jm'>{@link RestRequest#forwarded(Object) forwarded(Object)}
 * 		<li class='jm'>{@link RestRequest#from(Object) from(Object)}
 * 		<li class='jm'>{@link RestRequest#host(Object) host(Object)}
 * 		<li class='jm'>{@link RestRequest#ifMatch(Object) ifMatch(Object)}
 * 		<li class='jm'>{@link RestRequest#ifModifiedSince(Object) ifModifiedSince(Object)}
 * 		<li class='jm'>{@link RestRequest#ifNoneMatch(Object) ifNoneMatch(Object)}
 * 		<li class='jm'>{@link RestRequest#ifRange(Object) ifRange(Object)}
 * 		<li class='jm'>{@link RestRequest#ifUnmodifiedSince(Object) ifUnmodifiedSince(Object)}
 * 		<li class='jm'>{@link RestRequest#maxForwards(Object) maxForwards(Object)}
 * 		<li class='jm'>{@link RestRequest#origin(Object) origin(Object)}
 * 		<li class='jm'>{@link RestRequest#pragma(Object) pragma(Object)}
 * 		<li class='jm'>{@link RestRequest#proxyAuthorization(Object) proxyAuthorization(Object)}
 * 		<li class='jm'>{@link RestRequest#range(Object) proxyAuthorization(Object)}
 * 		<li class='jm'>{@link RestRequest#referer(Object) referer(Object)}
 * 		<li class='jm'>{@link RestRequest#te(Object) te(Object)}
 * 		<li class='jm'>{@link RestRequest#userAgent(Object) userAgent(Object)}
 * 		<li class='jm'>{@link RestRequest#upgrade(Object) upgrade(Object)}
 * 		<li class='jm'>{@link RestRequest#via(Object) via(Object)}
 * 		<li class='jm'>{@link RestRequest#warning(Object) warning(Object)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a client that adds an Authorization header to every request.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().header(<js>"Authorization"</js>, <js>"Foo"</js>).build();
 *
 * 	<jc>// Or do it on every request.</jc>
 * 	String response = c.get(<jsf>URL</jsf>).authorization(<js>"Foo"</js>).run().getBody().asString();
 *
 * 	<jc>// Or use an HttpHeader.</jc>
 * 	response = c.get(<jsf>URL</jsf>).headers(<jk>new</jk> Authorization(<js>"Foo"</js>)).run().getBody().asString();
 * </p>
 *
 * <p class='w900'>
 * Note that in the methods above, header values are specified as objects which are converted to strings at runtime.
 * This allows you to pass in headers whose values may change over time (such as <c>Authorization</c> headers
 * which may need to change every few minutes).
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> AuthGenerator {
 * 		<jk>public</jk> String toString() {
 * 			<jc>// Generate an updated auth token.</jc>
 * 		}
 * 	}
 *
 * 	<jc>// Create a client that adds an Authorization header to every request.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().authorization(<jk>new</jk> AuthGenerator()).build();
 * </p>
 *
 * <ul class='notes'>
 * 	<li>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>Header</c> or
 * 		<c>NameValuePair</c> objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Query Parameters</h4>
 * <p>
 * Per-client or per-request query parmameters can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#query(String,Object) query(String,Object)}
 * 		<li class='jm'>{@link RestClientBuilder#query(String,Object,HttpPartSchema,HttpPartSerializer) query(String,Object,HttpPartSerializer,HttpPartSchema)}
 * 		<li class='jm'>{@link RestClientBuilder#query(Object...) query(Object...)}
 * 		<li class='jm'>{@link RestClientBuilder#queryPairs(Object...) queryPairs(Object...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#query(String,Object) query(String,Object)}
 * 		<li class='jm'>{@link RestRequest#query(EnumSet,String,Object) query(EnumSet&lt;AddFlag&gt;,String,Object)}
 * 		<li class='jm'>{@link RestRequest#query(Object...) query(Object...)}
 * 		<li class='jm'>{@link RestRequest#query(EnumSet,Object...) query(EnumSet&lt;AddFlag&gt;,Object...)}
 * 		<li class='jm'>{@link RestRequest#queryPairs(Object...) queryPairs(Object...)}
 * 		<li class='jm'>{@link RestRequest#queryCustom(Object) queryCustom(Object)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a client that adds a ?foo=bar query parameter to every request.</jc>
 * 	RestClient c = RestClient.<jsm>create</jsm>().query(<js>"foo"</js>, <js>"bar"</js>).build();
 *
 * 	<jc>// Or do it on every request.</jc>
 * 	String response = c.get(<jsf>URL</jsf>).query(<js>"foo"</js>, <js>"bar"</js>).run().getBody().asString();
 * </p>
 *
 * <ul class='notes'>
 * 	<li>Like header values, you can pass in objects that get converted to strings at runtime.
 * 	<li>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>NameValuePair</c>
 * 		objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Form Data</h4>
 *
 * <p class='w900'>
 * Per-client or per-request form-data parameters can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#formData(String,Object) formData(String,Object)}
 * 		<li class='jm'>{@link RestClientBuilder#formData(String,Object,HttpPartSchema,HttpPartSerializer) formData(String,Object,HttpPartSerializer,HttpPartSchema)}
 * 		<li class='jm'>{@link RestClientBuilder#formData(Object...) formData(Object...)}
 * 		<li class='jm'>{@link RestClientBuilder#formDataPairs(Object...) formDataPairs(Object...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#formData(String,Object) formData(String,Object)}
 * 		<li class='jm'>{@link RestRequest#formData(EnumSet,String,Object) formData(EnumSet&lt;AddFlag&gt;,String,Object)}
 * 		<li class='jm'>{@link RestRequest#formData(Object...) formData(Object...)}
 * 		<li class='jm'>{@link RestRequest#formData(EnumSet,Object...) formData(EnumSet&lt;AddFlag&gt;,Object...)}
 * 		<li class='jm'>{@link RestRequest#formDataPairs(Object...) formDataPairs(Object...)}
 * 		<li class='jm'>{@link RestRequest#formDataCustom(Object) formDataCustom(Object)}
 * 	</ul>
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>Like header values, you can pass in objects that get converted to strings at runtime.
 * 	<li>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>NameValuePair</c>
 * 		objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Body</h4>
 *
 * <p class='w900'>
 * The request body can either be passed in with the client creator method (e.g. {@link RestClient#post(Object,Object)}),
 * or can be specified via the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#body(Object) body(Object)}
 * 		<li class='jm'>{@link RestRequest#body(Object,HttpPartSchema) body(Object,HttpPartSchema)}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The request body can be any of the following types:
 * <ul class='javatree'>
 * 		<li class='jc'>
 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} defined on the client or request.
 * 		<li class='jc'>
 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
 * 		<li class='jc'>
 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
 * 		<li class='jc'>
 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
 * 		<li class='jc'>
 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
 * 		<li class='jc'>
 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
 * 		<li class='jc'>
 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
 * 	</ul>
 *
 * <ul class='notes'>
 * 	<li>If the serializer on the client or request is explicitly set to <jk>null</jk>, POJOs will be converted to strings
 * 		using the registered part serializer as content type <js>"text/plain</js>.  If the part serializer is also <jk>null</jk>,
 * 		POJOs will be converted to strings using {@link ClassMeta#toString(Object)} which typically just calls {@link Object#toString()}.
 * </ul>
 *
 *
 * <h4 class='topic'>Response Status</h4>
 *
 * <p class='w900'>
 * After execution using {@link RestRequest#run()} or {@link RestRequest#complete()}, the following methods can be used
 * to get the response status:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getStatusLine() getStatusLine()} <jk>returns</jk> {@link StatusLine}</c>
 * 		<li class='jm'><c>{@link RestResponse#getStatusLine(Mutable) getStatusLine(Mutable&lt;StatusLine&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponse#getStatusCode() getStatusCode()} <jk>returns</jk> <jk>int</jk></c>
 * 		<li class='jm'><c>{@link RestResponse#getStatusCode(Mutable) getStatusCode(Mutable&lt;Integer&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponse#getReasonPhrase() getReasonPhrase()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponse#getReasonPhrase(Mutable) getReasonPhrase(Mutable&lt;String&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponse#assertStatus() assertStatus()} <jk>returns</jk> {@link RestResponseStatusLineAssertion}</c>
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The methods with mutable parameters are provided to allow access to status values without breaking fluent call chains.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Only interested in status code.</jc>
 * 	<jk>int</jk> statusCode = c.get(<jsf>URL</jsf>).complete().getStatusCode();
 *
 *   <jc>// Interested in multiple values.</jc>
 * 	Mutable&lt;Integer&gt; statusCode;
 * 	Mutable&lt;String&gt; reasonPhrase;
 * 	c.get(<jsf>URL</jsf>).complete().getStatusCode(statusCode).getReasonPhrase(reasonPhrase);
 * 	System.<jsf>err</jsf>.println(<js>"statusCode="</js>+statusCode.get()+<js>", reasonPhrase="</js>+reasonPhrase.get());
 * </p>
 *
 * <ul class='notes'>
 * 	<li>If you are only interested in the response status and not the response body, be sure to use {@link RestRequest#complete()} instead
 * 		of {@link RestRequest#run()} to make sure the response body gets automatically cleaned up.  Otherwise you must
 * 		consume the response yourself.
 * </ul>
 *
 * <p class='w900'>
 * The assertion methods are provided for quickly asserting status codes in fluent calls.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Status assertion using a static value.</jc>
 * 	String body = c.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatus().is(200)
 * 		.getBody().asString();
 *
 * 	<jc>// Status assertion using a predicate.</jc>
 * 	String body = c.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatus().passes(x -&gt; x &lt; 400)
 * 		.getBody().asString();
 * </p>
 *
 *
 * <h4 class='topic'>Response Headers</h4>
 *
 * <p class='w900'>
 * Response headers are accessed through the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getHeader(String) getHeader(String)} <jk>returns</jk> {@link RestResponseHeader}</c>
 * 		<li class='jm'><c>{@link RestResponse#getHeaders(String) getHeaders(String)} <jk>returns</jk> {@link RestResponseHeader}[]</c>
 * 		<li class='jm'><c>{@link RestResponse#getFirstHeader(String) getFirstHeader(String)} <jk>returns</jk> {@link RestResponseHeader}</c>
 * 		<li class='jm'><c>{@link RestResponse#getLastHeader(String) getLastHeader(String)} <jk>returns</jk> {@link RestResponseHeader}</c>
 * 		<li class='jm'><c>{@link RestResponse#getAllHeaders() getAllHeaders()} <jk>returns</jk> {@link RestResponseHeader}[]</c>
 * 		<li class='jm'><c>{@link RestResponse#getStringHeader(String) getStringHeader(String)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponse#getStringHeader(String,String) getStringHeader(String,String)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponse#containsHeader(String) containsHeader(String)} <jk>returns</jk> <jk>boolean</jk></c>
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Unlike {@link RestResponse#getFirstHeader(String)} and {@link RestResponse#getLastHeader(String)}, the {@link RestResponse#getHeader(String)}
 * method returns an empty {@link RestResponseHeader} object instead of returning <jk>null</jk>.
 * This allows it to be used more easily in fluent calls.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// See if response contains Location header.</jc>
 * 	<jk>boolean</jk> hasLocationHeader = c.get(<jsf>URL</jsf>).complete().getHeader(<js>"Location"</js>).exists();
 * </p>
 *
 * <p class='w900'>
 * The {@link RestResponseHeader} class extends from the HttpClient {@link Header} class and provides several convenience
 * methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponseHeader}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponseHeader#exists() exists()} <jk>returns</jk> <jk>boolean</jk></c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asString() asString()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asString(Mutable) asString(Mutable&lt;String&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptionalString() asOptionalString()} <jk>returns</jk> Optional&lt;String&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asStringOrElse(String) asStringOrElse(String)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asStringOrElse(Mutable,String) asStringOrElse(Mutable&lt;String&gt;,String)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(Type,Type...) as(Type,Type...)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(Mutable,Type,Type...) as(Mutable&lt;T&gt;,Type,Type...)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(Class) as(Class&lt;T&gt;)} <jk>returns</jk>T</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(Mutable,Class) as(Mutable&lt;T&gt;,Class&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(ClassMeta) as(ClassMeta&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#as(Mutable,ClassMeta) as(Mutable&lt;T&gt;,ClassMeta&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(Type,Type...) asOptional(Type,Type...)} <jk>returns</jk> Optional&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(Mutable,Type,Type...) asOptional(Mutable&lt;Optional&lt;T&gt;&gt;,Type,Type...)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(Class) asOptional(Class&lt;T&gt;)} <jk>returns</jk> Optional&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(Mutable,Class) asOptional(Mutable&lt;Optional&lt;T&gt;&gt;,Class&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(ClassMeta) asOptional(ClassMeta&lt;T&gt;)} <jk>returns</jk> Optional&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asOptional(Mutable,ClassMeta) asOptional(Mutable&lt;Optional&lt;T&gt;&gt;,ClassMeta&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(Pattern) asMatcher(Pattern)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(Mutable,Pattern) asMatcher(Mutable&lt;Matcher&gt;,Pattern)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(String) asMatcher(String)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(Mutable,String) asMatcher(Mutable&lt;Matcher&gt;,String)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(String,int) asMatcher(String,int)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#asMatcher(Mutable,String,int) asMatcher(Mutable&lt;Matcher&gt;,String,int)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseHeader#assertThat() assertThat()} <jk>returns</jk> {@link FluentStringAssertion}</c>
 * 	</ul>
 * </ul>
 *
 *
 * <h4 class='topic'>Response Body</h4>
 *
 * <p class='w900'>
 * The response body is accessed through the following method:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getBody() getBody()} <jk>returns</jk> {@link RestResponseBody}</c>
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * The {@link RestResponseBody} class extends from the HttpClient {@link HttpEntity} class and provides several convenience
 * methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponseBody}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponseBody#asInputStream() asInputStream()} <jk>returns</jk> InputStream</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asReader() asReader()} <jk>returns</jk> Reader</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asReader(String) asReader(String)} <jk>returns</jk> Reader</c>
 * 		<li class='jm'><c>{@link RestResponseBody#pipeTo(OutputStream) pipeTo(OutputStream)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#pipeTo(Writer) pipeTo(Writer)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#pipeTo(Writer,String) pipeTo(Writer,String)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#pipeTo(Writer,boolean) pipeTo(Writer,boolean)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#pipeTo(Writer,String,boolean) pipeTo(Writer,String,boolean)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(Type,Type...) as(Type,Type...)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(Mutable,Type,Type...) as(Mutable&lt;T&gt;,Type,Type...)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(Class) as(Class&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(Mutable,Class) as(Mutable&lt;T&gt;,Class&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(ClassMeta) as(ClassMeta&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestResponseBody#as(Mutable,ClassMeta) as(Mutable&lt;T&gt;,ClassMeta&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(Class) asFuture(Class&lt;T&gt;)} <jk>returns</jk> Future&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(Mutable,Class) asFuture(Mutable&lt;Future&lt;T&gt;&gt;,Class&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(ClassMeta) asFuture(ClassMeta&lt;T&gt;)} <jk>returns</jk>Future&lt;T&gt; </c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(Mutable,ClassMeta) asFuture(Mutable&lt;Future&lt;T&gt;&gt;,ClassMeta&lt;T&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(Type,Type...) asFuture(Type,Type...)} <jk>returns</jk> Future&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asFuture(Mutable,Type,Type...) asFuture(Mutable&lt;Future&lt;T&gt;&gt;,Type,Type...)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asString() asString()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asString(Mutable) asString(Mutable&lt;String&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asStringFuture() asStringFuture()} <jk>returns</jk> Future&lt;String&gt;</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asStringFuture(Mutable) asStringFuture(Mutable&lt;Future&lt;String&gt;&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asAbbreviatedString(int) asAbbreviatedString(int)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asAbbreviatedString(Mutable,int) asAbbreviatedString(Mutable&lt;String&gt;,int)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asPojoRest(Class) asPojoRest(Class&lt;?&gt;)} <jk>returns</jk> {@link PojoRest}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asPojoRest(Mutable,Class) asPojoRest(Mutable&lt;PojoRest&gt;,Class&lt;?&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asPojoRest() asPojoRest()} <jk>returns</jk> {@link PojoRest}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asPojoRest(Mutable) asPojoRest(Mutable&lt;PojoRest&gt;)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(Pattern) asMatcher(Pattern)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(Mutable,Pattern) asMatcher(Mutable&lt;Matcher&gt;,Pattern)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(String) asMatcher(String)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(Mutable,String) asMatcher(Mutable&lt;Matcher&gt;,String)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(String,int) asMatcher(String,int)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#asMatcher(Mutable,String,int) asMatcher(Mutable&lt;Matcher&gt;,String,int)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link RestResponseBody#assertThat() assertThat()} <jk>returns</jk> {@link FluentStringAssertion}</c>
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Parse into a linked-list of strings.</jc>
 * 	List&lt;String&gt; l = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().as(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a linked-list of beans.</jc>
 * 	List&lt;MyBean&gt; l = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().as(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
 * 	List&lt;List&lt;String&gt;&gt; l = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().as(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a map of string keys/values.</jc>
 * 	Map&lt;String,String&gt; m = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
 * 	Map&lt;String,List&lt;MyBean&gt;&gt; m = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p class='w900'>
 * The response body can only be consumed once.  However, the {@link RestResponse#cacheBody()} and {@link RestResponseBody#cache()} methods are provided
 * to cache the response body in memory so that you can perform several operations against it.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Cache the response body so that you can perform multiple operations against it.</jc>
 * 	String body = client.get(<jsf>URL</jsf>).run()
 * 		.getBody().cache().assertValueContains(<js>"Success"</js>)
 * 		.getBody().asString();
 * </p>
 *
 *
 * <h4 class='topic'>Customizing Apache HttpClient</h4>
 *
 * <p class='w900'>
 * Several methods are provided for customizing the underlying HTTP client and client builder classes:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#httpClientBuilder(HttpClientBuilder) httpClientBuilder(HttpClientBuilder)}
 * 		<li class='jm'>{@link RestClientBuilder#createHttpClientBuilder() createHttpClientBuilder()}
 * 		<li class='jm'>{@link RestClientBuilder#createHttpClient() createHttpClient()}
 * 		<li class='jm'>{@link RestClientBuilder#createConnectionManager() createConnectionManager()}
 * 		<li class='jm'>{@link RestClientBuilder#pooled() pooled()}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Additionally, all methods on the <c>HttpClientBuilder</c> class are also exposed with fluent setters:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#disableRedirectHandling() disableRedirectHandling()}
 * 		<li class='jm'>{@link RestClientBuilder#redirectStrategy(RedirectStrategy) redirectStrategy(RedirectStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultCookieSpecRegistry(Lookup) defaultCookieSpecRegistry(Lookup&lt;CookieSpecProvider>)}
 * 		<li class='jm'>{@link RestClientBuilder#requestExecutor(HttpRequestExecutor) requestExecutor(HttpRequestExecutor)}
 * 		<li class='jm'>{@link RestClientBuilder#sslHostnameVerifier(HostnameVerifier) sslHostnameVerifier(HostnameVerifier)}
 * 		<li class='jm'>{@link RestClientBuilder#publicSuffixMatcher(PublicSuffixMatcher) publicSuffixMatcher(PublicSuffixMatcher)}
 * 		<li class='jm'>{@link RestClientBuilder#sslContext(SSLContext) sslContext(SSLContext)}
 * 		<li class='jm'>{@link RestClientBuilder#sslSocketFactory(LayeredConnectionSocketFactory) sslSocketFactory(LayeredConnectionSocketFactory)}
 * 		<li class='jm'>{@link RestClientBuilder#maxConnTotal(int) maxConnTotal(int)}
 * 		<li class='jm'>{@link RestClientBuilder#maxConnPerRoute(int) maxConnPerRoute(int)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultSocketConfig(SocketConfig) defaultSocketConfig(SocketConfig)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultConnectionConfig(ConnectionConfig) defaultConnectionConfig(ConnectionConfig)}
 * 		<li class='jm'>{@link RestClientBuilder#connectionTimeToLive(long,TimeUnit) connectionTimeToLive(long,TimeUnit)}
 * 		<li class='jm'>{@link RestClientBuilder#connectionManager(HttpClientConnectionManager) connectionManager(HttpClientConnectionManager)}
 * 		<li class='jm'>{@link RestClientBuilder#connectionManagerShared(boolean) connectionManagerShared(boolean)}
 * 		<li class='jm'>{@link RestClientBuilder#connectionReuseStrategy(ConnectionReuseStrategy) connectionReuseStrategy(ConnectionReuseStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#keepAliveStrategy(ConnectionKeepAliveStrategy) keepAliveStrategy(ConnectionKeepAliveStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#targetAuthenticationStrategy(AuthenticationStrategy) targetAuthenticationStrategy(AuthenticationStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#proxyAuthenticationStrategy(AuthenticationStrategy) proxyAuthenticationStrategy(AuthenticationStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#userTokenHandler(UserTokenHandler) userTokenHandler(UserTokenHandler)}
 * 		<li class='jm'>{@link RestClientBuilder#disableConnectionState() disableConnectionState()}
 * 		<li class='jm'>{@link RestClientBuilder#schemePortResolver(SchemePortResolver) schemePortResolver(SchemePortResolver)}
 * 		<li class='jm'>{@link RestClientBuilder#userAgent(String) userAgent(String)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultHeaders(Collection) defaultHeaders(Collection&lt;? extends Header>)}
 * 		<li class='jm'>{@link RestClientBuilder#addInterceptorFirst(HttpResponseInterceptor) addInterceptorFirst(HttpResponseInterceptor)}
 * 		<li class='jm'>{@link RestClientBuilder#addInterceptorLast(HttpResponseInterceptor) addInterceptorLast(HttpResponseInterceptor)}
 * 		<li class='jm'>{@link RestClientBuilder#addInterceptorFirst(HttpRequestInterceptor) addInterceptorFirst(HttpRequestInterceptor)}
 * 		<li class='jm'>{@link RestClientBuilder#addInterceptorLast(HttpRequestInterceptor) addInterceptorLast(HttpRequestInterceptor)}
 * 		<li class='jm'>{@link RestClientBuilder#disableCookieManagement() disableCookieManagement()}
 * 		<li class='jm'>{@link RestClientBuilder#disableContentCompression() disableContentCompression()}
 * 		<li class='jm'>{@link RestClientBuilder#disableAuthCaching() disableAuthCaching()}
 * 		<li class='jm'>{@link RestClientBuilder#httpProcessor(HttpProcessor) httpProcessor(HttpProcessor)}
 * 		<li class='jm'>{@link RestClientBuilder#retryHandler(HttpRequestRetryHandler) retryHandler(HttpRequestRetryHandler)}
 * 		<li class='jm'>{@link RestClientBuilder#disableAutomaticRetries() disableAutomaticRetries()}
 * 		<li class='jm'>{@link RestClientBuilder#proxy(HttpHost proxy) proxy(HttpHost proxy)}
 * 		<li class='jm'>{@link RestClientBuilder#routePlanner(HttpRoutePlanner) routePlanner(HttpRoutePlanner)}
 * 		<li class='jm'>{@link RestClientBuilder#connectionBackoffStrategy(ConnectionBackoffStrategy) connectionBackoffStrategy(ConnectionBackoffStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#backoffManager(BackoffManager) backoffManager(BackoffManager)}
 * 		<li class='jm'>{@link RestClientBuilder#serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy) serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultCookieStore(CookieStore) defaultCookieStore(CookieStore)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultCredentialsProvider(CredentialsProvider) defaultCredentialsProvider(CredentialsProvider)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultAuthSchemeRegistry(Lookup) defaultAuthSchemeRegistry(Lookup&lt;AuthSchemeProvider>)}
 * 		<li class='jm'>{@link RestClientBuilder#contentDecoderRegistry(Map) contentDecoderRegistry(Map&lt;String,InputStreamFactory>)}
 * 		<li class='jm'>{@link RestClientBuilder#defaultRequestConfig(RequestConfig) defaultRequestConfig(RequestConfig)}
 * 		<li class='jm'>{@link RestClientBuilder#useSystemProperties() useSystemProperties()}
 * 		<li class='jm'>{@link RestClientBuilder#evictExpiredConnections() evictExpiredConnections()}
 * 		<li class='jm'>{@link RestClientBuilder#evictIdleConnections(long,TimeUnit) evictIdleConnections(long,TimeUnit)}
 * 	</ul>
 * </ul>
 *
 *
 * <h4 class='topic'>Custom Call Handlers</h4>
 *
 * <p class='w900'>
 * The {@link RestCallHandler} interface provides the ability to provide custom handling of requests.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#callHandler(Class) callHandler(Class&lt;? extends RestCallHandler&gt;)}
 * 		<li class='jm'>{@link RestClientBuilder#callHandler(RestCallHandler) callHandler(RestCallHandler)}
 * 	</ul>
 * 	<li class='jic'>{@link RestCallHandler}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestCallHandler#execute(HttpHost,HttpEntityEnclosingRequestBase,HttpContext) execute(HttpHost,HttpEntityEnclosingRequestBase,HttpContext)} <jk>returns</jk> HttpResponse</c>
 * 		<li class='jm'><c>{@link RestCallHandler#execute(HttpHost,HttpRequestBase,HttpContext) execute(HttpHost,HttpRequestBase,HttpContext)} <jk>returns</jk> HttpResponse</c>
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Note that there are other ways of accomplishing this such as extending the {@link RestClient} class and overriding
 * the {@link #execute(HttpHost,HttpEntityEnclosingRequestBase,HttpContext)} and {@link #execute(HttpHost,HttpRequestBase,HttpContext)}
 * or by defining your own {@link HttpRequestExecutor}.  Using this interface is often simpler though.
 *
 *
 * <h4 class='topic'>Logging / Debugging</h4>
 *
 * <p class='w900'>
 * The following methods provide logging of requests and responses:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#logger(Logger) logger(Logger)}
 * 		<li class='jm'>{@link RestClientBuilder#logToConsole() logToConsole()}
 * 		<li class='jm'>{@link RestClientBuilder#logRequests(DetailLevel,Level) logRequests(DetailLevel,Level)}
 * 	</ul>
 * </ul>
 *
 * <p class='notes w900'>
 * It should be noted that if you enable request logging detail level {@link DetailLevel#FULL}, response bodies will be cached by default which may introduce
 * a performance penalty.
 *
 * <p class='w900'>
 * Additionally, the following method is also provided for enabling debug mode:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#debug() debug()}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Enabling debug mode has the following effects:
 * <ul>
 * 	<li>{@link Context#CONTEXT_debug} is enabled.
 * 	<li>{@link #RESTCLIENT_leakDetection} is enabled.
 * 	<li>{@link RestClientBuilder#logToConsole()} is called.
 * </ul>
 *
 *
 * <h4 class='topic'>Interceptors</h4>
 *
 * <p class='w900'>
 * The {@link RestCallInterceptor} API provides a quick way of intercepting and manipulating requests and responses.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClientBuilder}
 * 	<ul>
 * 		<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...) interceptors(RestCallInterceptor...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#interceptors(RestCallInterceptor...) interceptors(RestCallInterceptor...)}
 * 	</ul>
 * 	<li class='jic'>{@link RestCallInterceptor}
 * 	<ul>
 * 		<li class='jm'>{@link RestCallInterceptor#onInit(RestRequest) onInit(RestRequest)}
 * 		<li class='jm'>{@link RestCallInterceptor#onConnect(RestRequest,RestResponse) onConnect(RestRequest,RestResponse)}
 * 		<li class='jm'>{@link RestCallInterceptor#onClose(RestRequest,RestResponse) onClose(RestRequest,RestResponse)}
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Note that the {@link HttpRequestInterceptor} and {@link HttpResponseInterceptor} classes can also be used for
 * intercepting requests.
 *
 *
 * <h4 class='topic'>REST Proxies</h4>
 *
 * <p class='w900'>
 * One of the more powerful features of the REST client class is the ability to produce Java interface proxies against
 * arbitrary remote REST resources.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Define a Remote proxy for interacting with a REST interface.</jc>
 * 	<ja>@Remote</ja>(path=<js>"/petstore"</js>)
 * 	<jk>public interface</jk> PetStore {
 * 		<ja>@RemoteMethod</ja>(httpMethod=<jsf>POST</jsf>, path=<js>"/pets"</js>)
 * 		Pet addPet(
 * 			<ja>@Body</ja> CreatePet pet,
 * 			<ja>@Header</ja>(<js>"E-Tag"</js>) UUID etag,
 * 			<ja>@Query</ja>(<js>"debug"</js>) <jk>boolean</jk> debug
 * 		);
 * 	}
 *
 * 	<jc>// Use a RestClient with default Simple JSON support.</jc>
 * 	<jk>try</jk> (RestClient c = RestClient.<jsm>create</jsm>().simpleJson().build()) {
 * 		PetStore store = c.getRemote(PetStore.<jk>class</jk>, <js>"http://localhost:10000"</js>);
 * 		CreatePet cp = <jk>new</jk> CreatePet(<js>"Fluffy"</js>, 9.99);
 * 		Pet p = store.addPet(cp, UUID.<jsm>randomUUID</jsm>(), <jk>true</jk>);
 * 	}
 * </p>
 *
 * <p class='w900'>
 * The methods to retrieve remote interfaces are:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestClient#getRemote(Class) getRemote(Class&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestClient#getRemote(Class,Object) getRemote(Class&lt;T&gt;,Object)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestClient#getRemote(Class,Object,Serializer,Parser) getRemote(Class&lt;T&gt;,Object,Serializer,Parser)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestClient#getRrpcInterface(Class) getRrpcInterface(Class&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestClient#getRrpcInterface(Class,Object) getRrpcInterface(Class&lt;T&gt;,Object)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link RestClient#getRrpcInterface(Class,Object,Serializer,Parser) getRrpcInterface(Class&lt;T&gt;,Object,Serializer,Parser)} <jk>returns</jk> T</c>
 * 	</ul>
 * </ul>
 *
 * <p class='w900'>
 * Two basic types of remote interfaces are provided:
 *
 * <ul class='spaced-list'>
 * 	<li>{@link Remote @Remote}-annotated interfaces.  These can be defined against arbitrary external REST resources.
 * 	<li>RPC-over-REST interfaces.  These are Java interfaces that allow you to make method calls on server-side POJOs.
 * </ul>
 *
 * <p class='w900'>
 * Refer to the following documentation on both flavors:
 *
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
 * 	<li class='link'>{@doc juneau-rest-server.restRPC}
 * </ul>
 *
 * <br>
 * <hr class='w900'>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
@ConfigurableContext(nocache=true)
public class RestClient extends BeanContext implements HttpClient, Closeable, RestCallHandler, RestCallInterceptor {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RestClient.";

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_callHandler RESTCLIENT_callHandler}
	 * 	<li><b>Name:</b>  <js>"RestClient.callHandler.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.rest.client2.RestCallHandler}&gt;
	 * 			<li>{@link org.apache.juneau.rest.client2.RestCallHandler}
	 * 		</ul>
	 * 	<li><b>Default:</b>  <c><jk>null</jk></c>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#callHandler(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#callHandler(RestCallHandler)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Allows you to provide a custom handler for making HTTP calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that handles processing of requests using a custom handler.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.callHandler(
	 * 			<jk>new</jk> RestCallHandler() {
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 					<jc>// Custom handle requests with request bodies.</jc>
	 * 				}
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 					<jc>// Custom handle requests without request bodies.</jc>
	 * 				}
	 * 			}
	 * 		)
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_callHandler = PREFIX + "callHandler.o";

	/**
	 * Configuration property:  Error codes predicate.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_errorCodes RESTCLIENT_errorCodes}
	 * 	<li><b>Name:</b>  <js>"RestClient.errorCodes.o"</js>
	 * 	<li><b>Data type:</b>  {@link java.util.function.Predicate}&lt;{@link java.lang.Integer}&gt;
	 * 	<li><b>Default:</b>  <code>x -&gt; x&gt;=400</code>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#errorCodes(Predicate)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#errorCodes(Predicate)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Defines a predicate to test for error codes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that considers any 300+ responses to be errors.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.errorCodes(x -&gt; x &gt;= 300)
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_errorCodes = PREFIX + "errorCodes.o";

	/**
	 * Configuration property:  Executor service.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_executorService RESTCLIENT_executorService}
	 * 	<li><b>Name:</b>  <js>"RestClient.executorService.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link java.util.concurrent.ExecutorService}&gt;</c>
	 * 			<li>{@link java.util.concurrent.ExecutorService}
	 * 		</ul>
	 * 	<li><b>Default:</b>  <jk>null</jk>.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#executorService(ExecutorService, boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
	 *
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#runFuture()}
	 * 	<li class='jm'>{@link RestRequest#completeFuture()}
	 * 	<li class='jm'>{@link RestResponseBody#asFuture(Class)} (and similar methods)
	 * </ul>
	 *
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized executor service.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.executorService(<jk>new</jk> ThreadPoolExecutor(1, 1, 30, TimeUnit.<jsf>SECONDS</jsf>, <jk>new</jk> ArrayBlockingQueue&lt;Runnable&gt;(10)), <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use it to asynchronously run a request.</jc>
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URL</jsf>).runFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>try</jk> {
	 * 		String body = f.get().getBody().asString();
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 	}
	 * 	<jc>// Use it to asynchronously retrieve a response.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asFuture(MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String RESTCLIENT_executorService = PREFIX + "executorService.o";

	/**
	 * Configuration property:  Shut down executor service on close.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_executorServiceShutdownOnClose RESTCLIENT_executorServiceShutdownOnClose}
	 * 	<li><b>Name:</b>  <js>"RestClient.executorServiceShutdownOnClose.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestClient.executorServiceShutdownOnClose</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_EXECUTORSERVICESHUTDOWNONCLOSE</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#executorService(ExecutorService, boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 */
	public static final String RESTCLIENT_executorServiceShutdownOnClose = PREFIX + "executorServiceShutdownOnClose.b";

	/**
	 * Configuration property:  Request form-data parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_formData RESTCLIENT_formData}
	 * 	<li><b>Name:</b>  <js>"RestClient.formData.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.http.NameValuePair}&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestClientBuilder}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#formData(String,Object) formData(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#formData(String,Object,HttpPartSchema,HttpPartSerializer) formData(String,Object,HttpPartSerializer,HttpPartSchema)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#formData(Object...) formData(Map<String, Object>)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#formDataPairs(Object...) formData(Map<String, Object>)}
	 * 			</ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestRequest}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formData(String,Object) formData(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formData(EnumSet,String,Object) formData(EnumSet&lt;AddFlag&gt;,String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formData(Object...) formData(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formData(EnumSet,Object...) formData(EnumSet&lt;AddFlag&gt;Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formDataPairs(Object...) formDataPairs(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#formDataCustom(Object) customFormData(Object)}
	 * 			</ul>
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Query parameters to add to every request.
	 */
	public static final String RESTCLIENT_formData = PREFIX + "formData.lo";

	/**
	 * Configuration property:  Request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_headers RESTCLIENT_headers}
	 * 	<li><b>Name:</b>  <js>"RestClient.headers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.http.Header} | {@link org.apache.http.NameValuePair}&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestClientBuilder}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#header(String,Object) header(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#header(String,Object,HttpPartSchema,HttpPartSerializer) header(String,Object,HttpPartSerializer,HttpPartSchema)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#header(Object) header(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#headers(Object...) headers(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#headerPairs(Object...) headerPairs(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#accept(Object) accept(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#acceptCharset(Object) acceptCharset(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#acceptEncoding(Object) acceptEncoding(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#acceptLanguage(Object) acceptLanguage(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#authorization(Object) authorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#cacheControl(Object) cacheControl(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#clientVersion(Object) clientVersion(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#connection(Object) connection(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#contentLength(Object) contentLength(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#contentType(Object) contentType(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#date(Object) date(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#expect(Object) expect(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#forwarded(Object) forwarded(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#from(Object) from(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#host(Object) host(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ifMatch(Object) ifMatch(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ifModifiedSince(Object) ifModifiedSince(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ifNoneMatch(Object) ifNoneMatch(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ifRange(Object) ifRange(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ifUnmodifiedSince(Object) ifUnmodifiedSince(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#maxForwards(Object) maxForwards(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#origin(Object) origin(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#pragma(Object) pragma(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#proxyAuthorization(Object) proxyAuthorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#range(Object) proxyAuthorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#referer(Object) referer(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#te(Object) te(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#userAgent(Object) userAgent(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#upgrade(Object) upgrade(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#via(Object) via(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#warning(Object) warning(Object)}
	 * 			</ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestRequest}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#header(String,Object) header(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#header(EnumSet,String,Object) header(EnumSet&gt;AddFlag&gt;,String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#header(Object) header(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#header(EnumSet,Object) header(EnumSet&gt;AddFlag&gt;,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#headers(Object...) headers(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#headers(EnumSet,Object...) headers(EnumSet&gt;AddFlag&gt;,Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#headerPairs(Object...) headerPairs(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#accept(Object) accept(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#acceptCharset(Object) acceptCharset(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#acceptEncoding(Object) acceptEncoding(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#acceptLanguage(Object) acceptLanguage(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#authorization(Object) authorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#cacheControl(Object) cacheControl(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#clientVersion(Object) clientVersion(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#connection(Object) connection(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#contentLength(Object) contentLength(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#contentType(Object) contentType(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#date(Object) date(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#expect(Object) expect(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#forwarded(Object) forwarded(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#from(Object) from(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#host(Object) host(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ifMatch(Object) ifMatch(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ifModifiedSince(Object) ifModifiedSince(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ifNoneMatch(Object) ifNoneMatch(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ifRange(Object) ifRange(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ifUnmodifiedSince(Object) ifUnmodifiedSince(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#maxForwards(Object) maxForwards(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#origin(Object) origin(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#pragma(Object) pragma(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#proxyAuthorization(Object) proxyAuthorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#range(Object) proxyAuthorization(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#referer(Object) referer(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#te(Object) te(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#userAgent(Object) userAgent(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#upgrade(Object) upgrade(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#via(Object) via(Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#warning(Object) warning(Object)}
	 * 			</ul>
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Headers to add to every request.
	 */
	public static final String RESTCLIENT_headers = PREFIX + "headers.lo";

	/**
	 * Configuration property:  Ignore errors.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_ignoreErrors RESTCLIENT_ignoreErrors}
	 * 	<li><b>Name:</b>  <js>"RestClient.ignoreErrors.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>Default:</b>  <jk>false</jk> (<jk>true</jk> for <l>MockRestClient</l>)
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ignoreErrors()}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#ignoreErrors(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#ignoreErrors()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * When enabled, HTTP error response codes (e.g. <l>&gt;=400</l>) will not cause a {@link RestCallException} to
	 * be thrown.
	 * <p>
	 * Note that this is equivalent to <c>builder.errorCodes(x -&gt; <jk>false</jk>);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that doesn't throws a RestCallException when a 500 error occurs.</jc>
	 * 	RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreErrors()
	 * 		.build()
	 * 		.get(<js>"/error"</js>)  <jc>// Throws a 500 error</jc>
	 * 		.run()
	 * 		.assertStatus().is(500);
	 * </p>
	 */
	public static final String RESTCLIENT_ignoreErrors = PREFIX + "ignoreErrors.b";

	/**
	 * Configuration property:  Call interceptors.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_interceptors RESTCLIENT_interceptors}
	 * 	<li><b>Name:</b>  <js>"RestClient.interceptors.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Class&lt;{@link org.apache.juneau.rest.client2.RestCallInterceptor}&gt; | {@link org.apache.juneau.rest.client2.RestCallInterceptor}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#interceptors(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#interceptors(RestCallInterceptor...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds an interceptor that can be called to hook into specified events in the lifecycle of a single request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *   <jc>// Customized interceptor (note you can also extend from BasicRestCallInterceptor as well.</jc>
	 * 	<jk>public class</jk> MyRestCallInterceptor <jk>implements</jk> RestCallInterceptor {
	 *
	 * 		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 * 		<jk>public void</jk> init(RestRequest req) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
	 *			// set on the request from the client.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// HttpRequestInterceptor</jc>
	 *		<jk>public void</jk> process(HttpRequest request, HttpContext context) {
	 *			<jc>// Intercept before the request is sent to the server.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *		<jk>public void</jk> connect(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after an HTTP response has been received.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// HttpResponseInterceptor</jc>
	 *		<jk>public void</jk> process(HttpResponse response, HttpContext context) <jk>throws</jk> HttpException, IOException {
	 *			<jc>// Intercept before the message body is evaluated.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *		<jk>public void</jk> close(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jc>// Intercept when the response body is consumed.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a client with a customized interceptor.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.interceptors(MyRestCallInterceptor.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_interceptors = PREFIX + "interceptors.lo";

	/**
	 * Add to the Call interceptors property.
	 */
	public static final String RESTCLIENT_interceptors_add = PREFIX + "interceptors.so/add";

	/**
	 * Configuration property:  Keep HttpClient open.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_keepHttpClientOpen RESTCLIENT_keepHttpClientOpen}
	 * 	<li><b>Name:</b>  <js>"RestClient.keepHttpClientOpen.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestClient.keepHttpClientOpen</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_KEEPHTTPCLIENTOPEN</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#keepHttpClientOpen()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized client and don't close the client  service.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClient(myHttpClient)
	 * 		.keepHttpClientOpen()
	 * 		.build();
	 *
	 * 	client.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 */
	public static final String RESTCLIENT_keepHttpClientOpen = PREFIX + "keepHttpClientOpen.b";

	/**
	 * Configuration property:  Enable leak detection.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_leakDetection RESTCLIENT_leakDetection}
	 * 	<li><b>Name:</b>  <js>"RestClient.leakDetection.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestClient.leakDetection</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_LEAKDETECTION</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#leakDetection()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enable client and request/response leak detection.
	 *
	 * <p>
	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
	 * when the <c>finalize</c> methods are invoked.
	 *
	 * <p>
	 * Automatically enabled with {@link Context#CONTEXT_debug}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that logs a message if </jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.leakDetection()
	 * 		.logToConsole()  <jc>// Also log the error message to System.err</jc>
	 * 		.build();
	 *
	 * 	client.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 */
	public static final String RESTCLIENT_leakDetection = PREFIX + "leakDetection.b";

	/**
	 * Configuration property:  Logger.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logger RESTCLIENT_logger}
	 * 	<li><b>Name:</b>  <js>"RestClient.logger.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link java.util.logging.Logger}
	 * 		</ul>
	 * 	<li><b>Default:</b>  <c>Logger.<jsm>getLogger</jsm>(RestClient.<jk>class</jk>.getName())</c>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#logger(Logger)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the logger to use for logging.
	 */
	public static final String RESTCLIENT_logger = PREFIX + "logger.o";

	/**
	 * Configuration property:  Log to console.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logToConsole RESTCLIENT_logToConsole}
	 * 	<li><b>Name:</b>  <js>"RestClient.logToConsole.b"</js>
	 * 	<li><b>System property:</b>  <c>RestClient.logToConsole</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_LOGTOCONSOLE</c>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><jk>boolean</jk>
	 * 		</ul>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#logToConsole()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, log messages are sent to the console in addition to the existing logger.
	 */
	public static final String RESTCLIENT_logToConsole = PREFIX + "logToConsole.b";

	/**
	 * Configuration property:  Log requests.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logRequests RESTCLIENT_logRequests}
	 * 	<li><b>Name:</b>  <js>"RestClient.logRequests.s"</js>
	 * 	<li><b>System property:</b>  <c>RestClient.logRequests</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_LOGREQUESTS</c>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.DetailLevel}
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.DetailLevel#NONE}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#logRequests(DetailLevel,Level)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Causes requests/responses to be logged at the specified log level at the end of the request.
	 *
	 * <p>
	 * <jsf>SIMPLE</jsf> detail produces a log message like the following:
	 * <p class='bcode w800 console'>
	 * 	POST http://localhost:10000/testUrl, HTTP/1.1 200 OK
	 * </p>
	 *
	 * <p>
	 * <jsf>FULL</jsf> detail produces a log message like the following:
	 * <p class='bcode w800 console'>
	 * 	=== HTTP Call (outgoing) =======================================================
	 * 	=== REQUEST ===
	 * 	POST http://localhost:10000/testUrl
	 * 	---request headers---
	 * 		Debug: true
	 * 		No-Trace: true
	 * 		Accept: application/json
	 * 	---request entity---
	 * 		Content-Type: application/json
	 * 	---request content---
	 * 	{"foo":"bar","baz":123}
	 * 	=== RESPONSE ===
	 * 	HTTP/1.1 200 OK
	 * 	---response headers---
	 * 		Content-Type: application/json;charset=utf-8
	 * 		Content-Length: 21
	 * 		Server: Jetty(8.1.0.v20120127)
	 * 	---response content---
	 * 	{"message":"OK then"}
	 * 	=== END ========================================================================
	 * </p>
	 *
	 * <p>
	 * By default, the message is logged to the default logger.  It can be logged to a different logger via the
	 * {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logger} setting or logged to the console using the
	 * {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logToConsole} setting.
	 */
	public static final String RESTCLIENT_logRequests = PREFIX + "logRequests.s";

	/**
	 * Configuration property:  Log requests log level.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logRequestsLevel RESTCLIENT_logRequestsLevel}
	 * 	<li><b>Name:</b>  <js>"RestClient.logRequestsLevel.s"</js>
	 * 	<li><b>System property:</b>  <c>RestClient.logRequestsLevel</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_LOGREQUESTLEVEL</c>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link java.util.logging.Level}
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link java.util.logging.Level#INFO}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#logRequests(DetailLevel,Level)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used in combination with {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logRequests} to specify the
	 * level to log request messages to.
	 */
	public static final String RESTCLIENT_logRequestsLevel = PREFIX + "logRequestsLevel.s";

	/**
	 * Configuration property:  Log requests predicate.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logRequestsPredicate RESTCLIENT_logRequestsPredicate}
	 * 	<li><b>Name:</b>  <js>"RestClient.logRequestsPredicate.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link java.util.function.BiPredicate}&lt;{@link org.apache.juneau.rest.client2.RestRequest},{@link org.apache.juneau.rest.client2.RestResponse}&gt;
	 * 		</ul>
	 * 	<li><b>Default:</b>  <c>(req,res) -&gt; true</c>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#logRequests(DetailLevel,Level)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used in combination with {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_logRequests} to specify the
	 * level to log request messages to.
	 */
	public static final String RESTCLIENT_logRequestsPredicate = PREFIX + "logRequestsPredicate.o";

	/**
	 * Configuration property:  Parsers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_parsers RESTCLIENT_parsers}
	 * 	<li><b>Name:</b>  <js>"RestClient.parsers.lo"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.parser.Parser}&gt;</c>
	 * 			<li>{@link org.apache.juneau.parser.Parser}
	 * 		</ul>
	 * 	<li><b>Default:</b>  No parsers.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#parser(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#parser(Parser)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#parsers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#parsers(Parser...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates the specified {@link Parser Parsers} with the HTTP client.
	 *
	 * <p>
	 * The parsers are used to parse the HTTP response body into a POJO.
	 *
	 * <p>
	 * The parser that best matches the <c>Accept</c> header will be used to parse the response body.
	 * <br>If no <c>Accept</c> header is specified, the first parser in the list will be used.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>)
	 * 		.strict()  <jc>// Enable strict mode on parsers.</jc>
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_parsers = PREFIX + "parsers.lo";

	/**
	 * Configuration property:  Part parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_partParser RESTCLIENT_partParser}
	 * 	<li><b>Name:</b>  <js>"RestClient.partParser.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.httppart.HttpPartParser}&gt;</c>
	 * 			<li>{@link org.apache.juneau.httppart.HttpPartParser}
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiParser};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#partParser(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part parser is {@link OpenApiParser} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for incoming HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partParser(UonParser.<js>class</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_partParser = PREFIX + "partParser.o";

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_partSerializer RESTCLIENT_partSerializer}
	 * 	<li><b>Name:</b>  <js>"RestClient.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.httppart.HttpPartSerializer}&gt;</c>
	 * 			<li>{@link org.apache.juneau.httppart.HttpPartSerializer}
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiSerializer};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#partSerializer(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part serializer is {@link OpenApiSerializer} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for outgoing HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partSerializer(UonSerializer.<js>class</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_partSerializer = PREFIX + "partSerializer.o";

	/**
	 * Configuration property:  Request query parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_query RESTCLIENT_query}
	 * 	<li><b>Name:</b>  <js>"RestClient.query.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.http.NameValuePair}&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestClientBuilder}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#query(Object...) query(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#query(String,Object) query(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#query(String,Object,HttpPartSchema,HttpPartSerializer) query(String,Object,HttpPartSerializer,HttpPartSchema)}
	 * 			</ul>
	 * 			<li class='jc'>{@link org.apache.juneau.rest.client2.RestRequest}
	 * 			<ul>
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#query(String,Object) query(String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#query(EnumSet,String,Object) query(EnumSet&lt;AddFlag&gt;,String,Object)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#query(Object...) query(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#query(EnumSet,Object...) query(EnumSet&lt;AddFlag&gt;,Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#queryPairs(Object...) queryPairs(Object...)}
	 * 				<li class='jm'>{@link org.apache.juneau.rest.client2.RestRequest#queryCustom(Object) queryCustom(Object)}
	 * 			</ul>
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Query parameters to add to every request.
	 */
	public static final String RESTCLIENT_query = PREFIX + "query.lo";

	/**
	 * Configuration property:  Root URI.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_rootUri RESTCLIENT_rootUri}
	 * 	<li><b>Name:</b>  <js>"RestClient.rootUri.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestClient.rootUri</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCLIENT_ROOTURI</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#rootUrl(Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When set, relative URL strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUri(<js>"http://localhost:10000/foo"</js>)
	 * 		.build();
	 *
	 * 	Bar bar = client
	 * 		.get(<js>"/bar"</js>)  // Relative to http://localhost:10000/foo
	 * 		.run()
	 * 		.getBody().as(Bar.<jk>class</jk>);
	 * </p>
	 */
	public static final String RESTCLIENT_rootUri = PREFIX + "rootUri.s";

	/**
	 * Configuration property:  Serializers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.client2.RestClient#RESTCLIENT_serializers RESTCLIENT_serializers}
	 * 	<li><b>Name:</b>  <js>"RestClient.serializers.lo"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li><c>Class&lt;? <jk>extends</jk> {@link org.apache.juneau.serializer.Serializer}&gt;</c>
	 * 			<li>{@link org.apache.juneau.serializer.Serializer}
	 * 		</ul>
	 * 	<li><b>Default:</b>  No serializers.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#serializer(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#serializer(Serializer)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#serializers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.client2.RestClientBuilder#serializers(Serializer...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates the specified {@link Serializer Serializers} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <p>
	 * The serializer that best matches the <c>Content-Type</c> header will be used to serialize the request body.
	 * <br>If no <c>Content-Type</c> header is specified, the first serializer in the list will be used.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for request bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializers(JsonSerializer.<jk>class</jk>,XmlSerializer.<jk>class</jk>)
	 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
	 * 		.build();
	 * </p>
	 */
	public static final String RESTCLIENT_serializers = PREFIX + "serializers.lo";

	static final String RESTCLIENT_httpClient = PREFIX + "httpClient.o";
	static final String RESTCLIENT_httpClientBuilder= PREFIX + "httpClientBuilder.o";

	private static final Set<String> NO_BODY_METHODS = ASet.unmodifiable("GET","HEAD","DELETE","CONNECT","OPTIONS","TRACE");

	private final List<Object> headers, query, formData;
	final CloseableHttpClient httpClient;
	private final boolean keepHttpClientOpen, leakDetection;
	private final UrlEncodingSerializer urlEncodingSerializer;  // Used for form posts only.
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final RestCallHandler callHandler;
	private final String rootUrl;
	private volatile boolean isClosed = false;
	private final StackTraceElement[] creationStack;
	private final Logger logger;
	final DetailLevel logRequests;
	final BiPredicate<RestRequest,RestResponse> logRequestsPredicate;
	final Level logRequestsLevel;
	final boolean ignoreErrors;
	private final boolean logToConsole;
	private StackTraceElement[] closedStack;
	private static final ConcurrentHashMap<Class<?>,Context> requestContexts = new ConcurrentHashMap<>();

	// These are read directly by RestCall.
	final SerializerGroup serializers;
	final ParserGroup parsers;
	Predicate<Integer> errorCodes;

	final RestCallInterceptor[] interceptors;

	// This is lazy-created.
	private volatile ExecutorService executorService;
	private final boolean executorServiceShutdownOnClose;

	/**
	 * Instantiates a new clean-slate {@link RestClientBuilder} object.
	 *
	 * @return A new {@link RestClientBuilder} object.
	 */
	public static RestClientBuilder create() {
		return new RestClientBuilder(PropertyStore.DEFAULT);
	}

	@Override /* Context */
	public RestClientBuilder builder() {
		return new RestClientBuilder(getPropertyStore());
	}

	private static final
		Predicate<Integer> ERROR_CODES_DEFAULT = x ->  x>=400;

	private static final
		BiPredicate<RestRequest,RestResponse> LOG_REQUESTS_PREDICATE_DEFAULT = (req,res) -> true;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing the unmodifiable configuration for this client.
	 */
	@SuppressWarnings("unchecked")
	protected RestClient(PropertyStore ps) {
		super(ps);
		this.httpClient = getInstanceProperty(RESTCLIENT_httpClient, CloseableHttpClient.class, null);
		this.keepHttpClientOpen = getBooleanProperty(RESTCLIENT_keepHttpClientOpen, false);
		this.errorCodes = getInstanceProperty(RESTCLIENT_errorCodes, Predicate.class, ERROR_CODES_DEFAULT);
		this.executorServiceShutdownOnClose = getBooleanProperty(RESTCLIENT_executorServiceShutdownOnClose, false);
		this.rootUrl = StringUtils.nullIfEmpty(getStringProperty(RESTCLIENT_rootUri, "").replaceAll("\\/$", ""));
		this.leakDetection = getBooleanProperty(RESTCLIENT_leakDetection, isDebug());
		this.ignoreErrors = getBooleanProperty(RESTCLIENT_ignoreErrors, false);
		this.logger = getInstanceProperty(RESTCLIENT_logger, Logger.class, Logger.getLogger(RestClient.class.getName()));
		this.logRequests = getInstanceProperty(RESTCLIENT_logRequests, DetailLevel.class, isDebug() ? DetailLevel.FULL : DetailLevel.NONE);
		this.logRequestsLevel = getInstanceProperty(RESTCLIENT_logRequestsLevel, Level.class, isDebug() ? Level.WARNING : Level.OFF);
		this.logToConsole = getBooleanProperty(RESTCLIENT_logToConsole, isDebug());
		this.logRequestsPredicate = getInstanceProperty(RESTCLIENT_logRequestsPredicate, BiPredicate.class, LOG_REQUESTS_PREDICATE_DEFAULT);

		SerializerGroupBuilder sgb = SerializerGroup.create();
		for (Object o : getArrayProperty(RESTCLIENT_serializers, Object.class)) {
			if (o instanceof Serializer) {
				sgb.append((Serializer)o);  // Don't apply PropertyStore.
			} else if (o instanceof Class) {
				Class<?> c = (Class<?>)o;
				if (! Serializer.class.isAssignableFrom(c))
					throw new ConfigException("RESTCLIENT_serializers property had invalid class of type ''{0}''", c.getName());
				sgb.append(ContextCache.INSTANCE.create((Class<? extends Serializer>)o, ps));
			} else {
				throw new ConfigException("RESTCLIENT_serializers property had invalid object of type ''{0}''", o.getClass().getName());
			}
		}
		this.serializers = sgb.build();

		ParserGroupBuilder pgb = ParserGroup.create();
		for (Object o : getArrayProperty(RESTCLIENT_parsers, Object.class)) {
			if (o instanceof Parser) {
				pgb.append((Parser)o);  // Don't apply PropertyStore.
			} else if (o instanceof Class) {
				Class<?> c = (Class<?>)o;
				if (! Parser.class.isAssignableFrom(c))
					throw new ConfigException("RESTCLIENT_parsers property had invalid class of type ''{0}''", c.getName());
				pgb.append(ContextCache.INSTANCE.create((Class<? extends Parser>)o, ps));
			} else {
				throw new ConfigException("RESTCLIENT_parsers property had invalid object of type ''{0}''", o.getClass().getName());
			}
		}
		this.parsers = pgb.build();

		this.urlEncodingSerializer = new SerializerBuilder(ps).build(UrlEncodingSerializer.class);
		this.partSerializer = getInstanceProperty(RESTCLIENT_partSerializer, HttpPartSerializer.class, OpenApiSerializer.class, ResourceResolver.FUZZY, ps);
		this.partParser = getInstanceProperty(RESTCLIENT_partParser, HttpPartParser.class, OpenApiParser.class, ResourceResolver.FUZZY, ps);
		this.executorService = getInstanceProperty(RESTCLIENT_executorService, ExecutorService.class, null);

		HttpPartSerializerSession partSerializerSession = partSerializer.createPartSession(null);

		Function<Object,Object> f = x -> x instanceof SerializedNameValuePair.Builder ? ((SerializedNameValuePair.Builder)x).serializer(partSerializerSession, false).build() : x;

		this.headers = Collections.unmodifiableList(
			getListProperty(RESTCLIENT_headers, Object.class)
				.stream()
				.map(f)
				.collect(Collectors.toList())
		);

		this.query = Collections.unmodifiableList(
			getListProperty(RESTCLIENT_query, Object.class)
				.stream()
				.map(f)
				.collect(Collectors.toList())
		);

		this.formData = Collections.unmodifiableList(
			getListProperty(RESTCLIENT_formData, Object.class)
				.stream()
				.map(f)
				.collect(Collectors.toList())
		);

		this.callHandler = getInstanceProperty(RESTCLIENT_callHandler, RestCallHandler.class, BasicRestCallHandler.class, ResourceResolver.FUZZY, ps, this);

		this.interceptors = getInstanceArrayProperty(RESTCLIENT_interceptors, RestCallInterceptor.class, new RestCallInterceptor[0]);

		creationStack = isDebug() ? Thread.currentThread().getStackTrace() : null;
	}

	/**
	 * Returns <jk>true</jk> if specified http method has content.
	 * <p>
	 * By default, anything not in this list can have content:  <c>GET, HEAD, DELETE, CONNECT, OPTIONS, TRACE</c>.
	 *
	 * @param httpMethod The HTTP method.  Must be upper-case.
	 * @return <jk>true</jk> if specified http method has content.
	 */
	protected boolean hasContent(String httpMethod) {
		return ! NO_BODY_METHODS.contains(httpMethod);
	}

	/**
	 * Calls {@link CloseableHttpClient#close()} on the underlying {@link CloseableHttpClient}.
	 *
	 * <p>
	 * It's good practice to call this method after the client is no longer used.
	 *
	 * @throws IOException Thrown by underlying stream.
	 */
	@Override
	public void close() throws IOException {
		isClosed = true;
		if (! keepHttpClientOpen)
			httpClient.close();
		if (executorService != null && executorServiceShutdownOnClose)
			executorService.shutdown();
		if (creationStack != null)
			closedStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Same as {@link #close()}, but ignores any exceptions.
	 */
	public void closeQuietly() {
		isClosed = true;
		try {
			if (! keepHttpClientOpen)
				httpClient.close();
			if (executorService != null && executorServiceShutdownOnClose)
				executorService.shutdown();
		} catch (Throwable t) {}
		if (creationStack != null)
			closedStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Execute the specified no-body request (e.g. GET/DELETE).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * <p>
	 * The behavior of this method can also be modified by specifying a different {@link RestCallHandler}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * 	<li class='jm'>{@link RestClientBuilder#callHandler(Class)}
	 * 	<li class='jm'>{@link RestClientBuilder#callHandler(RestCallHandler)}
	 * </ul>
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* RestCallHandler */
	public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
		return callHandler.execute(target, request, context);
	}

	/**
	 * Execute the specified body request (e.g. POST/PUT).
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * <p>
	 * The behavior of this method can also be modified by specifying a different {@link RestCallHandler}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * 	<li class='jm'>{@link RestClientBuilder#callHandler(Class)}
	 * 	<li class='jm'>{@link RestClientBuilder#callHandler(RestCallHandler)}
	 * </ul>
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* RestCallHandler */
	public HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
		return callHandler.execute(target, request, context);
	}

	/**
	 * Perform a <c>GET</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest get(Object url) throws RestCallException {
		return request("GET", url, false);
	}

	/**
	 * Perform a <c>GET</c> request against the root URL.
	 *
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest get() throws RestCallException {
		return request("GET", null, false);
	}

	/**
	 * Perform a <c>PUT</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 		<li class='jc'>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest put(Object url, Object body) throws RestCallException {
		return request("PUT", url, true).body(body);
	}

	/**
	 * Perform a <c>PUT</c> request against the specified URL using a plain text body bypassing the serializer.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request bypassing the serializer.
	 * @param contentType The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest put(Object url, String body, String contentType) throws RestCallException {
		return request("PUT", url, true).bodyString(body).contentType(contentType);
	}

	/**
	 * Same as {@link #put(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestRequest#body(Object)} or {@link RestRequest#formData(String, Object)}
	 * to set the contents on the result object.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest put(Object url) throws RestCallException {
		return request("PUT", url, true);
	}

	/**
	 * Perform a <c>POST</c> request against the specified URL.
	 *
	 * <ul class='notes'>
	 * 	<li>Use {@link #formPost(Object, Object)} for <c>application/x-www-form-urlencoded</c> form posts.
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 		<li class='jc'>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest post(Object url, Object body) throws RestCallException {
		return request("POST", url, true).body(body);
	}

	/**
	 * Perform a <c>POST</c> request against the specified URL as a plain text body bypassing the serializer.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request bypassing the serializer.
	 * @param contentType
	 * 	The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest post(Object url, String body, String contentType) throws RestCallException {
		return request("POST", url, true).bodyString(body).contentType(contentType);
	}

	/**
	 * Same as {@link #post(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestRequest#body(Object)} or {@link RestRequest#formData(String, Object)} to set the
	 * contents on the result object.
	 *
	 * <ul class='notes'>
	 * 	<li>Use {@link #formPost(Object, Object)} for <c>application/x-www-form-urlencoded</c> form posts.
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest post(Object url) throws RestCallException {
		return request("POST", url, true);
	}

	/**
	 * Perform a <c>DELETE</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest delete(Object url) throws RestCallException {
		return request("DELETE", url, false);
	}

	/**
	 * Perform an <c>OPTIONS</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest options(Object url) throws RestCallException {
		return request("OPTIONS", url, true);
	}

	/**
	 * Perform a <c>HEAD</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest head(Object url) throws RestCallException {
		return request("HEAD", url, false);
	}

	/**
	 * Perform a <c>POST</c> request with a content type of <c>application/x-www-form-urlencoded</c>
	 * against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link NameValuePair} - URL-encoded as a single name-value pair.
	 * 		<li class='jc'>{@link NameValuePair} array - URL-encoded as name value pairs.
	 * 		<li class='jc'>{@link NameValuePairs} - URL-encoded as name value pairs.
	 * 		<li class='jc'>{@link Reader}/{@link InputStream}- Streamed directly and <l>Content-Type</l> set to <js>"application/x-www-form-urlencoded"</js>
	 * 		<li class='jc'>{@link ReaderResource}/{@link ReaderResourceBuilder}/{@link StreamResource}/{@link StreamResourceBuilder}/{@link HttpEntity}- Streamed directly and <l>Content-Type</l> set to <js>"application/x-www-form-urlencoded"</js> if not already specified on the entity.
	 * 		<li class='jc'>{@link Object} - Converted to a {@link SerializedHttpEntity} using {@link UrlEncodingSerializer} to serialize.
	 * 		<li class='jc'>{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPost(Object url, Object body) throws RestCallException {
		RestRequest req = request("POST", url, true);
		try {
			if (body instanceof Supplier)
				body = ((Supplier<?>)body).get();
			if (body instanceof NameValuePair)
				return req.body(new UrlEncodedFormEntity(AList.of((NameValuePair)body)));
			if (body instanceof NameValuePair[])
				return req.body(new UrlEncodedFormEntity(Arrays.asList((NameValuePair[])body)));
			if (body instanceof NameValuePairs)
				return req.body(new UrlEncodedFormEntity((NameValuePairs)body));
			if (body instanceof HttpEntity) {
				HttpEntity e = (HttpEntity)body;
				if (e.getContentType() == null)
					req.contentType("application/x-www-form-urlencoded");
				return req.body(e);
			}
			if (body instanceof Reader || body instanceof InputStream)
				return req.contentType("application/x-www-form-urlencoded").body(body);
			if (body instanceof ReaderResourceBuilder)
				body = ((ReaderResourceBuilder)body).build();
			if (body instanceof ReaderResource) {
				ReaderResource r = (ReaderResource)body;
				if (r.getContentType() == null)
					req.contentType("application/x-www-form-urlencoded");
				return req.body(r);
			}
			if (body instanceof StreamResourceBuilder)
				body = ((StreamResourceBuilder)body).build();
			if (body instanceof StreamResource) {
				StreamResource r = (StreamResource)body;
				if (r.getContentType() == null)
					req.contentType("application/x-www-form-urlencoded");
				return req.body(r);
			}
			return req.body(new SerializedHttpEntity(body, urlEncodingSerializer, null, null));
		} catch (IOException e) {
			throw new RestCallException(e);
		}
	}

	/**
	 * Same as {@link #formPost(Object, Object)} but doesn't specify the input yet.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPost(Object url) throws RestCallException {
		return request("POST", url, true);
	}

	/**
	 * Perform a <c>POST</c> request with a content type of <c>application/x-www-form-urlencoded</c>
	 * against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param parameters
	 * 	The parameters of the form post.
	 * 	<br>The parameters represent name/value pairs and must be an even number of arguments.
	 * 	<br>Parameters are converted to {@link BasicNameValuePair} objects.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPostPairs(Object url, Object...parameters) throws RestCallException {
		return formPost(url, new NameValuePairs(parameters));
	}

	/**
	 * Perform a <c>PATCH</c> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 		<li class='jc'>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest patch(Object url, Object body) throws RestCallException {
		return request("PATCH", url, true).body(body);
	}

	/**
	 * Perform a <c>PATCH</c> request against the specified URL as a plain text body bypassing the serializer.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URL as the body of the request bypassing the serializer.
	 * @param contentType
	 * 	The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest patch(Object url, String body, String contentType) throws RestCallException {
		return request("PATCH", url, true).bodyString(body).contentType(contentType);
	}

	/**
	 * Same as {@link #patch(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call {@link RestRequest#body(Object)} to set the contents on the result object.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest patch(Object url) throws RestCallException {
		return request("PATCH", url, true);
	}


	/**
	 * Performs a REST call where the entire call is specified in a simple string.
	 *
	 * <p>
	 * This method is useful for performing callbacks when the target of a callback is passed in
	 * on an initial request, for example to signal when a long-running process has completed.
	 *
	 * <p>
	 * The call string can be any of the following formats:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"[method] [url]"</js> - e.g. <js>"GET http://localhost/callback"</js>
	 * 	<li>
	 * 		<js>"[method] [url] [payload]"</js> - e.g. <js>"POST http://localhost/callback some text payload"</js>
	 * 	<li>
	 * 		<js>"[method] [headers] [url] [payload]"</js> - e.g. <js>"POST {'Content-Type':'text/json'} http://localhost/callback {'some':'json'}"</js>
	 * </ul>
	 * <p>
	 * The payload will always be sent using a simple {@link StringEntity}.
	 *
	 * @param callString The call string.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest callback(String callString) throws RestCallException {
		callString = emptyIfNull(callString);

		// S01 - Looking for end of method.
		// S02 - Found end of method, looking for beginning of URL or headers.
		// S03 - Found beginning of headers, looking for end of headers.
		// S04 - Found end of headers, looking for beginning of URL.
		// S05 - Found beginning of URL, looking for end of URL.

		StateMachineState state = S01;

		int mark = 0;
		String method = null, headers = null, uri = null, content = null;
		for (int i = 0; i < callString.length(); i++) {
			char c = callString.charAt(i);
			if (state == S01) {
				if (isWhitespace(c)) {
					method = callString.substring(mark, i);
					state = S02;
				}
			} else if (state == S02) {
				if (! isWhitespace(c)) {
					mark = i;
					if (c == '{')
						state = S03;
					else
						state = S05;
				}
			} else if (state == S03) {
				if (c == '}') {
					headers = callString.substring(mark, i+1);
					state = S04;
				}
			} else if (state == S04) {
				if (! isWhitespace(c)) {
					mark = i;
					state = S05;
				}
			} else /* (state == S05) */ {
				if (isWhitespace(c)) {
					uri = callString.substring(mark, i);
					content = callString.substring(i).trim();
					break;
				}
			}
		}

		if (state != S05)
			throw new RestCallException("Invalid format for call string.  State=" + state);

		try {
			RestRequest req = request(method, uri, isNotEmpty(content));
			if (headers != null)
				for (Map.Entry<String,Object> e : OMap.ofJson(headers).entrySet())
					req.headerString(e.getKey(), e.getValue());
			if (isNotEmpty(content))
				req.bodyString(content);
			return req;
		} catch (ParseException e) {
			throw new RestCallException(e, "Invalid format for call string.");
		}
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The HTTP body content.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 		<li class='jc'>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * 	This parameter is IGNORED if {@link HttpMethod#hasContent()} is <jk>false</jk>.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(HttpMethod method, Object url, Object body) throws RestCallException {
		RestRequest rc = request(method.name(), url, method.hasContent());
		if (method.hasContent())
			rc.body(body);
		return rc;
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(HttpMethod method, Object url) throws RestCallException {
		RestRequest rc = request(method.name(), url, method.hasContent());
		return rc;
	}

	/**
	 * Perform a generic REST call.
	 *
	 * <p>
	 * Typically you're going to use {@link #request(HttpMethod, Object)} or {@link #request(HttpMethod, Object, Object)},
	 * but this method is provided to allow you to perform non-standard HTTP methods (e.g. HTTP FOO).
	 *
	 * @param method The method name (e.g. <js>"GET"</js>, <js>"OPTIONS"</js>).
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param hasBody Boolean flag indicating if the specified request has content associated with it.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(String method, Object url, boolean hasBody) throws RestCallException {
		if (isClosed) {
			Exception e2 = null;
			if (closedStack != null) {
				e2 = new Exception("Creation stack:");
				e2.setStackTrace(closedStack);
				throw new RestCallException(e2, "RestClient.close() has already been called.  This client cannot be reused.");
			}
			throw new RestCallException("RestClient.close() has already been called.  This client cannot be reused.  Closed location stack trace can be displayed by setting the system property 'org.apache.juneau.rest.client2.RestClient.trackCreation' to true.");
		}

		RestRequest req = createRequest(toURI(url), method.toUpperCase(Locale.ENGLISH), hasBody);

		for (Object o : headers)
			req.header(toHeader(o));

		for (Object o : query)
			req.query(toQuery(o));

		for (Object o : formData)
			req.formData(toFormData(o));

		onInit(req);

		return req;
	}

	/**
	 * Creates a {@link RestRequest} object from the specified {@link HttpRequest} object.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own specialized {@link RestRequest} objects.
	 *
	 * @param uri The target.
	 * @param method The HTTP method (uppercase).
	 * @param hasBody Whether this method has a request entity.
	 * @return A new {@link RestRequest} object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
		return new RestRequest(this, uri, method, hasBody);
	}

	/**
	 * Create a new proxy interface against a 3rd-party REST interface.
	 *
	 * <p>
	 * The URL to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link Remote#path() @Remote(path)} annotation on the interface (<c>remote-path</c>).
	 * 	<li>The {@link RestClientBuilder#rootUrl(Object) rootUrl} on the client (<c>root-url</c>).
	 * 	<li>The fully-qualified class name of the interface (<c>class-name</c>).
	 * </ul>
	 *
	 * <p>
	 * The URL calculation is as follows:
	 * <ul>
	 * 	<li><c>remote-path</c> - If remote path is absolute.
	 * 	<li><c>root-url/remote-path</c> - If remote path is relative and root-url has been specified.
	 * 	<li><c>root-url/class-name</c> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URL, a {@link RemoteMetadataException} is thrown.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> org.apache.foo;
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"http://hostname/resturl/myinterface1"</js>)
	 * 	<jk>public interface</jk> MyInterface1 { ... }
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myinterface2"</js>)
	 * 	<jk>public interface</jk> MyInterface2 { ... }
	 *
	 * 	<jk>public interface</jk> MyInterface3 { ... }
	 *
	 * 	<jc>// Resolves to "http://localhost/resturl/myinterface1"</jc>
	 * 	MyInterface1 i1 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.build()
	 * 		.getRemote(MyInterface1.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/myinterface2"</jc>
	 * 	MyInterface2 i2 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemote(MyInterface2.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/org.apache.foo.MyInterface3"</jc>
	 * 	MyInterface3 i3 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemote(MyInterface3.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRemote(Class<T> interfaceClass) {
		return getRemote(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRemote(Class)} except explicitly specifies the URL of the REST interface.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param rootUrl The URL of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRemote(Class<T> interfaceClass, Object rootUrl) {
		return getRemote(interfaceClass, rootUrl, null, null);
	}

	/**
	 * Same as {@link #getRemote(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
	 * </ul>

	 * @param interfaceClass The interface to create a proxy for.
	 * @param rootUrl The URL of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRemote(final Class<T> interfaceClass, Object rootUrl, final Serializer serializer, final Parser parser) {

		if (rootUrl == null)
			rootUrl = this.rootUrl;

		final String restUrl2 = trimSlashes(emptyIfNull(rootUrl));

		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final RemoteMeta rm = new RemoteMeta(interfaceClass);

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RemoteMethodMeta rmm = rm.getMethodMeta(method);

						if (rmm == null)
							throw new RuntimeException("Method is not exposed as a remote method.");

						String url = rmm.getFullPath();
						if (url.indexOf("://") == -1)
							url = restUrl2 + '/' + url;
						if (url.indexOf("://") == -1)
							throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote resource.");

						String httpMethod = rmm.getHttpMethod();
						HttpPartSerializerSession s = getPartSerializerSession();

						try {
							RestRequest rc = request(httpMethod, url, hasContent(httpMethod));

							rc.serializer(serializer);
							rc.parser(parser);

							for (RemoteMethodArg a : rmm.getPathArgs())
								rc.path(a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer(s));

							for (RemoteMethodArg a : rmm.getQueryArgs())
								rc.query(a.isSkipIfEmpty() ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer(s));

							for (RemoteMethodArg a : rmm.getFormDataArgs())
								rc.formData(a.isSkipIfEmpty() ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer(s));

							for (RemoteMethodArg a : rmm.getHeaderArgs())
								rc.header(a.isSkipIfEmpty() ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer(s));

							RemoteMethodArg ba = rmm.getBodyArg();
							if (ba != null)
								rc.body(args[ba.getIndex()], ba.getSchema());

							if (rmm.getRequestArgs().length > 0) {
								for (RemoteMethodBeanArg rmba : rmm.getRequestArgs()) {
									RequestBeanMeta rbm = rmba.getMeta();
									Object bean = args[rmba.getIndex()];
									if (bean != null) {
										for (RequestBeanPropertyMeta p : rbm.getProperties()) {
											Object val = p.getGetter().invoke(bean);
											HttpPartType pt = p.getPartType();
											HttpPartSerializerSession ps = p.getSerializer(s);
											String pn = p.getPartName();
											HttpPartSchema schema = p.getSchema();
											boolean sie = schema.isSkipIfEmpty();
											if (pt == PATH)
												rc.path(pn, val, schema, p.getSerializer(s));
											else if (val != null) {
												if (pt == QUERY)
													rc.query(sie ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, pn, val, schema, ps);
												else if (pt == FORMDATA)
													rc.formData(sie ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, pn, val, schema, ps);
												else if (pt == HEADER)
													rc.header(sie ? SKIP_IF_EMPTY_FLAGS : DEFAULT_FLAGS, pn, val, schema, ps);
												else if (pt == HttpPartType.BODY)
													rc.body(val, schema);
											}
										}
									}
								}
							}

							if (rmm.getOtherArgs().length > 0) {
								Object[] otherArgs = new Object[rmm.getOtherArgs().length];
								int i = 0;
								for (RemoteMethodArg a : rmm.getOtherArgs())
									otherArgs[i++] = args[a.getIndex()];
								rc.body(otherArgs);
							}

							RemoteMethodReturn rmr = rmm.getReturns();
							if (rmr.isFuture()) {
								return getExecutorService(true).submit(new Callable<Object>() {
									@Override
									public Object call() throws Exception {
										return executeRemote(rc, method, rmr);
									}
								});
							} else if (rmr.isCompletableFuture()) {
								CompletableFuture<Object> cf = new CompletableFuture<>();
								getExecutorService(true).submit(new Callable<Object>() {
									@Override
									public Object call() throws Exception {
										cf.complete(executeRemote(rc, method, rmr));
										return null;
									}
								});
								return cf;
							}

							return executeRemote(rc, method, rmr);

						} catch (RestCallException e) {
							// Try to throw original exception if possible.
							e.throwServerException(interfaceClass.getClassLoader(), rmm.getExceptions());
							throw new RuntimeException(e);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	Object executeRemote(RestRequest rc, Method method, RemoteMethodReturn rmr) throws RestCallException {
		if (rmr.getReturnValue() == RemoteReturn.NONE) {
			rc.complete();
			return null;
		} else if (rmr.getReturnValue() == RemoteReturn.STATUS) {
			rc.ignoreErrors();
			int returnCode = rc.complete().getStatusCode();
			Class<?> rt = method.getReturnType();
			if (rt == Integer.class || rt == int.class)
				return returnCode;
			if (rt == Boolean.class || rt == boolean.class)
				return returnCode < 400;
			throw new RestCallException("Invalid return type on method annotated with @RemoteMethod(returns=HTTP_STATUS).  Only integer and booleans types are valid.");
		} else if (rmr.getReturnValue() == RemoteReturn.BEAN) {
			rc.ignoreErrors();
			return rc.run().as(rmr.getResponseBeanMeta());
		} else {
			rc.ignoreErrors();
			Object v = rc.run().getBody().as(rmr.getReturnType());
			if (v == null && method.getReturnType().isPrimitive())
				v = ClassInfo.of(method.getReturnType()).getPrimitiveDefault();
			return v;
		}
	}

	/**
	 * Create a new Remote Interface against a {@link RemoteInterface @RemoteInterface}-annotated class.
	 *
	 * <p>
	 * Remote interfaces are interfaces exposed on the server side using either the <c>RrpcServlet</c>
	 * or <c>RRPC</c> REST methods.
	 *
	 * <p>
	 * The URL to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link Remote#path() @Remote(path)} annotation on the interface (<c>remote-path</c>).
	 * 	<li>The {@link RestClientBuilder#rootUrl(Object) rootUrl} on the client (<c>root-url</c>).
	 * 	<li>The fully-qualified class name of the interface (<c>class-name</c>).
	 * </ul>
	 *
	 * <p>
	 * The URL calculation is as follows:
	 * <ul>
	 * 	<li><c>remote-path</c> - If remote path is absolute.
	 * 	<li><c>root-url/remote-path</c> - If remote path is relative and root-url has been specified.
	 * 	<li><c>root-url/class-name</c> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URL, a {@link RemoteMetadataException} is thrown.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.restRPC}
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass) {
		return getRrpcInterface(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class)} except explicitly specifies the URL of the REST interface.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.restRPC}
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass, final Object restUrl) {
		return getRrpcInterface(interfaceClass, restUrl, null, null);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-rest-server.restRPC}
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRrpcInterface(final Class<T> interfaceClass, Object restUrl, final Serializer serializer, final Parser parser) {

		if (restUrl == null) {
			RemoteInterfaceMeta rm = new RemoteInterfaceMeta(interfaceClass, stringify(restUrl));
			String path = rm.getPath();
			if (path.indexOf("://") == -1) {
				if (rootUrl == null)
					throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote interface.");
				path = trimSlashes(rootUrl) + '/' + path;
			}
			restUrl = path;
		}

		final String restUrl2 = stringify(restUrl);

		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final RemoteInterfaceMeta rm = new RemoteInterfaceMeta(interfaceClass, restUrl2);

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RemoteInterfaceMethod rim = rm.getMethodMeta(method);

						if (rim == null)
							throw new RuntimeException("Method is not exposed as a remote method.");

						String url = rim.getUrl();

						try {
							RestRequest rc = request("POST", url, true).serializer(serializer).body(args);

							Object v = rc.run().getBody().as(method.getGenericReturnType());
							if (v == null && method.getReturnType().isPrimitive())
								v = ClassInfo.of(method.getReturnType()).getPrimitiveDefault();
							return v;

						} catch (RestCallException e) {
							// Try to throw original exception if possible.
							e.throwServerException(interfaceClass.getClassLoader(), method.getExceptionTypes());
							throw new RuntimeException(e);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (leakDetection && ! isClosed && ! keepHttpClientOpen) {
			StringBuilder sb = new StringBuilder("WARNING:  RestClient garbage collected before it was finalized.");  // NOT DEBUG
			if (creationStack != null) {
				sb.append("\nCreation Stack:");  // NOT DEBUG
				for (StackTraceElement e : creationStack)
					sb.append("\n\t" + e);  // NOT DEBUG
			}
			log(WARNING, sb.toString());
		}
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param t Thrown exception.  Can be <jk>null</jk>.
	 * @param msg The message.
	 * @param args Optional message arguments.
	 */
	protected void log(Level level, Throwable t, String msg, Object...args) {
		logger.log(level, t, msg(msg, args));
		if (logToConsole) {
			System.err.println(msg(msg, args).get());
			t.printStackTrace();
		}
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 */
	protected void log(Level level, String msg, Object...args) {
		logger.log(level, msg(msg, args));
		if (logToConsole)
			System.err.println(msg(msg, args).get());
	}

	private Supplier<String> msg(String msg, Object...args) {
		return ()->args.length == 0 ? msg : MessageFormat.format(msg, args);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// RestCallInterceptor methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Interceptor method called immediately after the RestRequest object is created and all headers/query/form-data has been copied from the client.
	 *
	 * <p>
	 * Subclasses can override this method to intercept the request and perform special modifications.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(Class...)}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	@Override
	public void onInit(RestRequest req) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onInit(req);
		} catch (Exception e) {
			throw RestCallException.create(e);
		}
	}

	/**
	 * Interceptor method called immediately after an HTTP response has been received.
	 *
	 * <p>
	 * Subclasses can override this method to intercept the response and perform special modifications.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(Class...)}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	@Override
	public void onConnect(RestRequest req, RestResponse res) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(req, res);
		} catch (Exception e) {
			throw RestCallException.create(e);
		}
	}

	/**
	 * Interceptor method called immediately after the RestRequest object is created and all headers/query/form-data has been set on the request from the client.
	 *
	 * <p>
	 * Subclasses can override this method to handle any cleanup operations.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(Class...)}
	 * 	<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	@Override
	public void onClose(RestRequest req, RestResponse res) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onClose(req, res);
		} catch (Exception e) {
			throw RestCallException.create(e);
		}
	}

	//------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClient.
	//------------------------------------------------------------------------------------------------

	/**
	 * Obtains the parameters for this client.
	 *
	 * These parameters will become defaults for all requests being executed with this client, and for the parameters of dependent objects in this client.
	 *
	 * @return The default parameters.
	 * @deprecated Use {@link RequestConfig}.
	 */
	@Deprecated
	@Override /* HttpClient */
	public HttpParams getParams() {
		return httpClient.getParams();
	}

	/**
	 * Obtains the connection manager used by this client.
	 *
	 * @return The connection manager.
	 * @deprecated Use {@link HttpClientBuilder}.
	 */
	@Deprecated
	@Override /* HttpClient */
	public ClientConnectionManager getConnectionManager() {
		return httpClient.getConnectionManager();
	}

	/**
	 * Executes HTTP request using the default context.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param request The request to execute.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
		return httpClient.execute(request);
	}

	/**
	 * Executes HTTP request using the given context.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
		return httpClient.execute(request, context);
	}

	/**
	 * Executes HTTP request using the default context.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @return The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
		return httpClient.execute(target, request);
	}

	/**
	 * Executes HTTP request using the given context.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * 	<li>The {@link #execute(HttpHost,HttpEntityEnclosingRequestBase,HttpContext)} and
	 * 		{@link #execute(HttpHost,HttpRequestBase,HttpContext)} methods have been provided as wrappers around this method.
	 * 		Subclasses can override these methods for handling requests with and without bodies separately.
	 * 	<li>The {@link RestCallHandler} interface can also be implemented to intercept this method.
	 * </ul>
	 *
	 * @param target The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 		target or by inspecting the request.
	 * @param request The request to execute.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return
	 * 	The response to the request.
	 * 	<br>This is always a final response, never an intermediate response with an 1xx status code.
	 * 	<br>Whether redirects or authentication challenges will be returned or handled automatically depends on the
	 * 		implementation and configuration of this client.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
		return httpClient.execute(target, request, context);
	}

	/**
	 * Executes HTTP request using the default context and processes the response using the given response handler.
	 *
 	 * <p>
	 * The content entity associated with the response is fully consumed and the underlying connection is released back
	 * to the connection manager automatically in all cases relieving individual {@link ResponseHandler ResponseHandlers}
	 * from having to manage resource deallocation internally.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param request The request to execute.
	 * @param responseHandler The response handler.
	 * @return Object returned by response handler.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
		return httpClient.execute(request, responseHandler);
	}

	/**
	 * Executes HTTP request using the given context and processes the response using the given response handler.
	 *
	 * <p>
	 * The content entity associated with the response is fully consumed and the underlying connection is released back
	 * to the connection manager automatically in all cases relieving individual {@link ResponseHandler ResponseHandlers}
	 * from having to manage resource deallocation internally.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param request The request to execute.
	 * @param responseHandler The response handler.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return The response object as generated by the response handler.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
		return httpClient.execute(request, responseHandler, context);
	}

	/**
	 * Executes HTTP request to the target using the default context and processes the response using the given response handler.
	 *
	 * <p>
	 * The content entity associated with the response is fully consumed and the underlying connection is released back
	 * to the connection manager automatically in all cases relieving individual {@link ResponseHandler ResponseHandlers}
	 * from having to manage resource deallocation internally.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param target
	 * 	The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default target or by inspecting the request.
	 * @param request The request to execute.
	 * @param responseHandler The response handler.
	 * @return The response object as generated by the response handler.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
		return httpClient.execute(target, request, responseHandler);
	}

	/**
	 * Executes a request using the default context and processes the response using the given response handler.
	 *
	 * <p>
	 * The content entity associated with the response is fully consumed and the underlying connection is released back
	 * to the connection manager automatically in all cases relieving individual {@link ResponseHandler ResponseHandlers}
	 * from having to manage resource deallocation internally.
	 *
	 * <ul class='notes'>
	 * 	<li>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * </ul>
	 *
	 * @param target
	 * 	The target host for the request.
	 * 	<br>Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default target or by inspecting the request.
	 * @param request The request to execute.
	 * @param responseHandler The response handler.
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return The response object as generated by the response handler.
	 * @throws IOException In case of a problem or the connection was aborted.
	 * @throws ClientProtocolException In case of an http protocol error.
	 */
	@Override /* HttpClient */
	public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
		return httpClient.execute(target, request, responseHandler, context);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	HttpPartSerializerSession getPartSerializerSession() {
		return partSerializer == null ? null : partSerializer.createPartSession(null);
	}

	HttpPartParserSession getPartParserSession() {
		return partParser == null ? null : partParser.createPartSession(null);
	}

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	URI toURI(Object url) throws RestCallException {
		try {
			if (url instanceof URI)
				return (URI)url;
			if (url instanceof URL)
				((URL)url).toURI();
			if (url instanceof URIBuilder)
				return ((URIBuilder)url).build();
			String s = url == null ? "" : url.toString();
			if (rootUrl != null && ! absUrlPattern.matcher(s).matches()) {
				if (s.isEmpty())
					s = rootUrl;
				else {
					StringBuilder sb = new StringBuilder(rootUrl);
					if (! s.startsWith("/"))
						sb.append('/');
					sb.append(s);
					s = sb.toString();
				}
			}
			s = fixUrl(s);
			return new URI(s);
		} catch (URISyntaxException e) {
			throw new RestCallException(e, "Invalid URL encountered:  " + url);  // Shouldn't happen.
		}
	}

	ExecutorService getExecutorService(boolean create) {
		if (executorService != null || ! create)
			return executorService;
		synchronized(this) {
			if (executorService == null)
				executorService = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
			return executorService;
		}
	}

	/*
	 * Returns the serializer that best matches the specified content type.
	 * If no match found or the content type is null, returns the serializer in the list if it's a list of one.
	 * Returns null if no serializers are defined.
	 */
	Serializer getMatchingSerializer(String mediaType) {
		if (serializers.isEmpty())
			return null;
		if (mediaType != null) {
			Serializer s = serializers.getSerializer(mediaType);
			if (s != null)
				return s;
		}
		List<Serializer> l = serializers.getSerializers();
		return l.size() == 1 ? l.get(0) : null;
	}

	/*
	 * Returns the parser that best matches the specified content type.
	 * If no match found or the content type is null, returns the parser in the list if it's a list of one.
	 * Returns null if no parsers are defined.
	 */
	Parser getMatchingParser(String mediaType) {
		if (parsers.isEmpty())
			return null;
		if (mediaType != null) {
			Parser p = parsers.getParser(mediaType);
			if (p != null)
				return p;
		}
		List<Parser> l = parsers.getParsers();
		return l.size() == 1 ? l.get(0) : null;
	}

	@SuppressWarnings("unchecked")
	<T extends Context> T getInstance(Class<T> c) {
		Context o = requestContexts.get(c);
		if (o == null) {
			o = ContextCache.INSTANCE.create(c, getPropertyStore());
			requestContexts.put(c, o);
		}
		return (T)o;
	}

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("RestClient", new DefaultFilteringOMap()
				.a("errorCodes", errorCodes)
				.a("executorService", executorService)
				.a("executorServiceShutdownOnClose", executorServiceShutdownOnClose)
				.a("headers", headers)
				.a("interceptors", interceptors)
				.a("keepHttpClientOpen", keepHttpClientOpen)
				.a("partParser", partParser)
				.a("partSerializer", partSerializer)
				.a("query", query)
				.a("rootUri", rootUrl)
			);
	}
}
