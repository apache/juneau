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
package org.apache.juneau.rest.ops;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.beans.*;

/**
 * Mixin that exposes a content-negotiated index of all {@link RestOp @RestOp}-annotated methods on the
 * host resource (and any other mixins on it) at {@code /options} (configurable).
 *
 * <p>
 * The op returns a {@link RouteDescriptions} POJO rather than writing a fixed format, so Juneau's content
 * negotiation serves a browsable HTML page (with clickable route links), JSON, or XML based on the request
 * {@code Accept} header &mdash; mirroring the way {@code NavigationMixin.getChildren(...)} produces a
 * navigation page while still serving JSON to API clients. The host therefore needs serializers configured
 * (e.g. via {@code BasicUniversalConfig} / a {@code Basic*} servlet base); a bare {@code @Rest} host with no
 * serializers cannot content-negotiate the listing.
 *
 * <p>
 * Sibling of {@link EchoMixin} ({@code /echo/*}) and {@link AdminMixin}
 * ({@code /admin/*}). All three classes live in the {@code org.apache.juneau.rest.ops}
 * ops/introspection mixin pack.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /options} can be overridden via the SVL variable
 * {@code ${juneau.routeindex.path:options}} &mdash; set via system property
 * ({@code -Djuneau.routeindex.path=routes}), environment variable
 * ({@code JUNEAU_ROUTEINDEX_PATH=routes}), or {@code Config} key
 * ({@code juneau.routeindex.path = routes}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code options}), leading slash ({@code /options}), or trailing
 * slash ({@code options/}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (10.0.0):</b> Earlier development snapshots of this mixin mounted at both
 * {@code /options} <i>and</i> {@code /routes} as historical aliases on a single op. That dual
 * default has been collapsed to a single SVL-configurable mount as part of the
 * "single path per op" principle. Deployers who relied on the
 * {@code /routes} alias must now set {@code -Djuneau.routeindex.path=routes} or compose a
 * second instance with the override.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/&#123;juneau.routeindex.path:options&#125;")}
 * on {@link #getRoutes}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc). Note also
 * that the route-index walks the host's {@link RestContext} via {@link #resolveHostContext}
 * &mdash; it only makes sense in a host-attached composition, not standalone.
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=RouteIndexMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 * 		<ja>@RestGet</ja>(path=<js>"/items"</js>, summary=<js>"List items"</js>) <jk>public</jk> List&lt;Item&gt; items() { ... }
 * 	}
 * </p>
 *
 * <h5 class='section'>Output:</h5>
 *
 * <p>
 * Each {@code @RestOp}-annotated method on the host (and on any other mixins on the host) is
 * surfaced as a single entry, ordered by path. Rendered as JSON (for an {@code Accept: application/json}
 * request) the listing looks like:
 *
 * <p class='bjson'>
 * 	[
 * 		{
 * 			<jok>"path"</jok>: <jov>"/items"</jov>,
 * 			<jok>"methods"</jok>: [<jov>"GET"</jov>],
 * 			<jok>"summary"</jok>: <jov>"List items"</jov>,
 * 			<jok>"description"</jok>: <jov>""</jov>,
 * 			<jok>"deprecated"</jok>: <jov>false</jov>
 * 		}
 * 	]
 * </p>
 *
 * <p>
 * <b>Excluded entries:</b>
 * <ul class='spaced-list'>
 * 	<li>The route-index endpoint itself (it shouldn't echo its own listing).
 * 	<li>Any operation marked {@link OpSwagger#ignore() @OpSwagger(ignore=true)} &mdash; consistent
 * 		with how those operations are excluded from the OpenAPI spec by
 * 		{@code BasicSwaggerProviderSession}. Convention endpoints (favicon, robots, version, etc.),
 * 		static-files mixin handlers, and the sibling ops-pack endpoints all carry that annotation
 * 		and are therefore omitted from the index, matching the audience separation: api-docs is
 * 		for documented public API; route-index is for the same surface but in machine-readable
 * 		form.
 * 	<li>Lifecycle / filter beans &mdash; only methods with a {@link RestOp}-group annotation
 * 		(GET / POST / PUT / DELETE / PATCH / OPTIONS / RestOp) are listed; {@link RestStartCall}
 * 		/ {@link RestEndCall} / converters / matchers are not.
 * </ul>
 *
 * <p>
 * The handler itself carries {@link OpSwagger#ignore() @OpSwagger(ignore=true)} so the
 * route-index endpoint is excluded from the OpenAPI spec.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link EchoMixin}
 * 	<li class='jc'>{@link AdminMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are REST media type strings and path names; intentional
})
public class RouteIndexMixin {

	private static final List<Class<? extends Annotation>> REST_OP_ANNOTATIONS = List.of(
		RestGet.class, RestPost.class, RestPut.class, RestDelete.class,
		RestPatch.class, RestOptions.class, RestOp.class);

	/** No-arg constructor &mdash; route-index has no configurable state. */
	public RouteIndexMixin() { /* intentionally empty */ }

	/**
	 * [GET /options] &mdash; return the route index as a content-negotiated POJO.
	 *
	 * <p>
	 * Returns a {@link RouteDescriptions} list rather than writing a fixed format, so the response is
	 * content-negotiated through the host's configured serializers: an {@code Accept: text/html} request
	 * renders a browsable table (with each {@linkplain RouteDescription#getPath() path} as a clickable link),
	 * while {@code application/json} / {@code text/xml} clients receive the same entries in their requested
	 * format &mdash; mirroring the way {@code NavigationMixin.getChildren(...)} backs the child-resource
	 * navigation page.
	 *
	 * @param req The current REST request &mdash; supplies the host {@link RestContext}.
	 * @return The route-index entries, ordered by path.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.routeindex.path:options})}",
		summary="Route index",
		description="Content-negotiated list of @RestOp-annotated methods on the host (excluding hidden / ops endpoints).",
		swagger=@OpSwagger(ignore=true)
	)
	public RouteDescriptions getRoutes(RestRequest req) {
		return collect(resolveHostContext(req.getContext()));
	}

	private static RestContext resolveHostContext(RestContext c) {
		var ctx = c;
		while (ctx.isMixinContext() && ctx.getParentContext() != null)
			ctx = ctx.getParentContext();
		return ctx;
	}

	/**
	 * Collects the route-index entries from the supplied host {@link RestContext} and any of its
	 * registered mixin sub-contexts (test/inspection helper).
	 *
	 * @param hostCtx The host context. Must not be {@code null}.
	 * @return A list of route-index entries, ordered by path.
	 */
	public RouteDescriptions collect(RestContext hostCtx) {
		var seen = new HashSet<Method>();
		var entries = new RouteDescriptions();
		for (var oc : hostCtx.getRestOperations().getOpContexts())
			addEntry(entries, seen, oc);
		for (var mixinCtx : hostCtx.getMixinContexts().values())
			for (var oc : mixinCtx.getRestOperations().getOpContexts())
				addEntry(entries, seen, oc);
		entries.sort(RouteIndexMixin::compareByPathThenMethod);
		return entries;
	}

	private static void addEntry(RouteDescriptions entries, Set<Method> seen, RestOpContext oc) {
		var m = oc.getJavaMethod();
		if (m == null || ! seen.add(m))
			return;
		if (isHiddenFromIndex(m))
			return;
		if (isSelfHandler(m))
			return;
		entries.append(
			oc.getPathPattern(),
			List.of(oc.getHttpMethod()),
			readSummary(m),
			readDescription(m),
			m.isAnnotationPresent(Deprecated.class) || m.getDeclaringClass().isAnnotationPresent(Deprecated.class)
		);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive-complexity: linear walk over a small annotation list; splitting hurts JIT.
	})
	private static boolean isHiddenFromIndex(Method m) {
		for (var aClass : REST_OP_ANNOTATIONS) {
			var a = m.getAnnotation(aClass);
			if (a == null)
				continue;
			try {
				var sw = aClass.getMethod("swagger").invoke(a);
				if (sw instanceof OpSwagger os && os.ignore())
					return true;
			} catch (ReflectiveOperationException e) {
				// Annotation chain shape mismatch — treat as not-hidden so we don't silently drop
				// real entries on a future refactor.
				return false;
			}
		}
		return false;
	}

	private static boolean isSelfHandler(Method m) {
		return m.getDeclaringClass() == RouteIndexMixin.class;
	}

	@SuppressWarnings({
		"java:S3776" // Same as isHiddenFromIndex — short loop, single concern.
	})
	private static String readSummary(Method m) {
		for (var aClass : REST_OP_ANNOTATIONS) {
			var a = m.getAnnotation(aClass);
			if (a == null)
				continue;
			try {
				var s = (String) aClass.getMethod("summary").invoke(a);
				if (s != null && ! s.isEmpty())
					return s;
			} catch (ReflectiveOperationException e) {
				// fall through
			}
		}
		return "";
	}

	@SuppressWarnings({
		"java:S3776" // Same as isHiddenFromIndex — short loop, single concern.
	})
	private static String readDescription(Method m) {
		for (var aClass : REST_OP_ANNOTATIONS) {
			var a = m.getAnnotation(aClass);
			if (a == null)
				continue;
			try {
				var d = aClass.getMethod("description").invoke(a);
				if (d instanceof String[] arr && arr.length > 0)
					return String.join(" ", arr);
				if (d instanceof String s && ! s.isEmpty())
					return s;
			} catch (ReflectiveOperationException e) {
				// fall through
			}
		}
		return "";
	}

	private static int compareByPathThenMethod(RouteDescription a, RouteDescription b) {
		var c = String.valueOf(a.getPath()).compareTo(String.valueOf(b.getPath()));
		if (c != 0)
			return c;
		return String.valueOf(a.getMethods()).compareTo(String.valueOf(b.getMethods()));
	}
}
