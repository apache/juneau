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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

class DurationFormatPlacement_Test {

	@Test void a01_marshallingContextBuilder_controlsDurationAndPeriod() throws Exception {
		var mc = MarshallingContext.create().durationFormat(DurationFormat.MILLIS).periodFormat(PeriodFormat.DAYS).build();
		var s = Json5Serializer.create().marshallingContext(mc).build();
		var p = Json5Parser.create().marshallingContext(mc).build();
		assertEquals("5000", s.serialize(Duration.ofSeconds(5)));
		assertEquals("'3'", s.serialize(Period.ofDays(3)));
		assertEquals(Duration.ofSeconds(5), p.parse("5000", Duration.class));
	}

	@Marshalled(durationFormat=DurationFormat.SECONDS, periodFormat=PeriodFormat.DAYS)
	public static class A02 {
		public Duration d = Duration.ofSeconds(5);
		public Period p = Period.ofDays(2);
	}

	@Test void a02_marshalled_appliesClassLevelDefault() throws Exception {
		var s = Json5Serializer.create().durationFormat(DurationFormat.MILLIS).periodFormat(PeriodFormat.ISO_8601).build();
		assertEquals("{d:5.0,p:2}", s.serialize(new A02()));
	}

	public static class A03 {
		@MarshalledProp(durationFormat=DurationFormat.HOCON)
		public Duration d = Duration.ofHours(2);
		@MarshalledProp(periodFormat=PeriodFormat.DAYS)
		public Period p = Period.ofDays(7);
	}

	@Test void a03_marshalledProp_overridesClassAndContext() throws Exception {
		var s = Json5Serializer.create().durationFormat(DurationFormat.MILLIS).periodFormat(PeriodFormat.ISO_8601).build();
		assertEquals("{d:'2h',p:7}", s.serialize(new A03()));
	}

	@MarshalledConfig(durationFormat=DurationFormat.SECONDS, periodFormat=PeriodFormat.DAYS)
	static class A04Config {}

	@Test void a04_marshalledConfig_appliesToBuilderApply() throws Exception {
		var s = Json5Serializer.create().applyAnnotations(A04Config.class).build();
		assertEquals("5.000000000", s.serialize(Duration.ofSeconds(5)));
		assertEquals("'4'", s.serialize(Period.ofDays(4)));
	}

	@Marshalled(durationFormat=DurationFormat.SECONDS)
	public static class A05 {
		@MarshalledProp(durationFormat=DurationFormat.HOCON)
		public Duration d = Duration.ofMinutes(90);
	}

	@Test void a05_precedence_property_over_class_over_context() throws Exception {
		var s = Json5Serializer.create().durationFormat(DurationFormat.MILLIS).build();
		assertEquals("{d:'90m'}", s.serialize(new A05()));
	}
}
