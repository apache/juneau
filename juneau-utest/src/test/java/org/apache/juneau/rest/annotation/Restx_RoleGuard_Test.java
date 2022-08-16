// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.annotation;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Restx_RoleGuard_Test {

	private static RestOperation[] ops(RestOperation...ops) {
		return ops;
	}

	private static RestOperation op(String method, String url) {
		return RestOperation.of(method, url);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guard on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A1 {
		@RestOp
		public String a() {
			return "OK";
		}
		@RestGet
		public String b() {
			return "OK";
		}
		@RestPut
		public String c() {
			return "OK";
		}
		@RestPost
		public String d() {
			return "OK";
		}
		@RestDelete
		public String e() {
			return "OK";
		}
	}

	@Test
	public void a01a_onClass_simple() throws Exception {
		MockRestClient a1 = MockRestClient.buildLax(A1.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			a1.request(op).roles("foo").run().assertStatus(200);
			a1.request(op).roles("foo","bar").run().assertStatus(200);
			a1.request(op).roles("bar","foo").run().assertStatus(200);
			a1.request(op).run().assertStatus(403);
			a1.request(op).roles("foo2").run().assertStatus(403);
			a1.request(op).roles("foo2","bar").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guard on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A2 {
		@RestOp(roleGuard="foo")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void a02a_onMethod_simple() throws Exception {
		MockRestClient a2 = MockRestClient.buildLax(A2.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			a2.request(op).roles("foo").run().assertStatus(200);
			a2.request(op).roles("foo","bar").run().assertStatus(200);
			a2.request(op).roles("bar","foo").run().assertStatus(200);
			a2.request(op).run().assertStatus(403);
			a2.request(op).roles("foo2").run().assertStatus(403);
			a2.request(op).roles("foo2","bar").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guards on class and method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A3 {
		@RestOp(roleGuard="bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void a03a_onBoth_simple() throws Exception {
		MockRestClient a3 = MockRestClient.buildLax(A3.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			a3.request(op).roles("foo","bar").run().assertStatus(200);
			a3.request(op).roles("bar","foo").run().assertStatus(200);
			a3.request(op).roles("bar","foo","baz").run().assertStatus(200);
			a3.request(op).run().assertStatus(403);
			a3.request(op).roles("foo").run().assertStatus(403);
			a3.request(op).roles("bar").run().assertStatus(403);
			a3.request(op).roles("foo2").run().assertStatus(403);
			a3.request(op).roles("foo2","bar").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guards on class and method, inherited
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A4a {
		@RestOp(roleGuard="bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="bar")
		public String e() {
			return "OK";
		}
	}

	@Rest(roleGuard="baz")
	public static class A4b extends A4a {
		@Override
		@RestOp(roleGuard="qux")
		public String a() {
			return "OK";
		}
		@Override
		@RestGet(roleGuard="qux")
		public String b() {
			return "OK";
		}
		@Override
		@RestPut(roleGuard="qux")
		public String c() {
			return "OK";
		}
		@Override
		@RestPost(roleGuard="qux")
		public String d() {
			return "OK";
		}
		@Override
		@RestDelete(roleGuard="qux")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void a04a_inheritence_simple() throws Exception {
		MockRestClient a4 = MockRestClient.buildLax(A4b.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			a4.request(op).roles("foo","bar","baz","qux").run().assertStatus(200);
			a4.request(op).roles("foo","bar","baz","qux","quux").run().assertStatus(200);
			a4.request(op).roles("foo","bar","baz").run().assertStatus(403);
			a4.request(op).roles("foo","bar","qux").run().assertStatus(403);
			a4.request(op).roles("foo","baz","qux").run().assertStatus(403);
			a4.request(op).roles("bar","baz","qux").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Rest(roleGuard), multiple guards on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B1 {
		@RestOp
		public String a() {
			return "OK";
		}
		@RestGet
		public String b() {
			return "OK";
		}
		@RestPut
		public String c() {
			return "OK";
		}
		@RestPost
		public String d() {
			return "OK";
		}
		@RestDelete
		public String e() {
			return "OK";
		}
	}

	@Rest(roleGuard="foo,bar")
	public static class B1a extends B1 {}

	@Test
	public void b01a_orsWithComma_pass() throws Exception {
		MockRestClient b1a = MockRestClient.buildLax(B1a.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1a.request(op).roles("foo").run().assertStatus(200);
			b1a.request(op).roles("bar").run().assertStatus(200);
			b1a.request(op).roles("foo","bar").run().assertStatus(200);
			b1a.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1a.request(op).roles().run().assertStatus(403);
			b1a.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo | bar")
	public static class B1b extends B1 {}

	@Test
	public void b01b_orsWithSinglePipe_pass() throws Exception {
		MockRestClient b1b = MockRestClient.buildLax(B1b.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1b.request(op).roles("foo").run().assertStatus(200);
			b1b.request(op).roles("bar").run().assertStatus(200);
			b1b.request(op).roles("foo","bar").run().assertStatus(200);
			b1b.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1b.request(op).roles().run().assertStatus(403);
			b1b.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo || bar")
	public static class B1c extends B1 {}

	@Test
	public void b01c_orsWithDoublePipe_pass() throws Exception {
		MockRestClient b1c = MockRestClient.buildLax(B1c.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1c.request(op).roles("foo").run().assertStatus(200);
			b1c.request(op).roles("bar").run().assertStatus(200);
			b1c.request(op).roles("foo","bar").run().assertStatus(200);
			b1c.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1c.request(op).roles().run().assertStatus(403);
			b1c.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo & bar")
	public static class B1d extends B1 {}

	@Test
	public void b01d_andsWithSingleAmp_pass() throws Exception {
		MockRestClient b1d = MockRestClient.buildLax(B1d.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1d.request(op).roles("foo","bar").run().assertStatus(200);
			b1d.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1d.request(op).roles().run().assertStatus(403);
			b1d.request(op).roles("foo").run().assertStatus(403);
			b1d.request(op).roles("bar").run().assertStatus(403);
			b1d.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo && bar")
	public static class B1e extends B1 {}

	@Test
	public void b01e_andsWithDoubleAmp_pass() throws Exception {
		MockRestClient b1e = MockRestClient.buildLax(B1e.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1e.request(op).roles("foo","bar").run().assertStatus(200);
			b1e.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1e.request(op).roles().run().assertStatus(403);
			b1e.request(op).roles("foo").run().assertStatus(403);
			b1e.request(op).roles("bar").run().assertStatus(403);
			b1e.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="(foo) && (bar)")
	public static class B1f extends B1 {}

	@Test
	public void b01f_andsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient b1f = MockRestClient.buildLax(B1f.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1f.request(op).roles("foo","bar").run().assertStatus(200);
			b1f.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1f.request(op).roles().run().assertStatus(403);
			b1f.request(op).roles("foo").run().assertStatus(403);
			b1f.request(op).roles("bar").run().assertStatus(403);
			b1f.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo && (bar || baz)")
	public static class B1g extends B1 {}

	@Test
	public void b01g_complex_pass() throws Exception {
		MockRestClient b1g = MockRestClient.buildLax(B1g.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1g.request(op).roles("foo","bar").run().assertStatus(200);
			b1g.request(op).roles("foo","baz").run().assertStatus(200);
			b1g.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1g.request(op).roles().run().assertStatus(403);
			b1g.request(op).roles("foo").run().assertStatus(403);
			b1g.request(op).roles("bar","baz").run().assertStatus(403);
			b1g.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="foo || (bar && baz)")
	public static class B1h extends B1 {}

	@Test
	public void b01h_complex_pass() throws Exception {
		MockRestClient b1h = MockRestClient.buildLax(B1h.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b1h.request(op).roles("foo").run().assertStatus(200);
			b1h.request(op).roles("bar","baz").run().assertStatus(200);
			b1h.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b1h.request(op).roles().run().assertStatus(403);
			b1h.request(op).roles("bar").run().assertStatus(403);
			b1h.request(op).roles("baz").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestOp(roleGuard), multiple guards on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B2a {
		@RestOp(roleGuard="foo,bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo,bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo,bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo,bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo,bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void b02a_orsWithComma_pass() throws Exception {
		MockRestClient b2a = MockRestClient.buildLax(B2a.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b2a.request(op).roles("foo").run().assertStatus(200);
			b2a.request(op).roles("bar").run().assertStatus(200);
			b2a.request(op).roles("foo","bar").run().assertStatus(200);
			b2a.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b2a.request(op).roles().run().assertStatus(403);
			b2a.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class B2b {
		@RestOp(roleGuard="foo | bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo | bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo | bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo | bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo | bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void b02b_orsWithSinglePipe_pass() throws Exception {
		MockRestClient b2b = MockRestClient.buildLax(B2b.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b2b.request(op).roles("foo").run().assertStatus(200);
			b2b.request(op).roles("bar").run().assertStatus(200);
			b2b.request(op).roles("foo","bar").run().assertStatus(200);
			b2b.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b2b.request(op).roles().run().assertStatus(403);
			b2b.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class B2c {
		@RestOp(roleGuard="foo || bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo || bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo || bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo || bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo || bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void b02c_orsWithDoublePipe_pass() throws Exception {
		MockRestClient b2c = MockRestClient.buildLax(B2c.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b2c.request(op).roles("foo").run().assertStatus(200);
			b2c.request(op).roles("bar").run().assertStatus(200);
			b2c.request(op).roles("foo","bar").run().assertStatus(200);
			b2c.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b2c.request(op).roles().run().assertStatus(403);
			b2c.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class B2d {
		@RestOp(roleGuard="foo & bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo & bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo & bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo & bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo & bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void b02d_andsWithSingleAmp_pass() throws Exception {
		MockRestClient b2d = MockRestClient.buildLax(B2d.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b2d.request(op).roles("foo","bar").run().assertStatus(200);
			b2d.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b2d.request(op).roles().run().assertStatus(403);
			b2d.request(op).roles("foo").run().assertStatus(403);
			b2d.request(op).roles("bar").run().assertStatus(403);
			b2d.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class B2e {
		@RestOp(roleGuard="foo && bar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="foo && bar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="foo && bar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="foo && bar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="foo && bar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void b02e_andsWithDoubleAmp_pass() throws Exception {
		MockRestClient b2e = MockRestClient.buildLax(B2e.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			b2e.request(op).roles("foo","bar").run().assertStatus(200);
			b2e.request(op).roles("foo","bar","baz").run().assertStatus(200);
			b2e.request(op).roles().run().assertStatus(403);
			b2e.request(op).roles("foo").run().assertStatus(403);
			b2e.request(op).roles("bar").run().assertStatus(403);
			b2e.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class B2f {
		@RestOp(roleGuard="(foo) && (bar)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02f_andsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient b2f = MockRestClient.buildLax(B2f.class);
		b2f.get().roles("foo","bar").run().assertStatus(200);
		b2f.get().roles("foo","bar","baz").run().assertStatus(200);
		b2f.get().roles().run().assertStatus(403);
		b2f.get().roles("foo").run().assertStatus(403);
		b2f.get().roles("bar").run().assertStatus(403);
		b2f.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class B2g {
		@RestOp(roleGuard="foo && (bar || baz)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02g_complex_pass() throws Exception {
		MockRestClient b2g = MockRestClient.buildLax(B2g.class);
		b2g.get().roles("foo","bar").run().assertStatus(200);
		b2g.get().roles("foo","baz").run().assertStatus(200);
		b2g.get().roles("foo","bar","baz").run().assertStatus(200);
		b2g.get().roles().run().assertStatus(403);
		b2g.get().roles("foo").run().assertStatus(403);
		b2g.get().roles("bar","baz").run().assertStatus(403);
		b2g.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class B2h {
		@RestOp(roleGuard="foo || (bar && baz)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02h_complex_pass() throws Exception {
		MockRestClient b2h = MockRestClient.buildLax(B2h.class);
		b2h.get().roles("foo").run().assertStatus(200);
		b2h.get().roles("bar","baz").run().assertStatus(200);
		b2h.get().roles("foo","bar","baz").run().assertStatus(200);
		b2h.get().roles().run().assertStatus(403);
		b2h.get().roles("bar").run().assertStatus(403);
		b2h.get().roles("baz").run().assertStatus(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Rest(roleGuard), pattern guards on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(rolesDeclared="foo,bar,baz")
	public static class C1 {
		@RestOp
		public String a() {
			return "OK";
		}
		@RestGet
		public String b() {
			return "OK";
		}
		@RestPut
		public String c() {
			return "OK";
		}
		@RestPost
		public String d() {
			return "OK";
		}
		@RestDelete
		public String e() {
			return "OK";
		}
	}

	@Rest(roleGuard="fo*,*ar")
	public static class C1a extends C1 {}

	@Test
	public void c01a_orPatternsWithComma_pass() throws Exception {
		MockRestClient c1a = MockRestClient.buildLax(C1a.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1a.request(op).roles("foo").run().assertStatus(200);
			c1a.request(op).roles("bar").run().assertStatus(200);
			c1a.request(op).roles("foo","bar").run().assertStatus(200);
			c1a.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1a.request(op).roles().run().assertStatus(403);
			c1a.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* | *ar")
	public static class C1b extends C1 {}

	@Test
	public void c01b_orPatternsWithSinglePipe_pass() throws Exception {
		MockRestClient c1b = MockRestClient.buildLax(C1b.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1b.request(op).roles("foo").run().assertStatus(200);
			c1b.request(op).roles("bar").run().assertStatus(200);
			c1b.request(op).roles("foo","bar").run().assertStatus(200);
			c1b.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1b.request(op).roles().run().assertStatus(403);
			c1b.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* || *ar")
	public static class C1c extends C1 {}

	@Test
	public void c01c_orPatternsWithDoublePipe_pass() throws Exception {
		MockRestClient c1c = MockRestClient.buildLax(C1c.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1c.request(op).roles("foo").run().assertStatus(200);
			c1c.request(op).roles("bar").run().assertStatus(200);
			c1c.request(op).roles("foo","bar").run().assertStatus(200);
			c1c.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1c.request(op).roles().run().assertStatus(403);
			c1c.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* & *ar")
	public static class C1d extends C1 {}

	@Test
	public void c01d_andPatternsWithSingleAmp_pass() throws Exception {
		MockRestClient c1d = MockRestClient.buildLax(C1d.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1d.request(op).roles("foo","bar").run().assertStatus(200);
			c1d.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1d.request(op).roles().run().assertStatus(403);
			c1d.request(op).roles("foo").run().assertStatus(403);
			c1d.request(op).roles("bar").run().assertStatus(403);
			c1d.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* && *ar")
	public static class C1e extends C1 {}

	@Test
	public void c01e_andPatternsWithDoubleAmp_pass() throws Exception {
		MockRestClient c1e = MockRestClient.buildLax(C1e.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1e.request(op).roles("foo","bar").run().assertStatus(200);
			c1e.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1e.request(op).roles().run().assertStatus(403);
			c1e.request(op).roles("foo").run().assertStatus(403);
			c1e.request(op).roles("bar").run().assertStatus(403);
			c1e.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="(fo*) && (*ar)")
	public static class C1f extends C1 {}

	@Test
	public void c01f_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient c1f = MockRestClient.buildLax(C1f.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1f.request(op).roles("foo","bar").run().assertStatus(200);
			c1f.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1f.request(op).roles().run().assertStatus(403);
			c1f.request(op).roles("foo").run().assertStatus(403);
			c1f.request(op).roles("bar").run().assertStatus(403);
			c1f.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* && (*ar || *az)")
	public static class C1g extends C1 {}

	@Test
	public void c01g_complexPatterns_pass() throws Exception {
		MockRestClient c1g = MockRestClient.buildLax(C1g.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1g.request(op).roles("foo","bar").run().assertStatus(200);
			c1g.request(op).roles("foo","baz").run().assertStatus(200);
			c1g.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1g.request(op).roles().run().assertStatus(403);
			c1g.request(op).roles("foo").run().assertStatus(403);
			c1g.request(op).roles("bar","baz").run().assertStatus(403);
			c1g.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest(roleGuard="fo* || (*ar && *az)")
	public static class C1h extends C1 {}

	@Test
	public void c01h_complexPatterns_pass() throws Exception {
		MockRestClient c1h = MockRestClient.buildLax(C1h.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c1h.request(op).roles("foo").run().assertStatus(200);
			c1h.request(op).roles("bar","baz").run().assertStatus(200);
			c1h.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c1h.request(op).roles().run().assertStatus(403);
			c1h.request(op).roles("bar").run().assertStatus(403);
			c1h.request(op).roles("baz").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestOp(roleGuard), pattern guards on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C2a {
		@RestOp(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void c02a_orPatternsWithComma_pass() throws Exception {
		MockRestClient c2a = MockRestClient.buildLax(C2a.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			c2a.request(op).roles("foo").run().assertStatus(200);
			c2a.request(op).roles("bar").run().assertStatus(200);
			c2a.request(op).roles("foo","bar").run().assertStatus(200);
			c2a.request(op).roles("foo","bar","baz").run().assertStatus(200);
			c2a.request(op).roles().run().assertStatus(403);
			c2a.request(op).roles("baz").run().assertStatus(403);
		}
	}

	@Rest
	public static class C2b {
		@RestOp(roleGuard="fo* | *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02b_orPatternsWithSinglePipe_pass() throws Exception {
		MockRestClient c2b = MockRestClient.buildLax(C2b.class);
		c2b.get().roles("foo").run().assertStatus(200);
		c2b.get().roles("bar").run().assertStatus(200);
		c2b.get().roles("foo","bar").run().assertStatus(200);
		c2b.get().roles("foo","bar","baz").run().assertStatus(200);
		c2b.get().roles().run().assertStatus(403);
		c2b.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2c {
		@RestOp(roleGuard="fo* || *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02c_orPatternsWithDoublePipe_pass() throws Exception {
		MockRestClient c2c = MockRestClient.buildLax(C2c.class);
		c2c.get().roles("foo").run().assertStatus(200);
		c2c.get().roles("bar").run().assertStatus(200);
		c2c.get().roles("foo","bar").run().assertStatus(200);
		c2c.get().roles("foo","bar","baz").run().assertStatus(200);
		c2c.get().roles().run().assertStatus(403);
		c2c.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2d {
		@RestOp(roleGuard="fo* & *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02d_andPatternsWithSingleAmp_pass() throws Exception {
		MockRestClient c2d = MockRestClient.buildLax(C2d.class);
		c2d.get().roles("foo","bar").run().assertStatus(200);
		c2d.get().roles("foo","bar","baz").run().assertStatus(200);
		c2d.get().roles().run().assertStatus(403);
		c2d.get().roles("foo").run().assertStatus(403);
		c2d.get().roles("bar").run().assertStatus(403);
		c2d.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2e {
		@RestOp(roleGuard="fo* && *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02e_andPatternsWithDoubleAmp_pass() throws Exception {
		MockRestClient c2e = MockRestClient.buildLax(C2e.class);
		c2e.get().roles("foo","bar").run().assertStatus(200);
		c2e.get().roles("foo","bar","baz").run().assertStatus(200);
		c2e.get().roles().run().assertStatus(403);
		c2e.get().roles("foo").run().assertStatus(403);
		c2e.get().roles("bar").run().assertStatus(403);
		c2e.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2f {
		@RestOp(roleGuard="(fo*) && (*ar)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02f_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient c2f = MockRestClient.buildLax(C2f.class);
		c2f.get().roles("foo","bar").run().assertStatus(200);
		c2f.get().roles("foo","bar","baz").run().assertStatus(200);
		c2f.get().roles().run().assertStatus(403);
		c2f.get().roles("foo").run().assertStatus(403);
		c2f.get().roles("bar").run().assertStatus(403);
		c2f.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2g {
		@RestOp(roleGuard="fo* && (*ar || *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02g_complexPatterns_pass() throws Exception {
		MockRestClient c2g = MockRestClient.buildLax(C2g.class);
		c2g.get().roles("foo","bar").run().assertStatus(200);
		c2g.get().roles("foo","baz").run().assertStatus(200);
		c2g.get().roles("foo","bar","baz").run().assertStatus(200);
		c2g.get().roles().run().assertStatus(403);
		c2g.get().roles("foo").run().assertStatus(403);
		c2g.get().roles("bar","baz").run().assertStatus(403);
		c2g.get().roles("baz").run().assertStatus(403);
	}

	@Rest
	public static class C2h {
		@RestOp(roleGuard="fo* || (*ar && *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02h_complexPatterns_pass() throws Exception {
		MockRestClient c2h = MockRestClient.buildLax(C2h.class);
		c2h.get().roles("foo").run().assertStatus(200);
		c2h.get().roles("bar","baz").run().assertStatus(200);
		c2h.get().roles("foo","bar","baz").run().assertStatus(200);
		c2h.get().roles().run().assertStatus(403);
		c2h.get().roles("bar").run().assertStatus(403);
		c2h.get().roles("baz").run().assertStatus(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestOp(roleGuard), pattern guards on method but no roles defined
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestOp(roleGuard="fo*,*ar")
		public String a() {
			return "OK";
		}
		@RestGet(roleGuard="fo*,*ar")
		public String b() {
			return "OK";
		}
		@RestPut(roleGuard="fo*,*ar")
		public String c() {
			return "OK";
		}
		@RestPost(roleGuard="fo*,*ar")
		public String d() {
			return "OK";
		}
		@RestDelete(roleGuard="fo*,*ar")
		public String e() {
			return "OK";
		}
	}

	@Test
	public void d01_patternsWithoutRoles() throws Exception {
		MockRestClient d = MockRestClient.buildLax(D.class);

		for (RestOperation op : ops(op("get","/a"),op("get","/b"),op("put","/c"),op("post","/d"),op("delete","/e"))) {
			d.request(op).roles().run().assertStatus(403);
			d.request(op).roles("foo").run().assertStatus(403);
			d.request(op).roles("bar").run().assertStatus(403);
			d.request(op).roles("baz").run().assertStatus(403);
			d.request(op).roles("foo","bar").run().assertStatus(403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestOp(roleGuard), any role.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(rolesDeclared="foo,bar,baz")
	public static class E {
		@RestOp(roleGuard="*")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void e01_anyRole_pass() throws Exception {
		MockRestClient e = MockRestClient.buildLax(E.class);
		e.get().roles("foo").run().assertStatus(200);
		e.get().roles("bar").run().assertStatus(200);
		e.get().roles("baz").run().assertStatus(200);
		e.get().roles("foo","bar").run().assertStatus(200);
		e.get().roles().run().assertStatus(403);
	}
}
