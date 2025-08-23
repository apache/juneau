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
package org.apache.juneau.a;

import org.apache.juneau.annotation.*;

// Default class
@SuppressWarnings({"unused"})
public class A1 {
	public int f1;
	protected int f2;
	int f3;
	private int f4;

	@BeanIgnore
	private int f5, f6, f7, f8;

	public int getF5() { return f5; }
	public void setF5(int v) { this.f5 = v; }
	protected int getF6() { return f6; }
	protected void setF6(int v) { this.f6 = v; }
	int getF7() { return f7; }
	void setF7(int v) { this.f7 = v; }
	private int getF8() { return f8; }
	private void setF8(int v) { this.f8 = v; }

	public A2 g2;
	public A3 g3;
	public A4 g4;
	public A5 g5;

	public static A1 create() {
		A1 x = new A1();
		x.f1 = 1;
		x.f2 = 2;
		x.f3 = 3;
		x.f4 = 4;
		x.f5 = 5;
		x.f6 = 6;
		x.f7 = 7;
		x.f8 = 8;
		x.g2 = new A2();
		x.g2.f1 = 1;
		x.g2.f2 = 2;
		x.g2.f3 = 3;
		x.g2.f4 = 4;
		x.g2.f5 = 5;
		x.g2.f6 = 6;
		x.g2.f7 = 7;
		x.g2.f8 = 8;
		x.g3 = new A3();
		x.g3.f1 = 1;
		x.g3.f2 = 2;
		x.g3.f3 = 3;
		x.g3.f4 = 4;
		x.g3.f5 = 5;
		x.g3.f6 = 6;
		x.g3.f7 = 7;
		x.g3.f8 = 8;
		x.g4 = new A4();
		x.g4.f1 = 1;
		x.g4.f2 = 2;
		x.g4.f3 = 3;
		x.g4.f4 = 4;
		x.g4.f5 = 5;
		x.g4.f6 = 6;
		x.g4.f7 = 7;
		x.g4.f8 = 8;
		x.g5 = new A5();
		x.g5.f1 = 1;
		x.g5.f2 = 2;
		x.g5.f3 = 3;
		x.g5.f4 = 4;
		x.g5.f5 = 5;
		x.g5.f6 = 6;
		x.g5.f7 = 7;
		x.g5.f8 = 8;
		return x;
	}

	public static class A2 {
		public int f1;
		protected int f2;
		int f3;
		private int f4;

		@BeanIgnore
		private int f5, f6, f7, f8;

		public int getF5() { return f5; }
		public void setF5(int v) { this.f5 = v; }
		protected int getF6() { return f6; }
		protected void setF6(int v) { this.f6 = v; }
		int getF7() { return f7; }
		void setF7(int v) { this.f7 = v; }
		private int getF8() { return f8; }
		private void setF8(int v) { this.f8 = v; }

		@Override /* Object */
		public String toString() {
			return "A2";
		}
	}

	protected static class A3 {
		public int f1;
		protected int f2;
		int f3;
		private int f4;

		@BeanIgnore
		private int f5, f6, f7, f8;

		public int getF5() { return f5; }
		public void setF5(int v) { this.f5 = v; }
		protected int getF6() { return f6; }
		protected void setF6(int v) { this.f6 = v; }
		int getF7() { return f7; }
		void setF7(int v) { this.f7 = v; }
		private int getF8() { return f8; }
		private void setF8(int v) { this.f8 = v; }

		@Override /* Object */
		public String toString() {
			return "A3";
		}
	}

	static class A4 {
		public int f1;
		protected int f2;
		int f3;
		private int f4;

		@BeanIgnore
		private int f5, f6, f7, f8;

		public int getF5() { return f5; }
		public void setF5(int v) { this.f5 = v; }
		protected int getF6() { return f6; }
		protected void setF6(int v) { this.f6 = v; }
		int getF7() { return f7; }
		void setF7(int v) { this.f7 = v; }
		private int getF8() { return f8; }
		private void setF8(int v) { this.f8 = v; }

		@Override /* Object */
		public String toString() {
			return "A4";
		}
	}

	private static class A5 {
		public int f1;
		protected int f2;
		int f3;
		private int f4;

		@BeanIgnore
		private int f5, f6, f7, f8;

		public int getF5() { return f5; }
		public void setF5(int v) { this.f5 = v; }
		protected int getF6() { return f6; }
		protected void setF6(int v) { this.f6 = v; }
		int getF7() { return f7; }
		void setF7(int v) { this.f7 = v; }
		private int getF8() { return f8; }
		private void setF8(int v) { this.f8 = v; }

		@Override /* Object */
		public String toString() {
			return "A5";
		}
	}
}
