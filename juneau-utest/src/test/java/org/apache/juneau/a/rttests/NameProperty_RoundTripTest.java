/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.a.rttests;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class NameProperty_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// @NameProperty method.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_nameProperty(RoundTrip_Tester t) throws Exception {
		var x = new NamePropertyMethodContainer().init();
		x = t.roundTrip(x);
		assertBean(x, "bean{f2},m{k1{f2}}", "{2},{{2}}");
		if (t.isValidationOnly())
			return;
		assertBean(x, "bean{name}", "{bean}");
		assertBean(x, "m{k1{name}}", "{{k1}}");
	}

	public static class NamePropertyMethodContainer {
		public NamePropertyMethodBean bean;
		public Map<String,NamePropertyMethodBean> m;

		NamePropertyMethodContainer init() {
			bean = new NamePropertyMethodBean().init();
			m = new LinkedHashMap<>();
			m.put("k1", new NamePropertyMethodBean().init());
			return this;
		}

	}
	public static class NamePropertyMethodBean {
		String name;
		public int f2;

		@NameProperty
		protected void setName(String name) {
			this.name = name;
		}

		NamePropertyMethodBean init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @NameProperty field.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_namePropertyField(RoundTrip_Tester t) throws Exception {
		var x = new NamePropertyFieldContainer().init();
		x = t.roundTrip(x);
		assertBean(x, "bean{f2},m{k1{f2}}", "{2},{{2}}");
		if (t.isValidationOnly())
			return;
		assertBean(x, "bean{name}", "{bean}");
		assertBean(x, "m{k1{name}}", "{{k1}}");
	}

	public static class NamePropertyFieldContainer {
		public NamePropertyFieldBean bean;
		public Map<String,NamePropertyFieldBean> m;

		NamePropertyFieldContainer init() {
			bean = new NamePropertyFieldBean().init();
			m = new LinkedHashMap<>();
			m.put("k1", new NamePropertyFieldBean().init());
			return this;
		}
	}
	public static class NamePropertyFieldBean {
		@NameProperty
		public String name;
		public int f2;

		NamePropertyFieldBean init() {
			f2 = 2;
			return this;
		}
	}

	//====================================================================================================
	// @NameProperty read-only (getter only, no setter).
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_readOnlyNameProperty(RoundTrip_Tester t) throws Exception {
		var x = new ReadOnlyNamePropertyContainer().init();
		var originalName = x.bean.getName(); // Should be "initialName"
		x = t.roundTrip(x);
		assertBean(x, "bean{f2},m{k1{f2}}", "{2},{{2}}");
		if (t.isValidationOnly())
			return;
		// The name should NOT have changed because the property is read-only
		assertEquals(originalName, x.bean.getName(), "Read-only name property should not be set by parser");
		assertBean(x, "bean{name}", "{initialName}");
		assertBean(x, "m{k1{name}}", "{{initialName}}");
	}

	public static class ReadOnlyNamePropertyContainer {
		public ReadOnlyNamePropertyBean bean;
		public Map<String,ReadOnlyNamePropertyBean> m;

		ReadOnlyNamePropertyContainer init() {
			bean = new ReadOnlyNamePropertyBean().init();
			m = new LinkedHashMap<>();
			m.put("k1", new ReadOnlyNamePropertyBean().init());
			return this;
		}
	}
	public static class ReadOnlyNamePropertyBean {
		private String name = "initialName";
		public int f2;

		@NameProperty
		public String getName() {
			return name;
		}

		ReadOnlyNamePropertyBean init() {
			f2 = 2;
			return this;
		}
	}
}

