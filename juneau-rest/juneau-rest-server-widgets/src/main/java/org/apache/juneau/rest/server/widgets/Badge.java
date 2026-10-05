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

import org.apache.juneau.commons.bean.*;

/**
 * An overlay badge on a header action (a bell count, for example) or a {@link BarBadge}.
 *
 * <p>
 * A badge is exactly one of a <b>count</b> badge ({@link #count}) or a <b>dot</b> badge ({@link #dot}); it never
 * carries both.  The optional {@link #max} clamps a large count to <js>"&lt;max&gt;+"</js> and {@link #tone} selects
 * the badge colour from the shared {@link StatusTone} palette.  A count is painted with {@code textContent}
 * &mdash; never {@code innerHTML}.
 *
 * <p>
 * The tone is serialized as its enum name (e.g. <js>"WARNING"</js>); the client lowercases it into the
 * {@code data-juneau-badge-tone} attribute.  Colours map as:
 * <ul>
 * 	<li>{@link StatusTone#INFO} &mdash; blue
 * 	<li>{@link StatusTone#SUCCESS} &mdash; green
 * 	<li>{@link StatusTone#WARNING} &mdash; amber
 * 	<li>{@link StatusTone#ERROR} &mdash; red
 * 	<li>{@link StatusTone#NEUTRAL} &mdash; neutral
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A red unread-count badge that caps at "99+".</jc>
 * 	Badge <jv>badge</jv> = Badge.<jsm>count</jsm>(120).tone(StatusTone.<jsf>ERROR</jsf>).max(99).label(<js>"unread notifications"</js>);
 * 	<jv>badge</jv>.validate();
 * </p>
 *
 * @since 10.0.0
 */
@BeanType(properties="count,dot,max,tone,label")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class Badge {

	/** A count badge value (mutually exclusive with {@link #dot}); when set must be {@code >= 0}. */
	public Integer count;

	/** A dot (presence) badge with no number (mutually exclusive with {@link #count}). */
	public Boolean dot;

	/** Optional display clamp; a count above this renders as <js>"&lt;max&gt;+"</js>.  When set must be {@code >= 1}. */
	public Integer max;

	/** Optional colour tone; <jk>null</jk> leaves the badge in its default colour. */
	public StatusTone tone;

	/** Optional visually-hidden context for screen readers, e.g. <js>"unread notifications"</js>. */
	public String label;

	/**
	 * Creates a count badge.
	 *
	 * @param value The count.  Must be {@code >= 0}.
	 * @return A new {@link Badge}.
	 */
	public static Badge count(int value) {
		var b = new Badge();
		b.count = value;
		return b;
	}

	/**
	 * Creates a dot (presence) badge with no number.
	 *
	 * @return A new {@link Badge}.
	 */
	public static Badge dot() {
		var b = new Badge();
		b.dot = true;
		return b;
	}

	/**
	 * Sets the color tone.
	 *
	 * @param value The tone.  Can be <jk>null</jk> to use the default colour.
	 * @return This object.
	 */
	public Badge tone(StatusTone value) {
		tone = value;
		return this;
	}

	/**
	 * Sets the display clamp.
	 *
	 * @param value The clamp.  Must be {@code >= 1}.
	 * @return This object.
	 */
	public Badge max(int value) {
		max = value;
		return this;
	}

	/**
	 * Sets the visually-hidden screen-reader context.
	 *
	 * @param value The label.
	 * @return This object.
	 */
	public Badge label(String value) {
		label = value;
		return this;
	}

	/**
	 * Fail-closed bean validation; called by the enclosing bean's {@code validate()}.
	 *
	 * @throws IllegalArgumentException If this badge is not well-formed.
	 */
	public void validate() {
		var hasCount = count != null;
		var hasDot = dot != null && dot;
		if (hasCount == hasDot)
			throw iaex("Badge must declare exactly one of count/dot.");
		if (hasCount && count < 0)
			throw iaex("Badge count must be >= 0.");
		if (max != null && max < 1)
			throw iaex("Badge max must be >= 1 when set.");
	}
}
