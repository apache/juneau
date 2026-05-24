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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

import jakarta.servlet.*;

/**
 * Implements the child resources of a {@link Rest}-annotated class.
 *
 * <p>
 * Holds a registry of {@link RestContext} instances keyed by {@link RestContext#getPath() composed path}.
 * Reads (request routing via {@link #findMatch(RestSession.Builder)} and the {@link #asMap()} accessor) are
 * lock-free against a volatile copy-on-write snapshot; mutations
 * ({@link #addChild(Class) addChild} / {@link #removeChild(String) removeChild}) synchronize on an internal
 * write lock and atomically replace the snapshot. This makes runtime child management safe even while
 * requests are in flight on other threads.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public class RestChildren {

	/**
	 * Builder class.
	 */
	public static class Builder {

		final RestContext parent;
		final BeanStore beanStore;
		final ServletConfig servletConfig;
		final List<RestContext> list;

		/**
		 * Constructor.
		 *
		 * @param parent The parent {@link RestContext} that owns these children. Used for parent-context wiring
		 * 	on dynamically added children. May be {@code null} when constructing from contexts already built
		 * 	externally and runtime add/remove is not required.
		 * @param beanStore The bean store used for dependency injection on dynamically added children. Must not be
		 * 	{@code null}.
		 * @param servletConfig The {@link ServletConfig} passed through to dynamically added child contexts.
		 * 	May be {@code null}.
		 */
		protected Builder(RestContext parent, BeanStore beanStore, ServletConfig servletConfig) {
			this.parent = parent;
			this.beanStore = beanStore;
			this.servletConfig = servletConfig;
			this.list = list();
		}

		/**
		 * Returns the bean store used by this builder.
		 *
		 * @return The bean store used by this builder.
		 */
		public BeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Adds a child resource to this builder.
		 *
		 * @param value The REST context of the child resource.
		 * @return This object.
		 */
		public Builder add(RestContext value) {
			list.add(value);
			return this;
		}

		/**
		 * Builds the children.
		 *
		 * @return A new {@link RestChildren}.
		 */
		public RestChildren build() {
			return new RestChildren(this);
		}
	}

	/**
	 * Static creator.
	 *
	 * @param parent The parent {@link RestContext}. Required for runtime {@link #addChild(Class) addChild} support
	 * 	(may be {@code null} otherwise).
	 * @param beanStore The bean store used for child instantiation.
	 * @param servletConfig The {@link ServletConfig} propagated to dynamically added children. May be {@code null}.
	 * @return A new builder for this object.
	 */
	public static Builder create(RestContext parent, BeanStore beanStore, ServletConfig servletConfig) {
		return new Builder(parent, beanStore, servletConfig);
	}

	private final RestContext parent;
	private final BeanStore beanStore;
	private final ServletConfig servletConfig;
	private final Object writeLock = new Object();

	private volatile Map<String,RestContext> children;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestChildren(Builder builder) {
		this.parent = builder.parent;
		this.beanStore = builder.beanStore;
		this.servletConfig = builder.servletConfig;
		var initial = new LinkedHashMap<String,RestContext>();
		for (var rc : builder.list)
			initial.put(rc.getPath(), rc);
		this.children = Collections.unmodifiableMap(initial);
	}

	/**
	 * Returns the children in this object as a map.
	 *
	 * <p>
	 * The keys are the {@link RestContext#getPath() paths} of the child contexts. The returned map is an
	 * unmodifiable snapshot taken at the time of the call; subsequent {@link #addChild(Class) addChild} /
	 * {@link #removeChild(String) removeChild} calls do not affect this view.
	 *
	 * @return The children as an unmodifiable map.
	 */
	public Map<String,RestContext> asMap() {
		return children;
	}

	/**
	 * Called during servlet destruction on all children to invoke all {@link RestDestroy} and {@link Servlet#destroy()} methods.
	 */
	public void destroy() {
		for (var r : children.values())
			destroyChild(r);
	}

	/**
	 * Looks through the registered children of this object and returns the best match.
	 *
	 * @param builder The HTTP call builder.
	 * @return The child that best matches the call, or an empty {@link Optional} if a match could not be made.
	 */
	public Optional<RestChildMatch> findMatch(RestSession.Builder builder) {
		var snapshot = children;  // single volatile read; consistent for the rest of the method
		var pi = builder.getPathInfoUndecoded();
		if ((! snapshot.isEmpty()) && nn(pi) && ! pi.equals("/")) {
			for (var rc : snapshot.values()) {
				UrlPathMatcher upp = rc.getPathMatcher();
				UrlPathMatch uppm = upp.match(builder.getUrlPath());
				if (nn(uppm)) {
					return opt(RestChildMatch.create(uppm, rc));
				}
			}
		}
		return opte();
	}

	/**
	 * Called during servlet initialization on all children to invoke all {@link RestPostInit} child-last methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInit() throws ServletException {
		for (var childContext : children.values())
			childContext.postInit();
	}

	/**
	 * Called during servlet initialization on all children to invoke all {@link RestPostInit} child-first methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInitChildFirst() throws ServletException {
		for (var childContext : children.values())
			childContext.postInitChildFirst();
	}

	//-------------------------------------------------------------------------------------------------------------
	// Dynamic add/remove API.
	//
	// Routing reads (findMatch / asMap) are lock-free against the volatile `children` snapshot.
	// Mutations synchronize on writeLock and atomically swap in a new unmodifiable LinkedHashMap.
	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Dynamically registers a child REST resource by class, instantiating it via the parent's {@link BeanStore}.
	 *
	 * <p>
	 * The path under which the child is registered is determined by the {@link Rest#path() @Rest(path)} annotation
	 * on the resource class composed against the parent's full path.
	 *
	 * <p>
	 * The new child's {@link RestContext#postInit()} and {@link RestContext#postInitChildFirst()} lifecycle hooks
	 * are invoked before this method returns, mirroring the eager-init behavior of {@code @Rest(children = ...)}.
	 *
	 * @param resourceClass The {@code @Rest}-annotated resource class.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 * @throws IllegalStateException If a child is already registered at the resolved path.
	 */
	public RestContext addChild(Class<?> resourceClass) throws ServletException {
		return addChildInternal(resourceClass, null, "", false);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource.
	 *
	 * <p>
	 * The path under which the child is registered is determined by the {@link Rest#path() @Rest(path)} annotation
	 * on the resource's class composed against the parent's full path.
	 *
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 * @throws IllegalStateException If a child is already registered at the resolved path.
	 */
	public RestContext addChild(Object resource) throws ServletException {
		return addChildInternal(resource.getClass(), resource, "", false);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource, optionally replacing any existing child at the
	 * same resolved path.
	 *
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @param replace If {@code true}, an existing child at the same path is destroyed and removed before the new
	 * 	child is added. If {@code false}, an existing child causes an {@link IllegalStateException}.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 */
	public RestContext addChild(Object resource, boolean replace) throws ServletException {
		return addChildInternal(resource.getClass(), resource, "", replace);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource at an explicit path.
	 *
	 * <p>
	 * The supplied {@code path} overrides whatever {@link Rest#path() @Rest(path)} would normally provide. Useful
	 * for mounting the same servlet class at multiple paths, or for test fixtures that compose paths programmatically.
	 *
	 * @param path The path segment under the parent at which to mount this child. Leading slashes are trimmed.
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 * @throws IllegalStateException If a child is already registered at the resolved path.
	 */
	public RestContext addChild(String path, Object resource) throws ServletException {
		return addChildInternal(resource.getClass(), resource, path, false);
	}

	/**
	 * Dynamically registers a pre-instantiated child REST resource at an explicit path, optionally replacing any
	 * existing child at the same path.
	 *
	 * @param path The path segment under the parent at which to mount this child. Leading slashes are trimmed.
	 * @param resource The {@code @Rest}-annotated resource instance.
	 * @param replace If {@code true}, an existing child at the same path is destroyed and removed before the new
	 * 	child is added. If {@code false}, an existing child causes an {@link IllegalStateException}.
	 * @return The newly-built child {@link RestContext}.
	 * @throws ServletException If construction or lifecycle initialization of the child fails.
	 */
	public RestContext addChild(String path, Object resource, boolean replace) throws ServletException {
		return addChildInternal(resource.getClass(), resource, path, replace);
	}

	/**
	 * Removes the child registered at the given composed path.
	 *
	 * <p>
	 * The removed child has {@link RestContext#destroy()} invoked (which runs {@code @RestDestroy} hooks and
	 * recursively destroys any grandchildren), followed by {@link Servlet#destroy()} if the underlying resource is
	 * a {@link Servlet}.
	 *
	 * @param path The composed path key (as returned by {@link RestContext#getPath()}) of the child to remove.
	 * @return The removed and destroyed {@link RestContext}, or {@code null} if no child was registered at the
	 * 	given path.
	 */
	public RestContext removeChild(String path) {
		RestContext removed;
		synchronized (writeLock) {
			removed = children.get(path);
			if (removed == null)
				return null;
			var next = new LinkedHashMap<>(children);
			next.remove(path);
			children = Collections.unmodifiableMap(next);
		}
		destroyChild(removed);
		return removed;
	}

	/**
	 * Removes the first child whose resource class matches the given type.
	 *
	 * @param resourceClass The resource class to match. Matching is performed via {@link RestContext#getResourceClass()}.
	 * @return The removed and destroyed {@link RestContext}, or {@code null} if no matching child was found.
	 */
	public RestContext removeChild(Class<?> resourceClass) {
		String key = null;
		RestContext removed = null;
		synchronized (writeLock) {
			for (var e : children.entrySet()) {
				if (e.getValue().getResourceClass() == resourceClass) {
					key = e.getKey();
					removed = e.getValue();
					break;
				}
			}
			if (removed == null)
				return null;
			var next = new LinkedHashMap<>(children);
			next.remove(key);
			children = Collections.unmodifiableMap(next);
		}
		destroyChild(removed);
		return removed;
	}

	//-------------------------------------------------------------------------------------------------------------
	// Internal helpers.
	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Shared child-context construction recipe — used both by the eager init memoizer in {@link RestContext} and by
	 * the runtime {@link #addChild(Class) addChild} family of methods.
	 *
	 * <p>
	 * Resolves a {@link Supplier} for the resource (prefers an existing bean of the same class in the bean store,
	 * otherwise instantiates via {@link BeanInstantiator}), constructs the child {@link RestContext} using the
	 * supplied {@code parent} / {@code servletConfig} / {@code pathOverride}, and invokes any
	 * {@code setContext(RestContext)} method on the resource via reflection (so that {@code RestServlet}-style
	 * resources receive their context handle).
	 *
	 * @param parent The parent {@link RestContext}. Must not be {@code null}.
	 * @param beanStore The bean store to resolve instances and dependencies from. Must not be {@code null}.
	 * @param servletConfig The {@link ServletConfig} to pass through to the child. May be {@code null}.
	 * @param resourceClass The resource class. Must not be {@code null}.
	 * @param resourceInstance A pre-supplied instance of the resource. If {@code null}, the instance is resolved
	 * 	from the bean store or freshly instantiated via {@link BeanInstantiator}.
	 * @param pathOverride An explicit path segment (relative to the parent). Use {@code ""} to read the path from
	 * 	{@link Rest#path() @Rest(path)} on the resource class.
	 * @return The newly-built child {@link RestContext}, with its {@code setContext} method (if any) invoked.
	 * @throws Exception If construction or reflective wiring fails.
	 */
	static RestContext buildChildContext(
			RestContext parent,
			BeanStore beanStore,
			ServletConfig servletConfig,
			Class<?> resourceClass,
			Object resourceInstance,
			String pathOverride) throws Exception {
		Supplier<?> so;
		if (resourceInstance != null) {
			final Object r = resourceInstance;
			so = () -> r;
		} else if (beanStore.getBean(resourceClass).isPresent()) {
			so = () -> beanStore.getBean(resourceClass).get();
		} else {
			Object o = BeanInstantiator.of(resourceClass, beanStore).run();
			so = () -> o;
		}
		var cc = new RestContext(new RestContext.Args(resourceClass, parent, servletConfig, so, pathOverride, null, null, null, false));
		var mi = ClassInfo.of(so.get())
			.getMethod(x -> x.hasName("setContext") && x.hasParameterTypes(RestContext.class))
			.orElse(null);
		if (nn(mi))
			mi.accessible().invoke(so.get(), cc);
		return cc;
	}

	private RestContext addChildInternal(Class<?> resourceClass, Object resourceInstance, String pathOverride, boolean replace) throws ServletException {
		if (parent == null || beanStore == null)
			throw illegalState("Cannot add a child to a RestChildren that was not initialized with a parent RestContext.");
		RestContext cc;
		try {
			cc = buildChildContext(parent, beanStore, servletConfig, resourceClass, resourceInstance, pathOverride);
		} catch (Exception e) {
			throw new ServletException("Failed to build child REST context for " + resourceClass.getName(), unwrapThrowable(e));
		}
		var key = cc.getPath();
		RestContext replaced = null;
		synchronized (writeLock) {
			if (children.containsKey(key)) {
				if (! replace) {
					destroyQuietly(cc);
					throw illegalState("Child resource already registered at path ''{0}''.", key);
				}
				replaced = children.get(key);
				var withoutExisting = new LinkedHashMap<>(children);
				withoutExisting.remove(key);
				// We don't publish the intermediate "without existing" snapshot — single atomic swap below.
				withoutExisting.put(key, cc);
				children = Collections.unmodifiableMap(withoutExisting);
			} else {
				var next = new LinkedHashMap<>(children);
				next.put(key, cc);
				children = Collections.unmodifiableMap(next);
			}
		}
		if (replaced != null)
			destroyChild(replaced);
		cc.postInit();
		cc.postInitChildFirst();
		return cc;
	}

	private static void destroyChild(RestContext child) {
		// Servlet-backed resources delegate Servlet.destroy() through to RestContext.destroy() (see RestServlet.destroy),
		// so we must avoid double-destruction in that case — call Servlet.destroy() and let it transitively call
		// RestContext.destroy() exactly once.
		if (child.getResource() instanceof Servlet s)
			s.destroy();
		else
			child.destroy();
	}

	private static void destroyQuietly(RestContext child) {
		try {
			destroyChild(child);
		} catch (Exception ignored) {
			// best-effort cleanup of a child we're about to throw away
		}
	}

	private static Throwable unwrapThrowable(Throwable t) {
		if (t instanceof InvocationTargetException t2)
			return t2.getTargetException();
		return t;
	}
}
