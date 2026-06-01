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

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that emits a JSON index of all {@link RestOp @RestOp}-annotated methods on the host
 * resource (and any other mixins on it) at {@code /options} (configurable).
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
 * Resolution happens once at {@link RestContext} construction time; see the FINISHED-99 archive
 * (SVL resolution in {@code @RestOp(path)}) for the full resolution chain.
 *
 * <p>
 * Override accepts bare token ({@code options}), leading slash ({@code /options}), or trailing
 * slash ({@code options/}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (9.5.0):</b> Earlier development snapshots of this mixin mounted at both
 * {@code /options} <i>and</i> {@code /routes} as historical aliases on a single op. That dual
 * default has been collapsed to a single SVL-configurable mount as part of the
 * "single path per op" principle (see FINISHED-101). Deployers who relied on the
 * {@code /routes} alias must now set {@code -Djuneau.routeindex.path=routes} or compose a
 * second instance with the override.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/${juneau.routeindex.path:options}")}
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
 * surfaced as a single entry; the request returns a JSON list ordered by path:
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
 * @since 9.5.0
 */
// @formatter:off
@Rest
public class RouteIndexMixin {

	private static final List<Class<? extends Annotation>> REST_OP_ANNOTATIONS = List.of(
		RestGet.class, RestPost.class, RestPut.class, RestDelete.class,
		RestPatch.class, RestOptions.class, RestOp.class);

	/** No-arg constructor &mdash; route-index has no configurable state. */
	public RouteIndexMixin() {}

	/**
	 * [GET /options] &mdash; emit the route index as a JSON list.
	 *
	 * @param req The current REST request &mdash; supplies the host {@link RestContext}.
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.routeindex.path:options})}",
		summary="Route index",
		description="JSON list of @RestOp-annotated methods on the host (excluding hidden / ops endpoints).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getRoutes(RestRequest req, RestResponse res) throws IOException {
		var hostCtx = resolveHostContext(req.getContext());
		var entries = collect(hostCtx);
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT_READABLE.serialize(entries, w);
		}
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
	public List<Map<String,Object>> collect(RestContext hostCtx) {
		var seen = new HashSet<Method>();
		var entries = new ArrayList<Map<String,Object>>();
		for (var oc : hostCtx.getRestOperations().getOpContexts())
			addEntry(entries, seen, oc);
		for (var mixinCtx : hostCtx.getMixinContexts().values())
			for (var oc : mixinCtx.getRestOperations().getOpContexts())
				addEntry(entries, seen, oc);
		entries.sort(RouteIndexMixin::compareByPathThenMethod);
		return entries;
	}

	private static void addEntry(List<Map<String,Object>> entries, Set<Method> seen, RestOpContext oc) {
		var m = oc.getJavaMethod();
		if (m == null || ! seen.add(m))
			return;
		if (isHiddenFromIndex(m))
			return;
		if (isSelfHandler(m))
			return;
		var entry = new LinkedHashMap<String,Object>();
		entry.put("path", oc.getPathPattern());
		entry.put("methods", List.of(oc.getHttpMethod()));
		entry.put("summary", readSummary(m));
		entry.put("description", readDescription(m));
		entry.put("deprecated", m.isAnnotationPresent(Deprecated.class)
			|| m.getDeclaringClass().isAnnotationPresent(Deprecated.class));
		entries.add(entry);
	}

	@SuppressWarnings("java:S3776") // Cognitive-complexity: linear walk over a small annotation list; splitting hurts JIT.
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

	@SuppressWarnings("java:S3776") // Same as isHiddenFromIndex — short loop, single concern.
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

	@SuppressWarnings("java:S3776") // Same as isHiddenFromIndex — short loop, single concern.
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

	private static int compareByPathThenMethod(Map<String,Object> a, Map<String,Object> b) {
		var c = String.valueOf(a.get("path")).compareTo(String.valueOf(b.get("path")));
		if (c != 0)
			return c;
		return String.valueOf(a.get("methods")).compareTo(String.valueOf(b.get("methods")));
	}
}
