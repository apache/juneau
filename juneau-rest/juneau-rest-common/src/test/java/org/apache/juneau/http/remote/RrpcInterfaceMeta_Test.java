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
package org.apache.juneau.http.remote;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RrpcInterfaceMeta}.
 */
class RrpcInterfaceMeta_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Compat constructor -- isInterface() check on a non-interface class
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_compatCtor_notAnInterface_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "is not an interface", () -> new RrpcInterfaceMeta(String.class, "http://x"));
	}

	@Test void a02_compatCtor_includesUnannotatedMethods() {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertEquals(1, m.getMethodMetas().size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// of(Class) -- version header injection (buildHeaders)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_versioned_defaultVersionHeaderName() {
		var m = RrpcInterfaceMeta.of(VersionedIface.class);
		var found = m.getHeaders().stream().filter(e -> e.getKey().equals("Client-Version")).findFirst();
		assertTrue(found.isPresent());
		assertEquals("2.0", found.get().getValue());
	}

	@Test void b02_of_versioned_customVersionHeaderName() {
		var m = RrpcInterfaceMeta.of(CustomVersionHeaderIface.class);
		var found = m.getHeaders().stream().filter(e -> e.getKey().equals("X-Api-Version")).findFirst();
		assertTrue(found.isPresent());
		assertEquals("3.1", found.get().getValue());
	}

	@Test void b03_of_unversioned_noVersionHeader() {
		var m = RrpcInterfaceMeta.of(PlainRemoteIface.class);
		assertTrue(m.getHeaders().isEmpty());
	}

	@Test void b04_of_headerEntry_noDelimiter_isSkipped() {
		var m = RrpcInterfaceMeta.of(MalformedHeaderIface.class);
		// "NoColonHere" has no ':' delimiter and is silently dropped; only the well-formed entry survives.
		assertEquals(1, m.getHeaders().size());
		assertEquals("Good", m.getHeaders().get(0).getKey());
	}

	//------------------------------------------------------------------------------------------------------------------
	// getMethodMetaByPath(null)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getMethodMetaByPath_null_returnsNull() {
		var m = RrpcInterfaceMeta.of(PlainRemoteIface.class);
		assertNull(m.getMethodMetaByPath(null));
	}

	@Test void c02_getMethodMetaByPath_found() {
		var m = RrpcInterfaceMeta.of(PlainRemoteIface.class);
		assertEquals(1, m.getMethodsByPath().size());
		var path = m.getMethodsByPath().keySet().iterator().next();
		assertNotNull(m.getMethodMetaByPath(path));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RemoteOp -- value()/method()/path() inference (buildRemoteOpMeta)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_remoteOp_value_noSpace_methodOnly() {
		var m = RrpcInterfaceMeta.of(RemoteOpIface.class);
		var meta = m.getMethodMeta(getMethod(RemoteOpIface.class, "plainGet"));
		assertEquals("GET", meta.getHttpMethod());
		assertEquals("plainGet", meta.getPath());
	}

	@Test void d02_remoteOp_value_withSpace_methodAndPath() {
		var m = RrpcInterfaceMeta.of(RemoteOpIface.class);
		var meta = m.getMethodMeta(getMethod(RemoteOpIface.class, "putWidget"));
		assertEquals("PUT", meta.getHttpMethod());
		assertEquals("widgets/1", meta.getPath());
	}

	@Test void d03_remoteOp_inferBoth_fromMethodName() {
		var m = RrpcInterfaceMeta.of(RemoteOpIface.class);
		var meta = m.getMethodMeta(getMethod(RemoteOpIface.class, "getFoo"));
		assertEquals("GET", meta.getHttpMethod());
		assertEquals("foo", meta.getPath());
	}

	@Test void d04_remoteOp_explicitPathAttr_overridesValue() {
		var m = RrpcInterfaceMeta.of(RemoteOpIface.class);
		var meta = m.getMethodMeta(getMethod(RemoteOpIface.class, "explicitPath"));
		assertEquals("POST", meta.getHttpMethod());
		assertEquals("custom/path", meta.getPath());
	}

	@Test void d05_remoteOp_invalidMethod_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid @RemoteOp method",
			() -> RrpcInterfaceMeta.of(InvalidMethodIface.class));
	}

	private static java.lang.reflect.Method getMethod(Class<?> c, String name) {
		for (var m : c.getMethods())
			if (m.getName().equals(name))
				return m;
		throw new AssertionError("Method not found: " + name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// headerList() -- genuinely engine-specific member: one-time build-time warning on the NG engine
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_headerList_set_emitsOneTimeWarning() {
		var records = new java.util.concurrent.CopyOnWriteArrayList<java.util.logging.LogRecord>();
		var handler = new java.util.logging.Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { records.add(r); }
			@Override public void flush() {}
			@Override public void close() {}
		};
		var log = java.util.logging.Logger.getLogger("org.apache.juneau.http.remote");
		log.addHandler(handler);
		try {
			// First resolution warns; subsequent resolutions are de-duplicated (one-time).
			RrpcInterfaceMeta.of(HeaderListIface.class);
			RrpcInterfaceMeta.of(HeaderListIface.class);
		} finally {
			log.removeHandler(handler);
		}
		var matches = records.stream().filter(r -> r.getMessage().contains("headerList")).count();
		assertEquals(1L, matches);
		var msg = records.stream().filter(r -> r.getMessage().contains("headerList")).findFirst().get().getMessage();
		assertTrue(msg.contains(HeaderListIface.class.getName()));
		assertTrue(msg.contains("not honored by the next-generation"));
	}

	@Test void f02_headerList_unset_noWarning() {
		var records = new java.util.concurrent.CopyOnWriteArrayList<java.util.logging.LogRecord>();
		var handler = new java.util.logging.Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { records.add(r); }
			@Override public void flush() {}
			@Override public void close() {}
		};
		var log = java.util.logging.Logger.getLogger("org.apache.juneau.http.remote");
		log.addHandler(handler);
		try {
			RrpcInterfaceMeta.of(NoHeaderListIface.class);
		} finally {
			log.removeHandler(handler);
		}
		assertEquals(0L, records.stream().filter(r -> r.getMessage().contains("headerList")).count());
	}

	/** Stub supplier type used only to give {@code headerList()} a non-default value. */
	public static class HeaderListStub {}

	//------------------------------------------------------------------------------------------------------------------
	// toString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_toString() {
		var m = RrpcInterfaceMeta.of(PlainRemoteIface.class);
		var s = m.toString();
		assertTrue(s.contains("PlainRemoteIface"));
		assertTrue(s.contains("methods=1"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//------------------------------------------------------------------------------------------------------------------

	/** No @Remote annotation at all -- exercised only via the compat (Class,String) constructor. */
	public interface PlainIface {
		String get();
	}

	@Remote(version = "2.0")
	public interface VersionedIface {
		String get();
	}

	@Remote(version = "3.1", versionHeader = "X-Api-Version")
	public interface CustomVersionHeaderIface {
		String get();
	}

	@Remote
	public interface PlainRemoteIface {
		@RemoteGet String get();
	}

	@Remote(headers = {"NoColonHere", "Good: value"})
	public interface MalformedHeaderIface {
		String get();
	}

	@Remote
	public interface RemoteOpIface {
		@RemoteOp("GET") String plainGet();
		@RemoteOp("PUT /widgets/1") String putWidget();
		@RemoteOp String getFoo();
		@RemoteOp(method = "POST", path = "/custom/path") String explicitPath();
	}

	@Remote
	public interface InvalidMethodIface {
		@RemoteOp(method = "BOGUS", path = "/x") String bogus();
	}

	@Remote(headerList = HeaderListStub.class)
	public interface HeaderListIface {
		@RemoteGet String get();
	}

	@Remote
	public interface NoHeaderListIface {
		@RemoteGet String get();
	}
}
