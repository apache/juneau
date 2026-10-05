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
package org.apache.juneau.commons.bean;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Tests the module-opens diagnostic raised when a non-public record's canonical constructor can't be made accessible.
 */
public class BeanMeta_RecordAccess_Test extends TestBase {

	record PackageRec(String name) {}
	public record PublicRec(String name) {}

	static class Hidden {
		public record NestedPublicRec(String name) {}
	}

	private static ConstructorInfo ctor(Class<?> c) {
		return ClassInfo.of(c).getDeclaredConstructors().get(0);
	}

	@Test void a01_nonPublicRecord_notOpened_throwsClearMessage() {
		var ci = ClassInfo.of(PackageRec.class);
		var e = assertThrows(BeanRuntimeException.class, () -> BeanMeta.makeRecordConstructorAccessible(ci, ctor(PackageRec.class), x -> false));
		var m = e.getMessage();
		assertTrue(m.contains("PackageRec"), m);
		assertTrue(m.contains("opens org.apache.juneau.commons.bean to"), m);
		assertTrue(m.contains("--add-opens"), m);
	}

	@Test void a02_nonPublicRecord_opened_returnsConstructor() {
		var ci = ClassInfo.of(PackageRec.class);
		var c = ctor(PackageRec.class);
		assertSame(c, BeanMeta.makeRecordConstructorAccessible(ci, c, x -> true));
	}

	@Test void a03_publicRecord_failedAccess_doesNotThrow() {
		var ci = ClassInfo.of(PublicRec.class);
		var c = ctor(PublicRec.class);
		assertSame(c, BeanMeta.makeRecordConstructorAccessible(ci, c, x -> false));
	}

	@Test void a03b_publicRecord_alwaysAttemptsSetAccessible() {
		var calls = new java.util.concurrent.atomic.AtomicInteger();
		var c = ctor(PublicRec.class);
		BeanMeta.makeRecordConstructorAccessible(ClassInfo.of(PublicRec.class), c, x -> { calls.incrementAndGet(); return true; });
		assertEquals(1, calls.get());
	}

	@Test void a05_publicRecordInNonPublicClass_failedAccess_throws() {
		// Not effectively public, so a failed setAccessible is reported.
		var ci = ClassInfo.of(Hidden.NestedPublicRec.class);
		assertThrows(BeanRuntimeException.class, () -> BeanMeta.makeRecordConstructorAccessible(ci, ctor(Hidden.NestedPublicRec.class), x -> false));
	}

	@Test void a06_publicRecordInNonPublicClass_bindsNormally() {
		var ci = ClassInfo.of(Hidden.NestedPublicRec.class);
		assertNotNull(BeanMeta.makeRecordConstructorAccessible(ci, ctor(Hidden.NestedPublicRec.class)));
	}

	@Test void a04_realSetAccessible_nonPublicRecord_succeedsOnClasspath() {
		var ci = ClassInfo.of(PackageRec.class);
		assertNotNull(BeanMeta.makeRecordConstructorAccessible(ci, ctor(PackageRec.class)));
	}
}
