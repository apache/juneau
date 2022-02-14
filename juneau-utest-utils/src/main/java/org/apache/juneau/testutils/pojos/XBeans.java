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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;

public class XBeans {

	@Bean(sort=true)
	public static class XA {
		public String a;
		public int b;
		public boolean c;

		public static XA get() {
			XA t = new XA();
			t.a = "a";
			t.b = 1;
			t.c = true;
			return t;
		}

	}

	@Bean(sort=true)
	public static class XB {
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

		public void setF11(String[] f11) { this.f11 = f11; }
		public void setF12(List<String> f12) { this.f12 = f12; }
		public void setF13(int[] f13) { this.f13 = f13; }
		public void setF14(List<Integer> f14) { this.f14 = f14; }
		public void setF15(String[][] f15) { this.f15 = f15; }
		public void setF16(List<String[]> f16) { this.f16 = f16; }
		public void setF17(XA[] f17) { this.f17 = f17; }
		public void setF18(List<XA> f18) { this.f18 = f18; }
		public void setF19(XA[][] f19) { this.f19 = f19; }
		public void setF20(List<List<XA>> f20) { this.f20 = f20; }

		public static XB get() {
			XB t = new XB();
			t.f01 = new String[]{"a","b"};
			t.f02 = alist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = alist(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = alist(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new XA[]{XA.get(),XA.get()};
			t.f08 = alist(XA.get(),XA.get());
			t.f09 = new XA[][]{{XA.get()},{XA.get()}};
			t.f10 = alist(Arrays.asList(XA.get()),Arrays.asList(XA.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(alist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(alist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(alist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XA[]{XA.get(),XA.get()});
			t.setF18(alist(XA.get(),XA.get()));
			t.setF19(new XA[][]{{XA.get()},{XA.get()}});
			t.setF20(alist(Arrays.asList(XA.get()),Arrays.asList(XA.get())));
			return t;
		}

		public static XB INSTANCE = get();
	}

	@UrlEncoding(expandedParams=true)
	public static class XC extends XB {
		public static XC get() {
			XC t = new XC();
			t.f01 = new String[]{"a","b"};
			t.f02 = alist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = alist(3, 4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = alist(new String[]{"i","j"}, new String[]{"k","l"});
			t.f07 = new XA[]{XA.get(),XA.get()};
			t.f08 = alist(XA.get(), XA.get());
			t.f09 = new XA[][]{{XA.get()},{XA.get()}};
			t.f10 = alist(Arrays.asList(XA.get()), Arrays.asList(XA.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(alist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(alist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(alist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XA[]{XA.get(),XA.get()});
			t.setF18(alist(XA.get(), XA.get()));
			t.setF19(new XA[][]{{XA.get()},{XA.get()}});
			t.setF20(alist(Arrays.asList(XA.get()), Arrays.asList(XA.get())));
			return t;
		}

		public static XC INSTANCE = get();
	}


	@Bean(on="XD,XE,XF",sort=true)
	@UrlEncoding(on="C",expandedParams=true)
	public static class Annotations {}

	public static class XD {
		public String a;
		public int b;
		public boolean c;

		public static XD get() {
			XD t = new XD();
			t.a = "a";
			t.b = 1;
			t.c = true;
			return t;
		}

	}

	public static class XE {
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

		public void setF11(String[] f11) { this.f11 = f11; }
		public void setF12(List<String> f12) { this.f12 = f12; }
		public void setF13(int[] f13) { this.f13 = f13; }
		public void setF14(List<Integer> f14) { this.f14 = f14; }
		public void setF15(String[][] f15) { this.f15 = f15; }
		public void setF16(List<String[]> f16) { this.f16 = f16; }
		public void setF17(XD[] f17) { this.f17 = f17; }
		public void setF18(List<XD> f18) { this.f18 = f18; }
		public void setF19(XD[][] f19) { this.f19 = f19; }
		public void setF20(List<List<XD>> f20) { this.f20 = f20; }

		public static XE get() {
			XE t = new XE();
			t.f01 = new String[]{"a","b"};
			t.f02 = alist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = alist(3,4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = alist(new String[]{"i","j"},new String[]{"k","l"});
			t.f07 = new XD[]{XD.get(),XD.get()};
			t.f08 = alist(XD.get(),XD.get());
			t.f09 = new XD[][]{{XD.get()},{XD.get()}};
			t.f10 = alist(Arrays.asList(XD.get()),Arrays.asList(XD.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(alist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(alist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(alist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XD[]{XD.get(),XD.get()});
			t.setF18(alist(XD.get(),XD.get()));
			t.setF19(new XD[][]{{XD.get()},{XD.get()}});
			t.setF20(alist(Arrays.asList(XD.get()),Arrays.asList(XD.get())));
			return t;
		}

		public static XE INSTANCE = get();
	}

	public static class XF extends XE {
		public static XF get() {
			XF t = new XF();
			t.f01 = new String[]{"a","b"};
			t.f02 = alist("c","d");
			t.f03 = new int[]{1,2};
			t.f04 = alist(3, 4);
			t.f05 = new String[][]{{"e","f"},{"g","h"}};
			t.f06 = alist(new String[]{"i","j"}, new String[]{"k","l"});
			t.f07 = new XD[]{XD.get(),XD.get()};
			t.f08 = alist(XD.get(), XD.get());
			t.f09 = new XD[][]{{XD.get()},{XD.get()}};
			t.f10 = alist(Arrays.asList(XD.get()), Arrays.asList(XD.get()));
			t.setF11(new String[]{"a","b"});
			t.setF12(alist("c","d"));
			t.setF13(new int[]{1,2});
			t.setF14(alist(3,4));
			t.setF15(new String[][]{{"e","f"},{"g","h"}});
			t.setF16(alist(new String[]{"i","j"},new String[]{"k","l"}));
			t.setF17(new XD[]{XD.get(),XD.get()});
			t.setF18(alist(XD.get(), XD.get()));
			t.setF19(new XD[][]{{XD.get()},{XD.get()}});
			t.setF20(alist(Arrays.asList(XD.get()), Arrays.asList(XD.get())));
			return t;
		}

		public static XF INSTANCE = get();
	}
}
