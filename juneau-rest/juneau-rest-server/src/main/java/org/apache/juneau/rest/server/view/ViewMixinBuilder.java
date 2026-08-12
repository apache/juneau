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
package org.apache.juneau.rest.server.view;

/**
 * Minimal shared contract implemented by the builder of every view-engine mixin
 * ({@code JspMixin.Builder}, {@code ThymeleafMixin.Builder}, {@code MustacheMixin.Builder},
 * {@code FreemarkerMixin.Builder}).
 *
 * <p>
 * The four view-engine bridges each evolved their own builder independently (§2.3.1 worker-bean
 * composition), and their per-engine knobs legitimately diverge &mdash; e.g. only Thymeleaf exposes
 * {@code templateMode(...)}, only Mustache/FreeMarker expose {@code templateSuffix(...)}. This
 * interface pins down the small subset of knobs that are meaningful to <b>every</b> engine, so a
 * caller who has learned one bridge's shape can rely on these two methods transferring to the
 * others without re-reading each bridge's Javadoc.
 *
 * <p>
 * Implementations return their own concrete builder type from each method so call chains stay
 * fluent (e.g. {@code JspMixin.create().basePath(...).build()}); the self-bounded type parameter
 * {@code <B>} captures that covariant return type.
 *
 * <p>
 * <b>Note on {@code cacheTemplates(boolean)}:</b> not every engine has a real caching knob to back
 * this with. Bridges that can honor it (e.g. Mustache, FreeMarker, Thymeleaf) apply it to their
 * bridge-default engine/factory/configuration. The JSP bridge accepts and reports the flag for
 * interface conformance, but has no effect: JSP recompilation is entirely delegated to the servlet
 * container via {@code RequestDispatcher.forward(...)}, and containers manage their own JSP
 * recompilation policy (e.g. Tomcat's {@code development} / {@code modificationTestInterval}
 * settings) outside this bridge's control. See each {@code *Mixin.Builder#cacheTemplates(boolean)}
 * override's own Javadoc for the exact per-engine behavior.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @param <B> The implementing builder's own concrete type.
 * @since 10.0.0
 */
public interface ViewMixinBuilder<B extends ViewMixinBuilder<B>> {

	/**
	 * Sets the base path under which the engine's template resources are resolved.
	 *
	 * @param value The base path. Bridges treat {@code null} / blank as "reset to the bridge's own default".
	 * @return This object (for method chaining).
	 */
	B basePath(String value);

	/**
	 * Sets whether the bridge's default engine caches resolved/compiled templates.
	 *
	 * <p>
	 * Typically defaults to {@code true} (production-safe); set {@code false} during development
	 * to pick up template edits without restarting the server. See the class-level Javadoc for the
	 * JSP-bridge caveat.
	 *
	 * @param value The cache flag.
	 * @return This object (for method chaining).
	 */
	B cacheTemplates(boolean value);
}
