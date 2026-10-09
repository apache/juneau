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
package org.apache.juneau.rest.server.bus;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.marshaller.*;

/**
 * The bus wire format, version 1 (spec §11.4): one JSON object per frame, in a canonical field order shared with the
 * browser codec and pinned by {@code bus-frames-corpus.json}.
 *
 * <p>
 * The SSE transport writes each frame as {@code event: bus}, {@code id: <seq>}, {@code data: <frame>}; the WebSocket
 * transport sends one frame per text message.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   String <jv>f</jv> = BusFrames.<jsm>pub</jsm>(<js>"ops.jobs"</js>, Json.<jsm>of</jsm>(<jv>payload</jv>), <jk>true</jk>, 7);
 *   <jc>// {"v":1,"type":"pub","topic":"ops.jobs","payload":{...},"retained":true,"seq":7}</jc>
 *
 *   JsonMap <jv>up</jv> = BusFrames.<jsm>decode</jsm>(<jv>text</jv>);   <jc>// BusRefusal bus:bad-frame when malformed</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class BusFrames {

	/** The wire format version. */
	public static final int VERSION = 1;

	/** The SSE event name carrying frames. */
	public static final String EVENT = "bus";

	/** The frame types of version 1. */
	public static final Set<String> TYPES = Set.of("pub", "clear", "resync-begin", "resync-end", "ping", "error");

	/** The largest {@code seq} both codecs represent exactly: {@code Number.MAX_SAFE_INTEGER}. */
	private static final long MAX_SAFE_SEQ = 9007199254740991L;

	private static final Pattern TRAILING_SEQ = Pattern.compile("\"seq\":(\\d+)}$");

	private BusFrames() {}

	/**
	 * A {@code pub} frame.
	 *
	 * @param topic The topic.
	 * @param payloadJson The payload, already serialized as JSON.
	 * @param retained Whether the value is retained.
	 * @param seq The session sequence number.
	 * @return The encoded frame.
	 */
	public static String pub(String topic, String payloadJson, boolean retained, long seq) {
		return pubRaw(topic, payloadJson, retained, seq);
	}

	/**
	 * A {@code clear} frame.
	 *
	 * @param topic The retained topic being cleared.
	 * @param seq The session sequence number.
	 * @return The encoded frame.
	 */
	public static String clear(String topic, long seq) {
		return head("clear").append(",\"topic\":").append(quote(topic)).append(",\"seq\":").append(seq).append('}').toString();
	}

	/** @param seq The session sequence number. @return The encoded {@code resync-begin} frame. */
	public static String resyncBegin(long seq) {
		return head("resync-begin").append(",\"seq\":").append(seq).append('}').toString();
	}

	/** @param seq The session sequence number. @return The encoded {@code resync-end} frame. */
	public static String resyncEnd(long seq) {
		return head("resync-end").append(",\"seq\":").append(seq).append('}').toString();
	}

	/** @return The encoded {@code ping} frame. */
	public static String ping() {
		return head("ping").append('}').toString();
	}

	/**
	 * An {@code error} frame.
	 *
	 * @param code The {@code bus:*} code.
	 * @param message Fixed server text.
	 * @param topic The topic concerned.  Can be <jk>null</jk>.
	 * @return The encoded frame.
	 */
	public static String error(String code, String message, String topic) {
		var sb = head("error").append(",\"code\":").append(quote(code)).append(",\"message\":").append(quote(message));
		if (topic != null)
			sb.append(",\"topic\":").append(quote(topic));
		return sb.append('}').toString();
	}

	/**
	 * Encodes a frame given as a map, in canonical field order, omitting absent optional fields.
	 *
	 * @param frame The frame ({@code type} required; {@code v} is always written as {@code 1}).
	 * @return The encoded frame.
	 * @throws IllegalArgumentException On an unknown {@code type}, or when {@code topic}, {@code code} or {@code message}
	 * 	is missing or not a string, or {@code retained} is not a boolean.
	 */
	public static String encode(Map<String,?> frame) {
		var type = String.valueOf(frame.get("type"));
		return switch (type) {
			case "pub" -> pubRaw(requireString(frame, "topic"), Json.of(frame.get("payload")), optionalBoolean(frame, "retained"),
				seqOf(frame.get("seq")));
			case "clear" -> {
				var sb = head("clear").append(",\"topic\":").append(quote(requireString(frame, "topic")));
				var seq = seqOf(frame.get("seq"));
				if (seq != null)
					sb.append(",\"seq\":").append(seq);
				yield sb.append('}').toString();
			}
			case "resync-begin", "resync-end" -> {
				var sb = head(type);
				var seq = seqOf(frame.get("seq"));
				if (seq != null)
					sb.append(",\"seq\":").append(seq);
				yield sb.append('}').toString();
			}
			case "ping" -> ping();
			case "error" -> error(requireString(frame, "code"), requireString(frame, "message"), optionalString(frame, "topic"));
			default -> throw new IllegalArgumentException("BusFrames.encode: unknown frame type '" + type + "'");
		};
	}

	/**
	 * Decodes and validates one frame.  The result holds only the known fields of its type, in canonical order.
	 *
	 * @param text The frame text (strict RFC 8259 JSON).
	 * @return The frame.
	 * @throws BusRefusal {@code bus:bad-frame} when the text is not a valid version-1 frame.
	 */
	public static JsonMap decode(String text) throws BusRefusal {
		JsonMap m;
		try {
			m = text == null || ! isSingleObject(text) ? null : JsonMap.ofString(text, JsonParser.DEFAULT);
		} catch (RuntimeException e) {
			throw BusRefusal.badFrame("frame is not a JSON object");
		}
		if (m == null)
			throw BusRefusal.badFrame("frame is not a JSON object");
		if (! (m.get("v") instanceof Number v) || v.doubleValue() != VERSION)
			throw BusRefusal.badFrame("unsupported frame version");
		if (! (m.get("type") instanceof String type) || ! TYPES.contains(type))
			throw BusRefusal.badFrame("unknown frame type");
		var out = new JsonMap();
		out.put("v", VERSION);
		out.put("type", type);
		switch (type) {
			case "pub" -> {
				requireTopic(m, out);
				if (! m.containsKey("payload"))
					throw BusRefusal.badFrame("pub frame has no payload");
				out.put("payload", m.get("payload"));
				if (m.containsKey("retained")) {
					if (! (m.get("retained") instanceof Boolean))
						throw BusRefusal.badFrame("retained must be a boolean");
					out.put("retained", m.get("retained"));
				}
				optionalSeq(m, out);
			}
			case "clear" -> {
				requireTopic(m, out);
				optionalSeq(m, out);
			}
			case "resync-begin", "resync-end" -> optionalSeq(m, out);
			case "error" -> {
				if (! (m.get("code") instanceof String code))
					throw BusRefusal.badFrame("error frame has no code");
				if (! (m.get("message") instanceof String message))
					throw BusRefusal.badFrame("error frame has no message");
				out.put("code", code);
				out.put("message", message);
				if (m.get("topic") instanceof String topic)
					out.put("topic", topic);
			}
			default -> { /* ping: no fields */ }
		}
		return out;
	}

	/**
	 * Extracts the {@code seq} of a frame produced by this class (it is always the last field when present).
	 *
	 * @param encoded An encoded frame.
	 * @return The sequence number, or empty for {@code ping} and {@code error}.
	 */
	public static OptionalLong seq(String encoded) {
		var m = TRAILING_SEQ.matcher(encoded);
		return m.find() ? OptionalLong.of(Long.parseLong(m.group(1))) : OptionalLong.empty();
	}

	/**
	 * The UTF-8 length of a string, without allocating its bytes.
	 *
	 * @param s The string.
	 * @return The number of UTF-8 bytes.
	 */
	public static int utf8Length(String s) {
		var n = 0;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < 0x80)
				n++;
			else if (c < 0x800)
				n += 2;
			else if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
				n += 4;
				i++;
			} else
				n += 3;
		}
		return n;
	}

	/**
	 * Quotes a string exactly as JavaScript's {@code JSON.stringify} does (lowercase {@code \\u} hex, lone surrogates
	 * escaped, solidus not escaped).
	 *
	 * @param s The string.
	 * @return The JSON string literal.
	 */
	static String quote(String s) {
		var sb = new StringBuilder(s.length() + 2).append('"');
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				case '\b' -> sb.append("\\b");
				case '\f' -> sb.append("\\f");
				default -> {
					if (c < 0x20)
						sb.append(String.format("\\u%04x", (int) c));
					else if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
						sb.append(c).append(s.charAt(i + 1));
						i++;
					} else if (Character.isSurrogate(c))
						sb.append(String.format("\\u%04x", (int) c));
					else
						sb.append(c);
				}
			}
		}
		return sb.append('"').toString();
	}

	/**
	 * Whether the text is one JSON object optionally surrounded by whitespace.  {@link JsonParser} stops after the first
	 * value and ignores whatever follows, but a frame is the whole text.  String contents are skipped so a brace inside
	 * a string does not end the object early; well-formedness itself is left to the parser.
	 */
	private static boolean isSingleObject(String text) {
		var n = text.length();
		var i = 0;
		while (i < n && isWhitespace(text.charAt(i)))
			i++;
		if (i == n || text.charAt(i) != '{')
			return false;
		var depth = 0;
		for (; i < n; i++) {
			var c = text.charAt(i);
			if (c == '"') {
				for (i++; i < n && text.charAt(i) != '"'; i++)
					if (text.charAt(i) == '\\')
						i++;
			} else if (c == '{' || c == '[') {
				depth++;
			} else if (c == '}' || c == ']') {
				if (--depth == 0) {
					for (i++; i < n; i++)
						if (! isWhitespace(text.charAt(i)))
							return false;
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r';
	}

	private static StringBuilder head(String type) {
		return new StringBuilder(64).append("{\"v\":").append(VERSION).append(",\"type\":").append(quote(type));
	}

	private static String pubRaw(String topic, String payloadJson, Boolean retained, Long seq) {
		var sb = head("pub").append(",\"topic\":").append(quote(topic)).append(",\"payload\":").append(payloadJson);
		if (retained != null)
			sb.append(",\"retained\":").append(retained);
		if (seq != null)
			sb.append(",\"seq\":").append(seq);
		return sb.append('}').toString();
	}

	private static void requireTopic(JsonMap in, JsonMap out) throws BusRefusal {
		if (! (in.get("topic") instanceof String topic) || topic.isEmpty())
			throw BusRefusal.badFrame("frame has no topic");
		out.put("topic", topic);
	}

	private static void optionalSeq(JsonMap in, JsonMap out) throws BusRefusal {
		if (! in.containsKey("seq"))
			return;
		var seq = seqOf(in.get("seq"));
		if (seq == null)
			throw BusRefusal.badFrame("seq must be a non-negative integer");
		out.put("seq", seq);
	}

	private static Long seqOf(Object o) {
		if (! (o instanceof Number n))
			return null;
		var d = n.doubleValue();
		if (d < 0 || d > MAX_SAFE_SEQ || d != Math.floor(d))
			return null;
		return n.longValue();
	}

	private static String requireString(Map<String,?> frame, String key) {
		if (! (frame.get(key) instanceof String s))
			throw new IllegalArgumentException("BusFrames.encode: '" + key + "' must be a string");
		return s;
	}

	private static String optionalString(Map<String,?> frame, String key) {
		return frame.get(key) == null ? null : requireString(frame, key);
	}

	private static Boolean optionalBoolean(Map<String,?> frame, String key) {
		var o = frame.get(key);
		if (o != null && ! (o instanceof Boolean))
			throw new IllegalArgumentException("BusFrames.encode: '" + key + "' must be a boolean");
		return (Boolean) o;
	}
}
