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
package org.apache.juneau.examples.mcp;

import java.util.*;

/**
 * The example's entire "domain": a tiny in-memory map of note title &rarr; note body.
 *
 * <p>
 * Every MCP surface the {@link ExampleMcpServer} exposes (tools, prompt, resources, resource
 * template, completions) is just a thin wrapper over this one object, so a reader can see the
 * whole scenario at a glance without wading through business logic. It is deliberately trivial;
 * the interesting part is the MCP wiring, not the storage.
 *
 * <p>
 * Access is synchronized because subscription/tool handlers can run on different threads
 * (the held-open SSE pump, concurrent HTTP requests). A {@link LinkedHashMap} keeps listing
 * order stable so the demo output is deterministic.
 */
public final class NoteStore {

	/** URI scheme used to address a single note, e.g. {@code note:///groceries}. */
	public static final String SCHEME = "note:///";

	private final Map<String,String> notes = new LinkedHashMap<>();

	/**
	 * Stores (or replaces) a note.
	 *
	 * @param title The note title (its key). Must not be <jk>null</jk>.
	 * @param body The note body. Must not be <jk>null</jk>.
	 */
	public synchronized void put(String title, String body) {
		notes.put(title, body);
	}

	/**
	 * Returns the body of a note.
	 *
	 * @param title The note title. Can be <jk>null</jk>.
	 * @return The body, or <jk>null</jk> if no such note exists.
	 */
	public synchronized String get(String title) {
		return notes.get(title);
	}

	/**
	 * Removes a note.
	 *
	 * @param title The note title. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if a note was actually removed.
	 */
	public synchronized boolean remove(String title) {
		return notes.remove(title) != null;
	}

	/**
	 * Returns a snapshot of all note titles, in insertion order.
	 *
	 * @return An immutable, point-in-time copy of the titles. Never <jk>null</jk>.
	 */
	public synchronized List<String> titles() {
		return List.copyOf(notes.keySet());
	}

	/**
	 * Builds the canonical resource URI for a note title (e.g. {@code note:///groceries}).
	 *
	 * @param title The note title.
	 * @return The resource URI.
	 */
	public static String uriFor(String title) {
		return SCHEME + title;
	}
}
