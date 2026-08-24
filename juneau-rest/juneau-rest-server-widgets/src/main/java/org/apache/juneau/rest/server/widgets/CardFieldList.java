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

import org.apache.juneau.commons.http.*;

/**
 * A read-only key/value {@link CardBody} &mdash; the v1 refreshable card body.
 *
 * <p>
 * Server-rendered from each {@link CardField}'s initial value so a static field-list paints with JavaScript
 * disabled.  When a {@link #refreshEndpoint} is set, the enclosing card gains a built-in refresh button (and,
 * optionally, an auto-refresh poll timer): the client re-fills the {@code [data-juneau-card-field]} slots from a
 * data-only GET returning {@code {contractVersion, fields}}.
 *
 * <p>
 * The refresh wire lives here &mdash; not on {@link Card} &mdash; so a non-refreshable body can never carry a
 * dangling refresh endpoint.
 *
 * @since 10.0.0
 */
public class CardFieldList implements CardBody {

	/** The frozen contract version for the refresh GET envelope and the stamped {@code data-juneau-card-contract}. */
	public static final String CONTRACT_VERSION = "1";

	/** CSS-grid column hint, stamped into the emitted markup.  Must be {@code >= 1}. */
	public int columns = 2;

	/** The fields, in display order.  At least one is required; data keys must be unique and non-blank. */
	public List<CardField> fields;

	/**
	 * Optional same-origin, <b>non-templated</b> refresh path.  When set, enables the built-in refresh button on
	 * the enclosing card.  A {@code {…}} template placeholder is rejected (a field-list is not row-scoped).
	 */
	public String refreshEndpoint;

	/**
	 * Optional auto-refresh interval, in milliseconds.  Requires {@link #refreshEndpoint}.  Clamped in
	 * {@link #validate()} up to {@link SafePathTemplate#MIN_POLL_INTERVAL_MS} (commons; the same numeric floor
	 * {@code ViewDef.poll()} uses).  {@link Integer} (not {@code int}) so an unset interval is representable.
	 */
	public Integer pollIntervalMs;

	/**
	 * Creates an empty field-list with the default {@code columns} of {@code 2}.
	 *
	 * @return A new {@link CardFieldList}.
	 */
	public static CardFieldList create() {
		return new CardFieldList();
	}

	/**
	 * Sets the CSS-grid column hint.
	 *
	 * @param value The column count.  Must be {@code >= 1}.
	 * @return This object.
	 */
	public CardFieldList columns(int value) {
		columns = value;
		return this;
	}

	/**
	 * Sets the fields, in display order.
	 *
	 * @param value The fields.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public CardFieldList fields(CardField...value) {
		fields = l(value);
		return this;
	}

	/**
	 * Sets the same-origin, non-templated refresh endpoint.
	 *
	 * @param endpoint The refresh path.  Must not be <jk>null</jk> or blank when set.
	 * @return This object.
	 */
	public CardFieldList refresh(String endpoint) {
		refreshEndpoint = endpoint;
		return this;
	}

	/**
	 * Sets (or clears) the auto-refresh interval.
	 *
	 * @param value The interval in milliseconds, or <jk>null</jk> to clear.
	 * @return This object.
	 */
	public CardFieldList pollIntervalMs(Integer value) {
		pollIntervalMs = value;
		return this;
	}

	@Override /* CardBody */
	public void validate() {
		if (columns < 1)
			throw iaex("CardFieldList columns must be >= 1.");
		if (fields == null || fields.isEmpty())
			throw iaex("CardFieldList must declare at least one field.");
		var keys = new HashSet<String>();
		for (var f : fields) {
			if (f == null)
				throw iaex("CardFieldList field must not be null.");
			if (f.data == null || f.data.isBlank())
				throw iaex("CardField data must not be null or blank.");
			if (!keys.add(f.data))
				throw iaex("CardFieldList duplicate field data key '%s'.", f.data);
		}
		if (pollIntervalMs != null && (refreshEndpoint == null || refreshEndpoint.isBlank()))
			throw iaex("CardFieldList pollIntervalMs requires a refreshEndpoint.");
		if (refreshEndpoint != null && !refreshEndpoint.isBlank()) {
			if (!SafePathTemplate.isNonTemplatedPath(refreshEndpoint))
				throw iaex("CardFieldList refreshEndpoint must be a same-origin, non-templated path (no absolute URL, "
					+ "'//', scheme, '..', or '{…}' placeholder): %s", refreshEndpoint);
			if (pollIntervalMs != null)
				pollIntervalMs = (int) SafePathTemplate.clampPollInterval(pollIntervalMs);
		}
	}
}
