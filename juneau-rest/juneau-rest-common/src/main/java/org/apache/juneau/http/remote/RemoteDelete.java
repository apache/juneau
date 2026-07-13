/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;

/**
 * Annotation applied to Java methods on REST proxy interface classes.
 *
 * <p>
 * Note that this annotation is optional if you do not need to override any of the values.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * </ul>
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
@AnnotationGroup(RemoteOp.class)
public @interface RemoteDelete {

	/**
	 * REST service path.
	 *
	 * <p>
	 * If you do not specify a path, then the path is inferred from the Java method name.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// POST /pet</jc>
	 * 	<ja>@RemoteDelete</ja>
	 * 	<jk>public void</jk> postPet(...) {...}
	 * </p>
	 *
	 * <p>
	 * Note that you can also use {@link #value()} to specify the path in shortened form.
	 *
	 * <ul class='values'>
	 * 	<li>An absolute URL.
	 * 	<li>A relative URL interpreted as relative to the root URL defined on the <c>RestClient</c> and/or {@link Remote#path()}.
	 * 	<li>No path.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String path() default "";

	/**
	 * The value the remote method returns.
	 *
	 * <ul class='values'>
	 * 	<li>
	 * 		{@link RemoteReturn#BODY} (default) - The body of the HTTP response converted to a POJO.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul class='spaced-list'>
	 * 			<li>
	 * 				<jk>void</jk> - Don't parse any response.  Note that the method will still throw an exception if an
	 * 				error HTTP status is returned.
	 * 			<li>
	 * 				Any parsable POJO - The body of the response will be converted to the POJO using the parser defined
	 * 				on the <c>RestClient</c>.
	 * 			<li>
	 * 				Any POJO annotated with the {@link Response @Response} annotation.
	 * 				This allows for response beans to be used which also allows for OpenAPI-based parsing and validation.
	 * 			<li>
	 * 				<c>HttpResponse</c> - Returns the raw <c>HttpResponse</c> returned by the inner
	 * 				<c>HttpClient</c>.
	 * 			<li>
	 * 				{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>
	 * 				{@link InputStream} - Returns access to the raw input stream of the response.
	 * 		</ul>
	 * 	<li>
	 * 		{@link RemoteReturn#STATUS} - The HTTP status code on the response.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul>
	 * 			<li><jk>int</jk>/<c>Integer</c> - The HTTP response code.
	 * 			<li><jk>boolean</jk>/<c>Boolean</c> - <jk>true</jk> if the response code is <c>&lt;400</c>
	 * 		</ul>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	RemoteReturn returns() default RemoteReturn.BODY;

	/**
	 * REST path.
	 *
	 * <p>
	 * Can be used to provide a shortened form for the {@link #path()} value.
	 *
	 * <p>
	 * The following examples are considered equivalent.
	 * <p class='bjava'>
	 * 	<jc>// Normal form</jc>
	 * 	<ja>@RemoteDelete</ja>(path=<js>"/{propertyName}"</js>)
	 *
	 * 	<jc>// Shortened form</jc>
	 * 	<ja>@RemoteDelete</ja>(<js>"/{propertyName}"</js>)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";

	/**
	 * Base/host override applied to this method's request.
	 *
	 * <p>
	 * Replaces only the authority+root portion of the request URL while <i>preserving</i> the interface base path, this
	 * method's path, and any <c>{var}</c> templating.  For example, with <c>baseUrl=<js>"http://other-host"</js></c>, an
	 * interface path of <js>"/api"</js> and a method path of <js>"/users/{id}"</js>, the effective URL is
	 * <js>"http://other-host/api/users/{id}"</js>.
	 *
	 * <p>
	 * Precedence (most-specific first): an {@link Url @Url} parameter, then this method-level
	 * value, then the interface-level default ({@link Remote#baseUrl()}), then the client root URL.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * 	<li class='note'>
	 * 		Only <c>http</c>/<c>https</c> schemes are permitted when the override yields an absolute URL; other schemes
	 * 		are rejected (SSRF guardrail).
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String baseUrl() default "";

	/**
	 * Selects the request body format (and the {@code Content-Type} header) for this method's request.
	 *
	 * <p>
	 * Names a media type (e.g. <js>"application/xml"</js>).  The next-generation engine selects the matching registered
	 * request serializer from the client's serializer set so the request body bytes are actually written in that
	 * format, and sets the {@code Content-Type} header to this media type &mdash; cleanly replacing the serializer's
	 * default content type (exactly one {@code Content-Type} header is sent).
	 *
	 * <p>
	 * If no registered serializer matches this media type, the client's default serializer is used to write the body
	 * but this media type is still sent as the {@code Content-Type} label (supporting vendor media types such as
	 * <js>"application/vnd.example.v2+json"</js> whose bytes are really the default format).
	 *
	 * <p>
	 * Precedence (most-specific first): this method-level value, then the interface-level default
	 * ({@link Remote#contentType()}).  This dedicated attribute takes precedence over a constant {@code Content-Type}
	 * declared via {@link #headers()} (since it also drives serializer selection); a genuinely caller-supplied
	 * {@code @Header("Content-Type")} parameter still takes effect.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String contentType() default "";

	/**
	 * Sets the {@code Accept} header (and the response parser fallback) for this method's request.
	 *
	 * <p>
	 * Names a media type (e.g. <js>"application/xml"</js>) sent as the request's {@code Accept} header.  Response
	 * parsing remains driven by the <i>response</i> {@code Content-Type} (the server is authoritative about what it
	 * actually sent); this media type is used as the fallback parser only when the response is unlabeled or its
	 * {@code Content-Type} matches no registered parser.
	 *
	 * <p>
	 * If no registered parser matches this media type, the client's default parser is used as the fallback.
	 *
	 * <p>
	 * Precedence (most-specific first): this method-level value, then the interface-level default
	 * ({@link Remote#accept()}).  This dedicated attribute takes precedence over a constant {@code Accept} declared via
	 * {@link #headers()}; a genuinely caller-supplied {@code @Header("Accept")} parameter still takes effect.  Exactly
	 * one {@code Accept} header is sent.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String accept() default "";

	/**
	 * Constant request headers applied to every invocation of this method.
	 *
	 * <p>
	 * Each entry uses the <js>"Name: value"</js> form (the first <js>':'</js> separates name from value), mirroring
	 * {@link Remote#headers()}.  The header is emitted unconditionally on every call &mdash; no corresponding method
	 * parameter is required.  A caller-supplied parameter value for the same name still composes with the constant
	 * (it is not suppressed), and this method-level value takes precedence over an interface-level
	 * {@link Remote#headers()} constant of the same name.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] headers() default {};

	/**
	 * Constant request query parameters applied to every invocation of this method.
	 *
	 * <p>
	 * Each entry uses the <js>"name=value"</js> form (the first <js>'='</js> separates name from value).  The query
	 * parameter is emitted unconditionally on every call &mdash; no corresponding method parameter is required.  A
	 * caller-supplied parameter value for the same name still composes with the constant (it is not suppressed), and
	 * this method-level value takes precedence over an interface-level {@link Remote#queryData()} constant of the same
	 * name.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] queryData() default {};

	/**
	 * Constant request form-data parameters applied to every invocation of this method.
	 *
	 * <p>
	 * Each entry uses the <js>"name=value"</js> form (the first <js>'='</js> separates name from value).  The form-data
	 * field is emitted unconditionally on every call &mdash; no corresponding method parameter is required.  A
	 * caller-supplied parameter value for the same name still composes with the constant (it is not suppressed), and
	 * this method-level value takes precedence over an interface-level {@link Remote#formData()} constant of the same
	 * name.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] formData() default {};

	/**
	 * Lifecycle interceptors applied to every invocation of this method.
	 *
	 * <p>
	 * Each class must implement the next-generation <c>RestCallInterceptor</c> SPI and provide a public no-arg
	 * constructor.  Method-level interceptors run <i>after</i> (closest to the call) any interface-level
	 * ({@link Remote#interceptors()}) and builder-configured interceptors; the three sets are unioned in the order
	 * builder &rarr; interface &rarr; method.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The member type is {@code Class<?>[]} (not {@code Class<? extends RestCallInterceptor>[]}) because
	 * 		<c>RestCallInterceptor</c> lives in the {@code juneau-rest-client} module, which this annotation's module
	 * 		must not depend on; each element is resolved to a <c>RestCallInterceptor</c> at proxy-build time.
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] interceptors() default {};

	/**
	 * Per-call response/read timeout applied to this method's request.
	 *
	 * <p>
	 * Expressed as a duration string (e.g. <js>"30s"</js>, <js>"1500ms"</js>, <js>"1m"</js>) parsed by Juneau's
	 * duration parser, and wired to the underlying transport's response timeout.  Scalar precedence: this method-level
	 * value overrides the interface-level default ({@link Remote#timeout()}).  Connect timeouts remain a client-level
	 * setting.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String timeout() default "";

	/**
	 * Maximum number of automatic retry attempts for this method's request.
	 *
	 * <p>
	 * A value of {@code 0} (the default) disables retries.  Retries are attempted only on connection failures and on
	 * retryable HTTP statuses ({@code 429} and {@code 5xx}), with a short exponential backoff between attempts.
	 *
	 * <p>
	 * <b>Safety gating (hard rules):</b>
	 * <ul>
	 * 	<li>Auto-retry applies only to idempotent verbs (<c>GET</c>/<c>PUT</c>/<c>DELETE</c>/<c>HEAD</c>); non-idempotent
	 * 		verbs (<c>POST</c>/<c>PATCH</c>) are retried only when {@link #retryNonIdempotent()} is <jk>true</jk>.
	 * 	<li>A request whose body is not repeatable (e.g. a streaming {@link InputStream}/{@link Reader}
	 * 		body) is never retried.
	 * 	<li>Retries are disabled for streaming return modes ({@link RemoteReturn#RESPONSE}, raw
	 * 		{@link InputStream}/{@link Reader} returns, and streaming cursors) and for
	 * 		{@link java.util.concurrent.Future}/{@link java.util.concurrent.CompletableFuture} returns.
	 * </ul>
	 *
	 * <p>
	 * Scalar precedence: a positive method-level value overrides the interface-level default ({@link Remote#retries()}).
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	int retries() default 0;

	/**
	 * Opts a non-idempotent verb (<c>POST</c>/<c>PATCH</c>) into automatic retries.
	 *
	 * <p>
	 * Has no effect unless {@link #retries()} (or {@link Remote#retries()}) is positive.  Even when enabled, a request
	 * with a non-repeatable body is never retried.  The effective value is the logical OR of this method-level value
	 * and the interface-level default ({@link Remote#retryNonIdempotent()}).
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean retryNonIdempotent() default false;

	/**
	 * Throws a generic exception when the response status is an error ({@code >=400}) and no typed exception from the
	 * method's {@code throws} clause matched.
	 *
	 * <p>
	 * Composes with the typed-exception mapping: a declared exception type whose status code matches the response is
	 * still thrown in preference to the generic exception.  Respects the same return-mode gating as the typed mapping.
	 * The effective value is the logical OR of this method-level value and the interface-level default
	 * ({@link Remote#throwOnError()}).
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
	 * 		ignores this attribute.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean throwOnError() default false;
}