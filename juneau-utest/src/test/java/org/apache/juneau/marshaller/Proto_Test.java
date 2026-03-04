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
package org.apache.juneau.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.collections.JsonMap;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Proto} marshaller.
 */
class Proto_Test {

	@Test
	void e01_of() throws Exception {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var proto = Proto.of(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("Alice"));
		assertTrue(proto.contains("age"));
		assertTrue(proto.contains("30"));
	}

	@Test
	void e02_to() throws Exception {
		var input = "name: \"Alice\"\nage: 30";
		var bean = Proto.to(input, JsonMap.class);
		assertNotNull(bean);
		assertEquals("Alice", bean.get("name"));
		assertEquals(30L, bean.get("age"));
	}

	@Test
	void e03_roundTrip() throws Exception {
		var original = JsonMap.of("s", "hello", "n", 42, "b", true);
		var proto = Proto.of(original);
		var roundTrip = Proto.to(proto, JsonMap.class);
		assertEquals("hello", roundTrip.get("s"));
		assertEquals(42L, roundTrip.get("n"));
		assertEquals(true, roundTrip.get("b"));
	}

	@Test
	void e04_defaultInstance() throws Exception {
		var bean = JsonMap.of("x", 1);
		var proto = Proto.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("x"));
		var parsed = Proto.DEFAULT.read(proto, JsonMap.class);
		assertEquals(1L, parsed.get("x"));
	}

	/** Simple POJO for e05 - used to debug bean serialization. */
	public static class SimpleBean {
		public String name;
		public int age;
	}

	@Test
	void e05_plainBeanRoundTrip() throws Exception {
		var bean = new SimpleBean();
		bean.name = "Bob";
		bean.age = 25;
		var proto = Proto.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("Bob"), "Proto must serialize bean fields, got: [" + proto + "]");
		var parsed = Proto.DEFAULT.read(proto, SimpleBean.class);
		assertEquals("Bob", parsed.name);
		assertEquals(25, parsed.age);
	}

	/** Bean with date/time and duration for e06. */
	public static class DateTimeBean {
		public Instant instant;
		public LocalDate localDate;
		public Duration duration;
	}

	@Test
	void e06_dateTimeRoundTrip() throws Exception {
		var bean = new DateTimeBean();
		bean.instant = Instant.parse("2012-12-21T12:34:56Z");
		bean.localDate = LocalDate.parse("2012-12-21");
		bean.duration = Duration.ofHours(2).plusMinutes(30);
		var proto = Proto.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("2012-12-21"), "Proto must serialize date fields");
		var parsed = Proto.DEFAULT.read(proto, DateTimeBean.class);
		assertEquals(Instant.parse("2012-12-21T12:34:56Z"), parsed.instant);
		assertEquals(LocalDate.parse("2012-12-21"), parsed.localDate);
		assertEquals(Duration.ofHours(2).plusMinutes(30), parsed.duration);
	}

	@Test
	void e07_epochMillisToDate() throws Exception {
		var expected = Instant.parse("2012-12-21T12:34:56Z");
		var input = "ts: " + expected.toEpochMilli();
		var bean = Proto.to(input, EpochBean.class);
		assertNotNull(bean);
		assertEquals(expected, bean.ts);
	}

	/** Bean with Instant for epoch millis test. */
	public static class EpochBean {
		public Instant ts;
	}
}
