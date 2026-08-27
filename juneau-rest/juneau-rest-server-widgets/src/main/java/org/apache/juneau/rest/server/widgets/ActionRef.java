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

/**
 * An opaque id naming a write action on the enclosing view.
 *
 * <p>
 * This type deliberately does <b>not</b> import any views-module type.  Existence of the named action on the
 * enclosing view's action catalog is checked in views ({@code RowDetailDef}/{@code ViewDef.validate()}), never
 * here.  The bar does not grow its own write protocol (no endpoint / method / csrf fields).
 *
 * @since 10.0.0
 */
public class ActionRef implements ActionBarItem {

	/** The opaque action id.  Must not be blank. */
	public String id;

	/** The visual weight this bar paints for this action.  Defaults to {@link Emphasis#SECONDARY}. */
	public Emphasis emphasis = Emphasis.SECONDARY;

	/**
	 * Creates an action reference with the specified id.
	 *
	 * @param id The action id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link ActionRef}.
	 */
	public static ActionRef of(String id) {
		if (id == null || id.isBlank())
			throw iaex("ActionRef id must not be null or blank.");
		var a = new ActionRef();
		a.id = id;
		return a;
	}

	/**
	 * Sets the visual weight this bar paints for this action.
	 *
	 * <p>
	 * This is a per-bar rendering property, not a property of the action itself &mdash; the same action can be
	 * the primary button in one bar (e.g. the Detail View header) and an ordinary item in another.  Java-only:
	 * never marshalled into {@code VIEW_META}, so it does not move {@link ActionBar#CONTRACT_VERSION}.
	 *
	 * @param value The emphasis.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ActionRef emphasis(Emphasis value) {
		this.emphasis = value;
		return this;
	}

	/** The visual weight an {@link ActionRef} button is painted with by the {@link ActionBar} that renders it. */
	public enum Emphasis {

		/** The bar's single most prominent action &mdash; solid accent fill.  At most one per bar. */
		PRIMARY,

		/** The default weight &mdash; neutral fill, accent-coloured label. */
		SECONDARY
	}
}
