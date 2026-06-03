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

import static jakarta.servlet.http.HttpServletResponse.*;
import static java.util.logging.Level.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.text.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.commons.logging.Logger;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Servlet implementation of a REST resource.
 *
 * <p>
 * 	The {@link RestServlet} class is the entry point for your REST resources.
 * 	It extends directly from <l>HttpServlet</l> and is deployed like any other servlet.
 * </p>
 * <p>
 * 	When the servlet <l>init()</l> method is called, it triggers the code to find and process the <l>@Rest</l>
 * 	annotations on that class and all child classes.
 * 	These get constructed into a {@link RestContext} object that holds all the configuration
 * 	information about your resource in a read-only object.
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Users will typically extend from {@link BasicRestServlet} or {@link BasicRestServletGroup}
 * 		instead of this class directly.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., MSG_servletInitError)
})
public abstract class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	// Error message constants
	private static final String MSG_servletInitError = "Servlet init error on class ''{0}''";

	private final AtomicReference<RestContext> context = new AtomicReference<>();
	private final AtomicReference<Exception> initException = new AtomicReference<>();

	/**
	 * The programmatic configuration builder stashed on this instance, or <jk>null</jk> when the
	 * resource was constructed without a builder.  Mutable so it can be written by either the
	 * {@link #RestServlet(RestBuilder<?>)} constructor or {@link Builder#build()} (the no-arg-only setter-stash path).
	 * Read non-reflectively by {@link RestContext} during construction so builder-supplied values take precedence
	 * over {@link Rest @Rest} annotation values.
	 */
	RestBuilder<?> restBuilder;

	/**
	 * Default constructor.
	 */
	protected RestServlet() {}

	/**
	 * Builder-injection constructor.
	 *
	 * @param builder The programmatic configuration builder.  May be <jk>null</jk>.
	 */
	protected RestServlet(RestBuilder<?> builder) {
		this.restBuilder = builder;
	}

	/**
	 * Returns the programmatic configuration builder stashed on this resource, or <jk>null</jk> if none.
	 *
	 * @return The stashed builder, or <jk>null</jk>.
	 * @since 9.5.0
	 */
	public RestBuilder<?> getRestBuilder() {
		return restBuilder;
	}

	/**
	 * Creates a new fluent builder for programmatically configuring an instance of the specified resource type.
	 *
	 * <p>
	 * Builder-supplied values take precedence over the resource class's own {@link Rest @Rest} annotation values.
	 *
	 * @param <R> The resource type.
	 * @param type The resource type to build.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 * @since 9.5.0
	 */
	public static <R extends RestServlet> DefaultBuilder<R> builder(Class<R> type) {
		return new DefaultBuilder<>(type);
	}

	@Override /* Overridden from GenericServlet */
	public synchronized void destroy() {
		if (nn(context.get()))
			context.get().destroy();
		super.destroy();
	}

	/**
	 * Returns the read-only context object that contains all the configuration information about this resource.
	 *
	 * <p>
	 * This object is <jk>null</jk> during the call to {@link #init(ServletConfig)} but is populated by the time
	 * {@link #init()} is called.
	 *
	 * <p>
	 * Resource classes that don't extend from {@link RestServlet} can add the following method to their class to get
	 * access to this context object:
	 * <p class='bjava'>
	 * 	<jk>public void</jk> init(RestServletContext <jv>context</jv>) <jk>throws</jk> Exception;
	 * </p>
	 *
	 * @return The context information on this servlet.
	 */
	public synchronized RestContext getContext() {
		var rc = context.get();
		if (rc == null)
			throw new InternalServerError("RestContext object not set on resource.");
		return rc;
	}

	/**
	 * Returns the path for this resource as defined by the @Rest(path) annotation or RestContext.Builder.path(String) method
	 * concatenated with those on all parent classes.
	 *
	 * @return The path defined on this servlet, or an empty string if not specified.
	 */
	public synchronized String getPath() {
		var context2 = this.context.get();
		if (nn(context2))
			return context2.getFullPath();
		var ci = ClassInfo.of(getClass());
		// @formatter:off
		return rstream(AP.find(Rest.class, ci))
			.map(x -> x.inner().path())
			.filter(Utils::ne)
			.map(StringUtils::trimSlashes)
			.findFirst()
			.orElse("");
		// @formatter:on
	}

	/**
	 * Returns the runtime-overridden top-level mount paths for this resource.
	 *
	 * <p>
	 * Subclasses can override this method to substitute the {@link Rest#paths() @Rest(paths)} annotation
	 * defaults at construction time. This is the &quot;getter&quot; rung in the runtime-override resolution
	 * chain documented on {@link RestContext#getPaths()}:
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
	 * 	<li class='note'>
	 * 		The scalar accessor {@link #getPath()} remains orthogonal to this method &mdash; the singular
	 * 		{@code @Rest(path=...)} attribute has its own resolution chain and is unaffected.
	 * </ul>
	 *
	 * @return The runtime-overridden mount paths in any accepted shape, or {@code null} to inherit the
	 * 	annotation default.
	 * @since 9.5.0
	 */
	public Object getPaths() { return null; }

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

	@Override /* Overridden from Servlet */
	public synchronized void init(ServletConfig servletConfig) throws ServletException {
		try {
			if (nn(context.get()))
				return;
			super.init(servletConfig);
			context.set(new RestContext(new RestContext.Args(this.getClass(), null, servletConfig, () -> this, "", null, null, null, false, restBuilder)));
			context.get().postInit();
			context.get().postInitChildFirst();
		} catch (ServletException e) {
			initException.set(e);
			log(SEVERE, e, MSG_servletInitError, cn(this));
			throw e;
		} catch (BasicHttpException e) {
			initException.set(e);
			log(SEVERE, e, MSG_servletInitError, cn(this));
		} catch (Exception e) {
			initException.set(new InternalServerError(e));
			log(SEVERE, e, MSG_servletInitError, cn(this));
		}
	}

	/**
	 * Log a message.
	 *
	 * <p>
	 * Subclasses can intercept the handling of these messages by overriding {@link #doLog(Level, Throwable, Supplier)}.
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
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
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		doLog(level, cause, fs(msg, args));
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 */
	@Override /* Overridden from Servlet */
	public void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, InternalServerError, IOException {
		try {
			if (nn(initException.get()))
				throw initException.get();
			if (context.get() == null)
				throw new InternalServerError(
					"Servlet {0} not initialized.  init(ServletConfig) was not called.  This can occur if you've overridden this method but didn't call super.init(RestConfig).", cn(this));
			getContext().execute(this, r1, r2);

		} catch (Exception e) {
			r2.sendError(SC_INTERNAL_SERVER_ERROR, lm(e));
		}
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
	 * Sets the context object for this servlet.
	 *
	 * <p>
	 * This method is effectively a no-op if {@link #init(ServletConfig)} has already been called.
	 *
	 * @param context Sets the context object on this servlet.
	 * @throws ServletException If error occurred during initialization.
	 */
	protected synchronized void setContext(RestContext context) throws ServletException {
		if (this.context.get() == null) {
			super.init(context.getBuilder());
			this.context.set(context);
		}
	}

	/**
	 * Fluent builder for programmatically configuring a {@link RestServlet} subclass.
	 *
	 * <p>
	 * This is the subclassable, self-typed (CRTP) flavor builder.  Its {@code SELF} type parameter is left open so
	 * a user subclass's bespoke setters chain with true covariant returns alongside the inherited
	 * {@link RestBuilder} surface.  For the common case where the builder is not subclassed,
	 * use {@link RestServlet#builder(Class)} which returns the concrete {@link DefaultBuilder} leaf.
	 *
	 * @param <R> The resource type produced by {@link #build()}.
	 * @param <SELF> The concrete builder type (self type).
	 * @since 9.5.0
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public static class Builder<R extends RestServlet, SELF extends Builder<R, SELF>> extends AbstractRestBuilder<R, SELF> {

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
	 * Concrete default leaf builder returned by {@link RestServlet#builder(Class)} for the common (non-subclassed)
	 * case, so callers are not forced to spell the {@code SELF} type parameter.
	 *
	 * @param <R> The resource type produced by {@link #build()}.
	 * @since 9.5.0
	 */
	public static final class DefaultBuilder<R extends RestServlet> extends Builder<R, DefaultBuilder<R>> {
		DefaultBuilder(Class<R> type) {
			super(type);
		}
	}
}