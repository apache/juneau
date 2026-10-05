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
package org.apache.juneau;

import static org.apache.juneau.commons.reflect.Visibility.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Tests binding of non-public (private / package-private) records.
 */
class NonPublicRecord_Test extends TestBase {

	public record PublicRec(String name, int age) {}
	record PackageRec(String name, int age) {}
	private record PrivateRec(String name, int age) {}
	protected record ProtectedRec(String name, int age) {}

	static class Hidden {
		public record NestedPublicRec(String name, int age) {}
	}

	private static Json5Serializer.Builder sb() {
		return Json5Serializer.create();
	}

	private static Json5Parser.Builder pb() {
		return Json5Parser.create();
	}

	@Test void a01_publicRecord_default() throws Exception {
		assertString("{age:1,name:'a'}", Json5Serializer.DEFAULT.write(new PublicRec("a", 1)));
		assertBean(Json5Parser.DEFAULT.read("{name:'a',age:1}", PublicRec.class), "name,age", "a,1");
	}

	@Test void a02_privateRecord_default_notBound() throws Exception {
		var s = Json5Serializer.DEFAULT.write(new PrivateRec("a", 1));
		assertFalse(s.startsWith("{"), s);
	}

	@Test void a03_packageRecord_default_notBound() throws Exception {
		var s = Json5Serializer.DEFAULT.write(new PackageRec("a", 1));
		assertFalse(s.startsWith("{"), s);
	}

	@Test void b01_privateRecord_roundTrip() throws Exception {
		var s = sb().beanClassVisibility(PRIVATE).beanConstructorVisibility(PRIVATE).build();
		var p = pb().beanClassVisibility(PRIVATE).beanConstructorVisibility(PRIVATE).build();
		var json = s.write(new PrivateRec("a", 1));
		assertString("{age:1,name:'a'}", json);
		assertBean(p.read(json, PrivateRec.class), "name,age", "a,1");
	}

	@Test void b02_packageRecord_roundTrip() throws Exception {
		var s = sb().beanClassVisibility(PRIVATE).beanConstructorVisibility(PRIVATE).build();
		var p = pb().beanClassVisibility(PRIVATE).beanConstructorVisibility(PRIVATE).build();
		var json = s.write(new PackageRec("a", 1));
		assertString("{age:1,name:'a'}", json);
		assertBean(p.read(json, PackageRec.class), "name,age", "a,1");
	}

	@Test void b03_protectedRecord_roundTrip() throws Exception {
		var s = sb().beanClassVisibility(PROTECTED).beanConstructorVisibility(PROTECTED).build();
		var p = pb().beanClassVisibility(PROTECTED).beanConstructorVisibility(PROTECTED).build();
		var json = s.write(new ProtectedRec("a", 1));
		assertString("{age:1,name:'a'}", json);
		assertBean(p.read(json, ProtectedRec.class), "name,age", "a,1");
	}

	@Test void b04_privateRecord_classVisibilityOnly() throws Exception {
		// Class visibility alone is enough; a record's canonical constructor is implied by the record declaration.
		var s = sb().beanClassVisibility(PRIVATE).build();
		var p = pb().beanClassVisibility(PRIVATE).build();
		var json = s.write(new PrivateRec("a", 1));
		assertString("{age:1,name:'a'}", json);
		assertBean(p.read(json, PrivateRec.class), "name,age", "a,1");
	}

	@Test void b05_publicRecordInPackagePrivateClass_roundTrip() throws Exception {
		var s = sb().beanClassVisibility(PRIVATE).build();
		var p = pb().beanClassVisibility(PRIVATE).build();
		var json = s.write(new Hidden.NestedPublicRec("a", 1));
		assertString("{age:1,name:'a'}", json);
		assertBean(p.read(json, Hidden.NestedPublicRec.class), "name,age", "a,1");
	}
}
