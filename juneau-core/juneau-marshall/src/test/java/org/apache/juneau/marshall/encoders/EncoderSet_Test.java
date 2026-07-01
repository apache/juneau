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
package org.apache.juneau.marshall.encoders;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"resource"    // Stream/reader instances are intentional short-lived test fixtures; auto-close not required for these assertions.
})
class EncoderSet_Test extends TestBase {

	//====================================================================================================
	// Test matching
	//====================================================================================================
	@Test void a01_encoderGroupMatching() {

		var s = EncoderSet.create().add(Encoder1.class, Encoder2.class, Encoder3.class).build();
		assertInstanceOf(Encoder1.class, s.getEncoder("gzip1"));
		assertInstanceOf(Encoder2.class, s.getEncoder("gzip2"));
		assertInstanceOf(Encoder2.class, s.getEncoder("gzip2a"));
		assertInstanceOf(Encoder3.class, s.getEncoder("gzip3"));
		assertInstanceOf(Encoder3.class, s.getEncoder("gzip3a"));
		assertInstanceOf(Encoder3.class, s.getEncoder("gzip3,gzip2,gzip1"));
		assertInstanceOf(Encoder1.class, s.getEncoder("gzip3;q=0.9,gzip2;q=0.1,gzip1"));
		assertInstanceOf(Encoder3.class, s.getEncoder("gzip2;q=0.9,gzip1;q=0.1,gzip3"));
		assertInstanceOf(Encoder2.class, s.getEncoder("gzip1;q=0.9,gzip3;q=0.1,gzip2"));
	}

	public static class Encoder1 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("gzip1");
		}
	}

	public static class Encoder2 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("gzip2","gzip2a");
		}
	}

	public static class Encoder3 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("gzip3","gzip3a");
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test void a02_inheritence() {
		var sb = EncoderSet.create().add(E1.class, E2.class);
		var s = sb.build();
		assertList(s.getSupportedEncodings(), "E1", "E2", "E2a");

		sb.add(E3.class, E4.class);
		s = sb.build();
		assertList(s.getSupportedEncodings(), "E3", "E4", "E4a", "E1", "E2", "E2a");

		sb.add(E5.class);
		s = sb.build();
		assertList(s.getSupportedEncodings(), "E5", "E3", "E4", "E4a", "E1", "E2", "E2a");
	}

	public static class E1 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("E1");
		}
	}

	public static class E2 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("E2","E2a");
		}
	}

	public static class E3 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("E3");
		}
	}

	public static class E4 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("E4","E4a");
		}
	}

	public static class E5 extends GzipEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return a("E5");
		}
	}

	//====================================================================================================
	// Builder edge-case coverage
	//====================================================================================================

	@Test void b01_builder_addInstancesDirectly() {
		var instance = new E1();
		var s = EncoderSet.create().add(instance).build();
		assertInstanceOf(E1.class, s.getEncoder("E1"));
	}

	@Test void b02_builder_addNoInheritClearsEntries() {
		var sb = EncoderSet.create().add(E1.class, E2.class);
		assertEquals(2, sb.inner().size());
		// Adding NoInherit triggers clear() on the first pass; E1 and E2 are gone
		sb.add(EncoderSet.NoInherit.class, E3.class);
		assertTrue(sb.inner().stream().noneMatch(x -> x.equals(E1.class)));
		assertTrue(sb.inner().stream().noneMatch(x -> x.equals(E2.class)));
	}

	@Test void b03_builder_addInvalidClassThrows() {
		assertThrows(IllegalArgumentException.class, () ->
			EncoderSet.create().add(String.class));
	}

	@Test void b04_builder_setWithInherit() {
		var sb = EncoderSet.create().add(E1.class, E2.class);
		// Inherit preserves existing entries and inserts them
		sb.set(E3.class, EncoderSet.Inherit.class);
		var s = sb.build();
		var codings = s.getSupportedEncodings();
		assertTrue(codings.contains("E3"));
		assertTrue(codings.contains("E1"));
		assertTrue(codings.contains("E2"));
	}

	@Test void b05_builder_setWithInvalidClassThrows() {
		assertThrows(IllegalArgumentException.class, () ->
			EncoderSet.create().set(String.class));
	}

	@Test void b06_builder_implBypassesBuild() {
		var preset = EncoderSet.create().add(E1.class).build();
		var s = EncoderSet.create().impl(preset).build();
		assertSame(preset, s);
	}

	@Test void b07_builder_inner_returnsEntries() {
		var sb = EncoderSet.create().add(E1.class);
		var inner = sb.inner();
		assertFalse(inner.isEmpty());
	}

	@Test void b08_builder_isEmpty() {
		var sb = EncoderSet.create();
		assertTrue(sb.isEmpty());
		sb.add(E1.class);
		assertFalse(sb.isEmpty());
	}

	@Test void b09_builder_toStringWithClass() {
		var sb = EncoderSet.create().add(E1.class);
		var s = sb.toString();
		assertTrue(s.contains("class:"));
	}

	@Test void b10_builder_toStringWithInstance() {
		var sb = EncoderSet.create().add(new E1());
		var s = sb.toString();
		assertTrue(s.contains("object:"));
	}

	@Test void b11_builder_toStringWithNull() {
		var sb = EncoderSet.create();
		sb.inner().add(null);
		var s = sb.toString();
		assertTrue(s.contains("null"));
	}

	@Test void b12_builder_create_withBeanStore() {
		var bs = new BasicBeanStore(null);
		var sb = EncoderSet.create(bs);
		assertSame(bs, sb.beanStore());
	}

	@Test void b13_builder_copy_isIndependent() {
		var sb1 = EncoderSet.create().add(E1.class);
		var sb2 = sb1.copy();
		sb2.add(E2.class);
		// sb1 should not have E2
		assertFalse(sb1.inner().stream().anyMatch(x -> x.equals(E2.class)));
	}
}