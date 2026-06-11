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
package org.apache.juneau.microservice.tomcat;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.marshall.collections.JsonMap.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.catalina.*;
import org.apache.catalina.startup.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.tomcat.util.descriptor.web.*;

import jakarta.servlet.*;

/**
 * Drives the embedded Tomcat server lifecycle on behalf of the microservice via the {@link MicroserviceListener} fan-out.
 *
 * <p>
 * Contributed by {@link TomcatConfiguration} as a <c>@Bean</c>.  The microservice's {@link Microservice#start()}
 * call invokes {@link #onStart(Microservice)}, which:
 * <ol>
 * 	<li>Picks the first available port from the resolved {@link TomcatSettings#getPorts() ports} (falling back to
 * 		<c>Tomcat/port</c> config, then <c>Tomcat-Port</c> manifest, then <c>8000</c>).
 * 	<li>Resolves the {@link Tomcat} instance — preferring an externally-supplied <c>@Bean Tomcat</c>, otherwise
 * 		building one programmatically via the {@link TomcatServerFactory} against a writable base directory.
 * 	<li>Mounts servlets from {@code Tomcat/servlets}, {@code Tomcat/servletMap}, {@code Tomcat/servletAttributes}
 * 		config sections, plus auto-mounts every {@code @Bean Servlet} whose runtime class is annotated with
 * 		{@link Rest @Rest} (at {@link Rest#path()}, or <c>"/"</c> when no path is set).
 * 	<li>Materializes the connector via {@link Tomcat#getConnector()}, starts the server, and publishes the bound
 * 		port via the {@code availablePort} / {@code juneau.serverPort} system properties.
 * </ol>
 *
 * <p>
 * {@link #onStop(Microservice)} performs the inverse: stops and destroys the server gracefully in a worker thread,
 * then deletes the auto-created temp base directory (if one was created).
 *
 * <p>
 * External callers reach this component via
 * {@code Microservice.getInstance().getBeanStore().getBean(TomcatServerComponent.class).orElseThrow()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcatBasics">juneau-microservice-tomcat Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_SNAKE_CASE convention (e.g., ROOT_CONTEXT_PATH)
})
public class TomcatServerComponent implements MicroserviceListener {

	private static final Random RANDOM = new Random();
	private static final String ROOT_CONTEXT_PATH = "";
	private static final String ROOT_DOC_BASE = ".";

	private final Messages messages = Messages.of(TomcatServerComponent.class);
	private final AtomicReference<Tomcat> tomcat = new AtomicReference<>();
	private final AtomicReference<Microservice> microservice = new AtomicReference<>();
	private final AtomicReference<File> baseDir = new AtomicReference<>();
	private final AtomicBoolean ownsBaseDir = new AtomicBoolean(false);

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
			} catch (IOException e) {
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
	 * Resolves the top-level Tomcat mount path-specs for an auto-discovered {@code @Rest}-annotated servlet.
	 *
	 * <p>
	 * Delegates to {@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore)} so the runtime-override
	 * resolution chain (programmatic &gt; {@code getPaths()} getter &gt; annotation default) is honored at
	 * mount time. The programmatic Builder rung is N/A for Tomcat auto-discovery (the {@code RestContext} does
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
			return Arrays.stream(resolved).map(TomcatServerComponent::normalizeExactPathSpec).toArray(String[]::new);
		return new String[] {normalizePathSpec(r.path())};
	}

	/**
	 * Constructor.
	 */
	@SuppressWarnings({
		"java:S1186" // Empty constructor; all state is initialized via onStart(Microservice) when wired through the bean store.
	})
	public TomcatServerComponent() {}

	@Override /* Overridden from MicroserviceListener */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for server creation logic
	})
	public void onStart(Microservice ms) {
		try {
			microservice.set(ms);
			var store = ms.getBeanStore();
			ClassInfo.of(this).inject(this, store);
			var cf = ms.getConfig();
			var mf = ms.getManifest();
			var settings = store.getBean(TomcatSettings.class).orElseGet(() -> TomcatSettings.create().build());

			// Port resolution: settings > Tomcat/port config > Tomcat-Port manifest > 8000
			var ports = settings.getPorts();
			if (ports == null)
				ports = cf.get("Tomcat/port").as(int[].class)
					.orElseGet(() -> mf.get("Tomcat-Port").map(TomcatServerComponent::parseIntArray).orElseGet(() -> ints(8000)));
			var availablePort = findOpenPort(ports);

			if (availablePortEnv.isEmpty())
				System.setProperty("availablePort", String.valueOf(availablePort));

			// Prefer a @Bean-supplied Tomcat, else build one programmatically via the factory.
			var injectedTomcat = store.getBean(Tomcat.class).orElse(null);
			if (nn(injectedTomcat)) {
				tomcat.set(injectedTomcat);
			} else {
				var factory = store.getBean(TomcatServerFactory.class).orElseGet(BasicTomcatServerFactory::new);
				tomcat.set(createServer(factory, resolveBaseDir(settings)));
			}
			tomcat.get().setPort(availablePort);
			tomcat.get().getConnector();

			// Publish back to the store for downstream beans / lookups.
			store.addBean(Tomcat.class, tomcat.get());

			// Track each servlet pathSpec with its declaring source so we can fail loudly on collisions.
			var mountedPaths = new LinkedHashMap<String,String>();

			for (var s : cf.get("Tomcat/servlets").asStringArray().orElse(new String[0]))
				mountConfiguredServlet(s, mountedPaths);

			cf.get("Tomcat/servletMap").asMap().orElse(EMPTY_MAP).forEach((k, v) -> mountMappedServlet(k, v, mountedPaths));

			cf.get("Tomcat/servletAttributes").asMap().orElse(EMPTY_MAP).forEach(this::addServletAttribute);

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

			tomcat.get().start();
			ms.out(messages, "ServerStarted", getPort());
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from MicroserviceListener */
	public void onStop(Microservice ms) {
		final Logger logger = ms.getLogger();
		var t = new Thread("TomcatServerComponentStop") {
			@Override /* Overridden from Thread */
			public void run() {
				try {
					var t2 = tomcat.get();
					if (t2 == null)
						return;
					ms.out(messages, "StoppingServer");
					t2.stop();
					t2.destroy();
					ms.out(messages, "ServerStopped");
				} catch (Exception e) {
					logger.log(Level.WARNING, lm(e), e);
				} finally {
					cleanupBaseDir();
				}
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override /* Overridden from MicroserviceListener */
	public void onConfigChange(Microservice ms, ConfigEvents events) {
		// No-op: Intentional empty implementation - this component is purely lifecycle-driven.
	}

	/**
	 * Adds an arbitrary servlet to this Tomcat server.
	 *
	 * @param servlet The servlet instance.
	 * @param pathSpec The context path of the servlet.
	 * @return This object.
	 */
	@SuppressWarnings("java:S3878") // Array wrapper required to dispatch to varargs overload; without it the call recurses infinitely back to this single-String overload.
	public TomcatServerComponent addServlet(Servlet servlet, String pathSpec) {
		return addServlet(servlet, new String[]{normalizePathSpec(pathSpec)});
	}

	/**
	 * Adds an arbitrary servlet to this Tomcat server at one or more context paths.
	 *
	 * @param servlet The servlet instance.
	 * @param pathSpecs The context paths of the servlet.
	 * @return This object.
	 */
	public TomcatServerComponent addServlet(Servlet servlet, String...pathSpecs) {
		var context = getContext();
		var servletName = cn(servlet) + "#" + UUID.randomUUID();
		Tomcat.addServlet(context, servletName, servlet);
		for (var pathSpec : pathSpecs)
			context.addServletMappingDecoded(pathSpec, servletName);
		return this;
	}

	/**
	 * Adds a servlet filter to this Tomcat server at the specified URL pattern.
	 *
	 * @param filter The filter instance.
	 * @param urlPattern The URL pattern the filter applies to (e.g. {@code "/*"}, {@code "/api/*"}).
	 * @return This object.
	 */
	@SuppressWarnings("java:S3878") // Array wrapper required to dispatch to varargs overload; without it the call recurses infinitely back to this single-String overload.
	public TomcatServerComponent addFilter(jakarta.servlet.Filter filter, String urlPattern) {
		return addFilter(filter, new String[]{urlPattern});
	}

	/**
	 * Adds a servlet filter to this Tomcat server at one or more URL patterns.
	 *
	 * @param filter The filter instance.
	 * @param urlPatterns The URL patterns the filter applies to.
	 * @return This object.
	 */
	public TomcatServerComponent addFilter(jakarta.servlet.Filter filter, String...urlPatterns) {
		var context = getContext();
		var filterName = cn(filter) + "#" + UUID.randomUUID();
		var fd = new FilterDef();
		fd.setFilter(filter);
		fd.setFilterName(filterName);
		context.addFilterDef(fd);
		for (var pattern : urlPatterns) {
			var fm = new FilterMap();
			fm.setFilterName(filterName);
			fm.addURLPattern(pattern);
			fm.setDispatcher(DispatcherType.REQUEST.name());
			context.addFilterMap(fm);
		}
		return this;
	}

	/**
	 * Adds an attribute to the root servlet context of the Tomcat server.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public TomcatServerComponent addServletAttribute(String name, Object value) {
		getContext().getServletContext().setAttribute(name, value);
		return this;
	}

	/**
	 * Returns the context path that this server is using.
	 *
	 * @return The context path.
	 */
	public String getContextPath() {
		return getContext().getPath();
	}

	/**
	 * Returns the root {@link Context} of the Tomcat server, creating it if necessary.
	 *
	 * @return The root context.
	 */
	public Context getContext() {
		var context = (Context)getServer().getHost().findChild(ROOT_CONTEXT_PATH);
		if (context != null)
			return context;
		return getServer().addContext(ROOT_CONTEXT_PATH, ROOT_DOC_BASE);
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
		} catch (UnknownHostException e) {
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
		return getServer().getConnector().getLocalPort();
	}

	/**
	 * Returns whether this server is using <js>"http"</js> or <js>"https"</js>.
	 *
	 * @return The protocol.
	 */
	public String getProtocol() {
		return getServer().getConnector().getSecure() ? "https" : "http";
	}

	/**
	 * Returns the underlying Tomcat server.
	 *
	 * @return The underlying Tomcat server.
	 * @throws IllegalStateException If the server has not been created yet.
	 */
	public Tomcat getServer() {
		return Objects.requireNonNull(tomcat.get(), "Tomcat not found.  Microservice has not been started yet.");
	}

	/**
	 * Returns the URI where this server is listening.
	 *
	 * @return The URI.
	 */
	public URI getURI() {
		var cp = getContextPath();
		try {
			return new URI(getProtocol(), null, getHostName(), getPort(), (cp == null || cp.isEmpty()) ? null : cp, null, null);
		} catch (URISyntaxException e) {
			throw toRex(e);
		}
	}

	private static Tomcat createServer(TomcatServerFactory factory, File dir) {
		try {
			return factory.create(dir);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	private File resolveBaseDir(TomcatSettings settings) {
		var configured = settings.getBaseDir();
		if (ne(configured)) {
			ownsBaseDir.set(false);
			var f = new File(configured);
			baseDir.set(f);
			return f;
		}
		try {
			var f = createTempBaseDir().toFile();
			ownsBaseDir.set(true);
			baseDir.set(f);
			return f;
		} catch (IOException e) {
			throw toRex(e);
		}
	}

	@SuppressWarnings({
		"java:S5443" // Temp dir created via Files.createTempDirectory with owner-only (rwx------) POSIX perms; non-POSIX fallback relies on OS-default owner-restricted temp dirs.
	})
	private static Path createTempBaseDir() throws IOException {
		try {
			// Explicitly restrict to owner-only access (rwx------) on POSIX file systems.
			Set<PosixFilePermission> ownerOnly = EnumSet.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE);
			return Files.createTempDirectory("juneau-tomcat",
				PosixFilePermissions.asFileAttribute(ownerOnly));
		} catch (UnsupportedOperationException e) {
			// Non-POSIX filesystem (e.g. Windows) — fall back to default temp-dir creation.
			return Files.createTempDirectory("juneau-tomcat");
		}
	}

	private void cleanupBaseDir() {
		if (ownsBaseDir.get()) {
			var f = baseDir.getAndSet(null);
			if (nn(f))
				deleteFile(f);
		}
	}

	private void mountConfiguredServlet(String className, Map<String,String> mountedPaths) {
		try {
			var c = info(Class.forName(className));
			if (c.isAssignableTo(RestServlet.class)) {
				var rs = (RestServlet)c.newInstance();
				mountWithCollisionCheck(rs, rs.getPath(), "Tomcat/servlets[" + className + "]", mountedPaths);
			} else {
				throw rex("Invalid servlet specified in Tomcat/servlets.  Must be a subclass of RestServlet: {0}", className);
			}
		} catch (ClassNotFoundException e) {
			throw toRex(e);
		}
	}

	private void mountMappedServlet(String pathKey, Object className, Map<String,String> mountedPaths) {
		try {
			var c = info(Class.forName(className.toString()));
			if (c.isAssignableTo(Servlet.class)) {
				var rs = (Servlet)c.newInstance();
				mountWithCollisionCheck(rs, pathKey, "Tomcat/servletMap[" + pathKey + "]", mountedPaths);
			} else {
				throw rex("Invalid servlet specified in Tomcat/servletMap.  Must be a subclass of Servlet: {0}", cn(className));
			}
		} catch (ClassNotFoundException e) {
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
