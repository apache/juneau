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

import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.runreport.*;

/**
 * One page of a run-view event stream: the envelope the events endpoint returns and the client polls.
 *
 * <p>
 * The wire form is <c>{contractVersion, events, next, more, terminal}</c>.  A page with no events still carries
 * <c>next</c>.
 */
public final class RunViewPage {

	/** The contract version carried by every page. */
	public static final String CONTRACT_VERSION = "1";

	/** The maximum number of events in one page. */
	public static final int MAX_PAGE_EVENTS = 10_000;

	/** The maximum serialized size of the events in one page; the same budget as a console-output page. */
	public static final int MAX_PAGE_CHARS = 8_388_608;

	private final List<RunEvent> events;
	private final String next;
	private final boolean more;
	private final boolean terminal;

	private RunViewPage(List<RunEvent> events, String next, boolean more, boolean terminal) {
		this.events = events;
		this.next = next;
		this.more = more;
		this.terminal = terminal;
	}

	/**
	 * Creates a page.
	 *
	 * @param events The events, in ascending <c>seq</c>.  Copied defensively.
	 * @param next The continuation token.
	 * @param more Whether the source holds further events right now.
	 * @param terminal Whether the source will never produce another event.
	 * @return A new page.  Call {@link #validate()} before sending it.
	 */
	public static RunViewPage of(List<RunEvent> events, String next, boolean more, boolean terminal) {
		return new RunViewPage(List.copyOf(events == null ? List.of() : events), next, more, terminal);
	}

	/**
	 * Returns the events.
	 *
	 * @return An unmodifiable list.
	 */
	public List<RunEvent> events() {
		return events;
	}

	/**
	 * Returns the continuation token.
	 *
	 * @return The token.
	 */
	public String next() {
		return next;
	}

	/**
	 * Returns whether the source holds further events right now.
	 *
	 * @return <jk>true</jk> if so.
	 */
	public boolean more() {
		return more;
	}

	/**
	 * Returns whether the source will never produce another event.
	 *
	 * @return <jk>true</jk> if so.
	 */
	public boolean terminal() {
		return terminal;
	}

	/**
	 * Validates the page: ascending numbered events, the token grammar, and the size caps.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If the page is not valid.
	 */
	public RunViewPage validate() {
		if (! RunViewChecks.isToken(next))
			throw iaex("RunViewPage next must match ^[A-Za-z0-9._~-]{1,128}$; got '%s'", ConsoleOutputChecks.clip(next));
		if (events.size() > MAX_PAGE_EVENTS)
			throw iaex("RunViewPage events must hold at most %s events; got %s", MAX_PAGE_EVENTS, events.size());
		if (more && events.isEmpty())
			throw iaex("RunViewPage must not set more with no events");
		var last = 0L;
		long chars = 0;
		for (var e : events) {
			if (e.seq() < 1)
				throw iaex("RunViewPage events must carry a seq >= 1; got %s on a '%s' event", e.seq(), e.kind().name().toLowerCase(Locale.ROOT));
			if (e.seq() <= last)
				throw iaex("RunViewPage events must be in strictly ascending seq order; got %s after %s", e.seq(), last);
			last = e.seq();
			chars += JsonSerializer.DEFAULT.toString(e.toContractMap()).length();
		}
		if (chars > MAX_PAGE_CHARS)
			throw iaex("RunViewPage events must total at most %s chars; got %s", MAX_PAGE_CHARS, chars);
		return this;
	}

	/**
	 * Returns the page in its wire form.
	 *
	 * @return An ordered map: <c>contractVersion</c>, <c>events</c>, <c>next</c>, <c>more</c>, <c>terminal</c>.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("contractVersion", CONTRACT_VERSION);
		var list = new ArrayList<Map<String,Object>>(events.size());
		for (var e : events)
			list.add(e.toContractMap());
		m.put("events", list);
		m.put("next", next);
		m.put("more", more);
		m.put("terminal", terminal);
		return m;
	}
}
