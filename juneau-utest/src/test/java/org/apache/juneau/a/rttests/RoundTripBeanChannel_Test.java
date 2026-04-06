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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Round-trip tests for {@link BeanChannel}, {@link ListBeanChannel}, {@link BeanSupplier}, and
 * {@link BeanConsumer} across all standard serialization formats inherited from {@link RoundTripTest_Base}.
 *
 * <p>
 * Test strategy:
 * <ul>
 * 	<li><b>a01-a04</b>: Serialize a plain {@code List} (well-tested), parse back via
 * 	    {@link org.apache.juneau.parser.ParserSession#parseToBeanConsumer} — exercises the consumer
 * 	    lifecycle across every format that has a parser.
 * 	<li><b>b01</b>: Full end-to-end lifecycle test using a custom {@link BeanSupplier} for
 * 	    serialization and a custom {@link BeanConsumer} for parsing. Skips RDF formats (known
 * 	    limitation: RDF parsers do not support collection round-trips without type annotations).
 * 	<li><b>c01-c02</b>: Direction-validation and serializer-acceptance tests run across all formats.
 * </ul>
 */
class RoundTripBeanChannel_Test extends RoundTripTest_Base {

	public static class Item {
		public String name;
		public int value;
		public Item() {}
		public Item(String name, int value) { this.name = name; this.value = value; }
	}

	// ====================================================================================================
	// parseToBeanConsumer tests — serialize plain List, parse into ListBeanChannel
	// Exercises the BeanConsumer lifecycle (begin/acceptThrows/complete) across every format with a parser.
	// ====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_parseToBeanConsumer_emptyList(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		var serialized = t.serialize(list(), t.getSerializer());
		var parsed = new ListBeanChannel<Item>();
		t.getParser().getSession().parseToBeanConsumer(serialized, parsed, Item.class);
		assertEquals(0, parsed.getList().size());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_parseToBeanConsumer_singleItem(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		var serialized = t.serialize(list(new Item("alpha", 1)), t.getSerializer());
		var parsed = new ListBeanChannel<Item>();
		t.getParser().getSession().parseToBeanConsumer(serialized, parsed, Item.class);
		assertEquals(1, parsed.getList().size());
		assertEquals("alpha", parsed.getList().get(0).name);
		assertEquals(1, parsed.getList().get(0).value);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_parseToBeanConsumer_multipleItems(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		var serialized = t.serialize(list(new Item("alpha", 1), new Item("beta", 2), new Item("gamma", 3)), t.getSerializer());
		var parsed = new ListBeanChannel<Item>();
		t.getParser().getSession().parseToBeanConsumer(serialized, parsed, Item.class);
		assertEquals(3, parsed.getList().size());
		assertEquals("alpha", parsed.getList().get(0).name);
		assertEquals("beta",  parsed.getList().get(1).name);
		assertEquals("gamma", parsed.getList().get(2).name);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_parseToBeanConsumer_strings(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		var serialized = t.serialize(list("x", "y", "z"), t.getSerializer());
		var parsed = new ListBeanChannel<String>();
		t.getParser().getSession().parseToBeanConsumer(serialized, parsed, String.class);
		assertEquals(List.of("x", "y", "z"), parsed.getList());
	}

	// ====================================================================================================
	// Lifecycle tests
	// ====================================================================================================

	/**
	 * Tests the BeanConsumer lifecycle (begin / acceptThrows / complete) across every format that
	 * has a parser.  Serialization uses a plain {@code List} so every serializer works uniformly.
	 */
	@ParameterizedTest
	@MethodSource("testers")
	void b01_beanConsumer_lifecycle(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		var log = new ArrayList<String>();

		var serialized = t.serialize(list("a", "b"), t.getSerializer());

		BeanConsumer<String> consumer = new BeanConsumer<>() {
			@Override public void begin() throws Exception { log.add("consumer.begin"); }
			@Override public void acceptThrows(String item) { log.add("accept:" + item); }
			@Override public void complete() throws Exception { log.add("consumer.complete"); }
		};

		t.getParser().getSession().parseToBeanConsumer(serialized, consumer, String.class);

		assertEquals(
			List.of("consumer.begin", "accept:a", "accept:b", "consumer.complete"),
			log
		);
	}

	/**
	 * Tests the BeanSupplier lifecycle (begin / iterator / complete) end-to-end.
	 * Uses an anonymous {@link BeanSupplier} (no getter methods, pure {@link Iterable}) so that
	 * text-based serializers treat it as a sequence.
	 * Skips RDF (requires type annotations for collection round-trips) and formats that apply
	 * additional XML-whitespace validation to the serialized output.
	 */
	@ParameterizedTest
	@MethodSource("testers")
	void b02_beanSupplier_lifecycle(RoundTrip_Tester t) throws Exception {
		assumeTrue(t.getParser() != null, "Skipping serialization-only tester: " + t.label);
		assumeFalse(t.label.contains("Rdf"), "Skipping RDF: collection round-trip requires type annotations");
		// Skip testers that impose format-specific validation on the serialized output (XML whitespace
		// check) or whose serializer does not support anonymous Iterable types (Yaml).
		assumeFalse(t.label.contains("[6]") || t.label.contains("[22]"),
			"Skipping tester with format limitations for anonymous BeanSupplier: " + t.label);
		var log = new ArrayList<String>();

		BeanSupplier<String> supplier = new BeanSupplier<>() {
			@Override public void begin() throws Exception { log.add("supplier.begin"); }
			@Override public Iterator<String> iterator() { return List.of("a", "b").iterator(); }
			@Override public void complete() throws Exception { log.add("supplier.complete"); }
		};

		var serialized = t.getSerializer().serialize(supplier);

		BeanConsumer<String> consumer = new BeanConsumer<>() {
			@Override public void begin() throws Exception { log.add("consumer.begin"); }
			@Override public void acceptThrows(String item) { log.add("accept:" + item); }
			@Override public void complete() throws Exception { log.add("consumer.complete"); }
		};

		t.getParser().getSession().parseToBeanConsumer(serialized, consumer, String.class);

		assertEquals(
			List.of("supplier.begin", "supplier.complete", "consumer.begin", "accept:a", "accept:b", "consumer.complete"),
			log
		);
	}

	// ====================================================================================================
	// Direction validation — these tests work with all serializers (no parsing needed).
	// ====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void c01_beanConsumer_rejected_by_serializer(RoundTrip_Tester t) {
		BeanConsumer<String> a = item -> {};
		assertThrows(SerializeException.class, () -> t.getSerializer().serialize(a));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void c02_beanChannel_accepted_by_serializer(RoundTrip_Tester t) throws Exception {
		// Use an anonymous BeanChannel (pure Iterable interface, no extra bean properties)
		// so that all serializers treat it as a sequence, not a bean.
		BeanChannel<String> a = new BeanChannel<>() {
			@Override public Iterator<String> iterator() { return List.of("hello").iterator(); }
			@Override public void acceptThrows(String item) {}
		};
		assertDoesNotThrow(() -> t.getSerializer().serialize(a));
	}
}
