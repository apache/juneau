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
package org.apache.juneau.testutils.pojos;

import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;

public class XBeans {

	private XBeans() {}

	@Bean(sort=true)
	public static class XA {
		public String a;
		public int b;
		public boolean c;

		public static XA get() {
			var t = new XA();
			t.a = "a";
			t.b = 1;
			t.c = true;
			return t;
		}

	}

	@Bean(sort=true)
	public static class XB {  // NOSONAR
		public String[] f01;
		public List<String> f02;
		public int[] f03;
		public List<Integer> f04;
		public String[][] f05;
		public List<String[]> f06;
		public XA[] f07;
		public List<XA> f08;
		public XA[][] f09;
		public List<List<XA>> f10;

		private String[] f11;
		private List<String> f12;
		private int[] f13;
		private List<Integer> f14;
		private String[][] f15;
		private List<String[]> f16;
		private XA[] f17;
		private List<XA> f18;
		private XA[][] f19;
		private List<List<XA>> f20;

		public String[] getF11() { return f11; }
		public List<String> getF12() { return f12; }
		public int[] getF13() { return f13; }
		public List<Integer> getF14() { return f14; }
		public String[][] getF15() { return f15; }
		public List<String[]> getF16() { return f16; }
		public XA[] getF17() { return f17; }
		public List<XA> getF18() { return f18; }
		public XA[][] getF19() { return f19; }
		public List<List<XA>> getF20() { return f20; }

		public void setF11(String[] v) { f11 = v; }
		public void setF12(List<String> v) { f12 = v; }
		public void setF13(int[] v) { f13 = v; }
		public void setF14(List<Integer> v) { f14 = v; }
		public void setF15(String[][] v) { f15 = v; }
		public void setF16(List<String[]> v) { f16 = v; }
		public void setF17(XA[] v) { f17 = v; }
		public void setF18(List<XA> v) { f18 = v; }
		public void setF19(XA[][] v) { f19 = v; }
		public void setF20(List<List<XA>> v) { f20 = v; }

		public static XB get() {
			var t = new XB();
			t.f01 = new String[]{"a","b"};
			t.f02 = ulist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = ulist(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = ulist(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new XA[]{XA.get(),XA.get()};
			t.f08 = ulist(XA.get(),XA.get());
			t.f09 = new XA[][]{{XA.get()},{XA.get()}};
			t.f10 = ulist(Arrays.asList(XA.get()),Arrays.asList(XA.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(ulist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(ulist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(ulist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XA[]{XA.get(),XA.get()});
			t.setF18(ulist(XA.get(),XA.get()));
			t.setF19(new XA[][]{{XA.get()},{XA.get()}});
			t.setF20(ulist(Arrays.asList(XA.get()),Arrays.asList(XA.get())));
			return t;
		}

		public static final XB INSTANCE = get();
	}

	@UrlEncoding(expandedParams=true)
	public static class XC extends XB {
		public static XC get() {
			var t = new XC();
			t.f01 = new String[]{"a","b"};
			t.f02 = ulist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = ulist(3, 4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = ulist(new String[]{"i","j"}, new String[]{"k","l"});
			t.f07 = new XA[]{XA.get(),XA.get()};
			t.f08 = ulist(XA.get(), XA.get());
			t.f09 = new XA[][]{{XA.get()},{XA.get()}};
			t.f10 = ulist(Arrays.asList(XA.get()), Arrays.asList(XA.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(ulist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(ulist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(ulist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XA[]{XA.get(),XA.get()});
			t.setF18(ulist(XA.get(), XA.get()));
			t.setF19(new XA[][]{{XA.get()},{XA.get()}});
			t.setF20(ulist(Arrays.asList(XA.get()), Arrays.asList(XA.get())));
			return t;
		}

		public static final XC INSTANCE = get();
	}

	@Bean(on="XD,XE,XF",sort=true)
	@UrlEncoding(on="C",expandedParams=true)
	public static class Annotations {}

	public static class XD {
		public String a;
		public int b;
		public boolean c;

		public static XD get() {
			var t = new XD();
			t.a = "a";
			t.b = 1;
			t.c = true;
			return t;
		}

	}

	public static class XE {  // NOSONAR
		public String[] f01;
		public List<String> f02;
		public int[] f03;
		public List<Integer> f04;
		public String[][] f05;
		public List<String[]> f06;
		public XD[] f07;
		public List<XD> f08;
		public XD[][] f09;
		public List<List<XD>> f10;

		private String[] f11;
		private List<String> f12;
		private int[] f13;
		private List<Integer> f14;
		private String[][] f15;
		private List<String[]> f16;
		private XD[] f17;
		private List<XD> f18;
		private XD[][] f19;
		private List<List<XD>> f20;

		public String[] getF11() { return f11; }
		public List<String> getF12() { return f12; }
		public int[] getF13() { return f13; }
		public List<Integer> getF14() { return f14; }
		public String[][] getF15() { return f15; }
		public List<String[]> getF16() { return f16; }
		public XD[] getF17() { return f17; }
		public List<XD> getF18() { return f18; }
		public XD[][] getF19() { return f19; }
		public List<List<XD>> getF20() { return f20; }

		public void setF11(String[] v) { f11 = v; }
		public void setF12(List<String> v) { f12 = v; }
		public void setF13(int[] v) { f13 = v; }
		public void setF14(List<Integer> v) { f14 = v; }
		public void setF15(String[][] v) { f15 = v; }
		public void setF16(List<String[]> v) { f16 = v; }
		public void setF17(XD[] v) { f17 = v; }
		public void setF18(List<XD> v) { f18 = v; }
		public void setF19(XD[][] v) { f19 = v; }
		public void setF20(List<List<XD>> v) { f20 = v; }

		public static XE get() {
			var t = new XE();
			t.f01 = new String[]{"a","b"};
			t.f02 = ulist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = ulist(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = ulist(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new XD[]{XD.get(),XD.get()};
			t.f08 = ulist(XD.get(),XD.get());
			t.f09 = new XD[][]{{XD.get()},{XD.get()}};
			t.f10 = ulist(Arrays.asList(XD.get()),Arrays.asList(XD.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(ulist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(ulist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(ulist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XD[]{XD.get(),XD.get()});
			t.setF18(ulist(XD.get(),XD.get()));
			t.setF19(new XD[][]{{XD.get()},{XD.get()}});
			t.setF20(ulist(Arrays.asList(XD.get()),Arrays.asList(XD.get())));
			return t;
		}

		public static final XE INSTANCE = get();
	}

	public static class XF extends XE {
		public static XF get() {
			var t = new XF();
			t.f01 = new String[]{"a","b"};
			t.f02 = ulist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = ulist(3, 4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = ulist(new String[]{"i","j"}, new String[]{"k","l"});
			t.f07 = new XD[]{XD.get(),XD.get()};
			t.f08 = ulist(XD.get(), XD.get());
			t.f09 = new XD[][]{{XD.get()},{XD.get()}};
			t.f10 = ulist(Arrays.asList(XD.get()), Arrays.asList(XD.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(ulist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(ulist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(ulist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XD[]{XD.get(),XD.get()});
			t.setF18(ulist(XD.get(), XD.get()));
			t.setF19(new XD[][]{{XD.get()},{XD.get()}});
			t.setF20(ulist(Arrays.asList(XD.get()), Arrays.asList(XD.get())));
			return t;
		}

		public static final XF INSTANCE = get();
	}
}