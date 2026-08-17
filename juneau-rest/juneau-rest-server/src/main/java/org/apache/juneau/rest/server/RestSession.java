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
package org.apache.juneau.rest.server;

import static java.util.Collections.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.logging.*;
import org.apache.juneau.rest.server.util.*;

import jakarta.servlet.http.*;

/**
 * Represents a single HTTP request.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., PROP_context)
	"resource"   // the per-call BasicBeanStore is owned by this RestSession (closed via finish/close paths); fluent add/addBean calls return the same store we already own.
})
public class RestSession extends ContextSession {

	/** Logger for the finish-path diagnostic-failure containment token (see {@link #finish()}). */
	private static final RichLogger LOG = RichLogger.getLogger(RestSession.class);

	// Property name constants
	private static final String PROP_context = "context";
	private static final String PROP_resource = "resource";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";
	private static final String ARG_value = "value";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required: annotation type parameter is unknown at static analysis time.
	})
	public static class Builder extends ContextSession.Builder {

		private HttpServletRequest req;
		private HttpServletResponse res;
		private Object resource;
		private RestContext ctx;
		private String pathInfoUndecoded;
		private UrlPath urlPath;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(RestContext ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override /* Overridden from Session.Builder */
		public RestSession build() {
			return new RestSession(this);
		}

		/**
		 * Returns the undecoded request servlet path info.
		 *
		 * @return The undecoded request servlet path info, or <jk>null</jk> if there is no extra path information.
		 */
		public String getPathInfoUndecoded() {
			if (pathInfoUndecoded == null)
				pathInfoUndecoded = RestUtils.getPathInfoUndecoded(req);
			return pathInfoUndecoded;
		}

		/**
		 * Returns the request path info as a {@link UrlPath} bean.
		 *
		 * @return The request path info as a {@link UrlPath} bean.
		 */
		public UrlPath getUrlPath() {
			if (urlPath == null)
				urlPath = UrlPath.of(getPathInfoUndecoded());
			return urlPath;
		}

		/**
		 * Adds resolved <c><ja>@Resource</ja>(path)</c> variable values to this call.
		 *
		 * @param value The variables to add to this call.
		 * 	<br>Can be <jk>null</jk> (ignored).
		 * @return This object.
		 */
		@SuppressWarnings({
			"unchecked" // Type erasure requires cast for pathVars
		})
		public Builder pathVars(Map<String,String> value) {
			if (nn(value) && ! value.isEmpty()) {
				var m = (Map<String,String>)req.getAttribute(REST_PATHVARS_ATTR);
				if (m == null) {
					m = new TreeMap<>();
					req.setAttribute(REST_PATHVARS_ATTR, m);
				}
				m.putAll(value);
			}
			return this;
		}

		/**
		 * Returns the HTTP servlet request object on this call.
		 *
		 * @return The HTTP servlet request object on this call.
		 */
		public HttpServletRequest req() {
			urlPath = null;
			pathInfoUndecoded = null;
			return req;
		}

		/**
		 * Specifies the HTTP servlet request object on this call.
		 *
		 * @param value The value for this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder req(HttpServletRequest value) {
			req = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Returns the HTTP servlet response object on this call.
		 *
		 * @return The HTTP servlet response object on this call.
		 */
		public HttpServletResponse res() {
			return res;
		}

		/**
		 * Specifies the HTTP servlet response object on this call.
		 *
		 * @param value The value for this setting.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder res(HttpServletResponse value) {
			res = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Specifies the servlet implementation bean.
		 *
		 * @param value The value for this setting.
		 * 	<br>Can be <jk>null</jk> (no outer bean will be used for instantiating inner classes).
		 * @return This object.
		 */
		public Builder resource(Object value) {
			resource = value;
			return this;
		}
	}

	/**
	 * Request attribute name for passing path variables from parent to child.
	 */
	private static final String REST_PATHVARS_ATTR = "juneau.pathVars";

	/**
	 * Public-internal servlet-request attribute key under which the active {@link RestSession} publishes itself.
	 *
	 * <p>
	 * The session-handle seam that lets code in other packages resolve the call's cached correlation id without a live
	 * public/custom-key attribute read: the debug formatter's {@code statusLine} (in the {@code logging} package) and
	 * {@code RequestIdFilter} (in the {@code filter} package) both read the session via {@link #fromRequest} and call
	 * {@link #getRequestId()}.  Not part of the public REST contract &mdash; the attribute name is an internal detail.
	 */
	public static final String REQUEST_SESSION_ATTR = "juneau.internal.RestSession";

	/**
	 * Creates a builder of this object.
	 *
	 * @param ctx The context creating this builder.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(RestContext ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Returns the {@link RestSession} published on the given servlet request via the {@link #REQUEST_SESSION_ATTR}
	 * session-handle seam, or <jk>null</jk> if none is present.
	 *
	 * @param req The servlet request.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The active session, or <jk>null</jk>.
	 */
	public static RestSession fromRequest(HttpServletRequest req) {
		return (nn(req) && req.getAttribute(REQUEST_SESSION_ATTR) instanceof RestSession s) ? s : null;
	}

	private final long startTime = System.currentTimeMillis();
	private final WritableBeanStore beanStore;
	private HttpServletRequest req;
	private HttpServletResponse res;
	private Map<String,String[]> queryParams;
	private final Object resource;
	private final RestContext context;
	private RestOpSession opSession;
	private String method;
	private String pathInfoUndecoded;
	private UrlPath urlPath;
	private UrlPathMatch urlPathMatch;

	/**
	 * [TODO-401] Tracks whether an HTTP status was explicitly assigned via {@link #status(int)} /
	 * {@link #status(HttpStatusLine)} on this call.  The {@code NotFound} sentinel in {@link #run()} keys off this
	 * flag instead of {@code getStatus() == 0}, so a genuine 404 stays a 404 under a real container whose response
	 * defaults to the servlet-spec {@code 200} (not the mock's coincidental {@code 0}).  A raw
	 * {@code HttpServletResponse.setStatus(...)} (e.g. from a {@code @RestStartCall} hook) bypasses this flag; that
	 * broader real-container 404-status audit is TODO-403's remit, not this fix.
	 */
	private boolean statusExplicitlySet;

	/** The correlation id resolved for this call at build time (mint-or-honor).  Never {@code null}. */
	private final String requestId;

	/**
	 * The single open {@code requestId} {@link LogContext} scope for this call, opened after id resolution and closed
	 * exactly once in {@link #finish()} (after emission) &mdash; on the request thread, so a pooled request thread never
	 * carries the scope into the next request.
	 */
	private final LogContext.Scope requestIdScope;

	/**
	 * Opaque holder for the debug-resolution snapshot ({@code RestDebugSnapshot}) published at the async-dispatch
	 * handoff and read back on the response-completion thread. Typed as {@link Object} because the snapshot type is a
	 * package-private detail of the {@code logging} package that this class must not expose. Immutable once stashed;
	 * published before the completion callback is registered.
	 */
	private Object debugSnapshot;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 * 	<br>Cannot be <jk>null</jk>.
	 */
	public RestSession(Builder builder) {
		super(builder);
		context = builder.ctx;
		resource = builder.resource;
		beanStore = new BasicBeanStore(context.getBeanStore()).addBean(RestContext.class, context);

		pathInfoUndecoded = builder.pathInfoUndecoded;
		req = beanStore.add(HttpServletRequest.class, builder.req);
		res = beanStore.add(HttpServletResponse.class, builder.res);
		urlPath = beanStore.add(UrlPath.class, builder.urlPath);

		// Always-on request-id correlation resolver.  Runs at every session build so it covers 404 / early-error paths
		// that never reach a @RestStartCall hook, and gives the debug formatter a synchronous, session-cached id to
		// render.  Resolve the id first, then open exactly one LogContext scope (closed in finish()).
		var settings = context.getRequestIdSettings();
		requestId = resolveRequestId(settings, req, res);
		req.setAttribute(REQUEST_SESSION_ATTR, this);
		req.setAttribute(settings.getAttributeKey(), requestId);
		res.setHeader(RequestIdConstants.HEADER, requestId);
		requestIdScope = RichLogger.context().with(RestServerConstants.REQUEST_ID, requestId);
	}

	/**
	 * Mints or honors the correlation id for this call.
	 *
	 * <p>
	 * Honors (in order): a non-empty id already stashed under the settings attribute key (idempotent re-entry or a
	 * parent filter); else a sanitized, length-capped, non-truncated, validator-accepted incoming {@code X-Request-Id}
	 * header (sanitize-and-accept); else a freshly minted id from the settings supplier.
	 *
	 * @param settings The resolved request-id settings.  Must not be <jk>null</jk>.
	 * @param req The servlet request.  Must not be <jk>null</jk>.
	 * @param res The servlet response.  Must not be <jk>null</jk>.
	 * @return The resolved id.  Never <jk>null</jk> (unless a custom supplier returns <jk>null</jk>).
	 */
	private static String resolveRequestId(RequestIdSettings settings, HttpServletRequest req, HttpServletResponse res) {
		var existing = req.getAttribute(settings.getAttributeKey());
		if (existing instanceof String s && ! s.isEmpty())
			return s;
		var incoming = req.getHeader(RequestIdConstants.HEADER);
		if (nn(incoming)) {
			var sanitized = DebugTextSanitizer.sanitize(incoming, RequestIdConstants.MAX_LEN);
			if (nn(sanitized) && ! sanitized.isEmpty() && ! sanitized.endsWith(DebugTextSanitizer.TRUNCATED_MARKER) && settings.getValidator().test(sanitized))
				return sanitized;
		}
		return settings.getIdSupplier().get();
	}

	/**
	 * Installs bounded body-caching wrappers on this call's request/response for debug capture (Phase A).
	 *
	 * <p>
	 * Called by the two-phase debug pipeline when the resolved logger is loggable at
	 * {@link java.util.logging.Level#FINEST FINEST}. Idempotent — the wrappers no-op if already installed.
	 *
	 * <p>
	 * The capture cap is snapshotted from {@link RestContext#getRestDebugFormatter()}<c>.bodyCap()</c> at install
	 * time, so a formatter override actually raises, lowers, or (via {@code bodyCap(0)}) disables the number of
	 * bytes retained &mdash; not just the wrapper's own 8&nbsp;KB default.
	 *
	 * @return This object.
	 * @throws IOException Occurs if the request/response streams could not be wrapped.
	 */
	public RestSession installCapture() throws IOException {
		var cap = resolveBodyCap();
		req = CachingHttpServletRequest.wrap(req, cap);
		res = CachingHttpServletResponse.wrap(res, cap);
		return this;
	}

	/**
	 * Resolves the body capture cap in effect for this call, per {@link RestDebugFormatter#bodyCap()}.
	 *
	 * <p>
	 * Mirrors the formatter-resolution fallback used by {@link RestDebugPipeline} (a bean-store lookup can return
	 * <jk>null</jk> despite the {@link RestContext#getRestDebugFormatter()} javadoc contract) so the cap enforced at
	 * capture time always matches the cap the eventual render will report against.
	 *
	 * @return The body capture cap, in bytes.
	 */
	private int resolveBodyCap() {
		var formatter = context.getRestDebugFormatter();
		return formatter != null ? formatter.bodyCap() : new BasicRestDebugFormatter().bodyCap();
	}

	/**
	 * Identifies that an exception occurred during this call.
	 *
	 * @param value The thrown exception.
	 * 	<br>Can be <jk>null</jk> (will clear the exception attribute and remove the exception from the bean store).
	 * @return This object.
	 */
	public RestSession exception(Throwable value) {
		req.setAttribute("Exception", value);
		beanStore.addBean(Throwable.class, value);
		return this;
	}

	/**
	 * Called at the end of a call to finish any remaining tasks such as flushing buffers and logging the response.
	 *
	 * <p>
	 * When the request has been handed off to a real {@code AsyncContext}, the response body/headers are written later
	 * on the response-completion thread. In that case this method neither flushes nor emits nor writes formatter-visible
	 * finish-time attributes — the completion hook owns all of that after the async body exists. The synchronous and
	 * synchronous-fallback (e.g. MockRestClient) paths are unchanged: they set finish-time attributes, flush, and emit
	 * here.
	 *
	 * @return This object.
	 */
	public RestSession finish() {
		// Emit-before-close: the requestId LogContext scope stays open through synchronous emission (so the emitted
		// record carries the structured id), then always closes in the finally — on the request thread, even on the
		// async path (where the snapshot was already captured during run()) — so a pooled thread never leaks the scope.
		try {
			var asyncOwned = org.apache.juneau.rest.server.processor.AsyncResponseProcessor.isAsyncDispatchOwned(req);
			try {
				if (! asyncOwned)
					setFinishTimeAttributes();
				if (nn(opSession))
					opSession.finish();
				else if (! asyncOwned) {
					// Skip flush when AsyncContext has been started — see AsyncResponseProcessor.
					res.flushBuffer();
				}
			} catch (Exception e) {
				exception(e);
			}
			// Skip synchronous emission on the async path — the completion hook emits after the body/headers are written.
			if (asyncOwned)
				return this;
			// Contain diagnostic formatting/emission: a formatter (or a scrubber that escaped the fail-closed guard) throwing
			// a RuntimeException/Error during a completed request must not escape and fail the request thread. Log only a
			// fixed token — never e.getMessage(), the body, the stack, or a second formatter pass — so a scrubber throwing
			// new RuntimeException(body) cannot re-leak the secret the placeholder just refused.
			try {
				RestDebugPipeline.emit(this);
			} catch (Throwable t) {  // NOSONAR - deliberate containment of any diagnostic failure at request completion.
				LOG.log(Level.WARNING, "debug formatter failed");
			}
			return this;
		} finally {
			closeRequestIdScope();
		}
	}

	/**
	 * Closes this call's {@code requestId} {@link LogContext} scope, if open.
	 *
	 * <p>
	 * Idempotent: safe to call more than once (and from a build path that never reaches {@link #finish()}, such as a
	 * throwaway error session).  Must run on the request thread that opened the scope so a pooled thread does not carry
	 * the scope into the next request.
	 */
	public void closeRequestIdScope() {
		if (nn(requestIdScope))
			requestIdScope.close();
	}

	/**
	 * Records the finish-time request attributes consumed by the debug formatter (currently {@code ExecTime}).
	 *
	 * <p>
	 * On the synchronous path this is called from {@link #finish()}. On the asynchronous path it is called by the
	 * completion hook immediately before rendering — never by {@link #finish()} after the async handoff — so the record
	 * always reflects the true request duration and the completion thread never races the request thread over
	 * formatter-visible response state. Idempotent: recomputing the value on repeat calls is harmless.
	 */
	public void setFinishTimeAttributes() {
		req.setAttribute("ExecTime", System.currentTimeMillis() - startTime);
	}

	/**
	 * Stashes the opaque debug-resolution snapshot on the request thread at the async-dispatch handoff.
	 *
	 * <p>
	 * Internal plumbing for the two-thread async debug pipeline; not part of the public REST contract. The value is a
	 * package-private {@code RestDebugSnapshot} passed as {@link Object} so this class does not expose the type.
	 *
	 * @param value The snapshot, or <jk>null</jk> when access logging is off.
	 */
	public void stashDebugSnapshot(Object value) { debugSnapshot = value; }

	/**
	 * Returns the opaque debug-resolution snapshot stashed at the async-dispatch handoff.
	 *
	 * <p>
	 * Internal plumbing for the two-thread async debug pipeline; not part of the public REST contract.
	 *
	 * @return The stashed snapshot, or <jk>null</jk> if none was stashed (synchronous path or access logging off).
	 */
	public Object getDebugSnapshot() { return debugSnapshot; }

	/**
	 * Returns the correlation id resolved for this call (mint-or-honor), cached at build time.
	 *
	 * <p>
	 * This is the synchronous, session-cached value the debug formatter and {@code RequestIdFilter} read through the
	 * {@link #fromRequest(HttpServletRequest) session-handle seam} &mdash; never a live public/custom-key attribute
	 * read.
	 *
	 * @return The resolved request id.  Never <jk>null</jk> (unless a custom id supplier returned <jk>null</jk>).
	 */
	public String getRequestId() { return requestId; }

	/**
	 * Returns the bean store of this call.
	 *
	 * @return The bean store of this call.
	 */
	public WritableBeanStore getBeanStore() { return beanStore; }

	/**
	 * Returns the context that created this call.
	 *
	 * @return The context that created this call.
	 */
	@Override
	public RestContext getContext() { return context; }

	/**
	 * Returns the exception that occurred during this call.
	 *
	 * @return The exception that occurred during this call, or <jk>null</jk> if no exception occurred.
	 */
	public Throwable getException() { return (Throwable)req.getAttribute("Exception"); }

	private static final AsciiSet VALID_METHOD_CHARS = AsciiSet.create().ranges("A-Z", "a-z" ,"0-9").chars("_-").build();

	/**
	 * Returns the HTTP method name.
	 *
	 * @return The HTTP method name, always uppercased.
	 * @throws MethodNotAllowed If the method parameter contains invalid/malformed characters.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for REST method resolution/mapping
	})
	public String getMethod() throws NotFound {
		if (method == null) {

			Set<String> s1 = context.getAllowedMethodParams();
			Set<String> s2 = context.getAllowedMethodHeaders();

			if (! s1.isEmpty()) {
				String[] x = getQueryParams().get("method");
				if (nn(x) && (s1.contains("*") || s1.contains(x[0])))
					method = x[0];
				if (method != null && ! VALID_METHOD_CHARS.containsOnly(method)) {
					throw new MethodNotAllowed();
				}
			}

			if (method == null && ! s2.isEmpty()) {
				var x = req.getHeader("X-Method");
				if (nn(x) && (s2.contains("*") || s2.contains(x)))
					method = x;
				if (method != null && ! VALID_METHOD_CHARS.containsOnly(method)) {
					throw new MethodNotAllowed();
				}
			}

			if (method == null)
				method = req.getMethod();

			method = ucr(method);
		}

		return method;
	}

	/**
	 * Returns the operation session of this REST session.
	 *
	 * <p>
	 * The operation session is created once the Java method to be invoked has been determined.
	 *
	 * @return The operation session of this REST session.
	 * @throws InternalServerError If operation session has not been created yet.
	 */
	public RestOpSession getOpSession() throws InternalServerError {
		if (opSession == null)
			throw new InternalServerError("Op Session not created.");
		return opSession;
	}

	/**
	 * Returns the {@link RestOpSession} created during {@link #run()} if one has been built, or {@code null} otherwise.
	 *
	 * <p>
	 * Unlike {@link #getOpSession()}, this method does not throw — it lets callers in lifecycle code paths
	 * (such as mixin endCall dual-firing in {@link RestContext#execute(Object, jakarta.servlet.http.HttpServletRequest,
	 * jakarta.servlet.http.HttpServletResponse)}) inspect whether an operation was ever resolved without risking
	 * a spurious 500 when 404/405/412 paths bypass operation creation entirely.
	 *
	 * @return The operation session, or {@code null} if one was never created (e.g. early routing failure).
	 * @since 10.0.0
	 */
	public RestOpSession getOpSessionOrNull() { return opSession; }

	/**
	 * Shortcut for calling <c>getRequest().getPathInfo()</c>.
	 *
	 * @return The request servlet path info.
	 */
	public String getPathInfo() { return req.getPathInfo(); }

	/**
	 * Same as {@link #getPathInfo()} but doesn't decode encoded characters.
	 *
	 * @return The undecoded request servlet path info, or <jk>null</jk> if there is no extra path information.
	 */
	public String getPathInfoUndecoded() {
		if (pathInfoUndecoded == null)
			pathInfoUndecoded = RestUtils.getPathInfoUndecoded(req);
		return pathInfoUndecoded;
	}

	/**
	 * Returns resolved <c><ja>@Resource</ja>(path)</c> variable values on this call.
	 *
	 * @return Resolved <c><ja>@Resource</ja>(path)</c> variable values on this call.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for pathVars
	})
	public Map<String,String> getPathVars() {
		var m = (Map<String,String>)req.getAttribute(REST_PATHVARS_ATTR);
		return m == null ? emptyMap() : m;
	}

	/**
	 * Returns the query parameters on the request.
	 *
	 * <p>
	 * Unlike {@link HttpServletRequest#getParameterMap()}, this doesn't parse the content if it's a POST.
	 *
	 * @return The query parameters on the request.
	 */
	public Map<String,String[]> getQueryParams() {
		if (queryParams == null) {
			if (req.getMethod().equalsIgnoreCase("POST")) {
				var listMap = RestUtils.parseQuery(req.getQueryString());
				queryParams = map();
				for (var e : listMap.entrySet()) {
					if (e.getValue() == null)
						queryParams.put(e.getKey(), null);
					else
						queryParams.put(e.getKey(), array(e.getValue(), String.class));
				}
			} else
				queryParams = req.getParameterMap();
		}
		return queryParams;
	}

	/**
	 * Returns the HTTP servlet request of this REST call.
	 *
	 * @return the HTTP servlet request of this REST call.
	 */
	public HttpServletRequest getRequest() { return req; }

	/**
	 * Returns the REST object.
	 *
	 * @return The rest object, or <jk>null</jk> if no resource bean was specified.
	 */
	public Object getResource() { return resource; }

	/**
	 * Returns the HTTP servlet response of this REST call.
	 *
	 * @return the HTTP servlet response of this REST call.
	 */
	public HttpServletResponse getResponse() { return res; }

	/**
	 * Shortcut for calling <c>getRequest().getServletPath()</c>.
	 *
	 * @return The request servlet path.
	 */
	public String getServletPath() { return req.getServletPath(); }

	/**
	 * Shortcut for calling <c>getRequest().getStatus()</c>.
	 *
	 * @return The response status code.
	 */
	public int getStatus() { return res.getStatus(); }

	/**
	 * Returns the request path info as a {@link UrlPath} bean.
	 *
	 * @return The request path info as a {@link UrlPath} bean.
	 */
	public UrlPath getUrlPath() {
		if (urlPath == null)
			urlPath = UrlPath.of(getPathInfoUndecoded());
		return urlPath;
	}

	/**
	 * Returns the URL path pattern match on this call.
	 *
	 * @return The URL path pattern match on this call, or <jk>null</jk> if not set.
	 */
	public UrlPathMatch getUrlPathMatch() { return urlPathMatch; }

	/**
	 * Runs this session.
	 *
	 * <p>
	 * Does the following:
	 * <ol>
	 * 	<li>Finds the Java method to invoke and creates a {@link RestOpSession} for it.
	 * 	<li>Invokes {@link RestPreCall} methods by calling {@link RestContext#preCall(RestOpSession)}.
	 * 	<li>Invokes Java method by calling {@link RestOpSession#run()}.
	 * 	<li>Invokes {@link RestPostCall} methods by calling {@link RestContext#postCall(RestOpSession)}.
	 * 	<li>If the Java method produced output, finds the response processor for it and runs it by calling {@link RestContext#processResponse(RestOpSession)}.
	 * 	<li>If no Java method matched, generates a 404/405/412 by calling {@link RestContext#handleNotFound(RestSession)}.
	 * </ol>
	 *
	 * @throws Exception Any exception can be thrown.
	 */
	public void run() throws Exception {
		try {
			opSession = context.getRestOperations().findOperation(this).createSession(this).build();
			// For mixin endpoints, fire the mixin's @RestStartCall hooks now that the operation is resolved.
			// The host's @RestStartCall hooks already fired in RestContext#execute() before s.run().
			// Order: host first, mixin second.
			var opRestContext = opSession.getContext().getContext();
			if (opRestContext.isMixinContext())
				opRestContext.startCall(this);
			if (! authenticateOrChallenge(opRestContext))
				return;
			context.preCall(opSession);
			opSession.run();
			context.postCall(opSession);
			if (res.getStatus() == 0)
				res.setStatus(200);
			if (opSession.getResponse().hasContent()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				context.processResponse(opSession);
			}
		} catch (NotFound e) {
			if (! statusExplicitlySet)
				status(404);
			exception(e);
			context.handleNotFound(this);
		}
	}

	/**
	 * Runs the resource-level authentication fold ({@link RestAuthenticator}) and, on failure, writes the 401 challenge.
	 *
	 * <p>
	 * Runs before preCall so {@link RestPreCall @RestPreCall} hooks, guards (roleGuard), and {@code @Auth} arg resolution
	 * all see the resolved principal/roles.
	 *
	 * @param opRestContext The operation's REST context.
	 * @return <jk>true</jk> if the request should proceed, or <jk>false</jk> if a 401 challenge was sent and processing
	 * 	should stop.
	 * @throws IOException If writing the challenge response fails.
	 */
	private boolean authenticateOrChallenge(RestContext opRestContext) throws IOException {
		try {
			opRestContext.authenticate(opSession);
			return true;
		} catch (AuthenticationException e) {
			AuthFilter.sendChallenge(res, e);
			return false;
		}
	}

	/**
	 * Sets the HTTP status on this call.
	 *
	 * @param value The status code.
	 * @return This object.
	 */
	public RestSession status(int value) {
		statusExplicitlySet = true;
		res.setStatus(value);
		return this;
	}

	/**
	 * Sets the HTTP status on this call.
	 *
	 * @param value The status code.
	 * 	<br>Can be <jk>null</jk> (ignored).
	 * @return This object.
	 */
	public RestSession status(HttpStatusLine value) {
		if (nn(value)) {
			statusExplicitlySet = true;
			res.setStatus(value.getStatusCode());
		}
		return this;
	}

	/**
	 * Sets the URL path pattern match on this call.
	 *
	 * @param value The match pattern.
	 * 	<br>Can be <jk>null</jk> (stored as an explicit <jk>null</jk> binding, distinct from no binding at all).
	 * @return This object.
	 */
	public RestSession urlPathMatch(UrlPathMatch value) {
		urlPathMatch = beanStore.add(UrlPathMatch.class, value);
		return this;
	}

	@Override /* Overridden from ContextSession */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_context, context)
			.a(PROP_resource, resource);
	}
}