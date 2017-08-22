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
package org.apache.juneau.transforms;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;

@SuppressWarnings({"javadoc"})
public class DateSwapTest {

	@BeforeClass
	public static void beforeClass() {
		TestUtils.setTimeZone("GMT-5");
	}

	@AfterClass
	public static void afterClass() {
		TestUtils.unsetTimeZone();
	}

	//====================================================================================================
	//====================================================================================================
	@Test
	public void testBeanWithDate() throws Exception {
		A testBeanA = new A().init();

		final String jsonData = new JsonSerializerBuilder()
			.pojoSwaps(DateSwap.ISO8601DT.class)
			.build()
			.serialize(testBeanA);
		final ObjectMap data = new JsonParserBuilder()
			.pojoSwaps(DateSwap.ISO8601DT.class)
			.build()
			.parse(jsonData, ObjectMap.class);

		final DateSwap.ISO8601DT dateSwap = new DateSwap.ISO8601DT();
		// this works
		final String sValue = data.getString("birthday");
		dateSwap.unswap(BeanContext.DEFAULT.createSession(), sValue, data.getBeanSession().getClassMeta(Date.class));
		// this does not work
		data.getSwapped("birthday", dateSwap);
	}

	public static class A {
		public Date birthday;

		public A init() {
			birthday = new Date();
			return this;
		}
	}
}