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
 * <h5 class='section'>Builder support:</h5>
 *
 * <p>
 * The fluent programmatic-builder surface ({@link RestBuilder} / {@link Builder}) lets a mixin be configured
 * programmatically rather than (or in addition to) by annotation &mdash; builder-supplied values take precedence
 * over {@link Rest @Rest} annotation values.  Use {@link #builder(Class)} for the common case, or subclass
 * {@link Builder} for capability mixins that add their own setters (TODO-143 Option B).
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
	 * The programmatic configuration builder stashed on this instance (TODO-143 &sect;2.4), or <jk>null</jk> when the
	 * mixin was constructed without a builder.  Mutable so it can be written by either the
	 * {@link #RestMixin(RestBuilder)} constructor or {@link Builder#build()}.  Read non-reflectively by
	 * {@link RestContext} during mixin sub-context construction so builder-supplied values take precedence over
	 * {@link Rest @Rest} annotation values.
	 */
	RestBuilder restBuilder;

	/**
	 * Default constructor.
	 */
	protected RestMixin() {}

	/**
	 * Builder-injection constructor (TODO-145 &sect;2.4 constructor trio).
	 *
	 * @param builder The programmatic configuration builder.  May be <jk>null</jk>.
	 */
	protected RestMixin(RestBuilder builder) {
		this.restBuilder = builder;
	}

	/**
	 * Returns the programmatic configuration builder stashed on this mixin, or <jk>null</jk> if none.
	 *
	 * @return The stashed builder, or <jk>null</jk>.
	 */
	public RestBuilder getRestBuilder() {
		return restBuilder;
	}

	/**
	 * Creates a new fluent builder for programmatically configuring an instance of the specified mixin type.
	 *
	 * @param <R> The mixin type.
	 * @param type The mixin type to build.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <R extends RestMixin> DefaultBuilder<R> builder(Class<R> type) {
		return new DefaultBuilder<>(type);
	}

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

	/**
	 * Fluent builder for programmatically configuring a {@link RestMixin} subclass.
	 *
	 * <p>
	 * Subclassable, self-typed (CRTP) flavor builder (TODO-143 Option B).  Capability mixins (e.g.
	 * {@code FaviconMixin}) extend this and add their own worker-config setters, which chain with true covariant
	 * returns alongside the inherited {@link RestBuilder} surface.  For the common (non-subclassed) case use
	 * {@link RestMixin#builder(Class)} which returns the concrete {@link DefaultBuilder} leaf.
	 *
	 * @param <R> The mixin type produced by {@link #build()}.
	 * @param <SELF> The concrete builder type (self type).
	 */
	public static class Builder<R extends RestMixin, SELF extends Builder<R, SELF>> extends AbstractRestBuilder<R, SELF> {

		/**
		 * Constructor.
		 *
		 * @param type The mixin type produced by {@link #build()}.  Must not be <jk>null</jk>.
		 */
		protected Builder(Class<R> type) {
			super(type);
		}

		@Override /* AbstractRestBuilder */
		public R build() {
			var r = createResource();
			r.restBuilder = this;
			return r;
		}
	}

	/**
	 * Concrete default leaf builder returned by {@link RestMixin#builder(Class)} for the common (non-subclassed)
	 * case.
	 *
	 * @param <R> The mixin type produced by {@link #build()}.
	 */
	public static final class DefaultBuilder<R extends RestMixin> extends Builder<R, DefaultBuilder<R>> {
		DefaultBuilder(Class<R> type) {
			super(type);
		}
	}
}
