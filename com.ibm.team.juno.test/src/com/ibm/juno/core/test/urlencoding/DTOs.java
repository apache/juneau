/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.urlencoding;

import java.util.*;

import com.ibm.juno.core.urlencoding.annotation.*;

public class DTOs {

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

	@SuppressWarnings("serial")
	public static class B {
		public String[] f1;
		public List<String> f2;
		public int[] f3;
		public List<Integer> f4;
		public String[][] f5;
		public List<String[]> f6;
		public A[] f7;
		public List<A> f8;
		public A[][] f9;
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
			t.f1 = new String[]{"a","b"};
			t.f2 = new ArrayList<String>(){{add("c");add("d");}};
			t.f3 = new int[]{1,2};
			t.f4 = new ArrayList<Integer>(){{add(3);add(4);}};
			t.f5 = new String[][]{{"e","f"},{"g","h"}};
			t.f6 = new ArrayList<String[]>(){{add(new String[]{"i","j"});add(new String[]{"k","l"});}};
			t.f7 = new A[]{A.create(),A.create()};
			t.f8 = new ArrayList<A>(){{add(A.create());add(A.create());}};
			t.f9 = new A[][]{{A.create()},{A.create()}};
			t.f10 = new ArrayList<List<A>>(){{add(Arrays.asList(A.create()));add(Arrays.asList(A.create()));}};
			t.setF11(new String[]{"a","b"});
			t.setF12(new ArrayList<String>(){{add("c");add("d");}});
			t.setF13(new int[]{1,2});
			t.setF14(new ArrayList<Integer>(){{add(3);add(4);}});
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(new ArrayList<String[]>(){{add(new String[]{"i","j"});add(new String[]{"k","l"});}});
			t.setF17(new A[]{A.create(),A.create()});
			t.setF18(new ArrayList<A>(){{add(A.create());add(A.create());}});
			t.setF19(new A[][]{{A.create()},{A.create()}});
			t.setF20(new ArrayList<List<A>>(){{add(Arrays.asList(A.create()));add(Arrays.asList(A.create()));}});
			return t;
		}
	}

	@UrlEncoding(expandedParams=true)
	public static class C extends B {
		@SuppressWarnings("serial")
		static C create() {
			C t = new C();
			t.f1 = new String[]{"a","b"};
			t.f2 = new ArrayList<String>(){{add("c");add("d");}};
			t.f3 = new int[]{1,2};
			t.f4 = new ArrayList<Integer>(){{add(3);add(4);}};
			t.f5 = new String[][]{{"e","f"},{"g","h"}};
			t.f6 = new ArrayList<String[]>(){{add(new String[]{"i","j"});add(new String[]{"k","l"});}};
			t.f7 = new A[]{A.create(),A.create()};
			t.f8 = new ArrayList<A>(){{add(A.create());add(A.create());}};
			t.f9 = new A[][]{{A.create()},{A.create()}};
			t.f10 = new ArrayList<List<A>>(){{add(Arrays.asList(A.create()));add(Arrays.asList(A.create()));}};
			t.setF11(new String[]{"a","b"});
			t.setF12(new ArrayList<String>(){{add("c");add("d");}});
			t.setF13(new int[]{1,2});
			t.setF14(new ArrayList<Integer>(){{add(3);add(4);}});
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(new ArrayList<String[]>(){{add(new String[]{"i","j"});add(new String[]{"k","l"});}});
			t.setF17(new A[]{A.create(),A.create()});
			t.setF18(new ArrayList<A>(){{add(A.create());add(A.create());}});
			t.setF19(new A[][]{{A.create()},{A.create()}});
			t.setF20(new ArrayList<List<A>>(){{add(Arrays.asList(A.create()));add(Arrays.asList(A.create()));}});
			return t;
		}
	}
}
