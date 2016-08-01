/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.a;

import org.apache.juneau.annotation.*;

// Default class
@SuppressWarnings({"unused","synthetic-access"})
public class A1 {
	public int f1;
	protected int f2;
	int f3;
	private int f4;

	@BeanIgnore
	private int f5, f6, f7, f8;

	public int getF5() { return f5; }
	public void setF5(int f5) { this.f5 = f5; }
	protected int getF6() { return f6; }
	protected void setF6(int f6) { this.f6 = f6; }
	int getF7() { return f7; }
	void setF7(int f7) { this.f7 = f7; }
	private int getF8() { return f8; }
	private void setF8(int f8) { this.f8 = f8; }

	public A2 a2;
	public A3 a3;
	public A4 a4;
	public A5 a5;

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
		x.a2 = new A2();
		x.a2.f1 = 1;
		x.a2.f2 = 2;
		x.a2.f3 = 3;
		x.a2.f4 = 4;
		x.a2.f5 = 5;
		x.a2.f6 = 6;
		x.a2.f7 = 7;
		x.a2.f8 = 8;
		x.a3 = new A3();
		x.a3.f1 = 1;
		x.a3.f2 = 2;
		x.a3.f3 = 3;
		x.a3.f4 = 4;
		x.a3.f5 = 5;
		x.a3.f6 = 6;
		x.a3.f7 = 7;
		x.a3.f8 = 8;
		x.a4 = new A4();
		x.a4.f1 = 1;
		x.a4.f2 = 2;
		x.a4.f3 = 3;
		x.a4.f4 = 4;
		x.a4.f5 = 5;
		x.a4.f6 = 6;
		x.a4.f7 = 7;
		x.a4.f8 = 8;
		x.a5 = new A5();
		x.a5.f1 = 1;
		x.a5.f2 = 2;
		x.a5.f3 = 3;
		x.a5.f4 = 4;
		x.a5.f5 = 5;
		x.a5.f6 = 6;
		x.a5.f7 = 7;
		x.a5.f8 = 8;
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
		public void setF5(int f5) { this.f5 = f5; }
		protected int getF6() { return f6; }
		protected void setF6(int f6) { this.f6 = f6; }
		int getF7() { return f7; }
		void setF7(int f7) { this.f7 = f7; }
		private int getF8() { return f8; }
		private void setF8(int f8) { this.f8 = f8; }

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
		public void setF5(int f5) { this.f5 = f5; }
		protected int getF6() { return f6; }
		protected void setF6(int f6) { this.f6 = f6; }
		int getF7() { return f7; }
		void setF7(int f7) { this.f7 = f7; }
		private int getF8() { return f8; }
		private void setF8(int f8) { this.f8 = f8; }

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
		public void setF5(int f5) { this.f5 = f5; }
		protected int getF6() { return f6; }
		protected void setF6(int f6) { this.f6 = f6; }
		int getF7() { return f7; }
		void setF7(int f7) { this.f7 = f7; }
		private int getF8() { return f8; }
		private void setF8(int f8) { this.f8 = f8; }

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
		public void setF5(int f5) { this.f5 = f5; }
		protected int getF6() { return f6; }
		protected void setF6(int f6) { this.f6 = f6; }
		int getF7() { return f7; }
		void setF7(int f7) { this.f7 = f7; }
		private int getF8() { return f8; }
		private void setF8(int f8) { this.f8 = f8; }

		@Override /* Object */
		public String toString() {
			return "A5";
		}
	}
}
