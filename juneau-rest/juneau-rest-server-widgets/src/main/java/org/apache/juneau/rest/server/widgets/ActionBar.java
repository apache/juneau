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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * An ordered bar of {@link ActionRef} ids and {@link SafeAction}s.
 *
 * <p>
 * Holds ids only &mdash; it does <b>not</b> import any views-module write-action type.  CSRF, confirm, and
 * write-result handling stay on the enclosing view's action catalog.
 *
 * @since 10.0.0
 */
public class ActionBar implements Widget {

	/** The frozen contract version for this widget. */
	public static final String CONTRACT_VERSION = "1";

	/** The ordered items; omitted / empty means no bar. */
	public List<ActionBarItem> items;

	/**
	 * Creates an empty action bar.
	 *
	 * @return A new {@link ActionBar}.
	 */
	public static ActionBar create() {
		return new ActionBar();
	}

	/**
	 * Sets the ordered items.
	 *
	 * @param value The items, in display order.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ActionBar items(ActionBarItem...value) {
		items = l(value);
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (items == null)
			return;
		for (var item : items) {
			if (item == null)
				throw iaex("ActionBar item must not be null.");
			if (item instanceof ActionRef ar) {
				if (ar.id == null || ar.id.isBlank())
					throw iaex("ActionRef id must not be null or blank.");
			} else if (!(item instanceof SafeAction))
				throw iaex("ActionBar item must be ActionRef or SafeAction.");
		}
	}
}
