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
package org.apache.juneau.rest.server.terminal.bus;

import static java.nio.charset.StandardCharsets.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.terminal.*;

/**
 * The {@code term.input} upstream handler: hands keystrokes from a browser terminal card to a {@link TerminalSink}.
 *
 * <p>
 * Wiring.  The handler answers through the bus, but the bus is built from the policy that holds the handler, so the
 * bus is bound afterwards, once:
 * <p class='bjava'>
 * 	TerminalInput <jv>input</jv> = TerminalInput.<jsm>create</jsm>(<jv>access</jv>);
 * 	ServerBus <jv>bus</jv> = ServerBus.<jsm>create</jsm>(BusPolicy.<jsm>create</jsm>()
 * 		.upstream(TerminalInput.<jsf>TOPIC</jsf>, <jv>input</jv>)
 * 		.downstream(TerminalInput.<jsf>STATUS_TOPIC</jsf>, <jk>false</jk>)
 * 		.build());
 * 	<jv>input</jv>.bind(<jv>bus</jv>);
 * </p>
 *
 * <p>
 * A frame's payload is <c>{"id": &lt;terminal id&gt;, "data": &lt;string&gt;}</c>; an empty {@code data} is a probe.
 * Every probe, and every refused data frame, is answered on {@link #STATUS_TOPIC} to the sending session only, with
 * <c>{"id", "state"}</c>, where the state is {@code live}, {@code denied}, {@code closed}, {@code busy} or {@code bad}.
 * An accepted data frame gets no answer.  The id is echoed only when it is a valid terminal id; otherwise the answer has
 * no {@code id} key.
 *
 * <p>
 * Typing needs both {@link TerminalAccess#canWatch(TerminalCaller, String)} and a present
 * {@link TerminalAccess#inputSink(TerminalCaller, String)}.  The bus's {@code maxUpstreamPerSecond} policy (20 by
 * default) bounds the frame rate per session, which is why the client batches keystrokes.
 *
 * <p>
 * Each accepted non-empty write is audited at INFO on the {@code juneau.terminal.input} logger as
 * <c>principal=&lt;name or -&gt; terminal=&lt;id&gt; bytes=&lt;n&gt; keys=&lt;list&gt;</c>, where the list names the
 * control keys present (for example <c>keys=^C,ESC,CR</c>), comma-separated, in order of first appearance, at most 8.
 * The typed content is never logged.  Refusals log at FINE with the state only.
 */
public final class TerminalInput implements UpstreamHandler {

	/** The upstream topic carrying keystrokes. */
	public static final String TOPIC = "term.input";

	/** The downstream topic carrying the per-session answers.  Never retained. */
	public static final String STATUS_TOPIC = "term.input-status";

	private static final Logger AUDIT = Logger.getLogger("juneau.terminal.input");
	private static final int MAX_KEYS = 8;
	private static final Set<String> KEYS = Set.of("id", "data");

	private final TerminalAccess access;
	private final AtomicReference<ServerBus> bus = new AtomicReference<>();

	private TerminalInput(TerminalAccess access) {
		this.access = access;
	}

	/**
	 * Creates a handler.
	 *
	 * @param access Who may watch and who may type.  Must not be <jk>null</jk>.
	 * @return A new, unbound handler.
	 * @throws IllegalArgumentException If {@code access} is <jk>null</jk>.
	 */
	public static TerminalInput create(TerminalAccess access) {
		if (access == null)
			throw new IllegalArgumentException("TerminalInput.create: access must not be null");
		return new TerminalInput(access);
	}

	/**
	 * Binds the bus the answers are published on.  May be called once.
	 *
	 * @param bus The bus whose policy holds this handler.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code bus} is <jk>null</jk>.
	 * @throws IllegalStateException If a bus is already bound.
	 */
	public TerminalInput bind(ServerBus bus) {
		if (bus == null)
			throw new IllegalArgumentException("TerminalInput.bind: bus must not be null");
		if (! this.bus.compareAndSet(null, bus))
			throw new IllegalStateException("TerminalInput.bind: already bound");
		return this;
	}

	/**
	 * Handles one {@code term.input} frame.
	 *
	 * @param session The sending session.  Answers go to it only.
	 * @param topic The topic, {@link #TOPIC}.
	 * @param payload The frame payload, <c>{"id", "data"}</c>.
	 * @throws IllegalStateException If no bus is bound.
	 * @throws Exception Whatever the sink's {@code write} throws; the bus answers {@code bus:upstream-failed}.
	 */
	@Override /* UpstreamHandler */
	public void handle(BusSession session, String topic, JsonMap payload) throws Exception {
		var b = bus.get();
		if (b == null)
			throw new IllegalStateException("TerminalInput: handle called before bind");
		var caller = new SessionCaller(session);

		var rawId = payload.get("id");
		var id = rawId instanceof String s && RunViewChecks.isRunId(s) ? s : null;
		if (id == null || ! (payload.get("data") instanceof String data) || ! KEYS.equals(payload.keySet())) {
			refuse(b, session, caller, id, "bad");
			return;
		}

		var sink = access.canWatch(caller, id) ? access.inputSink(caller, id).orElse(null) : null;
		if (sink == null) {
			refuse(b, session, caller, id, "denied");
			return;
		}
		if (! sink.acceptsInput()) {
			refuse(b, session, caller, id, "closed");
			return;
		}
		if (data.isEmpty()) {
			answer(b, session, id, "live");
			return;
		}
		var bytes = data.getBytes(UTF_8);
		if (! sink.write(bytes)) {
			// A refused write is "busy" unless the process stopped accepting input since the check above.
			refuse(b, session, caller, id, sink.acceptsInput() ? "busy" : "closed");
			return;
		}
		AUDIT.info(() -> String.format(Locale.ROOT, "principal=%s terminal=%s bytes=%d keys=%s", principal(caller), id, bytes.length, keys(bytes)));
	}

	private static void refuse(ServerBus b, BusSession session, TerminalCaller caller, String id, String state) {
		AUDIT.fine(() -> String.format(Locale.ROOT, "principal=%s terminal=%s state=%s", principal(caller), id == null ? "-" : id, state));
		answer(b, session, id, state);
	}

	private static void answer(ServerBus b, BusSession session, String id, String state) {
		// The bus serializer writes null map values, so an id that failed the grammar is left out rather than nulled.
		var payload = id == null ? JsonMap.of("state", state) : JsonMap.of("id", id, "state", state);
		b.publishTo(s -> s == session, STATUS_TOPIC, payload);
	}

	/**
	 * The principal for an audit record: {@code -} when anonymous, otherwise the name with every character
	 * {@code <= 0x20}, every ISO control (0x7F, 0x80-0x9F), U+2028, U+2029, {@code '='}, {@code '\'} and {@code '%'}
	 * percent-encoded as its UTF-8 bytes ({@code %XX} per byte, upper-case hex; for example U+2028 is {@code %E2%80%A8}),
	 * so a name can neither split the record nor forge a {@code key=value} field.
	 */
	private static String principal(TerminalCaller c) {
		var name = c.principalName();
		if (name == null)
			return "-";
		var sb = new StringBuilder(name.length());
		for (var i = 0; i < name.length(); i++) {
			var ch = name.charAt(i);
			if (ch <= 0x20 || Character.isISOControl(ch) || ch == '\u2028' || ch == '\u2029' || ch == '=' || ch == '\\' || ch == '%')
				for (var x : String.valueOf(ch).getBytes(UTF_8))
					sb.append('%').append(String.format(Locale.ROOT, "%02X", x & 0xFF));
			else
				sb.append(ch);
		}
		return sb.toString();
	}

	/** The control keys present, comma-separated, in order of first appearance, at most {@value #MAX_KEYS}. */
	private static String keys(byte[] bytes) {
		var names = new LinkedHashSet<String>();
		for (var x : bytes) {
			var name = keyName(x & 0xFF);
			if (name != null) {
				names.add(name);
				if (names.size() == MAX_KEYS)
					break;
			}
		}
		return String.join(",", names);
	}

	/**
	 * The audit name of a control byte, or <jk>null</jk> for any other byte.
	 *
	 * <p>
	 * {@code ^C}, {@code ^D}, {@code ^Z}, {@code ESC}, {@code CR}, {@code TAB} and {@code BS} (0x08 and 0x7F) are named;
	 * every other C0 byte uses standard caret notation ({@code b + 0x40}): {@code ^A}..{@code ^Y} for the letters,
	 * {@code ^J} for LF, and {@code ^@}, {@code ^\}, {@code ^]}, {@code ^^}, {@code ^_} for 0x00 and 0x1C-0x1F.
	 */
	private static String keyName(int b) {
		return switch (b) {
			case 0x03 -> "^C";
			case 0x04 -> "^D";
			case 0x1A -> "^Z";
			case 0x1B -> "ESC";
			case 0x0D -> "CR";
			case 0x09 -> "TAB";
			case 0x08 -> "BS";
			case 0x7F -> "BS";  // xterm's Backspace sends DEL (0x7F) by default.
			default -> b < 0x20 ? "^" + (char)(b + 0x40) : null;
		};
	}
}
