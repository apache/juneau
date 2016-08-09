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
package org.apache.juneau.server;


import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.transform.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testTransforms",
	transforms={TestTransforms.TransformA2.class}
)
public class TestTransforms extends TestTransformsParent {
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
	public A test1b(@Content A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testClassTransformOverridesParentClassTransform/{a}")
	public A test1c(@Attr A a) {
		return a;
	}

	//====================================================================================================
	// Test method transform overrides class transform
	// Should return "A3-1".
	//====================================================================================================
	@RestMethod(name="GET", path="/testMethodTransformOverridesClassTransform", transforms={TransformA3.class})
	public A test2a() {
		return new A();
	}
	@RestMethod(name="PUT", path="/testMethodTransformOverridesClassTransform", transforms={TransformA3.class})
	public A test2b(@Content A a) {
		return a;
	}
	@RestMethod(name="PUT", path="/testMethodTransformOverridesClassTransform/{a}", transforms={TransformA3.class})
	public A test2c(@Attr A a) {
		return a;
	}


	public static class A {
		public int f1;
	}

	public static class TransformA1 extends PojoTransform<A,String> {
		@Override /* PojoTransform */
		public String transform(A a) throws SerializeException {
			return "A1-" + a.f1;
		}
		@Override /* PojoTransform */
		public A normalize(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A1"))
				throw new RuntimeException("Invalid input for TransformA1!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class TransformA2 extends PojoTransform<A,String> {
		@Override /* PojoTransform */
		public String transform(A a) throws SerializeException {
			return "A2-" + a.f1;
		}
		@Override /* PojoTransform */
		public A normalize(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A2"))
				throw new RuntimeException("Invalid input for TransformA2!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}

	public static class TransformA3 extends PojoTransform<A,String> {
		@Override /* PojoTransform */
		public String transform(A a) throws SerializeException {
			return "A3-" + a.f1;
		}
		@Override /* PojoTransform */
		public A normalize(String in, ClassMeta<?> hint) throws ParseException {
			if (! in.startsWith("A3"))
				throw new RuntimeException("Invalid input for TransformA3!");
			A a = new A();
			a.f1 = Integer.parseInt(in.substring(3));
			return a;
		}
	}
}
