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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Represents a connection to a remote REST resource.
 * <p>
 * Instances of this class are created by the various {@code doX()} methods on the {@link RestClient} class.
 * <p>
 * This class uses only Java standard APIs.  Requests can be built up using a fluent interface with method chaining, like so...
 *
 * <p class='bcode'>
 * 	RestClient client = <jk>new</jk> RestClient();
 * 	RestCall c = client.doPost(<jsf>URL</jsf>).setInput(o).setHeader(x,y);
 * 	MyBean b = c.getResponse(MyBean.<jk>class</jk>);
 * </p>
 * <p>
 * The actual connection and request/response transaction occurs when calling one of the <code>getResponseXXX()</code> methods.
 *
 * <h5 class='section'>Additional information:</h5>
 * <ul>
 * 	<li><a class="doclink" href="package-summary.html#RestClient">org.apache.juneau.rest.client &gt; REST client API</a> for more information and code examples.
 * </ul>
 */
@SuppressWarnings("hiding")
public final class RestCall {

	private final RestClient client;                       // The client that created this call.
	private final HttpRequestBase request;                 // The request.
	private HttpResponse response;                         // The response.
	private List<RestCallInterceptor> interceptors = new ArrayList<RestCallInterceptor>();               // Used for intercepting and altering requests.

	private boolean isConnected = false;                   // connect() has been called.
	private boolean allowRedirectsOnPosts;
	private int retries = 1;
	private int redirectOnPostsTries = 5;
	private long retryInterval = -1;
	private RetryOn retryOn;
	private boolean ignoreErrors;
	private boolean byLines = false;
	private TeeWriter writers = new TeeWriter();
	private StringWriter capturedResponseWriter;
	private String capturedResponse;
	private TeeOutputStream outputStreams = new TeeOutputStream();
	private boolean isClosed = false;
	private boolean isFailed = false;
	private Object input;
	private Serializer serializer;
	private Parser parser;

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param request The wrapped Apache HTTP client request object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestCall(RestClient client, HttpRequestBase request) throws RestCallException {
		this.client = client;
		this.request = request;
		for (RestCallInterceptor i : this.client.interceptors)
			interceptor(i);
		this.retryOn = client.retryOn;
		this.retries = client.retries;
		this.retryInterval = client.retryInterval;
		this.serializer = client.serializer;
		this.parser = client.parser;
	}

	/**
	 * Sets the input for this REST call.
	 *
	 * @param input The input to be sent to the REST resource (only valid for PUT and POST) requests. <br>
	 * Can be of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 	<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 	<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 	<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * </ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestCall input(final Object input) throws RestCallException {
		this.input = input;
		return this;
	}

	/**
	 * Specifies the serializer to use on this call.
	 * <p>
	 * Overrides the serializer specified on the {@link RestClient}.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object (for method chaining).
	 */
	public RestCall serializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Specifies the parser to use on this call.
	 * <p>
	 * Overrides the parser specified on the {@link RestClient}.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object (for method chaining).
	 */
	public RestCall parser(Parser parser) {
		this.parser = parser;
		return this;
	}


	//--------------------------------------------------------------------------------
	// HTTP headers
	//--------------------------------------------------------------------------------

	/**
	 * Convenience method for setting a header value on the request.
	 * <p>
	 * Equivalent to calling <code>restCall.getRequest().setHeader(name, value.toString())</code>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public RestCall header(String name, Object value) {
		request.setHeader(name, value.toString());
		return this;
	}

	/**
	 * Sets the value for the <code>Accept</code> request header.
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall accept(Object value) {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <code>Accept-Charset</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall acceptCharset(Object value) {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <code>Accept-Encoding</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall acceptEncoding(Object value) {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <code>Accept-Language</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall acceptLanguage(Object value) {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <code>Authorization</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall authorization(Object value) {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <code>Cache-Control</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <code>Connection</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall connection(Object value) {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <code>Content-Length</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall contentLength(Object value) {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <code>Content-Type</code> request header.
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall contentType(Object value) {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <code>Date</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall date(Object value) {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <code>Expect</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall expect(Object value) {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <code>Forwarded</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall forwarded(Object value) {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <code>From</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall from(Object value) {
		return header("From", value);
	}

	/**
	 * Sets the value for the <code>Host</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall host(Object value) {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <code>If-Match</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall ifMatch(Object value) {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Modified-Since</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall ifModifiedSince(Object value) {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <code>If-None-Match</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall ifNoneMatch(Object value) {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Range</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall ifRange(Object value) {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <code>If-Unmodified-Since</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall ifUnmodifiedSince(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Max-Forwards</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall maxForwards(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Origin</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall origin(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Pragma</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall pragma(Object value) {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <code>Proxy-Authorization</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall proxyAuthorization(Object value) {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <code>Range</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall range(Object value) {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <code>Referer</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall referer(Object value) {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <code>TE</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall te(Object value) {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <code>User-Agent</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall userAgent(Object value) {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <code>Upgrade</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall upgrade(Object value) {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <code>Via</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall via(Object value) {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <code>Warning</code> request header.
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestCall warning(Object value) {
		return header("Warning", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param version The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	public RestCall clientVersion(String version) {
		return header("X-Client-Version", version);
	}

	/**
	 * Make this call retryable if an error response (>=400) is received.
	 *
	 * @param retries The number of retries to attempt.
	 * @param interval The time in milliseconds between attempts.
	 * @param retryOn Optional object used for determining whether a retry should be attempted.
	 * If <jk>null</jk>, uses {@link RetryOn#DEFAULT}.
	 * @return This object (for method chaining).
	 * @throws RestCallException If current entity is not repeatable.
	 */
	public RestCall retryable(int retries, long interval, RetryOn retryOn) throws RestCallException {
		if (request instanceof HttpEntityEnclosingRequestBase) {
			if (input != null && input instanceof HttpEntity) {
				HttpEntity e = (HttpEntity)input;
				if (e != null && ! e.isRepeatable())
					throw new RestCallException("Attempt to make call retryable, but entity is not repeatable.");
				}
			}
		this.retries = retries;
		this.retryInterval = interval;
		this.retryOn = (retryOn == null ? RetryOn.DEFAULT : retryOn);
		return this;

	}

	/**
	 * For this call, allow automatic redirects when a 302 or 307 occurs when
	 * 	performing a POST.
	 * <p>
	 * Note that this can be inefficient since the POST body needs to be serialized
	 * 	twice.
	 * The preferred approach if possible is to use the {@link LaxRedirectStrategy} strategy
	 * 	on the underlying HTTP client.  However, this method is provided if you don't
	 * 	have access to the underlying client.
	 *
	 * @param b Redirect flag.
	 * @return This object (for method chaining).
	 */
	public RestCall allowRedirectsOnPosts(boolean b) {
		this.allowRedirectsOnPosts = b;
		return this;
	}

	/**
	 * Specify the number of redirects to follow before throwing an exception.
	 *
	 * @param maxAttempts Allow a redirect to occur this number of times.
	 * @return This object (for method chaining).
	 */
	public RestCall redirectMaxAttempts(int maxAttempts) {
		this.redirectOnPostsTries = maxAttempts;
		return this;
	}

	/**
	 * Add an interceptor for this call only.
	 *
	 * @param interceptor The interceptor to add to this call.
	 * @return This object (for method chaining).
	 */
	public RestCall interceptor(RestCallInterceptor interceptor) {
		interceptors.add(interceptor);
		interceptor.onInit(this);
		return this;
	}

	/**
	 * Pipes the request output to the specified writer when {@link #run()} is called.
	 * <p>
	 * The writer is not closed.
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param w The writer to pipe the output to.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(Writer w) {
		return pipeTo(w, false);
	}

	/**
	 * Pipe output from response to the specified writer when {@link #run()} is called.
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param w The writer to write the output to.
	 * @param close Close the writer when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(Writer w, boolean close) {
		return pipeTo(null, w, close);
	}

	/**
	 * Pipe output from response to the specified writer when {@link #run()} is called and associate
	 * that writer with an ID so it can be retrieved through {@link #getWriter(String)}.
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @param w The writer to write the output to.
	 * @param close Close the writer when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(String id, Writer w, boolean close) {
		writers.add(id, w, close);
		return this;
	}

	/**
	 * Retrieves a writer associated with an ID via {@link #pipeTo(String, Writer, boolean)}
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @return The writer, or <jk>null</jk> if no writer is associated with that ID.
	 */
	public Writer getWriter(String id) {
		return writers.getWriter(id);
	}

	/**
	 * When output is piped to writers, flush the writers after every line of output.
	 *
	 * @return This object (for method chaining).
	 */
	public RestCall byLines() {
		this.byLines = true;
		return this;
	}

	/**
	 * Pipes the request output to the specified output stream when {@link #run()} is called.
	 * <p>
	 * The output stream is not closed.
	 * <p>
	 * This method can be called multiple times to pipe to multiple output streams.
	 *
	 * @param os The output stream to pipe the output to.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(OutputStream os) {
		return pipeTo(os, false);
	}

	/**
	 * Pipe output from response to the specified output stream when {@link #run()} is called.
	 * <p>
	 * This method can be called multiple times to pipe to multiple output stream.
	 *
	 * @param os The output stream to write the output to.
	 * @param close Close the output stream when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(OutputStream os, boolean close) {
		return pipeTo(null, os, close);
	}

	/**
	 * Pipe output from response to the specified output stream when {@link #run()} is called and associate
	 * that output stream with an ID so it can be retrieved through {@link #getOutputStream(String)}.
	 * <p>
	 * This method can be called multiple times to pipe to multiple output stream.
	 *
	 * @param id A string identifier that can be used to retrieve the output stream using {@link #getOutputStream(String)}
	 * @param os The output stream to write the output to.
	 * @param close Close the output stream when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(String id, OutputStream os, boolean close) {
		outputStreams.add(id, os, close);
		return this;
	}

	/**
	 * Retrieves an output stream associated with an ID via {@link #pipeTo(String, OutputStream, boolean)}
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @return The writer, or <jk>null</jk> if no writer is associated with that ID.
	 */
	public OutputStream getOutputStream(String id) {
		return outputStreams.getOutputStream(id);
	}

	/**
	 * Prevent {@link RestCallException RestCallExceptions} from being thrown when HTTP status 400+ is encountered.
	 * @return This object (for method chaining).
	 */
	public RestCall ignoreErrors() {
		this.ignoreErrors = true;
		return this;
	}

	/**
	 * Stores the response text so that it can later be captured using {@link #getCapturedResponse()}.
	 * <p>
	 * This method should only be called once.  Multiple calls to this method are ignored.
	 *
	 * @return This object (for method chaining).
	 */
	public RestCall captureResponse() {
		if (capturedResponseWriter == null) {
			capturedResponseWriter = new StringWriter();
			writers.add(capturedResponseWriter, false);
		}
		return this;
	}


	/**
	 * Look for the specified regular expression pattern in the response output.
	 * <p>
	 * Causes a {@link RestCallException} to be thrown if the specified pattern is found in the output.
	 * <p>
	 * This method uses {@link #getCapturedResponse()} to read the response text and so does not affect the other output
	 * 	methods such as {@link #getResponseAsString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Throw a RestCallException if FAILURE or ERROR is found in the output.</jc>
	 * 	restClient.doGet(<jsf>URL</jsf>)
	 * 		.failurePattern(<js>"FAILURE|ERROR"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param errorPattern A regular expression to look for in the response output.
	 * @return This object (for method chaining).
	 */
	public RestCall failurePattern(final String errorPattern) {
		responsePattern(
			new ResponsePattern(errorPattern) {
				@Override
				public void onMatch(RestCall rc, Matcher m) throws RestCallException {
					throw new RestCallException("Failure pattern detected.");
				}
			}
		);
		return this;
	}

	/**
	 * Look for the specified regular expression pattern in the response output.
	 * <p>
	 * Causes a {@link RestCallException} to be thrown if the specified pattern is not found in the output.
	 * <p>
	 * This method uses {@link #getCapturedResponse()} to read the response text and so does not affect the other output
	 * 	methods such as {@link #getResponseAsString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Throw a RestCallException if SUCCESS is not found in the output.</jc>
	 * 	restClient.doGet(<jsf>URL</jsf>)
	 * 		.successPattern(<js>"SUCCESS"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param successPattern A regular expression to look for in the response output.
	 * @return This object (for method chaining).
	 */
	public RestCall successPattern(String successPattern) {
		responsePattern(
			new ResponsePattern(successPattern) {
				@Override
				public void onNoMatch(RestCall rc) throws RestCallException {
					throw new RestCallException("Success pattern not detected.");
				}
			}
		);
		return this;
	}

	/**
	 * Adds a response pattern finder to look for regular expression matches in the response output.
	 * <p>
	 * This method can be called multiple times to add multiple response pattern finders.
	 * <p>
	 * {@link ResponsePattern ResponsePatterns} use the {@link #getCapturedResponse()} to read the response text and so does not affect the other output
	 * 	methods such as {@link #getResponseAsString()}.
	 *
	 * @param responsePattern The response pattern finder.
	 * @return This object (for method chaining).
	 */
	public RestCall responsePattern(final ResponsePattern responsePattern) {
		captureResponse();
		interceptor(
			new RestCallInterceptor() {
				@Override
				public void onClose(RestCall restCall) throws RestCallException {
					responsePattern.match(RestCall.this);
				}
			}
		);
		return this;
	}

	/**
	 * Set configuration settings on this request.
	 * <p>
	 * Use {@link RequestConfig#custom()} to create configuration parameters for the request.
	 *
	 * @param config The new configuration settings for this request.
	 * @return This object (for method chaining).
	 */
	public RestCall setConfig(RequestConfig config) {
		this.request.setConfig(config);
		return this;
	}

	/**
	 * @return The HTTP response code.
	 * @throws RestCallException
	 * @deprecated Use {@link #run()}.
	 */
	@Deprecated
	public int execute() throws RestCallException {
		return run();
	}

	/**
	 * Method used to execute an HTTP response where you're only interested in the HTTP response code.
	 * <p>
	 * The response entity is discarded unless one of the pipe methods have been specified to pipe the
	 * 	 output to an output stream or writer.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>try</jk> {
	 * 		RestClient client = <jk>new</jk> RestClient();
	 * 		<jk>int</jk> rc = client.doGet(url).execute();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public int run() throws RestCallException {
		connect();
		try {
			StatusLine status = response.getStatusLine();
			int sc = status.getStatusCode();
			if (sc >= 400 && ! ignoreErrors)
				throw new RestCallException(sc, status.getReasonPhrase(), request.getMethod(), request.getURI(), getResponseAsString()).setHttpResponse(response);
			if (outputStreams.size() > 0 || writers.size() > 0)
				getReader();
			return sc;
		} catch (RestCallException e) {
			isFailed = true;
			throw e;
		} catch (IOException e) {
			isFailed = true;
			throw new RestCallException(e).setHttpResponse(response);
		} finally {
			close();
		}
	}

	/**
	 * Connects to the REST resource.
	 * <p>
	 * If this is a <code>PUT</code> or <code>POST</code>, also sends the input to the remote resource.<br>
	 * <p>
	 * Typically, you would only call this method if you're not interested in retrieving the body of the HTTP response.
	 * Otherwise, you're better off just calling one of the {@link #getReader()}/{@link #getResponse(Class)}/{@link #pipeTo(Writer)}
	 * 	methods directly which automatically call this method already.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException If an exception or <code>400+</code> HTTP status code occurred during the connection attempt.
	 */
	public RestCall connect() throws RestCallException {

		if (isConnected)
			return this;
		isConnected = true;

		try {

			if (input != null) {
				if (! (request instanceof HttpEntityEnclosingRequestBase))
					throw new RestCallException(0, "Method does not support content entity.", request.getMethod(), request.getURI(), null);
				HttpEntity entity = (input instanceof HttpEntity) ? (HttpEntity)input : new RestRequestEntity(input, getSerializer());
				((HttpEntityEnclosingRequestBase)request).setEntity(entity);
				if (retries > 1 && ! entity.isRepeatable())
					throw new RestCallException("Rest call set to retryable, but entity is not repeatable.");
			}

			int sc = 0;
			while (retries > 0) {
				retries--;
				Exception ex = null;
				try {
					response = client.execute(request);
					sc = (response == null || response.getStatusLine() == null) ? -1 : response.getStatusLine().getStatusCode();
				} catch (Exception e) {
					ex = e;
					sc = -1;
					if (response != null)
						EntityUtils.consumeQuietly(response.getEntity());
				}
				if (! retryOn.onCode(sc))
					retries = 0;
				if (retries > 0) {
					for (RestCallInterceptor rci : interceptors)
						rci.onRetry(this, sc, request, response, ex);
					request.reset();
					long w = retryInterval;
					synchronized(this) {
						wait(w);
					}
				} else if (ex != null) {
					throw ex;
				}
			}
			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(this, sc, request, response);
			if (response == null)
				throw new RestCallException("HttpClient returned a null response");
			StatusLine sl = response.getStatusLine();
			String method = request.getMethod();
			sc = sl.getStatusCode(); // Read it again in case it was changed by one of the interceptors.
			if (sc >= 400 && ! ignoreErrors)
				throw new RestCallException(sc, sl.getReasonPhrase(), method, request.getURI(), getResponseAsString()).setHttpResponse(response);
			if ((sc == 307 || sc == 302) && allowRedirectsOnPosts && method.equalsIgnoreCase("POST")) {
				if (redirectOnPostsTries-- < 1)
					throw new RestCallException(sc, "Maximum number of redirects occurred.  Location header: " + response.getFirstHeader("Location"), method, request.getURI(), getResponseAsString());
				Header h = response.getFirstHeader("Location");
				if (h != null) {
					reset();
					request.setURI(URI.create(h.getValue()));
					retries++;  // Redirects should affect retries.
					connect();
				}
			}

		} catch (RestCallException e) {
			isFailed = true;
			try {
			close();
			} catch (RestCallException e2) { /* Ignore */ }
			throw e;
		} catch (Exception e) {
			isFailed = true;
			close();
			throw new RestCallException(e).setHttpResponse(response);
		}

		return this;
	}

	private void reset() {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
		request.reset();
		isConnected = false;
		isClosed = false;
		isFailed = false;
		if (capturedResponseWriter != null)
			capturedResponseWriter.getBuffer().setLength(0);
	}

	/**
	 * Connects to the remote resource (if <code>connect()</code> hasn't already been called) and returns the HTTP response message body as a reader.
	 * <p>
	 * If an {@link Encoder} has been registered with the {@link RestClient}, then the underlying input stream
	 * 	will be wrapped in the encoded stream (e.g. a <code>GZIPInputStream</code>).
	 * <p>
	 * If present, automatically handles the <code>charset</code> value in the <code>Content-Type</code> response header.
	 * <p>
	 * <b>IMPORTANT:</b>  It is your responsibility to close this reader once you have finished with it.
	 *
	 * @return The HTTP response message body reader.  <jk>null</jk> if response was successful but didn't contain a body (e.g. HTTP 204).
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public Reader getReader() throws IOException {
		InputStream is = getInputStream();
		if (is == null)
			return null;

		// Figure out what the charset of the response is.
		String cs = null;
		Header contentType = response.getLastHeader("Content-Type");
		String ct = contentType == null ? null : contentType.getValue();

		// First look for "charset=" in Content-Type header of response.
		if (ct != null && ct.contains("charset="))
			cs = ct.substring(ct.indexOf("charset=")+8).trim();

		if (cs == null)
			cs = "UTF-8";

		Reader isr = new InputStreamReader(is, cs);

		if (writers.size() > 0) {
			StringWriter sw = new StringWriter();
			writers.add(sw, true);
			IOPipe.create(isr, writers).byLines(byLines).run();
			return new StringReader(sw.toString());
		}

		return new InputStreamReader(is, cs);
	}

	/**
	 * Returns the response text as a string if {@link #captureResponse()} was called on this object.
	 * <p>
	 * Note that while similar to {@link #getResponseAsString()}, this method can be called multiple times
	 * 	to retrieve the response text multiple times.
	 * <p>
	 * Note that this method returns <jk>null</jk> if you have not called one of the methods that cause
	 * 	the response to be processed.  (e.g. {@link #run()}, {@link #getResponse()}, {@link #getResponseAsString()}.
	 *
	 * @return The captured response, or <jk>null</jk> if {@link #captureResponse()} has not been called.
	 * @throws IllegalStateException If trying to call this method before the response is consumed.
	 */
	public String getCapturedResponse() {
		if (! isClosed)
			throw new IllegalStateException("This method cannot be called until the response has been consumed.");
		if (capturedResponse == null && capturedResponseWriter != null && capturedResponseWriter.getBuffer().length() > 0)
			capturedResponse = capturedResponseWriter.toString();
		return capturedResponse;
	}

	/**
	 * Returns the parser specified on the client to use for parsing HTTP response bodies.
	 *
	 * @return The parser.
	 * @throws RestCallException If no parser was defined on the client.
	 */
	protected Parser getParser() throws RestCallException {
		if (parser == null)
			throw new RestCallException(0, "No parser defined on client", request.getMethod(), request.getURI(), null);
		return parser;
	}

	/**
	 * Returns the serializer specified on the client to use for serializing HTTP request bodies.
	 *
	 * @return The serializer.
	 * @throws RestCallException If no serializer was defined on the client.
	 */
	protected Serializer getSerializer() throws RestCallException {
		if (serializer == null)
			throw new RestCallException(0, "No serializer defined on client", request.getMethod(), request.getURI(), null);
		return serializer;
	}

	/**
	 * Returns the value of the <code>Content-Length</code> header.
	 *
	 * @return The value of the <code>Content-Length</code> header, or <code>-1</code> if header is not present.
	 * @throws IOException
	 */
	public int getContentLength() throws IOException {
		connect();
		Header h = response.getLastHeader("Content-Length");
		if (h == null)
			return -1;
		long l = Long.parseLong(h.getValue());
		if (l > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return (int)l;
	}

	/**
	 * Connects to the remote resource (if <code>connect()</code> hasn't already been called) and returns the HTTP response message body as an input stream.
	 * <p>
	 * If an {@link Encoder} has been registered with the {@link RestClient}, then the underlying input stream
	 * 	will be wrapped in the encoded stream (e.g. a <code>GZIPInputStream</code>).
	 * <p>
	 * <b>IMPORTANT:</b>  It is your responsibility to close this reader once you have finished with it.
	 *
	 * @return The HTTP response message body input stream. <jk>null</jk> if response was successful but didn't contain a body (e.g. HTTP 204).
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 * @throws IllegalStateException If an attempt is made to read the response more than once.
	 */
	public InputStream getInputStream() throws IOException {
		if (isClosed)
			throw new IllegalStateException("Method cannot be called.  Response has already been consumed.");
		connect();
		if (response == null)
			throw new RestCallException("Response was null");
		if (response.getEntity() == null)  // HTTP 204 results in no content.
			return null;
		InputStream is = response.getEntity().getContent();

		if (outputStreams.size() > 0) {
			ByteArrayInOutStream baios = new ByteArrayInOutStream();
			outputStreams.add(baios, true);
			IOPipe.create(is, baios).run();
			return baios.getInputStream();
		}
		return is;
	}

	/**
	 * Connects to the remote resource (if {@code connect()} hasn't already been called) and returns the HTTP response message body as plain text.
	 *
	 * @return The response as a string.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public String getResponseAsString() throws IOException {
		try {
			Reader r = getReader();
			String s = IOUtils.read(r).toString();
			return s;
		} catch (IOException e) {
			isFailed = true;
			throw e;
		} finally {
			close();
		}
	}

	/**
	 * Same as {@link #getResponse(Type, Type...)} except optimized for a non-parameterized class.
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = restClient.doGet(url).getResponse(String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = restClient.doGet(url).getResponse(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] ba = restClient.doGet(url).getResponse(MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * See {@link #getResponse(Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a connection error occurred.
	 */
	public <T> T getResponse(Class<T> type) throws IOException, ParseException {
		BeanContext bc = getParser().getBeanContext();
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return getResponse(bc.getClassMeta(type));
	}

	/**
	 * Parses HTTP body into the specified object type.
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Use the {@link #getResponse(Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param type The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a connection error occurred.
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getResponse(Type type, Type...args) throws IOException, ParseException {
		BeanContext bc = getParser().getBeanContext();
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return (T)getResponse(bc.getClassMeta(type, args));
	}

	/**
	 * Parses the output from the connection into the specified type and then wraps that in a {@link PojoRest}.
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @param innerType The class type of the POJO being wrapped.
	 * @return The parsed output wapped in a {@link PojoRest}.
	 * @throws IOException If a connection error occurred.
	 * @throws ParseException If the input contains a syntax error or is malformed for the <code>Content-Type</code> header.
	 */
	public PojoRest getResponsePojoRest(Class<?> innerType) throws IOException, ParseException {
		return new PojoRest(getResponse(innerType));
	}

	/**
	 * Converts the output from the connection into an {@link ObjectMap} and then wraps that in a {@link PojoRest}.
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @return The parsed output wapped in a {@link PojoRest}.
	 * @throws IOException If a connection error occurred.
	 * @throws ParseException If the input contains a syntax error or is malformed for the <code>Content-Type</code> header.
	 */
	public PojoRest getResponsePojoRest() throws IOException, ParseException {
		return getResponsePojoRest(ObjectMap.class);
	}

	<T> T getResponse(ClassMeta<T> type) throws IOException, ParseException {
		try {
		Parser p = getParser();
		T o = null;
			if (! p.isReaderParser()) {
			InputStream is = getInputStream();
			o = ((InputStreamParser)p).parse(is, type);
		} else {
			Reader r = getReader();
			o = ((ReaderParser)p).parse(r, type);
			}
		return o;
		} catch (ParseException e) {
			isFailed = true;
			throw e;
		} catch (IOException e) {
			isFailed = true;
			throw e;
		} finally {
			close();
		}
	}

	BeanContext getBeanContext() throws RestCallException {
		BeanContext bc = getParser().getBeanContext();
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return bc;
	}

	/**
	 * Returns access to the {@link HttpUriRequest} passed to {@link HttpClient#execute(HttpUriRequest)}.
	 *
	 * @return The {@link HttpUriRequest} object.
	 */
	public HttpUriRequest getRequest() {
		return request;
	}

	/**
	 * Returns access to the {@link HttpResponse} returned by {@link HttpClient#execute(HttpUriRequest)}.
	 * Returns <jk>null</jk> if {@link #connect()} has not yet been called.
	 *
	 * @return The HTTP response object.
	 * @throws IOException
	 */
	public HttpResponse getResponse() throws IOException {
		connect();
		return response;
	}

	/**
	 * Shortcut for calling <code>getRequest().setHeader(header)</code>
	 *
	 * @param header The header to set on the request.
	 * @return This object (for method chaining).
	 */
	public RestCall header(Header header) {
		request.setHeader(header);
		return this;
	}

	/** Use close() */
	@Deprecated
	public void consumeResponse() {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
	}

	/**
	 * Cleans up this HTTP call.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException Can be thrown by one of the {@link RestCallInterceptor#onClose(RestCall)} calls.
	 */
	public RestCall close() throws RestCallException {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
		isClosed = true;
		if (! isFailed)
			for (RestCallInterceptor r : interceptors)
				r.onClose(this);
		return this;
	}

	/**
	 * Adds a {@link RestCallLogger} to the list of interceptors on this class.
	 *
	 * @param level The log level to log events at.
	 * @param log The logger.
	 * @return This object (for method chaining).
	 */
	public RestCall logTo(Level level, Logger log) {
		interceptor(new RestCallLogger(level, log));
		return this;
	}
}
