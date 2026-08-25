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

import java.util.*;

/**
 * The one closed status-tone palette shared by every surface that paints a semantic status colour:
 * {@code info} / {@code success} / {@code warning} / {@code error} / {@code neutral}.
 *
 * <p>
 * Exactly five values, and no synonyms.  A surface that accepts a tone accepts a {@link #wire()} token from
 * {@link #WIRE_TOKENS} and <b>fails closed</b> on anything else, so the same five names mean the same five things in
 * Java, in the emitted {@code is-<tone>} CSS class, and in the {@code juneau-renders.js} tone map.
 *
 * <p>
 * Deliberately distinct from {@link Tone}, which is the older {@link Badge} overlay palette
 * ({@code NEUTRAL, ACCENT, SUCCESS, DANGER, WARN}) and maps to a different set of {@code --jc-*} tokens.  The two are
 * not interchangeable: an {@code ACCENT}/{@code DANGER}/{@code WARN} name is not a status tone and is rejected by any
 * surface that takes a {@link StatusTone}.
 *
 * @since 10.0.0
 */
public enum StatusTone {

	/** Informational (neither good nor bad); maps to {@code is-info}. */
	INFO,

	/** Healthy / completed; maps to {@code is-success}. */
	SUCCESS,

	/** Approaching a limit or needing attention; maps to {@code is-warning}. */
	WARNING,

	/** Failed / over a hard limit; maps to {@code is-error}. */
	ERROR,

	/** No semantic colour; inherits the surrounding text colour and emits no tone class. */
	NEUTRAL;

	/**
	 * The five legal wire tokens, in declaration order.
	 *
	 * <p>
	 * The single source of truth every fail-closed tone check reads, so no caller can hand-roll a fourth or sixth
	 * value.
	 */
	public static final Set<String> WIRE_TOKENS = wireTokens();

	private static Set<String> wireTokens() {
		var s = new LinkedHashSet<String>();
		for (var t : values())
			s.add(t.wire());
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Returns the lowercase wire token for this tone.
	 *
	 * @return The wire token (e.g. <c>"warning"</c>).
	 */
	public String wire() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Returns <jk>true</jk> if the specified token is one of the five legal tones.
	 *
	 * @param token The candidate wire token.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the token is a legal tone; <jk>false</jk> for <jk>null</jk>, blank, differently-cased,
	 * 	or off-palette values.
	 */
	public static boolean isValid(String token) {
		return token != null && WIRE_TOKENS.contains(token);
	}
}
