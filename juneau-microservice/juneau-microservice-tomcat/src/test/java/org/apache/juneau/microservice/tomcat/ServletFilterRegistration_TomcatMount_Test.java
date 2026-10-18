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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.*;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Starts a real embedded Tomcat through {@link TomcatServerComponent} and verifies that {@link ServletFilterRegistration} beans are
 * mounted on the requested URL patterns and dispatcher types, in the defined order, with the {@link AuthFilterChain}
 * mount re-expressed through the same path.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.TomcatMicroserviceTest
@SuppressWarnings({
	"java:S1192", // Repeated URL pattern and tag literals keep each test self-contained and readable.
	"java:S3415", // Expected value is already first; Sonar misreads the upper-case LOG constant as the expected argument
	"resource" // Microservice instances are test fixtures managed by the test lifecycle; explicit close is not needed for these assertions.
})
class ServletFilterRegistration_TomcatMount_Test extends TestBase {

	private static final List<String> LOG = new CopyOnWriteArrayList<>();

	private static final EnumSet<DispatcherType> ALL = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC, DispatcherType.ERROR);

	@BeforeEach void clearLog() {
		LOG.clear();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------------

	/** Records the tag and dispatcher type of each pass through it. */
	public static class Recorder implements Filter {
		private final String tag;

		Recorder(String tag) {
			this.tag = tag;
		}

		@Override /* Filter */
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
			LOG.add(tag + ":" + req.getDispatcherType());
			chain.doFilter(req, res);
		}
	}

	/** An authentication filter that records the pass and never authenticates. */
	public static class RecordingAuth extends AuthFilter {
		@Override /* Authenticator */
		public Optional<AuthResult> authenticate(HttpServletRequest req) {
			LOG.add("AUTH:" + req.getDispatcherType());
			return Optional.empty();
		}
	}

	@Rest(path="/hello")
	public static class Hello extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.getWriter().print("hello async=" + req.isAsyncSupported());
		}
	}

	@Rest(path="/other")
	public static class Other extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.getWriter().print("other");
		}
	}

	@Rest(path="/fwd")
	public static class Fwd extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
			req.getRequestDispatcher("/hello/f").forward(req, res);
		}
	}

	@Rest(path="/err")
	public static class Err extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.sendError(500);
		}
	}

	@Rest(path="/async")
	public static class Async extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) {
			var ac = req.startAsync();
			ac.start(() -> ac.dispatch("/hello/a"));
		}
	}

	/** Routes a 500 to {@code /hello/err}, so the error dispatch reaches a filtered servlet. */
	public static class ErrorPageFactory extends BasicTomcatServerFactory {
		@Override /* TomcatServerFactory */
		public Tomcat create(File baseDir) throws Exception {
			var tomcat = super.create(baseDir);
			var page = new ErrorPage();
			page.setErrorCode(500);
			page.setLocation("/hello/err");
			((Context)tomcat.getHost().findChild("")).addErrorPage(page);
			return tomcat;
		}
	}

	/** The server and the servlets every test uses. */
	@Configuration
	static class Base_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean TomcatServerFactory factory() { return new ErrorPageFactory(); }
		@Bean(name="hello") Servlet hello() { return new Hello(); }
		@Bean(name="other") Servlet other() { return new Other(); }
		@Bean(name="fwd") Servlet fwd() { return new Fwd(); }
		@Bean(name="err") Servlet err() { return new Err(); }
		@Bean(name="async") Servlet async() { return new Async(); }
	}

	private static Microservice create(Class<?>... configurations) throws Exception {
		// The user configurations come before TomcatConfiguration so their beans beat its defaults.
		var classes = new Class<?>[configurations.length + 2];
		classes[0] = Base_Config.class;
		System.arraycopy(configurations, 0, classes, 1, configurations.length);
		classes[classes.length - 1] = TomcatConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	private static void start(Microservice ms) throws Exception {
		ms.start();
	}

	private static String get(Microservice ms, String path) throws Exception {
		var port = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow().getPort();
		var conn = (HttpURLConnection)URI.create("http://localhost:" + port + path).toURL().openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		try {
			try (var in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
				return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} finally {
			conn.disconnect();
		}
	}

	private static ServletFilterRegistration.Builder reg(String tag, String...patterns) {
		return ServletFilterRegistration.create(new Recorder(tag)).urlPatterns(patterns);
	}

	private static ServletFilterRegistration.Builder regAll(String tag, String...patterns) {
		return reg(tag, patterns).dispatcherTypes(ALL.toArray(DispatcherType[]::new));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - dispatcher types
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class A_AllDispatchers_Config {
		@Bean(name="f") ServletFilterRegistration f() { return regAll("F", "/hello/*").build(); }
	}

	@Test void a01_request() throws Exception {
		var ms = create(A_AllDispatchers_Config.class);
		try {
			start(ms);
			assertEquals("hello async=true", get(ms, "/hello/a"));
			assertEquals(List.of("F:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Test void a02_forward() throws Exception {
		var ms = create(A_AllDispatchers_Config.class);
		try {
			start(ms);
			assertEquals("hello async=true", get(ms, "/fwd/x"));
			assertEquals(List.of("F:FORWARD"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Test void a03_async() throws Exception {
		var ms = create(A_AllDispatchers_Config.class);
		try {
			start(ms);
			assertTrue(get(ms, "/async/x").startsWith("hello"));
			assertEquals(List.of("F:ASYNC"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Test void a04_error() throws Exception {
		var ms = create(A_AllDispatchers_Config.class);
		try {
			start(ms);
			assertTrue(get(ms, "/err/x").startsWith("hello"));
			assertEquals(List.of("F:ERROR"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class A_AsyncOnly_Config {
		@Bean(name="f") ServletFilterRegistration f() { return reg("F", "/hello/*").dispatcherTypes(DispatcherType.ASYNC).build(); }
	}

	@Test void a05_asyncOnly_doesNotFilterPlainRequest() throws Exception {
		var ms = create(A_AsyncOnly_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of(), LOG);
			get(ms, "/async/x");
			assertEquals(List.of("F:ASYNC"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class A_Default_Config {
		@Bean(name="f") ServletFilterRegistration f() { return reg("F", "/hello/*").build(); }
	}

	@Test void a06_defaultIsRequestOnly() throws Exception {
		var ms = create(A_Default_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			get(ms, "/fwd/x");
			get(ms, "/err/x");
			assertEquals(List.of("F:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - patterns
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_patternScoping_outsidePatternNotFiltered() throws Exception {
		var ms = create(A_Default_Config.class);
		try {
			start(ms);
			assertEquals("other", get(ms, "/other/x"));
			assertEquals(List.of(), LOG);
			get(ms, "/hello/a");
			assertEquals(List.of("F:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_Overlap_Config {
		@Bean(name="f") ServletFilterRegistration f() { return reg("F", "/hello/*", "/*").build(); }
	}

	@Test void b02_overlappingPatterns_runOnce() throws Exception {
		var ms = create(B_Overlap_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("F:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class B_TwoPatterns_Config {
		@Bean(name="f") ServletFilterRegistration f() { return reg("F", "/hello/*", "/other/*").build(); }
	}

	@Test void b03_multiplePatterns_eachMapped() throws Exception {
		var ms = create(B_TwoPatterns_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			get(ms, "/other/a");
			assertEquals(List.of("F:REQUEST", "F:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - order
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class C_Order_Config {
		@Bean(name="a") ServletFilterRegistration a() { return reg("A", "/*").order(2).build(); }
		@Bean(name="b") ServletFilterRegistration b() { return reg("B", "/*").order(1).build(); }
		@Bean(name="c") ServletFilterRegistration c() { return reg("C", "/*").order(ServletFilterRegistration.HIGHEST_PRECEDENCE).build(); }
		@Bean(name="d") ServletFilterRegistration d() { return reg("D", "/*").order(ServletFilterRegistration.LOWEST_PRECEDENCE).build(); }
	}

	@Test void c01_explicitOrder_lowerRunsFirst() throws Exception {
		var ms = create(C_Order_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("C:REQUEST", "B:REQUEST", "A:REQUEST", "D:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class C_Tie_Config {
		@Bean(name="z") ServletFilterRegistration z() { return reg("Z", "/*").build(); }
		@Bean(name="m") ServletFilterRegistration m() { return reg("M", "/*").build(); }
		@Bean(name="q") ServletFilterRegistration q() { return reg("Q", "/*").build(); }
	}

	@Test void c02_equalOrder_byBeanName() throws Exception {
		var ms = create(C_Tie_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("M:REQUEST", "Q:REQUEST", "Z:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class C_OrderAnnotation_Config {
		@Bean(name="x") @org.apache.juneau.commons.inject.Order(-1000) ServletFilterRegistration x() { return reg("X", "/*").order(5).build(); }
		@Bean(name="y") @org.apache.juneau.commons.inject.Order(1000) ServletFilterRegistration y() { return reg("Y", "/*").order(1).build(); }
	}

	@Test void c03_orderAnnotationIgnored() throws Exception {
		var ms = create(C_OrderAnnotation_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("Y:REQUEST", "X:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - AuthFilterChain auto-mount
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class D_Auth_Config {
		@Bean AuthFilterChain auth(BeanStore bs) { return AuthFilterChain.create(bs).append(new RecordingAuth()).build(); }
	}

	@Test void d01_authAutoMount_present() throws Exception {
		var ms = create(D_Auth_Config.class);
		try {
			start(ms);
			assertEquals("hello async=true", get(ms, "/hello/a"));
			assertEquals(List.of("AUTH:REQUEST"), LOG);
			get(ms, "/fwd/x");
			assertEquals(List.of("AUTH:REQUEST", "AUTH:REQUEST"), LOG, "Auth stays on REQUEST: the forward is not filtered");
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class D_AuthAndFilters_Config {
		@Bean AuthFilterChain auth(BeanStore bs) { return AuthFilterChain.create(bs).append(new RecordingAuth()).build(); }
		@Bean(name="boundary") ServletFilterRegistration boundary() { return reg("BOUNDARY", "/hello/*").order(ServletFilterRegistration.HIGHEST_PRECEDENCE).build(); }
		@Bean(name="after") ServletFilterRegistration after() { return reg("AFTER", "/hello/*").build(); }
	}

	@Test void d02_boundaryBeforeAuth_defaultAfterAuth() throws Exception {
		var ms = create(D_AuthAndFilters_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("BOUNDARY:REQUEST", "AUTH:REQUEST", "AFTER:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class D_UnnamedTie_Config {
		@Bean AuthFilterChain auth(BeanStore bs) { return AuthFilterChain.create(bs).append(new RecordingAuth()).build(); }
		@Bean ServletFilterRegistration unnamed() { return reg("U", "/*").order(ServletFilterRegistration.AUTH_ORDER).build(); }
	}

	@Test void d03_unnamedRegistrationAtAuthOrder_authFirst() throws Exception {
		var ms = create(D_UnnamedTie_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("AUTH:REQUEST", "U:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class D_Replace_Config {
		@Bean AuthFilterChain auth(BeanStore bs) { return AuthFilterChain.create(bs).append(new RecordingAuth()).build(); }
		@Bean(name="authReg") ServletFilterRegistration authReg(AuthFilterChain chain) {
			return ServletFilterRegistration.create(chain).urlPatterns("/hello/*").dispatcherTypes(DispatcherType.FORWARD).build();
		}
	}

	@Test void d04_replaceHook_registrationOfAuthInstanceReplacesAutoMount() throws Exception {
		var ms = create(D_Replace_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of(), LOG, "The automatic REQUEST mount at /* is replaced");
			get(ms, "/fwd/x");
			assertEquals(List.of("AUTH:FORWARD"), LOG);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - collisions and misuse
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class E_DuplicateInstance_Config {
		private static final Filter SHARED = new Recorder("S");
		@Bean(name="one") ServletFilterRegistration one() { return ServletFilterRegistration.create(SHARED).urlPatterns("/a/*").build(); }
		@Bean(name="two") ServletFilterRegistration two() { return ServletFilterRegistration.create(SHARED).urlPatterns("/b/*").build(); }
	}

	@Test void e01_sameInstanceTwice_failsAtStart() throws Exception {
		var ms = create(E_DuplicateInstance_Config.class);
		try {
			var root = rootCause(assertThrows(Exception.class, ms::start));
			assertInstanceOf(IllegalStateException.class, root);
			assertTrue(root.getMessage().contains("'one'") && root.getMessage().contains("'two'"), root.getMessage());
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class E_DuplicateName_Config {
		@Bean(name="one") ServletFilterRegistration one() { return reg("1", "/a/*").name("same").build(); }
		@Bean(name="two") ServletFilterRegistration two() { return reg("2", "/b/*").name("same").build(); }
	}

	@Test void e02_duplicateName_failsAtStart() throws Exception {
		var ms = create(E_DuplicateName_Config.class);
		try {
			var root = rootCause(assertThrows(Exception.class, ms::start));
			assertInstanceOf(IllegalStateException.class, root);
			assertTrue(root.getMessage().contains("same"), root.getMessage());
		} finally {
			ms.stop();
		}
	}

	@Configuration
	static class E_TwoInstancesOneClass_Config {
		@Bean(name="one") ServletFilterRegistration one() { return reg("1", "/hello/*").build(); }
		@Bean(name="two") ServletFilterRegistration two() { return reg("2", "/hello/*").build(); }
	}

	@Test void e03_twoInstancesOfOneClass_bothRun() throws Exception {
		var ms = create(E_TwoInstancesOneClass_Config.class);
		try {
			start(ms);
			get(ms, "/hello/a");
			assertEquals(List.of("1:REQUEST", "2:REQUEST"), LOG);
		} finally {
			ms.stop();
		}
	}

	@Test void e04_addFilterAfterStart_rejected() throws Exception {
		var ms = create();
		try {
			start(ms);
			var c = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow();
			var late = new Recorder("L");
			var req = EnumSet.of(DispatcherType.REQUEST);
			var registration = ServletFilterRegistration.create(late).urlPatterns("/hello/*").build();
			assertThrows(IllegalStateException.class, () -> c.addFilter(late, req, "/hello/*"));
			assertThrows(IllegalStateException.class, () -> c.addFilter(registration));
		} finally {
			ms.stop();
		}
	}

	@Test void e05_addFilterOverload_validatesInput() throws Exception {
		var ms = create();
		try {
			start(ms);
			var c = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow();
			var f = new Recorder("L");
			var req = EnumSet.of(DispatcherType.REQUEST);
			assertThrows(IllegalArgumentException.class, () -> c.addFilter(null, req, "/a/*"));
			assertThrows(IllegalArgumentException.class, () -> c.addFilter(f, req));
			assertThrows(IllegalArgumentException.class, () -> c.addFilter(f, req, "/console*"));
			var none = EnumSet.noneOf(DispatcherType.class);
			assertThrows(IllegalArgumentException.class, () -> c.addFilter(f, none, "/a/*"));
			assertThrows(IllegalArgumentException.class, () -> c.addFilter((ServletFilterRegistration)null));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - async support
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class F_NotAsync_Config {
		@Bean(name="f") ServletFilterRegistration f() { return reg("F", "/hello/*").asyncSupported(false).build(); }
	}

	@Test void f01_asyncSupportedFalse_reachesTheRequest() throws Exception {
		var ms = create(F_NotAsync_Config.class);
		try {
			start(ms);
			assertEquals("hello async=false", get(ms, "/hello/a"));
		} finally {
			ms.stop();
		}
	}

	@Test void f02_defaultRegistration_isAsyncSupported() throws Exception {
		var ms = create(A_Default_Config.class);
		try {
			start(ms);
			assertEquals("hello async=true", get(ms, "/hello/a"));
		} finally {
			ms.stop();
		}
	}

	@Test void f03_authAutoMount_isAsyncSupported() throws Exception {
		var ms = create(D_Auth_Config.class);
		try {
			start(ms);
			assertEquals("hello async=true", get(ms, "/hello/a"), "The automatic AuthFilterChain mount must not turn async off");
		} finally {
			ms.stop();
		}
	}

	private static Throwable rootCause(Throwable t) {
		var x = t;
		while (x.getCause() != null && x.getCause() != x)
			x = x.getCause();
		return x;
	}
}
