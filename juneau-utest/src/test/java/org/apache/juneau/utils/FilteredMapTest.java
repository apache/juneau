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
package org.apache.juneau.utils;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.junit.jupiter.api.*;

class FilteredMapTest extends TestBase {

	Map<?,?> m3;

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var m = JsonMap.ofJson("{a:'1',b:'2'}");

		ClassMeta<Map<String,Object>> cm = BeanContext.DEFAULT.getClassMeta(Map.class, String.class, Object.class);
		ClassMeta<Map<String,String>> cm2 = BeanContext.DEFAULT.getClassMeta(Map.class, String.class, String.class);

		var m2 = new FilteredMap<>(cm, m, a("a"));

		assertBean(m2, "a", "1");

		m2.entrySet().iterator().next().setValue("3");
		assertBean(m2, "a", "3");

		assertThrows(IllegalArgumentException.class, ()->new FilteredMap<>(cm2, null, new String[0]));
		assertThrows(IllegalArgumentException.class, ()->new FilteredMap<>(cm, m, null));
	}
}