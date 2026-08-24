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

import org.apache.juneau.commons.bean.*;

/**
 * A labelled count widget in a {@link BarSlot} &mdash; the "N change pending"-style badge to the right of the subtab
 * ribbon.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,badge")
public final class BarBadge implements BarWidget {

	/** The stable widget id, unique within its {@link BarSlot}.  Required, non-blank. */
	public String id;

	/** The contextual label painted as {@code textContent} (e.g. <js>"change pending"</js>). */
	public String label;

	/** The overlay count/tone badge (amber for the yellow "pending" badge). */
	public Badge badge;

	/**
	 * Creates a bar badge with the given id.
	 *
	 * @param id The stable widget id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link BarBadge}.
	 */
	public static BarBadge of(String id) {
		var b = new BarBadge();
		b.id = id;
		return b;
	}

	/**
	 * Sets the contextual label.
	 *
	 * @param value The label text.
	 * @return This object.
	 */
	public BarBadge label(String value) {
		label = value;
		return this;
	}

	/**
	 * Sets the count/tone badge.
	 *
	 * @param value The badge.
	 * @return This object.
	 */
	public BarBadge badge(Badge value) {
		badge = value;
		return this;
	}
}
