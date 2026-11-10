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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/** Serving test for {@link RowDetailMixin}. */
@SuppressWarnings({
	"unchecked", // JSON maps
	"resource" // Mock client held in a static field
})
class RowDetailMixin_Test extends TestBase {

	public static class Pet {
		private final String id;
		public Pet(String id) { this.id = id; }
		public String getId() { return id; }
		public String getName() { return "Rex-" + id; }
		public String getEmail() { return "x@y.z"; }
		@org.apache.juneau.commons.bean.BeanIgnore
		public String getHidden() { return "h"; }
	}

	public static class Owner {
		public String getId() { return "o"; }
		public String getName() { return "n"; }
	}

	static final RowDetail PET = RowDetail.fromBean(Pet.class, id -> "p1".equals(id) ? new Pet(id) : null)
		.title("{name}")
		.section("Info", "id", "name")
		.tab(DetailTab.fields("More", "email").count(id -> 7));

	static final RowDetail OWNER = RowDetail.fromBean(Owner.class, id -> new Owner()).key("owner");

	@Rest
	public static class R extends BasicRestServlet implements RowDetailMixin {
		private static final long serialVersionUID = 1L;
		@Override public Collection<RowDetail> rowDetails() { return List.of(PET, OWNER); }
	}

	static final RowDetail SHARED = RowDetail.fromBean(Pet.class, id -> "p1".equals(id) ? new Pet(id) : null).key("shared");

	static final RowDetail BOOM = RowDetail.fromBean(Pet.class, id -> { throw new IllegalStateException("lookup failed"); }).key("boom")
		.tab(DetailTab.fields("Info", "name").count(id -> { throw new IllegalStateException("count failed"); }));

	static final RowDetail BADCOUNT = RowDetail.fromBean(Pet.class, Pet::new).key("badcount")
		.tab(DetailTab.fields("Info", "name").count(id -> { throw new IllegalStateException("count failed"); }));

	@Rest
	public static class R2 extends BasicRestServlet implements RowDetailMixin {
		private static final long serialVersionUID = 1L;
		@Override public Collection<RowDetail> rowDetails() { return List.of(SHARED, BOOM, BADCOUNT); }
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);
	private static final MockRestClient c2 = MockRestClient.buildLax(R2.class);

	private static void assertSame404(org.apache.juneau.rest.client.classic.RestResponse r) throws Exception {
		r.assertStatus(404);
		assertEquals("no-store", r.getStringHeader("Cache-Control").orElse(null));
		assertEquals("{}", r.getContent().asString().replaceAll("\\s", ""));
	}

	@Test void a01_detailShape() throws Exception {
		var body = c.get("/row-detail/pet/p1").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var m = Json.to(body, Map.class);
		assertEquals("1", m.get("contractVersion"));
		var f = (Map<String,Object>) m.get("fields");
		assertEquals("Rex-p1", f.get("name"));
		assertNotNull(m.get("meta"));
		assertEquals(7, ((Map<String,Object>) m.get("counts")).get("more"));
	}

	@Test void a02_tabShape() throws Exception {
		var body = c.get("/row-detail/pet/p1/tab/more").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var f = (Map<String,Object>) Json.to(body, Map.class).get("fields");
		assertEquals("x@y.z", f.get("email"));
		assertNull(f.get("name"));
	}

	@Test void a03_countsShape() throws Exception {
		var body = c.get("/row-detail/pet/p1/counts").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		assertEquals(7, ((Map<String,Object>) Json.to(body, Map.class).get("counts")).get("more"));
	}

	@Test void a04_identical404() throws Exception {
		for (var p : List.of("/row-detail/nope/p1", "/row-detail/pet/zzz", "/row-detail/pet/p1/tab/nope",
				"/row-detail/pet/..", "/row-detail/pet/bad%20id", "/row-detail/pet/p1/tab/Bad_Id"))
			assertSame404(c.get(p).header("Accept", "application/json").run());
	}

	@Test void a05_unnamedPropertiesNotServed() throws Exception {
		var body = c.get("/row-detail/pet/p1/tab/more").header("Accept", "application/json").run().getContent().asString();
		assertFalse(body.contains("Rex"));
	}

	@Test void a06_duplicateKey_failsStartup() {
		RowDetailMixin mixin = () -> List.of(PET, RowDetail.fromBean(Pet.class, id -> null));
		assertThrows(IllegalStateException.class, mixin::rowDetailIndex);
	}

	@Test void z01_oneDetailOnTwoTables_servesFieldsOfBoth() throws Exception {
		TableSpec.create("ta").dataUrl("/a").columns(Column.create("id"), Column.create("name")).detail(SHARED).toCardBody();
		TableSpec.create("tb").dataUrl("/b").columns(Column.create("id"), Column.create("email")).detail(SHARED).toCardBody();
		var body = c2.get("/row-detail/shared/p1").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var f = (Map<String,Object>)Json.to(body, Map.class).get("fields");
		assertTrue(f.containsKey("name"), body);
		assertTrue(f.containsKey("email"), body);
	}

	@Test void z02_throwingLookup_isTheSameEmpty404() throws Exception {
		assertSame404(c2.get("/row-detail/boom/p1").header("Accept", "application/json").run());
		assertSame404(c2.get("/row-detail/boom/p1/counts").header("Accept", "application/json").run());
		assertSame404(c2.get("/row-detail/boom/p1/tab/info").header("Accept", "application/json").run());
	}

	@Test void z03_throwingCounter_dropsItsCount_notTheDetail() throws Exception {
		var body = c2.get("/row-detail/badcount/p1").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var counts = (Map<String,Object>)Json.to(body, Map.class).get("counts");
		assertFalse(counts.containsKey("info"), body);
	}
}
