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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.rest.server.views.ConsoleOutputChecks.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;

/**
 * The response envelope of a console-output lines endpoint ({@code ?after=}, {@code ?tail=}, {@code ?before=}).
 *
 * <p>
 * Endpoints return {@link #toContractMap()}, never this bean.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConsoleOutputPage <jv>page</jv> = ConsoleOutputPage.<jsm>forward</jsm>()
 * 		.lines(<jv>lines</jv>).next(<js>"42"</js>)
 * 		.status(<js>"RUNNING"</js>, Style.<jsf>ACCENT</jsf>, <jk>false</jk>, <jv>startedAt</jv>, <jk>null</jk>);
 * </p>
 *
 * @since 10.0.0
 */
public class ConsoleOutputPage {

	/** The contract version, always {@code "1"}. */
	public static final String CONTRACT_VERSION = "1";

	/** The visible-character budget of one page, summed across lines. */
	public static final int MAX_PAGE_CHARS = 8_388_608;

	/** Page kinds. */
	public enum Kind {
		/** A forward or tail page: carries {@code next}. */
		FORWARD,
		/** An earlier ({@code ?before=}) page: never carries {@code next}. */
		EARLIER
	}

	/** This page's kind. */
	public final Kind kind;
	/** Lines in strictly ascending {@code n}. */
	public List<ConsoleOutputLine> lines = new ArrayList<>();
	/** Continuation token (forward and tail pages only). */
	public String next;
	/** The server capped this page; fetch again immediately. */
	public boolean more;
	/** Lines exist before {@code lines[0]}. */
	public boolean hasEarlier;
	/** Token for {@code ?before=}; present exactly when {@link #hasEarlier}. */
	public String before;
	/** Free-form display state. */
	public String state;
	/** Optional state style. */
	public Style stateStyle;
	/** Stop polling after this page. */
	public boolean terminal;
	/** Absent means not started yet. */
	public Instant startedAt;
	/** Only on terminal pages. */
	public Long durationMs;
	/** The server clock; defaulted by {@link #validate()}. */
	public Instant now;

	private ConsoleOutputPage(Kind kind) {
		this.kind = kind;
	}

	/**
	 * Creates a forward or tail page.
	 *
	 * @return A new page.
	 */
	public static ConsoleOutputPage forward() {
		return new ConsoleOutputPage(Kind.FORWARD);
	}

	/**
	 * Creates an earlier ({@code ?before=}) page.
	 *
	 * @return A new page.
	 */
	public static ConsoleOutputPage earlier() {
		return new ConsoleOutputPage(Kind.EARLIER);
	}

	/**
	 * Sets the lines.
	 *
	 * @param value The lines, ascending in {@code n}.
	 * @return This object.
	 */
	public ConsoleOutputPage lines(List<ConsoleOutputLine> value) {
		lines = value == null ? new ArrayList<>() : new ArrayList<>(value);
		return this;
	}

	/**
	 * Sets the continuation token.
	 *
	 * @param value The token.
	 * @return This object.
	 */
	public ConsoleOutputPage next(String value) {
		next = value;
		return this;
	}

	/**
	 * Sets the capped-page flag.
	 *
	 * @param value The flag.
	 * @return This object.
	 */
	public ConsoleOutputPage more(boolean value) {
		more = value;
		return this;
	}

	/**
	 * Sets the earlier-page token; also sets {@link #hasEarlier} to whether it is non-null.
	 *
	 * @param token The token of {@code lines[0]}, or <jk>null</jk> when nothing precedes it.
	 * @return This object.
	 */
	public ConsoleOutputPage before(String token) {
		before = token;
		hasEarlier = token != null;
		return this;
	}

	/**
	 * Sets the state fields.
	 *
	 * @param state The display state.
	 * @param stateStyle The optional style.
	 * @param terminal Whether this is the terminal page.
	 * @param startedAt The start instant, or <jk>null</jk> when pending.
	 * @param durationMs The duration (terminal only), or <jk>null</jk>.
	 * @return This object.
	 */
	public ConsoleOutputPage status(String state, Style stateStyle, boolean terminal, Instant startedAt, Long durationMs) {
		this.state = state;
		this.stateStyle = stateStyle;
		this.terminal = terminal;
		this.startedAt = startedAt;
		this.durationMs = durationMs;
		return this;
	}

	/**
	 * Sets the server clock value.
	 *
	 * @param value The instant.
	 * @return This object.
	 */
	public ConsoleOutputPage now(Instant value) {
		now = value;
		return this;
	}

	/**
	 * Visible characters of one line: its text, or the summed text of its text fragments.
	 *
	 * @param line The line.
	 * @return The count.
	 */
	public static int visibleChars(ConsoleOutputLine line) {
		if (line.text != null)
			return line.text.length();
		var c = 0;
		if (line.frags != null)
			for (var f : line.frags)
				if (f != null && f.text != null)
					c += f.text.length();
		return c;
	}

	/**
	 * Validates this page and every line in it (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If the page breaks the contract.
	 */
	public ConsoleOutputPage validate() {
		if (lines == null)
			lines = new ArrayList<>();
		Long last = null;
		for (var i = 0; i < lines.size(); i++) {
			var l = lines.get(i);
			if (l == null)
				throw iaex("ConsoleOutputPage lines[%s] is null.", i);
			if (l.n == null)
				throw iaex("ConsoleOutputPage lines[%s].n is required in a page response.", i);
			if (last != null && l.n <= last)
				throw iaex("ConsoleOutputPage lines must be strictly increasing in n; got %s after %s.", l.n, last);
			last = l.n;
			l.validate();
			if (Boolean.TRUE.equals(l.open) && i != lines.size() - 1)
				throw iaex("ConsoleOutputPage lines[%s] is open but not the last line.", i);
		}
		if (terminal && ! lines.isEmpty() && Boolean.TRUE.equals(lines.get(lines.size() - 1).open))
			throw iaex("ConsoleOutputPage a terminal page cannot end on an open line.");
		if (kind == Kind.FORWARD && next == null)
			throw iaex("ConsoleOutputPage forward page needs next.");
		if (kind == Kind.EARLIER && next != null)
			throw iaex("ConsoleOutputPage earlier page must not carry next.");
		if (next != null && ! isToken(next))
			throw iaex("ConsoleOutputPage next is not a valid token; got '%s'.", clip(next));
		if (hasEarlier != (before != null))
			throw iaex("ConsoleOutputPage before must be present exactly when hasEarlier is true.");
		if (before != null && ! isToken(before))
			throw iaex("ConsoleOutputPage before is not a valid token; got '%s'.", clip(before));
		if (more && lines.isEmpty())
			throw iaex("ConsoleOutputPage more must be false when lines is empty.");
		if (more && terminal)
			throw iaex("ConsoleOutputPage more must be false on a terminal page.");
		if (more && kind == Kind.EARLIER)
			throw iaex("ConsoleOutputPage more must be false on an earlier page.");
		if (durationMs != null && ! terminal)
			throw iaex("ConsoleOutputPage durationMs is only allowed on a terminal page.");
		if (durationMs != null && durationMs < 0)
			throw iaex("ConsoleOutputPage durationMs must be >= 0; got %s.", durationMs);
		if (now == null)
			now = Instant.now();
		return this;
	}

	/**
	 * Returns the wire form: an ordered map with absent members omitted.
	 *
	 * @return A new map.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", CONTRACT_VERSION);
		var l = new ArrayList<Object>(lines.size());
		for (var line : lines)
			l.add(line.toContractMap());
		m.put("lines", l);
		if (next != null)
			m.put("next", next);
		m.put("more", more);
		m.put("hasEarlier", hasEarlier);
		if (before != null)
			m.put("before", before);
		if (state != null)
			m.put("state", state);
		if (stateStyle != null)
			m.put("stateStyle", stateStyle.wire());
		m.put("terminal", terminal);
		if (startedAt != null)
			m.put("startedAt", ConsoleOutputLine.INSTANT_FORMAT.format(startedAt));
		if (durationMs != null)
			m.put("durationMs", durationMs);
		if (now != null)
			m.put("now", ConsoleOutputLine.INSTANT_FORMAT.format(now));
		return m;
	}
}
