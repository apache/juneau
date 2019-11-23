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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.transforms.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related bean context properties in REST resources.
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BeanContextPropertiesTest  {

	//=================================================================================================================
	// Validate that transforms defined on class transform to underlying bean context.
	//=================================================================================================================

	@Rest
	@BeanConfig(pojoSwaps=TemporalDateSwap.IsoInstant.class)
	public static class A {
		@RestMethod(name=GET, path="/{d1}")
		public String testClassTransforms(@Path(name="d1") Date d1, @Query(name="d2") Date d2, @Header(name="X-D3") Date d3) throws Exception {
			TemporalDateSwap df = TemporalDateSwap.IsoInstant.class.newInstance();
			BeanSession session = BeanContext.DEFAULT.createSession();
			return "d1="+df.swap(session, d1)+",d2="+df.swap(session, d2)+",d3="+df.swap(session, d3)+"";
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_testClassTransforms() throws Exception {
		a.get("/2001-07-04T15:30:45Z?d2=2001-07-05T15:30:45Z").header("X-D3", "2001-07-06T15:30:45Z").execute()
			.assertBody("d1=2001-07-04T15:30:45Z,d2=2001-07-05T15:30:45Z,d3=2001-07-06T15:30:45Z");
	}
}