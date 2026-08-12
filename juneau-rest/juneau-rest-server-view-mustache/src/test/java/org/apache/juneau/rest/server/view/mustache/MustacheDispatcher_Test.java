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
package org.apache.juneau.rest.server.view.mustache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import com.github.mustachejava.*;

/**
 * Unit tests for {@link MustacheDispatcher#render(String, RestRequest, RestResponse) render(...)} and
 * {@link MustacheDispatcher#resolveMustacheFactory(RestRequest) resolveMustacheFactory(...)}, driven against
 * mocked {@link RestRequest}/{@link RestResponse}/{@link MustacheFactory}/{@link Mustache} (the latter two
 * are plain interfaces, trivial to mock without a servlet container or real template resources). The
 * raw-render happy path is already exercised end-to-end via {@code MockRest} in
 * {@code MustacheMixin_MockRest_Test}; this class fills in the branches that path doesn't reach (registered
 * factory bean, engine failure, an already-set Content-Type, a {@code null} path segment, and a
 * writer-level {@code IOException}).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // BasicBeanStore instances are short-lived in-memory test fixtures backed by a Map, consumed synchronously by the mocked request; nothing external to leak.
})
class MustacheDispatcher_Test extends TestBase {

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

	private static MustacheFactory mockFactory(Mustache mustache) {
		var factory = mock(MustacheFactory.class);
		when(factory.compile(anyString())).thenReturn(mustache);
		return factory;
	}

	private static Mustache noopMustache() {
		var mustache = mock(Mustache.class);
		when(mustache.execute(any(Writer.class), anyMap())).thenAnswer(inv -> inv.getArgument(0));
		return mustache;
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: resolveMustacheFactory
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_resolveMustacheFactory_prefersRegisteredBean() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var registered = mockFactory(noopMustache());
		beanStore.addBean(MustacheFactory.class, registered);

		assertSame(registered, dispatcher.resolveMustacheFactory(mockRequest(beanStore)));
	}

	@Test void a02_resolveMustacheFactory_secondCallReusesCachedDefault() {
		var dispatcher = MustacheDispatcher.create().build();
		var req = mockRequest(new BasicBeanStore());

		// First call builds+caches the bridge default (no MustacheFactory bean registered); the second
		// call's outer null-check takes the "already built" branch instead of re-entering the lock.
		var f1 = dispatcher.resolveMustacheFactory(req);
		var f2 = dispatcher.resolveMustacheFactory(req);
		assertNotNull(f1);
		assertSame(f1, f2);
	}

	@Test void a03_resolveMustacheFactory_cacheTemplatesFalse_rebuildsEveryCall() {
		var dispatcher = MustacheDispatcher.create().cacheTemplates(false).build();
		var req = mockRequest(new BasicBeanStore());

		var f1 = dispatcher.resolveMustacheFactory(req);
		var f2 = dispatcher.resolveMustacheFactory(req);
		assertNotNull(f1);
		assertNotNull(f2);
		assertNotSame(f1, f2,
			"cacheTemplates(false) must bypass the lazy singleton and rebuild a fresh factory (with an empty compile-cache) on every call");
	}

	@Test void a04_resolveMustacheFactory_registeredBeanIgnoresCacheTemplatesFlag() throws Exception {
		var dispatcher = MustacheDispatcher.create().cacheTemplates(false).build();
		var beanStore = new BasicBeanStore();
		var registered = mockFactory(noopMustache());
		beanStore.addBean(MustacheFactory.class, registered);
		var req = mockRequest(beanStore);

		assertSame(registered, dispatcher.resolveMustacheFactory(req));
		assertSame(registered, dispatcher.resolveMustacheFactory(req),
			"A user-supplied MustacheFactory bean is returned as-is regardless of cacheTemplates");
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: render
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_render_nullPathTreatedAsEmpty() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var mustache = noopMustache();
		beanStore.addBean(MustacheFactory.class, mockFactory(mustache));
		var res = mockResponse();

		dispatcher.render(null, mockRequest(beanStore), res);

		verify(res).setHeader("Content-Type", MustacheViewRenderer.DEFAULT_CONTENT_TYPE);
	}

	@Test void b02_render_explicitContentTypeNotOverridden() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(MustacheFactory.class, mockFactory(noopMustache()));
		var res = mockResponse();
		when(res.containsHeader("Content-Type")).thenReturn(true);

		dispatcher.render("hello", mockRequest(beanStore), res);

		verify(res, never()).setHeader(eq("Content-Type"), any());
	}

	@Test void b03_render_writerIOException_propagatesAsIs() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(MustacheFactory.class, mockFactory(noopMustache()));
		var res = mock(RestResponse.class);
		when(res.getWriter()).thenThrow(new IOException("broken pipe"));

		var ex = assertThrows(IOException.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), res));
		assertEquals("broken pipe", ex.getMessage());
	}

	@Test void b04_render_linkageErrorFromEngine_wrapsWithNoEngineDiagnostic() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var factory = mock(MustacheFactory.class);
		when(factory.compile(anyString())).thenThrow(new NoClassDefFoundError("com.github.mustachejava.Mustache"));
		beanStore.addBean(MustacheFactory.class, factory);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("Mustache engine"));
	}

	@Test void b05_render_templateRenderThrowsRuntimeException_wrapsInternalServerError() throws Exception {
		var dispatcher = MustacheDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var factory = mock(MustacheFactory.class);
		when(factory.compile(anyString())).thenThrow(new RuntimeException("boom"));
		beanStore.addBean(MustacheFactory.class, factory);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("Mustache render failed"));
	}

	@Test void b06_render_traversalEscape_throwsForbidden() throws Exception {
		var dispatcher = MustacheDispatcher.create().basePath("/templates/").build();
		var beanStore = new BasicBeanStore();

		assertThrows(Forbidden.class,
			() -> dispatcher.render("../../../etc/passwd", mockRequest(beanStore), mockResponse()));
	}
}
