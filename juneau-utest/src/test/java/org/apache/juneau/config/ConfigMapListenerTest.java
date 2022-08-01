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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.config.event.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.config.store.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigMapListenerTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Sanity tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testBasicDefaultSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"foo=bar"
		);

		final CountDownLatch latch = new CountDownLatch(1);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(foo = baz)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("", "foo", "baz", null, null, null);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("foo = baz|");
	}

	@Test
	public void testBasicNormalSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"foo=bar"
		);

		final CountDownLatch latch = new CountDownLatch(1);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(S1/foo = baz)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("S1", "foo", "baz", null, null, null);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|foo = baz|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add new entries.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testAddNewEntries() throws Exception {
		ConfigStore s = initStore("A.cfg"
		);

		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(k = vb), SET(S1/k1 = v1b)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("", "k", "vb", null, null, null);
		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = vb|[S1]|k1 = v1b|");
	}

	@Test
	public void testAddNewEntriesWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg"
		);

		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(k^* = kb # C), SET(S1/k1^* = k1b # C1)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("", "k", "kb", "^*", "C", Arrays.asList("#k"));
		cm.setEntry("S1", "k1", "k1b", "^*", "C1", Arrays.asList("#k1"));
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#k|k<^*> = kb # C|[S1]|#k1|k1<^*> = k1b # C1|");
	}

	@Test
	public void testAddExistingEntriesWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#ka",
			"k=va # Ca",
			"#S1",
			"[S1]",
			"#k1a",
			"k1=v1a # Cb"
		);

		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(k^* = kb # Cb), SET(S1/k1^* = k1b # Cb1)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("", "k", "kb", "^*", "Cb", Arrays.asList("#kb"));
		cm.setEntry("S1", "k1", "k1b", "^*", "Cb1", Arrays.asList("#k1b"));
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#kb|k<^*> = kb # Cb|#S1|[S1]|#k1b|k1<^*> = k1b # Cb1|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Remove existing entries.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testRemoveExistingEntries() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"k=v",
			"[S1]",
			"k1=v1"
		);

		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[REMOVE_ENTRY(k), REMOVE_ENTRY(S1/k1)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.removeEntry("", "k");
		cm.removeEntry("S1", "k1");
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|");
	}

	@Test
	public void testRemoveExistingEntriesWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#ka",
			"k=va # Ca",
			"#S1",
			"[S1]",
			"#k1a",
			"k1=v1a # Cb"
		);

		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[REMOVE_ENTRY(k), REMOVE_ENTRY(S1/k1)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.removeEntry("", "k");
		cm.removeEntry("S1", "k1");
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#S1|[S1]|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add new sections.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testAddNewSections() throws Exception {
		ConfigStore s = initStore("A.cfg"
		);

		final CountDownLatch latch = new CountDownLatch(1);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(S3/k3 = v3)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setSection("", Arrays.asList("#D1"));
		cm.setSection("S1", Arrays.asList("#S1"));
		cm.setSection("S2", null);
		cm.setSection("S3", Collections.<String>emptyList());
		cm.setEntry("S3", "k3", "v3", null, null, null);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#D1||#S1|[S1]|[S2]|[S3]|k3 = v3|");
	}

	@Test
	public void testModifyExistingSections() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#Da",
			"",
			"#S1a",
			"[S1]",
			"[S2]",
			"[S3]"
		);

		final CountDownLatch latch = new CountDownLatch(1);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(S3/k3 = v3)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setSection("", Arrays.asList("#Db"));
		cm.setSection("S1", Arrays.asList("#S1b"));
		cm.setSection("S2", null);
		cm.setSection("S3", Collections.<String>emptyList());
		cm.setEntry("S3", "k3", "v3", null, null, null);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#Db||#S1b|[S1]|[S2]|[S3]|k3 = v3|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Remove sections.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testRemoveSections() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#Da",
			"",
			"k = v",
			"",
			"#S1",
			"[S1]",
			"#k1",
			"k1 = v1",
			"[S2]",
			"#k2",
			"k2 = v2",
			"[S3]"
		);

		final CountDownLatch latch = new CountDownLatch(3);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[REMOVE_ENTRY(k), REMOVE_ENTRY(S1/k1), REMOVE_ENTRY(S2/k2)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.removeSection("");
		cm.removeSection("S1");
		cm.removeSection("S2");
		cm.removeSection("S3");
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Update from store.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testUpdateFromStore() throws Exception {
		ConfigStore s = initStore("A.cfg");

		final CountDownLatch latch = new CountDownLatch(3);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is("[SET(k = v # cv), SET(S1/k1 = v1 # cv1), SET(S2/k2 = v2 # cv2)]");
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		s.update("A.cfg",
			"#Da",
			"",
			"k = v # cv",
			"",
			"#S1",
			"[S1]",
			"#k1",
			"k1 = v1 # cv1",
			"[S2]",
			"#k2",
			"k2 = v2 # cv2",
			"[S3]"
		);
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#Da||k = v # cv||#S1|[S1]|#k1|k1 = v1 # cv1|[S2]|#k2|k2 = v2 # cv2|[S3]|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Merges.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeNoOverwrite() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a"
		);

		final CountDownLatch latch = new CountDownLatch(2);
		final Queue<String> eventList = new ConcurrentLinkedQueue<>();
		eventList.add("[SET(S1/k1 = v1b)]");
		eventList.add("[SET(S2/k2 = v2b)]");

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is(eventList.poll());
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("S2", "k2", "v2b", null, null, null);
		s.update("A.cfg",
			"[S1]",
			"k1 = v1b"
		);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1b|[S2]|k2 = v2b|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// If we're modifying an entry and it changes on the file system, we should overwrite the change on save().
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeWithOverwrite() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a"
		);

		final CountDownLatch latch = new CountDownLatch(2);
		final Queue<String> eventList = new ConcurrentLinkedQueue<>();
		eventList.add("[SET(S1/k1 = v1b)]");
		eventList.add("[SET(S1/k1 = v1c)]");

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(ConfigEvents events) throws Exception {
				assertObject(events).asString().is(eventList.poll());
			}
		};

		ConfigMap cm = s.getMap("A.cfg");
		cm.register(l);
		cm.setEntry("S1", "k1", "v1c", null, null, null);
		s.update("A.cfg",
			"[S1]",
			"k1 = v1b"
		);
		cm.commit();
		wait(latch);
		assertNull(l.error);
		cm.unregister(l);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1c|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// If the contents of a file have been modified on the file system before a signal has been received.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeWithOverwriteNoSignal() throws Exception {

		final Queue<String> contents = new ConcurrentLinkedQueue<>();
		contents.add("[S1]\nk1 = v1a");
		contents.add("[S1]\nk1 = v1b");
		contents.add("[S1]\nk1 = v1c");
		contents.add("[S1]\nk1 = v1c");

		MemoryStore s = new MemoryStore(MemoryStore.create()) {
			@Override
			public synchronized String read(String name) {
				return contents.poll();
			}
		};
		try {
			final CountDownLatch latch = new CountDownLatch(2);
			final Queue<String> eventList = new ConcurrentLinkedQueue<>();
			eventList.add("[SET(S1/k1 = v1b)]");
			eventList.add("[SET(S1/k1 = v1c)]");

			LatchedListener l = new LatchedListener(latch) {
				@Override
				public void check(ConfigEvents events) throws Exception {
					assertObject(events).asString().is(eventList.poll());
				}
			};

			ConfigMap cm = s.getMap("A.cfg");
			cm.register(l);
			cm.setEntry("S1", "k1", "v1c", null, null, null);
			cm.commit();
			wait(latch);
			assertNull(l.error);
			cm.unregister(l);

			assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1c|");

		} finally {
			s.close();
		}
	}

	@Test
	public void testMergeWithConstantlyUpdatingFile() throws Exception {

		MemoryStore s = new MemoryStore(MemoryStore.create()) {
			char c = 'a';
			@Override
			public synchronized String read(String name) {
				return "[S1]\nk1 = v1" + (c++);
			}
		};
		try {
			final CountDownLatch latch = new CountDownLatch(10);
			final Queue<String> eventList = new ConcurrentLinkedQueue<>();
			eventList.add("[SET(S1/k1 = v1b)]");
			eventList.add("[SET(S1/k1 = v1c)]");
			eventList.add("[SET(S1/k1 = v1d)]");
			eventList.add("[SET(S1/k1 = v1e)]");
			eventList.add("[SET(S1/k1 = v1f)]");
			eventList.add("[SET(S1/k1 = v1g)]");
			eventList.add("[SET(S1/k1 = v1h)]");
			eventList.add("[SET(S1/k1 = v1i)]");
			eventList.add("[SET(S1/k1 = v1j)]");
			eventList.add("[SET(S1/k1 = v1k)]");

			LatchedListener l = new LatchedListener(latch) {
				@Override
				public void check(ConfigEvents events) throws Exception {
					assertObject(events).asString().is(eventList.poll());
				}
			};

			ConfigMap cm = s.getMap("A.cfg");
			cm.register(l);
			cm.setEntry("S1", "k1", "v1c", null, null, null);
			assertThrown(()->cm.commit()).asMessage().is("Unable to store contents of config to store.");
			wait(latch);
			assertNull(l.error);
			cm.unregister(l);

			assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1c|");

		} finally {
			s.close();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utilities.
	//-----------------------------------------------------------------------------------------------------------------

	private static ConfigStore initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}

	public static class LatchedListener implements ConfigEventListener {
		private final CountDownLatch latch;
		private volatile String error = null;
		public LatchedListener(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onConfigChange(ConfigEvents events) {
			try {
				check(events);
			} catch (Exception e) {
				error = e.getLocalizedMessage();
			}
			for (int i = 0; i < events.size(); i++)
				latch.countDown();
		}

		public void check(ConfigEvents events) throws Exception {
		}
	}

	private static void wait(CountDownLatch latch) throws InterruptedException {
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new RuntimeException("Latch failed.");
	}
}
