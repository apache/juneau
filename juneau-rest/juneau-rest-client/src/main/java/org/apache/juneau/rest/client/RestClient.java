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
package org.apache.juneau.rest.client;

import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.rest.client.RestOperation.*;
import static java.util.logging.Level.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StateMachineState.*;
import static java.lang.Character.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.client.assertion.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Utility class for interfacing with remote REST interfaces.
 *
 * <p>
 * Built upon the feature-rich Apache HttpClient library, the Juneau RestClient API adds support for fluent-style
 * REST calls and the ability to perform marshalling of POJOs to and from HTTP parts.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a basic REST client with JSON support and download a bean.</jc>
 * 	MyBean <jv>bean</jv> = RestClient.<jsm>create</jsm>()
 * 		.json5()
 * 		.build()
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.assertStatus().asCode().is(200)
 * 		.assertHeader(<js>"Content-Type"</js>).matchesSimple(<js>"application/json*"</js>)
 * 		.getContent().as(MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * Breaking apart the fluent call, we can see the classes being used:
 * <p class='bjava'>
 * 	RestClient.Builder <jv>builder</jv> = RestClient.<jsm>create</jsm>().json5();
 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
 * 	RestRequest <jv>req</jv> = <jv>client</jv>.get(<jsf>URI</jsf>);
 * 	RestResponse <jv>res</jv> = <jv>req</jv>.run();
 * 	RestResponseStatusLineAssertion <jv>statusLineAssertion</jv> = <jv>res</jv>.assertStatus();
 * 	FluentIntegerAssertion&lt;RestResponse&gt; <jv>codeAssertion</jv> = <jv>statusLineAssertion</jv>.asCode();
 * 	<jv>res</jv> = <jv>codeAssertion</jv>.is(200);
 * 	FluentStringAssertion&lt;RestResponse&gt; <jv>headerAssertion</jv> = <jv>res</jv>.assertHeader(<js>"Content-Type"</js>);
 * 	<jv>res</jv> = <jv>headerAssertion</jv>.matchesSimple(<js>"application/json*"</js>);
 * 	RestResponseBody <jv>content</jv> = <jv>res</jv>.getContent();
 * 	MyBean <jv>bean</jv> = <jv>content</jv>.as(MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * It additionally provides support for creating remote proxy interfaces using REST as the transport medium.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Define a Remote proxy for interacting with a REST interface.</jc>
 * 	<ja>@Remote</ja>(path=<js>"/petstore"</js>)
 * 	<jk>public interface</jk> PetStore {
 *
 * 		<ja>@RemotePost</ja>(<js>"/pets"</js>)
 * 		Pet addPet(
 * 			<ja>@Content</ja> CreatePet <jv>pet</jv>,
 * 			<ja>@Header</ja>(<js>"E-Tag"</js>) UUID <jv>etag</jv>,
 * 			<ja>@Query</ja>(<js>"debug"</js>) <jk>boolean</jk> <jv>debug</jv>
 * 		);
 * 	}
 *
 * 	<jc>// Use a RestClient with default JSON 5 support.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json5().build();
 *
 * 	PetStore <jv>store</jv> = <jv>client</jv>.getRemote(PetStore.<jk>class</jk>, <js>"http://localhost:10000"</js>);
 * 	CreatePet <jv>createPet</jv> = <jk>new</jk> CreatePet(<js>"Fluffy"</js>, 9.99);
 * 	Pet <jv>pet</jv> = <jv>store</jv>.addPet(<jv>createPet</jv>, UUID.<jsm>randomUUID</jsm>(), <jk>true</jk>);
 * </p>
 *
 * <p>
 * The classes are closely tied to Apache HttpClient, yet provide lots of additional functionality:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient} <jk>extends</jk> {@link HttpClient}, creates {@link RestRequest} objects.
 * 	<li class='jc'>{@link RestRequest} <jk>extends</jk> {@link HttpUriRequest}, creates {@link RestResponse} objects.
 * 	<li class='jc'>{@link RestResponse} creates {@link ResponseContent} and {@link ResponseHeader} objects.
 * 	<li class='jc'>{@link ResponseContent} <jk>extends</jk> {@link HttpEntity}
 * 	<li class='jc'>{@link ResponseHeader} <jk>extends</jk> {@link Header}
 * </ul>
 *
 *
 * <p>
 * Instances of this class are built using the {@link Builder} class which can be constructed using
 * the {@link #create() RestClient.create()} method as shown above.
 *
 * <p>
 * Clients are typically created with a root URI so that relative URIs can be used when making requests.
 * This is done using the {@link Builder#rootUrl(Object)} method.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a client where all URIs are relative to localhost.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json().rootUrl(<js>"http://localhost:5000"</js>).build();
 *
 * 	<jc>// Use relative paths.</jc>
 * 	String <jv>body</jv> = <jv>client</jv>.get(<js>"/subpath"</js>).run().getContent().asString();
 * </p>
 *
 * <p>
 * The {@link RestClient} class creates {@link RestRequest} objects using the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'>{@link RestClient#get(Object) get(uri)} / {@link RestClient#get() get()}
 * 		<li class='jm'>{@link RestClient#put(Object,Object) put(uri,body)} / {@link RestClient#put(Object) put(uri)}
 * 		<li class='jm'>{@link RestClient#post(Object) post(uri,body)} / {@link RestClient#post(Object) post(uri)}
 * 		<li class='jm'>{@link RestClient#patch(Object,Object) patch(uri,body)} / {@link RestClient#patch(Object) patch(uri)}
 * 		<li class='jm'>{@link RestClient#delete(Object) delete(uri)}
 * 		<li class='jm'>{@link RestClient#head(Object) head(uri)}
 * 		<li class='jm'>{@link RestClient#options(Object) options(uri)}
 * 		<li class='jm'>{@link RestClient#formPost(Object,Object) formPost(uri,body)} / {@link RestClient#formPost(Object) formPost(uri)}
 * 		<li class='jm'>{@link RestClient#formPostPairs(Object,String...) formPostPairs(uri,parameters...)}
 * 		<li class='jm'>{@link RestClient#request(String,Object,Object) request(method,uri,body)}
 * 	</ul>
 * </ul>
 *
 * <p>
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
 * <p>
 * The distinction between the two methods is that {@link RestRequest#complete() complete()} automatically consumes the response body and
 * {@link RestRequest#run() run()} does not.  Note that you must consume response bodies in order for HTTP connections to be freed up
 * for reuse!  The {@link InputStream InputStreams} returned by the {@link ResponseContent} object are auto-closing once
 * they are exhausted, so it is often not necessary to explicitly close them.
 *
 * <p>
 * The following examples show the distinction between the two calls:
 *
 * <p class='bjava'>
 * 	<jc>// Consuming the response, so use run().</jc>
 * 	String <jv>body</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().asString();
 *
 * 	<jc>// Only interested in response status code, so use complete().</jc>
 * 	<jk>int</jk> <jv>status</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).complete().getStatusCode();
 * </p>
 *
 *
 * <h4 class='topic'>POJO Marshalling</h4>
 *
 * <p>
 * By default, JSON support is provided for HTTP request and response bodies.
 * Other languages can be specified using any of the following builder methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#json() json()}
 * 		<li class='jm'>{@link Builder#json5() json5()}
 * 		<li class='jm'>{@link Builder#xml() xml()}
 * 		<li class='jm'>{@link Builder#html() html()}
 * 		<li class='jm'>{@link Builder#plainText() plainText()}
 * 		<li class='jm'>{@link Builder#msgPack() msgPack()}
 * 		<li class='jm'>{@link Builder#uon() uon()}
 * 		<li class='jm'>{@link Builder#urlEnc() urlEnc()}
 * 		<li class='jm'>{@link Builder#openApi() openApi()}
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a basic REST client with JSON 5 support.</jc>
 * 	<jc>// Typically easier to use when performing unit tests.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json5().build();
 * </p>
 *
 * <p>
 * Clients can also support multiple languages:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a REST client with support for multiple languages.</jc>
 * 	RestClient <jv>client1</jv> = RestClient.<jsm>create</jsm>().json().xml().openApi().build();
 *
 * 	<jc>// Create a REST client with support for all supported languages.</jc>
 * 	RestClient <jv>client2</jv> = RestClient.<jsm>create</jsm>().universal().build();
 * </p>
 *
 * <p>
 * When using clients with multiple language support, you must specify the <c>Content-Type</c> header on requests
 * with bodies to specify which serializer should be selected.
 *
 * <p class='bjava'>
 * 	<jc>// Create a REST client with support for multiple languages.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().universal().build();
 *
 * 	<jv>client</jv>
 * 		.post(<jsf>URI</jsf>, <jv>myBean</jv>)
 * 		.contentType(<js>"application/json"</js>)
 * 		.complete()
 * 		.assertStatus().is(200);
 * </p>
 *
 * <p>
 * Languages can also be specified per-request.
 *
 * <p class='bjava'>
 * 	<jc>// Create a REST client with no default languages supported.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().build();
 *
 * 	<jc>// Use JSON for this request.</jc>
 * 	<jv>client</jv>
 * 		.post(<jsf>URI</jsf>, <jv>myBean</jv>)
 * 		.json()
 * 		.complete()
 * 		.assertStatus().is(200);
 * </p>
 *
 *
 * <p>
 * The {@link Builder} class provides convenience methods for setting common serializer and parser
 * settings.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a basic REST client with JSON support.</jc>
 * 	<jc>// Use single-quotes and whitespace.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json().sq().ws().build();
 * </p>
 *
 * <p>
 * 	Other methods are also provided for specifying the serializers and parsers used for lower-level marshalling support:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#serializer(Serializer) serializer(Serializer)}
 * 		<li class='jm'>{@link Builder#parser(Parser) parser(Parser)}
 * 		<li class='jm'>{@link Builder#marshaller(Marshaller) marshaller(Marshaller)}
 * 	</ul>
 * </ul>
 *
 * <p>
 * HTTP parts (headers, query parameters, form data...) are serialized and parsed using the {@link HttpPartSerializer}
 * and {@link HttpPartParser} APIs.  By default, clients are configured to use {@link OpenApiSerializer} and
 * {@link OpenApiParser}.  These can be overridden using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#partSerializer(Class) partSerializer(Class&lt;? extends HttpPartSerializer>)}
 * 		<li class='jm'>{@link Builder#partParser(Class) partParser(Class&lt;? extends HttpPartParser>)}
 * 	</ul>
 * </ul>
 *
 *
 * <h4 class='topic'>Request Headers</h4>
 * <p>
 * Per-client or per-request headers can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#headers() headerData()}
 * 		<li class='jm'>{@link Builder#header(String,String) header(String,Object)}
 * 		<li class='jm'>{@link Builder#header(String,Supplier) header(String,Supplier&lt;?&gt;)}
 * 		<li class='jm'>{@link Builder#headers(Header...) headers(Header...)}
 * 		<li class='jm'>{@link Builder#headersDefault(Header...) defaultHeaders(Header...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#header(String,Object) header(String,Object)}
 * 		<li class='jm'>{@link RestRequest#headers(Header...) headers(Header...)}
 * 		<li class='jm'>{@link RestRequest#headersBean(Object) headersBean(Object)}
 * 		<li class='jm'>{@link RestRequest#headerPairs(String...) headerPairs(String...)}
 * 	</ul>
 * </ul>
 *
 * <p>
 * The supplier methods are particularly useful for header values whose values may change over time (such as <c>Authorization</c> headers
 * which may need to change every few minutes).
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a client that adds a dynamic Authorization header to every request.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().header(<js>"Authorization"</js>, ()-&gt;getMyAuthToken()).build();
 * </p>
 *
 * <p>
 * The {@link HttpPartSchema} API allows you to define OpenAPI schemas to POJO data structures on both requests
 * and responses.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a client that adds a header "Foo: bar|baz" to every request.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>()
 * 		.header(<js>"Foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), <jsf>T_ARRAY_PIPES</jsf>)
 * 		.build();
 * </p>
 *
 * <p>
 * The methods with {@link ListOperation} parameters allow you to control whether new headers get appended, prepended, or
 * replace existing headers with the same name.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>Header</c> or
 * 		<c>NameValuePair</c> objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Query Parameters</h4>
 * <p>
 * Per-client or per-request query parameters can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#queryData() queryData()}
 * 		<li class='jm'>{@link Builder#queryData(String,String) queryData(String,String)}
 * 		<li class='jm'>{@link Builder#queryData(String,Supplier) queryData(String,Supplier&lt;?&gt;)}
 * 		<li class='jm'>{@link Builder#queryData(NameValuePair...) queryData(NameValuePair...)}
 * 		<li class='jm'>{@link Builder#queryDataDefault(NameValuePair...) defaultQueryData(NameValuePair...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#queryData(String,Object) queryData(String,Object)}
 * 		<li class='jm'>{@link RestRequest#queryData(NameValuePair...) queryData(NameValuePair...)}
 * 		<li class='jm'>{@link RestRequest#queryDataBean(Object) queryDataBean(Object)}
 * 		<li class='jm'>{@link RestRequest#queryCustom(Object) queryCustom(Object)}
 * 		<li class='jm'>{@link RestRequest#queryDataPairs(String...) queryDataPairs(String...)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a client that adds a ?foo=bar query parameter to every request.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().query(<js>"foo"</js>, <js>"bar"</js>).build();
 *
 * 	<jc>// Or do it on every request.</jc>
 * 	String <jv>response</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).query(<js>"foo"</js>, <js>"bar"</js>).run().getContent().asString();
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Like header values, dynamic values and OpenAPI schemas are supported.
 * 	<li class='note'>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>NameValuePair</c>
 * 		objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Form Data</h4>
 *
 * <p>
 * Per-client or per-request form-data parameters can be specified using the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#formData() formData()}
 * 		<li class='jm'>{@link Builder#formData(String,String) formData(String,String)}
 * 		<li class='jm'>{@link Builder#formData(String,Supplier) formData(String,Supplier&lt;?&gt;)}
 * 		<li class='jm'>{@link Builder#formData(NameValuePair...) formDatas(NameValuePair...)}
 * 		<li class='jm'>{@link Builder#formDataDefault(NameValuePair...) defaultFormData(NameValuePair...)}
 * 	</ul>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#formData(String,Object) formData(String,Object)}
 * 		<li class='jm'>{@link RestRequest#formData(NameValuePair...) formData(NameValuePair...)}
 * 		<li class='jm'>{@link RestRequest#formDataBean(Object) formDataBean(Object)}
 * 		<li class='jm'>{@link RestRequest#formDataCustom(Object) formDataCustom(Object)}
 * 		<li class='jm'>{@link RestRequest#formDataPairs(String...) formDataPairs(String...)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Like header values, dynamic values and OpenAPI schemas are supported.
 * 	<li class='note'>Methods that pass in POJOs convert values to strings using the part serializers.  Methods that pass in <c>NameValuePair</c>
 * 		objects use the values returned by that bean directly.
 * </ul>
 *
 *
 * <h4 class='topic'>Request Body</h4>
 *
 * <p>
 * The request body can either be passed in with the client creator method (e.g. {@link RestClient#post(Object,Object) post(uri,body)}),
 * or can be specified via the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul>
 * 		<li class='jm'>{@link RestRequest#content(Object) body(Object)}
 * 		<li class='jm'>{@link RestRequest#content(Object,HttpPartSchema) body(Object,HttpPartSchema)}
 * 	</ul>
 * </ul>
 *
 * <p>
 * The request body can be any of the following types:
 * <ul class='javatree'>
 * 		<li class='jc'>
 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} defined on the client or request.
 * 		<li class='jc'>
 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
 * 		<li class='jc'>
 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
 * 		<li class='jc'>
 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
 * 		<li class='jc'>
 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
 * 		<li class='jc'>
 * 			{@link PartList} - Converted to a URL-encoded FORM post.
 * 		<li class='jc'>
 * 			{@link Supplier} - A supplier of anything on this list.
 * 	</ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>If the serializer on the client or request is explicitly set to <jk>null</jk>, POJOs will be converted to strings
 * 		using the registered part serializer as content type <js>"text/plain</js>.  If the part serializer is also <jk>null</jk>,
 * 		POJOs will be converted to strings using {@link ClassMeta#toString(Object)} which typically just calls {@link Object#toString()}.
 * </ul>
 *
 *
 * <h4 class='topic'>Response Status</h4>
 *
 * <p>
 * After execution using {@link RestRequest#run()} or {@link RestRequest#complete()}, the following methods can be used
 * to get the response status:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getStatusLine() getStatusLine()} <jk>returns</jk> {@link StatusLine}</c>
 * 		<li class='jm'><c>{@link RestResponse#getStatusCode() getStatusCode()} <jk>returns</jk> <jk>int</jk></c>
 * 		<li class='jm'><c>{@link RestResponse#getReasonPhrase() getReasonPhrase()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponse#assertStatus() assertStatus()} <jk>returns</jk> {@link FluentResponseStatusLineAssertion}</c>
 * 	</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Only interested in status code.</jc>
 * 	<jk>int</jk> <jv>statusCode</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).complete().getStatusCode();
 * </p>
 *
 * <p>
 * Equivalent methods with mutable parameters are provided to allow access to status values without breaking fluent call chains.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Interested in multiple values.</jc>
 * 	Value&lt;Integer&gt; <jv>statusCode</jv> = Value.<jsm>create</jsm>();
 * 	Value&lt;String&gt; <jv>reasonPhrase</jv> = Value.<jsm>create</jsm>();
 *
 * 	<jv>client</jv>.get(<jsf>URI</jsf>).complete().getStatusCode(<jv>statusCode</jv>).getReasonPhrase(<jv>reasonPhrase</jv>);
 * 	System.<jsf>err</jsf>.println(<js>"statusCode="</js>+<jv>statusCode</jv>.get()+<js>", reasonPhrase="</js>+<jv>reasonPhrase</jv>.get());
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>If you are only interested in the response status and not the response body, be sure to use {@link RestRequest#complete()} instead
 * 		of {@link RestRequest#run()} to make sure the response body gets automatically cleaned up.  Otherwise you must
 * 		consume the response yourself.
 * </ul>
 *
 * <p>
 * The assertion method is provided for quickly asserting status codes in fluent calls.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Status assertion using a static value.</jc>
 * 	String <jv>body</jv> = <jv>client</jv>.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.assertStatus().asCode().isBetween(200,399)
 * 		.getContent().asString();
 *
 * 	<jc>// Status assertion using a predicate.</jc>
 * 	String <jv>body</jv> = <jv>client</jv>.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.assertStatus().asCode().is(<jv>x</jv> -&gt; <jv>x</jv>&lt;400)
 * 		.getContent().asString();
 * </p>
 *
 *
 * <h4 class='topic'>Response Headers</h4>
 *
 * <p>
 * Response headers are accessed through the following methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getHeaders(String) getHeaders(String)} <jk>returns</jk> {@link ResponseHeader}[]</c>
 * 		<li class='jm'><c>{@link RestResponse#getFirstHeader(String) getFirstHeader(String)} <jk>returns</jk> {@link ResponseHeader}</c>
 * 		<li class='jm'><c>{@link RestResponse#getLastHeader(String) getLastHeader(String)} <jk>returns</jk> {@link ResponseHeader}</c>
 * 		<li class='jm'><c>{@link RestResponse#getAllHeaders() getAllHeaders()} <jk>returns</jk> {@link ResponseHeader}[]</c>
 * 		<li class='jm'><c>{@link RestResponse#getStringHeader(String) getStringHeader(String)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link RestResponse#containsHeader(String) containsHeader(String)} <jk>returns</jk> <jk>boolean</jk></c>
 * 	</ul>
 * </ul>
 *
 * <p>
 * The {@link RestResponse#getFirstHeader(String)} and {@link RestResponse#getLastHeader(String)} methods return an empty {@link ResponseHeader} object instead of<jk>null</jk>.
 * This allows it to be used more easily in fluent calls.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// See if response contains Location header.</jc>
 * 	<jk>boolean</jk> <jv>hasLocationHeader</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).complete().getLastHeader(<js>"Location"</js>).exists();
 * </p>
 *
 * <p>
 * The {@link ResponseHeader} class extends from the HttpClient {@link Header} class and provides several convenience
 * methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link ResponseHeader}
 * 	<ul>
 * 		<li class='jm'><c>{@link ResponseHeader#isPresent() isPresent()} <jk>returns</jk> <jk>boolean</jk></c>
 * 		<li class='jm'><c>{@link ResponseHeader#asString() asString()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link ResponseHeader#as(Type,Type...) as(Type,Type...)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link ResponseHeader#as(Class) as(Class&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asMatcher(Pattern) asMatcher(Pattern)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asMatcher(String) asMatcher(String)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asHeader(Class) asHeader(Class&lt;T <jk>extends</jk> BasicHeader&gt; c)} <jk>returns</jk> {@link BasicHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asStringHeader() asStringHeader()} <jk>returns</jk> {@link BasicStringHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asIntegerHeader() asIntegerHeader()} <jk>returns</jk> {@link BasicIntegerHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asLongHeader() asLongHeader()} <jk>returns</jk> {@link BasicLongHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asDateHeader() asDateHeader()} <jk>returns</jk> {@link BasicDateHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asCsvHeader() asCsvHeader()} <jk>returns</jk> {@link BasicCsvHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asEntityTagsHeader() asEntityTagsHeader()} <jk>returns</jk> {@link BasicEntityTagsHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asStringRangesHeader() asStringRangesHeader()} <jk>returns</jk> {@link BasicStringRangesHeader}</c>
 * 		<li class='jm'><c>{@link ResponseHeader#asUriHeader() asUriHeader()} <jk>returns</jk> {@link BasicUriHeader}</c>
 * 	</ul>
 * </ul>
 *
 * <p>
 * The {@link ResponseHeader#schema(HttpPartSchema)} method allows you to perform parsing of OpenAPI formats for
 * header parts.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse the header "Foo: bar|baz".</jc>
 * 	List&lt;String&gt; <jv>fooHeader</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.complete()
 * 		.getHeader(<js>"Foo"</js>).schema(<jsf>T_ARRAY_PIPES</jsf>).as(List.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 *
 * <p>
 * Assertion methods are also provided for fluent-style calls:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link ResponseHeader}
 * 	<ul>
 * 		<li class='jm'><c>{@link ResponseHeader#assertValue() assertValue()} <jk>returns</jk> {@link FluentResponseHeaderAssertion}</c>
 * 	</ul>
 * </ul>
 *
 * <p>
 * Note how in the following example, the fluent assertion returns control to the {@link RestResponse} object after
 * the assertion has been completed:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Assert the response content type is any sort of JSON.</jc>
 * 	String <jv>body</jv> = <jv>client</jv>.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getHeader(<js>"Content-Type"</js>).assertValue().matchesSimple(<js>"application/json*"</js>)
 * 		.getContent().asString();
 * </p>
 *
 *
 * <h4 class='topic'>Response Body</h4>
 *
 * <p>
 * The response body is accessed through the following method:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestResponse#getContent() getContent()} <jk>returns</jk> {@link ResponseContent}</c>
 * 	</ul>
 * </ul>
 *
 * <p>
 * The {@link ResponseContent} class extends from the HttpClient {@link HttpEntity} class and provides several convenience
 * methods:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link ResponseContent}
 * 	<ul>
 * 		<li class='jm'><c>{@link ResponseContent#asInputStream() asInputStream()} <jk>returns</jk> InputStream</c>
 * 		<li class='jm'><c>{@link ResponseContent#asReader() asReader()} <jk>returns</jk> Reader</c>
 * 		<li class='jm'><c>{@link ResponseContent#asReader(Charset) asReader(Charset)} <jk>returns</jk> Reader</c>
 * 		<li class='jm'><c>{@link ResponseContent#pipeTo(OutputStream) pipeTo(OutputStream)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link ResponseContent#pipeTo(Writer) pipeTo(Writer)} <jk>returns</jk> {@link RestResponse}</c>
 * 		<li class='jm'><c>{@link ResponseContent#as(Type,Type...) as(Type,Type...)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link ResponseContent#as(Class) as(Class&lt;T&gt;)} <jk>returns</jk> T</c>
 * 		<li class='jm'><c>{@link ResponseContent#asFuture(Class) asFuture(Class&lt;T&gt;)} <jk>returns</jk> Future&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link ResponseContent#asFuture(Type,Type...) asFuture(Type,Type...)} <jk>returns</jk> Future&lt;T&gt;</c>
 * 		<li class='jm'><c>{@link ResponseContent#asString() asString()} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link ResponseContent#asStringFuture() asStringFuture()} <jk>returns</jk> Future&lt;String&gt;</c>
 * 		<li class='jm'><c>{@link ResponseContent#asAbbreviatedString(int) asAbbreviatedString(int)} <jk>returns</jk> String</c>
 * 		<li class='jm'><c>{@link ResponseContent#asObjectRest(Class) asObjectRest(Class&lt;?&gt;)} <jk>returns</jk> {@link ObjectRest}</c>
 * 		<li class='jm'><c>{@link ResponseContent#asObjectRest() asObjectRest()} <jk>returns</jk> {@link ObjectRest}</c>
 * 		<li class='jm'><c>{@link ResponseContent#asMatcher(Pattern) asMatcher(Pattern)} <jk>returns</jk> {@link Matcher}</c>
 * 		<li class='jm'><c>{@link ResponseContent#asMatcher(String) asMatcher(String)} <jk>returns</jk> {@link Matcher}</c>
 * 	</ul>
 * </ul>
 *
 * <br>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse into a linked-list of strings.</jc>
 * 	List&lt;String&gt; <jv>list1</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().as(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a linked-list of beans.</jc>
 * 	List&lt;MyBean&gt; <jv>list2</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().as(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
 * 	List&lt;List&lt;String&gt;&gt; <jv>list3</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().as(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a map of string keys/values.</jc>
 * 	Map&lt;String,String&gt; <jv>map1</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 *
 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
 * 	Map&lt;String,List&lt;MyBean&gt;&gt; <jv>map2</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * The response body can only be consumed once unless it has been cached into memory.  In many cases, the body is
 * automatically cached when using the assertions methods or methods such as {@link ResponseContent#asString()}.
 * However, methods that involve reading directly from the input stream cannot be called twice.
 * In these cases, the {@link RestResponse#cacheContent()} and {@link ResponseContent#cache()} methods are provided
 * to cache the response body in memory so that you can perform several operations against it.
 *
 * <p class='bjava'>
 * 	<jc>// Cache the response body so we can access it twice.</jc>
 * 	InputStream <jv>inputStream</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.cacheBody()
 * 		.getContent().pipeTo(<jv>someOtherStream</jv>)
 * 		.getContent().asInputStream();
 * </p>
 *
 * <p>
 * Assertion methods are also provided for fluent-style calls:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link ResponseContent}
 * 	<ul>
 * 		<li class='jm'><c>{@link ResponseContent#assertValue() assertValue()} <jk>returns</jk> {@link FluentResponseBodyAssertion}</c>
 * 	</ul>
 * </ul>
 *
 * <br>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Assert that the body contains the string "Success".</jc>
 * 	String <jv>body</jv> = <jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().assertString().contains(<js>"Success"</js>)
 * 		.getContent().asString();
 * </p>
 *
 * <p>
 * Object assertions allow you to parse the response body into a POJO and then perform various tests on that resulting
 * POJO.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse bean into POJO and then validate that it was parsed correctly.</jc>
 * 	MyBean <jv>bean</jv> = <jv>client</jv>.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.getContent().assertObject(MyBean.<jk>class</jk>).asJson().is(<js>"{foo:'bar'}"</js>)
 * 		.getContent().as(MyBean.<jk>class</jk>);
 * </p>
 *
 *
 * <h4 class='topic'>Custom Call Handlers</h4>
 *
 * <p>
 * The {@link RestCallHandler} interface provides the ability to provide custom handling of requests.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#callHandler() callHandler()}
 * 	</ul>
 * 	<li class='jic'>{@link RestCallHandler}
 * 	<ul>
 * 		<li class='jm'><c>{@link RestCallHandler#run(HttpHost,HttpRequest,HttpContext) run(HttpHost,HttpRequest,HttpContext)} <jk>returns</jk> HttpResponse</c>
 * 	</ul>
 * </ul>
 *
 * <p>
 * Note that there are other ways of accomplishing this such as extending the {@link RestClient} class and overriding
 * the {@link #run(HttpHost,HttpRequest,HttpContext)} method
 * or by defining your own {@link HttpRequestExecutor}.  Using this interface is often simpler though.
 *
 *
 * <h4 class='topic'>Interceptors</h4>
 *
 * <p>
 * The {@link RestCallInterceptor} API provides a quick way of intercepting and manipulating requests and responses beyond
 * the existing {@link HttpRequestInterceptor} and {@link HttpResponseInterceptor} APIs.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#interceptors(Object...) interceptors(Object...)}
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
 *
 * <h4 class='topic'>Logging / Debugging</h4>
 *
 * <p>
 * The following methods provide logging of requests and responses:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#logger(Logger) logger(Logger)}
 * 		<li class='jm'>{@link Builder#logToConsole() logToConsole()}
 * 		<li class='jm'>{@link Builder#logRequests(DetailLevel,Level,BiPredicate) logRequests(DetailLevel,Level,BiPredicate)}
 * 	</ul>
 * </ul>
 *
 * <p>
 * The following example shows the results of logging all requests that end with <c>/bean</c>.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	MyBean <jv>bean</jv> = RestClient
 * 		.<jsm>create</jsm>()
 * 		.json5()
 * 		.logRequests(DetailLevel.<jsf>FULL</jsf>, Level.<jsf>SEVERE</jsf>, (<jv>req</jv>,<jv>res</jv>)-&gt;<jv>req</jv>.getUri().endsWith(<js>"/bean"</js>))
 * 		.logToConsole()
 * 		.build()
 * 		.post(<js>"http://localhost/bean"</js>, <jv>anotherBean</jv>)
 * 		.run()
 * 		.getContent().as(MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * This produces the following console output:
 *
 * <p class='bconsole'>
 * 	=== HTTP Call (outgoing) ======================================================
 * 	=== REQUEST ===
 * 	POST http://localhost/bean
 * 	---request headers---
 * 		Accept: application/json5
 * 	---request entity---
 * 	Content-Type: application/json5
 * 	---request content---
 * 	{f:1}
 * 	=== RESPONSE ===
 * 	HTTP/1.1 200
 * 	---response headers---
 * 		Content-Type: application/json
 * 	---response content---
 * 	{f:1}
 * 	=== END =======================================================================",
 * </p>
 *
 *
 * <p class='notes'>
 * It should be noted that if you enable request logging detail level {@link DetailLevel#FULL}, response bodies will be cached by default which may introduce
 * a performance penalty.
 *
 * <p>
 * Additionally, the following method is also provided for enabling debug mode:
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#debug() debug()}
 * 	</ul>
 * </ul>
 *
 * <p>
 * Enabling debug mode has the following effects:
 * <ul>
 * 	<li>{@link org.apache.juneau.Context.Builder#debug()} is enabled.
 * 	<li>{@link Builder#detectLeaks()} is enabled.
 * 	<li>{@link Builder#logToConsole()} is called.
 * </ul>
 *
 *
 * <h4 class='topic'>REST Proxies</h4>
 *
 * <p>
 * One of the more powerful features of the REST client class is the ability to produce Java interface proxies against
 * arbitrary remote REST resources.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Define a Remote proxy for interacting with a REST interface.</jc>
 * 	<ja>@Remote</ja>(path=<js>"/petstore"</js>)
 * 	<jk>public interface</jk> PetStore {
 *
 * 		<ja>@RemotePost</ja>(<js>"/pets"</js>)
 * 		Pet addPet(
 * 			<ja>@Content</ja> CreatePet <jv>pet</jv>,
 * 			<ja>@Header</ja>(<js>"E-Tag"</js>) UUID <jv>etag</jv>,
 * 			<ja>@Query</ja>(<js>"debug"</js>) <jk>boolean</jk> <jv>debug</jv>
 * 		);
 * 	}
 *
 * 	<jc>// Use a RestClient with default JSON 5 support.</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json5().build())
 *
 * 	PetStore <jv>store</jv> = <jv>client</jv>.getRemote(PetStore.<jk>class</jk>, <js>"http://localhost:10000"</js>);
 * 	CreatePet <jv>createPet</jv> = <jk>new</jk> CreatePet(<js>"Fluffy"</js>, 9.99);
 * 	Pet <jv>pet</jv> = <jv>store</jv>.addPet(<jv>createPet</jv>, UUID.<jsm>randomUUID</jsm>(), <jk>true</jk>);
 * </p>
 *
 * <p>
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
 * <p>
 * Two basic types of remote interfaces are provided:
 *
 * <ul class='spaced-list'>
 * 	<li>{@link Remote @Remote}-annotated interfaces.  These can be defined against arbitrary external REST resources.
 * 	<li>RPC-over-REST interfaces.  These are Java interfaces that allow you to make method calls on server-side POJOs.
 * </ul>
 *
 * <p>
 * Refer to the following documentation on both flavors:
 *
 * <ul class='doctree'>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
 * </ul>
 *
 * <br>
 * <hr>
 * <h4 class='topic'>Customizing Apache HttpClient</h4>
 *
 * <p>
 * Several methods are provided for customizing the underlying HTTP client and client builder classes:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder}
 * 	<ul>
 * 		<li class='jm'>{@link Builder#httpClientBuilder(HttpClientBuilder) httpClientBuilder(HttpClientBuilder)} - Set the client builder yourself.
 * 		<li class='jm'>{@link Builder#createHttpClientBuilder() createHttpClientBuilder()} - Override to create the client builder.
 * 		<li class='jm'>{@link Builder#createHttpClient() createHttpClient()} - Override to create the client.
 * 		<li class='jm'>{@link Builder#createConnectionManager() createConnectionManager()} - Override to create the connection management.
 * 	</ul>
 * </ul>
 *
 * <p>
 * Additionally, all methods on the <c>HttpClientBuilder</c> class have been extended with fluent setters.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a client with customized HttpClient settings.</jc>
 * 	MyBean <jv>bean</jv> = RestClient
 * 		.<jsm>create</jsm>()
 * 		.disableRedirectHandling()
 * 		.connectionManager(<jv>myConnectionManager</jv>)
 * 		.addInterceptorFirst(<jv>myHttpRequestInterceptor</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * Refer to the {@link HttpClientBuilder HTTP Client Builder API} for more information.
 *
 *
 * <h4 class='topic'>Extending RestClient</h4>
 *
 * <p>
 * The <c>RestClient</c> API has been designed to allow for the ability to be easily extended.
 * The following example that overrides the primary run method shows how this can be done.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyRestClient <jk>extends</jk> RestClient {
 *
 * 		<jc>// Must provide this constructor!</jc>
 * 		<jk>public</jk> MyRestClient(RestClient.Builder <jv>builder</jv>) {
 * 			<jk>super</jk>(<jv>builder</jv>);
 * 		}
 *
 * 		<jd>/** Optionally override to customize builder settings before initialization. </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected void</jk> init(RestClient.Builder) {...}
 *
 * 		<jd>/** Optionally override to provide post-initialization (e.g. setting up SAML handshakes, etc...). </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected void</jk> init() {...}
 *
 * 		<jd>/** Optionally override to customize requests when they're created (e.g. add headers to each request). </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> RestRequest request(RestOperation) {...}
 *
 * 		<jd>/** Optionally override to implement your own call handling. </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> HttpResponse run(HttpHost, HttpRequest, HttpContext) {...}
 *
 * 		<jd>/** Optionally override to customize requests before they're executed. </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected void</jk> onCallInit(RestRequest) {...}
 *
 * 		<jd>/** Optionally override to customize responses as soon as a connection is made. </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected void</jk> onCallConnect(RestRequest, RestResponse) {...}
 *
 * 		<jd>/** Optionally override to perform any call cleanup. </jd>
 * 		<ja>@Override</ja>
 * 		<jk>protected void</jk> onCallClose(RestRequest, RestResponse) {...}
 * 	}
 *
 * 	<jc>// Instantiate your client.</jc>
 * 	MyRestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json().build(MyRestClient.<jk>class</jk>);
 * </p>
 *
 * <p>
 * The {@link RestRequest} and {@link RestResponse} objects can also be extended and integrated by overriding the
 * {@link RestClient#createRequest(URI,String,boolean)} and {@link RestClient#createResponse(RestRequest,HttpResponse,Parser)} methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a> * </ul>
 */
public class RestClient extends BeanContextable implements HttpClient, Closeable {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final RestCallInterceptor[] EMPTY_REST_CALL_INTERCEPTORS = new RestCallInterceptor[0];

	/**
	 * Instantiates a new clean-slate {@link Builder} object.
	 *
	 * @return A new {@link Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters(ignore={"beanMapPutReturnsOldValue","example","exampleJson","debug","mediaType"})
	public static class Builder extends BeanContextable.Builder {

		BeanStore beanStore = BeanStore.create().build();

		private HttpClientBuilder httpClientBuilder;
		private CloseableHttpClient httpClient;

		private HeaderList headerData;
		private PartList queryData, formData, pathData;
		private BeanCreator<RestCallHandler> callHandler;
		private SerializerSet.Builder serializers;
		private ParserSet.Builder parsers;
		private HttpPartSerializer.Creator partSerializer;
		private HttpPartParser.Creator partParser;
		private UrlEncodingSerializer.Builder urlEncodingSerializer;

		private boolean pooled;

		String rootUrl;
		boolean skipEmptyHeaderData, skipEmptyFormData, skipEmptyQueryData, executorServiceShutdownOnClose, ignoreErrors, keepHttpClientOpen, detectLeaks,
			logToConsole;
		Logger logger;
		DetailLevel logRequests;
		Level logRequestsLevel;
		BiPredicate<RestRequest,RestResponse> logRequestsPredicate;
		Predicate<Integer> errorCodes = x ->  x<=0 || x>=400;
		HttpClientConnectionManager connectionManager;
		PrintStream console;
		ExecutorService executorService;
		List<RestCallInterceptor> interceptors;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super();
		}

		@Override /* Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}

		@Override /* Context.Builder */
		public RestClient build() {
			return build(RestClient.class);
		}

		//------------------------------------------------------------------------------------------------------------------
		// Convenience marshalling support methods.
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Convenience method for specifying JSON as the marshalling transmission media type.
		 *
		 * <p>
		 * {@link JsonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"application/json"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #xml()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses JSON marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder json() {
			return serializer(JsonSerializer.class).parser(JsonParser.class);
		}

		/**
		 * Convenience method for specifying Simplified JSON as the marshalling transmission media type.
		 *
		 * <p>
		 * Simplified JSON is typically useful for automated tests because you can do simple string comparison of results
		 * without having to escape lots of quotes.
		 *
		 * <p>
		 * 	{@link Json5Serializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link Json5Parser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"application/json5"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #xml()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(Json5Serializer.<jk>class</jk>).parser(Json5Parser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses Simplified JSON marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json5().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder json5() {
			return serializer(Json5Serializer.class).parser(Json5Parser.class);
		}

		/**
		 * Convenience method for specifying XML as the marshalling transmission media type.
		 *
		 * <p>
		 * {@link XmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link XmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/xml"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/xml"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(XmlSerializer.<jk>class</jk>).parser(XmlParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses XML marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().xml().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder xml() {
			return serializer(XmlSerializer.class).parser(XmlParser.class);
		}

		/**
		 * Convenience method for specifying HTML as the marshalling transmission media type.
		 *
		 * <p>
		 * POJOs are converted to HTML without any sort of doc wrappers.
		 *
		 * <p>
		 * 	{@link HtmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(HtmlSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses HTML marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().html().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder html() {
			return serializer(HtmlSerializer.class).parser(HtmlParser.class);
		}

		/**
		 * Convenience method for specifying HTML DOC as the marshalling transmission media type.
		 *
		 * <p>
		 * POJOs are converted to fully renderable HTML pages.
		 *
		 * <p>
		 * 	{@link HtmlDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()} or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(HtmlDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses HTML Doc marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlDoc().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder htmlDoc() {
			return serializer(HtmlDocSerializer.class).parser(HtmlParser.class);
		}

		/**
		 * Convenience method for specifying Stripped HTML DOC as the marshalling transmission media type.
		 *
		 * <p>
		 * Same as {@link #htmlDoc()} but without the header and body tags and page title and description.
		 *
		 * <p>
		 * 	{@link HtmlStrippedDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(HtmlStrippedDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses HTML Stripped Doc marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlStrippedDoc().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder htmlStrippedDoc() {
			return serializer(HtmlStrippedDocSerializer.class).parser(HtmlParser.class);
		}

		/**
		 * Convenience method for specifying Plain Text as the marshalling transmission media type.
		 *
		 * <p>
		 * Plain text marshalling typically only works on simple POJOs that can be converted to and from strings using
		 * swaps, swap methods, etc...
		 *
		 * <p>
		 * 	{@link PlainTextSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link PlainTextParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/plain"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/plain"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(PlainTextSerializer.<jk>class</jk>).parser(PlainTextParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses Plain Text marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().plainText().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder plainText() {
			return serializer(PlainTextSerializer.class).parser(PlainTextParser.class);
		}

		/**
		 * Convenience method for specifying MessagePack as the marshalling transmission media type.
		 *
		 * <p>
		 * MessagePack is a binary equivalent to JSON that takes up considerably less space than JSON.
		 *
		 * <p>
		 * 	{@link MsgPackSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link MsgPackParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(MsgPackSerializer.<jk>class</jk>).parser(MsgPackParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses MessagePack marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().msgPack().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder msgPack() {
			return serializer(MsgPackSerializer.class).parser(MsgPackParser.class);
		}

		/**
		 * Convenience method for specifying UON as the marshalling transmission media type.
		 *
		 * <p>
		 * UON is Url-Encoding Object notation that is equivalent to JSON but suitable for transmission as URL-encoded
		 * query and form post values.
		 *
		 * <p>
		 * 	{@link UonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	{@link UonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/uon"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/uon"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(UonSerializer.<jk>class</jk>).parser(UonParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses UON marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().uon().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder uon() {
			return serializer(UonSerializer.class).parser(UonParser.class);
		}

		/**
		 * Convenience method for specifying URL-Encoding as the marshalling transmission media type.
		 *
		 * <p>
		 * 	{@link UrlEncodingSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 		<li>This serializer is NOT used when using the {@link RestRequest#formData(String, Object)} (and related) methods for constructing
		 * 			the request body.  Instead, the part serializer specified via {@link #partSerializer(Class)} is used.
		 * 	</ul>
		 * <p>
		 * 	{@link UrlEncodingParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(UrlEncodingSerializer.<jk>class</jk>).parser(UrlEncodingParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses URL-Encoded marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().urlEnc().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder urlEnc() {
			return serializer(UrlEncodingSerializer.class).parser(UrlEncodingParser.class);
		}

		/**
		 * Convenience method for specifying OpenAPI as the marshalling transmission media type.
		 *
		 * <p>
		 * OpenAPI is a language that allows serialization to formats that use {@link HttpPartSchema} objects to describe their structure.
		 *
		 * <p>
		 * 	{@link OpenApiSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 		<li>Typically the {@link RestRequest#content(Object, HttpPartSchema)} method will be used to specify the body of the request with the
		 * 			schema describing it's structure.
		 * 	</ul>
		 * <p>
		 * 	{@link OpenApiParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 		<li>Typically the {@link ResponseContent#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header will be set to <js>"text/openapi"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	<c>Content-Type</c> request header will be set to <js>"text/openapi"</js> unless overridden
		 * 		via {@link #headers()}, or per-request via {@link RestRequest#header(Header)}.
		 * <p>
		 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
		 * 	<ul>
		 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
		 * 		last-enabled language if the headers are not set.
		 * 	</ul>
		 * <p>
		 * 	Identical to calling <c>serializer(OpenApiSerializer.<jk>class</jk>).parser(OpenApiParser.<jk>class</jk>)</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses OpenAPI marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().openApi().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder openApi() {
			return serializer(OpenApiSerializer.class).parser(OpenApiParser.class);
		}

		/**
		 * Convenience method for specifying all available transmission types.
		 *
		 * <p>
		 * 	All basic Juneau serializers will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
		 * 	<ul>
		 * 		<li>The serializers can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	All basic Juneau parsers will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
		 * 	<ul>
		 * 		<li>The parsers can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 			bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * 	</ul>
		 * <p>
		 * 	<c>Accept</c> request header must be set via {@link #headers()}, or per-request
		 * 		via {@link RestRequest#header(Header)} in order for the correct parser to be selected.
		 * <p>
		 * 	<c>Content-Type</c> request header must be set via {@link #headers()},
		 * 		or per-request via {@link RestRequest#header(Header)} in order for the correct serializer to be selected.
		 * <p>
		 * 	Similar to calling <c>json().json5().html().xml().uon().urlEnc().openApi().msgPack().plainText()</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses universal marshalling.</jc>
		 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().universal().build();
		 * </p>
		 *
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder universal() {
			return
				serializers(
					JsonSerializer.class,
					Json5Serializer.class,
					HtmlSerializer.class,
					XmlSerializer.class,
					UonSerializer.class,
					UrlEncodingSerializer.class,
					OpenApiSerializer.class,
					MsgPackSerializer.class,
					PlainTextSerializer.class
				)
				.parsers(
					JsonParser.class,
					Json5Parser.class,
					XmlParser.class,
					HtmlParser.class,
					UonParser.class,
					UrlEncodingParser.class,
					OpenApiParser.class,
					MsgPackParser.class,
					PlainTextParser.class
				);
		}

		//------------------------------------------------------------------------------------------------------------------
		// httpClientBuilder
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the HTTP client builder.
		 *
		 * @return The HTTP client builder.
		 */
		public final HttpClientBuilder httpClientBuilder() {
			if (httpClientBuilder == null)
				httpClientBuilder = createHttpClientBuilder();
			return httpClientBuilder;
		}

		/**
		 * Creates an instance of an {@link HttpClientBuilder} to be used to create the {@link HttpClient}.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own client builder.
		 * The builder can also be specified using the {@link #httpClientBuilder(HttpClientBuilder)} method.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A Builder that provides it's own customized HttpClientBuilder.</jc>
		 * 	<jk>public class</jk> MyBuilder <jk>extends</jk> Builder {
		 * 		<ja>@Override</ja>
		 * 		<jk>protected</jk> HttpClientBuilder createHttpClientBuilder() {
		 * 			<jk>return</jk> HttpClientBuilder.<jsm>create</jsm>();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Instantiate.</jc>
		 * 	RestClient <jv>client</jv> = <jk>new</jk> MyBuilder().build();
		 * </p>
		 *
		 * @return The HTTP client builder to use to create the HTTP client.
		 */
		protected HttpClientBuilder createHttpClientBuilder() {
			return HttpClientBuilder.create();
		}

		/**
		 * Sets the {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
		 *
		 * <p>
		 * This can be used to bypass the builder created by {@link #createHttpClientBuilder()} method.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses a customized HttpClientBuilder.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.httpClientBuilder(HttpClientBuilder.<jsm>create</jsm>())
		 * 		.build();
		 * </p>
		 *
		 * @param value The {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder httpClientBuilder(HttpClientBuilder value) {
			this.httpClientBuilder = value;
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// httpClient
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
		 *
		 * <p>
		 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
		 * {@link #httpClient(CloseableHttpClient)} method.
		 *
		 * <p>
		 * Subclasses can override this method to provide specially-configured HTTP clients to handle stuff such as
		 * SSL/TLS certificate handling, authentication, etc.
		 *
		 * <p>
		 * The default implementation returns an instance of {@link HttpClient} using the client builder returned by
		 * {@link #createHttpClientBuilder()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A Builder that provides it's own customized HttpClient.</jc>
		 * 	<jk>public class</jk> MyBuilder <jk>extends</jk> Builder {
		 * 		<ja>@Override</ja>
		 * 		<jk>protected</jk> HttpClientBuilder createHttpClient() {
		 * 			<jk>return</jk> HttpClientBuilder.<jsm>create</jsm>().build();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Instantiate.</jc>
		 * 	RestClient <jv>client</jv> = <jk>new</jk> MyBuilder().build();
		 * </p>
		 *
		 * @return The HTTP client to use.
		 */
		protected CloseableHttpClient createHttpClient() {
			if (connectionManager == null)
				connectionManager = createConnectionManager();
			httpClientBuilder().setConnectionManager(connectionManager);
			return httpClientBuilder().build();
		}

		/**
		 * Sets the {@link HttpClient} to be used to handle all HTTP communications with the target server.
		 *
		 * <p>
		 * This can be used to bypass the client created by {@link #createHttpClient()} method.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses a customized HttpClient.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.httpClient(HttpClientBuilder.<jsm>create</jsm>().build())
		 * 		.build();
		 * </p>
		 *
		 * @param value The {@link HttpClient} to be used to handle all HTTP communications with the target server.
		 * @return This object.
		 */
		@FluentSetter
		public Builder httpClient(CloseableHttpClient value) {
			this.httpClient = value;
			return this;
		}

		final CloseableHttpClient getHttpClient() {
			return httpClient != null ? httpClient : createHttpClient();
		}

		//------------------------------------------------------------------------------------------------------------------
		// serializers
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the serializer group sub-builder.
		 *
		 * @return The serializer group sub-builder.
		 */
		public final SerializerSet.Builder serializers() {
			if (serializers == null)
				serializers = createSerializers();
			return serializers;
		}

		/**
		 * Instantiates the serializer group sub-builder.
		 *
		 * @return A new serializer group sub-builder.
		 */
		protected SerializerSet.Builder createSerializers() {
			return SerializerSet.create().beanContext(beanContext());
		}

		/**
		 * Serializer.
		 *
		 * <p>
		 * Associates the specified {@link Serializer Serializer} with the HTTP client.
		 *
		 * <p>
		 * The serializer is used to serialize POJOs into the HTTP request body.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a class, the serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON transport for request bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.serializer(JsonSerializer.<jk>class</jk>)
		 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link JsonSerializer}.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		@FluentSetter
		public Builder serializer(Class<? extends Serializer> value) {
			return serializers(value);
		}

		/**
		 * Serializer.
		 *
		 * <p>
		 * Associates the specified {@link Serializer Serializer} with the HTTP client.
		 *
		 * <p>
		 * The serializer is used to serialize POJOs into the HTTP request body.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a pre-instantiated serializer, the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined
		 * 	on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses a predefined JSON serializer request bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.serializer(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link JsonSerializer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder serializer(Serializer value) {
			return serializers(value);
		}

		/**
		 * Serializers.
		 *
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
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in classes, the serializers can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON and XML transport for request bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>)
		 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link JsonSerializer}.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		@FluentSetter
		public Builder serializers(Class<? extends Serializer>...value) {
			serializers().add(value);
			return this;
		}

		/**
		 * Serializers.
		 *
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
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a pre-instantiated serializers, the serializer property setters (e.g. {@link #sortCollections()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined
		 * 	on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses predefined JSON and XML serializers for request bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.serializers(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>, XmlSerializer.<jsf>DEFAULT_READABLE</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link JsonSerializer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder serializers(Serializer...value) {
			serializers().add(value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// parsers
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the parser group sub-builder.
		 *
		 * @return The parser group sub-builder.
		 */
		public final ParserSet.Builder parsers() {
			if (parsers == null)
				parsers = createParsers();
			return parsers;
		}

		/**
		 * Instantiates the parser group sub-builder.
		 *
		 * @return A new parser group sub-builder.
		 */
		protected ParserSet.Builder createParsers() {
			return ParserSet.create().beanContext(beanContext());
		}

		/**
		 * Parser.
		 *
		 * <p>
		 * Associates the specified {@link Parser Parser} with the HTTP client.
		 *
		 * <p>
		 * The parser is used to parse the HTTP response body into a POJO.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a class, the parser can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON transport for response bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.parser(JsonParser.<jk>class</jk>)
		 * 		.strict()  <jc>// Enable strict mode on JsonParser.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link JsonParser#DEFAULT}.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		@FluentSetter
		public Builder parser(Class<? extends Parser> value) {
			return parsers(value);
		}

		/**
		 * Parser.
		 *
		 * <p>
		 * Associates the specified {@link Parser Parser} with the HTTP client.
		 *
		 * <p>
		 * The parser is used to parse the HTTP response body into a POJO.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a pre-instantiated parser, the parser property setters (e.g. {@link #strict()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined
		 * 	on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses a predefined JSON parser for response bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link JsonParser#DEFAULT}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder parser(Parser value) {
			return parsers(value);
		}

		/**
		 * Parsers.
		 *
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
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in classes, the parsers can be configured using any of the parser property setters (e.g. {@link #strict()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.parser(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>)
		 * 		.strict()  <jc>// Enable strict mode on parsers.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link JsonParser#DEFAULT}.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		@FluentSetter
		public Builder parsers(Class<? extends Parser>...value) {
			parsers().add(value);
			return this;
		}

		/**
		 * Parsers.
		 *
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
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in pre-instantiated parsers, the parser property setters (e.g. {@link #strict()}) or
		 * 	bean context property setters (e.g. {@link #swaps(Class...)}) defined
		 * 	on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>, XmlParser.<jsf>DEFAULT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link JsonParser#DEFAULT}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder parsers(Parser...value) {
			parsers().add(value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// partSerializer
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the part serializer sub-builder.
		 *
		 * @return The part serializer sub-builder.
		 */
		public final HttpPartSerializer.Creator partSerializer() {
			if (partSerializer == null)
				partSerializer = createPartSerializer();
			return partSerializer;
		}

		/**
		 * Instantiates the part serializer sub-builder.
		 *
		 * @return A new part serializer sub-builder.
		 */
		protected HttpPartSerializer.Creator createPartSerializer() {
			return HttpPartSerializer.creator().type(OpenApiSerializer.class).beanContext(beanContext());
		}

		/**
		 * Part serializer.
		 *
		 * <p>
		 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
		 *
		 * <p>
		 * The default part serializer is {@link OpenApiSerializer} which allows for schema-driven marshalling.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses UON format by default for outgoing HTTP parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.partSerializer(UonSerializer.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link OpenApiSerializer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) {
			partSerializer().type(value);
			return this;
		}

		/**
		 * Part serializer.
		 *
		 * <p>
		 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
		 *
		 * <p>
		 * The default part serializer is {@link OpenApiSerializer} which allows for schema-driven marshalling.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses UON format by default for outgoing HTTP parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.partSerializer(UonSerializer.<jsf>DEFAULT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link OpenApiSerializer}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder partSerializer(HttpPartSerializer value) {
			partSerializer().impl(value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// partParser
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the part parser sub-builder.
		 *
		 * @return The part parser sub-builder.
		 */
		public final HttpPartParser.Creator partParser() {
			if (partParser == null)
				partParser = createPartParser();
			return partParser;
		}

		/**
		 * Instantiates the part parser sub-builder.
		 *
		 * @return A new part parser sub-builder.
		 */
		protected HttpPartParser.Creator createPartParser() {
			return HttpPartParser.creator().type(OpenApiParser.class).beanContext(beanContext());
		}

		/**
		 * Part parser.
		 *
		 * <p>
		 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
		 *
		 * <p>
		 * The default part parser is {@link OpenApiParser} which allows for schema-driven marshalling.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses UON format by default for incoming HTTP parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.partParser(UonParser.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link OpenApiParser}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder partParser(Class<? extends HttpPartParser> value) {
			partParser().type(value);
			return this;
		}

		/**
		 * Part parser.
		 *
		 * <p>
		 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
		 *
		 * <p>
		 * The default part parser is {@link OpenApiParser} which allows for schema-driven marshalling.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses UON format by default for incoming HTTP parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.partParser(UonParser.<jsf>DEFAULT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link OpenApiParser}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder partParser(HttpPartParser value) {
			partParser().impl(value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// urlEncodingSerializer
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the URL-encoding serializer sub-builder.
		 *
		 * @return The URL-encoding serializer sub-builder.
		 */
		public final UrlEncodingSerializer.Builder urlEncodingSerializer() {
			if (urlEncodingSerializer == null)
				urlEncodingSerializer = createUrlEncodingSerializer();
			return urlEncodingSerializer;
		}

		/**
		 * Instantiates the URL-encoding serializer sub-builder.
		 *
		 * @return A new URL-encoding serializer sub-builder.
		 */
		protected UrlEncodingSerializer.Builder createUrlEncodingSerializer() {
			return UrlEncodingSerializer.create().beanContext(beanContext());
		}

		//------------------------------------------------------------------------------------------------------------------
		// headerData
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the builder for the list of headers that get applied to all requests created by this builder.
		 *
		 * <p>
		 * This is the primary method for accessing the request header list.
		 * On first call, the builder is created via the method {@link #createHeaderData()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that adds a "Foo: bar" header on every request.</jc>
		 * 	RestClient.Builder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
		 * 	<jv>builder</jv>.headerData().setDefault(<js>"Foo"</js>, <js>"bar"</js>));
		 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
		 * </p>
		 *
		 * <p>
		 * The following convenience methods are also provided for updating the headers:
		 * <ul>
		 * 	<li class='jm'>{@link #headers(Header...)}
		 * 	<li class='jm'>{@link #headersDefault(Header...)}
		 * 	<li class='jm'>{@link #header(String,String)}
		 * 	<li class='jm'>{@link #header(String,Supplier)}
		 * 	<li class='jm'>{@link #mediaType(String)}
		 * 	<li class='jm'>{@link #mediaType(MediaType)}
		 * 	<li class='jm'>{@link #accept(String)}
		 * 	<li class='jm'>{@link #acceptCharset(String)}
		 * 	<li class='jm'>{@link #clientVersion(String)}
		 * 	<li class='jm'>{@link #contentType(String)}
		 * 	<li class='jm'>{@link #debug()}
		 * 	<li class='jm'>{@link #noTrace()}
		 * </ul>
		 *
		 * @return The header list builder.
		 */
		public final HeaderList headers() {
			if (headerData == null)
				headerData = createHeaderData();
			return headerData;
		}

		/**
		 * Creates the builder for the header list.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own implementation.
		 *
		 * <p>
		 * The default behavior creates an empty builder.
		 *
		 * @return The header list builder.
		 * @see #headers()
		 */
		protected HeaderList createHeaderData() {
			return HeaderList.create();
		}

		/**
		 * Appends multiple headers to all requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>import static</jk> org.apache.juneau.http.HttpHeaders.*;
		 *
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.headers(
		 * 			<jsf>ACCEPT_TEXT_XML</jsf>,
		 * 			<jsm>stringHeader</jsm>(<js>"Foo"</js>, <js>"bar"</js>)
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(<jv>parts</jv>)</c>.
		 *
		 * @param parts
		 * 	The header to set.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder headers(Header...parts) {
			headers().append(parts);
			return this;
		}

		/**
		 * Sets default header values.
		 *
		 * <p>
		 * Uses default values for specified headers if not otherwise specified on the outgoing requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.headersDefault(<jsm>stringHeader</jsm>(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>));
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().setDefault(<jv>parts</jv>)</c>.
		 *
		 * @param parts The header values.
		 * @return This object.
		 * @see #headers()
		 */
		public Builder headersDefault(Header...parts) {
			headers().setDefault(parts);
			return this;
		}

		/**
		 * Appends a header to all requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.header(<js>"Foo"</js>, <js>"bar"</js>);
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The header name.
		 * @param value The header value.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder header(String name, String value) {
			headers().append(name, value);
			return this;
		}

		/**
		 * Appends a header to all requests using a dynamic value.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.header(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>);
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The header name.
		 * @param value The header value supplier.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder header(String name, Supplier<String> value) {
			headers().append(name, value);
			return this;
		}

		/**
		 * Appends the <c>Accept</c> and <c>Content-Type</c> headers on all requests made by this client.
		 *
		 * <p>
		 * Headers are appended to the end of the current header list.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>), ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @param value The new header values.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder mediaType(String value) {
			super.mediaType(MediaType.of(value));
			return headers(Accept.of(value), ContentType.of(value));
		}

		/**
		 * Appends the <c>Accept</c> and <c>Content-Type</c> headers on all requests made by this client.
		 *
		 * <p>
		 * Headers are appended to the end of the current header list.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>), ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @param value The new header values.
		 * @return This object.
		 * @see #headers()
		 */
		@Override
		@FluentSetter
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return headers(Accept.of(value), ContentType.of(value));
		}

		/**
		 * Appends an <c>Accept</c> header on this request.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @param value
		 * 	The new header value.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder accept(String value) {
			return headers(Accept.of(value));
		}

		/**
		 * Sets the value for the <c>Accept-Charset</c> request header on all requests.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(AcceptCharset.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @param value The new header value.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder acceptCharset(String value) {
			return headers(AcceptCharset.of(value));
		}

		/**
		 * Sets the client version by setting the value for the <js>"Client-Version"</js> header.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(ClientVersion.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @param value The version string (e.g. <js>"1.2.3"</js>)
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder clientVersion(String value) {
			return headers(ClientVersion.of(value));
		}

		/**
		 * Sets the value for the <c>Content-Type</c> request header on all requests.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * <p>
		 * This overrides the media type specified on the serializer.
		 *
		 * @param value The new header value.
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder contentType(String value) {
			return headers(ContentType.of(value));
		}

		/**
		 * Sets the value for the <c>Debug</c> request header on all requests.
		 *
		 * <p>
		 * This is a shortcut for calling <c>headerData().append(Debug.<jsm>of</jsm>(<jv>value</jv>))</c>.
		 *
		 * @return This object.
		 * @see #headers()
		 */
		@Override
		@FluentSetter
		public Builder debug() {
			super.debug();
			serializers().forEach(x -> x.debug());
			return headers(Debug.TRUE);
		}

		/**
		 * When called, <c>No-Trace: true</c> is added to requests.
		 *
		 * <p>
		 * This gives the opportunity for the servlet to not log errors on invalid requests.
		 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
		 * results of testing.
		 *
		 * <p>
		 * It's up to the server to decide whether to allow for this.
		 * The <c>BasicTestRestLogger</c> class watches for this header and prevents logging of status 400+ responses to
		 * prevent needless logging of test scenarios.
		 *
		 * @return This object.
		 * @see #headers()
		 */
		@FluentSetter
		public Builder noTrace() {
			return headers(NoTrace.of(true));
		}

		//------------------------------------------------------------------------------------------------------------------
		// queryData
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the builder for the list of query parameters that get applied to all requests created by this builder.
		 *
		 * <p>
		 * This is the primary method for accessing the query parameter list.
		 * On first call, the builder is created via the method {@link #createQueryData()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that adds a "foo=bar" query parameter on every request.</jc>
		 * 	RestClient.Builder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
		 * 	<jv>builder</jv>.queryData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
		 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
		 * </p>
		 *
		 * <p>
		 * The following convenience methods are also provided for updating the parameters:
		 * <ul>
		 * 	<li class='jm'>{@link #queryData(NameValuePair...)}
		 * 	<li class='jm'>{@link #queryDataDefault(NameValuePair...)}
		 * 	<li class='jm'>{@link #queryData(String,String)}
		 * 	<li class='jm'>{@link #queryData(String,Supplier)}
		 * </ul>
		 *
		 * @return The query data list builder.
		 */
		public final PartList queryData() {
			if (queryData == null)
				queryData = createQueryData();
			return queryData;
		}

		/**
		 * Creates the builder for the query data list.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own implementation.
		 *
		 * <p>
		 * The default behavior creates an empty builder.
		 *
		 * @return The query data list builder.
		 * @see #queryData()
		 */
		protected PartList createQueryData() {
			return PartList.create();
		}

		/**
		 * Appends multiple query parameters to the URI of all requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
		 *
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.queryData(
		 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
		 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>queryData().append(<jv>parts</jv>)</c>.
		 *
		 * @param parts
		 * 	The query parameters.
		 * @return This object.
		 * @see #queryData()
		 */
		@FluentSetter
		public Builder queryData(NameValuePair...parts) {
			queryData().append(parts);
			return this;
		}

		/**
		 * Sets default query parameter values.
		 *
		 * <p>
		 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.queryDataDefault(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>queryData().setDefault(<jv>parts</jv>)</c>.
		 *
		 * @param parts The parts.
		 * @return This object.
		 * @see #queryData()
		 */
		public Builder queryDataDefault(NameValuePair...parts) {
			queryData().setDefault(parts);
			return this;
		}

		/**
		 * Appends a query parameter to the URI.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.queryData(<js>"foo"</js>, <js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>queryData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value.
		 * @return This object.
		 * @see #queryData()
		 */
		@FluentSetter
		public Builder queryData(String name, String value) {
			queryData().append(name, value);
			return this;
		}

		/**
		 * Appends a query parameter with a dynamic value to the URI.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.queryData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>queryData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value supplier.
		 * @return This object.
		 * @see #queryData()
		 */
		@FluentSetter
		public Builder queryData(String name, Supplier<String> value) {
			queryData().append(name, value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// formData
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the builder for the list of form data parameters that get applied to all requests created by this builder.
		 *
		 * <p>
		 * This is the primary method for accessing the form data parameter list.
		 * On first call, the builder is created via the method {@link #createFormData()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that adds a "foo=bar" form-data parameter on every request.</jc>
		 * 	RestClient.Builder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
		 * 	<jv>builder</jv>.formData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
		 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
		 * </p>
		 *
		 * <p>
		 * The following convenience methods are also provided for updating the parameters:
		 * <ul>
		 * 	<li class='jm'>{@link #formData(NameValuePair...)}
		 * 	<li class='jm'>{@link #formDataDefault(NameValuePair...)}
		 * 	<li class='jm'>{@link #formData(String,String)}
		 * 	<li class='jm'>{@link #formData(String,Supplier)}
		 * </ul>
		 *
		 * @return The form data list builder.
		 */
		public final PartList formData() {
			if (formData == null)
				formData = createFormData();
			return formData;
		}

		/**
		 * Creates the builder for the form data list.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own implementation.
		 *
		 * <p>
		 * The default behavior creates an empty builder.
		 *
		 * @return The query data list builder.
		 * @see #formData()
		 */
		protected PartList createFormData() {
			return PartList.create();
		}

		/**
		 * Appends multiple form-data parameters to the request bodies of all URL-encoded form posts.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
		 *
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.formData(
		 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
		 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>formData().append(<jv>parts</jv>)</c>.
		 *
		 * @param parts
		 * 	The form-data parameters.
		 * @return This object.
		 * @see #formData()
		 */
		@FluentSetter
		public Builder formData(NameValuePair...parts) {
			formData().append(parts);
			return this;
		}

		/**
		 * Sets default form-data parameter values.
		 *
		 * <p>
		 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.formDataDefault(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>formData().setDefault(<jv>parts</jv>)</c>.
		 *
		 * @param parts The parts.
		 * @return This object.
		 * @see #formData()
		 */
		public Builder formDataDefault(NameValuePair...parts) {
			formData().setDefault(parts);
			return this;
		}

		/**
		 * Appends a form-data parameter to all request bodies.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>formData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value.
		 * @return This object.
		 * @see #formData()
		 */
		@FluentSetter
		public Builder formData(String name, String value) {
			formData().append(name, value);
			return this;
		}

		/**
		 * Appends a form-data parameter with a dynamic value to all request bodies.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.formData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>formData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value supplier.
		 * @return This object.
		 * @see #formData()
		 */
		@FluentSetter
		public Builder formData(String name, Supplier<String> value) {
			formData().append(name, value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// pathData
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the builder for the list of path data parameters that get applied to all requests created by this builder.
		 *
		 * <p>
		 * This is the primary method for accessing the path data parameter list.
		 * On first call, the builder is created via the method {@link #createFormData()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses "bar" for the "{foo}" path variable on every request.</jc>
		 * 	RestClient.Builder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
		 * 	<jv>builder</jv>.pathData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
		 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
		 * </p>
		 *
		 * <p>
		 * The following convenience methods are also provided for updating the parameters:
		 * <ul>
		 * 	<li class='jm'>{@link #pathData(NameValuePair...)}
		 * 	<li class='jm'>{@link #pathDataDefault(NameValuePair...)}
		 * 	<li class='jm'>{@link #pathData(String,String)}
		 * 	<li class='jm'>{@link #pathData(String,Supplier)}
		 * </ul>
		 *
		 * @return The form data list builder.
		 */
		public final PartList pathData() {
			if (pathData == null)
				pathData = createPathData();
			return pathData;
		}

		/**
		 * Creates the builder for the path data list.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own implementation.
		 *
		 * <p>
		 * The default behavior creates an empty builder.
		 *
		 * @return The query data list builder.
		 * @see #pathData()
		 */
		protected PartList createPathData() {
			return PartList.create();
		}

		/**
		 * Sets multiple path parameters on all requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
		 *
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.pathData(
		 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
		 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>pathData().append(<jv>parts</jv>)</c>.
		 *
		 * @param parts
		 * 	The path parameters.
		 * @return This object.
		 * @see #pathData()
		 */
		@FluentSetter
		public Builder pathData(NameValuePair...parts) {
			pathData().append(parts);
			return this;
		}

		/**
		 * Sets default path parameter values.
		 *
		 * <p>
		 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.pathDataDefault(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>pathData().setDefault(<jv>parts</jv>)</c>.
		 *
		 * @param parts The parts.
		 * @return This object.
		 * @see #pathData()
		 */
		public Builder pathDataDefault(NameValuePair...parts) {
			pathData().setDefault(parts);
			return this;
		}

		/**
		 * Appends a path parameter to all request bodies.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.pathData(<js>"foo"</js>, <js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>pathData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value.
		 * @return This object.
		 * @see #pathData()
		 */
		@FluentSetter
		public Builder pathData(String name, String value) {
			pathData().append(name, value);
			return this;
		}

		/**
		 * Sets a path parameter with a dynamic value to all request bodies.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.pathData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is a shortcut for calling <c>pathData().append(<jv>name</jv>,<jv>value</jv>)</c>.
		 *
		 * @param name The parameter name.
		 * @param value The parameter value supplier.
		 * @return This object.
		 * @see #pathData()
		 */
		@FluentSetter
		public Builder pathData(String name, Supplier<String> value) {
			pathData().set(name, value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// callHandler
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Returns the creator for the rest call handler.
		 *
		 * <p>
		 * Allows you to provide a custom handler for making HTTP calls.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that handles processing of requests using a custom handler.</jc>
		 * 	<jk>public class</jk> MyRestCallHandler <jk>implements</jk> RestCallHandler {
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> HttpResponse run(HttpHost <jv>target</jv>, HttpRequest <jv>request</jv>, HttpContext <jv>context</jv>) <jk>throws</jk> IOException {
		 * 			<jc>// Custom handle requests.</jc>
		 * 		}
		 * 	}
		 *
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.callHandler(MyRestCallHandler.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		The {@link RestClient#run(HttpHost, HttpRequest, HttpContext)} method can also be overridden to produce the same results.
		 * 	<li class='note'>
		 * 		Use {@link BeanCreator#impl(Object)} to specify an already instantiated instance.
		 * 	<li class='note'>
		 * 		Use {@link BeanCreator#type(Class)} to specify a subtype to instantiate.
		 * 		<br>Subclass must have a public constructor that takes in any args available
		 * 		in the bean store of this builder (including {@link RestClient} itself).
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jic'>{@link RestCallHandler}
		 * </ul>
		 *
		 * @return The creator for the rest call handler.
		 */
		public final BeanCreator<RestCallHandler> callHandler() {
			if (callHandler == null)
				callHandler = createCallHandler();
			return callHandler;
		}

		/**
		 * Creates the creator for the rest call handler.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own implementation.
		 *
		 * <p>
		 * The default behavior creates a bean creator initialized to return a {@link BasicRestCallHandler}.
		 *
		 * @return The creator for the rest call handler.
		 * @see #callHandler()
		 */
		protected BeanCreator<RestCallHandler> createCallHandler() {
			return beanStore.createBean(RestCallHandler.class).type(BasicRestCallHandler.class);
		}

		/**
		 * REST call handler class.
		 *
		 * <p>
		 * Specifies a custom handler for making HTTP calls.
		 *
		 * <p>
		 * This is a shortcut for <c>callHandler().type(<jv>value</jv>)</c>.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 * @see #callHandler()
		 */
		@FluentSetter
		public Builder callHandler(Class<? extends RestCallHandler> value) {
			callHandler().type(value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// errorCodes
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Errors codes predicate.
		 *
		 * <p>
		 * Defines a predicate to test for error codes.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that considers any 300+ responses to be errors.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.errorCodes(<jv>x</jv> -&gt; <jv>x</jv>&gt;=300)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is <code>x -&gt; x &gt;= 400</code>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder errorCodes(Predicate<Integer> value) {
			errorCodes = assertArgNotNull("value", value);
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// Logging.
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Logger.
		 *
		 * <p>
		 * Specifies the logger to use for logging.
		 *
		 * <p>
		 * If not specified, uses the following logger:
		 * <p class='bjava'>
		 * 	Logger.<jsm>getLogger</jsm>(RestClient.<jk>class</jk>.getName());
		 * </p>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that logs messages to a special logger.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.logger(Logger.<jsm>getLogger</jsm>(<js>"MyLogger"</js>))  <jc>// Log to MyLogger logger.</jc>
		 * 		.logToConsole()  <jc>// Also log to console.</jc>
		 * 		.logRequests(<jsf>FULL</jsf>, <jsf>WARNING</jsf>)  <jc>// Log requests with full detail at WARNING level.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param value The logger to use for logging.
		 * @return This object.
		 */
		@FluentSetter
		public Builder logger(Logger value) {
			logger = value;
			return this;
		}

		/**
		 * Log to console.
		 *
		 * <p>
		 * Specifies to log messages to the console.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that logs messages to a special logger.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.logToConsole()
		 * 		.logRequests(<jsf>FULL</jsf>, <jsf>INFO</jsf>)  <jc>// Level is ignored when logging to console.</jc>
		 * 		.build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder logToConsole() {
			logToConsole = true;
			return this;
		}

		/**
		 * Log requests.
		 *
		 * <p>
		 * Causes requests/responses to be logged at the specified log level at the end of the request.
		 *
		 * <p>
		 * <jsf>SIMPLE</jsf> detail produces a log message like the following:
		 * <p class='bconsole'>
		 * 	POST http://localhost:10000/testUrl, HTTP/1.1 200 OK
		 * </p>
		 *
		 * <p>
		 * <jsf>FULL</jsf> detail produces a log message like the following:
		 * <p class='bconsole'>
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
		 * {@link #logger(Logger)} method or logged to the console using the
		 * {@link #logToConsole()} method.
		 *
		 * @param detail The detail level of logging.
		 * @param level The log level.
		 * @param test A predicate to use per-request to see if the request should be logged.  If <jk>null</jk>, always logs.
		 * @return This object.
		 */
		@FluentSetter
		public Builder logRequests(DetailLevel detail, Level level, BiPredicate<RestRequest,RestResponse> test) {
			logRequests = detail;
			logRequestsLevel = level;
			logRequestsPredicate = test;
			return this;
		}

		//------------------------------------------------------------------------------------------------------------------
		// HttpClientConnectionManager methods.
		//------------------------------------------------------------------------------------------------------------------

		/**
		 * Creates the {@link HttpClientConnectionManager} returned by {@link #createConnectionManager()}.
		 *
		 * <p>
		 * Subclasses can override this method to provide their own connection manager.
		 *
		 * <p>
		 * The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager} if {@link #pooled()}
		 * was called or {@link BasicHttpClientConnectionManager} if not..
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A Builder that provides it's own customized HttpClientConnectionManager.</jc>
		 * 	<jk>public class</jk> MyBuilder <jk>extends</jk> Builder {
		 * 		<ja>@Override</ja>
		 * 		<jk>protected</jk> HttpClientConnectionManager createConnectionManager() {
		 * 			<jk>return new</jk> PoolingHttpClientConnectionManager();
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Instantiate.</jc>
		 * 	RestClient <jv>client</jv> = <jk>new</jk> MyBuilder().build();
		 * </p>
		 *
		 * @return The HTTP client builder to use to create the HTTP client.
		 */
		protected HttpClientConnectionManager createConnectionManager() {
			return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
		}

		/**
		 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
		 * instead of a {@link BasicHttpClientConnectionManager}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses pooled connections.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.pooled()
		 * 		.build();
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder pooled() {
			this.pooled = true;
			return this;
		}

		/**
		 * Assigns {@link HttpClientConnectionManager} instance.
		 *
		 * @param value New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
		 */
		@FluentSetter
		public Builder connectionManager(HttpClientConnectionManager value) {
			connectionManager = value;
			httpClientBuilder().setConnectionManager(value);
			return this;
		}

		/**
		 * Defines the connection manager is to be shared by multiple client instances.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>If the connection manager is shared its life-cycle is expected to be managed by the caller and it will not be shut down if the client is closed.
		 * </ul>
		 *
		 * @param shared New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
		 */
		@FluentSetter
		public Builder connectionManagerShared(boolean shared) {
			httpClientBuilder().setConnectionManagerShared(shared);
			return this;
		}

		/**
		 * Set up this client to use BASIC auth.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Construct a client that uses BASIC authentication.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.basicAuth(<js>"http://localhost"</js>, 80, <js>"me"</js>, <js>"mypassword"</js>)
		 * 		.build();
		 * </p>
		 *
		 * @param host The auth scope hostname.
		 * @param port The auth scope port.
		 * @param user The username.
		 * @param pw The password.
		 * @return This object.
		 */
		@FluentSetter
		public Builder basicAuth(String host, int port, String user, String pw) {
			AuthScope scope = new AuthScope(host, port);
			Credentials up = new UsernamePasswordCredentials(user, pw);
			CredentialsProvider p = new BasicCredentialsProvider();
			p.setCredentials(scope, up);
			defaultCredentialsProvider(p);
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Console print stream
		 *
		 * <p>
		 * Allows you to redirect the console output to a different print stream.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder console(PrintStream value) {
			console = value;
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Executor service.
		 *
		 * <p>
		 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
		 *
		 * <p>
		 * This executor service is used to create {@link Future} objects on the following methods:
		 * <ul>
		 * 	<li class='jm'>{@link RestRequest#runFuture()}
		 * 	<li class='jm'>{@link RestRequest#completeFuture()}
		 * 	<li class='jm'>{@link ResponseContent#asFuture(Class)} (and similar methods)
		 * </ul>
		 *
		 * <p>
		 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
		 * and a queue size of 10.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client with a customized executor service.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.executorService(<jk>new</jk> ThreadPoolExecutor(1, 1, 30, TimeUnit.<jsf>SECONDS</jsf>, <jk>new</jk> ArrayBlockingQueue&lt;Runnable&gt;(10)), <jk>true</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Use it to asynchronously run a request.</jc>
		 * 	Future&lt;RestResponse&gt; <jv>responseFuture</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).runFuture();
		 *
		 * 	<jc>// Do some other stuff.</jc>
		 *
		 * 	<jc>// Now read the response.</jc>
		 * 	String <jv>body</jv> = <jv>responseFuture</jv>.get().getContent().asString();
		 *
		 * 	<jc>// Use it to asynchronously retrieve a response.</jc>
		 * 	Future&lt;MyBean&gt; <jv>myBeanFuture</jv> = <jv>client</jv>
		 * 		.get(<jsf>URI</jsf>)
		 * 		.run()
		 * 		.getContent().asFuture(MyBean.<jk>class</jk>);
		 *
		 * 	<jc>// Do some other stuff.</jc>
		 *
		 * 	<jc>// Now read the response.</jc>
		 * 	MyBean <jv>bean</jv> = <jv>myBeanFuture</jv>.get();
		 * </p>
		 *
		 * @param executorService The executor service.
		 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
		 * @return This object.
		 */
		@FluentSetter
		public Builder executorService(ExecutorService executorService, boolean shutdownOnClose) {
			this.executorService = executorService;
			this.executorServiceShutdownOnClose = shutdownOnClose;
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Keep HttpClient open.
		 *
		 * <p>
		 * Don't close this client when the {@link RestClient#close()} method is called.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client with a customized client and don't close the client  service.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.httpClient(<jv>myHttpClient</jv>)
		 * 		.keepHttpClientOpen()
		 * 		.build();
		 *
		 * 	<jv>client</jv>.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder keepHttpClientOpen() {
			keepHttpClientOpen = true;
			return this;
		}

		/**
		 * Ignore errors.
		 *
		 * <p>
		 * When enabled, HTTP error response codes (e.g. <l>&gt;=400</l>) will not cause a {@link RestCallException} to
		 * be thrown.
		 * <p>
		 * Note that this is equivalent to <c>builder.errorCodes(x -&gt; <jk>false</jk>);</c>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that doesn't throws a RestCallException when a 500 error occurs.</jc>
		 * 	RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.ignoreErrors()
		 * 		.build()
		 * 		.get(<js>"/error"</js>)  <jc>// Throws a 500 error</jc>
		 * 		.run()
		 * 		.assertStatus().is(500);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreErrors() {
			ignoreErrors = true;
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Call interceptors.
		 *
		 * <p>
		 * Adds an interceptor that can be called to hook into specified events in the lifecycle of a single request.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 *   <jc>// Customized interceptor (note you can also extend from BasicRestCallInterceptor as well.</jc>
		 * 	<jk>public class</jk> MyRestCallInterceptor <jk>implements</jk> RestCallInterceptor {
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public void</jk> onInit(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
		 *			<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
		 *			// set on the request from the client.</jc>
		 *		}
		 *
		 *		<ja>@Override</ja>
		 *		<jk>public void</jk> onConnect(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
		 *			<jc>// Intercept immediately after an HTTP response has been received.</jc>
		 *		}
		 *
		 *		<ja>@Override</ja>
		 *		<jk>public void</jk> onClose(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
		 * 			<jc>// Intercept when the response body is consumed.</jc>
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a client with a customized interceptor.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.interceptors(MyRestCallInterceptor.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link RestClient#onCallInit(RestRequest)}, {@link RestClient#onCallConnect(RestRequest,RestResponse)}, and
		 * {@link RestClient#onCallClose(RestRequest,RestResponse)} methods can also be overridden to produce the same results.
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>Can be implementations of any of the following:
		 * 	<ul>
		 * 		<li class='jic'>{@link RestCallInterceptor}
		 * 		<li class='jic'>{@link HttpRequestInterceptor}
		 * 		<li class='jic'>{@link HttpResponseInterceptor}
		 * 	</ul>
		 * @return This object.
		 * @throws Exception If one or more interceptors could not be created.
		 */
		@FluentSetter
		public Builder interceptors(Class<?>...values) throws Exception {
			for (Class<?> c : values) {
				ClassInfo ci = ClassInfo.of(c);
				if (ci != null) {
					if (ci.isChildOfAny(RestCallInterceptor.class, HttpRequestInterceptor.class, HttpResponseInterceptor.class))
						interceptors(ci.newInstance());
					else
						throw new ConfigException("Invalid class of type ''{0}'' passed to interceptors().", ci.getName());
				}
			}
			return this;
		}

		/**
		 * Call interceptors.
		 *
		 * <p>
		 * Adds an interceptor that gets called immediately after a connection is made.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client with a customized interceptor.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.interceptors(
		 * 			<jk>new</jk> RestCallInterceptor() {
		 *
		 * 				<ja>@Override</ja>
		 * 				<jk>public void</jk> onInit(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
		 *					<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
		 *					// set on the request from the client.</jc>
		 *				}
		 *
		 *				<ja>@Override</ja>
		 *				<jk>public void</jk> onConnect(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
		 *					<jc>// Intercept immediately after an HTTP response has been received.</jc>
		 *				}
		 *
		 *				<ja>@Override</ja>
		 *				<jk>public void</jk> onClose(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
		 * 					<jc>// Intercept when the response body is consumed.</jc>
		 * 				}
		 * 			}
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>The {@link RestClient#onCallInit(RestRequest)}, {@link RestClient#onCallConnect(RestRequest,RestResponse)}, and
		 * {@link RestClient#onCallClose(RestRequest,RestResponse)} methods can also be overridden to produce the same results.
		 * </ul>
		 *
		 * @param value
		 * 	The values to add to this setting.
		 * 	<br>Can be implementations of any of the following:
		 * 	<ul>
		 * 		<li class='jic'>{@link RestCallInterceptor}
		 * 		<li class='jic'>{@link HttpRequestInterceptor}
		 * 		<li class='jic'>{@link HttpResponseInterceptor}
		 * 	</ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder interceptors(Object...value) {
			List<RestCallInterceptor> l = list();
			for (Object o : value) {
				ClassInfo ci = ClassInfo.of(o);
				if (ci != null) {
					if (! ci.isChildOfAny(HttpRequestInterceptor.class, HttpResponseInterceptor.class, RestCallInterceptor.class))
						throw new ConfigException("Invalid object of type ''{0}'' passed to interceptors().", ci.getName());
					if (o instanceof HttpRequestInterceptor)
						addInterceptorLast((HttpRequestInterceptor)o);
					if (o instanceof HttpResponseInterceptor)
						addInterceptorLast((HttpResponseInterceptor)o);
					if (o instanceof RestCallInterceptor)
						l.add((RestCallInterceptor)o);
				}
			}
			if (interceptors == null)
				interceptors = l;
			else
				interceptors.addAll(0, l);
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Enable leak detection.
		 *
		 * <p>
		 * Enable client and request/response leak detection.
		 *
		 * <p>
		 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
		 * when the <c>finalize</c> methods are invoked.
		 *
		 * <p>
		 * Automatically enabled with {@link org.apache.juneau.Context.Builder#debug()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that logs a message if </jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.detectLeaks()
		 * 		.logToConsole()  <jc>// Also log the error message to System.err</jc>
		 * 		.build();
		 *
		 * 	<jv>client</jv>.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder detectLeaks() {
			detectLeaks = true;
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Marshaller
		 *
		 * <p>
		 * Shortcut for specifying the serializers and parsers
		 * using the serializer and parser defined in a marshaller.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
		 * 	parser property setters (e.g. {@link #strict()}), or bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses Simplified-JSON transport using an existing marshaller.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.marshaller(Json5.<jsf>DEFAULT_READABLE</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder marshaller(Marshaller value) {
			if (value != null)
				serializer(value.getSerializer()).parser(value.getParser());
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Marshalls
		 *
		 * <p>
		 * Shortcut for specifying the serializers and parsers
		 * using the serializer and parser defined in a marshaller.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
		 * 	parser property setters (e.g. {@link #strict()}), or bean context property setters (e.g. {@link #swaps(Class...)}) defined on this builder class have no effect.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses JSON and XML transport using existing marshalls.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.marshaller(Json.<jsf>DEFAULT_READABLE</jsf>, Xml.<jsf>DEFAULT_READABLE</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder marshallers(Marshaller...value) {
			for (Marshaller m : value)
				if (m != null)
					serializer(m.getSerializer()).parser(m.getParser());
			return this;
		}

		/**
		 * <i><l>RestClient</l> configuration property:&emsp;</i>  Root URI.
		 *
		 * <p>
		 * When set, relative URI strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
		 * will be prefixed with the specified root.
		 * <br>This root URI is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URI string.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a client that uses UON format by default for HTTP parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.rootUrl(<js>"http://localhost:10000/foo"</js>)
		 * 		.build();
		 *
		 * 	Bar <jv>bar</jv> = <jv>client</jv>
		 * 		.get(<js>"/bar"</js>)  <jc>// Relative to http://localhost:10000/foo</jc>
		 * 		.run()
		 * 		.getContent().as(Bar.<jk>class</jk>);
		 * </p>
		 *
		 * @param value
		 * 	The root URI to prefix to relative URI strings.
		 * 	<br>Trailing slashes are trimmed.
		 * 	<br>Usually a <c>String</c> but you can also pass in <c>URI</c> and <c>URL</c> objects as well.
		 * @return This object.
		 */
		@FluentSetter
		public Builder rootUrl(Object value) {
			String s = stringify(value);
			if (! isEmpty(s))
				s = s.replaceAll("\\/$", "");
			if (isEmpty(s))
				rootUrl = null;
			else if (s.indexOf("://") == -1)
				throw new BasicRuntimeException("Invalid rootUrl value: ''{0}''.  Must be a valid absolute URL.", value);
			else
				rootUrl = s;
			return this;
		}

		/**
		 * Returns the root URI defined for this client.
		 *
		 * <p>
		 * Returns <jk>null</jk> in leu of an empty string.
		 * Trailing slashes are trimmed.
		 *
		 * @return The root URI defined for this client.
		 */
		public String getRootUri() {
			return rootUrl;
		}

		/**
		 * Skip empty form data.
		 *
		 * <p>
		 * When enabled, form data consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <jk>false</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyFormData(boolean value) {
			skipEmptyFormData = true;
			return this;
		}

		/**
		 * Skip empty form data.
		 *
		 * <p>
		 * When enabled, form data consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyFormData() {
			return skipEmptyFormData(true);
		}

		/**
		 * Skip empty header data.
		 *
		 * <p>
		 * When enabled, headers consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <jk>false</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyHeaderData(boolean value) {
			skipEmptyHeaderData = true;
			return this;
		}

		/**
		 * Skip empty header data.
		 *
		 * <p>
		 * When enabled, headers consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyHeaderData() {
			return skipEmptyHeaderData(true);
		}

		/**
		 * Skip empty query data.
		 *
		 * <p>
		 * When enabled, query parameters consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <jk>false</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyQueryData(boolean value) {
			skipEmptyQueryData = true;
			return this;
		}

		/**
		 * Skip empty query data.
		 *
		 * <p>
		 * When enabled, query parameters consisting of empty strings will be skipped on requests.
		 * Note that <jk>null</jk> values are already skipped.
		 *
		 * <p>
		 * The {@link Schema#skipIfEmpty()} annotation overrides this setting.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder skipEmptyQueryData() {
			return skipEmptyQueryData(true);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// BeanTraverse Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Automatically detect POJO recursions.
		 *
		 * <p>
		 * When enabled, specifies that recursions should be checked for during traversal.
		 *
		 * <p>
		 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
		 * <br>In general, unchecked recursions cause stack-overflow-errors.
		 * <br>These show up as {@link BeanRecursionException BeanRecursionException} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON client that automatically checks for recursions.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.detectRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> A {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	A <jv>a</jv> = <jk>new</jk> A();
		 * 	<jv>a</jv>.<jf>f</jf> = <jv>a</jv>;
		 *
		 *	<jk>try</jk> {
		 * 		<jc>// Throws a RestCallException with an inner SerializeException and not a StackOverflowError</jc>
		 * 		<jv>client</jv>
		 * 			.post(<js>"http://localhost:10000/foo"</js>, <jv>a</jv>)
		 * 			.run();
		 *	} <jk>catch</jk> (RestCallException <jv>e</jv>} {
		 *		<jc>// Handle exception.</jc>
		 *	}
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder detectRecursions() {
			serializers().forEach(x -> x.detectRecursions());
			return this;
		}

		/**
		 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Ignore recursion errors.
		 *
		 * <p>
		 * When enabled, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
		 *
		 * <p>
		 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
		 * 	the following when <jsf>BEANTRAVERSE_ignoreRecursions</jsf> is <jk>true</jk>...
		 *
		 * <p class='bjson'>
		 * 	{A:{B:{C:<jk>null</jk>}}}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON client that ignores recursions.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.ignoreRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> A {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	A <jv>a</jv> = <jk>new</jk> A();
		 * 	<jv>a</jv>.<jf>f</jf> = <jv>a</jv>;
		 *
		 * 	<jc>// Produces request body "{f:null}"</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>a</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreRecursions() {
			serializers().forEach(x -> x.ignoreRecursions());
			return this;
		}

		/**
		 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Initial depth.
		 *
		 * <p>
		 * The initial indentation level at the root.
		 *
		 * <p>
		 * Useful when constructing document fragments that need to be indented at a certain level when whitespace is enabled.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled and an initial depth of 2.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.ws()
		 * 		.initialDepth(2)
		 * 		.build();
		 *
		 * 	<jc>// Our bean to serialize.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <jk>null</jk>;
		 * 	}
		 *
		 * 	<jc>// Produces request body "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#initialDepth(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <c>0</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder initialDepth(int value) {
			serializers().forEach(x -> x.initialDepth(value));
			return this;
		}

		/**
		 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Max serialization depth.
		 *
		 * <p>
		 * When enabled, abort traversal if specified depth is reached in the POJO tree.
		 *
		 * <p>
		 * If this depth is exceeded, an exception is thrown.
		 *
		 * <p>
		 * This prevents stack overflows from occurring when trying to traverse models with recursive references.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that throws an exception if the depth reaches greater than 20.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.maxDepth(20)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#maxDepth(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <c>100</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder maxDepth(int value) {
			serializers().forEach(x -> x.maxDepth(value));
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Serializer Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * When enabled, <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
		 * <br>For example, when serializing a <c>Map&lt;String,Object&gt;</c> field where the bean class cannot be determined from
		 * the type of the values.
		 *
		 * <p>
		 * Note the differences between the following settings:
		 * <ul class='javatree'>
		 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
		 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON client that adds _type to nodes in the request body.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.addBeanTypes()
		 * 		.build();
		 *
		 * 	<jc>// Our map of beans to serialize.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	AMap <jv>map</jv> = AMap.of(<js>"foo"</js>, <jk>new</jk> MyBean());
		 *
		 * 	<jc>// Request body will contain:  {"foo":{"_type":"mybean","foo":"bar"}}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypes() {
			serializers().forEach(x -> x.addBeanTypes());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Add type attribute to root nodes.
		 *
		 * <p>
		 * When enabled, <js>"_type"</js> properties will be added to top-level beans.
		 *
		 * <p>
		 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
		 * type information that might normally be included to determine the data type will not be serialized.
		 *
		 * <p>
		 * For example, when serializing a top-level POJO with a {@link Bean#typeName() @Bean(typeName)} value, a
		 * <js>'_type'</js> attribute will only be added when this setting is enabled.
		 *
		 * <p>
		 * Note the differences between the following settings:
		 * <ul class='javatree'>
		 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
		 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON client that adds _type to root node.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.addRootType()
		 * 		.build();
		 *
		 * 	<jc>// Our bean to serialize.</jc>
		 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {"_type":"mybean","foo":"bar"}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#addRootType()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addRootType() {
			serializers().forEach(x -> x.addRootType());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Don't trim null bean property values.
		 *
		 * <p>
		 * When enabled, null bean values will be serialized to the output.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Not enabling this setting will cause <c>Map</c>s with <jk>null</jk> values to be lost during parsing.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that serializes null properties.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.keepNullProperties()
		 * 		.build();
		 *
		 * 	<jc>// Our bean to serialize.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <jk>null</jk>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {foo:null}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#keepNullProperties()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder keepNullProperties() {
			serializers().forEach(x -> x.keepNullProperties());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Sort arrays and collections alphabetically.
		 *
		 * <p>
		 * When enabled, copies and sorts the contents of arrays and collections before serializing them.
		 *
		 * <p>
		 * Note that this introduces a performance penalty since it requires copying the existing collection.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that sorts arrays and collections before serialization.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.sortCollections()
		 * 		.build();
		 *
		 * 	<jc>// An unsorted array</jc>
		 * 	String[] <jv>array</jv> = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>}
		 *
		 * 	<jc>// Request body will contain:  ["bar","baz","foo"]</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>array</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortCollections()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortCollections() {
			serializers().forEach(x -> x.sortCollections());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Sort maps alphabetically.
		 *
		 * <p>
		 * When enabled, copies and sorts the contents of maps by their keys before serializing them.
		 *
		 * <p>
		 * Note that this introduces a performance penalty.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that sorts maps before serialization.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.sortMaps()
		 * 		.build();
		 *
		 * 	<jc>// An unsorted map.</jc>
		 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
		 *
		 * 	<jc>// Request body will contain:  {"bar":2,"baz":3,"foo":1}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#sortMaps()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sortMaps() {
			serializers().forEach(x -> x.sortMaps());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim empty lists and arrays.
		 *
		 * <p>
		 * When enabled, empty lists and arrays will not be serialized.
		 *
		 * <p>
		 * Note that enabling this setting has the following effects on parsing:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		Map entries with empty list values will be lost.
		 * 	<li>
		 * 		Bean properties with empty list values will not be set.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that skips empty arrays and collections.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.trimEmptyCollections()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a field with an empty array.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String[] <jf>foo</jf> = {};
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyCollections()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyCollections() {
			serializers().forEach(x -> x.trimEmptyCollections());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim empty maps.
		 *
		 * <p>
		 * When enabled, empty map values will not be serialized to the output.
		 *
		 * <p>
		 * Note that enabling this setting has the following effects on parsing:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		Bean properties with empty map values will not be set.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that skips empty maps.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.trimEmptyMaps()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a field with an empty map.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> AMap <jf>foo</jf> = AMap.<jsm>of</jsm>();
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimEmptyMaps()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimEmptyMaps() {
			serializers().forEach(x -> x.trimEmptyMaps());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim strings.
		 *
		 * <p>
		 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that trims strings before serialization.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.trimStrings()
		 * 		.build();
		 *
		 *	<jc>// A map with space-padded keys/values</jc>
		 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
		 *
		 * 	<jc>// Request body will contain:  {"foo":"bar"}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#trimStrings()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStringsOnWrite() {
			serializers().forEach(x -> x.trimStrings());
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI context bean.
		 *
		 * <p>
		 * Bean used for resolution of URIs to absolute or root-relative form.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Our URI contextual information.</jc>
		 * 	String <jv>authority</jv> = <js>"http://localhost:10000"</js>;
		 * 	String <jv>contextRoot</jv> = <js>"/myContext"</js>;
		 * 	String <jv>servletPath</jv> = <js>"/myServlet"</js>;
		 * 	String <jv>pathInfo</jv> = <js>"/foo"</js>;
		 *
		 * 	<jc>// Create a UriContext object.</jc>
		 * 	UriContext <jv>uriContext</jv> = <jk>new</jk> UriContext(<jv>authority</jv>, <jv>contextRoot</jv>, <jv>servletPath</jv>, <jv>pathInfo</jv>);
		 *
		 * 	<jc>// Create a REST client with JSON serializer and associate our context.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.uriContext(<jv>uriContext</jv>)
		 * 		.uriRelativity(<jsf>RESOURCE</jsf>)  <jc>// Assume relative paths are relative to servlet.</jc>
		 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)  <jc>// Serialize URIs as absolute paths.</jc>
		 * 		.build();
		 *
		 * 	<jc>// A relative URI</jc>
		 * 	URI <jv>uri</jv> = <jk>new</jk> URI(<js>"bar"</js>);
		 *
		 * 	<jc>// Request body will contain:  "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jv>uri</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriContext(UriContext)}
		 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriContext(UriContext value) {
			serializers().forEach(x -> x.uriContext(value));
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI relativity.
		 *
		 * <p>
		 * Defines what relative URIs are relative to when serializing any of the following:
		 * <ul>
		 * 	<li>{@link java.net.URI}
		 * 	<li>{@link java.net.URL}
		 * 	<li>Properties and classes annotated with {@link Uri @Uri}
		 * </ul>
		 *
		 * <p>
		 * See {@link #uriContext(UriContext)} for examples.
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#RESOURCE}
		 * 		- Relative URIs should be considered relative to the servlet URI.
		 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#PATH_INFO}
		 * 		- Relative URIs should be considered relative to the request URI.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriRelativity(UriRelativity)}
		 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriRelativity#RESOURCE}
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriRelativity(UriRelativity value) {
			serializers().forEach(x -> x.uriRelativity(value));
			return this;
		}

		/**
		 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI resolution.
		 *
		 * <p>
		 * Defines the resolution level for URIs when serializing any of the following:
		 * <ul>
		 * 	<li>{@link java.net.URI}
		 * 	<li>{@link java.net.URL}
		 * 	<li>Properties and classes annotated with {@link Uri @Uri}
		 * </ul>
		 *
		 * <p>
		 * See {@link #uriContext(UriContext)} for examples.
		 *
		 * <ul class='values'>
		 * 	<li class='jf'>{@link UriResolution#ABSOLUTE}
		 * 		- Resolve to an absolute URI (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
		 * 	<li class='jf'>{@link UriResolution#ROOT_RELATIVE}
		 * 		- Resolve to a root-relative URI (e.g. <js>"/context-root/servlet-path/path-info"</js>).
		 * 	<li class='jf'>{@link UriResolution#NONE}
		 * 		- Don't do any URI resolution.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.Serializer.Builder#uriResolution(UriResolution)}
		 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MarshallingUris">URIs</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link UriResolution#NONE}
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriResolution(UriResolution value) {
			serializers().forEach(x -> x.uriResolution(value));
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// WriterSerializer Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Maximum indentation.
		 *
		 * <p>
		 * Specifies the maximum indentation level in the serialized document.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that indents a maximum of 20 tabs.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.ws()  <jc>// Enable whitespace</jc>
		 * 		.maxIndent(20)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#maxIndent(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <c>100</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder maxIndent(int value) {
			serializers().forEachWS(x -> x.maxIndent(value));
			return this;
		}

		/**
		 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Quote character.
		 *
		 * <p>
		 * Specifies the character to use for quoting attributes and values.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that uses single quotes.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.quoteChar(<js>'\''</js>)
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {'foo':'bar'}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#quoteChar(char)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <js>'"'</js>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder quoteChar(char value) {
			serializers().forEachWS(x -> x.quoteChar(value));
			return this;
		}

		/**
		 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Quote character.
		 *
		 * <p>
		 * Specifies to use single quotes for quoting attributes and values.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer that uses single quotes.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.sq()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {'foo':'bar'}</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#quoteChar(char)}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder sq() {
			serializers().forEachWS(x -> x.sq());
			return this;
		}

		/**
		 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Use whitespace.
		 *
		 * <p>
		 * When enabled, whitespace is added to the output to improve readability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.useWhitespace()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {\n\t"foo": "bar"\n\}\n</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace()}
		 * </ul>
		 * @return This object.
		 */
		@FluentSetter
		public Builder useWhitespace() {
			serializers().forEachWS(x -> x.useWhitespace());
			return this;
		}

		/**
		 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Use whitespace.
		 *
		 * <p>
		 * When enabled, whitespace is added to the output to improve readability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.ws()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Request body will contain:  {\n\t"foo": "bar"\n\}\n</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ws() {
			serializers().forEachWS(x -> x.ws());
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// OutputStreamSerializer Properties
		//-----------------------------------------------------------------------------------------------------------------

		//-----------------------------------------------------------------------------------------------------------------
		// Parser Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>Parser</l> configuration property:&emsp;</i>  Debug output lines.
		 *
		 * <p>
		 * When parse errors occur, this specifies the number of lines of input before and after the
		 * error location to be printed as part of the exception message.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser whose exceptions print out 100 lines before and after the parse error location.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.debug()  <jc>// Enable debug mode to capture Reader contents as strings.</jc>
		 * 		.debugOuputLines(100)
		 * 		.build();
		 *
		 * 	<jc>// Try to parse some bad JSON.</jc>
		 * 	<jk>try</jk> {
		 * 		<jv>client</jv>
		 * 			.get(<js>"/pathToBadJson"</js>)
		 * 			.run()
		 * 			.getContent().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
		 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
		 * 		System.<jsf>err</jsf>.println(<jv>e</jv>.getMessage());  <jc>// Will display 200 lines of the output.</jc>
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#debugOutputLines(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is <c>5</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debugOutputLines(int value) {
			parsers().forEach(x -> x.debugOutputLines(value));
			return this;
		}

		/**
		 * <i><l>Parser</l> configuration property:&emsp;</i>  Strict mode.
		 *
		 * <p>
		 * When enabled, strict mode for the parser is enabled.
		 *
		 * <p>
		 * Strict mode can mean different things for different parsers.
		 *
		 * <table class='styled'>
		 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
		 * 	<tr>
		 * 		<td>All reader-based parsers</td>
		 * 		<td>
		 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
		 * 			Otherwise, malformed input is ignored.
		 * 		</td>
		 * 	</tr>
		 * 	<tr>
		 * 		<td>{@link JsonParser}</td>
		 * 		<td>
		 * 			When enabled, throws exceptions on the following invalid JSON syntax:
		 * 			<ul>
		 * 				<li>Unquoted attributes.
		 * 				<li>Missing attribute values.
		 * 				<li>Concatenated strings.
		 * 				<li>Javascript comments.
		 * 				<li>Numbers and booleans when Strings are expected.
		 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
		 * 			</ul>
		 * 		</td>
		 * 	</tr>
		 * </table>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON parser using strict mode.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.strict()
		 * 		.build();
		 *
		 * 	<jc>// Try to parse some bad JSON.</jc>
		 * 	<jk>try</jk> {
		 * 		<jv>client</jv>
		 * 			.get(<js>"/pathToBadJson"</js>)
		 * 			.run()
		 * 			.getContent().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
		 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
		 * 		<jc>// Handle exception.</jc>
		 * 	}
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#strict()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder strict() {
			parsers().forEach(x -> x.strict());
			return this;
		}

		/**
		 * <i><l>Parser</l> configuration property:&emsp;</i>  Trim parsed strings.
		 *
		 * <p>
		 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being added to
		 * the POJO.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with JSON parser with trim-strings enabled.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.json()
		 * 		.trimStringsOnRead()
		 * 		.build();
		 *
		 * 	<jc>// Try to parse JSON containing {" foo ":" bar "}.</jc>
		 * 	Map&lt;String,String&gt; <jv>map</jv> = <jv>client</jv>
		 * 		.get(<js>"/pathToJson"</js>)
		 * 		.run()
		 * 		.getContent().as(HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
		 *
		 * 	<jc>// Make sure strings are trimmed.</jc>
		 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, <jv>map</jv>.get(<js>"foo"</js>));
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.parser.Parser.Builder#trimStrings()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStringsOnRead() {
			parsers().forEach(x -> x.trimStrings());
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// ReaderParser Properties
		//-----------------------------------------------------------------------------------------------------------------

		//-----------------------------------------------------------------------------------------------------------------
		// InputStreamParser Properties
		//-----------------------------------------------------------------------------------------------------------------

		//-----------------------------------------------------------------------------------------------------------------
		// OpenApi Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default OpenAPI format for HTTP parts.
		 *
		 * <p>
		 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#format()} for
		 * the OpenAPI serializer and parser on this client.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with UON part serialization and parsing.</jc>
		 * 	RestClient <jv>client</jv>  = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.oapiFormat(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Set a header with a value in UON format.</jc>
		 * 	<jv>client</jv>
		 * 		.get(<js>"/uri"</js>)
		 * 		.header(<js>"Foo"</js>, <js>"bar baz"</js>)  <jc>// Will be serialized as:  'bar baz'</jc>
		 * 		.run();
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT32 INT32} - Signed 32 bits.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT64 INT64} - Signed 64 bits.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
		 * 	</ul>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializer.Builder#format(HttpPartFormat)}
		 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParser.Builder#format(HttpPartFormat)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is {@link HttpPartFormat#NO_FORMAT}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder oapiFormat(HttpPartFormat value) {
			serializers().forEach(OpenApiSerializer.Builder.class, x -> x.format(value));
			parsers().forEach(OpenApiParser.Builder.class, x -> x.format(value));
			partSerializer().builder(OpenApiSerializer.Builder.class).ifPresent(x -> x.format(value));
			partParser().builder(OpenApiParser.Builder.class).ifPresent(x -> x.format(value));
			return this;
		}

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default collection format for HTTP parts.
		 *
		 * <p>
		 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#collectionFormat()} for the
		 * OpenAPI serializer and parser on this client.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with CSV format for http parts.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>CSV</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// An arbitrary data structure.</jc>
		 * 	AList <jv>list</jv> = AList.<jsm>of</jsm>(
		 * 		<js>"foo"</js>,
		 * 		<js>"bar"</js>,
		 * 		AMap.<jsm>of</jsm>(
		 * 			<js>"baz"</js>, AList.<jsm>of</jsm>(<js>"qux"</js>,<js>"true"</js>,<js>"123"</js>)
		 *		)
		 *	);
		 *
		 * 	<jc>// Set a header with a comma-separated list.</jc>
		 * 	<jv>client</jv>
		 * 		.get(<js>"/uri"</js>)
		 * 		.header(<js>"Foo"</js>, <jv>list</jv>)  <jc>// Will be serialized as: foo=bar,baz=qux\,true\,123</jc>
		 * 		.run();
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link HttpPartCollectionFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
		 * 	</ul>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializer.Builder#collectionFormat(HttpPartCollectionFormat)}
		 * 	<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParser.Builder#collectionFormat(HttpPartCollectionFormat)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is {@link HttpPartCollectionFormat#NO_COLLECTION_FORMAT}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder oapiCollectionFormat(HttpPartCollectionFormat value) {
			serializers().forEach(OpenApiSerializer.Builder.class, x -> x.collectionFormat(value));
			parsers().forEach(OpenApiParser.Builder.class, x -> x.collectionFormat(value));
			partSerializer().builder(OpenApiSerializer.Builder.class, x -> x.collectionFormat(value));
			partParser().builder(OpenApiParser.Builder.class, x -> x.collectionFormat(value));
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// UON Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>UonSerializer</l> configuration property:&emsp;</i>  Parameter format.
		 *
		 * <p>
		 * Specifies the format of parameters when using the {@link UrlEncodingSerializer} to serialize Form Posts.
		 *
		 * <p>
		 * Specifies the format to use for GET parameter keys and values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with URL-Encoded serializer that serializes values in plain-text format.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.urlEnc()
		 * 		.paramFormat(<jsf>PLAINTEXT</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// An arbitrary data structure.</jc>
		 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(
		 * 		<js>"foo"</js>, <js>"bar"</js>,
		 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
		 * 	);
		 *
		 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"/uri"</js>, <jv>map</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link ParamFormat#UON} (default) - Use UON notation for parameters.
		 * 	<li class='jf'>{@link ParamFormat#PLAINTEXT} - Use plain text for parameters.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.uon.UonSerializer.Builder#paramFormat(ParamFormat)}
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder paramFormat(ParamFormat value) {
			serializers().forEach(UonSerializer.Builder.class, x -> x.paramFormat(value));
			return this;
		}

		/**
		 * <i><l>UonSerializer</l> configuration property:&emsp;</i>  Parameter format.
		 *
		 * <p>
		 * Specifies the format of parameters when using the {@link UrlEncodingSerializer} to serialize Form Posts.
		 *
		 * <p>
		 * Specifies plaintext as the format to use for GET parameter keys and values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a REST client with URL-Encoded serializer that serializes values in plain-text format.</jc>
		 * 	RestClient <jv>client</jv> = RestClient
		 * 		.<jsm>create</jsm>()
		 * 		.urlEnc()
		 * 		.build();
		 *
		 * 	<jc>// An arbitrary data structure.</jc>
		 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(
		 * 		<js>"foo"</js>, <js>"bar"</js>,
		 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
		 * 	);
		 *
		 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
		 * 	<jv>client</jv>
		 * 		.post(<js>"/uri"</js>, <jv>map</jv>)
		 * 		.run();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.uon.UonSerializer.Builder#paramFormatPlain()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder paramFormatPlain() {
			serializers().forEach(UonSerializer.Builder.class, x -> x.paramFormatPlain());
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		// </FluentSetters>

		//------------------------------------------------------------------------------------------------
		// Passthrough methods for HttpClientBuilder.
		//------------------------------------------------------------------------------------------------

		/**
		 * Disables automatic redirect handling.
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableRedirectHandling()
		 */
		@FluentSetter
		public Builder disableRedirectHandling() {
			httpClientBuilder().disableRedirectHandling();
			return this;
		}

		/**
		 * Assigns {@link RedirectStrategy} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #disableRedirectHandling()} method.
		 * </ul>
		 *
		 * @param redirectStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setRedirectStrategy(RedirectStrategy)
		 */
		@FluentSetter
		public Builder redirectStrategy(RedirectStrategy redirectStrategy) {
			httpClientBuilder().setRedirectStrategy(redirectStrategy);
			return this;
		}

		/**
		 * Assigns default {@link CookieSpec} registry which will be used for request execution if not explicitly set in the client execution context.
		 *
		 * @param cookieSpecRegistry New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
		 */
		@FluentSetter
		public Builder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
			httpClientBuilder().setDefaultCookieSpecRegistry(cookieSpecRegistry);
			return this;
		}

		/**
		 * Assigns {@link HttpRequestExecutor} instance.
		 *
		 * @param requestExec New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
		 */
		@FluentSetter
		public Builder requestExecutor(HttpRequestExecutor requestExec) {
			httpClientBuilder().setRequestExecutor(requestExec);
			return this;
		}

		/**
		 * Assigns {@link javax.net.ssl.HostnameVerifier} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
		 * 		and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
		 * </ul>
		 *
		 * @param hostnameVerifier New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
		 */
		@FluentSetter
		public Builder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
			httpClientBuilder().setSSLHostnameVerifier(hostnameVerifier);
			return this;
		}

		/**
		 * Assigns file containing public suffix matcher.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>Instances of this class can be created with {@link PublicSuffixMatcherLoader}.
		 * </ul>
		 *
		 * @param publicSuffixMatcher New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
		 */
		@FluentSetter
		public Builder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
			httpClientBuilder().setPublicSuffixMatcher(publicSuffixMatcher);
			return this;
		}

		/**
		 * Assigns {@link SSLContext} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
		 *  	and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
		 * </ul>
		 *
		 * @param sslContext New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setSSLContext(SSLContext)
		 */
		@FluentSetter
		public Builder sslContext(SSLContext sslContext) {
			httpClientBuilder().setSSLContext(sslContext);
			return this;
		}

		/**
		 * Assigns {@link LayeredConnectionSocketFactory} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param sslSocketFactory New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
		 */
		@FluentSetter
		public Builder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
			httpClientBuilder().setSSLSocketFactory(sslSocketFactory);
			return this;
		}

		/**
		 * Assigns maximum total connection value.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param maxConnTotal New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setMaxConnTotal(int)
		 */
		@FluentSetter
		public Builder maxConnTotal(int maxConnTotal) {
			httpClientBuilder().setMaxConnTotal(maxConnTotal);
			return this;
		}

		/**
		 * Assigns maximum connection per route value.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param maxConnPerRoute New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setMaxConnPerRoute(int)
		 */
		@FluentSetter
		public Builder maxConnPerRoute(int maxConnPerRoute) {
			httpClientBuilder().setMaxConnPerRoute(maxConnPerRoute);
			return this;
		}

		/**
		 * Assigns default {@link SocketConfig}.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param config New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
		 */
		@FluentSetter
		public Builder defaultSocketConfig(SocketConfig config) {
			httpClientBuilder().setDefaultSocketConfig(config);
			return this;
		}

		/**
		 * Assigns default {@link ConnectionConfig}.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param config New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
		 */
		@FluentSetter
		public Builder defaultConnectionConfig(ConnectionConfig config) {
			httpClientBuilder().setDefaultConnectionConfig(config);
			return this;
		}

		/**
		 * Sets maximum time to live for persistent connections.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
		 * </ul>
		 *
		 * @param connTimeToLive New property value.
		 * @param connTimeToLiveTimeUnit New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
		 */
		@FluentSetter
		public Builder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
			httpClientBuilder().setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
			return this;
		}

		/**
		 * Assigns {@link ConnectionReuseStrategy} instance.
		 *
		 * @param reuseStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
		 */
		@FluentSetter
		public Builder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
			httpClientBuilder().setConnectionReuseStrategy(reuseStrategy);
			return this;
		}

		/**
		 * Assigns {@link ConnectionKeepAliveStrategy} instance.
		 *
		 * @param keepAliveStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
		 */
		@FluentSetter
		public Builder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
			httpClientBuilder().setKeepAliveStrategy(keepAliveStrategy);
			return this;
		}

		/**
		 * Assigns {@link AuthenticationStrategy} instance for target host authentication.
		 *
		 * @param targetAuthStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
		 */
		@FluentSetter
		public Builder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
			httpClientBuilder().setTargetAuthenticationStrategy(targetAuthStrategy);
			return this;
		}

		/**
		 * Assigns {@link AuthenticationStrategy} instance for proxy authentication.
		 *
		 * @param proxyAuthStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
		 */
		@FluentSetter
		public Builder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
			httpClientBuilder().setProxyAuthenticationStrategy(proxyAuthStrategy);
			return this;
		}

		/**
		 * Assigns {@link UserTokenHandler} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #disableConnectionState()} method.
		 * </ul>
		 *
		 * @param userTokenHandler New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
		 */
		@FluentSetter
		public Builder userTokenHandler(UserTokenHandler userTokenHandler) {
			httpClientBuilder().setUserTokenHandler(userTokenHandler);
			return this;
		}

		/**
		 * Disables connection state tracking.
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableConnectionState()
		 */
		@FluentSetter
		public Builder disableConnectionState() {
			httpClientBuilder().disableConnectionState();
			return this;
		}

		/**
		 * Assigns {@link SchemePortResolver} instance.
		 *
		 * @param schemePortResolver New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
		 */
		@FluentSetter
		public Builder schemePortResolver(SchemePortResolver schemePortResolver) {
			httpClientBuilder().setSchemePortResolver(schemePortResolver);
			return this;
		}

		/**
		 * Adds this protocol interceptor to the head of the protocol processing list.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @param itcp New property value.
		 * @return This object.
		 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
		 */
		@FluentSetter
		public Builder addInterceptorFirst(HttpResponseInterceptor itcp) {
			httpClientBuilder().addInterceptorFirst(itcp);
			return this;
		}

		/**
		 * Adds this protocol interceptor to the tail of the protocol processing list.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @param itcp New property value.
		 * @return This object.
		 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
		 */
		@FluentSetter
		public Builder addInterceptorLast(HttpResponseInterceptor itcp) {
			httpClientBuilder().addInterceptorLast(itcp);
			return this;
		}

		/**
		 * Adds this protocol interceptor to the head of the protocol processing list.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @param itcp New property value.
		 * @return This object.
		 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
		 */
		@FluentSetter
		public Builder addInterceptorFirst(HttpRequestInterceptor itcp) {
			httpClientBuilder().addInterceptorFirst(itcp);
			return this;
		}

		/**
		 * Adds this protocol interceptor to the tail of the protocol processing list.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @param itcp New property value.
		 * @return This object.
		 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
		 */
		@FluentSetter
		public Builder addInterceptorLast(HttpRequestInterceptor itcp) {
			httpClientBuilder().addInterceptorLast(itcp);
			return this;
		}

		/**
		 * Disables state (cookie) management.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableCookieManagement()
		 */
		@FluentSetter
		public Builder disableCookieManagement() {
			httpClientBuilder().disableCookieManagement();
			return this;
		}

		/**
		 * Disables automatic content decompression.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableContentCompression()
		 */
		@FluentSetter
		public Builder disableContentCompression() {
			httpClientBuilder().disableContentCompression();
			return this;
		}

		/**
		 * Disables authentication scheme caching.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
		 * </ul>
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableAuthCaching()
		 */
		@FluentSetter
		public Builder disableAuthCaching() {
			httpClientBuilder().disableAuthCaching();
			return this;
		}

		/**
		 * Assigns {@link HttpProcessor} instance.
		 *
		 * @param httpprocessor New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
		 */
		@FluentSetter
		public Builder httpProcessor(HttpProcessor httpprocessor) {
			httpClientBuilder().setHttpProcessor(httpprocessor);
			return this;
		}

		/**
		 * Assigns {@link HttpRequestRetryHandler} instance.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #disableAutomaticRetries()} method.
		 * </ul>
		 *
		 * @param retryHandler New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
		 */
		@FluentSetter
		public Builder retryHandler(HttpRequestRetryHandler retryHandler) {
			httpClientBuilder().setRetryHandler(retryHandler);
			return this;
		}

		/**
		 * Disables automatic request recovery and re-execution.
		 *
		 * @return This object.
		 * @see HttpClientBuilder#disableAutomaticRetries()
		 */
		@FluentSetter
		public Builder disableAutomaticRetries() {
			httpClientBuilder().disableAutomaticRetries();
			return this;
		}

		/**
		 * Assigns default proxy value.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This value can be overridden by the {@link #routePlanner(HttpRoutePlanner)} method.
		 * </ul>
		 *
		 * @param proxy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setProxy(HttpHost)
		 */
		@FluentSetter
		public Builder proxy(HttpHost proxy) {
			httpClientBuilder().setProxy(proxy);
			return this;
		}

		/**
		 * Assigns {@link HttpRoutePlanner} instance.
		 *
		 * @param routePlanner New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
		 */
		@FluentSetter
		public Builder routePlanner(HttpRoutePlanner routePlanner) {
			httpClientBuilder().setRoutePlanner(routePlanner);
			return this;
		}

		/**
		 * Assigns {@link ConnectionBackoffStrategy} instance.
		 *
		 * @param connectionBackoffStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
		 */
		@FluentSetter
		public Builder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
			httpClientBuilder().setConnectionBackoffStrategy(connectionBackoffStrategy);
			return this;
		}

		/**
		 * Assigns {@link BackoffManager} instance.
		 *
		 * @param backoffManager New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
		 */
		@FluentSetter
		public Builder backoffManager(BackoffManager backoffManager) {
			httpClientBuilder().setBackoffManager(backoffManager);
			return this;
		}

		/**
		 * Assigns {@link ServiceUnavailableRetryStrategy} instance.
		 *
		 * @param serviceUnavailStrategy New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
		 */
		@FluentSetter
		public Builder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
			httpClientBuilder().setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
			return this;
		}

		/**
		 * Assigns default {@link CookieStore} instance which will be used for request execution if not explicitly set in the client execution context.
		 *
		 * @param cookieStore New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
		 */
		@FluentSetter
		public Builder defaultCookieStore(CookieStore cookieStore) {
			httpClientBuilder().setDefaultCookieStore(cookieStore);
			return this;
		}

		/**
		 * Assigns default {@link CredentialsProvider} instance which will be used for request execution if not explicitly set in the client execution context.
		 *
		 * @param credentialsProvider New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
		 */
		@FluentSetter
		public Builder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
			httpClientBuilder().setDefaultCredentialsProvider(credentialsProvider);
			return this;
		}

		/**
		 * Assigns default {@link org.apache.http.auth.AuthScheme} registry which will be used for request execution if not explicitly set in the client execution context.
		 *
		 * @param authSchemeRegistry New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
		 */
		@FluentSetter
		public Builder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
			httpClientBuilder().setDefaultAuthSchemeRegistry(authSchemeRegistry);
			return this;
		}

		/**
		 * Assigns a map of {@link org.apache.http.client.entity.InputStreamFactory InputStreamFactories} to be used for automatic content decompression.
		 *
		 * @param contentDecoderMap New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
		 */
		@FluentSetter
		public Builder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
			httpClientBuilder().setContentDecoderRegistry(contentDecoderMap);
			return this;
		}

		/**
		 * Assigns default {@link RequestConfig} instance which will be used for request execution if not explicitly set in the client execution context.
		 *
		 * @param config New property value.
		 * @return This object.
		 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
		 */
		@FluentSetter
		public Builder defaultRequestConfig(RequestConfig config) {
			httpClientBuilder().setDefaultRequestConfig(config);
			return this;
		}

		/**
		 * Use system properties when creating and configuring default implementations.
		 *
		 * @return This object.
		 * @see HttpClientBuilder#useSystemProperties()
		 */
		@FluentSetter
		public Builder useSystemProperties() {
			httpClientBuilder().useSystemProperties();
			return this;
		}

		/**
		 * Makes this instance of {@link HttpClient} proactively evict expired connections from the connection pool using a background thread.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
		 * 	<li class='note'>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
		 * 	<li class='note'>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
		 * </ul>
		 *
		 * @return This object.
		 * @see HttpClientBuilder#evictExpiredConnections()
		 */
		@FluentSetter
		public Builder evictExpiredConnections() {
			httpClientBuilder().evictExpiredConnections();
			return this;
		}

		/**
		 * Makes this instance of {@link HttpClient} proactively evict idle connections from the connection pool using a background thread.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
		 * 	<li class='note'>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
		 * 	<li class='note'>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
		 * </ul>
		 *
		 * @param maxIdleTime New property value.
		 * @param maxIdleTimeUnit New property value.
		 * @return This object.
		 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
		 */
		@FluentSetter
		public Builder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
			httpClientBuilder().evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
			return this;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HeaderList headerData;
	final PartList queryData, formData, pathData;
	final CloseableHttpClient httpClient;

	private final HttpClientConnectionManager connectionManager;
	private final boolean keepHttpClientOpen, detectLeaks, skipEmptyHeaderData, skipEmptyQueryData, skipEmptyFormData;
	private final BeanStore beanStore;
	private final UrlEncodingSerializer urlEncodingSerializer;  // Used for form posts only.
	final HttpPartSerializer partSerializer;
	final HttpPartParser partParser;
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
	private final PrintStream console;
	private StackTraceElement[] closedStack;
	private static final ConcurrentHashMap<Class<?>,Context> requestContexts = new ConcurrentHashMap<>();

	// These are read directly by RestCall.
	final SerializerSet serializers;
	final ParserSet parsers;
	Predicate<Integer> errorCodes;

	final RestCallInterceptor[] interceptors;

	private final Map<Class<?>, HttpPartParser> partParsers = new ConcurrentHashMap<>();
	private final Map<Class<?>, HttpPartSerializer> partSerializers = new ConcurrentHashMap<>();

	// This is lazy-created.
	private volatile ExecutorService executorService;
	private final boolean executorServiceShutdownOnClose;

	private static final
		BiPredicate<RestRequest,RestResponse> LOG_REQUESTS_PREDICATE_DEFAULT = (req,res) -> true;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this client.
	 */
	public RestClient(Builder builder) {
		super(builder);

		beanStore = builder.beanStore
			.addBean(RestClient.class, this);

		httpClient = builder.getHttpClient();
		headerData = builder.headers().copy();
		queryData = builder.queryData().copy();
		formData = builder.formData().copy();
		pathData = builder.pathData().copy();
		callHandler = builder.callHandler().run();
		skipEmptyHeaderData = builder.skipEmptyHeaderData;
		skipEmptyQueryData = builder.skipEmptyQueryData;
		skipEmptyFormData = builder.skipEmptyFormData;
		rootUrl = builder.rootUrl;
		errorCodes = builder.errorCodes;
		connectionManager = builder.connectionManager;
		console = builder.console != null ? builder.console : System.err;
		executorService = builder.executorService;
		executorServiceShutdownOnClose = builder.executorServiceShutdownOnClose;
		ignoreErrors = builder.ignoreErrors;
		keepHttpClientOpen = builder.keepHttpClientOpen;
		detectLeaks = builder.detectLeaks;
		logger = builder.logger != null ? builder.logger : Logger.getLogger(RestClient.class.getName());
		logToConsole = builder.logToConsole || isDebug();
		logRequests = builder.logRequests != null ? builder.logRequests : isDebug() ? DetailLevel.FULL : DetailLevel.NONE;
		logRequestsLevel = builder.logRequestsLevel != null ? builder.logRequestsLevel : isDebug() ? Level.WARNING : Level.OFF;
		logRequestsPredicate = builder.logRequestsPredicate != null ? builder.logRequestsPredicate : LOG_REQUESTS_PREDICATE_DEFAULT;
		interceptors = builder.interceptors != null ? builder.interceptors.toArray(EMPTY_REST_CALL_INTERCEPTORS) : EMPTY_REST_CALL_INTERCEPTORS;
		serializers = builder.serializers().build();
		parsers = builder.parsers().build();
		partSerializer = builder.partSerializer().create();
		partParser = builder.partParser().create();
		urlEncodingSerializer = builder.urlEncodingSerializer().build();
		creationStack = isDebug() ? Thread.currentThread().getStackTrace() : null;

		init();
	}

	@Override /* Context */
	public Builder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	/**
	 * Perform optional initialization on builder before it is used.
	 *
	 * <p>
	 * Default behavior is a no-op.
	 *
	 * @param builder The builder to initialize.
	 */
	protected void init(RestClient.Builder builder) {}

	/**
	 * Gets called add the end of the constructor call to perform any post-initialization.
	 */
	protected void init() {
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
	 * Entrypoint for executing all requests and returning a response.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * <p>
	 * The behavior of this method can also be modified by specifying a different {@link RestCallHandler}.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link Builder#callHandler()}
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
	protected HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws ClientProtocolException, IOException {
		return callHandler.run(target, request, context);
	}

	/**
	 * Perform a <c>GET</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest get(Object uri) throws RestCallException {
		return request(op("GET", uri, NO_BODY));
	}

	/**
	 * Perform a <c>GET</c> request against the root URI.
	 *
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest get() throws RestCallException {
		return request(op("GET", null, NO_BODY));
	}

	/**
	 * Perform a <c>PUT</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} / {@link HttpResource} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest put(Object uri, Object body) throws RestCallException {
		return request(op("PUT", uri, body));
	}

	/**
	 * Perform a <c>PUT</c> request against the specified URI using a plain text body bypassing the serializer.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request bypassing the serializer.
	 * @param contentType The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest put(Object uri, String body, ContentType contentType) throws RestCallException {
		return request(op("PUT", uri, stringBody(body))).header(contentType);
	}

	/**
	 * Same as {@link #put(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestRequest#content(Object)} or {@link RestRequest#formData(String, Object)}
	 * to set the contents on the result object.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest put(Object uri) throws RestCallException {
		return request(op("PUT", uri, NO_BODY));
	}

	/**
	 * Perform a <c>POST</c> request against the specified URI.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Use {@link #formPost(Object, Object)} for <c>application/x-www-form-urlencoded</c> form posts.
	 * </ul>
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} / {@link HttpResource} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest post(Object uri, Object body) throws RestCallException {
		return request(op("POST", uri, body));
	}

	/**
	 * Perform a <c>POST</c> request against the specified URI as a plain text body bypassing the serializer.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request bypassing the serializer.
	 * @param contentType
	 * 	The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest post(Object uri, String body, ContentType contentType) throws RestCallException {
		return request(op("POST", uri, stringBody(body))).header(contentType);
	}

	/**
	 * Same as {@link #post(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestRequest#content(Object)} or {@link RestRequest#formData(String, Object)} to set the
	 * contents on the result object.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Use {@link #formPost(Object, Object)} for <c>application/x-www-form-urlencoded</c> form posts.
	 * </ul>
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest post(Object uri) throws RestCallException {
		return request(op("POST", uri, NO_BODY));
	}

	/**
	 * Perform a <c>DELETE</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest delete(Object uri) throws RestCallException {
		return request(op("DELETE", uri, NO_BODY));
	}

	/**
	 * Perform an <c>OPTIONS</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest options(Object uri) throws RestCallException {
		return request(op("OPTIONS", uri, NO_BODY));
	}

	/**
	 * Perform a <c>HEAD</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest head(Object uri) throws RestCallException {
		return request(op("HEAD", uri, NO_BODY));
	}

	/**
	 * Perform a <c>POST</c> request with a content type of <c>application/x-www-form-urlencoded</c>
	 * against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request.
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link NameValuePair} - URL-encoded as a single name-value pair.
	 * 		<li>{@link NameValuePair} array - URL-encoded as name value pairs.
	 * 		<li>{@link PartList} - URL-encoded as name value pairs.
	 * 		<li>{@link Reader}/{@link InputStream}- Streamed directly and <l>Content-Type</l> set to <js>"application/x-www-form-urlencoded"</js>
	 * 		<li>{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>{@link Object} - Converted to a {@link SerializedEntity} using {@link UrlEncodingSerializer} to serialize.
	 * 		<li>{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPost(Object uri, Object body) throws RestCallException {
		RestRequest req = request(op("POST", uri, NO_BODY));
		try {
			if (body instanceof Supplier)
				body = ((Supplier<?>)body).get();
			if (body instanceof NameValuePair)
				return req.content(new UrlEncodedFormEntity(alist((NameValuePair)body)));
			if (body instanceof NameValuePair[])
				return req.content(new UrlEncodedFormEntity(alist((NameValuePair[])body)));
			if (body instanceof PartList)
				return req.content(new UrlEncodedFormEntity(((PartList)body)));
			if (body instanceof HttpResource)
				((HttpResource)body).getHeaders().forEach(x-> req.header(x));
			if (body instanceof HttpEntity) {
				HttpEntity e = (HttpEntity)body;
				if (e.getContentType() == null)
					req.header(ContentType.APPLICATION_FORM_URLENCODED);
				return req.content(e);
			}
			if (body instanceof Reader || body instanceof InputStream)
				return req.header(ContentType.APPLICATION_FORM_URLENCODED).content(body);
			return req.content(serializedEntity(body, urlEncodingSerializer, null));
		} catch (IOException e) {
			throw new RestCallException(null, e, "Could not read form post body.");
		}
	}

	/**
	 * Same as {@link #formPost(Object, Object)} but doesn't specify the input yet.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPost(Object uri) throws RestCallException {
		return request(op("POST", uri, NO_BODY));
	}

	/**
	 * Perform a <c>POST</c> request with a content type of <c>application/x-www-form-urlencoded</c>
	 * against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param parameters
	 * 	The parameters of the form post.
	 * 	<br>The parameters represent name/value pairs and must be an even number of arguments.
	 * 	<br>Parameters are converted to {@link BasicPart} objects.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest formPostPairs(Object uri, String...parameters) throws RestCallException {
		return formPost(uri, partList(parameters));
	}

	/**
	 * Perform a <c>PATCH</c> request against the specified URI.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest patch(Object uri, Object body) throws RestCallException {
		return request(op("PATCH", uri, body));
	}

	/**
	 * Perform a <c>PATCH</c> request against the specified URI as a plain text body bypassing the serializer.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The object to serialize and transmit to the URI as the body of the request bypassing the serializer.
	 * @param contentType
	 * 	The content type of the request.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest patch(Object uri, String body, ContentType contentType) throws RestCallException {
		return request(op("PATCH", uri, stringBody(body))).header(contentType);
	}

	/**
	 * Same as {@link #patch(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call {@link RestRequest#content(Object)} to set the contents on the result object.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException REST call failed.
	 */
	public RestRequest patch(Object uri) throws RestCallException {
		return request(op("PATCH", uri, NO_BODY));
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
	 * 		<js>"[method] [uri]"</js> - e.g. <js>"GET http://localhost/callback"</js>
	 * 	<li>
	 * 		<js>"[method] [uri] [payload]"</js> - e.g. <js>"POST http://localhost/callback some text payload"</js>
	 * 	<li>
	 * 		<js>"[method] [headers] [uri] [payload]"</js> - e.g. <js>"POST {'Content-Type':'text/json'} http://localhost/callback {'some':'json'}"</js>
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
		// S02 - Found end of method, looking for beginning of URI or headers.
		// S03 - Found beginning of headers, looking for end of headers.
		// S04 - Found end of headers, looking for beginning of URI.
		// S05 - Found beginning of URI, looking for end of URI.

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
			throw new RestCallException(null, null, "Invalid format for call string.  State={0}", state);

		try {
			RestRequest req = request(method, uri, isNotEmpty(content));
			if (headers != null)
				JsonMap.ofJson(headers).forEach((k,v) -> req.header(stringHeader(k, stringify(v))));
			if (isNotEmpty(content))
				req.contentString(content);
			return req;
		} catch (ParseException e) {
			throw new RestCallException(null, e, "Invalid format for call string.");
		}
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The HTTP method.
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param body
	 * 	The HTTP body content.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			{@link Supplier} - A supplier of anything on this list.
	 * 	</ul>
	 * 	This parameter is IGNORED if the method type normally does not have content.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(String method, Object uri, Object body) throws RestCallException {
		return request(op(method, uri, body));
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The HTTP method.
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(String method, Object uri) throws RestCallException {
		return request(op(method, uri, NO_BODY));
	}

	/**
	 * Perform a generic REST call.
	 *
	 * <p>
	 * Typically you're going to use {@link #request(String, Object)} or {@link #request(String, Object, Object)},
	 * but this method is provided to allow you to perform non-standard HTTP methods (e.g. HTTP FOO).
	 *
	 * @param method The method name (e.g. <js>"GET"</js>, <js>"OPTIONS"</js>).
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @param hasBody Boolean flag indicating if the specified request has content associated with it.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestRequest request(String method, Object uri, boolean hasBody) throws RestCallException {
		return request(op(method, uri, NO_BODY).hasContent(hasBody));
	}

	/**
	 * Perform an arbitrary request against the specified URI.
	 *
	 * <p>
	 * All requests feed through this method so it can be used to intercept request creations and make modifications
	 * (such as add headers).
	 *
	 * @param op The operation that identifies the HTTP method, URL, and optional payload.
	 * @return
	 * 	A {@link RestRequest} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	protected RestRequest request(RestOperation op) throws RestCallException {
		if (isClosed) {
			Exception e2 = null;
			if (closedStack != null) {
				e2 = new Exception("Creation stack:");
				e2.setStackTrace(closedStack);
				throw new RestCallException(null, e2, "RestClient.close() has already been called.  This client cannot be reused.");
			}
			throw new RestCallException(null, null, "RestClient.close() has already been called.  This client cannot be reused.  Closed location stack trace can be displayed by setting the system property 'org.apache.juneau.rest.client2.RestClient.trackCreation' to true.");
		}

		RestRequest req = createRequest(toURI(op.getUri(), rootUrl), op.getMethod(), op.hasContent());

		onCallInit(req);

		req.content(op.getContent());

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
	 * Creates a {@link RestResponse} object from the specified {@link HttpResponse} object.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own specialized {@link RestResponse} objects.
	 *
	 * @param request The request creating this response.
	 * @param httpResponse The response object to wrap.
	 * @param parser The parser to use to parse the response.
	 *
	 * @return A new {@link RestResponse} object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new RestResponse(this, request, httpResponse, parser);
	}

	/**
	 * Create a new proxy interface against a 3rd-party REST interface.
	 *
	 * <p>
	 * The URI to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link Remote#path() @Remote(path)} annotation on the interface (<c>remote-path</c>).
	 * 	<li>The {@link Builder#rootUrl(Object) rootUrl} on the client (<c>root-url</c>).
	 * 	<li>The fully-qualified class name of the interface (<c>class-name</c>).
	 * </ul>
	 *
	 * <p>
	 * The URI calculation is as follows:
	 * <ul>
	 * 	<li><c>remote-path</c> - If remote path is absolute.
	 * 	<li><c>root-uri/remote-path</c> - If remote path is relative and root-uri has been specified.
	 * 	<li><c>root-uri/class-name</c> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URI, a {@link RemoteMetadataException} is thrown.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jk>package</jk> org.apache.foo;
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"http://hostname/resturi/myinterface1"</js>)
	 * 	<jk>public interface</jk> MyInterface1 { ... }
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myinterface2"</js>)
	 * 	<jk>public interface</jk> MyInterface2 { ... }
	 *
	 * 	<jk>public interface</jk> MyInterface3 { ... }
	 *
	 * 	<jc>// Resolves to "http://localhost/resturi/myinterface1"</jc>
	 * 	MyInterface1 <jv>interface1</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.build()
	 * 		.getRemote(MyInterface1.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturi/myinterface2"</jc>
	 * 	MyInterface2 <jv>interface2</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturi"</js>)
	 * 		.build()
	 * 		.getRemote(MyInterface2.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturi/org.apache.foo.MyInterface3"</jc>
	 * 	MyInterface3 <jv>interface3</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturi"</js>)
	 * 		.build()
	 * 		.getRemote(MyInterface3.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Proxies">REST Proxies</a>
	 * </ul>
	 *
	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRemote(Class<T> interfaceClass) {
		return getRemote(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRemote(Class)} except explicitly specifies the URI of the REST interface.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Proxies">REST Proxies</a>
	 * </ul>
	 *
	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @param rootUrl The URI of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRemote(Class<T> interfaceClass, Object rootUrl) {
		return getRemote(interfaceClass, rootUrl, null, null);
	}

	/**
	 * Same as {@link #getRemote(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Proxies">REST Proxies</a>
	 * </ul>

	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @param rootUrl The URI of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRemote(final Class<T> interfaceClass, Object rootUrl, final Serializer serializer, final Parser parser) {

		if (rootUrl == null)
			rootUrl = this.rootUrl;

		final String restUrl2 = trimSlashes(emptyIfNull(rootUrl));

		return (T)Proxy.newProxyInstance(
			interfaceClass.getClassLoader(),
			new Class[] { interfaceClass },
			new InvocationHandler() {

				final RemoteMeta rm = new RemoteMeta(interfaceClass);

				@Override /* InvocationHandler */
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					RemoteOperationMeta rom = rm.getOperationMeta(method);

					String uri = rom.getFullPath();
					if (uri.indexOf("://") == -1)
						uri = restUrl2 + '/' + uri;
					if (uri.indexOf("://") == -1)
						throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote resource.");

					String httpMethod = rom.getHttpMethod();
					RestRequest rc = request(httpMethod, uri, hasContent(httpMethod));

					rc.serializer(serializer);
					rc.parser(parser);

					rm.getHeaders().forEach(x -> rc.header(x));
					rom.forEachPathArg(a -> rc.pathArg(a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer().orElse(partSerializer)));
					rom.forEachQueryArg(a -> rc.queryArg(a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer().orElse(partSerializer), a.isSkipIfEmpty()));
					rom.forEachFormDataArg(a -> rc.formDataArg(a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer().orElse(partSerializer), a.isSkipIfEmpty()));
					rom.forEachHeaderArg(a -> rc.headerArg(a.getName(), args[a.getIndex()], a.getSchema(), a.getSerializer().orElse(partSerializer), a.isSkipIfEmpty()));

					RemoteOperationArg ba = rom.getContentArg();
					if (ba != null)
						rc.content(args[ba.getIndex()], ba.getSchema());

					rom.forEachRequestArg(rmba -> {
							RequestBeanMeta rbm = rmba.getMeta();
							Object bean = args[rmba.getIndex()];
							if (bean != null) {
								for (RequestBeanPropertyMeta p : rbm.getProperties()) {
									Object val = safeSupplier(()->p.getGetter().invoke(bean));
									HttpPartType pt = p.getPartType();
									String pn = p.getPartName();
									HttpPartSchema schema = p.getSchema();
									if (pt == PATH)
										rc.pathArg(pn, val, schema, p.getSerializer().orElse(partSerializer));
									else if (val != null) {
										if (pt == QUERY)
											rc.queryArg(pn, val, schema, p.getSerializer().orElse(partSerializer), schema.isSkipIfEmpty());
										else if (pt == FORMDATA)
											rc.formDataArg(pn, val, schema, p.getSerializer().orElse(partSerializer), schema.isSkipIfEmpty());
										else if (pt == HEADER)
											rc.headerArg(pn, val, schema, p.getSerializer().orElse(partSerializer), schema.isSkipIfEmpty());
										else /* (pt == HttpPartType.BODY) */
											rc.content(val, schema);
									}
								}
							}
					});

					RemoteOperationReturn ror = rom.getReturns();
					if (ror.isFuture()) {
						return getExecutorService().submit(new Callable<Object>() {
							@Override
							public Object call() throws Exception {
								try {
									return executeRemote(interfaceClass, rc, method, rom);
								} catch (Exception e) {
									throw e;
								} catch (Throwable e) {
									throw asRuntimeException(e);
								}
							}
						});
					} else if (ror.isCompletableFuture()) {
						CompletableFuture<Object> cf = new CompletableFuture<>();
						getExecutorService().submit(new Callable<Object>() {
							@Override
							public Object call() throws Exception {
								try {
									cf.complete(executeRemote(interfaceClass, rc, method, rom));
								} catch (Throwable e) {
									cf.completeExceptionally(e);
								}
								return null;
							}
						});
						return cf;
					}

					return executeRemote(interfaceClass, rc, method, rom);
				}
		});
	}

	Object executeRemote(Class<?> interfaceClass, RestRequest rc, Method method, RemoteOperationMeta rom) throws Throwable {
		RemoteOperationReturn ror = rom.getReturns();

		try {
			Object ret = null;
			RestResponse res = null;
			rc.rethrow(RuntimeException.class);
			rom.forEachException(x -> rc.rethrow(x));
			if (ror.getReturnValue() == RemoteReturn.NONE) {
				res = rc.complete();
			} else if (ror.getReturnValue() == RemoteReturn.STATUS) {
				res = rc.complete();
				int returnCode = res.getStatusCode();
				Class<?> rt = method.getReturnType();
				if (rt == Integer.class || rt == int.class)
					ret = returnCode;
				else if (rt == Boolean.class || rt == boolean.class)
					ret = returnCode < 400;
				else
					throw new RestCallException(res, null, "Invalid return type on method annotated with @RemoteOp(returns=RemoteReturn.STATUS).  Only integer and booleans types are valid.");
			} else if (ror.getReturnValue() == RemoteReturn.BEAN) {
				rc.ignoreErrors();
				res = rc.run();
				ret = res.as(ror.getResponseBeanMeta());
			} else {
				Class<?> rt = method.getReturnType();
				if (Throwable.class.isAssignableFrom(rt))
					rc.ignoreErrors();
				res = rc.run();
				Object v = res.getContent().as(ror.getReturnType());
				if (v == null && rt.isPrimitive())
					v = ClassInfo.of(rt).getPrimitiveDefault();
				ret = v;
			}
			return ret;
		} catch (RestCallException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException)
				throw t;
			for (Class<?> t2 : method.getExceptionTypes())
				if (t2.isInstance(t))
					throw t;
			throw asRuntimeException(e);
		}
	}

	/**
	 * Create a new proxy interface against an RRPC-style service.
	 *
	 * <p>
	 * Remote interfaces are interfaces exposed on the server side using either the <c>RrpcServlet</c>
	 * or <c>RRPC</c> REST methods.
	 *
	 * <p>
	 * The URI to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link Remote#path() @Remote(path)} annotation on the interface (<c>remote-path</c>).
	 * 	<li>The {@link Builder#rootUrl(Object) rootUrl} on the client (<c>root-url</c>).
	 * 	<li>The fully-qualified class name of the interface (<c>class-name</c>).
	 * </ul>
	 *
	 * <p>
	 * The URI calculation is as follows:
	 * <ul>
	 * 	<li><c>remote-path</c> - If remote path is absolute.
	 * 	<li><c>root-url/remote-path</c> - If remote path is relative and root-url has been specified.
	 * 	<li><c>root-url/class-name</c> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URI, a {@link RemoteMetadataException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
	 * </ul>
	 *
	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass) {
		return getRrpcInterface(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class)} except explicitly specifies the URI of the REST interface.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
	 * </ul>
	 *
	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @param uri The URI of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass, final Object uri) {
		return getRrpcInterface(interfaceClass, uri, null, null);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
	 * </ul>
	 *
	 * @param <T> The interface to create a proxy for.
	 * @param interfaceClass The interface to create a proxy for.
	 * @param uri The URI of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRrpcInterface(final Class<T> interfaceClass, Object uri, final Serializer serializer, final Parser parser) {

		if (uri == null) {
			RrpcInterfaceMeta rm = new RrpcInterfaceMeta(interfaceClass, "");
			String path = rm.getPath();
			if (path.indexOf("://") == -1) {
				if (isEmpty(rootUrl))
					throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote interface.");
				path = trimSlashes(rootUrl) + '/' + path;
			}
			uri = path;
		}

		final String restUrl2 = stringify(uri);

		return (T)Proxy.newProxyInstance(
			interfaceClass.getClassLoader(),
			new Class[] { interfaceClass },
			new InvocationHandler() {

				final RrpcInterfaceMeta rm = new RrpcInterfaceMeta(interfaceClass, restUrl2);

				@Override /* InvocationHandler */
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					RrpcInterfaceMethodMeta rim = rm.getMethodMeta(method);

					String uri = rim.getUri();
					RestResponse res = null;

					try {
						RestRequest rc = request("POST", uri, true)
							.serializer(serializer)
							.content(args)
							.rethrow(RuntimeException.class)
							.rethrow(method.getExceptionTypes());

						res = rc.run();

						Object v = res.getContent().as(method.getGenericReturnType());
						if (v == null && method.getReturnType().isPrimitive())
							v = ClassInfo.of(method.getReturnType()).getPrimitiveDefault();
						return v;

					} catch (Throwable e) {
						if (e instanceof RestCallException) {
							Throwable t = e.getCause();
							if (t != null)
								e = t;
						}
						if (e instanceof RuntimeException)
							throw e;
						for (Class<?> t2 : method.getExceptionTypes())
							if (t2.isInstance(e))
								throw e;
						throw asRuntimeException(e);
					}
				}
		});
	}

	@Override
	protected void finalize() throws Throwable {
		if (detectLeaks && ! isClosed && ! keepHttpClientOpen) {
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
			console.println(msg(msg, args).get());
			if (t != null)
				t.printStackTrace(console);
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
			console.println(msg(msg, args).get());
	}

	private Supplier<String> msg(String msg, Object...args) {
		return ()->args.length == 0 ? msg : MessageFormat.format(msg, args);
	}

	/**
	 * Returns the part serializer associated with this client.
	 *
	 * @return The part serializer associated with this client.
	 */
	protected HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the part parser associated with this client.
	 *
	 * @return The part parser associated with this client.
	 */
	protected HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the part serializer instance of the specified type.
	 *
	 * @param c The part serializer class.
	 * @return The part serializer.
	 */
	protected HttpPartSerializer getPartSerializer(Class<? extends HttpPartSerializer> c) {
		HttpPartSerializer x = partSerializers.get(c);
		if (x == null) {
			try {
				x = beanStore.createBean(c).run();
			} catch (ExecutableException e) {
				throw asRuntimeException(e);
			}
			partSerializers.put(c, x);
		}
		return x;
	}

	/**
	 * Returns the part parser instance of the specified type.
	 *
	 * @param c The part parser class.
	 * @return The part parser.
	 */
	protected HttpPartParser getPartParser(Class<? extends HttpPartParser> c) {
		HttpPartParser x = partParsers.get(c);
		if (x == null) {
			try {
				x = beanStore.createBean(c).run();
			} catch (ExecutableException e) {
				throw asRuntimeException(e);
			}
			partParsers.put(c, x);
		}
		return x;
	}

	/**
	 * Returns <jk>true</jk> if empty request header values should be ignored.
	 *
	 * @return <jk>true</jk> if empty request header values should be ignored.
	 */
	protected boolean isSkipEmptyHeaderData() {
		return skipEmptyHeaderData;
	}

	/**
	 * Returns <jk>true</jk> if empty request query parameter values should be ignored.
	 *
	 * @return <jk>true</jk> if empty request query parameter values should be ignored.
	 */
	protected boolean isSkipEmptyQueryData() {
		return skipEmptyQueryData;
	}

	/**
	 * Returns <jk>true</jk> if empty request form-data parameter values should be ignored.
	 *
	 * @return <jk>true</jk> if empty request form-data parameter values should be ignored.
	 */
	protected boolean isSkipEmptyFormData() {
		return skipEmptyFormData;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Part list builders methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a mutable copy of the header data defined on this client.
	 *
	 * <p>
	 * Used during the construction of {@link RestRequest} objects.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own builder.
	 *
	 * @return A new builder.
	 */
	public HeaderList createHeaderData() {
		return headerData.copy();
	}

	/**
	 * Creates a mutable copy of the query data defined on this client.
	 *
	 * <p>
	 * Used during the construction of {@link RestRequest} objects.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own builder.
	 *
	 * @return A new builder.
	 */
	public PartList createQueryData() {
		return queryData.copy();
	}

	/**
	 * Creates a mutable copy of the form data defined on this client.
	 *
	 * <p>
	 * Used during the construction of {@link RestRequest} objects.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own builder.
	 *
	 * @return A new builder.
	 */
	public PartList createFormData() {
		return formData.copy();
	}

	/**
	 * Creates a mutable copy of the path data defined on this client.
	 *
	 * <p>
	 * Used during the construction of {@link RestRequest} objects.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own builder.
	 *
	 * @return A new builder.
	 */
	public PartList createPathData() {
		return pathData.copy();
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
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link Builder#interceptors(Object...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	protected void onCallInit(RestRequest req) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onInit(req);
		} catch (RuntimeException | RestCallException e) {
			throw e;
		} catch (Exception e) {
			throw new RestCallException(null, e, "Interceptor threw an exception on init.");
		}
	}

	/**
	 * Interceptor method called immediately after an HTTP response has been received.
	 *
	 * <p>
	 * Subclasses can override this method to intercept the response and perform special modifications.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link Builder#interceptors(Object...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	protected void onCallConnect(RestRequest req, RestResponse res) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(req, res);
		} catch (RuntimeException | RestCallException e) {
			throw e;
		} catch (Exception e) {
			throw new RestCallException(res, e, "Interceptor threw an exception on connect.");
		}
	}

	/**
	 * Interceptor method called immediately after the RestRequest object is created and all headers/query/form-data has been set on the request from the client.
	 *
	 * <p>
	 * Subclasses can override this method to handle any cleanup operations.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link Builder#interceptors(Object...)}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws RestCallException If any of the interceptors threw an exception.
	 */
	protected void onCallClose(RestRequest req, RestResponse res) throws RestCallException {
		try {
			for (RestCallInterceptor rci : interceptors)
				rci.onClose(req, res);
		} catch (RuntimeException | RestCallException e) {
			throw e;
		} catch (Exception e) {
			throw new RestCallException(res, e, "Interceptor threw an exception on close.");
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
	 * Returns the connection manager if one was specified in the client builder.
	 *
	 * @return The connection manager.  May be <jk>null</jk>.
	 */
	public HttpClientConnectionManager getHttpClientConnectionManager() {
		return connectionManager;
	}

	/**
	 * Executes HTTP request using the default context.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
	 * 	<li class='note'>The {@link #run(HttpHost,HttpRequest,HttpContext)} method has been provided as a wrapper around this method.
	 * 		Subclasses can override these methods for handling requests with and without bodies separately.
	 * 	<li class='note'>The {@link RestCallHandler} interface can also be implemented to intercept this method.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method gets passed on directly to the underlying {@link HttpClient} class.
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
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	URI toURI(Object x, String rootUrl) throws RestCallException {
		try {
			if (x instanceof URI)
				return (URI)x;
			if (x instanceof URL)
				((URL)x).toURI();
			if (x instanceof URIBuilder)
				return ((URIBuilder)x).build();
			String s = x == null ? "" : x.toString();
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
			throw new RestCallException(null, e, "Invalid URI encountered:  {0}", x);  // Shouldn't happen.
		}
	}

	ExecutorService getExecutorService() {
		if (executorService != null)
			return executorService;
		synchronized(this) {
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
		return (l.size() == 1 ? l.get(0) : null);
	}

	boolean hasSerializers() {
		return ! serializers.getSerializers().isEmpty();
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
		return (l.size() == 1 ? l.get(0) : null);
	}

	boolean hasParsers() {
		return ! parsers.getParsers().isEmpty();
	}

	@SuppressWarnings("unchecked")
	<T extends Context> T getInstance(Class<T> c) {
		Context o = requestContexts.get(c);
		if (o == null) {
			if (Serializer.class.isAssignableFrom(c)) {
				o = Serializer.createSerializerBuilder((Class<? extends Serializer>)c).beanContext(getBeanContext()).build();
			} else if (Parser.class.isAssignableFrom(c)) {
				o = Parser.createParserBuilder((Class<? extends Parser>)c).beanContext(getBeanContext()).build();
			}
			requestContexts.put(c, o);
		}
		return (T)o;
	}

	private RestOperation op(String method, Object url, Object body) {
		return RestOperation.of(method, url, body);
	}

	private Reader stringBody(String body) {
		return body == null ? null : new StringReader(stringify(body));
	}

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("errorCodes", errorCodes)
			.append("executorService", executorService)
			.append("executorServiceShutdownOnClose", executorServiceShutdownOnClose)
			.append("headerData", headerData)
			.append("interceptors", interceptors)
			.append("keepHttpClientOpen", keepHttpClientOpen)
			.append("partParser", partParser)
			.append("partSerializer", partSerializer)
			.append("queryData", queryData)
			.append("rootUrl", rootUrl);
	}
}
