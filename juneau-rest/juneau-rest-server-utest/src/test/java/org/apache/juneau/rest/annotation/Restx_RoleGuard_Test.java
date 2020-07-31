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

import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Restx_RoleGuard_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guard on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A1 {
		@RestMethod
		public String get() {
			return "OK";
		}
	}

	@Test
	public void a01a_onClass_simple() throws Exception {
		MockRestClient a1 = MockRestClient.buildLax(A1.class);
		a1.get().roles("foo").run().assertCode().is(200);
		a1.get().roles("foo","bar").run().assertCode().is(200);
		a1.get().roles("bar","foo").run().assertCode().is(200);
		a1.get().run().assertCode().is(403);
		a1.get().roles("foo2").run().assertCode().is(403);
		a1.get().roles("foo2","bar").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guard on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A2 {
		@RestMethod(roleGuard="foo")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void a02a_onMethod_simple() throws Exception {
		MockRestClient a2 = MockRestClient.buildLax(A2.class);
		a2.get().roles("foo").run().assertCode().is(200);
		a2.get().roles("foo","bar").run().assertCode().is(200);
		a2.get().roles("bar","foo").run().assertCode().is(200);
		a2.get().run().assertCode().is(403);
		a2.get().roles("foo2").run().assertCode().is(403);
		a2.get().roles("foo2","bar").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guards on class and method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A3 {
		@RestMethod(roleGuard="bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void a03a_onBoth_simple() throws Exception {
		MockRestClient a3 = MockRestClient.buildLax(A3.class);
		a3.get().roles("foo","bar").run().assertCode().is(200);
		a3.get().roles("bar","foo").run().assertCode().is(200);
		a3.get().roles("bar","foo","baz").run().assertCode().is(200);
		a3.get().run().assertCode().is(403);
		a3.get().roles("foo").run().assertCode().is(403);
		a3.get().roles("bar").run().assertCode().is(403);
		a3.get().roles("foo2").run().assertCode().is(403);
		a3.get().roles("foo2","bar").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple guards on class and method, inherited
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(roleGuard="foo")
	public static class A4a {
		@RestMethod(roleGuard="bar")
		public String get() {
			return "OK";
		}
	}

	@Rest(roleGuard="baz")
	public static class A4b extends A4a {
		@Override
		@RestMethod(roleGuard="qux")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void a04a_inheritence_simple() throws Exception {
		MockRestClient a4 = MockRestClient.buildLax(A4b.class);
		a4.get().roles("foo","bar","baz","qux").run().assertCode().is(200);
		a4.get().roles("foo","bar","baz","qux","quux").run().assertCode().is(200);
		a4.get().roles("foo","bar","baz").run().assertCode().is(403);
		a4.get().roles("foo","bar","qux").run().assertCode().is(403);
		a4.get().roles("foo","baz","qux").run().assertCode().is(403);
		a4.get().roles("bar","baz","qux").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Rest(roleGuard), multiple guards on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B1 {
		@RestMethod
		public String get() {
			return "OK";
		}
	}

	@Rest(roleGuard="foo,bar")
	public static class B1a extends B1 {}

	@Test
	public void b01a_orsWithComma_pass() throws Exception {
		MockRestClient b1a = MockRestClient.buildLax(B1a.class);
		b1a.get().roles("foo").run().assertCode().is(200);
		b1a.get().roles("bar").run().assertCode().is(200);
		b1a.get().roles("foo","bar").run().assertCode().is(200);
		b1a.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1a.get().roles().run().assertCode().is(403);
		b1a.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo | bar")
	public static class B1b extends B1 {}

	@Test
	public void b01b_orsWithSinglePipe_pass() throws Exception {
		MockRestClient b1b = MockRestClient.buildLax(B1b.class);
		b1b.get().roles("foo").run().assertCode().is(200);
		b1b.get().roles("bar").run().assertCode().is(200);
		b1b.get().roles("foo","bar").run().assertCode().is(200);
		b1b.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1b.get().roles().run().assertCode().is(403);
		b1b.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo || bar")
	public static class B1c extends B1 {}

	@Test
	public void b01c_orsWithDoublePipe_pass() throws Exception {
		MockRestClient b1c = MockRestClient.buildLax(B1c.class);
		b1c.get().roles("foo").run().assertCode().is(200);
		b1c.get().roles("bar").run().assertCode().is(200);
		b1c.get().roles("foo","bar").run().assertCode().is(200);
		b1c.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1c.get().roles().run().assertCode().is(403);
		b1c.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo & bar")
	public static class B1d extends B1 {}

	@Test
	public void b01d_andsWithSingleAmp_pass() throws Exception {
		MockRestClient b1d = MockRestClient.buildLax(B1d.class);
		b1d.get().roles("foo","bar").run().assertCode().is(200);
		b1d.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1d.get().roles().run().assertCode().is(403);
		b1d.get().roles("foo").run().assertCode().is(403);
		b1d.get().roles("bar").run().assertCode().is(403);
		b1d.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo && bar")
	public static class B1e extends B1 {}

	@Test
	public void b01e_andsWithDoubleAmp_pass() throws Exception {
		MockRestClient b1e = MockRestClient.buildLax(B1e.class);
		b1e.get().roles("foo","bar").run().assertCode().is(200);
		b1e.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1e.get().roles().run().assertCode().is(403);
		b1e.get().roles("foo").run().assertCode().is(403);
		b1e.get().roles("bar").run().assertCode().is(403);
		b1e.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="(foo) && (bar)")
	public static class B1f extends B1 {}

	@Test
	public void b01f_andsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient b1f = MockRestClient.buildLax(B1f.class);
		b1f.get().roles("foo","bar").run().assertCode().is(200);
		b1f.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1f.get().roles().run().assertCode().is(403);
		b1f.get().roles("foo").run().assertCode().is(403);
		b1f.get().roles("bar").run().assertCode().is(403);
		b1f.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo && (bar || baz)")
	public static class B1g extends B1 {}

	@Test
	public void b01g_complex_pass() throws Exception {
		MockRestClient b1g = MockRestClient.buildLax(B1g.class);
		b1g.get().roles("foo","bar").run().assertCode().is(200);
		b1g.get().roles("foo","baz").run().assertCode().is(200);
		b1g.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1g.get().roles().run().assertCode().is(403);
		b1g.get().roles("foo").run().assertCode().is(403);
		b1g.get().roles("bar","baz").run().assertCode().is(403);
		b1g.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="foo || (bar && baz)")
	public static class B1h extends B1 {}

	@Test
	public void b01h_complex_pass() throws Exception {
		MockRestClient b1h = MockRestClient.buildLax(B1h.class);
		b1h.get().roles("foo").run().assertCode().is(200);
		b1h.get().roles("bar","baz").run().assertCode().is(200);
		b1h.get().roles("foo","bar","baz").run().assertCode().is(200);
		b1h.get().roles().run().assertCode().is(403);
		b1h.get().roles("bar").run().assertCode().is(403);
		b1h.get().roles("baz").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestMethod(roleGuard), multiple guards on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B2a {
		@RestMethod(roleGuard="foo,bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02a_orsWithComma_pass() throws Exception {
		MockRestClient b2a = MockRestClient.buildLax(B2a.class);
		b2a.get().roles("foo").run().assertCode().is(200);
		b2a.get().roles("bar").run().assertCode().is(200);
		b2a.get().roles("foo","bar").run().assertCode().is(200);
		b2a.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2a.get().roles().run().assertCode().is(403);
		b2a.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2b {
		@RestMethod(roleGuard="foo | bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02b_orsWithSinglePipe_pass() throws Exception {
		MockRestClient b2b = MockRestClient.buildLax(B2b.class);
		b2b.get().roles("foo").run().assertCode().is(200);
		b2b.get().roles("bar").run().assertCode().is(200);
		b2b.get().roles("foo","bar").run().assertCode().is(200);
		b2b.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2b.get().roles().run().assertCode().is(403);
		b2b.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2c {
		@RestMethod(roleGuard="foo || bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02c_orsWithDoublePipe_pass() throws Exception {
		MockRestClient b2c = MockRestClient.buildLax(B2c.class);
		b2c.get().roles("foo").run().assertCode().is(200);
		b2c.get().roles("bar").run().assertCode().is(200);
		b2c.get().roles("foo","bar").run().assertCode().is(200);
		b2c.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2c.get().roles().run().assertCode().is(403);
		b2c.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2d {
		@RestMethod(roleGuard="foo & bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02d_andsWithSingleAmp_pass() throws Exception {
		MockRestClient b2d = MockRestClient.buildLax(B2d.class);
		b2d.get().roles("foo","bar").run().assertCode().is(200);
		b2d.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2d.get().roles().run().assertCode().is(403);
		b2d.get().roles("foo").run().assertCode().is(403);
		b2d.get().roles("bar").run().assertCode().is(403);
		b2d.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2e {
		@RestMethod(roleGuard="foo && bar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02e_andsWithDoubleAmp_pass() throws Exception {
		MockRestClient b2e = MockRestClient.buildLax(B2e.class);
		b2e.get().roles("foo","bar").run().assertCode().is(200);
		b2e.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2e.get().roles().run().assertCode().is(403);
		b2e.get().roles("foo").run().assertCode().is(403);
		b2e.get().roles("bar").run().assertCode().is(403);
		b2e.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2f {
		@RestMethod(roleGuard="(foo) && (bar)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02f_andsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient b2f = MockRestClient.buildLax(B2f.class);
		b2f.get().roles("foo","bar").run().assertCode().is(200);
		b2f.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2f.get().roles().run().assertCode().is(403);
		b2f.get().roles("foo").run().assertCode().is(403);
		b2f.get().roles("bar").run().assertCode().is(403);
		b2f.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2g {
		@RestMethod(roleGuard="foo && (bar || baz)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02g_complex_pass() throws Exception {
		MockRestClient b2g = MockRestClient.buildLax(B2g.class);
		b2g.get().roles("foo","bar").run().assertCode().is(200);
		b2g.get().roles("foo","baz").run().assertCode().is(200);
		b2g.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2g.get().roles().run().assertCode().is(403);
		b2g.get().roles("foo").run().assertCode().is(403);
		b2g.get().roles("bar","baz").run().assertCode().is(403);
		b2g.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class B2h {
		@RestMethod(roleGuard="foo || (bar && baz)")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void b02h_complex_pass() throws Exception {
		MockRestClient b2h = MockRestClient.buildLax(B2h.class);
		b2h.get().roles("foo").run().assertCode().is(200);
		b2h.get().roles("bar","baz").run().assertCode().is(200);
		b2h.get().roles("foo","bar","baz").run().assertCode().is(200);
		b2h.get().roles().run().assertCode().is(403);
		b2h.get().roles("bar").run().assertCode().is(403);
		b2h.get().roles("baz").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Rest(roleGuard), pattern guards on class
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(rolesDeclared="foo,bar,baz")
	public static class C1 {
		@RestMethod
		public String get() {
			return "OK";
		}
	}

	@Rest(roleGuard="fo*,*ar")
	public static class C1a extends C1 {}

	@Test
	public void c01a_orPatternsWithComma_pass() throws Exception {
		MockRestClient c1a = MockRestClient.buildLax(C1a.class);
		c1a.get().roles("foo").run().assertCode().is(200);
		c1a.get().roles("bar").run().assertCode().is(200);
		c1a.get().roles("foo","bar").run().assertCode().is(200);
		c1a.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1a.get().roles().run().assertCode().is(403);
		c1a.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* | *ar")
	public static class C1b extends C1 {}

	@Test
	public void c01b_orPatternsWithSinglePipe_pass() throws Exception {
		MockRestClient c1b = MockRestClient.buildLax(C1b.class);
		c1b.get().roles("foo").run().assertCode().is(200);
		c1b.get().roles("bar").run().assertCode().is(200);
		c1b.get().roles("foo","bar").run().assertCode().is(200);
		c1b.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1b.get().roles().run().assertCode().is(403);
		c1b.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* || *ar")
	public static class C1c extends C1 {}

	@Test
	public void c01c_orPatternsWithDoublePipe_pass() throws Exception {
		MockRestClient c1c = MockRestClient.buildLax(C1c.class);
		c1c.get().roles("foo").run().assertCode().is(200);
		c1c.get().roles("bar").run().assertCode().is(200);
		c1c.get().roles("foo","bar").run().assertCode().is(200);
		c1c.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1c.get().roles().run().assertCode().is(403);
		c1c.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* & *ar")
	public static class C1d extends C1 {}

	@Test
	public void c01d_andPatternsWithSingleAmp_pass() throws Exception {
		MockRestClient c1d = MockRestClient.buildLax(C1d.class);
		c1d.get().roles("foo","bar").run().assertCode().is(200);
		c1d.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1d.get().roles().run().assertCode().is(403);
		c1d.get().roles("foo").run().assertCode().is(403);
		c1d.get().roles("bar").run().assertCode().is(403);
		c1d.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* && *ar")
	public static class C1e extends C1 {}

	@Test
	public void c01e_andPatternsWithDoubleAmp_pass() throws Exception {
		MockRestClient c1e = MockRestClient.buildLax(C1e.class);
		c1e.get().roles("foo","bar").run().assertCode().is(200);
		c1e.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1e.get().roles().run().assertCode().is(403);
		c1e.get().roles("foo").run().assertCode().is(403);
		c1e.get().roles("bar").run().assertCode().is(403);
		c1e.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="(fo*) && (*ar)")
	public static class C1f extends C1 {}

	@Test
	public void c01f_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient c1f = MockRestClient.buildLax(C1f.class);
		c1f.get().roles("foo","bar").run().assertCode().is(200);
		c1f.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1f.get().roles().run().assertCode().is(403);
		c1f.get().roles("foo").run().assertCode().is(403);
		c1f.get().roles("bar").run().assertCode().is(403);
		c1f.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* && (*ar || *az)")
	public static class C1g extends C1 {}

	@Test
	public void c01g_complexPatterns_pass() throws Exception {
		MockRestClient c1g = MockRestClient.buildLax(C1g.class);
		c1g.get().roles("foo","bar").run().assertCode().is(200);
		c1g.get().roles("foo","baz").run().assertCode().is(200);
		c1g.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1g.get().roles().run().assertCode().is(403);
		c1g.get().roles("foo").run().assertCode().is(403);
		c1g.get().roles("bar","baz").run().assertCode().is(403);
		c1g.get().roles("baz").run().assertCode().is(403);
	}

	@Rest(roleGuard="fo* || (*ar && *az)")
	public static class C1h extends C1 {}

	@Test
	public void c01h_complexPatterns_pass() throws Exception {
		MockRestClient c1h = MockRestClient.buildLax(C1h.class);
		c1h.get().roles("foo").run().assertCode().is(200);
		c1h.get().roles("bar","baz").run().assertCode().is(200);
		c1h.get().roles("foo","bar","baz").run().assertCode().is(200);
		c1h.get().roles().run().assertCode().is(403);
		c1h.get().roles("bar").run().assertCode().is(403);
		c1h.get().roles("baz").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestMethod(roleGuard), pattern guards on method
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C2a {
		@RestMethod(roleGuard="fo*,*ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02a_orPatternsWithComma_pass() throws Exception {
		MockRestClient c2a = MockRestClient.buildLax(C2a.class);
		c2a.get().roles("foo").run().assertCode().is(200);
		c2a.get().roles("bar").run().assertCode().is(200);
		c2a.get().roles("foo","bar").run().assertCode().is(200);
		c2a.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2a.get().roles().run().assertCode().is(403);
		c2a.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2b {
		@RestMethod(roleGuard="fo* | *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02b_orPatternsWithSinglePipe_pass() throws Exception {
		MockRestClient c2b = MockRestClient.buildLax(C2b.class);
		c2b.get().roles("foo").run().assertCode().is(200);
		c2b.get().roles("bar").run().assertCode().is(200);
		c2b.get().roles("foo","bar").run().assertCode().is(200);
		c2b.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2b.get().roles().run().assertCode().is(403);
		c2b.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2c {
		@RestMethod(roleGuard="fo* || *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02c_orPatternsWithDoublePipe_pass() throws Exception {
		MockRestClient c2c = MockRestClient.buildLax(C2c.class);
		c2c.get().roles("foo").run().assertCode().is(200);
		c2c.get().roles("bar").run().assertCode().is(200);
		c2c.get().roles("foo","bar").run().assertCode().is(200);
		c2c.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2c.get().roles().run().assertCode().is(403);
		c2c.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2d {
		@RestMethod(roleGuard="fo* & *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02d_andPatternsWithSingleAmp_pass() throws Exception {
		MockRestClient c2d = MockRestClient.buildLax(C2d.class);
		c2d.get().roles("foo","bar").run().assertCode().is(200);
		c2d.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2d.get().roles().run().assertCode().is(403);
		c2d.get().roles("foo").run().assertCode().is(403);
		c2d.get().roles("bar").run().assertCode().is(403);
		c2d.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2e {
		@RestMethod(roleGuard="fo* && *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02e_andPatternsWithDoubleAmp_pass() throws Exception {
		MockRestClient c2e = MockRestClient.buildLax(C2e.class);
		c2e.get().roles("foo","bar").run().assertCode().is(200);
		c2e.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2e.get().roles().run().assertCode().is(403);
		c2e.get().roles("foo").run().assertCode().is(403);
		c2e.get().roles("bar").run().assertCode().is(403);
		c2e.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2f {
		@RestMethod(roleGuard="(fo*) && (*ar)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02f_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		MockRestClient c2f = MockRestClient.buildLax(C2f.class);
		c2f.get().roles("foo","bar").run().assertCode().is(200);
		c2f.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2f.get().roles().run().assertCode().is(403);
		c2f.get().roles("foo").run().assertCode().is(403);
		c2f.get().roles("bar").run().assertCode().is(403);
		c2f.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2g {
		@RestMethod(roleGuard="fo* && (*ar || *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02g_complexPatterns_pass() throws Exception {
		MockRestClient c2g = MockRestClient.buildLax(C2g.class);
		c2g.get().roles("foo","bar").run().assertCode().is(200);
		c2g.get().roles("foo","baz").run().assertCode().is(200);
		c2g.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2g.get().roles().run().assertCode().is(403);
		c2g.get().roles("foo").run().assertCode().is(403);
		c2g.get().roles("bar","baz").run().assertCode().is(403);
		c2g.get().roles("baz").run().assertCode().is(403);
	}

	@Rest
	public static class C2h {
		@RestMethod(roleGuard="fo* || (*ar && *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void c02h_complexPatterns_pass() throws Exception {
		MockRestClient c2h = MockRestClient.buildLax(C2h.class);
		c2h.get().roles("foo").run().assertCode().is(200);
		c2h.get().roles("bar","baz").run().assertCode().is(200);
		c2h.get().roles("foo","bar","baz").run().assertCode().is(200);
		c2h.get().roles().run().assertCode().is(403);
		c2h.get().roles("bar").run().assertCode().is(403);
		c2h.get().roles("baz").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestMethod(roleGuard), pattern guards on method but no roles defined
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestMethod(roleGuard="fo*,*ar")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void d01_patternsWithoutRoles() throws Exception {
		MockRestClient d = MockRestClient.buildLax(D.class);
		d.get().roles().run().assertCode().is(403);
		d.get().roles("foo").run().assertCode().is(403);
		d.get().roles("bar").run().assertCode().is(403);
		d.get().roles("baz").run().assertCode().is(403);
		d.get().roles("foo","bar").run().assertCode().is(403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RestMethod(roleGuard), any role.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(rolesDeclared="foo,bar,baz")
	public static class E {
		@RestMethod(roleGuard="*")
		public String get() {
			return "OK";
		}
	}

	@Test
	public void e01_anyRole_pass() throws Exception {
		MockRestClient e = MockRestClient.buildLax(E.class);
		e.get().roles("foo").run().assertCode().is(200);
		e.get().roles("bar").run().assertCode().is(200);
		e.get().roles("baz").run().assertCode().is(200);
		e.get().roles("foo","bar").run().assertCode().is(200);
		e.get().roles().run().assertCode().is(403);
	}
}
