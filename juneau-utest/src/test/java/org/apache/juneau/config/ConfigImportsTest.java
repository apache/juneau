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
package org.apache.juneau.config;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.junit.*;

/**
 * Validates aspects of config imports.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ConfigImportsTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Value inheritance
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void oneSimpleImport() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A", "", "x=1");
		ms.write("B", "", "<A>");
		Config c = Config
			.create("B")
			.store(ms)
			.build();
		assertEquals("1", c.get("x").get());

		c.set("x", "2");
		c.commit();
		assertEquals("x=1", ms.read("A"));
		assertEquals("x = 2\n", ms.read("B"));
	}

	@Test
	public void twoSimpleImports() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A1", "", "x=1");
		ms.write("A2", "", "y=2");
		ms.write("B", "", "<A1>\n<A2>");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("x").get());
		assertEquals("2", c.get("y").get());

		c.set("x", "3");
		c.set("y", "4");
		c.commit();
		assertEquals("x=1", ms.read("A1"));
		assertEquals("y=2", ms.read("A2"));
		assertEquals("x = 3\ny = 4\n", ms.read("B"));
	}

	@Test
	public void nestedImports() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A1", "", "x=1");
		ms.write("A2", "", "<A1>\ny=2");
		ms.write("B", "", "<A2>");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("x").get());
		assertEquals("2", c.get("y").get());

		c.set("x", "3");
		c.set("y", "4");
		c.commit();
		assertEquals("x=1", ms.read("A1"));
		assertEquals("<A1>\ny=2", ms.read("A2"));
		assertEquals("x = 3\ny = 4\n", ms.read("B"));
	}

	@Test
	public void nestedImportsLoop() throws Exception {
		// This shouldn't blow up.
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A1", "", "<A2>\nx=1");
		ms.write("A2", "", "<A1>\ny=2");
		ms.write("B", "", "<A2>");
		assertThrown(()->Config.create("B").store(ms).build()).isExists();
	}

	@Test
	public void importNotFound() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("B", "", "<A>\nx=1");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("x").get());
	}

	@Test
	public void noOverwriteOnImports() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A", "", "x=1");
		ms.write("B", "", "<A>");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("x").get());
		c.set("x", "2");
		assertEquals("2", c.get("x").get());
		assertEquals("1", Config.create("A").store(ms).build().get("x").get());
	}

	@Test
	public void overlappingSections() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[A]\na2=2");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("A/a1").get());
		assertEquals("2", c.get("A/a2").get());
	}

	@Test
	public void overlappingSectionsImportAtEnd() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "[A]\na2=2\n<A>");
		Config c = Config.create("B").store(ms).build();
		assertEquals("1", c.get("A/a1").get());
		assertEquals("2", c.get("A/a2").get());
	}

	@Test
	public void overlappingSectionsAndValues() throws Exception {
		MemoryStore ms = MemoryStore.create().build();
		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[A]\na1=2");
		Config c = Config.create("B").store(ms).build();
		assertEquals("2", c.get("A/a1").get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Listeners
	//-----------------------------------------------------------------------------------------------------------------

	public static class TestListener implements ConfigEventListener {

		private boolean triggered;
		private ConfigEvents events;

		@Override
		public void onConfigChange(ConfigEvents events) {
			this.triggered = true;
			this.events = events;
		}

		public TestListener reset() {
			triggered = false;
			events = null;
			return this;
		}

		public boolean isTriggered() {
			return triggered;
		}

		public ConfigEvents getEvents() {
			return events;
		}

		public String getNewValue(String section, String key) {
			if (events.size() == 0)
				return null;
			for (ConfigEvent ce : events)
				if (eq(section, ce.getSection()) && eq(key, ce.getKey()))
					return ce.getValue();
			return null;
		}
	}

	@Test
	public void testUpdateOnParent() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[B]\nb1=2");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();

		assertEquals(2, ca.getConfigMap().getListeners().size());
		assertEquals(1, cb.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cb.addListener(l);

		assertEquals(0, ca.getListeners().size());
		assertEquals(1, cb.getListeners().size());

		ms.write("A", "x=1\n[A]\na1=1", "x=1\n[A]\na1=2");

		assertTrue(l.isTriggered());
		assertEquals(1, l.getEvents().size());
		assertEquals("2", l.getNewValue("A", "a1"));

		assertEquals("2", cb.get("A/a1").get());
		assertEquals("2", cb.get("B/b1").get());

		l.reset();
		cb.removeListener(l);
		assertEquals(0, ca.getListeners().size());
		assertEquals(0, cb.getListeners().size());

		ms.write("A", "x=1\n[A]\na1=2", "x=1\n[A]\na1=3");

		assertFalse(l.isTriggered());

		assertEquals("3", cb.get("A/a1").get());
		assertEquals("2", cb.get("B/b1").get());
	}

	@Test
	public void testUpdateOnGrandParent() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[B]\nb1=2");
		ms.write("C", "", "<B>\n[C]\nc1=3");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();
		Config cc = Config.create("C").store(ms).build();

		assertEquals(3, ca.getConfigMap().getListeners().size());
		assertEquals(2, cb.getConfigMap().getListeners().size());
		assertEquals(1, cc.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cc.addListener(l);

		assertEquals(0, ca.getListeners().size());
		assertEquals(0, cb.getListeners().size());
		assertEquals(1, cc.getListeners().size());

		ms.write("A", "x=1\n[A]\na1=1", "x=1\n[A]\na1=2");

		assertTrue(l.isTriggered());
		assertEquals(1, l.getEvents().size());
		assertEquals("2", l.getNewValue("A", "a1"));

		assertEquals("2", cc.get("A/a1").get());
		assertEquals("2", cc.get("B/b1").get());
		assertEquals("3", cc.get("C/c1").get());

		l.reset();
		cc.removeListener(l);
		assertEquals(0, ca.getListeners().size());
		assertEquals(0, cb.getListeners().size());
		assertEquals(0, cc.getListeners().size());

		ms.write("A", "x=1\n[A]\na1=2", "x=1\n[A]\na1=3");

		assertFalse(l.isTriggered());

		assertEquals("3", cc.get("A/a1").get());
		assertEquals("2", cc.get("B/b1").get());
		assertEquals("3", cc.get("C/c1").get());
	}

	@Test
	public void testUpdateOnParentSameSection() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[A]\nb1=2");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();

		assertEquals(2, ca.getConfigMap().getListeners().size());
		assertEquals(1, cb.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cb.addListener(l);

		ms.write("A", "x=1\n[A]\na1=1", "x=1\n[A]\na1=2");

		assertTrue(l.isTriggered());
		assertEquals(1, l.getEvents().size());
		assertEquals("2", l.getNewValue("A", "a1"));

		assertEquals("2", cb.get("A/a1").get());
		assertEquals("2", cb.get("A/b1").get());

		l.reset();
		cb.removeListener(l);
		assertEquals(0, ca.getListeners().size());
		assertEquals(0, cb.getListeners().size());

		ms.write("A", "x=1\n[A]\na1=2", "x=1\n[A]\na1=3");

		assertFalse(l.isTriggered());

		assertEquals("3", cb.get("A/a1").get());
		assertEquals("2", cb.get("A/b1").get());
	}

	@Test
	public void testUpdateOnParentSameSectionSameKey() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[A]\na1=2");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();

		assertEquals(2, ca.getConfigMap().getListeners().size());
		assertEquals(1, cb.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cb.addListener(l);

		ms.write("A", "x=1\n[A]\na1=1", "x=1\n[A]\na1=3");

		assertFalse(l.isTriggered());
		assertEquals("2", cb.get("A/a1").get());
	}

	@Test
	public void testUpdateOnGrandParentSameSection() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\n[A]\na1=1");
		ms.write("B", "", "<A>\n[A]\na1=2");
		ms.write("C", "", "<B>\n[A]\na1=3");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();
		Config cc = Config.create("C").store(ms).build();

		assertEquals(3, ca.getConfigMap().getListeners().size());
		assertEquals(2, cb.getConfigMap().getListeners().size());
		assertEquals(1, cc.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cc.addListener(l);

		ms.write("A", "x=1\n[A]\na1=1", "x=1\n[A]\na1=4");

		assertFalse(l.isTriggered());
		assertEquals("3", cc.get("A/a1").get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Listeners and dynamically modifying imports
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testUpdateOnParentDynamic() throws Exception {
		MemoryStore ms = MemoryStore.create().build();

		ms.write("A", "", "x=1\ny=1\n[A]\na1=1");
		ms.write("B", "", "x=2\n[B]\nb1=2");

		Config ca = Config.create("A").store(ms).build();
		Config cb = Config.create("B").store(ms).build();

		assertEquals(1, ca.getConfigMap().getListeners().size());
		assertEquals(1, cb.getConfigMap().getListeners().size());

		TestListener l = new TestListener();
		cb.addListener(l);

		assertEquals(0, ca.getListeners().size());
		assertEquals(1, cb.getListeners().size());

		ms.write("B", "x=2\n[B]\nb1=2", "x=2\n<A>\n[B]\nb1=2");

		assertTrue(l.isTriggered());
		assertEquals(2, l.getEvents().size());  // Should contain [SET(y = 1), SET(A/a1 = 1)]
		assertEquals("1", l.getNewValue("A", "a1"));

		assertEquals("1", cb.get("A/a1").get());
		assertEquals("2", cb.get("B/b1").get());

		assertEquals(2, ca.getConfigMap().getListeners().size());
		assertEquals(1, cb.getConfigMap().getListeners().size());

		l.reset();

		ms.write("B", "x=2\n<A>\n[B]\nb1=2", "x=2\n[B]\nb1=2");

		assertTrue(l.isTriggered());
		assertEquals(2, l.getEvents().size());  // Should contain [REMOVE_ENTRY(y), REMOVE_ENTRY(A/a1)]
 		assertEquals(null, l.getNewValue("A", "a1"));

		assertNull(cb.get("A/a1").orElse(null));
		assertEquals("2", cb.get("B/b1").get());

		l.reset();
		cb.removeListener(l);
		assertEquals(0, ca.getListeners().size());
		assertEquals(0, cb.getListeners().size());

		ms.write("B", "x=2\n[B]\nb1=2", "x=2\n<A>\n[B]\nb1=2");

		assertFalse(l.isTriggered());

		assertEquals("1", cb.get("A/a1").get());
		assertEquals("2", cb.get("B/b1").get());
	}
}
