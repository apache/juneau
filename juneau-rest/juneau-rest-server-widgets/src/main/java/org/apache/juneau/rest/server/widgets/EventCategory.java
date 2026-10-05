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
 * A declared calendar event category &mdash; a legend entry and the color family its events paint with.
 *
 * <p>
 * The color is a fixed token family ({@link CategoryColor}), never an author-supplied inline color, so the emitted
 * markup only ever carries a {@code jc-cal-cat--{blue|green|amber|red|neutral}} class built from the
 * <b>sanitized</b> id/color &mdash; no CSS-value injection surface.  Legend text and tooltip are painted with
 * {@code textContent}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class EventCategory {

	/** Stable, unique id; charset {@code [A-Za-z][A-Za-z0-9_-]*}; drives the color class. */
	public String id;

	/** Legend text; painted with {@code textContent}. */
	public String label;

	/** Fixed token family; defaults to {@link CategoryColor#NEUTRAL}. */
	public CategoryColor color;

	/** Optional legend tooltip; painted with {@code textContent}. */
	public String description;

	/**
	 * Creates an empty category.
	 *
	 * @return A new {@link EventCategory}.
	 */
	public static EventCategory create() {
		return new EventCategory();
	}

	/**
	 * Sets the id.
	 *
	 * @param value The id.  Must match {@code [A-Za-z][A-Za-z0-9_-]*}.
	 * @return This object.
	 */
	public EventCategory id(String value) {
		id = value;
		return this;
	}

	/**
	 * Sets the legend label.
	 *
	 * @param value The label.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public EventCategory label(String value) {
		label = value;
		return this;
	}

	/**
	 * Sets the color family.
	 *
	 * @param value The color family.
	 * @return This object.
	 */
	public EventCategory color(CategoryColor value) {
		color = value;
		return this;
	}

	/**
	 * Sets the legend tooltip.
	 *
	 * @param value The tooltip.
	 * @return This object.
	 */
	public EventCategory description(String value) {
		description = value;
		return this;
	}

	/**
	 * The effective color family &mdash; the declared {@link #color} or {@link CategoryColor#NEUTRAL} when unset.
	 *
	 * @return The effective color family.
	 */
	public CategoryColor effectiveColor() {
		return color == null ? CategoryColor.NEUTRAL : color;
	}

	/** The fixed category color families, each mapped to an existing {@code --jc-tag-*} token family. */
	public enum CategoryColor {

		/** Blue token family. */
		BLUE,

		/** Green token family. */
		GREEN,

		/** Amber token family. */
		AMBER,

		/** Red token family. */
		RED,

		/** Neutral token family (the default, and the fallback for an unknown wire category). */
		NEUTRAL;

		/** The lowercased token used in the {@code jc-cal-cat--{token}} class. */
		public String token() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
