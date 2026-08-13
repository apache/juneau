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
package org.apache.juneau.rest.server.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import freemarker.cache.*;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;

/**
 * Unit tests for {@link FreemarkerDispatcher#render(String, RestRequest, RestResponse) render(...)} and
 * {@link FreemarkerDispatcher#resolveConfiguration(RestRequest) resolveConfiguration(...)}, driven against
 * mocked {@link RestRequest}/{@link RestResponse} (plain non-final classes, straightforward to mock without
 * a servlet container). The raw-render happy path is already exercised end-to-end via {@code MockRest} in
 * {@code FreemarkerMixin_MockRest_Test}; this class fills in the branches that path doesn't reach (missing
 * template, engine failure, an already-set Content-Type, a {@code null} path segment, and a
 * pre-registered {@code Configuration} bean).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // BasicBeanStore instances are short-lived in-memory test fixtures backed by a Map, consumed synchronously by the mocked request; nothing external to leak.
})
class FreemarkerDispatcher_Test extends TestBase {

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

	private static Configuration stringLoaderConfig(String templateName, String templateSource) {
		var cfg = new Configuration(Configuration.VERSION_2_3_34);
		var loader = new StringTemplateLoader();
		loader.putTemplate(templateName, templateSource);
		cfg.setTemplateLoader(loader);
		return cfg;
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: resolveConfiguration
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_resolveConfiguration_prefersRegisteredBean() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var registered = stringLoaderConfig("hello", "Hi!");
		beanStore.addBean(Configuration.class, registered);

		assertSame(registered, dispatcher.resolveConfiguration(mockRequest(beanStore)));
	}

	@Test void a02_resolveConfiguration_secondCallReusesCachedDefault() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var req = mockRequest(new BasicBeanStore());

		// First call builds+caches the bridge default (no Configuration bean registered); the second
		// call's outer null-check takes the "already built" branch instead of re-entering the lock.
		var c1 = dispatcher.resolveConfiguration(req);
		var c2 = dispatcher.resolveConfiguration(req);
		assertNotNull(c1);
		assertSame(c1, c2);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: render
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_render_nullPathTreatedAsEmpty() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(Configuration.class, stringLoaderConfig("", "root template"));
		var res = mockResponse();

		dispatcher.render(null, mockRequest(beanStore), res);

		verify(res).setHeader("Content-Type", FreemarkerViewRenderer.DEFAULT_CONTENT_TYPE);
	}

	@Test void b02_render_explicitContentTypeNotOverridden() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		beanStore.addBean(Configuration.class, stringLoaderConfig("hello", "Hi!"));
		var res = mockResponse();
		when(res.containsHeader("Content-Type")).thenReturn(true);

		dispatcher.render("hello", mockRequest(beanStore), res);

		verify(res, never()).setHeader(eq("Content-Type"), any());
	}

	@Test void b03_render_missingTemplate_propagatesIOException() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		// Empty StringTemplateLoader -> getTemplate("does-not-exist") throws TemplateNotFoundException,
		// an IOException subclass, which render()'s "catch (IOException | BasicHttpException ex)" rethrows as-is.
		beanStore.addBean(Configuration.class, stringLoaderConfig("hello", "Hi!"));

		assertThrows(IOException.class,
			() -> dispatcher.render("does-not-exist", mockRequest(beanStore), mockResponse()));
	}

	@Test void b04_render_linkageErrorFromEngine_wrapsWithNoEngineDiagnostic() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var cfg = mock(Configuration.class);
		when(cfg.getTemplate("hello")).thenThrow(new NoClassDefFoundError("freemarker.core.Environment"));
		beanStore.addBean(Configuration.class, cfg);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("FreeMarker engine"));
	}

	@Test void b05_render_templateRenderThrowsRuntimeException_wrapsInternalServerError() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var beanStore = new BasicBeanStore();
		var cfg = mock(Configuration.class);
		when(cfg.getTemplate("hello")).thenThrow(new RuntimeException("boom"));
		beanStore.addBean(Configuration.class, cfg);

		var ex = assertThrows(InternalServerError.class,
			() -> dispatcher.render("hello", mockRequest(beanStore), mockResponse()));
		assertTrue(ex.getMessage().contains("FreeMarker render failed"));
	}

	@Test void b06_render_traversalEscape_throwsForbidden() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().basePath("/templates/").build();
		var beanStore = new BasicBeanStore();

		assertThrows(Forbidden.class,
			() -> dispatcher.render("../../../etc/passwd", mockRequest(beanStore), mockResponse()));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: buildDefaultConfiguration cacheTemplates flag
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_buildDefaultConfiguration_cacheTemplatesFalse_disablesUpdateDelay() {
		var dispatcher = FreemarkerDispatcher.create().cacheTemplates(false).build();
		var cfg = dispatcher.buildDefaultConfiguration();
		assertEquals(0L, cfg.getTemplateUpdateDelayMilliseconds());
	}

	@Test void c02_buildDefaultConfiguration_cacheTemplatesTrue_setsMaxUpdateDelay() {
		var dispatcher = FreemarkerDispatcher.create().cacheTemplates(true).build();
		var cfg = dispatcher.buildDefaultConfiguration();
		assertEquals(Long.MAX_VALUE, cfg.getTemplateUpdateDelayMilliseconds());
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section D: buildDefaultConfiguration object-wrapper / exposeFields behavior
	 * ---------------------------------------------------------------------------------------- */

	/** View-model DTO with a public field and no getter — the shape that bit the dogfooded consumer. */
	public static class D_FieldOnlyBean {
		public String name = "Alice";
	}

	private static String renderToString(Configuration cfg, String template, Object bean) throws Exception {
		var loader = new StringTemplateLoader();
		loader.putTemplate("t", template);
		cfg.setTemplateLoader(loader);
		var sw = new StringWriter();
		cfg.getTemplate("t").process(Map.of("bean", bean), sw);
		return sw.toString();
	}

	@Test void d01_buildDefaultConfiguration_exposeFieldsDefaultTrue_rendersPublicFieldValue() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().build();
		var cfg = dispatcher.buildDefaultConfiguration();

		assertEquals("Alice", renderToString(cfg, "${bean.name}", new D_FieldOnlyBean()));
	}

	@Test void d02_exposeFieldsFalse_publicFieldIsInvisibleToTemplates() throws Exception {
		var dispatcher = FreemarkerDispatcher.create().exposeFields(false).build();
		var cfg = dispatcher.buildDefaultConfiguration();

		// With exposeFields=false (FreeMarker's own version-default behavior), the public field
		// isn't visible as a bean property, so a defaulted reference falls through silently.
		assertEquals("MISSING", renderToString(cfg, "${bean.name!'MISSING'}", new D_FieldOnlyBean()));
	}

	@Test void d03_objectWrapperOverride_takesPrecedenceOverExposeFields() {
		var b = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_34);
		b.setExposeFields(false);
		var custom = b.build();
		var dispatcher = FreemarkerDispatcher.create().exposeFields(true).objectWrapper(custom).build();

		var cfg = dispatcher.buildDefaultConfiguration();

		assertSame(custom, cfg.getObjectWrapper());
	}

	@Test void d04_exposeFieldsAndObjectWrapperDefaultToTrueAndNull() {
		var dispatcher = FreemarkerDispatcher.create().build();
		assertTrue(dispatcher.isExposeFields());
		assertNull(dispatcher.getObjectWrapper());
	}
}
