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
package org.apache.juneau.microservice.jetty;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

import jakarta.servlet.DispatcherType;

import org.apache.juneau.commons.inject.BeanStore;
import org.apache.juneau.commons.inject.Value;
import org.apache.juneau.commons.reflect.ClassInfo;
import org.apache.juneau.config.event.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.auth.*;
import org.apache.juneau.rest.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;

import jakarta.servlet.*;

/**
 * Drives the Jetty server lifecycle on behalf of the microservice via the {@link MicroserviceListener} fan-out.
 *
 * <p>
 * Contributed by {@link JettyConfiguration} as a <c>@Bean</c>.  The microservice's {@link Microservice#start()}
 * call invokes {@link #onStart(Microservice)}, which:
 * <ol>
 * 	<li>Picks the first available port from the resolved {@link JettySettings#getPorts() ports} (falling back to
 * 		<c>Jetty/port</c> config, then <c>Jetty-Port</c> manifest, then <c>8000</c>).
 * 	<li>Resolves the Jetty {@link Server} — preferring an externally-supplied <c>@Bean Server</c>, otherwise
 * 		building one from <c>jetty.xml</c> via the {@link JettyServerFactory}.
 * 	<li>Mounts servlets from {@code Jetty/servlets}, {@code Jetty/servletMap}, {@code Jetty/servletAttributes}
 * 		config sections, plus auto-mounts every {@code @Bean Servlet} whose runtime class is annotated with
 * 		{@link Rest @Rest} (at {@link Rest#path()}, or <c>"/"</c> when no path is set).
 * 	<li>Starts the server and publishes the bound port via the {@code availablePort} / {@code juneau.serverPort}
 * 		system properties.
 * </ol>
 *
 * <p>
 * {@link #onStop(Microservice)} performs the inverse: stops the server gracefully in a worker thread.
 *
 * <p>
 * External callers reach this component via
 * {@code Microservice.getInstance().getBeanStore().getBean(JettyServerComponent.class).orElseThrow()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJettyBasics">juneau-microservice-jetty Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., KEY_SERVLET_CONTEXT_HANDLER)
})
public class JettyServerComponent implements MicroserviceListener {

	private static final String KEY_SERVLET_CONTEXT_HANDLER = "ServletContextHandler";
	private static final Random RANDOM = new Random();

	private final Messages messages = Messages.of(JettyServerComponent.class);
	private final AtomicReference<Server> server = new AtomicReference<>();
	private final AtomicReference<Microservice> microservice = new AtomicReference<>();

	/**
	 * Env-driven sentinel for {@code availablePort}; {@link Optional#empty()} when unset (in which case
	 * {@link #onStart(Microservice)} publishes the bound port back as the {@code availablePort} system property).
	 */
	@Value("${availablePort}")
	Optional<String> availablePortEnv = opte();

	/**
	 * Env-driven sentinel for {@code juneau.serverPort}; {@link Optional#empty()} when unset (in which case
	 * {@link #onStart(Microservice)} publishes the bound port back as the {@code juneau.serverPort} system property).
	 */
	@Value("${juneau.serverPort}")
	Optional<String> serverPortEnv = opte();

	private static int[] parseIntArray(String csv) {
		if (csv == null || csv.isEmpty())
			return new int[0];
		var parts = csv.split(",");
		var out = new int[parts.length];
		for (var i = 0; i < parts.length; i++)
			out[i] = Integer.parseInt(parts[i].trim());
		return out;
	}

	private static int findOpenPort(int[] ports) {
		for (var port : ports) {
			if (port == 0)
				port = RANDOM.nextInt(32767 - ports[0] + 1) + ports[0];
			try (var ss = new ServerSocket(port)) {
				return port;
			} catch (@SuppressWarnings("unused") IOException e) {
				// Port is in use, try next port in array
			}
		}
		return 0;
	}

	private static String normalizePathSpec(String rawPath) {
		var p = rawPath == null ? "" : rawPath;
		if (p.isEmpty() || "/".equals(p))
			return "/*";
		if (! p.startsWith("/"))
			p = "/" + p;
		if (! p.endsWith("/*"))
			p = trimTrailingSlashes(p) + "/*";
		return p;
	}

	private static String normalizeExactPathSpec(String rawPath) {
		var p = rawPath == null ? "" : trimTrailingSlashes(rawPath);
		if (p.isEmpty())
			return "/";
		if (! p.startsWith("/"))
			p = "/" + p;
		return p;
	}

	/**
	 * Resolves the top-level Jetty mount path-specs for an auto-discovered {@code @Rest}-annotated servlet.
	 *
	 * <p>
	 * Delegates to {@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore)} so the runtime-override
	 * resolution chain (programmatic &gt; {@code getPaths()} getter &gt; annotation default) is honored at
	 * mount time. The programmatic Builder rung is N/A for Jetty auto-discovery (the {@code RestContext} does
	 * not exist yet at this point in the lifecycle); users can substitute by overriding {@code getPaths()} on
	 * the resource class, or by leaning on SVL inside {@code @Rest(paths=...)} elements (e.g.
	 * {@code paths={"$C{health.paths}"}}) so each element is resolved through {@code Config} /
	 * {@code $E{...}} / {@code $S{...}} before being comma-split.
	 *
	 * @return The normalized exact-match path-specs (for multi-mount), or the singular {@code @Rest(path)}-style
	 * 	{@code "/...&#47;*"} pathspec when no {@code paths} are resolved.
	 */
	private static String[] restPathsFor(Servlet servlet, BeanStore store) {
		var cls = servlet.getClass();
		var r = cls.getAnnotation(Rest.class);
		if (r == null)
			return new String[] {"/*"};
		var resolved = RestContext.resolveTopLevelPaths(cls, servlet, store);
		if (resolved.length > 0)
			return Arrays.stream(resolved).map(JettyServerComponent::normalizeExactPathSpec).toArray(String[]::new);
		return new String[] {normalizePathSpec(r.path())};
	}

	/**
	 * Constructor.
	 */
	@SuppressWarnings({
		"java:S1186" // Empty constructor; all state is initialized via onStart(Microservice) when wired through the bean store.
	})
	public JettyServerComponent() {}

	@Override /* Overridden from MicroserviceListener */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for server creation logic
		"java:S1141", // Nested try blocks scope checked-exception translation tightly to single call sites
		"java:S6541", // Brain Method acceptable for server startup wiring logic
		"resource"    // ms.getBeanStore() is owned by the microservice lifecycle; do not close here.
	})
	public void onStart(Microservice ms) {
		try {
			microservice.set(ms);
			var store = ms.getBeanStore();
			ClassInfo.of(this).inject(this, store);
			var cf = ms.getConfig();
			var mf = ms.getManifest();
			var vr = ms.getVarResolver();
			var settings = store.getBean(JettySettings.class).orElseGet(() -> JettySettings.create().build());

			// Port resolution: settings > Jetty/port config > Jetty-Port manifest > 8000
			var ports = settings.getPorts();
			if (ports == null)
				ports = cf.get("Jetty/port").as(int[].class)
					.orElseGet(() -> mf.get("Jetty-Port").map(JettyServerComponent::parseIntArray).orElseGet(() -> ints(8000)));
			var availablePort = findOpenPort(ports);

			if (availablePortEnv.isEmpty())
				System.setProperty("availablePort", String.valueOf(availablePort));

			// Prefer a @Bean-supplied Server, else build one from jetty.xml.
			var injectedServer = store.getBean(Server.class).orElse(null);
			if (nn(injectedServer)) {
				server.set(injectedServer);
			} else {
				var factory = store.getBean(JettyServerFactory.class).orElseGet(BasicJettyServerFactory::new);
				var jettyXml = settings.getJettyXml();
				var jettyConfig = cf.get("Jetty/config").orElseGet(() -> mf.get("Jetty-Config").orElse("jetty.xml"));
				var resolveVars = firstNonNull(settings.getJettyXmlResolveVars(), cf.get("Jetty/resolveVars").asBoolean().orElse(false));
				boolean resolveVars2 = isTrue(resolveVars);

				if (jettyXml == null)
					jettyXml = loadSystemResourceAsString("jetty.xml", ".", "files");
				if (jettyXml == null)
					throw rex("jetty.xml file ''{0}'' was not found on the file system or classpath.", jettyConfig);

				if (resolveVars2)
					jettyXml = vr.resolve(jettyXml);

				ms.getLogger().info(jettyXml);

				try {
					server.set(factory.create(jettyXml));
				} catch (Exception e) {
					throw toRex(e);
				}
			}

			// Publish back to the store for downstream beans / lookups.
			store.addBean(Server.class, server.get());

			// Track each servlet pathSpec with its declaring source so we can fail loudly on collisions.
			var mountedPaths = new LinkedHashMap<String,String>();

			for (var s : cf.get("Jetty/servlets").asStringArray().orElse(new String[0])) {
				try {
					var c = info(Class.forName(s));
					if (c.isAssignableTo(RestServlet.class)) {
						var rs = (RestServlet)c.newInstance();
						mountWithCollisionCheck(rs, rs.getPath(), "Jetty/servlets[" + s + "]", mountedPaths);
					} else {
						throw rex("Invalid servlet specified in Jetty/servlets.  Must be a subclass of RestServlet: {0}", s);
					}
				} catch (ClassNotFoundException e) {
					throw toRex(e);
				}
			}

			cf.get("Jetty/servletMap").asMap().orElse(EMPTY_MAP).forEach((k, v) -> {
				try {
					var c = info(Class.forName(v.toString()));
					if (c.isAssignableTo(Servlet.class)) {
						var rs = (Servlet)c.newInstance();
						mountWithCollisionCheck(rs, k, "Jetty/servletMap[" + k + "]", mountedPaths);
					} else {
						throw rex("Invalid servlet specified in Jetty/servletMap.  Must be a subclass of Servlet: {0}", cn(v));
					}
				} catch (ClassNotFoundException e) {
					throw toRex(e);
				}
			});

			cf.get("Jetty/servletAttributes").asMap().orElse(EMPTY_MAP).forEach(this::addServletAttribute);

			// Auto-mount @Bean AuthFilterChain at /* before any servlet registration so the filter
			// runs before routing for all requests.
			store.getBean(AuthFilterChain.class).ifPresent(chain -> addFilter(chain, "/*"));

			// Auto-discover @Rest servlets contributed via @Configuration/@Bean methods.
			// Path source precedence: @Rest(path=...) on the resource class, else "/".
			for (var e : store.getBeansOfType(Servlet.class).entrySet()) {
				var servlet = e.getValue();
				var cls = servlet.getClass();
				if (cls.getAnnotation(Rest.class) == null)
					continue;
				var pathSpecs = restPathsFor(servlet, store);
				var source = "@Bean " + cls.getName() + (ne(e.getKey()) ? "[" + e.getKey() + "]" : "");
				for (var pathSpec : pathSpecs)
					checkPathCollision(pathSpec, source, mountedPaths);
				addServlet(servlet, pathSpecs);
			}

			if (serverPortEnv.isEmpty())
				System.setProperty("juneau.serverPort", String.valueOf(availablePort));

			server.get().start();
			ms.out(messages, "ServerStarted", getPort());
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from MicroserviceListener */
	public void onStop(Microservice ms) {
		final Logger logger = ms.getLogger();
		var t = new Thread("JettyServerComponentStop") {
			@Override /* Overridden from Thread */
			public void run() {
				try {
					var s = server.get();
					if (s == null || s.isStopping() || s.isStopped())
						return;
					ms.out(messages, "StoppingServer");
					s.stop();
					ms.out(messages, "ServerStopped");
				} catch (Exception e) {
					logger.log(Level.WARNING, lm(e), e);
				}
			}
		};
		t.start();
		try {
			t.join();
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override /* Overridden from MicroserviceListener */
	public void onConfigChange(Microservice ms, ConfigEvents events) {
		// No-op: Intentional empty implementation - this component is purely lifecycle-driven.
	}

	/**
	 * Adds an arbitrary servlet to this Jetty server.
	 *
	 * @param servlet The servlet instance.
	 * @param pathSpec The context path of the servlet.
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S3878" // Array wrapper required to dispatch to varargs overload; without it the call recurses infinitely back to this single-String overload.
	})
	public JettyServerComponent addServlet(Servlet servlet, String pathSpec) {
		if (nn(pathSpec) && ! pathSpec.endsWith("/*"))
			pathSpec = trimTrailingSlashes(pathSpec) + "/*";
		return addServlet(servlet, new String[]{pathSpec});
	}

	/**
	 * Adds an arbitrary servlet to this Jetty server at one or more context paths.
	 *
	 * @param servlet The servlet instance.
	 * @param pathSpecs The context paths of the servlet.
	 * @return This object.
	 */
	public JettyServerComponent addServlet(Servlet servlet, String...pathSpecs) {
		var sh = new ServletHolder(servlet);
		for (var pathSpec : pathSpecs)
			getServletContextHandler().addServlet(sh, pathSpec);
		return this;
	}

	/**
	 * Adds a servlet filter to this Jetty server at the specified URL pattern.
	 *
	 * @param filter The filter instance.
	 * @param urlPattern The URL pattern the filter applies to (e.g. {@code "/*"}, {@code "/api/*"}).
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S3878" // Array wrapper required to dispatch to varargs overload; without it the call recurses infinitely back to this single-String overload.
	})
	public JettyServerComponent addFilter(jakarta.servlet.Filter filter, String urlPattern) {
		return addFilter(filter, new String[]{urlPattern});
	}

	/**
	 * Adds a servlet filter to this Jetty server at one or more URL patterns.
	 *
	 * @param filter The filter instance.
	 * @param urlPatterns The URL patterns the filter applies to.
	 * @return This object.
	 */
	public JettyServerComponent addFilter(jakarta.servlet.Filter filter, String... urlPatterns) {
		var fh = new FilterHolder(filter);
		for (var urlPattern : urlPatterns)
			getServletContextHandler().addFilter(fh, urlPattern, EnumSet.of(DispatcherType.REQUEST));
		return this;
	}

	/**
	 * Adds an attribute to the Jetty server.
	 *
	 * @param name The server attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public JettyServerComponent addServletAttribute(String name, Object value) {
		getServer().setAttribute(name, value);
		return this;
	}

	/**
	 * Returns the context path that this server is using.
	 *
	 * @return The context path.
	 */
	public String getContextPath() {
		return getServletContextHandler().getContextPath();
	}

	/**
	 * Returns the hostname of this microservice.
	 *
	 * @return The hostname.
	 */
	public String getHostName() {
		String hostname = "localhost";
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (@SuppressWarnings("unused") UnknownHostException e) {
			// Cannot determine hostname, use default "localhost"
		}
		return hostname;
	}

	/**
	 * Returns the port that this server is listening on.
	 *
	 * @return The port.
	 */
	public int getPort() {
		for (var c : getServer().getConnectors())
			if (c instanceof ServerConnector c2)
				return c2.getPort();
		throw new IllegalStateException("Could not locate ServerConnector in Jetty server.");
	}

	/**
	 * Returns whether this server is using <js>"http"</js> or <js>"https"</js>.
	 *
	 * @return The protocol.
	 */
	public String getProtocol() {
		for (var c : getServer().getConnectors())
			if (c instanceof ServerConnector c2)
				for (var cf : c2.getConnectionFactories())
					if (cf instanceof SslConnectionFactory)
						return "https";
		return "http";
	}

	/**
	 * Returns the underlying Jetty server.
	 *
	 * @return The underlying Jetty server.
	 * @throws IllegalStateException If the server has not been created yet.
	 */
	public Server getServer() {
		return Objects.requireNonNull(server.get(), "Server not found.  Microservice has not been started yet.");
	}

	/**
	 * Finds and returns the servlet context handler defined in the Jetty container.
	 *
	 * @return The servlet context handler.
	 * @throws IllegalStateException If the context handler is not defined.
	 */
	public ServletContextHandler getServletContextHandler() {
		var obj = getServer().getAttribute(KEY_SERVLET_CONTEXT_HANDLER);
		if (obj instanceof ServletContextHandler obj2)
			return obj2;
		throw new IllegalStateException("Servlet context handler not found in jetty server or at attribute '" + KEY_SERVLET_CONTEXT_HANDLER + "'");
	}

	/**
	 * Returns the URI where this server is listening.
	 *
	 * @return The URI.
	 */
	public URI getURI() {
		var cp = getContextPath();
		try {
			return new URI(getProtocol(), null, getHostName(), getPort(), "/".equals(cp) ? null : cp, null, null);
		} catch (URISyntaxException e) {
			throw toRex(e);
		}
	}

	private void mountWithCollisionCheck(Servlet servlet, String rawPath, String source, Map<String,String> mountedPaths) {
		var pathSpec = normalizePathSpec(rawPath);
		checkPathCollision(pathSpec, source, mountedPaths);
		addServlet(servlet, pathSpec);
	}

	private void checkPathCollision(String pathSpec, String source, Map<String,String> mountedPaths) {
		var prior = mountedPaths.get(pathSpec);
		if (nn(prior))
			throw rex("Servlet mount path collision: ''{0}'' is already mounted by {1}; refused by {2}.", pathSpec, prior, source);
		mountedPaths.put(pathSpec, source);
	}
}
