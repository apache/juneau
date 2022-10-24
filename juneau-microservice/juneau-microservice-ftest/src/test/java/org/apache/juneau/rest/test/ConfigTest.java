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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigTest extends RestTestcase {

	private static String URL = "/testConfig";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		RestClient c = TestMicroservice.client().accept("text/json5").build();

		Map<String,Map<String,Object>> m = c.get(URL).run().getContent().as(Map.class, String.class, JsonMap.class);

		Config cf = Config.create().memStore().build().load(m);

		assertObject(cf.getSection("Test").asMap().get()).asJson().is("{int1:'1',int2:'[1,2,3]',int3:'1',int4:'1',int5:'-1',boolean1:'true',boolean2:'[true,true]',testManifestEntry:'test-value'}");

		assertEquals("'1'", c.get(URL + "/Test%2Fint1/" + getName(String.class)).run().getContent().asString());
		assertEquals("'[1,2,3]'", c.get(URL + "/Test%2Fint2/" + getName(String.class)).run().getContent().asString());
		assertEquals("['1','2','3']", c.get(URL + "/Test%2Fint2/" + getName(String[].class)).run().getContent().asString());
		assertEquals("[1,2,3]", c.get(URL + "/Test%2Fint2/" + getName(int[].class)).run().getContent().asString());
		assertEquals("[1,2,3]", c.get(URL + "/Test%2Fint2/" + getName(Integer[].class)).run().getContent().asString());
		assertEquals("1", c.get(URL + "/Test%2Fint3/" + getName(Integer.class)).run().getContent().asString());
		assertEquals("1", c.get(URL + "/Test%2Fint4/" + getName(Integer.class)).run().getContent().asString());
		assertEquals("-1", c.get(URL + "/Test%2Fint5/" + getName(Integer.class)).run().getContent().asString());
		assertEquals("true", c.get(URL + "/Test%2Fboolean1/" + getName(Boolean.class)).run().getContent().asString());
		assertEquals("[true,true]", c.get(URL + "/Test%2Fboolean2/" + getName(Boolean[].class)).run().getContent().asString());
		assertEquals("'test-value'", c.get(URL + "/Test%2FtestManifestEntry/" + getName(String.class)).run().getContent().asString());

		cf.close();
		c.closeQuietly();
	}

	private String getName(Class<?> c) {
		return urlEncode(c.getName());
	}
}
