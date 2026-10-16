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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;

import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Starts a real embedded Tomcat through {@link TomcatServerComponent} and verifies that the servlets it mounts are
 * async-capable, so that {@link jakarta.servlet.http.HttpServletRequest#startAsync()} and asynchronous REST operations
 * take the real asynchronous path.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.TomcatMicroserviceTest
@SuppressWarnings({
	"resource" // Microservice instances are test fixtures stopped in finally blocks.
})
class ServletAsync_TomcatMount_Test {

	private static final AtomicBoolean REST_ASYNC_SUPPORTED = new AtomicBoolean();

	@Rest(path="/probe")
	public static class Probe extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.getWriter().print("async=" + req.isAsyncSupported());
		}
	}

	@Rest(path="/completer")
	public static class Completer extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) {
			var ac = req.startAsync();
			ac.start(() -> {
				try {
					ac.getResponse().getWriter().print("completed");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} finally {
					ac.complete();
				}
			});
		}
	}

	@Rest(path="/rest")
	public static class FutureResource extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/future")
		public CompletableFuture<String> future(HttpServletRequest req) {
			REST_ASYNC_SUPPORTED.set(req.isAsyncSupported());
			return CompletableFuture.supplyAsync(() -> "future-result", CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
		}
	}

	@Configuration
	static class Servlets_Config {
		@Bean TomcatSettings tomcatSettings() { return TomcatSettings.create().ports(0).build(); }
		@Bean(name="probe") Servlet probe() { return new Probe(); }
		@Bean(name="completer") Servlet completer() { return new Completer(); }
		@Bean(name="rest") Servlet rest() { return new FutureResource(); }
	}

	private static Microservice start() throws Exception {
		var ms = Microservice.create().configurations(Servlets_Config.class, TomcatConfiguration.class).build();
		ms.start();
		return ms;
	}

	private static String get(Microservice ms, String path) throws Exception {
		var port = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow().getPort();
		var conn = (HttpURLConnection)URI.create("http://localhost:" + port + path).toURL().openConnection();
		conn.setRequestProperty("Accept", "text/plain");
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(10000);
		try {
			try (var in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
				return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} finally {
			conn.disconnect();
		}
	}

	@Test void a01_mountedServlet_isAsyncSupported() throws Exception {
		var ms = start();
		try {
			assertEquals("async=true", get(ms, "/probe/x"));
		} finally {
			ms.stop();
		}
	}

	@Test void a02_startAsyncCompletes() throws Exception {
		var ms = start();
		try {
			assertEquals("completed", get(ms, "/completer/x"));
		} finally {
			ms.stop();
		}
	}

	@Test void a03_restOpReturningFuture_takesAsyncPath() throws Exception {
		var ms = start();
		try {
			REST_ASYNC_SUPPORTED.set(false);
			assertEquals("future-result", get(ms, "/rest/future"));
			assertTrue(REST_ASYNC_SUPPORTED.get());
		} finally {
			ms.stop();
		}
	}
}
