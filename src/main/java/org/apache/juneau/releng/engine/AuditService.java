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

package org.apache.juneau.releng.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.juneau.releng.log.RunEventStore;

/**
 * A read-only view over what the release engine already persists: each run's event log ({@link RunEventStore}) and the
 * dropped-RC history held in each run's state ({@link RunState#rcHistory}).
 *
 * <p>
 * It never writes. Test-result events (suites, tests and replacements) are left out: they are bulk output of a step, not
 * a record of what the run did.
 */
public class AuditService {

	private static final Set<String> AUDITED = Set.of("step", "end", "note", "done");

	private final RunStateStore store;
	private final RunEventStore events;

	/**
	 * Wires the stores read by {@link #list()}.
	 *
	 * @param store The run-state store, for each run's {@code rcHistory}.
	 * @param events The run-event store.
	 */
	public AuditService(RunStateStore store, RunEventStore events) {
		this.store = store;
		this.events = events;
	}

	/**
	 * Every audited record of every run, runs oldest first, each run's events in file order followed by its dropped RCs.
	 *
	 * @return A fresh list of rows.
	 */
	public List<AuditEntry> list() {
		var runs = new LinkedHashSet<String>(events.versions());
		var states = new HashMap<String,RunState>();
		for (var rs : store.loadAll()) {
			runs.add(rs.version);
			states.put(rs.version, rs);
		}
		var out = new ArrayList<AuditEntry>();
		for (var run : runs.stream().sorted().toList()) {
			var seq = 0;
			for (var e : events.readEvents(run)) {
				seq++;
				var ev = str(e.get("ev"));
				if (ev != null && AUDITED.contains(ev))
					out.add(fromEvent(run, seq, ev, e));
			}
			var rs = states.get(run);
			if (rs != null) {
				var i = 0;
				for (var h : rs.rcHistory) {
					i++;
					out.add(new AuditEntry(run, i, "rc-dropped", h.droppedAt, "RC" + h.rc, "dropped", h.reason));
				}
			}
		}
		return out;
	}

	@SuppressWarnings({
		"java:S1192" // "status" is the event wire key, repeated once per event kind; a constant would obscure the switch.
	})
	private static AuditEntry fromEvent(String run, int seq, String ev, Map<String,Object> e) {
		return switch (ev) {
			case "step" -> new AuditEntry(run, seq, ev, null, str(e.get("id")), str(e.get("status")), str(e.get("title")));
			case "end" -> new AuditEntry(run, seq, ev, null, str(e.get("id")), str(e.get("status")), e.get("ms") == null ? null : e.get("ms") + " ms");
			case "note" -> new AuditEntry(run, seq, ev, null, str(e.get("step")), str(e.get("level")), str(e.get("text")));
			default -> new AuditEntry(run, seq, ev, null, null, str(e.get("status")), null);
		};
	}

	private static String str(Object o) {
		return o == null ? null : o.toString();
	}
}
