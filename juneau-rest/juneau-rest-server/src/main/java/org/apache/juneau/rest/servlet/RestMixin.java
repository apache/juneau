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
package org.apache.juneau.rest.servlet;

import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Optional base class for <c>@Rest</c> mixin classes &mdash; the third sibling of {@link RestServlet}
 * (servlet flavor) and {@link RestResource} (child-resource flavor), completing the
 * servlet / resource / mixin naming triad.
 *
 * <p>
 * A <i>mixin</i> is a plain {@link Rest @Rest}-annotated POJO whose {@code @RestOp} methods are pulled
 * into a host servlet/resource context when the host declares
 * {@link Rest#mixins() @Rest(mixins=ThatMixin.class)}. Mixins compose through the existing sub-context
 * model; {@code RestMixin} does <b>not</b> change that composition mechanism.
 *
 * <h5 class='section'>Opt-in, not mandatory:</h5>
 *
 * <p>
 * Extending {@code RestMixin} is <b>optional</b>. A bare {@code @Rest} POJO with no base class remains a
 * fully-supported mixin (annotation-only configuration), exactly as before &mdash; the framework reads
 * such mixins reflectively. Capability mixins that ship without state (e.g. the api-docs pack) stay
 * base-less; the new single-responsibility op-mixins extend this base to make the triad membership
 * explicit.
 *
 * <h5 class='section'>Builder support (deferred):</h5>
 *
 * <p>
 * The fluent programmatic-builder surface ({@code RestBuilder}/{@code RestMixin.Builder}) that would let
 * a mixin be configured programmatically rather than by annotation is <b>not</b> part of this base yet
 * &mdash; it is deferred along with the resource/servlet builder work. Until then, {@code RestMixin}
 * carries no builder or stashed-builder state and mixins are configured purely by their {@code @Rest} /
 * {@code @RestOp} annotations.
 *
 * <h5 class='section'>Reaching the host resource:</h5>
 *
 * <p>
 * Because a mixin's {@code @RestOp} methods are bound to a per-mixin {@link RestContext} sub-context (the
 * sub-context has no children of its own), host-level introspection performed against the mixin's <i>own</i>
 * context comes back empty.  {@link #getHostContext()} bridges that gap: it returns the {@link RestContext} of
 * the host (mixed-into) resource so a mixin op can enumerate the host's child resources, swagger, stats, etc.
 *
 * <p class='bjava'>
 * 	<jc>// Render the host's child-resource navigation list from a mixin op.</jc>
 * 	<ja>@RestGet</ja>(path=<js>"/"</js>)
 * 	<jk>public</jk> ChildResourceDescriptions getChildren(RestRequest <jv>req</jv>) {
 * 		<jk>return new</jk> ChildResourceDescriptions(getHostContext(), <jv>req</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RestServlet}
 * 	<li class='jc'>{@link RestResource}
 * 	<li class='jm'>{@link #getHostContext()}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
public abstract class RestMixin {

	/**
	 * The per-mixin {@link RestContext} sub-context this mixin instance is bound to, captured via
	 * {@link #setContext(RestContext)} when the host composes the mixin.
	 *
	 * <p>
	 * Remains {@code null} when a {@code RestMixin} subclass is instantiated directly (not composed via
	 * {@link Rest#mixins() @Rest(mixins=...)}), in which case {@link #getHostContext()} returns {@code null}.
	 */
	private final AtomicReference<RestContext> context = new AtomicReference<>();

	/**
	 * Captures the per-mixin {@link RestContext} sub-context this mixin instance is bound to.
	 *
	 * <p>
	 * Invoked reflectively by the host's {@code RestContext.buildMixinContext(...)} while composing the mixin
	 * (mirroring the {@code setContext(RestContext)} contract honored by {@link RestServlet} and
	 * {@link RestResource} for child resources).  The supplied context is the mixin sub-context whose
	 * {@link RestContext#getParentContext() parent} is the host &mdash; that linkage is what {@link #getHostContext()}
	 * reads.
	 *
	 * @param value The mixin sub-context. Must not be <jk>null</jk>.
	 */
	protected void setContext(RestContext value) {
		context.set(value);
	}

	/**
	 * Returns the {@link RestContext} of the host (mixed-into) resource this mixin is composed into.
	 *
	 * <p>
	 * A mixin's {@code @RestOp} methods are bound to a per-mixin sub-context that has no children of its own;
	 * this accessor returns the host context so a mixin op (or config-time code) can introspect the host
	 * &mdash; most commonly its child resources via {@link RestContext#getRestChildren()} for the navigation
	 * page.  It is backed by the mixin sub-context's already-populated
	 * {@link RestContext#getParentContext() parent linkage}, so it is usable at config time and does not depend
	 * on an in-flight request.
	 *
	 * <h5 class='section'>Edge cases:</h5><ul>
	 * 	<li class='note'><b>Standalone / no host</b> &mdash; when a {@code RestMixin} subclass is instantiated
	 * 		directly rather than composed via {@link Rest#mixins() @Rest(mixins=...)}, this returns {@code null},
	 * 		mirroring {@link RestContext#getParentContext()}'s top-level contract.  Callers needing host-only
	 * 		behavior should null-check.
	 * 	<li class='note'><b>Nested mixins</b> &mdash; under nested {@code @Rest(mixins=...)} the flat-inheritance
	 * 		rule collects every mixin as a mixin of the single top-level host, so this returns that top-level
	 * 		host (never an intermediate mixin).
	 * </ul>
	 *
	 * @return The host resource's {@link RestContext}, or {@code null} when this mixin is not composed into a host.
	 * @since 9.5.0
	 */
	public RestContext getHostContext() {
		var c = context.get();
		return c == null ? null : c.getParentContext();
	}
}
