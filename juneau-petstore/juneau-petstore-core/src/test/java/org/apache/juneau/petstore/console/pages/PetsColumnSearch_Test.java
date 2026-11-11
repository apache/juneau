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
package org.apache.juneau.petstore.console.pages;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class PetsColumnSearch_Test extends TestBase {

	private static JsonMap column(String name) {
		var cols = PetsPageBuilders.petsTable().toCardBody().getList("columns");
		for (var c : cols.elements(JsonMap.class))
			if (name.equals(c.getString("key")) || name.equals(c.getString("data")))
				return c;
		return fail("No column " + name);
	}

	@Test void a01_nameColumnIsSearchable() {
		var c = column("name");
		assertEquals("text", c.getString("searchType"));
		assertFalse(c.getList("searchOperators").isEmpty(), "operators are inherited from the table");
	}

	@Test void a02_idStatusSpeciesAreSearchableTagsIsNot() {
		for (var n : new String[]{"id", "species", "status"})
			assertNotNull(column(n).getString("searchType"), n);
		assertNull(column("tags").getString("searchType"));
	}
}
