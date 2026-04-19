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
package org.apache.juneau.rest;

import static java.util.Collections.emptyList;
import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.ServletConfig;

import org.apache.juneau.cp.BasicBeanStore;

/**
 * Bootstrap arguments for {@link RestContext}.
 *
 * <p>
 * Bundles the small, fixed set of inputs needed to construct a {@link RestContext} into a single immutable record,
 * replacing the chained {@link RestContext.Builder#init(Supplier)} / {@link RestContext.Builder#path(String)} /
 * {@link RestContext.Builder#children(Object...)} fluent calls of the legacy builder factory.
 *
 * <p>
 * All non-required values default sensibly:
 * <ul>
 * 	<li>{@code parentContext}, {@code servletConfig} — {@code null} (top-level resource, no servlet container)
 * 	<li>{@code path} — {@code ""} (no path prefix)
 * 	<li>{@code children} — empty list (no programmatic child resources)
 * 	<li>{@code beanStoreConfigurer} — no-op (no pre-build bean-store mutation)
 * </ul>
 *
 * <p>
 * The {@code beanStoreConfigurer} hook gives test fixtures and integration code (mock REST clients, etc.) a chance
 * to register beans on the {@link BasicBeanStore} *after* the resource has been wired in but *before* the
 * {@code findXxx()} memoizers fire. This is the post-builder replacement for the legacy pattern
 * {@code RestContext.create(...).init(...).beanStore(X.class, x).beanStore().addBeanType(Y.class, Yimpl.class)
 * .build()}.
 *
 * <p>
 * Required values:
 * <ul>
 * 	<li>{@code resourceClass} — the {@link Rest @Rest}-annotated REST resource type
 * 	<li>{@code resource} — the supplier that yields the resource instance during {@code init()}
 * </ul>
 *
 * <h5 class='section'>Note on naming:</h5>
 * <p>
 * Decision #27 in {@code TODO-16} originally settled on the {@code XArgs} suffix for ctor-arg-bundle records, but
 * {@code RestContextArgs} is already taken by the {@link org.apache.juneau.rest.arg.RestContextArgs} parameter
 * resolver in the {@link org.apache.juneau.rest.arg} package (one of an entire family of {@code *Args}
 * {@link org.apache.juneau.rest.annotation.RestOp @RestOp}-method arg-resolver classes:
 * {@link org.apache.juneau.rest.arg.RestSessionArgs RestSessionArgs},
 * {@link org.apache.juneau.rest.arg.RestOpContextArgs RestOpContextArgs}, etc.). The {@code Init} suffix avoids the
 * collision and reads naturally — this record consolidates the legacy {@link RestContext.Builder#init(Supplier)}
 * fluent method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Top-level resource, no servlet config, no children, no path prefix.</jc>
 * 	<jk>var</jk> <jv>ctx</jv> = <jk>new</jk> RestContext(<jk>new</jk> RestContextInit(MyResource.<jk>class</jk>, () -&gt; <jk>new</jk> MyResource()));
 *
 * 	<jc>// Child resource registered against a parent context with a path prefix.</jc>
 * 	<jk>var</jk> <jv>child</jv> = <jk>new</jk> RestContext(<jk>new</jk> RestContextInit(
 * 		MyChild.<jk>class</jk>, <jv>parent</jv>, <jk>null</jk>, () -&gt; <jk>new</jk> MyChild(), <js>"/child"</js>, List.<jsm>of</jsm>()));
 * </p>
 *
 * @param resourceClass The {@link Rest @Rest}-annotated REST resource class. Must not be {@code null}.
 * @param parentContext The parent {@link RestContext}, or {@code null} if this is a top-level resource.
 * @param servletConfig The {@link ServletConfig} from the servlet container, or {@code null} when none is available.
 * @param resource The supplier that provides the resource instance during initialization. Must not be {@code null}.
 * @param path The path prefix relative to the parent. Defaults to {@code ""}.
 * @param children Programmatically-registered child resources. Defaults to an empty list.
 * @param beanStoreConfigurer A pre-build hook that runs against the resolved {@link BasicBeanStore} after the
 * 	resource has been wired in but before any {@code findXxx()} memoizer fires. Defaults to a no-op. Use this to
 * 	register typed-class bindings (via {@link BasicBeanStore#addBeanType(Class, Class) addBeanType}) or instance
 * 	beans (via {@link BasicBeanStore#addBean(Class, Object) addBean}) that need to be visible to the memoizers.
 *
 * @since 9.2.1
 */
public record RestContextInit(
	Class<?> resourceClass,
	RestContext parentContext,
	ServletConfig servletConfig,
	Supplier<?> resource,
	String path,
	List<Object> children,
	Consumer<BasicBeanStore> beanStoreConfigurer
) {

	/**
	 * Compact canonical constructor — null-coalesces optional fields and validates the required ones.
	 *
	 * @param resourceClass The REST resource class. Must not be <jk>null</jk>.
	 * @param parentContext Optional parent context.
	 * @param servletConfig Optional servlet config.
	 * @param resource The resource supplier. Must not be <jk>null</jk>.
	 * @param path Optional path prefix. {@code null} is normalized to {@code ""}.
	 * @param children Optional child list. {@code null} is normalized to an empty list.
	 * @param beanStoreConfigurer Optional pre-build bean-store hook. {@code null} is normalized to a no-op.
	 */
	public RestContextInit {
		assertArgNotNull("resourceClass", resourceClass);
		assertArgNotNull("resource", resource);
		if (path == null)
			path = "";
		if (children == null)
			children = emptyList();
		if (beanStoreConfigurer == null)
			beanStoreConfigurer = bs -> {};
	}

	/**
	 * Backward-compat constructor — same as the canonical constructor but without the {@code beanStoreConfigurer} hook.
	 *
	 * @param resourceClass The REST resource class. Must not be <jk>null</jk>.
	 * @param parentContext Optional parent context.
	 * @param servletConfig Optional servlet config.
	 * @param resource The resource supplier. Must not be <jk>null</jk>.
	 * @param path Optional path prefix. {@code null} is normalized to {@code ""}.
	 * @param children Optional child list. {@code null} is normalized to an empty list.
	 */
	public RestContextInit(Class<?> resourceClass, RestContext parentContext, ServletConfig servletConfig,
			Supplier<?> resource, String path, List<Object> children) {
		this(resourceClass, parentContext, servletConfig, resource, path, children, null);
	}

	/**
	 * Convenience constructor for a top-level resource with no servlet config, no path prefix, and no programmatic children.
	 *
	 * @param resourceClass The REST resource class. Must not be <jk>null</jk>.
	 * @param resource The resource supplier. Must not be <jk>null</jk>.
	 */
	public RestContextInit(Class<?> resourceClass, Supplier<?> resource) {
		this(resourceClass, null, null, resource, "", emptyList(), null);
	}

	/**
	 * Convenience constructor for a top-level resource with a pre-build bean-store configurer.
	 *
	 * @param resourceClass The REST resource class. Must not be <jk>null</jk>.
	 * @param resource The resource supplier. Must not be <jk>null</jk>.
	 * @param beanStoreConfigurer Pre-build bean-store hook.
	 */
	public RestContextInit(Class<?> resourceClass, Supplier<?> resource, Consumer<BasicBeanStore> beanStoreConfigurer) {
		this(resourceClass, null, null, resource, "", emptyList(), beanStoreConfigurer);
	}
}
