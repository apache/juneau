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
package org.apache.juneau.rest.server.converter;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/**
 * Tests that {@link QueryableSettings#applyTo} configures an {@link InMemoryBeanQueryContext.Builder}, by exercising
 * each setting's documented effect through the public {@code find} API.
 */
class QueryableSettings_Test {

	public static class Person {
		private final String name;
		public Person(String name) { this.name = name; }
		public String getName() { return name; }
	}

	private static List<Person> people(int n) {
		var l = new ArrayList<Person>();
		for (var i = 0; i < n; i++)
			l.add(new Person("p" + i));
		return l;
	}

	private static InMemoryBeanQueryContext<Person> ctx(QueryableSettings settings) {
		return settings.applyTo(InMemoryBeanQueryContext.create(Person.class)).build();
	}

	@Test void a01_defaultLimit_appliedWhenRequestSpecifiesNone() {
		try (var s = ctx(QueryableSettings.create().defaultLimit(5).build()).getSession(people(20))) {
			assertSize(5, s.find(new BeanQuery()).rows());
		}
	}

	@Test void a02_maxLimit_clampsOversizedRequest() {
		try (var s = ctx(QueryableSettings.create().maxLimit(3).build()).getSession(people(20))) {
			assertSize(3, s.find(new BeanQuery().setLimit(1000)).rows());
		}
	}

	@Test void a03_maxLimit_null_isUncapped() {
		try (var s = ctx(QueryableSettings.create().maxLimit(null).build()).getSession(people(1500))) {
			assertSize(1500, s.find(new BeanQuery().setLimit(5000)).rows());
		}
	}

	@Test void a04_countPolicyNone_omitsCountsEvenWhenRequested() {
		try (var s = ctx(QueryableSettings.create().countPolicy(CountPolicy.NONE).build()).getSession(people(20))) {
			var page = s.find(new BeanQuery().setOpts("counts=true"));
			assertTrue(page.total().isEmpty());
			assertTrue(page.matched().isEmpty());
		}
	}

	@Test void a05_countPolicyBoth_alwaysCounts() {
		try (var s = ctx(QueryableSettings.create().countPolicy(CountPolicy.BOTH).build()).getSession(people(20))) {
			var page = s.find(new BeanQuery().setLimit(2));
			assertEquals(20L, page.total().getAsLong());
			assertEquals(20L, page.matched().getAsLong());
		}
	}

	@Test void a06_maxSearchLength_rejectsOverlongSearch() {
		try (var s = ctx(QueryableSettings.create().maxSearchLength(4).build()).getSession(people(1))) {
			var e = assertThrows(BeanQuerySyntaxException.class, () -> s.find(new BeanQuery().setSearch("name=toolong")));
			assertBean(e, "code", "SEARCH_TOO_LONG");
		}
	}

	@Test void a07_allowRegex_falseOptsOut() {
		try (var s = ctx(QueryableSettings.create().allowRegex(false).build()).getSession(people(1))) {
			var e = assertThrows(BeanQuerySyntaxException.class, () -> s.find(new BeanQuery().setSearch("name=$regex(p.*)")));
			assertBean(e, "code", "REGEX_DISABLED");
		}
	}

	@Test void a08_maxSortKeys_rejectsTooManyKeys() {
		try (var s = ctx(QueryableSettings.create().maxSortKeys(1).build()).getSession(people(1))) {
			assertThrows(BeanQuerySyntaxException.class, () -> s.find(new BeanQuery().setSort("name,name")));
		}
	}

	@Test void a09_default_leavesBuilderDefaultsUntouched() {
		// DEFAULT sets nothing: regex allowed, defaultLimit 100, maxLimit 1000 -- all from the context builder.
		try (var s = ctx(QueryableSettings.DEFAULT).getSession(people(1200))) {
			assertSize(100, s.find(new BeanQuery()).rows());
			assertSize(1000, s.find(new BeanQuery().setLimit(5000)).rows());
			assertSize(100, s.find(new BeanQuery().setSearch("name=$regex(p.*)")).rows());
		}
	}

	@Test void a10_maxLimit_belowBuilderDefaultLimit_narrowsUnsetDefaultLimit() {
		// The builder auto-narrows an unset defaultLimit; applyTo must not break that by setting defaultLimit itself.
		try (var s = ctx(QueryableSettings.create().maxLimit(10).build()).getSession(people(50))) {
			assertSize(10, s.find(new BeanQuery()).rows());
		}
	}

	@Test void a11_regexTimeout_applied() {
		var b = QueryableSettings.create().regexTimeout(Duration.ofMillis(10)).build();
		assertDoesNotThrow(() -> ctx(b));
	}

	@Test void a12_copy_roundTripsSetValues() {
		var settings = QueryableSettings.create().defaultLimit(7).maxLimit(null).build().copy().build();
		try (var s = ctx(settings).getSession(people(1500))) {
			assertSize(7, s.find(new BeanQuery()).rows());
			assertSize(1500, s.find(new BeanQuery().setLimit(5000)).rows());
		}
	}

	@Test void a13_applyTo_nullBuilder_throws() {
		assertThrows(IllegalArgumentException.class, () -> QueryableSettings.DEFAULT.applyTo((InMemoryBeanQueryContext.Builder<Person>)null));
	}

	@Test void a14_invalidValue_rejectedByContextBuilder() {
		var settings = QueryableSettings.create().defaultLimit(0).build();
		assertThrows(IllegalArgumentException.class, () -> ctx(settings));
	}
}
