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
package org.apache.juneau.server.test;

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.transform.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testTransforms",
	pojoSwaps={TransformsResource.SwapA2.class}
)
public class TransformsResource extends TransformsParentResource {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test class transform overrides parent class transform
	// Should return "A2-1".
	//====================================================================================================
	@RestMethod(name="GET", path="/testClassTransformOverridesParentClassTransform")
	public A testClassTransformOverridesParentClassTransform() {
		return new A();
	}
	@RestMethod(name="PUT", path="/testClassTransformOverridesParentClassTransform")
	public A test1b(@Body A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testClassTransformOverridesParentClassTransform/{a}")
	public A test1c(@Path A a) {
		return a;
	}

	//====================================================================================================
	// Test method transform overrides class transform
	// Should return "A3-1".
	//====================================================================================================
	@RestMethod(name="GET", path="/testMethodTransformOverridesClassTransform", pojoSwaps={SwapA3.class})
	public A test2a() {
		return new A();
	}
	@RestMethod(name="PUT", path="/testMethodTransformOverridesClassTransform", pojoSwaps={SwapA3.class})
	public A test2b(@Body A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testMethodTransformOverridesClassTransform/{a}", pojoSwaps={SwapA3.class})
	public A test2c(@Path A a) {
		return a;
	}


	public static class A {
		public int f1;
	}

	public static class SwapA1 extends PojoSwap<A,String> {
		@Override /* PojoSwap */
		public String swap(A a) throws SerializeException {
			return "A1-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(String in) throws ParseException {
			if (! in.startsWith("A1"))
				throw new RuntimeException("Invalid input for SwapA1!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class SwapA2 extends PojoSwap<A,String> {
		@Override /* PojoSwap */
		public String swap(A a) throws SerializeException {
			return "A2-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(String in) throws ParseException {
			if (! in.startsWith("A2"))
				throw new RuntimeException("Invalid input for SwapA2!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class SwapA3 extends PojoSwap<A,String> {
		@Override /* PojoSwap */
		public String swap(A a) throws SerializeException {
			return "A3-" + a.f1;
		}
		@Override /* PojoSwap */
		public A unswap(String in) throws ParseException {
			if (! in.startsWith("A3"))
				throw new RuntimeException("Invalid input for SwapA3!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}
}
