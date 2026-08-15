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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Acceptance tests for the env-driven {@code RestContext.*} default settings.
 *
 * <p>
 * 3-test triad per setting — system property set, unset (default), and {@code Settings.setGlobal} override.
 * Settings collapsed into {@link RestContextProperties} are read back through
 * {@link RestContext#getRestContextProperties()}; the two {@code Optional<String>} outliers still carried as
 * {@code @Value} fields on {@link RestContext} ({@code uriAuthority} / {@code uriContext}) are read back
 * reflectively. The downstream {@code mergeReplacedStringAttribute} pipeline applies its own DefaultConfig-driven
 * precedence on top of these values and is exercised by the existing {@code RestContext}-level tests; this class
 * scopes coverage to the env-resolution seam itself.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class RestContext_ValueAdoption_Test extends TestBase {

	@Rest
	public static class A {}

	private static final List<String> PROPS = List.of(
		"RestContext.allowedHeaderParams",
		"RestContext.allowedMethodHeaders",
		"RestContext.allowedMethodParams",
		"RestContext.disableContentParam",
		"RestContext.renderResponseStackTraces",
		"RestContext.problemDetails",
		"RestContext.virtualThreads",
		"RestContext.eagerInit",
		"RestContext.clientVersionHeader",
		"RestContext.uriRelativity",
		"RestContext.uriAuthority",
		"RestContext.uriContext",
		"RestContext.uriResolution"
	);

	@AfterEach
	void cleanup() {
		var s = Settings.get();
		for (var k : PROPS) {
			s.unsetGlobal(k);
			System.clearProperty(k);
		}
	}

	private RestContext ctx() throws Exception {
		var resource = new A();
		return new RestContext(new RestContext.Args(A.class, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	private <T> T fld(RestContext c, String name) throws Exception {
		Field f = RestContext.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T)f.get(c);
	}

	// -------------------- allowedHeaderParams (String) --------------------

	@Test
	void a01_allowedHeaderParams_set() throws Exception {
		System.setProperty("RestContext.allowedHeaderParams", "X-Custom1,X-Custom2");
		assertEquals("X-Custom1,X-Custom2", ctx().getRestContextProperties().getAllowedHeaderParams());
	}

	@Test
	void a02_allowedHeaderParams_unset() throws Exception {
		assertEquals("Accept,Content-Type", ctx().getRestContextProperties().getAllowedHeaderParams());
	}

	@Test
	void a03_allowedHeaderParams_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.allowedHeaderParams", "X-Global");
		assertEquals("X-Global", ctx().getRestContextProperties().getAllowedHeaderParams());
	}

	// -------------------- allowedMethodParams (String) --------------------

	@Test
	void b01_allowedMethodParams_set() throws Exception {
		System.setProperty("RestContext.allowedMethodParams", "GET,POST");
		assertEquals("GET,POST", ctx().getRestContextProperties().getAllowedMethodParams());
	}

	@Test
	void b02_allowedMethodParams_unset() throws Exception {
		assertEquals("HEAD,OPTIONS", ctx().getRestContextProperties().getAllowedMethodParams());
	}

	@Test
	void b03_allowedMethodParams_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.allowedMethodParams", "PUT");
		assertEquals("PUT", ctx().getRestContextProperties().getAllowedMethodParams());
	}

	// -------------------- disableContentParam (boolean) --------------------

	@Test
	void c01_disableContentParam_set() throws Exception {
		System.setProperty("RestContext.disableContentParam", "true");
		assertEquals("true", ctx().getRestContextProperties().getDisableContentParamRaw());
	}

	@Test
	void c02_disableContentParam_unset() throws Exception {
		assertEquals("false", ctx().getRestContextProperties().getDisableContentParamRaw());
	}

	@Test
	void c03_disableContentParam_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.disableContentParam", "true");
		assertEquals("true", ctx().getRestContextProperties().getDisableContentParamRaw());
	}

	// -------------------- renderResponseStackTraces (boolean) --------------------

	@Test
	void d01_renderResponseStackTraces_set() throws Exception {
		System.setProperty("RestContext.renderResponseStackTraces", "true");
		assertEquals("true", ctx().getRestContextProperties().getRenderResponseStackTracesRaw());
	}

	@Test
	void d02_renderResponseStackTraces_unset() throws Exception {
		assertEquals("false", ctx().getRestContextProperties().getRenderResponseStackTracesRaw());
	}

	@Test
	void d03_renderResponseStackTraces_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.renderResponseStackTraces", "true");
		assertEquals("true", ctx().getRestContextProperties().getRenderResponseStackTracesRaw());
	}

	// -------------------- problemDetails (boolean) --------------------

	@Test
	void e01_problemDetails_set() throws Exception {
		System.setProperty("RestContext.problemDetails", "true");
		assertEquals("true", ctx().getRestContextProperties().getProblemDetailsRaw());
	}

	@Test
	void e02_problemDetails_unset() throws Exception {
		assertEquals("false", ctx().getRestContextProperties().getProblemDetailsRaw());
	}

	@Test
	void e03_problemDetails_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.problemDetails", "true");
		assertEquals("true", ctx().getRestContextProperties().getProblemDetailsRaw());
	}

	// -------------------- clientVersionHeader (String) --------------------

	@Test
	void f01_clientVersionHeader_set() throws Exception {
		System.setProperty("RestContext.clientVersionHeader", "X-API-Version");
		assertEquals("X-API-Version", ctx().getRestContextProperties().getClientVersionHeader());
	}

	@Test
	void f02_clientVersionHeader_unset() throws Exception {
		assertEquals("Client-Version", ctx().getRestContextProperties().getClientVersionHeader());
	}

	@Test
	void f03_clientVersionHeader_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.clientVersionHeader", "X-Global-Version");
		assertEquals("X-Global-Version", ctx().getRestContextProperties().getClientVersionHeader());
	}

	// -------------------- defaultUriAuthority (Optional<String>) --------------------

	@Test
	void g01_uriAuthority_set() throws Exception {
		System.setProperty("RestContext.uriAuthority", "https://example.org");
		Optional<String> v = fld(ctx(), "defaultUriAuthority");
		assertEquals(o("https://example.org"), v);
	}

	@Test
	void g02_uriAuthority_unset() throws Exception {
		Optional<String> v = fld(ctx(), "defaultUriAuthority");
		assertTrue(v.isEmpty());
	}

	@Test
	void g03_uriAuthority_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.uriAuthority", "https://global.example.org");
		Optional<String> v = fld(ctx(), "defaultUriAuthority");
		assertEquals(o("https://global.example.org"), v);
	}

	// -------------------- defaultUriContext (Optional<String>) --------------------

	@Test
	void h01_uriContext_set() throws Exception {
		System.setProperty("RestContext.uriContext", "/api");
		Optional<String> v = fld(ctx(), "defaultUriContext");
		assertEquals(o("/api"), v);
	}

	@Test
	void h02_uriContext_unset() throws Exception {
		Optional<String> v = fld(ctx(), "defaultUriContext");
		assertTrue(v.isEmpty());
	}

	@Test
	void h03_uriContext_setGlobal() throws Exception {
		Settings.get().setGlobal("RestContext.uriContext", "/global");
		Optional<String> v = fld(ctx(), "defaultUriContext");
		assertEquals(o("/global"), v);
	}
}
