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
package org.apache.juneau.rest.server.view.thymeleaf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.thymeleaf.*;
import org.thymeleaf.context.*;
import org.thymeleaf.templateresolver.*;

/**
 * Unit tests for {@link ThymeleafDispatcher#render(String, RestRequest, RestResponse) render(...)} and
 * {@link ThymeleafDispatcher#resolveTemplateEngine(RestRequest) resolveTemplateEngine(...)}, driven against
 * mocked {@link RestRequest}/{@link RestResponse}/{@link TemplateEngine} (all plain non-final classes,
 * straightforward to mock without a servlet container or real template resources). The raw-render happy
 * path is already exercised end-to-end via {@code MockRest} in {@code ThymeleafMixin_MockRest_Test}; this
 * class fills in the branches that path doesn't reach (registered engine bean, engine failure, an
 * already-set Content-Type, a {@code null} path segment, and a writer-level {@code IOException}).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // BasicBeanStore instances are short-lived in-memory test fixtures backed by a Map, consumed synchronously by the mocked request; nothing external to leak.
})
class ThymeleafDispatcher_Test extends TestBase {

	private static RestRequest mockRequest(WritableBeanStore beanStore) {
		var req = mock(RestRequest.class);
		var ctx = mock(RestContext.class);
		when(req.getContext()).thenReturn(ctx);
		when(ctx.getBeanStore()).thenReturn(beanStore);
		return req;
	}

	private static RestResponse mockResponse() throws IOException {
		var res = mock(RestResponse.class);
		when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
		return res;
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: resolveTemplateEngine
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_resolveTemplateEngine_prefersRegisteredBean() {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var registered = mock(TemplateEngine.class);
		beanStore.addBean(TemplateEngine.class, registered);

		assertSame(registered, dispatcher.resolveTemplateEngine(mockRequest(beanStore)));
	}

	@Test void a02_resolveTemplateEngine_secondCallReusesCachedDefault() {
		var dispatcher = ThymeleafDispatcher.create().build();
		var req = mockRequest(new BasicBeanStore());

		// First call builds+caches the bridge default (no TemplateEngine bean registered); the second
		// call's outer null-check takes the "already built" branch instead of re-entering the lock.
		var e1 = dispatcher.resolveTemplateEngine(req);
		var e2 = dispatcher.resolveTemplateEngine(req);
		assertNotNull(e1);
		assertSame(e1, e2);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: render
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_render_nullPathTreatedAsEmpty() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var engine = mock(TemplateEngine.class);
		beanStore.addBean(TemplateEngine.class, engine);
		var res = mockResponse();

		dispatcher.render(null, mockRequest(beanStore), res);

		verify(res).setHeader("Content-Type", ThymeleafViewRenderer.DEFAULT_CONTENT_TYPE);
		verify(engine).process(eq(""), any(IContext.class), any(Writer.class));
	}

	@Test void b02_render_explicitContentTypeNotOverridden() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(TemplateEngine.class, mock(TemplateEngine.class));
		var res = mockResponse();
		when(res.containsHeader("Content-Type")).thenReturn(true);

		dispatcher.render("hello", mockRequest(beanStore), res);

		verify(res, never()).setHeader(eq("Content-Type"), any());
	}

	@Test void b03_render_writerIOException_propagatesAsIs() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(TemplateEngine.class, mock(TemplateEngine.class));
		var res = mock(RestResponse.class);
		when(res.getWriter()).thenThrow(new IOException("broken pipe"));

		var ex = assertThrows(IOException.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), res));
		assertEquals("broken pipe", ex.getMessage());
	}

	@Test void b04_render_linkageErrorFromEngine_wrapsWithNoEngineDiagnostic() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var engine = mock(TemplateEngine.class);
		doThrow(new NoClassDefFoundError("org.thymeleaf.TemplateEngine")).when(engine)
			.process(anyString(), any(IContext.class), any(Writer.class));
		beanStore.addBean(TemplateEngine.class, engine);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("Thymeleaf engine"));
	}

	@Test void b05_render_templateRenderThrowsRuntimeException_wrapsInternalServerError() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var engine = mock(TemplateEngine.class);
		doThrow(new RuntimeException("boom")).when(engine)
			.process(anyString(), any(IContext.class), any(Writer.class));
		beanStore.addBean(TemplateEngine.class, engine);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("Thymeleaf render failed"));
	}

	@Test void b06_render_traversalEscape_throwsForbidden() throws Exception {
		var dispatcher = ThymeleafDispatcher.create().basePath("/templates/").build();
		var beanStore = new BasicBeanStore();

		assertThrows(Forbidden.class,
			() -> dispatcher.render("../../../etc/passwd", mockRequest(beanStore), mockResponse()));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: buildDefaultEngine cacheTemplates flag
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_buildDefaultEngine_cacheTemplatesFalse_setsResolverNonCacheable() {
		var dispatcher = ThymeleafDispatcher.create().cacheTemplates(false).build();
		var engine = dispatcher.buildDefaultEngine();
		var resolver = (ClassLoaderTemplateResolver) engine.getTemplateResolvers().iterator().next();
		assertFalse(resolver.isCacheable());
	}

	@Test void c02_buildDefaultEngine_cacheTemplatesTrue_setsResolverCacheable() {
		var dispatcher = ThymeleafDispatcher.create().cacheTemplates(true).build();
		var engine = dispatcher.buildDefaultEngine();
		var resolver = (ClassLoaderTemplateResolver) engine.getTemplateResolvers().iterator().next();
		assertTrue(resolver.isCacheable());
	}
}
