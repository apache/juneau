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
package org.apache.juneau.rest.annotation2;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RoleGuardTest {

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
	static MockRest a1 = MockRest.build(A1.class);

	@Test
	public void a01a_onClass_simple_pass() throws Exception {
		a1.get().roles("foo").execute().assertStatus(200);
		a1.get().roles("foo","bar").execute().assertStatus(200);
		a1.get().roles("bar","foo").execute().assertStatus(200);
	}

	@Test
	public void a01b_onClass_simple_fail() throws Exception {
		a1.get().execute().assertStatus(403);
		a1.get().roles("foo2").execute().assertStatus(403);
		a1.get().roles("foo2","bar").execute().assertStatus(403);
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
	static MockRest a2 = MockRest.build(A2.class);

	@Test
	public void a02a_onMethod_simple_pass() throws Exception {
		a2.get().roles("foo").execute().assertStatus(200);
		a2.get().roles("foo","bar").execute().assertStatus(200);
		a2.get().roles("bar","foo").execute().assertStatus(200);
	}

	@Test
	public void a02b_onMethod_simple_fail() throws Exception {
		a2.get().execute().assertStatus(403);
		a2.get().roles("foo2").execute().assertStatus(403);
		a2.get().roles("foo2","bar").execute().assertStatus(403);
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
	static MockRest a3 = MockRest.build(A3.class);

	@Test
	public void a03a_onBoth_simple_pass() throws Exception {
		a3.get().roles("foo","bar").execute().assertStatus(200);
		a3.get().roles("bar","foo").execute().assertStatus(200);
		a3.get().roles("bar","foo","baz").execute().assertStatus(200);
	}

	@Test
	public void a03b_onBoth_simple_fail() throws Exception {
		a3.get().execute().assertStatus(403);
		a3.get().roles("foo").execute().assertStatus(403);
		a3.get().roles("bar").execute().assertStatus(403);
		a3.get().roles("foo2").execute().assertStatus(403);
		a3.get().roles("foo2","bar").execute().assertStatus(403);
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

	static MockRest a4 = MockRest.build(A4b.class);

	@Test
	public void a04a_inheritence_simple_pass() throws Exception {
		a4.get().roles("foo","bar","baz","qux").execute().assertStatus(200);
		a4.get().roles("foo","bar","baz","qux","quux").execute().assertStatus(200);
	}

	@Test
	public void a04b_inheritence_simple_fail() throws Exception {
		a3.get().execute().assertStatus(403);
		a3.get().roles("foo").execute().assertStatus(403);
		a3.get().roles("bar").execute().assertStatus(403);
		a3.get().roles("baz").execute().assertStatus(403);
		a3.get().roles("qux").execute().assertStatus(403);
		a4.get().roles("foo","bar","baz").execute().assertStatus(403);
		a4.get().roles("foo","bar","qux").execute().assertStatus(403);
		a4.get().roles("foo","baz","qux").execute().assertStatus(403);
		a4.get().roles("bar","baz","qux").execute().assertStatus(403);
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

	@Rest(roleGuard="foo | bar")
	public static class B1b extends B1 {}

	@Rest(roleGuard="foo || bar")
	public static class B1c extends B1 {}

	@Rest(roleGuard="foo & bar")
	public static class B1d extends B1 {}

	@Rest(roleGuard="foo && bar")
	public static class B1e extends B1 {}

	@Rest(roleGuard="(foo) && (bar)")
	public static class B1f extends B1 {}

	@Rest(roleGuard="foo && (bar || baz)")
	public static class B1g extends B1 {}

	@Rest(roleGuard="foo || (bar && baz)")
	public static class B1h extends B1 {}

	static MockRest b1a = MockRest.build(B1a.class);
	static MockRest b1b = MockRest.build(B1b.class);
	static MockRest b1c = MockRest.build(B1c.class);
	static MockRest b1d = MockRest.build(B1d.class);
	static MockRest b1e = MockRest.build(B1e.class);
	static MockRest b1f = MockRest.build(B1f.class);
	static MockRest b1g = MockRest.build(B1g.class);
	static MockRest b1h = MockRest.build(B1h.class);

	@Test
	public void b01a_orsWithComma_pass() throws Exception {
		// @Rest(roleGuard="foo,bar")
		b1a.get().roles("foo").execute().assertStatus(200);
		b1a.get().roles("bar").execute().assertStatus(200);
		b1a.get().roles("foo","bar").execute().assertStatus(200);
		b1a.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01b_orsWithComma_fail() throws Exception {
		// @Rest(roleGuard="foo,bar")
		b1a.get().roles().execute().assertStatus(403);
		b1a.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01c_orsWithSinglePipe_pass() throws Exception {
		// @Rest(roleGuard="foo | bar")
		b1b.get().roles("foo").execute().assertStatus(200);
		b1b.get().roles("bar").execute().assertStatus(200);
		b1b.get().roles("foo","bar").execute().assertStatus(200);
		b1b.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01d_orsWithSinglePipe_fail() throws Exception {
		// @Rest(roleGuard="foo | bar")
		b1b.get().roles().execute().assertStatus(403);
		b1b.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01e_orsWithDoublePipe_pass() throws Exception {
		// @Rest(roleGuard="foo || bar")
		b1c.get().roles("foo").execute().assertStatus(200);
		b1c.get().roles("bar").execute().assertStatus(200);
		b1c.get().roles("foo","bar").execute().assertStatus(200);
		b1c.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01f_orsWithDoublePipe_fail() throws Exception {
		// @Rest(roleGuard="foo || bar")
		b1c.get().roles().execute().assertStatus(403);
		b1c.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01g_andsWithSingleAmp_pass() throws Exception {
		// @Rest(roleGuard="foo & bar")
		b1d.get().roles("foo","bar").execute().assertStatus(200);
		b1d.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01h_andsWithSingleAmp_fail() throws Exception {
		// @Rest(roleGuard="foo & bar")
		b1d.get().roles().execute().assertStatus(403);
		b1d.get().roles("foo").execute().assertStatus(403);
		b1d.get().roles("bar").execute().assertStatus(403);
		b1d.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01i_andsWithDoubleAmp_pass() throws Exception {
		// @Rest(roleGuard="foo && bar")
		b1e.get().roles("foo","bar").execute().assertStatus(200);
		b1e.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01j_andsWithDoubleAmp_fail() throws Exception {
		// @Rest(roleGuard="foo && bar")
		b1e.get().roles().execute().assertStatus(403);
		b1e.get().roles("foo").execute().assertStatus(403);
		b1e.get().roles("bar").execute().assertStatus(403);
		b1e.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01k_andsWithDoubleAmpAndParens_pass() throws Exception {
		// @Rest(roleGuard="(foo) && (bar)")
		b1f.get().roles("foo","bar").execute().assertStatus(200);
		b1f.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01l_andsWithDoubleAmpAndParens_fail() throws Exception {
		// @Rest(roleGuard="(foo) && (bar)")
		b1f.get().roles().execute().assertStatus(403);
		b1f.get().roles("foo").execute().assertStatus(403);
		b1f.get().roles("bar").execute().assertStatus(403);
		b1f.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01m_complex_pass() throws Exception {
		// @Rest(roleGuard="foo && (bar || baz)")
		b1g.get().roles("foo","bar").execute().assertStatus(200);
		b1g.get().roles("foo","baz").execute().assertStatus(200);
		b1g.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01n_complex_fail() throws Exception {
		// @Rest(roleGuard="foo && (bar || baz)")
		b1g.get().roles().execute().assertStatus(403);
		b1g.get().roles("foo").execute().assertStatus(403);
		b1g.get().roles("bar","baz").execute().assertStatus(403);
		b1g.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b01o_complex_pass() throws Exception {
		// @Rest(roleGuard="foo || (bar && baz)")
		b1h.get().roles("foo").execute().assertStatus(200);
		b1h.get().roles("bar","baz").execute().assertStatus(200);
		b1h.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b01p_complex_fail() throws Exception {
		// @Rest(roleGuard="foo || (bar && baz)")
		b1h.get().roles().execute().assertStatus(403);
		b1h.get().roles("bar").execute().assertStatus(403);
		b1h.get().roles("baz").execute().assertStatus(403);
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

	@Rest
	public static class B2b {
		@RestMethod(roleGuard="foo | bar")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2c {
		@RestMethod(roleGuard="foo || bar")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2d {
		@RestMethod(roleGuard="foo & bar")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2e {
		@RestMethod(roleGuard="foo && bar")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2f {
		@RestMethod(roleGuard="(foo) && (bar)")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2g {
		@RestMethod(roleGuard="foo && (bar || baz)")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class B2h {
		@RestMethod(roleGuard="foo || (bar && baz)")
		public String get() {
			return "OK";
		}
	}

	static MockRest b2a = MockRest.build(B2a.class);
	static MockRest b2b = MockRest.build(B2b.class);
	static MockRest b2c = MockRest.build(B2c.class);
	static MockRest b2d = MockRest.build(B2d.class);
	static MockRest b2e = MockRest.build(B2e.class);
	static MockRest b2f = MockRest.build(B2f.class);
	static MockRest b2g = MockRest.build(B2g.class);
	static MockRest b2h = MockRest.build(B2h.class);

	@Test
	public void b02a_orsWithComma_pass() throws Exception {
		// @RestMethod(roleGuard="foo,bar")
		b2a.get().roles("foo").execute().assertStatus(200);
		b2a.get().roles("bar").execute().assertStatus(200);
		b2a.get().roles("foo","bar").execute().assertStatus(200);
		b2a.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02b_orsWithComma_fail() throws Exception {
		// @RestMethod(roleGuard="foo,bar")
		b2a.get().roles().execute().assertStatus(403);
		b2a.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02c_orsWithSinglePipe_pass() throws Exception {
		// @RestMethod(roleGuard="foo | bar")
		b2b.get().roles("foo").execute().assertStatus(200);
		b2b.get().roles("bar").execute().assertStatus(200);
		b2b.get().roles("foo","bar").execute().assertStatus(200);
		b2b.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02d_orsWithSinglePipe_fail() throws Exception {
		// @RestMethod(roleGuard="foo | bar")
		b2b.get().roles().execute().assertStatus(403);
		b2b.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02e_orsWithDoublePipe_pass() throws Exception {
		// @RestMethod(roleGuard="foo || bar")
		b2c.get().roles("foo").execute().assertStatus(200);
		b2c.get().roles("bar").execute().assertStatus(200);
		b2c.get().roles("foo","bar").execute().assertStatus(200);
		b2c.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02f_orsWithDoublePipe_fail() throws Exception {
		// @RestMethod(roleGuard="foo || bar")
		b2c.get().roles().execute().assertStatus(403);
		b2c.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02g_andsWithSingleAmp_pass() throws Exception {
		// @RestMethod(roleGuard="foo & bar")
		b2d.get().roles("foo","bar").execute().assertStatus(200);
		b2d.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02h_andsWithSingleAmp_fail() throws Exception {
		// @RestMethod(roleGuard="foo & bar")
		b2d.get().roles().execute().assertStatus(403);
		b2d.get().roles("foo").execute().assertStatus(403);
		b2d.get().roles("bar").execute().assertStatus(403);
		b2d.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02i_andsWithDoubleAmp_pass() throws Exception {
		// @RestMethod(roleGuard="foo && bar")
		b2e.get().roles("foo","bar").execute().assertStatus(200);
		b2e.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02j_andsWithDoubleAmp_fail() throws Exception {
		// @RestMethod(roleGuard="foo && bar")
		b2e.get().roles().execute().assertStatus(403);
		b2e.get().roles("foo").execute().assertStatus(403);
		b2e.get().roles("bar").execute().assertStatus(403);
		b2e.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02k_andsWithDoubleAmpAndParens_pass() throws Exception {
		// @RestMethod(roleGuard="(foo) && (bar)")
		b2f.get().roles("foo","bar").execute().assertStatus(200);
		b2f.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02l_andsWithDoubleAmpAndParens_fail() throws Exception {
		// @RestMethod(roleGuard="(foo) && (bar)")
		b2f.get().roles().execute().assertStatus(403);
		b2f.get().roles("foo").execute().assertStatus(403);
		b2f.get().roles("bar").execute().assertStatus(403);
		b2f.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02m_complex_pass() throws Exception {
		// @RestMethod(roleGuard="foo && (bar || baz)")
		b2g.get().roles("foo","bar").execute().assertStatus(200);
		b2g.get().roles("foo","baz").execute().assertStatus(200);
		b2g.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02n_complex_fail() throws Exception {
		// @RestMethod(roleGuard="foo && (bar || baz)")
		b2g.get().roles().execute().assertStatus(403);
		b2g.get().roles("foo").execute().assertStatus(403);
		b2g.get().roles("bar","baz").execute().assertStatus(403);
		b2g.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void b02o_complex_pass() throws Exception {
		// @RestMethod(roleGuard="foo || (bar && baz)")
		b2h.get().roles("foo").execute().assertStatus(200);
		b2h.get().roles("bar","baz").execute().assertStatus(200);
		b2h.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void b02p_complex_fail() throws Exception {
		// @RestMethod(roleGuard="foo || (bar && baz)")
		b2h.get().roles().execute().assertStatus(403);
		b2h.get().roles("bar").execute().assertStatus(403);
		b2h.get().roles("baz").execute().assertStatus(403);
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

	@Rest(roleGuard="fo* | *ar")
	public static class C1b extends C1 {}

	@Rest(roleGuard="fo* || *ar")
	public static class C1c extends C1 {}

	@Rest(roleGuard="fo* & *ar")
	public static class C1d extends C1 {}

	@Rest(roleGuard="fo* && *ar")
	public static class C1e extends C1 {}

	@Rest(roleGuard="(fo*) && (*ar)")
	public static class C1f extends C1 {}

	@Rest(roleGuard="fo* && (*ar || *az)")
	public static class C1g extends C1 {}

	@Rest(roleGuard="fo* || (*ar && *az)")
	public static class C1h extends C1 {}

	static MockRest c1a = MockRest.build(C1a.class);
	static MockRest c1b = MockRest.build(C1b.class);
	static MockRest c1c = MockRest.build(C1c.class);
	static MockRest c1d = MockRest.build(C1d.class);
	static MockRest c1e = MockRest.build(C1e.class);
	static MockRest c1f = MockRest.build(C1f.class);
	static MockRest c1g = MockRest.build(C1g.class);
	static MockRest c1h = MockRest.build(C1h.class);

	@Test
	public void c01a_orPatternsWithComma_pass() throws Exception {
		// @Rest(roleGuard="fo*,*ar")
		c1a.get().roles("foo").execute().assertStatus(200);
		c1a.get().roles("bar").execute().assertStatus(200);
		c1a.get().roles("foo","bar").execute().assertStatus(200);
		c1a.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01b_orPatternsWithComma_fail() throws Exception {
		// @Rest(roleGuard="fo*,*ar")
		c1a.get().roles().execute().assertStatus(403);
		c1a.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01c_orPatternsWithSinglePipe_pass() throws Exception {
		// @Rest(roleGuard="fo* | *ar")
		c1b.get().roles("foo").execute().assertStatus(200);
		c1b.get().roles("bar").execute().assertStatus(200);
		c1b.get().roles("foo","bar").execute().assertStatus(200);
		c1b.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01d_orPatternsWithSinglePipe_fail() throws Exception {
		// @Rest(roleGuard="fo* | *ar")
		c1b.get().roles().execute().assertStatus(403);
		c1b.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01e_orPatternsWithDoublePipe_pass() throws Exception {
		// @Rest(roleGuard="fo* || *ar")
		c1c.get().roles("foo").execute().assertStatus(200);
		c1c.get().roles("bar").execute().assertStatus(200);
		c1c.get().roles("foo","bar").execute().assertStatus(200);
		c1c.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01f_orPatternsWithDoublePipe_fail() throws Exception {
		// @Rest(roleGuard="fo* || *ar")
		c1c.get().roles().execute().assertStatus(403);
		c1c.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01g_andPatternsWithSingleAmp_pass() throws Exception {
		// @Rest(roleGuard="fo* & *ar")
		c1d.get().roles("foo","bar").execute().assertStatus(200);
		c1d.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01h_andPatternsWithSingleAmp_fail() throws Exception {
		// @Rest(roleGuard="fo* & *ar")
		c1d.get().roles().execute().assertStatus(403);
		c1d.get().roles("foo").execute().assertStatus(403);
		c1d.get().roles("bar").execute().assertStatus(403);
		c1d.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01i_andPatternsWithDoubleAmp_pass() throws Exception {
		// @Rest(roleGuard="fo* && *ar")
		c1e.get().roles("foo","bar").execute().assertStatus(200);
		c1e.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01j_andPatternsWithDoubleAmp_fail() throws Exception {
		// @Rest(roleGuard="fo* && *ar")
		c1e.get().roles().execute().assertStatus(403);
		c1e.get().roles("foo").execute().assertStatus(403);
		c1e.get().roles("bar").execute().assertStatus(403);
		c1e.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01k_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		// @Rest(roleGuard="(fo*) && (*ar)")
		c1f.get().roles("foo","bar").execute().assertStatus(200);
		c1f.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01l_andPatternsWithDoubleAmpAndParens_fail() throws Exception {
		// @Rest(roleGuard="(fo*) && (*ar)")
		c1f.get().roles().execute().assertStatus(403);
		c1f.get().roles("foo").execute().assertStatus(403);
		c1f.get().roles("bar").execute().assertStatus(403);
		c1f.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01m_complexPatterns_pass() throws Exception {
		// @Rest(roleGuard="fo* && (*ar || *az)")
		c1g.get().roles("foo","bar").execute().assertStatus(200);
		c1g.get().roles("foo","baz").execute().assertStatus(200);
		c1g.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01n_complexPatterns_fail() throws Exception {
		// @Rest(roleGuard="fo* && (*ar || *az)")
		c1g.get().roles().execute().assertStatus(403);
		c1g.get().roles("foo").execute().assertStatus(403);
		c1g.get().roles("bar","baz").execute().assertStatus(403);
		c1g.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c01o_complexPatterns_pass() throws Exception {
		// @Rest(roleGuard="fo* || (*ar && *az)")
		c1h.get().roles("foo").execute().assertStatus(200);
		c1h.get().roles("bar","baz").execute().assertStatus(200);
		c1h.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c01p_complexPatterns_fail() throws Exception {
		// @Rest(roleGuard="fo* || (*ar && *az)")
		c1h.get().roles().execute().assertStatus(403);
		c1h.get().roles("bar").execute().assertStatus(403);
		c1h.get().roles("baz").execute().assertStatus(403);
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

	@Rest
	public static class C2b {
		@RestMethod(roleGuard="fo* | *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2c {
		@RestMethod(roleGuard="fo* || *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2d {
		@RestMethod(roleGuard="fo* & *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2e {
		@RestMethod(roleGuard="fo* && *ar",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2f {
		@RestMethod(roleGuard="(fo*) && (*ar)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2g {
		@RestMethod(roleGuard="fo* && (*ar || *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	@Rest
	public static class C2h {
		@RestMethod(roleGuard="fo* || (*ar && *az)",rolesDeclared="foo,bar,baz")
		public String get() {
			return "OK";
		}
	}

	static MockRest c2a = MockRest.build(C2a.class);
	static MockRest c2b = MockRest.build(C2b.class);
	static MockRest c2c = MockRest.build(C2c.class);
	static MockRest c2d = MockRest.build(C2d.class);
	static MockRest c2e = MockRest.build(C2e.class);
	static MockRest c2f = MockRest.build(C2f.class);
	static MockRest c2g = MockRest.build(C2g.class);
	static MockRest c2h = MockRest.build(C2h.class);

	@Test
	public void c02a_orPatternsWithComma_pass() throws Exception {
		// @RestMethod(roleGuard="fo*,*ar")
		c2a.get().roles("foo").execute().assertStatus(200);
		c2a.get().roles("bar").execute().assertStatus(200);
		c2a.get().roles("foo","bar").execute().assertStatus(200);
		c2a.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02b_orPatternsWithComma_fail() throws Exception {
		// @RestMethod(roleGuard="fo*,*ar")
		c2a.get().roles().execute().assertStatus(403);
		c2a.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02c_orPatternsWithSinglePipe_pass() throws Exception {
		// @RestMethod(roleGuard="fo* | *ar")
		c2b.get().roles("foo").execute().assertStatus(200);
		c2b.get().roles("bar").execute().assertStatus(200);
		c2b.get().roles("foo","bar").execute().assertStatus(200);
		c2b.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02d_orPatternsWithSinglePipe_fail() throws Exception {
		// @RestMethod(roleGuard="fo* | *ar")
		c2b.get().roles().execute().assertStatus(403);
		c2b.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02e_orPatternsWithDoublePipe_pass() throws Exception {
		// @RestMethod(roleGuard="fo* || *ar")
		c2c.get().roles("foo").execute().assertStatus(200);
		c2c.get().roles("bar").execute().assertStatus(200);
		c2c.get().roles("foo","bar").execute().assertStatus(200);
		c2c.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02f_orPatternsWithDoublePipe_fail() throws Exception {
		// @RestMethod(roleGuard="foo || bar")
		c2c.get().roles().execute().assertStatus(403);
		c2c.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02g_andPatternsWithSingleAmp_pass() throws Exception {
		// @RestMethod(roleGuard="fo* & *ar")
		c2d.get().roles("foo","bar").execute().assertStatus(200);
		c2d.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02h_andPatternsWithSingleAmp_fail() throws Exception {
		// @RestMethod(roleGuard="fo* & *ar")
		c2d.get().roles().execute().assertStatus(403);
		c2d.get().roles("foo").execute().assertStatus(403);
		c2d.get().roles("bar").execute().assertStatus(403);
		c2d.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02i_andPatternsWithDoubleAmp_pass() throws Exception {
		// @RestMethod(roleGuard="fo* && *ar")
		c2e.get().roles("foo","bar").execute().assertStatus(200);
		c2e.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02j_andPatternsWithDoubleAmp_fail() throws Exception {
		// @RestMethod(roleGuard="fo* && *ar")
		c2e.get().roles().execute().assertStatus(403);
		c2e.get().roles("foo").execute().assertStatus(403);
		c2e.get().roles("bar").execute().assertStatus(403);
		c2e.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02k_andPatternsWithDoubleAmpAndParens_pass() throws Exception {
		// @RestMethod(roleGuard="(fo*) && (*ar)")
		c2f.get().roles("foo","bar").execute().assertStatus(200);
		c2f.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02l_andPatternsWithDoubleAmpAndParens_fail() throws Exception {
		// @RestMethod(roleGuard="(fo*) && (*ar)")
		c2f.get().roles().execute().assertStatus(403);
		c2f.get().roles("foo").execute().assertStatus(403);
		c2f.get().roles("bar").execute().assertStatus(403);
		c2f.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02m_complexPatterns_pass() throws Exception {
		// @RestMethod(roleGuard="fo* && (*ar || *az)")
		c2g.get().roles("foo","bar").execute().assertStatus(200);
		c2g.get().roles("foo","baz").execute().assertStatus(200);
		c2g.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02n_complexPatterns_fail() throws Exception {
		// @RestMethod(roleGuard="fo* && (*ar || *az)")
		c2g.get().roles().execute().assertStatus(403);
		c2g.get().roles("foo").execute().assertStatus(403);
		c2g.get().roles("bar","baz").execute().assertStatus(403);
		c2g.get().roles("baz").execute().assertStatus(403);
	}

	@Test
	public void c02o_complexPatterns_pass() throws Exception {
		// @RestMethod(roleGuard="fo* || (*ar && *az)")
		c2h.get().roles("foo").execute().assertStatus(200);
		c2h.get().roles("bar","baz").execute().assertStatus(200);
		c2h.get().roles("foo","bar","baz").execute().assertStatus(200);
	}

	@Test
	public void c02p_complexPatterns_fail() throws Exception {
		// @RestMethod(roleGuard="fo* || (*ar && *az)")
		c2h.get().roles().execute().assertStatus(403);
		c2h.get().roles("bar").execute().assertStatus(403);
		c2h.get().roles("baz").execute().assertStatus(403);
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

	static MockRest d = MockRest.build(D.class);

	@Test
	public void d01_patternsWithoutRoles_fail() throws Exception {
		// @RestMethod(roleGuard="fo*,*bar")
		d.get().roles().execute().assertStatus(403);
		d.get().roles("foo").execute().assertStatus(403);
		d.get().roles("bar").execute().assertStatus(403);
		d.get().roles("baz").execute().assertStatus(403);
		d.get().roles("foo","bar").execute().assertStatus(403);
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

	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01_anyRole_pass() throws Exception {
		// @RestMethod(roleGuard="*")
		e.get().roles("foo").execute().assertStatus(200);
		e.get().roles("bar").execute().assertStatus(200);
		e.get().roles("baz").execute().assertStatus(200);
		e.get().roles("foo","bar").execute().assertStatus(200);
	}

	@Test
	public void e02_anyRole_fail() throws Exception {
		// @RestMethod(roleGuard="*")
		e.get().roles().execute().assertStatus(403);
	}
}
