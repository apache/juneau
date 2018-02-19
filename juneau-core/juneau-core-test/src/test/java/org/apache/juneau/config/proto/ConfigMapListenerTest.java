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
package org.apache.juneau.config.proto;

import static org.junit.Assert.*;
import static org.apache.juneau.TestUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.junit.*;

public class ConfigMapListenerTest {
	
	//-----------------------------------------------------------------------------------------------------------------
	// Sanity tests.
	//-----------------------------------------------------------------------------------------------------------------
	
	@Test
	public void testBasicDefaultSection() throws Exception {
		Store s = initStore("Foo", 
			"foo=bar"
		);		
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(foo = baz)']", events);
			}
		};
		
		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("default", "foo", "baz");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("foo = baz|", cm.toString());
	}
	
	@Test
	public void testBasicNormalSection() throws Exception {
		Store s = initStore("Foo", 
			"[S1]",
			"foo=bar"
		);		
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(foo = baz)']", events);
			}
		};
		
		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("S1", "foo", "baz");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("[S1]|foo = baz|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Add new entries.
	//-----------------------------------------------------------------------------------------------------------------
	
	@Test
	public void testAddNewEntries() throws Exception {
		Store s = initStore("Foo"
		);		
		
		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k = vb)','SET(k1 = v1b)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("default", "k", "vb");
		cm.setValue("S1", "k1", "v1b");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("k = vb|[S1]|k1 = v1b|", cm.toString());
	}
	
	@Test
	public void testAddNewEntriesWithAttributes() throws Exception {
		Store s = initStore("Foo"
		);		
		
		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k^* = kb # C)','SET(k1^* = k1b # C1)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setEntry("default", "k", "kb", "^*", "C", Arrays.asList("#k"));
		cm.setEntry("S1", "k1", "k1b", "^*", "C1", Arrays.asList("#k1"));
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("#k|k^* = kb # C|[S1]|#k1|k1^* = k1b # C1|", cm.toString());
	}

	@Test
	public void testAddExistingEntriesWithAttributes() throws Exception {
		Store s = initStore("Foo",
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
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k^* = kb # Cb)','SET(k1^* = k1b # Cb1)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setEntry("default", "k", "kb", "^*", "Cb", Arrays.asList("#kb"));
		cm.setEntry("S1", "k1", "k1b", "^*", "Cb1", Arrays.asList("#k1b"));
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("#kb|k^* = kb # Cb|#S1|[S1]|#k1b|k1^* = k1b # Cb1|", cm.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Remove existing entries.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testRemoveExistingEntries() throws Exception {
		Store s = initStore("Foo",
			"k=v",
			"[S1]",
			"k1=v1"
		);		
		
		final CountDownLatch latch = new CountDownLatch(2);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k = null)','SET(k1 = null)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("default", "k", null);
		cm.setValue("S1", "k1", null);
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("[S1]|", cm.toString());
	}
	
	@Test
	public void testRemoveExistingEntriesWithAttributes() throws Exception {
		Store s = initStore("Foo",
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
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k = null)','SET(k1 = null)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("default", "k", null);
		cm.setValue("S1", "k1", null);
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("#S1|[S1]|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Add new sections.
	//-----------------------------------------------------------------------------------------------------------------
	
	@Test
	public void testAddNewSections() throws Exception {
		Store s = initStore("Foo"
		);		
		
		final CountDownLatch latch = new CountDownLatch(1);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k3 = v3)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setSection("default", Arrays.asList("#D1"));
		cm.setSection("S1", Arrays.asList("#S1"));
		cm.setSection("S2", null);
		cm.setSection("S3", Collections.<String>emptyList());
		cm.setValue("S3", "k3", "v3");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("#D1||#S1|[S1]|[S2]|[S3]|k3 = v3|", cm.toString());
	}

	@Test
	public void testModifyExistingSections() throws Exception {
		Store s = initStore("Foo",
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
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k3 = v3)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setSection("default", Arrays.asList("#Db"));
		cm.setSection("S1", Arrays.asList("#S1b"));
		cm.setSection("S2", null);
		cm.setSection("S3", Collections.<String>emptyList());
		cm.setValue("S3", "k3", "v3");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("#Db||#S1b|[S1]|[S2]|[S3]|k3 = v3|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Remove sections.
	//-----------------------------------------------------------------------------------------------------------------
	
	@Test
	public void testRemoveSections() throws Exception {
		Store s = initStore("Foo",
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
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k = null)','SET(k1 = null)','SET(k2 = null)']", events);
			}
		};

		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.removeSection("default");
		cm.removeSection("S1");
		cm.removeSection("S2");
		cm.removeSection("S3");
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Update from store.
	//-----------------------------------------------------------------------------------------------------------------
	
	@Test
	public void testUpdateFromStore() throws Exception {
		Store s = initStore("Foo");

		final CountDownLatch latch = new CountDownLatch(3);

		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals("['SET(k = v # cv)','SET(k1 = v1 # cv1)','SET(k2 = v2 # cv2)']", events);
			}
		};
		
		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		s.update("Foo",
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
		cm.unregisterListener(l);
		
		assertTextEquals("#Da||k = v # cv||#S1|[S1]|#k1|k1 = v1 # cv1|[S2]|#k2|k2 = v2 # cv2|[S3]|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Merges.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeNoOverwrite() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1a"
		);

		final CountDownLatch latch = new CountDownLatch(2);
		final Queue<String> eventList = new ConcurrentLinkedQueue<String>();
		eventList.add("['SET(k1 = v1b)']");
		eventList.add("['SET(k2 = v2b)']");
		
		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals(eventList.poll(), events);
			}
		};
		
		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("S2", "k2", "v2b");
		s.update("Foo",
			"[S1]",
			"k1 = v1b"
		);
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("[S1]|k1 = v1b|[S2]|k2 = v2b|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// If we're modifying an entry and it changes on the file system, we should overwrite the change on save().
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeWithOverwrite() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1a"
		);

		final CountDownLatch latch = new CountDownLatch(2);
		final Queue<String> eventList = new ConcurrentLinkedQueue<String>();
		eventList.add("['SET(k1 = v1b)']");
		eventList.add("['SET(k1 = v1c)']");
		
		LatchedListener l = new LatchedListener(latch) {
			@Override
			public void check(List<ChangeEvent> events) throws Exception {
				assertObjectEquals(eventList.poll(), events);
			}
		};
		
		ConfigMap cm = s.getMap("Foo");
		cm.registerListener(l);
		cm.setValue("S1", "k1", "v1c");
		s.update("Foo",
			"[S1]",
			"k1 = v1b"
		);
		cm.save();
		wait(latch);
		assertNull(l.error);
		cm.unregisterListener(l);
		
		assertTextEquals("[S1]|k1 = v1c|", cm.toString());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// If the contents of a file have been modified on the file system before a signal has been received.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void testMergeWithOverwriteNoSignal() throws Exception {
		
		final Queue<String> contents = new ConcurrentLinkedQueue<String>();
		contents.add("[S1]\nk1 = v1a");
		contents.add("[S1]\nk1 = v1b");
		contents.add("[S1]\nk1 = v1c");
		contents.add("[S1]\nk1 = v1c");
		
		MemoryStore s = new MemoryStore(null) {
			public synchronized String read(String name) {
				return contents.poll();
			}
		};
		try {
			final CountDownLatch latch = new CountDownLatch(2);
			final Queue<String> eventList = new ConcurrentLinkedQueue<String>();
			eventList.add("['SET(k1 = v1b)']");
			eventList.add("['SET(k1 = v1c)']");
			
			LatchedListener l = new LatchedListener(latch) {
				@Override
				public void check(List<ChangeEvent> events) throws Exception {
					assertObjectEquals(eventList.poll(), events);
				}
			};
			
			ConfigMap cm = s.getMap("Foo");
			cm.registerListener(l);
			cm.setValue("S1", "k1", "v1c");
			cm.save();
			wait(latch);
			assertNull(l.error);
			cm.unregisterListener(l);
			
			assertTextEquals("[S1]|k1 = v1c|", cm.toString());
			
		} finally {
			s.close();
		}
	}
	
	@Test
	public void testMergeWithConstantlyUpdatingFile() throws Exception {
		
		MemoryStore s = new MemoryStore(null) {
			char c = 'a';
			public synchronized String read(String name) {
				return "[S1]\nk1 = v1" + (c++);
			}
		};
		try {
			final CountDownLatch latch = new CountDownLatch(10);
			final Queue<String> eventList = new ConcurrentLinkedQueue<String>();
			eventList.add("['SET(k1 = v1b)']");
			eventList.add("['SET(k1 = v1c)']");
			eventList.add("['SET(k1 = v1d)']");
			eventList.add("['SET(k1 = v1e)']");
			eventList.add("['SET(k1 = v1f)']");
			eventList.add("['SET(k1 = v1g)']");
			eventList.add("['SET(k1 = v1h)']");
			eventList.add("['SET(k1 = v1i)']");
			eventList.add("['SET(k1 = v1j)']");
			eventList.add("['SET(k1 = v1k)']");
			
			LatchedListener l = new LatchedListener(latch) {
				@Override
				public void check(List<ChangeEvent> events) throws Exception {
					assertObjectEquals(eventList.poll(), events);
				}
			};
			
			ConfigMap cm = s.getMap("Foo");
			cm.registerListener(l);
			cm.setValue("S1", "k1", "v1c");
			try {
				cm.save();
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertEquals("Unable to store contents of config to store.", e.getMessage());
			}
			wait(latch);
			assertNull(l.error);
			cm.unregisterListener(l);
			
			assertTextEquals("[S1]|k1 = v1c|", cm.toString());
			
		} finally {
			s.close();
		}
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Utilities.
	//-----------------------------------------------------------------------------------------------------------------

	private static Store initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}
	
	public static class LatchedListener implements ChangeEventListener {
		private final CountDownLatch latch;
		private volatile String error = null;
		public LatchedListener(CountDownLatch latch) {
			this.latch = latch;
		}
		
		@Override
		public void onEvents(List<ChangeEvent> events) {
			try {
				check(events);
			} catch (Exception e) {
				error = e.getLocalizedMessage();
			}
			for (int i = 0; i < events.size(); i++)
				latch.countDown();
		}
		
		public void check(List<ChangeEvent> events) throws Exception {
		}
	}
	
	private static void wait(CountDownLatch latch) throws InterruptedException {
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new RuntimeException("Latch failed.");
	}
}
