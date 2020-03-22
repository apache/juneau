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
package org.apache.juneau.urlencoding;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.urlencoding.annotation.*;

public class DTOs2 {

	@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
	@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
	public static class Annotations {}

	public static class A {
		public String a;
		public int b;
		public boolean c;

		public static A create() {
			A t = new A();
			t.a = "a";
			t.b = 1;
			t.c = true;
			return t;
		}

	}

	public static class B {
		public String[] f01;
		public List<String> f02;
		public int[] f03;
		public List<Integer> f04;
		public String[][] f05;
		public List<String[]> f06;
		public A[] f07;
		public List<A> f08;
		public A[][] f09;
		public List<List<A>> f10;

		private String[] f11;
		private List<String> f12;
		private int[] f13;
		private List<Integer> f14;
		private String[][] f15;
		private List<String[]> f16;
		private A[] f17;
		private List<A> f18;
		private A[][] f19;
		private List<List<A>> f20;

		public String[] getF11() { return f11; }
		public List<String> getF12() { return f12; }
		public int[] getF13() { return f13; }
		public List<Integer> getF14() { return f14; }
		public String[][] getF15() { return f15; }
		public List<String[]> getF16() { return f16; }
		public A[] getF17() { return f17; }
		public List<A> getF18() { return f18; }
		public A[][] getF19() { return f19; }
		public List<List<A>> getF20() { return f20; }

		public void setF11(String[] f11) { this.f11 = f11; }
		public void setF12(List<String> f12) { this.f12 = f12; }
		public void setF13(int[] f13) { this.f13 = f13; }
		public void setF14(List<Integer> f14) { this.f14 = f14; }
		public void setF15(String[][] f15) { this.f15 = f15; }
		public void setF16(List<String[]> f16) { this.f16 = f16; }
		public void setF17(A[] f17) { this.f17 = f17; }
		public void setF18(List<A> f18) { this.f18 = f18; }
		public void setF19(A[][] f19) { this.f19 = f19; }
		public void setF20(List<List<A>> f20) { this.f20 = f20; }

		static B create() {
			B t = new B();
			t.f01 = new String[]{"a","b"};
			t.f02 = AList.of("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = AList.of(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = AList.of(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new A[]{A.create(),A.create()};
			t.f08 = AList.of(A.create(),A.create());
			t.f09 = new A[][]{{A.create()},{A.create()}};
			t.f10 = AList.of(Arrays.asList(A.create()),Arrays.asList(A.create()));
			t.setF11(new String[]{"a","b"});
			t.setF12(AList.of("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(AList.of(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(AList.of(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new A[]{A.create(),A.create()});
			t.setF18(AList.of(A.create(),A.create()));
			t.setF19(new A[][]{{A.create()},{A.create()}});
			t.setF20(AList.of(Arrays.asList(A.create()),Arrays.asList(A.create())));
			return t;
		}
	}

	public static class C extends B {
		static C create() {
			C t = new C();
			t.f01 = new String[]{"a","b"};
			t.f02 = AList.of("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = AList.of(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = AList.of(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new A[]{A.create(),A.create()};
			t.f08 = AList.of(A.create(),A.create());
			t.f09 = new A[][]{{A.create()},{A.create()}};
			t.f10 = AList.of(Arrays.asList(A.create()),Arrays.asList(A.create()));
			t.setF11(new String[]{"a","b"});
			t.setF12(AList.of("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(AList.of(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(AList.of(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new A[]{A.create(),A.create()});
			t.setF18(AList.of(A.create(),A.create()));
			t.setF19(new A[][]{{A.create()},{A.create()}});
			t.setF20(AList.of(Arrays.asList(A.create()),Arrays.asList(A.create())));
			return t;
		}
	}
}
