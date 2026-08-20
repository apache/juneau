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
package org.apache.juneau.rest.server.widgets;

/**
 * A client-only {@link ActionBar} item with no endpoint.
 *
 * <p>
 * This slice ships {@link #COLLAPSE} (collapse the child row).  Unlike an {@link ActionRef}, a safe action is
 * enabled immediately &mdash; including during an in-flight expand GET and after a failed GET &mdash; so the
 * user can dismiss a loading or error panel.
 *
 * @since 10.0.0
 */
public enum SafeAction implements ActionBarItem {

	/** Collapse the expanded child row. */
	COLLAPSE("collapse", "Collapse");

	private final String wire;
	private final String label;

	SafeAction(String wire, String label) {
		this.wire = wire;
		this.label = label;
	}

	/**
	 * Returns the wire token stamped onto {@code data-juneau-safe}.
	 *
	 * @return The wire token (e.g. {@code "collapse"}).
	 */
	public String wire() {
		return wire;
	}

	/**
	 * Returns the built-in button label, painted with {@code textContent}.
	 *
	 * @return The label.
	 */
	public String label() {
		return label;
	}
}
