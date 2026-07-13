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

import java.lang.annotation.*;

/**
 * Identifies a proxy against a REST interface.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * </ul>
 */
@Documented
@Target({ TYPE })
@Retention(RUNTIME)
@Inherited
public @interface Remote {

	/**
	 * Default request header list.
	 *
	 * <p>
	 * Specifies a supplier of headers to set on all requests. The value is interpreted by the active REST client:
	 * the classic Apache-HttpClient transport accepts subclasses of
	 * <c>org.apache.juneau.http.classic.header.HeaderList</c>; other transports may ignore the value.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supplier class must provide a public no-arg constructor.
	 * 	<li class='note'>
	 * 		Default <c>Void.class</c> means no default header list is supplied.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?> headerList() default Void.class;

	/**
	 * Default request headers.
	 *
	 * <p>
	 * Specifies headers to set on all requests.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] headers() default {};

	/**
	 * Default request query parameters.
	 *
	 * <p>
	 * Specifies always-applied constant query parameters emitted on <i>every</i> request the proxy makes &mdash; no
	 * corresponding method parameter is required.  Each entry uses the <js>"name=value"</js> form (the first
	 * <js>'='</js> separates name from value), mirroring the URL-query convention and the {@link #headers()}
	 * <js>"Name: value"</js> precedent.
	 *
	 * <p>
	 * A caller-supplied parameter value for the same name still composes with the constant (the constant is not
	 * suppressed).  A method-level constant of the same name (see {@code @RemoteOp(queryData=...)} and the verb
	 * annotations) takes precedence over the interface-level value declared here.
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
	 * Default request form-data parameters.
	 *
	 * <p>
	 * Specifies always-applied constant form-data fields emitted on <i>every</i> request the proxy makes &mdash; no
	 * corresponding method parameter is required.  Each entry uses the <js>"name=value"</js> form (the first
	 * <js>'='</js> separates name from value), mirroring the form-encoded convention and the {@link #headers()}
	 * <js>"Name: value"</js> precedent.
	 *
	 * <p>
	 * A caller-supplied parameter value for the same name still composes with the constant (the constant is not
	 * suppressed).  A method-level constant of the same name (see {@code @RemoteOp(formData=...)} and the verb
	 * annotations) takes precedence over the interface-level value declared here.
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
	 * REST service path.
	 *
	 * <ul class='values'>
	 * 	<li>An absolute URL.
	 * 	<li>A relative URL interpreted as relative to the root URL defined on the <c>RestClient</c>
	 * 	<li>No path interpreted as the class name (e.g. <js>"http://localhost/root-url/org.foo.MyInterface"</js>)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String path() default "";

	/**
	 * Alias for {@link #path()}.
	 *
	 * <p>
	 * Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * (e.g. <js>"$P{mySystemProperty}"</js>).
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";

	/**
	 * Base/host override applied to every request the proxy makes.
	 *
	 * <p>
	 * Replaces only the authority+root portion of the request URL &mdash; in effect, an annotation-declared substitute
	 * for the client's root URL &mdash; while <i>preserving</i> the interface base path, the method path, and any
	 * <c>{var}</c> templating.  For example, with <c>baseUrl=<js>"http://other-host"</js></c>, an interface path of
	 * <js>"/api"</js> and a method path of <js>"/users/{id}"</js>, the effective URL is
	 * <js>"http://other-host/api/users/{id}"</js>.
	 *
	 * <p>
	 * Precedence (most-specific first): an {@link org.apache.juneau.http.Url @Url} parameter, then a method-level
	 * {@code baseUrl} (see {@code @RemoteOp(baseUrl=...)} and the verb annotations), then this interface-level value,
	 * then the client root URL.
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
	 * Interface-level default request body format (and {@code Content-Type} header) for all methods.
	 *
	 * <p>
	 * Names a media type (e.g. <js>"application/xml"</js>) used as the default for every method on this interface.  The
	 * next-generation engine selects the matching registered request serializer from the client's serializer set so the
	 * request body bytes are actually written in that format, and sets the {@code Content-Type} header to this media
	 * type &mdash; cleanly replacing the serializer's default content type (exactly one {@code Content-Type} header is
	 * sent).  If no registered serializer matches, the client's default serializer is used but this media type is still
	 * sent as the label (supporting vendor media types).
	 *
	 * <p>
	 * A method-level {@link RemoteOp#contentType()} (or verb-annotation equivalent) overrides this default.
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
	 * Interface-level default {@code Accept} header (and response parser fallback) for all methods.
	 *
	 * <p>
	 * Names a media type (e.g. <js>"application/xml"</js>) sent as the default {@code Accept} header for every method on
	 * this interface.  Response parsing remains driven by the <i>response</i> {@code Content-Type}; this media type is
	 * the fallback parser only when the response is unlabeled or its {@code Content-Type} matches no registered parser.
	 *
	 * <p>
	 * A method-level {@link RemoteOp#accept()} (or verb-annotation equivalent) overrides this default.
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
	 * Specifies the client version of this interface.
	 *
	 * <p>
	 * Used to populate the <js>"Client-Version"</js> header that identifies what version of client this is
	 * so that the server side can handle older versions accordingly.
	 *
	 * <p>
	 * The format of this is a string of the format <c>#[.#[.#[...]]</c> (e.g. <js>"1.2.3"</js>).
	 *
	 * <p>
	 * The server side then uses an OSGi-version matching pattern to identify which methods to call:
	 * <p class='bjava'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String version() default "";

	/**
	 * Specifies the client version header name.
	 *
	 * <p>
	 * The default value is <js>"Client-Version"</js>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a>
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String versionHeader() default "";

	/**
	 * Default lifecycle interceptors applied to every request the proxy makes.
	 *
	 * <p>
	 * Each class must implement the next-generation <c>RestCallInterceptor</c> SPI and provide a public no-arg
	 * constructor.  Interface-level interceptors run <i>after</i> the builder-configured interceptors and <i>before</i>
	 * any method-level ({@code @RemoteOp/@RemoteGet/...(interceptors=...)}) interceptors; the three sets are unioned in
	 * the order builder &rarr; interface &rarr; method (method-level runs closest to the call).
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
	 * Default per-call response/read timeout applied to every request the proxy makes.
	 *
	 * <p>
	 * Expressed as a duration string (e.g. <js>"30s"</js>, <js>"1500ms"</js>, <js>"1m"</js>) parsed by Juneau's
	 * duration parser.  A method-level {@code timeout} ({@code @RemoteOp/@RemoteGet/...}) overrides this default.
	 * Connect timeouts remain a client-level setting (not configurable per call).
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
	 * Default maximum number of automatic retry attempts applied to every request the proxy makes.
	 *
	 * <p>
	 * A value of {@code 0} (the default) disables retries.  A positive method-level {@code retries} overrides this
	 * default.  See {@code @RemoteOp(retries=...)} for the retry trigger statuses and the (hard) safety gating rules.
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
	 * Opts non-idempotent verbs (<c>POST</c>/<c>PATCH</c>) into automatic retries for every method on the proxy.
	 *
	 * <p>
	 * Has no effect unless {@link #retries()} (or a method-level {@code retries}) is positive.  Even when enabled, a
	 * request with a non-repeatable body is never retried.  The effective value is the logical OR of this default and
	 * the method-level value.
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
	 * Default for throwing a generic exception on an error response, applied to every method on the proxy.
	 *
	 * <p>
	 * When <jk>true</jk> and the response status is an error ({@code >=400}) and no typed exception from the method's
	 * {@code throws} clause matched, a generic exception is thrown.  The effective value is the logical OR of this
	 * default and the method-level value.  See {@code @RemoteOp(throwOnError=...)}.
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