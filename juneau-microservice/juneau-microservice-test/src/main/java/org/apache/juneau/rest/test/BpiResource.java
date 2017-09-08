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
package org.apache.juneau.rest.test;

import org.apache.juneau.annotation.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testBpi"
)
public class BpiResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Validates that the @RestMethod(bpIncludes,bpExcludes) properties work.
	//====================================================================================================

	@RestMethod(name="GET", path="/test/a1", bpi="MyBeanA: a,_b")
	public Object testA1() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/a2", bpi="MyBeanA: a")
	public Object testA2() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/a3", bpi="MyBeanA: _b")
	public Object testA3() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/a4", bpi="MyBeanA: a")
	public Object testA4() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/a5", bpi="MyBeanA: _b")
	public Object testA5() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/a6", bpi="MyBeanA: a,_b")
	public Object testA6() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/b1", bpi="MyBeanB: a,_b")
	public Object testB1() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/b2", bpi="MyBeanB: a")
	public Object testB2() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/b3", bpi="MyBeanB: _b")
	public Object testB3() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/b4", bpi="MyBeanB: a")
	public Object testB4() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/b5", bpi="MyBeanB: _b'")
	public Object testB5() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/b6", bpi="MyBeanB: a,_b")
	public Object testB6() throws Exception {
		return new MyBeanB().init();
	}

	@RestMethod(name="GET", path="/test/c1", bpi="*: a")
	public Object testC1() throws Exception {
		return new MyBeanA().init();
	}

	@RestMethod(name="GET", path="/test/c2", bpi="org.apache.juneau.rest.test.BpIncludesResource$MyBeanA: a")
	public Object testC2() throws Exception {
		return new MyBeanA().init();
	}

	// Should not match.
	@RestMethod(name="GET", path="/test/d1", bpi="MyBean: a")
	public Object testD1() throws Exception {
		return new MyBeanA().init();
	}

	// Should not match.
	@RestMethod(name="GET", path="/test/d2", bpi="MyBean*: a")
	public Object testD2() throws Exception {
		return new MyBeanA().init();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Beans
	//-------------------------------------------------------------------------------------------------------------------

	public static class MyBeanA {
		public int a;
		@BeanProperty("_b") public String b;

		MyBeanA init() {
			a = 1;
			b = "foo";
			return this;
		}
	}

	@Bean(properties="_b,a")
	public static class MyBeanB {
		public int a;
		@BeanProperty("_b") public String b;

		MyBeanB init() {
			a = 1;
			b = "foo";
			return this;
		}
	}
}
