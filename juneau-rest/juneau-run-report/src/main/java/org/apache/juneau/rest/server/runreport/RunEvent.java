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
package org.apache.juneau.rest.server.runreport;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.json.*;

/**
 * One event of a run-view stream.
 *
 * <p>
 * An immutable value with static factories, one per event kind.  Every factory and <c>with*</c> method validates its
 * argument against the event contract and throws {@link IllegalArgumentException} naming the member, so a producer
 * finds its bug where it is written.  {@link #toContractMap()} produces the wire form, and {@link #fromMap(Map)} is
 * its strict inverse.
 *
 * <p>
 * A {@link #seq()} of <c>0</c> means the event has not been numbered yet; sources assign it.
 */
public final class RunEvent {

	/** The event kinds.  The wire form is lower case. */
	public enum Kind {
		/** A step starts or is updated. */ STEP,
		/** A step ends. */ END,
		/** A placeholder suite result. */ SUITE,
		/** One test result. */ TEST,
		/** Discard a step's suites and tests. */ REPLACE,
		/** A notable event. */ NOTE,
		/** The run finished. */ DONE
	}

	/** The state of a step that has not ended. */
	public enum StepState {
		/** The step is running. */ RUNNING,
		/** The step is blocked on something outside the run. */ WAITING
	}

	/** The final status of a step. */
	public enum EndStatus {
		/** Passed. */ OK,
		/** Failed. */ FAIL,
		/** Skipped. */ SKIP
	}

	/** The status of a test. */
	public enum TestStatus {
		/** Passed. */ PASS,
		/** Failed. */ FAIL,
		/** Skipped. */ SKIP,
		/** Errored. */ ERROR
	}

	/** The level of a note. */
	public enum Level {
		/** Informational. */ INFO,
		/** Warning. */ WARN,
		/** Error. */ ERROR
	}

	/** The final status of a run. */
	public enum DoneStatus {
		/** Passed. */ OK,
		/** Failed. */ FAIL,
		/** Cancelled. */ CANCELLED
	}

	/** The maximum length of one serialized event. */
	public static final int MAX_EVENT_CHARS = 65_536;

	/** The maximum length of a step title. */
	public static final int MAX_TITLE = 200;

	/** The maximum length of a suite or test name. */
	public static final int MAX_NAME = 512;

	/** The maximum length of a failure message. */
	public static final int MAX_MSG = 2000;

	/** The maximum length of a stack trace. */
	public static final int MAX_TRACE = 8000;

	/** The maximum length of a note text. */
	public static final int MAX_NOTE = 1000;

	/** The largest integer a JSON number can carry exactly. */
	public static final long MAX_SAFE_INT = 9_007_199_254_740_991L;

	private static final int MAX_COUNT = 1_000_000_000;

	private static final Map<Kind,List<String>> KEY_ORDER = Map.of(
		Kind.STEP, List.of("id", "title", "n", "status", "rawLine"),
		Kind.END, List.of("id", "status", "ms", "exit"),
		Kind.SUITE, List.of("step", "fw", "suite", "counts", "rawLine"),
		Kind.TEST, List.of("step", "fw", "suite", "name", "status", "ms", "msg", "trace", "rawLine"),
		Kind.REPLACE, List.of("step"),
		Kind.NOTE, List.of("level", "text", "href", "step"),
		Kind.DONE, List.of("status")
	);

	private final Kind kind;
	private final long seq;
	private final Map<String,Object> members;

	private RunEvent(Kind kind, long seq, Map<String,Object> members) {
		this.kind = kind;
		this.seq = seq;
		this.members = members;
	}

	private RunEvent with(String key, Object value) {
		var m = new HashMap<>(members);
		m.put(key, value);
		return new RunEvent(kind, seq, m);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Factories
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a <c>step</c> event.
	 *
	 * @param id The step id.  Must match <c>^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$</c>.
	 * @param title The step title, 1-200 chars.
	 * @return A new event.
	 */
	public static RunEvent step(String id, String title) {
		var m = new HashMap<String,Object>();
		m.put("id", stepId("step", "id", id));
		m.put("title", text("step", "title", title, 1, MAX_TITLE));
		return new RunEvent(Kind.STEP, 0, m);
	}

	/**
	 * Creates an <c>end</c> event.
	 *
	 * @param id The id of a known step.
	 * @param status The final status.
	 * @return A new event.
	 */
	public static RunEvent end(String id, EndStatus status) {
		var m = new HashMap<String,Object>();
		m.put("id", stepId("end", "id", id));
		m.put("status", wire("end", "status", status));
		return new RunEvent(Kind.END, 0, m);
	}

	/**
	 * Creates a <c>suite</c> placeholder event.
	 *
	 * @param step The id of a known step.
	 * @param fw The test framework key.
	 * @param suite The suite name, 1-512 chars.
	 * @param pass The pass count, 0-1,000,000,000.
	 * @param fail The fail count, 0-1,000,000,000.
	 * @param skip The skip count, 0-1,000,000,000.
	 * @return A new event.
	 */
	public static RunEvent suite(String step, String fw, String suite, int pass, int fail, int skip) {
		var m = new HashMap<String,Object>();
		m.put("step", stepId("suite", "step", step));
		m.put("fw", fw("suite", fw));
		m.put("suite", text("suite", "suite", suite, 1, MAX_NAME));
		var counts = new LinkedHashMap<String,Object>();
		counts.put("pass", count("pass", pass));
		counts.put("fail", count("fail", fail));
		counts.put("skip", count("skip", skip));
		m.put("counts", counts);
		return new RunEvent(Kind.SUITE, 0, m);
	}

	/**
	 * Creates a <c>test</c> event.
	 *
	 * @param step The id of a known step.
	 * @param fw The test framework key.
	 * @param suite The suite name, 1-512 chars.
	 * @param name The test name, 1-512 chars.
	 * @param status The test status.
	 * @return A new event.
	 */
	public static RunEvent test(String step, String fw, String suite, String name, TestStatus status) {
		var m = new HashMap<String,Object>();
		m.put("step", stepId("test", "step", step));
		m.put("fw", fw("test", fw));
		m.put("suite", text("test", "suite", suite, 1, MAX_NAME));
		m.put("name", text("test", "name", name, 1, MAX_NAME));
		m.put("status", wire("test", "status", status));
		return new RunEvent(Kind.TEST, 0, m);
	}

	/**
	 * Creates a <c>replace</c> event.
	 *
	 * @param step The id of a known step.
	 * @return A new event.
	 */
	public static RunEvent replace(String step) {
		var m = new HashMap<String,Object>();
		m.put("step", stepId("replace", "step", step));
		return new RunEvent(Kind.REPLACE, 0, m);
	}

	/**
	 * Creates a <c>note</c> event.
	 *
	 * @param level The level.
	 * @param text The plain text, 1-1,000 chars.
	 * @return A new event.
	 */
	public static RunEvent note(Level level, String text) {
		var m = new HashMap<String,Object>();
		m.put("level", wire("note", "level", level));
		m.put("text", text("note", "text", text, 1, MAX_NOTE));
		return new RunEvent(Kind.NOTE, 0, m);
	}

	/**
	 * Creates a <c>done</c> event.
	 *
	 * @param status The final run status.
	 * @return A new event.
	 */
	public static RunEvent done(DoneStatus status) {
		var m = new HashMap<String,Object>();
		m.put("status", wire("done", "status", status));
		return new RunEvent(Kind.DONE, 0, m);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Optional members
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the display ordinal of a <c>step</c> event.
	 *
	 * @param n The ordinal, 1-9999.
	 * @return A copy with the member set.
	 */
	public RunEvent withN(int n) {
		requireKind("n", Kind.STEP);
		if (n < 1 || n > 9999)
			throw iaex("RunEvent step n must be 1-9999; got '%s'", n);
		return with("n", n);
	}

	/**
	 * Sets the state of a <c>step</c> event.
	 *
	 * @param state The state.
	 * @return A copy with the member set.
	 */
	public RunEvent withState(StepState state) {
		requireKind("status", Kind.STEP);
		return with("status", wire("step", "status", state));
	}

	/**
	 * Sets the raw console line of a <c>step</c>, <c>suite</c> or <c>test</c> event.
	 *
	 * @param line The 1-based line, at least 1.
	 * @return A copy with the member set.
	 */
	public RunEvent withRawLine(int line) {
		requireKind("rawLine", Kind.STEP, Kind.SUITE, Kind.TEST);
		if (line < 1)
			throw iaex("RunEvent %s rawLine must be >= 1; got '%s'", wireName(kind), line);
		return with("rawLine", line);
	}

	/**
	 * Sets the duration of an <c>end</c> or <c>test</c> event.
	 *
	 * @param ms The duration in milliseconds, 0 to 9007199254740991.
	 * @return A copy with the member set.
	 */
	public RunEvent withMs(long ms) {
		requireKind("ms", Kind.END, Kind.TEST);
		if (ms < 0 || ms > MAX_SAFE_INT)
			throw iaex("RunEvent %s ms must be 0-%s; got '%s'", wireName(kind), MAX_SAFE_INT, ms);
		return with("ms", ms);
	}

	/**
	 * Sets the process exit code of an <c>end</c> event.
	 *
	 * @param exit The exit code.
	 * @return A copy with the member set.
	 */
	public RunEvent withExit(int exit) {
		requireKind("exit", Kind.END);
		return with("exit", exit);
	}

	/**
	 * Sets the failure message of a <c>test</c> event.
	 *
	 * @param msg The message, at most 2,000 chars.
	 * @return A copy with the member set.
	 */
	public RunEvent withMsg(String msg) {
		requireKind("msg", Kind.TEST);
		return with("msg", text("test", "msg", msg, 0, MAX_MSG));
	}

	/**
	 * Sets the stack trace of a <c>test</c> event.
	 *
	 * @param trace The trace, at most 8,000 chars.
	 * @return A copy with the member set.
	 */
	public RunEvent withTrace(String trace) {
		requireKind("trace", Kind.TEST);
		return with("trace", text("test", "trace", trace, 0, MAX_TRACE));
	}

	/**
	 * Sets the link of a <c>note</c> event.
	 *
	 * @param href An absolute http(s) URL or a same-origin path.
	 * @return A copy with the member set.
	 */
	public RunEvent withHref(String href) {
		requireKind("href", Kind.NOTE);
		if (! RunViewChecks.isNoteHref(href))
			throw iaex("RunEvent note href must be an absolute http(s) URL or a same-origin path; got '%s'", RunViewChecks.clip(href));
		return with("href", href);
	}

	/**
	 * Sets the step a <c>note</c> belongs under.
	 *
	 * @param step A step id.
	 * @return A copy with the member set.
	 */
	public RunEvent withStep(String step) {
		requireKind("step", Kind.NOTE);
		return with("step", stepId("note", "step", step));
	}

	/**
	 * Returns a copy with the sequence number set.
	 *
	 * @param seq The sequence number, 1 to 9007199254740991, or 0 for unassigned.
	 * @return A copy.
	 */
	public RunEvent withSeq(long seq) {
		if (seq < 0 || seq > MAX_SAFE_INT)
			throw iaex("RunEvent seq must be 0-%s; got '%s'", MAX_SAFE_INT, seq);
		return new RunEvent(kind, seq, members);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Accessors and wire form
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the event kind.
	 *
	 * @return The kind.
	 */
	public Kind kind() {
		return kind;
	}

	/**
	 * Returns the sequence number.
	 *
	 * @return The sequence number, or <c>0</c> when unassigned.
	 */
	public long seq() {
		return seq;
	}

	/**
	 * Returns the event in its wire form.
	 *
	 * <p>
	 * The map is ordered (<c>ev</c>, <c>seq</c> when assigned, then the members in contract order) and omits unset
	 * optional members.  A fresh map is returned on every call.
	 *
	 * @return The wire map.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("ev", wireName(kind));
		if (seq > 0)
			m.put("seq", seq);
		for (var key : KEY_ORDER.get(kind)) {
			var v = members.get(key);
			if (v == null)
				continue;
			if (v instanceof Map<?,?> c)
				v = new LinkedHashMap<>(c);
			m.put(key, v);
		}
		return m;
	}

	/**
	 * Checks that the serialized event fits in {@link #MAX_EVENT_CHARS}.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If it does not.
	 */
	public RunEvent validate() {
		var len = JsonSerializer.DEFAULT.toString(toContractMap()).length();
		if (len > MAX_EVENT_CHARS)
			throw iaex("RunEvent serialized size must be <= %s chars; got %s", MAX_EVENT_CHARS, len);
		return this;
	}

	/**
	 * Builds an event from its wire form.
	 *
	 * <p>
	 * The strict inverse of {@link #toContractMap()}.  Unknown members are ignored; an unknown <c>ev</c> or a
	 * malformed known member throws.
	 *
	 * @param map The wire map.
	 * @return A new event.
	 * @throws IllegalArgumentException If the map is not a valid event.
	 */
	public static RunEvent fromMap(Map<?,?> map) {
		if (map == null)
			throw iaex("RunEvent map must not be null");
		var ev = map.get("ev");
		if (! (ev instanceof String evs))
			throw iaex("RunEvent ev must be a string; got '%s'", ev);
		RunEvent e;
		switch (evs) {
			case "step" -> {
				e = step(str(map, "step", "id"), str(map, "step", "title"));
				if (map.get("n") != null)
					e = e.withN(toInt(map, "step", "n"));
				if (map.get("status") != null)
					e = e.withState(parse(StepState.class, "step", "status", map.get("status")));
				if (map.get("rawLine") != null)
					e = e.withRawLine(toInt(map, "step", "rawLine"));
			}
			case "end" -> {
				e = end(str(map, "end", "id"), parse(EndStatus.class, "end", "status", map.get("status")));
				if (map.get("ms") != null)
					e = e.withMs(toLong(map, "end", "ms"));
				if (map.get("exit") != null)
					e = e.withExit(toInt(map, "end", "exit"));
			}
			case "suite" -> {
				if (! (map.get("counts") instanceof Map<?,?> c))
					throw iaex("RunEvent suite counts must be an object; got '%s'", map.get("counts"));
				e = suite(str(map, "suite", "step"), str(map, "suite", "fw"), str(map, "suite", "suite"),
					toInt(c, "suite", "counts.pass"), toInt(c, "suite", "counts.fail"), toInt(c, "suite", "counts.skip"));
				if (map.get("rawLine") != null)
					e = e.withRawLine(toInt(map, "suite", "rawLine"));
			}
			case "test" -> {
				e = test(str(map, "test", "step"), str(map, "test", "fw"), str(map, "test", "suite"), str(map, "test", "name"),
					parse(TestStatus.class, "test", "status", map.get("status")));
				if (map.get("ms") != null)
					e = e.withMs(toLong(map, "test", "ms"));
				if (map.get("msg") != null)
					e = e.withMsg(str(map, "test", "msg"));
				if (map.get("trace") != null)
					e = e.withTrace(str(map, "test", "trace"));
				if (map.get("rawLine") != null)
					e = e.withRawLine(toInt(map, "test", "rawLine"));
			}
			case "replace" -> e = replace(str(map, "replace", "step"));
			case "note" -> {
				e = note(parse(Level.class, "note", "level", map.get("level")), str(map, "note", "text"));
				if (map.get("href") != null)
					e = e.withHref(str(map, "note", "href"));
				if (map.get("step") != null)
					e = e.withStep(str(map, "note", "step"));
			}
			case "done" -> e = done(parse(DoneStatus.class, "done", "status", map.get("status")));
			default -> throw iaex("RunEvent ev must be one of step, end, suite, test, replace, note, done; got '%s'", RunViewChecks.clip(evs));
		}
		if (map.get("seq") != null)
			e = e.withSeq(toLong(map, evs, "seq"));
		return e.validate();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static String wireName(Kind k) {
		return k.name().toLowerCase(Locale.ROOT);
	}

	private void requireKind(String member, Kind...allowed) {
		for (var k : allowed)
			if (k == kind)
				return;
		throw iaex("RunEvent %s does not have a '%s' member", wireName(kind), member);
	}

	private static String wire(String ev, String member, Enum<?> value) {
		if (value == null)
			throw iaex("RunEvent %s %s must not be null", ev, member);
		return value.name().toLowerCase(Locale.ROOT);
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String ev, String member, Object value) {
		if (value instanceof String s) {
			for (var c : type.getEnumConstants())
				if (c.name().toLowerCase(Locale.ROOT).equals(s))
					return c;
		}
		throw iaex("RunEvent %s %s must be one of %s; got '%s'", ev, member,
			Arrays.stream(type.getEnumConstants()).map(c -> c.name().toLowerCase(Locale.ROOT)).toList(), value instanceof String s ? RunViewChecks.clip(s) : String.valueOf(value));
	}

	private static String stepId(String ev, String member, String value) {
		if (! RunViewChecks.isStepId(value))
			throw iaex("RunEvent %s %s must match ^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$; got '%s'", ev, member, RunViewChecks.clip(value));
		return value;
	}

	private static String fw(String ev, String value) {
		if (! RunViewChecks.isFw(value))
			throw iaex("RunEvent %s fw must match ^[a-z0-9][a-z0-9._-]{0,31}$; got '%s'", ev, RunViewChecks.clip(value));
		return value;
	}

	private static String text(String ev, String member, String value, int min, int max) {
		if (value == null || value.length() < min || value.length() > max)
			throw iaex("RunEvent %s %s must be %s-%s chars; got %s", ev, member, min, max, value == null ? "null" : value.length() + " chars");
		return value;
	}

	private static int count(String member, int value) {
		if (value < 0 || value > MAX_COUNT)
			throw iaex("RunEvent suite counts.%s must be 0-%s; got '%s'", member, MAX_COUNT, value);
		return value;
	}

	private static String str(Map<?,?> map, String ev, String member) {
		var v = map.get(member);
		if (! (v instanceof String s))
			throw iaex("RunEvent %s %s must be a string; got '%s'", ev, member, v);
		return s;
	}

	private static long toLong(Map<?,?> map, String ev, String member) {
		var key = member.contains(".") ? member.substring(member.indexOf('.') + 1) : member;
		var v = map.get(key);
		if (v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte)
			return ((Number)v).longValue();
		if (v instanceof Number n) {
			var d = n.doubleValue();
			if (d == Math.rint(d) && Math.abs(d) <= MAX_SAFE_INT)
				return (long)d;
		}
		throw iaex("RunEvent %s %s must be an integer; got '%s'", ev, member, v);
	}

	private static int toInt(Map<?,?> map, String ev, String member) {
		var l = toLong(map, ev, member);
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
			throw iaex("RunEvent %s %s must fit in 32 bits; got '%s'", ev, member, l);
		return (int)l;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof RunEvent e && kind == e.kind && seq == e.seq && toContractMap().equals(e.toContractMap());
	}

	@Override /* Object */
	public int hashCode() {
		return Objects.hash(kind, seq, toContractMap());
	}

	@Override /* Object */
	public String toString() {
		return toContractMap().toString();
	}
}
