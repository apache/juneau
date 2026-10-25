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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import javax.xml.stream.*;

import org.junit.jupiter.api.*;

/**
 * Tests for the per-parser memoized {@link XMLInputFactory}.
 */
class XmlParserInputFactory_Test {

	/** A StAX factory that is not on the known thread-safe list. */
	private static class UnknownFactory extends XMLInputFactory {
		@Override public XMLStreamReader createXMLStreamReader(java.io.Reader r) { return null; }
		@Override public XMLStreamReader createXMLStreamReader(javax.xml.transform.Source s) { return null; }
		@Override public XMLStreamReader createXMLStreamReader(java.io.InputStream s) { return null; }
		@Override public XMLStreamReader createXMLStreamReader(java.io.InputStream s, String e) { return null; }
		@Override public XMLStreamReader createXMLStreamReader(String id, java.io.InputStream s) { return null; }
		@Override public XMLStreamReader createXMLStreamReader(String id, java.io.Reader r) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(java.io.Reader r) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(String id, java.io.Reader r) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(XMLStreamReader r) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(javax.xml.transform.Source s) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(java.io.InputStream s) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(java.io.InputStream s, String e) { return null; }
		@Override public javax.xml.stream.XMLEventReader createXMLEventReader(String id, java.io.InputStream s) { return null; }
		@Override public javax.xml.stream.XMLStreamReader createFilteredReader(XMLStreamReader r, StreamFilter f) { return null; }
		@Override public javax.xml.stream.XMLEventReader createFilteredReader(javax.xml.stream.XMLEventReader r, javax.xml.stream.EventFilter f) { return null; }
		@Override public XMLResolver getXMLResolver() { return null; }
		@Override public void setXMLResolver(XMLResolver r) { /* no-op */ }
		@Override public XMLReporter getXMLReporter() { return null; }
		@Override public void setXMLReporter(XMLReporter r) { /* no-op */ }
		@Override public void setProperty(String n, Object v) { /* no-op */ }
		@Override public Object getProperty(String n) { return null; }
		@Override public boolean isPropertySupported(String n) { return false; }
		@Override public void setEventAllocator(javax.xml.stream.util.XMLEventAllocator a) { /* no-op */ }
		@Override public javax.xml.stream.util.XMLEventAllocator getEventAllocator() { return null; }
	}

	@Test void a01_isShareable_unknownImplementation() {
		assertFalse(XmlReader.isShareable(new UnknownFactory()));
	}

	@Test void a02_isShareable_defaultImplementation() {
		assertTrue(XmlReader.isShareable(XMLInputFactory.newInstance()));
		assertTrue(XmlReader.isShareable());
	}

	@Test void b01_factoryMemoized() {
		var p = XmlParser.create().build();
		assertSame(p.getInputFactory(false), p.getInputFactory(false));
	}

	@Test void b02_validatingUnsupportedByFactoryStillThrowsEachTime() {
		// The JDK factory rejects IS_VALIDATING=true; the failure must not be cached away.
		var p = XmlParser.create().build();
		assertThrows(IllegalArgumentException.class, () -> p.getInputFactory(true));
		assertThrows(IllegalArgumentException.class, () -> p.getInputFactory(true));
	}

	@Test void b03_factoryKeepsHardening() throws Exception {
		var f = XmlParser.create().build().getInputFactory(false);
		assertEquals(Boolean.TRUE, f.getProperty(XMLInputFactory.IS_COALESCING));
		assertEquals(Boolean.FALSE, f.getProperty(XMLInputFactory.IS_VALIDATING));
	}

	@Test void c01_repeatedParsesShareFactory() {
		var p = XmlParser.create().build();
		for (var i = 0; i < 3; i++)
			assertEquals(Map.of("a", "b"), new HashMap<>((Map<?,?>)p.read("<A><a>b</a></A>", Object.class)));
	}

	@Test void c02_concurrentParses() throws Exception {
		var p = XmlParser.create().build();
		var pool = Executors.newFixedThreadPool(8);
		try {
			var futures = new ArrayList<Future<String>>();
			for (var i = 0; i < 200; i++) {
				var n = i;
				futures.add(pool.submit(() -> (String)((Map<?,?>)p.read("<A><a>v" + n + "</a></A>", Object.class)).get("a")));
			}
			for (var i = 0; i < futures.size(); i++)
				assertEquals("v" + i, futures.get(i).get());
		} finally {
			pool.shutdownNow();
		}
	}
}
