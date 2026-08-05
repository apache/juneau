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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.BeanStore;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.springboot.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.server.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.*;

/**
 * Real-Spring-Boot proof for the MCP-on-Spring-Boot recipe: that {@link SpringMcpRestServlet} actually
 * bridges the Spring {@code ApplicationContext} into an MCP tool handler's per-request {@code BeanStore},
 * versus the plain servlet-subclass path ({@link McpRestServlet}) which does not.
 *
 * <p>
 * Before this test, nothing anywhere exercised MCP + Spring Boot together: the recipe was documented
 * (see {@code SpringMcpRestServlet}'s javadoc and the {@code juneau-docs} MCP recipes topic) but never
 * proven against a real embedded-Tomcat Spring Boot context.
 *
 * <p>
 * Boots a single Spring Boot context with embedded Tomcat on a random port (mirroring
 * {@code BasicApiDocs_Springboot_Test}'s idiom) registering <b>two</b> independent MCP servlets side by
 * side, both backed by the very same {@code ApplicationContext} and the very same {@code @Bean
 * MySpringService} singleton:
 * <ul>
 * 	<li><b>Positive case</b> ({@link SpringMcpResource}, {@code /mcp-spring/mcp}): extends
 * 		{@link SpringMcpRestServlet}. Its {@code whoAmI} tool handler calls
 * 		{@code BeanStore.getBean(MySpringService.class)} and must resolve the real Spring-managed
 * 		singleton &mdash; proven by echoing back a per-instance random marker that exactly matches the
 * 		same instance {@code @Autowired} into this test class.
 * 	<li><b>Negative/contrast case</b> ({@link PlainMcpResource}, {@code /mcp-plain/mcp}): extends
 * 		{@link McpRestServlet} (the plain servlet-subclass path, no Spring base class). The identical
 * 		{@code whoAmI} tool handler's {@code BeanStore.getBean(MySpringService.class)} call must come
 * 		back empty, documenting the recipe's known boundary: subclassing {@link McpRestServlet} does not
 * 		carry Spring DI, even when deployed inside the same Spring Boot app as a servlet that does.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@SpringBootTest(classes = McpSpringBootIntegration_Test.TestApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpSpringBootIntegration_Test {

	/**
	 * A Spring-managed singleton service. Generates a random per-instance marker at construction so any
	 * two distinct instances are trivially distinguishable: a tool handler that echoes back a marker
	 * matching {@link #mySpringService}'s (the instance this test class itself has {@code @Autowired})
	 * can only be doing so because it resolved that exact same Spring-container-managed instance, not a
	 * coincidentally-equal one it constructed itself.
	 */
	static class MySpringService {
		private final String marker = "spring-bean-" + UUID.randomUUID();
		String getMarker() { return marker; }
	}

	/** Sentinel returned by {@link #whoAmITool()} when {@code BeanStore.getBean(MySpringService.class)} finds nothing. */
	private static final String NO_BEAN = "NO_SPRING_BEAN_RESOLVED";

	/**
	 * Shared tool logic registered identically on both {@link SpringMcpResource} and
	 * {@link PlainMcpResource} below: resolve {@link MySpringService} through the per-request
	 * {@link BeanStore} and echo back its marker, or {@link #NO_BEAN} if it could not be resolved. Using
	 * one shared handler implementation for both fixtures ensures the only variable between the two test
	 * cases is which servlet base class is doing the resolving, not any difference in the handler itself.
	 */
	private static McpToolHandler whoAmITool() {
		return McpToolHandler.of(
			new McpToolSpec().setName("whoAmI").setDescription("Resolves MySpringService via BeanStore.getBean(...)"),
			(arguments, ctx) -> McpToolOutcome.text(ctx.getBean(MySpringService.class).map(MySpringService::getMarker).orElse(NO_BEAN)));
	}

	// -------- positive case: the MCP-on-Spring-Boot recipe ---------

	/**
	 * The recipe under test. {@link SpringMcpRestServlet} extends {@link BasicSpringRestServlet}, whose
	 * {@code SpringRestServlet} ancestor has an {@code @Autowired ApplicationContext} field and a
	 * {@code @Bean createBeanStore(...)} factory method that wraps it in a {@code SpringBeanStore}; that
	 * store becomes this resource's {@code RestContext} bean store, and the MCP endpoint mixin's
	 * {@code handleMcpRequest} wraps a fresh per-request {@code BasicBeanStore} around it before invoking
	 * the tool handler &mdash; so {@code BeanStore.getBean(MySpringService.class)} chains all the way
	 * through to {@code ApplicationContext.getBeanProvider(MySpringService.class)}.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class SpringMcpResource extends SpringMcpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(whoAmITool());
		}
	}

	// -------- negative/contrast case: the servlet-subclass path does NOT carry Spring DI ---------

	/**
	 * Contrast fixture documenting the recipe's known boundary. {@link McpRestServlet} is a sibling
	 * {@code RestServlet} subclass to {@link BasicSpringRestServlet} (both ultimately extend
	 * {@code RestServlet}), <b>not</b> a subclass of it &mdash; so it never gets the
	 * {@code @Autowired ApplicationContext} field {@code SpringRestServlet} injects, and never publishes
	 * a {@code SpringBeanStore}-backed {@code createBeanStore()} bean. Its {@code RestContext} bean store
	 * is therefore a plain, Spring-oblivious store even though this servlet is registered in the very
	 * same Spring Boot context as {@link SpringMcpResource} (same {@code ApplicationContext}, same
	 * {@code @Bean MySpringService}). Its {@code whoAmI} tool handler's
	 * {@code BeanStore.getBean(MySpringService.class)} call therefore cannot see that bean.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class PlainMcpResource extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(whoAmITool());
		}
	}

	/**
	 * Minimal Spring Boot application registering both fixture servlets side by side at distinct
	 * container URL mappings, plus the single shared {@code @Bean MySpringService} both endpoints (try
	 * to) resolve.
	 */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

		@Bean
		public MySpringService mySpringService() {
			return new MySpringService();
		}

		@Bean
		public SpringMcpResource springMcpResource() {
			return new SpringMcpResource();
		}

		@Bean
		public ServletRegistrationBean<SpringMcpResource> springMcpRegistration(SpringMcpResource servlet) {
			return new ServletRegistrationBean<>(servlet, "/mcp-spring/*");
		}

		@Bean
		public PlainMcpResource plainMcpResource() {
			return new PlainMcpResource();
		}

		@Bean
		public ServletRegistrationBean<PlainMcpResource> plainMcpRegistration(PlainMcpResource servlet) {
			return new ServletRegistrationBean<>(servlet, "/mcp-plain/*");
		}
	}

	@Autowired
	MySpringService mySpringService;

	@LocalServerPort
	int port;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static String toolCallBody(String toolName) {
		var params = JsonMap.of("name", toolName, "arguments", JsonMap.of(), "_meta", validMeta());
		return Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1).setMethod("tools/call").setParams(params));
	}

	/**
	 * Drives a real {@code tools/call} for the {@code whoAmI} tool over real HTTP against {@code path},
	 * and returns the text content of the (successful) result.
	 */
	private String callWhoAmI(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Mcp-Method", "tools/call")
			.header("Mcp-Name", "whoAmI")
			.POST(BodyPublishers.ofString(toolCallBody("whoAmI")))
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofString());
		assertEquals(200, resp.statusCode(), () -> "Unexpected status; body=" + resp.body());
		var envelope = Json.to(resp.body(), JsonMap.class);
		assertNull(envelope.get("error"), () -> "Unexpected JSON-RPC error: " + envelope.get("error"));
		var result = (Map<?,?>) envelope.get("result");
		var content = (List<?>) result.get("content");
		var first = (Map<?,?>) content.get(0);
		return (String) first.get("text");
	}

	@Test
	void a01_springMcpRestServlet_toolHandlerResolvesRealSpringManagedBean() throws Exception {
		// SpringMcpRestServlet exposes the MCP endpoint via the McpEndpoint mixin's handleMcpRequest,
		// which is annotated @RestPost(path = "/mcp") -- relative to this resource's own (default,
		// root) @Rest path -- so the wire URL is basePath + "/mcp".
		var marker = callWhoAmI("/mcp-spring/mcp");

		assertNotEquals(NO_BEAN, marker, "Positive case: the Spring-DI-aware endpoint must resolve MySpringService");
		assertEquals(mySpringService.getMarker(), marker,
			"The MCP tool handler must resolve the SAME Spring-managed singleton this test class has @Autowired -- "
				+ "proving BeanStore.getBean(...) really flowed through to Spring's ApplicationContext, not just "
				+ "some independently-constructed lookalike");
	}

	@Test
	void b01_plainServletSubclassPath_doesNotCarrySpringDi() throws Exception {
		// Contrast/documentation case: same Spring Boot app, same ApplicationContext, same @Bean
		// MySpringService -- but this endpoint extends McpRestServlet (not SpringMcpRestServlet /
		// BasicSpringRestServlet), so its RestContext BeanStore was never bridged to Spring. This is a
		// known, documented boundary of the recipe, not a bug: BeanStore.getBean(MySpringService.class)
		// must come back empty here, in contrast to a01 above.
		// McpRestServlet's operation is declared @RestPost(path = "/") -- its servlet root, not "/mcp"
		// -- unlike the mixin path exercised in a01 above, so the wire URL is just basePath itself.
		var marker = callWhoAmI("/mcp-plain");

		assertEquals(NO_BEAN, marker,
			"extends McpRestServlet (the plain servlet-subclass path, no Spring bridge) must NOT resolve the "
				+ "Spring-managed bean -- this is the recipe's documented boundary, not a regression");
	}
}
