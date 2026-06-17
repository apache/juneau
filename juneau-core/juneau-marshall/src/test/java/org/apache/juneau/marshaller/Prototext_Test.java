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

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Prototext} marshaller.
 */
class Prototext_Test {

	@Test
	void e01_of() {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var proto = Prototext.of(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("Alice"));
		assertTrue(proto.contains("age"));
		assertTrue(proto.contains("30"));
	}

	@Test
	void e02_to() {
		var input = "name: \"Alice\"\nage: 30";
		var bean = Prototext.to(input, JsonMap.class);
		assertNotNull(bean);
		assertEquals("Alice", bean.get("name"));
		assertEquals(30L, bean.get("age"));
	}

	@Test
	void e03_roundTrip() {
		var original = JsonMap.of("s", "hello", "n", 42, "b", true);
		var proto = Prototext.of(original);
		var roundTrip = Prototext.to(proto, JsonMap.class);
		assertEquals("hello", roundTrip.get("s"));
		assertEquals(42L, roundTrip.get("n"));
		assertEquals(true, roundTrip.get("b"));
	}

	@Test
	void e04_defaultInstance() {
		var bean = JsonMap.of("x", 1);
		var proto = Prototext.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("x"));
		var parsed = Prototext.DEFAULT.read(proto, JsonMap.class);
		assertEquals(1L, parsed.get("x"));
	}

	/** Simple POJO for e05 - used to debug bean serialization. */
	public static class SimpleBean {
		public String name;
		public int age;
	}

	@Test
	void e05_plainBeanRoundTrip() {
		var bean = new SimpleBean();
		bean.name = "Bob";
		bean.age = 25;
		var proto = Prototext.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("Bob"), "Prototext must serialize bean fields, got: [" + proto + "]");
		var parsed = Prototext.DEFAULT.read(proto, SimpleBean.class);
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
	void e06_dateTimeRoundTrip() {
		var bean = new DateTimeBean();
		bean.instant = Instant.parse("2012-12-21T12:34:56Z");
		bean.localDate = LocalDate.parse("2012-12-21");
		bean.duration = Duration.ofHours(2).plusMinutes(30);
		var proto = Prototext.DEFAULT.write(bean);
		assertNotNull(proto);
		assertTrue(proto.contains("2012-12-21"), "Prototext must serialize date fields");
		var parsed = Prototext.DEFAULT.read(proto, DateTimeBean.class);
		assertEquals(Instant.parse("2012-12-21T12:34:56Z"), parsed.instant);
		assertEquals(LocalDate.parse("2012-12-21"), parsed.localDate);
		assertEquals(Duration.ofHours(2).plusMinutes(30), parsed.duration);
	}

	@Test
	void e07_epochMillisToDate() {
		var expected = Instant.parse("2012-12-21T12:34:56Z");
		var input = "ts: " + expected.toEpochMilli();
		var bean = Prototext.to(input, EpochBean.class);
		assertNotNull(bean);
		assertEquals(expected, bean.ts);
	}

	/** Bean with Instant for epoch millis test. */
	public static class EpochBean {
		public Instant ts;
	}
}
