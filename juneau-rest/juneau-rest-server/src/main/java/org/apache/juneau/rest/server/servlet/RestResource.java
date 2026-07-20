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
package org.apache.juneau.rest.server.servlet;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.commons.logging.Logger;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Identical to {@link RestServlet} but doesn't extend from {@link HttpServlet}.
 *
 * <p>
 * This is particularly useful in Spring Boot environments that auto-detect servlets to deploy in servlet containers,
 * but you want this resource to be deployed as a child instead.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClasses">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public abstract class RestResource {

	private AtomicReference<RestContext> context = new AtomicReference<>();

	/**
	 * The programmatic configuration builder stashed on this instance, or <jk>null</jk> when the
	 * resource was constructed without a builder.  Mutable so it can be written by either the
	 * {@link #RestResource(RestBuilder)} constructor or {@link Builder#build()}.  Read non-reflectively by
	 * {@link RestContext} during construction so builder-supplied values take precedence over {@code @Rest}
	 * annotation values.
	 */
	RestBuilder<?> restBuilder;

	/**
	 * Default constructor.
	 */
	protected RestResource() {}

	/**
	 * Builder-injection constructor.
	 *
	 * @param builder The programmatic configuration builder.  May be <jk>null</jk>.
	 */
	protected RestResource(RestBuilder<?> builder) {
		this.restBuilder = builder;
	}

	/**
	 * Returns the programmatic configuration builder stashed on this resource, or <jk>null</jk> if none.
	 *
	 * @return The stashed builder, or <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1452" // RestBuilder<?> wildcard return intentional; concrete builder type varies by REST resource class
	})
	public RestBuilder<?> getRestBuilder() {
		return restBuilder;
	}

	/**
	 * Creates a new fluent builder for programmatically configuring an instance of the specified resource type.
	 *
	 * @param <R> The resource type.
	 * @param type The resource type to build.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <R extends RestResource> DefaultBuilder<R> builder(Class<R> type) {
		return new DefaultBuilder<>(type);
	}

	/**
	 * Returns the current thread-local HTTP request.
	 *
	 * @return The current thread-local HTTP request, or <jk>null</jk> if it wasn't created.
	 */
	public synchronized RestRequest getRequest() { return getContext().getLocalSession().getOpSession().getRequest(); }

	/**
	 * Returns the current thread-local HTTP response.
	 *
	 * @return The current thread-local HTTP response, or <jk>null</jk> if it wasn't created.
	 */
	public synchronized RestResponse getResponse() { return getContext().getLocalSession().getOpSession().getResponse(); }

	/**
	 * Log a message.
	 *
	 * <p>
	 * Subclasses can intercept the handling of these messages by overriding {@link #doLog(Level, Throwable, Supplier)}.
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public void log(Level level, String msg, Object...args) {
		doLog(level, null, fs(msg, args));
	}

	/**
	 * Log a message.
	 *
	 * <p>
	 * Subclasses can intercept the handling of these messages by overriding {@link #doLog(Level, Throwable, Supplier)}.
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		doLog(level, cause, fs(msg, args));
	}

	/**
	 * Main logger method.
	 *
	 * <p>
	 * The default behavior logs a message to the Java logger of the class name.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own logger handling.
	 *
	 * @param level The log level.
	 * @param cause Optional throwable.
	 * @param msg The message to log.
	 */
	protected void doLog(Level level, Throwable cause, Supplier<String> msg) {
		var c = context.get();
		var logger = c == null ? null : c.getLogger();
		if (logger == null)
			logger = Logger.getLogger(cn(this));
		logger.log(level, cause, msg);
	}

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 *
	 * @return The context information on this servlet.
	 */
	protected RestContext getContext() {
		if (context.get() == null)
			throw new InternalServerError("RestContext object not set on resource.");
		return context.get();
	}

	/**
	 * Returns the runtime-overridden top-level mount paths for this resource.
	 *
	 * <p>
	 * Subclasses can override this method to substitute the {@code @Rest(paths)} annotation defaults at
	 * construction time. This is the &quot;getter&quot; rung in the runtime-override resolution chain
	 * documented on {@link RestContext#getPaths()}:
	 * <ol>
	 * 	<li>Programmatic ({@code RestContext.Builder.paths(String...)}) &mdash; highest precedence.
	 * 	<li>This {@code getPaths()} getter (when non-{@code null}).
	 * 	<li>{@code @Rest(paths={...})} annotation default (SVL-resolved per element, comma-split) &mdash;
	 * 		lowest precedence.
	 * </ol>
	 *
	 * <h5 class='section'>Accepted return shapes:</h5><ul>
	 * 	<li>{@code null} &mdash; no override; resolution falls through to the {@code @Rest(paths=...)}
	 * 		annotation default.
	 * 	<li>{@link String} &mdash; a single path or a comma-delimited list, e.g.
	 * 		{@code "/healthz,/readyz"}.
	 * 	<li>{@code String[]} &mdash; each element may itself be a comma-delimited list.
	 * 	<li>{@link java.util.Collection Collection}, {@link java.util.List List},
	 * 		{@link java.util.Set Set}, {@link Iterable}, or {@link java.util.stream.Stream Stream}
	 * 		of any of the above (nested mixes are recursively flattened).
	 * 	<li>Primitive arrays (boxed during flattening).
	 * </ul>
	 *
	 * <p>
	 * Non-{@link String} leaves are coerced via {@link String#valueOf(Object)}.  Each leaf string then runs
	 * through the same per-element pipeline used for {@code @Rest(paths=...)} annotation elements: SVL is
	 * applied via the bean-store-backed {@link org.apache.juneau.commons.svl.VarResolverSession VarResolverSession}
	 * (so {@code $C{key}} consults the live {@link org.apache.juneau.config.Config Config}, {@code $E{NAME,default}}
	 * and {@code $S{prop,default}} resolve through the bootstrap variable catalog), and the post-SVL value is
	 * comma-split with each piece trimmed and empty pieces dropped.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Returning {@code null} (the default) means &quot;inherit annotation&quot; &mdash; the next rung resolves.
	 * 	<li class='note'>
	 * 		Returning {@code new String[0]}, an empty collection, an empty string, or a blank string means
	 * 		&quot;explicitly clear&quot; &mdash; no top-level mounts.
	 * </ul>
	 *
	 * @return The runtime-overridden mount paths in any accepted shape, or {@code null} to inherit the
	 * 	annotation default.
	 * @since 10.0.0
	 */
	public Object getPaths() { return null; }

	/**
	 * Sets the context object for this servlet.
	 *
	 * @param context Sets the context object on this servlet.
	 * @throws ServletException If error occurred during post-initialiation.
	 */
	protected void setContext(RestContext context) throws ServletException {
		this.context.set(context);
	}

	/**
	 * Fluent builder for programmatically configuring a {@link RestResource} subclass.
	 *
	 * <p>
	 * Subclassable, self-typed (CRTP) flavor builder.  For the common (non-subclassed) case
	 * use {@link RestResource#builder(Class)} which returns the concrete {@link DefaultBuilder} leaf.
	 *
	 * @param <R> The resource type produced by {@link #build()}.
	 * @param <SELF> The concrete builder type (self type).
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public static class Builder<R extends RestResource, SELF extends Builder<R, SELF>> extends AbstractRestBuilder<R, SELF> {

		/**
		 * Constructor.
		 *
		 * @param type The resource type produced by {@link #build()}.  Must not be <jk>null</jk>.
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
	 * Concrete default leaf builder returned by {@link RestResource#builder(Class)} for the common (non-subclassed)
	 * case.
	 *
	 * @param <R> The resource type produced by {@link #build()}.
	 */
	public static final class DefaultBuilder<R extends RestResource> extends Builder<R, DefaultBuilder<R>> {
		DefaultBuilder(Class<R> type) {
			super(type);
		}
	}
}