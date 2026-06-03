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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.openapi.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.*;

/**
 * Used to denote that a class is a REST resource and to associate metadata on it.
 *
 * <p>
 * Usually used on a subclass of {@link RestServlet}, but can be used to annotate any class that you want to expose as
 * a REST resource.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>

 * </ul>
 */
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
@AnnotationGroup(Rest.class)
public @interface Rest {

	/**
	 * Allowed header URL parameters.
	 *
	 * <p>
	 * When specified, allows headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:
	 * <p class='burlenc'>
	 *  ?Accept=text/json&amp;Content-Type=text/json
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li class='note'>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String allowedHeaderParams() default "";

	/**
	 * Allowed method headers.
	 *
	 * <p>
	 * A comma-delimited list of HTTP method names that are allowed to be passed as values in an <c>X-Method</c> HTTP header
	 * to override the real HTTP method name.
	 * <p>
	 * Allows you to override the actual HTTP method with a simulated method.
	 * <br>For example, if an HTTP Client API doesn't support <c>PATCH</c> but does support <c>POST</c> (because
	 * <c>PATCH</c> is not part of the original HTTP spec), you can add a <c>X-Method: PATCH</c> header on a normal
	 * <c>HTTP POST /foo</c> request call which will make the HTTP call look like a <c>PATCH</c> request in any of the REST APIs.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Method names are case-insensitive.
	 * 	<li class='note'>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li class='note'>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String allowedMethodHeaders() default "";

	/**
	 * Allowed method parameters.
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:
	 * <p class='burlenc'>
	 *  ?method=OPTIONS
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li class='note'>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String allowedMethodParams() default "";

	/**
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The default call logger if not specified is {@link CallLogger}.
	 * 	<li class='note'>
	 * 		The resource class itself will be used if it implements the {@link CallLogger} interface and not
	 * 		explicitly overridden via this annotation.
	 * 	<li class='note'>
	 * 		The implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends CallLogger> callLogger() default CallLogger.Void.class;

	/**
	 * REST children.
	 *
	 * <p>
	 * Defines children of this resource.  Each child resource is mounted under its own URL subtree (per the
	 * child's own {@code @Rest(path=...)} / {@code @Rest(paths=...)}) and constructs its own {@link RestContext}.
	 *
	 * <h5 class='section'>Children vs. mixins (resolution semantics)</h5>
	 * <p>
	 * Children are <b>isolated from the parent's resolution chain</b> by design — a child's serializers, parsers,
	 * guards, hooks, call-logger, etc. are all resolved against the child's own {@link RestContext} only, NOT walked
	 * through the parent.  This is the opposite of how {@link #mixins() mixins} resolve.  See the
	 * <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerMixinSubContexts#mixin-vs-child-divergence">Mixin Sub-Contexts &mdash; Mixin-vs-child divergence</a>
	 * topic for the rationale (children own their lifecycle and are externally mounted; mixins are inline composers
	 * sharing the host's URL namespace and resolution chain).
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Children on child are combined with those on parent class.
	 * 	<li>Children are list parent-to-child in the order they appear in the annotation.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ChildResources">Child Resources</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerMixinSubContexts">Mixin Sub-Contexts</a> (for the mixin-vs-child divergence)
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] children() default {};

	/**
	 * REST mixins.
	 *
	 * <p>
	 * Defines operation-provider classes whose {@link RestOp @RestOp}-group methods should be composed into this
	 * resource.
	 *
	 * <p>
	 * Mixin methods are discovered the same way as local operation methods and surface under this resource's URL
	 * namespace.  On path/method collisions, local methods on this resource win over mixin methods.
	 *
	 * <h5 class='section'>Per-mixin RestContext + host-to-mixin inheritance (since 9.5.0)</h5>
	 * <p>
	 * Each mixin class is elevated to its own {@link RestContext} parent-linked to this host's {@link RestContext}.
	 * The mixin's class-level {@code @Rest(...)} configuration applies to its own endpoints, with inheritance from
	 * the host:
	 * <ul>
	 * 	<li><b>List-shaped properties</b> ({@code serializers}, {@code parsers}, {@code encoders}, {@code converters},
	 * 		{@code responseProcessors}, {@code restOpArgs}, {@code guards}) — host's chain runs first, then the mixin's
	 * 		appended.  Host endpoints see only the host's chain.
	 * 	<li><b>Replace-shaped properties</b> ({@code callLogger}, {@code debugEnablement}, {@code debugDefault},
	 * 		{@code partSerializer}, {@code partParser}) — the mixin's value wins over the host's for mixin endpoints
	 * 		when declared; otherwise the host's value is inherited.
	 * 	<li><b>{@code messages}</b> — the mixin's bundle is chained as a child of the host's via
	 * 		{@link org.apache.juneau.cp.Messages#chain Messages.chain(child, parent)} so mixin keys win and missing
	 * 		keys fall through to the host.
	 * 	<li><b>Lifecycle hooks</b> ({@code @RestStartCall}, {@code @RestEndCall}, {@code @RestPreCall},
	 * 		{@code @RestPostCall}, {@code @RestDestroy}) — dual-fire host-then-mixin for mixin-endpoint requests;
	 * 		host-only for host-endpoint requests.
	 * </ul>
	 *
	 * <p>
	 * Use {@link #noInherit() @Rest(noInherit=&#123;...&#125;)} on a mixin class to cut off inheritance for a specific
	 * property — same token set as the host's {@code noInherit} machinery (e.g. {@code "serializers"},
	 * {@code "guards"}, {@code "messages"}).
	 *
	 * <h5 class='section'>Mixins vs. children</h5>
	 * <p>
	 * Mixins are inline composers that share the host's URL namespace and inherit from the host's resolution chain.
	 * {@link #children() Children} are independently-mounted resources isolated from the host's resolution chain.
	 * See the
	 * <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerMixinSubContexts#mixin-vs-child-divergence">Mixin Sub-Contexts &mdash; Mixin-vs-child divergence</a>
	 * topic for the rationale.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Mixins on child are combined with those on parent class.
	 * 	<li>Mixins are listed parent-to-child in the order they appear in the annotation.
	 * 	<li>Transitive mixins ({@code A} mixes in {@code B}) parent-link flat to the host — both {@code A} and
	 * 		{@code B} get {@code parentContext = host}, never an {@code A → B} chain.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server &mdash; Mixins and Multi-Mount Paths</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerMixinSubContexts">REST Server &mdash; Mixin Sub-Contexts</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] mixins() default {};

	/**
	 * Client version header.
	 *
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String clientVersionHeader() default "";

	/**
	 * Optional location of configuration file for this servlet.
	 *
	 * <p>
	 * The configuration file .
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Config file is searched for in child-to-parent order.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Use the keyword <c>SYSTEM_DEFAULT</c> to refer to the system default configuration
	 * 		returned by the {@link Config#getSystemDefault()}.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String config() default "";

	/**
	 * Eagerly initializes framework-managed memoizers during {@link RestContext} construction.
	 *
	 * <p>
	 * When enabled, framework bean memoizers and operation/child-context memoizers are force-fired inside the
	 * constructor try/catch so startup-time configuration errors fail fast.
	 *
	 * <p>
	 * This setting is disabled by default.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> - Force eager initialization during context construction.
	 * 	<li><js>"false"</js> - Keep initialization lazy until first use (default).
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String eagerInit() default "";

	/**
	 * Opt this parent resource into deferred (first-invocation) construction of its {@link #children()} sub-resources.
	 *
	 * <p>
	 * When {@code "true"}, the {@link RestContext} instances for all {@code @Rest(children=...)} entries are
	 * <em>not</em> built at parent startup.  Instead, each child's routing entry is registered immediately (so URL
	 * matching is fully operational from the first request), but the full {@link RestContext} — including all of its
	 * bean-store setup, memoizers, and lifecycle hooks — is constructed on the first inbound request to that child's
	 * URL prefix.  Subsequent requests reuse the already-built context.
	 *
	 * <p>
	 * This is particularly useful when a parent resource exposes heavyweight admin or diagnostic children that are
	 * rarely invoked in production.  Setting {@code lazyChildren="true"} on the parent lets the parent boot fast;
	 * each child pays its construction cost only when (and if) it is first accessed.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> - Children are constructed on first invocation (deferred).
	 * 	<li><js>"false"</js> (default) - Children are constructed eagerly at parent startup.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The first request to a lazy child pays the full construction cost, which can be significant for
	 * 		heavyweight children.  If predictable first-request latency is required, do not opt in.
	 * 	<li class='note'>
	 * 		Concurrent first-requests to the same lazy child are serialized: only one thread runs the construction;
	 * 		others block until it completes.
	 * 	<li class='note'>
	 * 		A lazy child that is never invoked is never constructed.  Its lifecycle {@code @RestDestroy} / shutdown
	 * 		hooks are skipped at parent destruction time.
	 * 	<li class='note'>
	 * 		The programmatic knob {@link RestContext.Builder#lazyChildInit(boolean)} overrides this annotation.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$E{LAZY_CHILDREN,false}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link RestContext.Builder#lazyChildInit(boolean)}
	 * 	<li class='jm'>{@link #children()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String lazyChildren() default "";

	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] consumes() default {};

	/**
	 * Allowed serializer session option keys for this resource (ordered merge, prefix {@code -key} removes a key).
	 *
	 * <p>
	 * Comma-delimited list of session property keys (e.g. <js>"escapeSolidus,maxIndent"</js>) that clients may
	 * send via the <js>"X-Juneau-Serializer-Options"</js> header or <js>"juneauSerializerOptions"</js> query parameter.
	 * Keys not in the effective allowlist cause a {@code 400 Bad Request} response.
	 *
	 * <p>
	 * Entries are merged in application order. A leading hyphen removes a previously added key: <js>"-escapeSolidus"</js>.
	 * Method-level {@link org.apache.juneau.rest.annotation.RestGet#allowedSerializerOptions()} values are always merged on top.
	 * Use {@link #noInherit()} to prevent inheriting less-derived contributions.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SessionOptions#safe-properties">Session Options - Safe Properties</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] allowedSerializerOptions() default {};

	/**
	 * Allowed parser session option keys for this resource (ordered merge, prefix {@code -key} removes a key).
	 *
	 * <p>
	 * Comma-delimited list of session property keys that clients may send via the <js>"X-Juneau-Parser-Options"</js>
	 * header or <js>"juneauParserOptions"</js> query parameter. Parser options are ignored for operations without
	 * a request-body parser. Keys not in the effective allowlist cause a {@code 400 Bad Request} response.
	 *
	 * <p>
	 * Use {@link #noInherit()} to prevent inheriting less-derived contributions.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SessionOptions#safe-properties">Session Options - Safe Properties</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] allowedParserOptions() default {};

	/**
	 * Property names for which less-derived contributions are NOT inherited.
	 *
	 * <p>
	 * Accepted values include {@code "allowedParserOptions"}, {@code "allowedSerializerOptions"},
	 * {@code "allowedHeaderParams"}, {@code "allowedMethodHeaders"}, {@code "allowedMethodParams"},
	 * {@code "disableContentParam"}, {@code "renderResponseStackTraces"}, {@code "problemDetails"},
	 * {@code "eagerInit"}, {@code "lazyChildren"}, {@code "clientVersionHeader"},
	 * {@code "uriAuthority"}, {@code "uriContext"}, {@code "uriRelativity"}, and {@code "uriResolution"}.
	 * Each entry is SVL-resolved then comma-split. Prevents the named property from inheriting values from
	 * parent {@code @Rest} annotations (router hierarchy). The {@code noInherit} attribute itself is never inherited.
	 *
	 * <h5 class='section'>Mixin sub-contexts (since 9.5.0)</h5>
	 * <p>
	 * On a class declared via {@link #mixins() @Rest(mixins=...)} on a host, {@code noInherit} also blocks the
	 * host-to-mixin inheritance walk for the named property.  The token set extends to every contribution list
	 * exposed by {@code @Rest}: {@code "serializers"}, {@code "parsers"}, {@code "encoders"}, {@code "converters"},
	 * {@code "responseProcessors"}, {@code "restOpArgs"}, {@code "guards"}, {@code "callLogger"},
	 * {@code "debugEnablement"}, {@code "debugDefault"}, {@code "partSerializer"}, {@code "partParser"}, and
	 * {@code "messages"}.  For example, {@code @Rest(noInherit={"guards"})} on a mixin removes the host's guard
	 * chain from the mixin's endpoints (typical pattern for deliberately-unguarded probes like
	 * {@code HealthMixin}).
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SessionOptions">Session Options</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerMixinSubContexts">REST Server &mdash; Mixin Sub-Contexts</a> (for the per-property opt-out semantics)
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] noInherit() default {};

	/**
	 * Class-level response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Converters on child are combined with those on parent class.
	 * 	<li>Converters are executed child-to-parent in the order they appear in the annotation.
	 * 	<li>Converters on methods are executed before those on classes.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Enable debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * 	<li>
	 * 		HTTP requests/responses are logged to the registered {@link CallLogger}.
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> - Debug is enabled for all requests.
	 * 	<li><js>"false"</js> - Debug is disabled for all requests.
	 * 	<li><js>"conditional"</js> - Debug is enabled only for requests that have a <c class='snippet'>Debug: true</c> header.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		These debug settings can be overridden by the {@link Rest#debug()} annotation or at runtime by directly
	 * 		calling {@link RestRequest#setDebug()}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getDebugEnablement()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Debug debug() default @Debug;

	/**
	 * Default <c>Accept</c> header.
	 *
	 * <p>
	 * The default value for the <c>Accept</c> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String defaultAccept() default "";

	/**
	 * Default character encoding.
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link RestOp#defaultCharset}
	 * 	<li class='ja'>{@link RestGet#defaultCharset}
	 * 	<li class='ja'>{@link RestPut#defaultCharset}
	 * 	<li class='ja'>{@link RestPost#defaultCharset}
	 * 	<li class='ja'>{@link RestDelete#defaultCharset}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String defaultCharset() default "";

	/**
	 * Default <c>Content-Type</c> header.
	 *
	 * <p>
	 * The default value for the <c>Content-Type</c> header if not specified on a request.
	 *
	 * <p>
	 * This is a shortcut for using {@link #defaultRequestHeaders()} for just this specific header.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String defaultContentType() default "";

	/**
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <p>
	 * Affects values returned by the following methods:
	 * 	<ul>
	 * 		<li class='jm'>{@link RestRequest#getAttribute(String)}.
	 * 		<li class='jm'>{@link RestRequest#getAttributes()}.
	 * 	</ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(defaultRequestAttributes={<js>"Foo=bar"</js>, <js>"Baz: $C{REST/myAttributeValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestGet</ja>(defaultRequestAttributes={<js>"Foo: bar"</js>})
	 * 		<jk>public</jk> Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link RestOp#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestGet#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestPut#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestPost#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestDelete#defaultRequestAttributes()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestAttributes() default {};

	/**
	 * Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Optional servlet description.
	 *
	 * <p>
	 * It is used to populate the Swagger description field.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Description is searched for in child-to-parent order.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		The format is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Disable content URL parameter.
	 *
	 * <p>
	 * When enabled, the HTTP content content on PUT and POST requests can be passed in as text using the <js>"content"</js>
	 * URL parameter.
	 * <br>
	 * For example:
	 * <p class='burlenc'>
	 *  ?content=(name='John%20Smith',age=45)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableContentParam() default "";

	/**
	 * Opt the resource into <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a> /
	 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a>
	 * {@code application/problem+json} error responses.
	 *
	 * <p>
	 * When enabled, the resource:
	 * <ul class='spaced-list'>
	 * 	<li>Emits {@code application/problem+json} for thrown {@code BasicHttpException}s &mdash; regardless of the
	 * 		client's {@code Accept} header. The body is a serialized
	 * 		{@link org.apache.juneau.bean.rfc7807.Problem} populated from the exception's status code, reason phrase,
	 * 		and message.
	 * 	<li>Honors the client's {@code Accept} header on the success path. When a {@code @RestOp} method returns a
	 * 		{@link org.apache.juneau.bean.rfc7807.Problem} (or throws a
	 * 		{@link org.apache.juneau.bean.rfc7807.ProblemException}), the
	 * 		{@link org.apache.juneau.rest.processor.ProblemDetailsProcessor} only serializes it as
	 * 		{@code application/problem+json} when the {@code Accept} header matches that media type (or {@code *&#47;*}).
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> &mdash; enable problem-details responses for this resource.
	 * 	<li><js>"false"</js> &mdash; disable problem-details responses for this resource (overrides an inherited
	 * 		{@code "true"} from a parent {@code @Rest}).
	 * 	<li><js>""</js> (default) &mdash; inherit from the next-most-derived {@code @Rest} in the resource-class
	 * 		hierarchy. Default behavior (no opt-in anywhere in the chain) is unchanged from prior releases.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Use {@link #noInherit()} to prevent inheriting an opt-in from a parent {@code @Rest}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link org.apache.juneau.bean.rfc7807.Problem}
	 * 	<li class='jc'>{@link org.apache.juneau.bean.rfc7807.ProblemException}
	 * 	<li class='jc'>{@link org.apache.juneau.rest.processor.ProblemDetailsProcessor}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String problemDetails() default "";

	/**
	 * Opt this resource into per-request virtual-thread dispatch (Java 21+).
	 *
	 * <p>
	 * When enabled, every {@code @RestOp}-annotated handler invocation on this resource is submitted to a
	 * {@code Executors.newVirtualThreadPerTaskExecutor()} virtual-thread-per-task executor
	 * lazily built by the {@link org.apache.juneau.rest.RestContext}. The platform request thread blocks on
	 * the virtual thread's completion (so the handler's return value, exceptions, and observability hooks are
	 * preserved verbatim), but blocking I/O inside the handler now parks a virtual thread instead of the
	 * carrier — i.e. the carrier thread is freed to service other concurrent requests while the handler is
	 * parked on socket / file / lock waits. Combined with {@link java.util.concurrent.CompletableFuture}
	 * return types ({@code @RestGet} / {@code @RestPost} returning {@code CompletableFuture<T>}) this is the
	 * high-throughput pattern.
	 *
	 * <p>
	 * <b>Graceful degradation on Java 17/18/19/20:</b> the flag is detected during {@code RestContext}
	 * initialization. If the runtime is older than Java 21, a one-shot {@code WARNING} is logged and the
	 * resource falls back to the standard caller-thread dispatch path — no runtime error.
	 *
	 * <p>
	 * Per-{@code @RestOp} overrides are available via {@link RestOp#virtualThreads()}; the op-level setting
	 * takes precedence over the resource-level setting.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> &mdash; enable virtual-thread dispatch on Java 21+ (silently disabled on older JVMs).
	 * 	<li><js>"false"</js> &mdash; explicitly disable.
	 * 	<li><js>""</js> (default) &mdash; inherit from the next-most-derived {@code @Rest} in the resource-class
	 * 		hierarchy.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$E{ENABLE_VIRTUAL_THREADS,false}"</js>).
	 * 	<li class='note'>
	 * 		Synchronized blocks and JNI calls in handler code <i>pin</i> a virtual thread to its carrier thread
	 * 		— prefer {@link java.util.concurrent.locks.ReentrantLock} over {@code synchronized} in handlers run
	 * 		under this flag.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link RestOp#virtualThreads()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String virtualThreads() default "";

	/**
	 * Per-resource observability opt-in / opt-out control.
	 *
	 * <p>
	 * Controls whether the observability block ({@link org.apache.juneau.rest.metrics.MetricsRecorder} /
	 * {@link org.apache.juneau.rest.tracing.TracerHook}) fires for operations on this resource.
	 * Per-operation overrides are available via {@link RestOp#observability()} (and the verb annotations).
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> &mdash; strict opt-in: the resource <em>requires</em> a wired observability backend.
	 * 		If neither a {@code @Bean MetricsRecorder} nor a {@code @Bean TracerHook} is registered when the
	 * 		{@link org.apache.juneau.rest.RestContext} is built, construction fails with a precise error.
	 * 		All operations on this resource have observability enabled.
	 * 	<li><js>"false"</js> &mdash; explicit opt-out: the observability block is short-circuited for every
	 * 		operation on this resource, even when a wired backend is present. Also suppresses
	 * 		{@link org.apache.juneau.rest.tracing.TraceContextResponseProcessor} header injection for this resource.
	 * 	<li><js>""</js> (default) &mdash; inherits the existing behavior: the observability block runs per-op,
	 * 		but a missing {@code @Bean} silently falls back to the NoOp singleton — no startup error.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$E{ENABLE_OBSERVABILITY,false}"</js>).
	 * 	<li class='note'>
	 * 		Per-operation overrides via {@link RestOp#observability()} (or verb annotations) take precedence over
	 * 		this resource-level setting.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link RestOp#observability()}
	 * 	<li class='jc'>{@link org.apache.juneau.rest.metrics.MetricsRecorder}
	 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.TracerHook}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 9.5.0
	 */
	String observability() default "";

	/**
	 * Configurable timeout (milliseconds) applied to {@link java.util.concurrent.CompletableFuture}-returning
	 * handlers by {@link org.apache.juneau.rest.processor.AsyncResponseProcessor}. Default is 30,000 ms.
	 *
	 * <p>
	 * On timeout, the future is cancelled with {@code mayInterruptIfRunning=true} and the response is
	 * committed as {@code 504 Gateway Timeout}. Set to {@code "0"} to disable the timeout entirely.
	 *
	 * <p>
	 * Per-{@code @RestOp} overrides are available via {@link RestOp#asyncTimeoutMillis()}; the op-level
	 * setting takes precedence over the resource-level setting.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$E{ASYNC_TIMEOUT_MS,30000}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link org.apache.juneau.rest.processor.AsyncResponseProcessor}
	 * 	<li class='ja'>{@link RestOp#asyncTimeoutMillis()}
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 9.5.0
	 */
	String asyncTimeoutMillis() default "";

	/**
	 * Names the {@link java.util.concurrent.Executor} bean used to route {@link java.util.concurrent.CompletableFuture}
	 * completion callbacks through a dedicated thread pool.
	 *
	 * <p>
	 * When set, the {@link org.apache.juneau.rest.processor.AsyncResponseProcessor} switches from
	 * {@code future.whenComplete(callback)} to {@code future.whenCompleteAsync(callback, executor)}, so the
	 * response-handler work runs on the named pool instead of the future's natural completion thread (often an
	 * I/O-driver or database-callback thread).
	 *
	 * <p>
	 * The value is the {@link org.apache.juneau.commons.inject.Bean#name() @Bean(name=...)} lookup key for an
	 * {@link java.util.concurrent.Executor} registered in the resource's bean store.  Empty string (default)
	 * means no override — completion callbacks run on the future's natural thread (the existing behavior).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, asyncCompletionExecutor=<js>"myCompletionPool"</js>)
	 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
	 *
	 * 		<ja>@Bean</ja>(name=<js>"myCompletionPool"</js>)
	 * 		<jk>public</jk> Executor completionPool() {
	 * 			<jk>return</jk> Executors.newFixedThreadPool(8);
	 * 		}
	 *
	 * 		<ja>@RestGet</ja>(<js>"/orders/{id}"</js>)
	 * 		<jk>public</jk> CompletableFuture&lt;Order&gt; getOrder(<ja>@Path</ja> String id) {
	 * 			<jk>return</jk> orderService.fetchAsync(id); <jc>// callback runs on myCompletionPool</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Per-operation overrides are available via {@link RestOp#asyncCompletionExecutor()} (and the verb annotations).
	 * A reference to a bean name that does not resolve in the bean store causes a startup failure.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		MDC propagation still works when this executor is set — the
	 * 		{@link org.apache.juneau.rest.processor.MdcAsyncListener} wraps the callback <em>before</em> it is
	 * 		routed through the executor, so the MDC snapshot is restored on whichever thread the executor picks.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link org.apache.juneau.rest.processor.AsyncResponseProcessor}
	 * 	<li class='ja'>{@link RestOp#asyncCompletionExecutor()}
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 9.5.0
	 */
	String asyncCompletionExecutor() default "";

	/**
	 * Specifies the compression encoders for this resource.
	 *
	 * <p>
	 * Encoders are used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <p>
	 * Encoders are automatically inherited from {@link Rest#encoders()} annotations on parent classes with the encoders on child classes
	 * prepended to the encoder group.
	 * The {@link org.apache.juneau.encoders.EncoderSet.NoInherit} class can be used to prevent inheriting from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that handles GZIP compression.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		encoders={
	 * 			GzipEncoder.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The encoders can also be tailored at the method level using {@link RestOp#encoders()} (and related annotations).
	 *
	 * <p>
	 * For programmatic equivalents, contribute an {@link org.apache.juneau.encoders.EncoderSet} bean via
	 * {@link org.apache.juneau.commons.inject.Bean @Bean(name="encoders")}.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Encoders on child are combined with those on parent class.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerEncoders">Encoders</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Default form data parameter definitions.
	 *
	 * <p>
	 * Provides default values for {@link FormData @FormData} annotations on method parameters across all methods in this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		formDataParams={
	 * 			<ja>@FormData</ja>(name=<js>"action"</js>, def=<js>"submit"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	FormData[] formDataParams() default {};

	/**
	 * Class-level guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Guards on child are combined with those on parent class.
	 * 	<li>Guards are executed child-to-parent in the order they appear in the annotation.
	 * 	<li>Guards on methods are executed before those on classes.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Default header parameter definitions.
	 *
	 * <p>
	 * Provides default values for {@link Header @Header} annotations on method parameters across all methods in this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		headerParams={
	 * 			<ja>@Header</ja>(name=<js>"Accept-Language"</js>, def=<js>"en-US"</js>),
	 * 			<ja>@Header</ja>(name=<js>"X-API-Version"</js>, def=<js>"1.0"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	Header[] headerParams() default {};

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link RestOp#maxInput}
	 * 	<li class='ja'>{@link RestPost#maxInput}
	 * 	<li class='ja'>{@link RestPut#maxInput}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maxInput() default "";

	/**
	 * Messages.
	 *
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * There are two possible formats:
	 * <ul>
	 * 	<li>A simple string - Represents the {@link org.apache.juneau.cp.Messages.Builder#name(String) name} of the resource bundle.
	 * 		<br><br><i>Example:</i>
	 * 		<p class='bjava'>
	 * 	<jc>// Bundle name is Messages.properties.</jc>
	 * 	<ja>@Rest</ja>(messages=<js>"Messages"</js>)
	 * 		</p>
	 * 	<li>Simplified JSON - Represents parameters for the {@link org.apache.juneau.cp.Messages.Builder} class.
	 * 		<br><br><i>Example:</i>
	 * 		<p class='bjava'>
	 * 	<jc>// Bundles can be found in two packages.</jc>
	 * 	<ja>@Rest</ja>(messages=<js>"{name:'Messages',baseNames:['{package}.{name}','{package}.i18n.{name}']"</js>)
	 * 		</p>
	 * </ul>
	 *
	 * <p>
	 * If the bundle name is not specified, the class name of the resource object is used.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String messages() default "";

	/**
	 * Specifies the parsers for converting HTTP request bodies into POJOs.
	 *
	 * <p>
	 * Parsers are used to convert the content of HTTP requests into POJOs.
	 * <br>Any of the Juneau framework parsers can be used in this setting.
	 * <br>The parser selected is based on the request <c>Content-Type</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Parser#getMediaTypes()}
	 * </ul>
	 *
	 * <p>
	 * Parsers are automatically inherited from {@link Rest#parsers()} annotations on parent classes with the parsers on child classes
	 * prepended to the parser group.
	 * The {@link org.apache.juneau.parser.ParserSet.NoInherit} class can be used to prevent inheriting from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that can consume JSON and XML.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		parsers={
	 * 			JsonParser.<jk>class</jk>,
	 * 			XmlParser.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The parsers can also be tailored at the method level using {@link RestOp#parsers()} (and related annotations).
	 *
	 * <p>
	 * For programmatic equivalents, contribute a {@link org.apache.juneau.parser.ParserSet} bean via
	 * {@link org.apache.juneau.commons.inject.Bean @Bean(name="parsers")}.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Parsers on child override those on parent class.
	 * 	<li>{@link org.apache.juneau.parser.ParserSet.Inherit} class can be used to inherit and augment values from parent.
	 * 	<li>{@link org.apache.juneau.parser.ParserSet.NoInherit} class can be used to suppress inheriting from parent.
	 * 	<li>Parsers on methods take precedence over those on classes.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] parsers() default {};

	/**
	 * HTTP part parser.
	 *
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> partParser() default HttpPartParser.Void.class;

	/**
	 * HTTP part serializer.
	 *
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> partSerializer() default HttpPartSerializer.Void.class;

	/**
	 * Resource path.
	 *
	 * <p>
	 * Used in the following situations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		On child resources (resource classes attached to parents via the {@link #children()} annotation) to identify
	 * 		the subpath used to access the child resource relative to the parent.
	 * 	<li>
	 * 		On top-level {@link RestServlet} classes deployed as Spring beans when <c>JuneauRestInitializer</c> is being used.
	 * </ul>
	 *
	 * <h5 class='topic'>On child resources</h5>
	 * <p>
	 * The typical usage is to define a path to a child resource relative to the parent resource.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		children={ChildResource.<jk>class</jk>}
	 * 	)
	 * 	<jk>public class</jk> TopLevelResource <jk>extends</jk> BasicRestServlet {...}
	 *
	 * 	<ja>@Rest</ja>(
	 *		path=<js>"/child"</js>,
	 *		children={GrandchildResource.<jk>class</jk>}
	 *	)
	 *	<jk>public class</jk> ChildResource {...}
	 *
	 *	<ja>@Rest</ja>(
	 *		path=<js>"/grandchild"</js>
	 *	)
	 *	<jk>public class</jk> GrandchildResource {
	 *		<ja>@RestGet</ja>(<js>"/"</js>)
	 *		<jk>public</jk> String sayHello() {
	 *			<jk>return</jk> <js>"Hello!"</js>;
	 *		}
	 *	}
	 * </p>
	 * <p>
	 * In the example above, assuming the <c>TopLevelResource</c> servlet is deployed to path <c>/myContext/myServlet</c>,
	 * then the <c>sayHello</c> method is accessible through the URI <c>/myContext/myServlet/child/grandchild</c>.
	 *
	 * <p>
	 * Note that in this scenario, the <c>path</c> attribute is not defined on the top-level resource.
	 * Specifying the path on the top-level resource has no effect, but can be used for readability purposes.
	 *
	 * <h5 class='topic'>Path variables</h5>
	 * <p>
	 * The path can contain variables that get resolved to {@link org.apache.juneau.http.annotation.Path @Path} parameters
	 * or access through the {@link RestRequest#getPathParams()} method.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/myResource/{foo}/{bar}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 *		<ja>@RestGet</ja>(<js>"/{baz}"</js>)
	 *		<jk>public void</jk> String doX(<ja>@Path</ja> String <jv>foo</jv>, <ja>@Path</ja> <jk>int</jk> <jv>bar</jv>, <ja>@Path</ja> MyPojo <jv>baz</jv>) {
	 *			...
	 *		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Variables can be used on either top-level or child resources and can be defined on multiple levels.
	 *
	 * <p>
	 * All variables in the path must be specified or else the target will not resolve and a <c>404</c> will result.
	 *
	 * <p>
	 * When variables are used on a path of a top-level resource deployed as a Spring bean in a Spring Boot application,
	 * the first part of the URL must be a literal which will be used as the servlet path of the registered servlet.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The leading slash is optional.  <js>"/myResource"</js> and <js>"myResource"</js> is equivalent.
	 * 	<li class='note'>
	 * 		The paths <js>"/myResource"</js> and <js>"/myResource/*"</js> are equivalent.
	 * 	<li class='note'>
	 * 		Paths must not end with <js>"/"</js> (per the servlet spec).
	 * 	<li class='note'>
	 * 		Effective only when this class is registered directly with the servlet container as a top-level
	 * 		resource.  When this class is imported as a mixin via {@code @Rest(mixins=...)}, the importing
	 * 		host's own {@code path()} or {@link #paths()} governs the mount and this annotation is silently
	 * 		ignored &mdash; mixin endpoints land in the host's URL namespace.
	 * </ul>
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Path is searched for in child-to-parent order.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String path() default "";

	/**
	 * Additional servlet mount paths.
	 *
	 * <p>
	 * Optional multi-mount companion to {@link #path()} for top-level servlet deployment.
	 *
	 * <p>
	 * When specified, servlet containers may mount this resource on each listed path.
	 * This is primarily intended for built-in support endpoints such as health probes where multiple exact URLs
	 * should be served by a single servlet instance.
	 *
	 * <h5 class='section'>Runtime substitution (since 9.5.0)</h5>
	 * <p>
	 * Each array element is treated as a template:
	 * <ol>
	 * 	<li>The element runs through
	 * 		<a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL</a>
	 * 		variable substitution (e.g. {@code $C{health.paths}}, {@code $E{HEALTH_PATHS,/healthz}},
	 * 		{@code $S{my.system.prop,/healthz}}) using the bootstrap {@link org.apache.juneau.commons.svl.VarResolver
	 * 		VarResolver} on the bean store.
	 * 	<li>The post-SVL value is split on {@code ,}; each piece is trimmed and empty pieces are dropped.
	 * 		A single template element can therefore expand to zero, one, or many mount paths.
	 * </ol>
	 *
	 * <h5 class='figure'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// 1. Single key resolving to a multi-value config string.</jc>
	 * 	<ja>@Rest</ja>(paths={<js>"$C{health.paths}"</js>})
	 *
	 * 	<jc>// 2. Mix literal + resolved element.</jc>
	 * 	<ja>@Rest</ja>(paths={<js>"/api"</js>, <js>"$C{extra.paths}"</js>})
	 *
	 * 	<jc>// 3. Env var with comma-separated defaults baked in.</jc>
	 * 	<ja>@Rest</ja>(paths={<js>"$E{HEALTH_PATHS,/healthz,/readyz}"</js>})
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Paths are normalized to servlet path-specs by the hosting runtime.
	 * 	<li class='note'>
	 * 		When both {@link #path()} and {@link #paths()} are present, runtimes may use {@link #paths()} for
	 * 		top-level mounting and continue using {@link #path()} for child-resource composition.
	 * 	<li class='note'>
	 * 		The annotation default sits at the lowest rung of the runtime-override resolution chain &mdash; see
	 * 		{@link RestContext#getPaths()} for the full precedence order (programmatic &gt; getter &gt; annotation).
	 * 	<li class='note'>
	 * 		An SVL failure (unresolved variable with no default) falls back to the literal element rather than
	 * 		throwing during construction.
	 * 	<li class='note'>
	 * 		Effective only when this class is registered directly with the servlet container as a top-level
	 * 		resource.  When this class is imported as a mixin via {@code @Rest(mixins=...)}, the importing
	 * 		host's own {@link #path()} or {@code paths()} governs the mount and this annotation is silently
	 * 		ignored &mdash; mixin endpoints land in the host's URL namespace.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] paths() default {};

	/**
	 * Default path parameter definitions.
	 *
	 * <p>
	 * Provides default values for {@link Path @Path} annotations on method parameters across all methods in this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		pathParams={
	 * 			<ja>@Path</ja>(name=<js>"version"</js>, def=<js>"v1"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	Path[] pathParams() default {};

	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] produces() default {};

	/**
	 * Default query parameter definitions.
	 *
	 * <p>
	 * Provides default values for {@link Query @Query} annotations on method parameters across all methods in this class.
	 * Values specified here are used as defaults when the same property is not explicitly defined on a method parameter's
	 * {@link Query @Query} annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define common query parameters at class level</jc>
	 * 	<ja>@Rest</ja>(
	 * 		queryParams={
	 * 			<ja>@Query</ja>(name=<js>"format"</js>, def=<js>"json"</js>, description=<js>"Output format"</js>),
	 * 			<ja>@Query</ja>(name=<js>"verbose"</js>, def=<js>"false"</js>, schema=<ja>@Schema</ja>(type=<js>"boolean"</js>))
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Method will inherit the default query parameter definitions</jc>
	 * 		<ja>@RestGet</ja>
	 * 		<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"format"</js>) String format, <ja>@Query</ja>(<js>"verbose"</js>) <jk>boolean</jk> verbose) {...}
	 *
	 * 		<jc>// Can override defaults on a per-method basis</jc>
	 * 		<ja>@RestGet</ja>
	 * 		<jk>public</jk> String doGet2(<ja>@Query</ja>(name=<js>"format"</js>, def=<js>"xml"</js>) String format) {...}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		These defaults apply to validation, parsing, and OpenAPI/Swagger documentation generation.
	 * 	<li class='note'>
	 * 		Method-level {@link Query @Query} annotations take precedence over class-level definitions.
	 * 	<li class='note'>
	 * 		The {@link Query#name() name} attribute must be specified for each query definition.
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	Query[] queryParams() default {};

	/**
	 * Render response stack traces in responses.
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String renderResponseStackTraces() default "";

	/**
	 * Response processors.
	 *
	 * <p>
	 * Specifies a list of {@link ResponseProcessor} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setContent(Object)} into appropriate HTTP responses.
	 *
	 * @return The annotation value.
	 */
	Class<? extends ResponseProcessor>[] responseProcessors() default {};

	/**
	 * Java REST operation method parameter resolvers.
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * <br>This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestOpArg>[] restOpArgs() default {};

	/**
	 * Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <p>
	 * This is a shortcut for specifying {@link RestOp#roleGuard()} on all the REST operations on a class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | baz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || baz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link #rolesDeclared()}.
	 * 	<li>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String roleGuard() default "";

	/**
	 * Declared roles.
	 *
	 * <p>
	 * A comma-delimited list of all possible user roles.
	 *
	 * <p>
	 * Used in conjunction with {@link #roleGuard()} is used with patterns.
	 *
	 * <p>
	 * This is a shortcut for specifying {@link RestOp#rolesDeclared()} on all the REST operations on a class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String rolesDeclared() default "";

	/**
	 * Specifies the serializers for POJOs into HTTP response bodies.
	 *
	 * <p>
	 * Serializer are used to convert POJOs to HTTP response bodies.
	 * <br>Any of the Juneau framework serializers can be used in this setting.
	 * <br>The serializer selected is based on the request <c>Accept</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
	 * </ul>
	 *
	 * <p>
	 * Serializers are automatically inherited from {@link Rest#serializers()} annotations on parent classes with the serializers on child classes
	 * prepended to the serializer group.
	 * The {@link org.apache.juneau.serializer.SerializerSet.NoInherit} class can be used to prevent inheriting from the parent class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a REST resource that can produce JSON and XML.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		serializers={
	 * 			JsonParser.<jk>class</jk>,
	 * 			XmlParser.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The serializers can also be tailored at the method level using {@link RestOp#serializers()} (and related annotations).
	 *
	 * <p>
	 * For programmatic equivalents, contribute a {@link org.apache.juneau.serializer.SerializerSet} bean via
	 * {@link org.apache.juneau.commons.inject.Bean @Bean(name="serializers")}.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Serializers on child override those on parent class.
	 * 	<li>{@link org.apache.juneau.serializer.SerializerSet.Inherit} class can be used to inherit and augment values from parent.
	 * 	<li>{@link org.apache.juneau.serializer.SerializerSet.NoInherit} class can be used to suppress inheriting from parent.
	 * 	<li>Serializers on methods take precedence over those on classes.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Optional site name.
	 *
	 * <p>
	 * The site name is intended to be a title that can be applied to the entire site.
	 *
	 * <p>
	 * One possible use is if you want to add the same title to the top of all pages by defining a header on a
	 * common parent class like so:
	 * <p class='bjava'>
	 *  <ja>@HtmlDocConfig</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$RS{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$RS{title}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String siteName() default "";

	/**
	 * Static files.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@code BasicRestResource.getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@code BasicRestServlet.getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <p>
	 * The static file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getStaticFiles()}
	 * </ul>
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Static files on child are combined with those on parent class.
	 * 	<li>Static files are are executed child-to-parent in the order they appear in the annotation.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends StaticFiles> staticFiles() default StaticFiles.Void.class;

	/**
	 * Provides swagger-specific metadata on this resource.
	 *
	 * <p>
	 * Used to populate the auto-generated OPTIONS swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/addressBook"</js>,
	 *
	 * 		<jc>// Swagger info.</jc>
	 * 		swagger=@Swagger({
	 * 			<js>"contact:{name:'John Smith',email:'john@smith.com'},"</js>,
	 * 			<js>"license:{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'},"</js>,
	 * 			<js>"version:'2.0',</js>,
	 * 			<js>"termsOfService:'You are on your own.',"</js>,
	 * 			<js>"tags:[{name:'Java',description:'Java utility',externalDocs:{description:'Home page',url:'http://juneau.apache.org'}}],"</js>,
	 * 			<js>"externalDocs:{description:'Home page',url:'http://juneau.apache.org'}"</js>
	 * 		})
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Swagger}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Swagger swagger() default @Swagger;

	/**
	 * Swagger provider.
	 *
	 * @return The annotation value.
	 */
	Class<? extends SwaggerProvider> swaggerProvider() default SwaggerProvider.Void.class;

	/**
	 * OpenAPI 3.1 provider.
	 *
	 * <p>
	 * Sibling of {@link #swaggerProvider()} for the OpenAPI 3.1 emission path. Defaults to
	 * {@link org.apache.juneau.rest.openapi.OpenApiProvider.Void OpenApiProvider.Void}
	 * which resolves to {@code BasicOpenApiProvider} unless the bean store provides an override.
	 *
	 * @return The annotation value.
	 */
	Class<? extends OpenApiProvider> openApiProvider() default OpenApiProvider.Void.class;

	/**
	 * Optional servlet title.
	 *
	 * <p>
	 * It is used to populate the Swagger title field.
	 *
	 * <h5 class='section'>Inheritance Rules</h5>
	 * <ul>
	 * 	<li>Label is searched for in child-to-parent order.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Corresponds to the swagger field <c>/info/title</c>.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] title() default {};

	/**
	 * Resource authority path.
	 *
	 * <p>
	 * Overrides the authority path value for this resource and any child resources.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriAuthority() default "";

	/**
	 * Resource context path.
	 *
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriContext() default "";

	/**
	 * URI-resolution relativity.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriRelativity() default "";

	/**
	 * URI-resolution.
	 *
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriResolution() default "";
}