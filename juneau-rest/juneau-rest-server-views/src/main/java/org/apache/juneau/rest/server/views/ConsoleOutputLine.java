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
import java.time.format.*;
import java.util.*;

/**
 * One record of a console-output region: an OTel-aligned log line with optional display hints.
 *
 * <p>
 * Exactly one of {@link #text} or {@link #frags} is set. Display hints live under {@link #ui}. Endpoints always
 * serialize {@link #toContractMap()}, never this bean, so absent members are omitted rather than written as
 * {@code null}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>log</jv>.append(ConsoleOutputLine.<jsm>info</jsm>(<js>"Compiling 214 sources"</js>));
 * 	<jv>log</jv>.append(ConsoleOutputLine.<jsm>severe</jsm>(<js>"BUILD FAILED"</js>).icon(<js>"cancel"</js>));
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConsoleOutputRegion">Console Output Region</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class ConsoleOutputLine {

	/** Maximum characters of {@link #text} before truncation. */
	public static final int MAX_TEXT_CHARS = 65_536;

	/** Maximum number of fragments. */
	public static final int MAX_FRAGS = 512;

	/** Maximum characters of a block fragment tooltip. */
	public static final int MAX_TOOLTIP_CHARS = 2048;

	/** Maximum characters of a block fragment label. */
	public static final int MAX_LABEL_CHARS = 256;

	/** Maximum image width in CSS pixels. */
	public static final int MAX_IMAGE_WIDTH = 4096;

	/** The wire format of {@link #instant}: always millisecond precision, UTC. */
	public static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	/**
	 * Line severity, mapped to an OpenTelemetry SeverityNumber.
	 */
	public enum Level {
		/** OTel ERROR (17). */
		SEVERE(17, Style.ERROR),
		/** OTel WARN (13). */
		WARNING(13, Style.WARN),
		/** OTel INFO (9). */
		INFO(9, null),
		/** OTel DEBUG (5). */
		FINE(5, Style.MUTED);

		private final int severityNumber;
		private final Style defaultStyle;

		Level(int severityNumber, Style defaultStyle) {
			this.severityNumber = severityNumber;
			this.defaultStyle = defaultStyle;
		}

		/**
		 * The OpenTelemetry SeverityNumber.
		 *
		 * @return 17, 13, 9 or 5.
		 */
		public int severityNumber() {
			return severityNumber;
		}

		/**
		 * The style used when a line sets neither {@code ui.color} nor {@code ui.style}.
		 *
		 * @return The default style, or <jk>null</jk> for the console text colour.
		 */
		public Style defaultStyle() {
			return defaultStyle;
		}

		/**
		 * Maps a {@code java.util.logging} level.
		 *
		 * @param level The JUL level. <jk>null</jk> maps to {@link #INFO}.
		 * @return The console level.
		 */
		public static Level of(java.util.logging.Level level) {
			if (level == null)
				return INFO;
			var v = level.intValue();
			if (v >= java.util.logging.Level.SEVERE.intValue())
				return SEVERE;
			if (v >= java.util.logging.Level.WARNING.intValue())
				return WARNING;
			if (v >= java.util.logging.Level.CONFIG.intValue())
				return INFO;
			return FINE;
		}
	}

	/**
	 * Semantic display style, mapped to theme tokens on the client.
	 */
	public enum Style {
		/** Success tone. */
		SUCCESS,
		/** Warning tone. */
		WARN,
		/** Error tone. */
		ERROR,
		/** Muted text. */
		MUTED,
		/** Accent (info) tone. */
		ACCENT;

		/**
		 * The wire form.
		 *
		 * @return The lower-case name.
		 */
		public String wire() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * An inline image shown below the line text.
	 */
	public static class Image {
		/** Same-origin rooted path. */
		public String src;
		/** Alternative text; {@code ""} only for decorative images. */
		public String alt;
		/** Optional width in CSS pixels, 1 to {@link ConsoleOutputLine#MAX_IMAGE_WIDTH}. */
		public Integer width;

		Image copy() {
			var c = new Image();
			c.src = src;
			c.alt = alt;
			c.width = width;
			return c;
		}

		void validate() {
			if (! isSafeLineImageSrc(src))
				throw iaex("ConsoleOutputLine ui.image.src must be a same-origin rooted path; got '%s'.", clip(src));
			if (alt == null)
				throw iaex("ConsoleOutputLine ui.image.alt is required (use \"\" for a decorative image).");
			if (width != null && (width < 1 || width > MAX_IMAGE_WIDTH))
				throw iaex("ConsoleOutputLine ui.image.width must be 1..%s; got %s.", MAX_IMAGE_WIDTH, width);
		}

		Map<String,Object> toContractMap() {
			var m = new LinkedHashMap<String,Object>();
			m.put("src", src);
			m.put("alt", alt);
			if (width != null)
				m.put("width", width);
			return m;
		}
	}

	/**
	 * Display hints, corresponding to OTel Attributes {@code ui.*}.
	 */
	public static class Ui {
		/** Semantic style. */
		public Style style;
		/** Validated colour; wins over {@link #style}. */
		public String color;
		/** Registered icon name. */
		public String icon;
		/** Inline image. */
		public Image image;
		/** Marks structural chatter that the user may dim or hide. */
		public Boolean marker;

		boolean isEmpty() {
			return style == null && color == null && icon == null && image == null && ! Boolean.TRUE.equals(marker);
		}

		Ui copy() {
			var c = new Ui();
			c.style = style;
			c.color = color;
			c.icon = icon;
			c.image = image == null ? null : image.copy();
			c.marker = marker;
			return c;
		}

		void validate() {
			if (color != null && ! isSafeColor(color))
				throw iaex("ConsoleOutputLine ui.color must be #rgb, #rrggbb or rgb(r,g,b); got '%s'.", clip(color));
			if (icon != null && ! isIconName(icon))
				throw iaex("ConsoleOutputLine ui.icon is not a valid icon name; got '%s'.", clip(icon));
			if (image != null)
				image.validate();
		}

		Map<String,Object> toContractMap() {
			var m = new LinkedHashMap<String,Object>();
			if (style != null)
				m.put("style", style.wire());
			if (color != null)
				m.put("color", color);
			if (icon != null)
				m.put("icon", icon);
			if (image != null)
				m.put("image", image.toContractMap());
			if (Boolean.TRUE.equals(marker))
				m.put("marker", true);
			return m;
		}
	}

	/**
	 * A fragment of a line body: either a text fragment or a block fragment.
	 */
	public static class Frag {
		/** <jk>true</jk> for a block fragment. */
		public boolean block;
		/** Text (text fragments only). */
		public String text;
		/** Semantic style. */
		public Style style;
		/** Validated colour (text colour, or block fill). */
		public String color;
		/** Bold (text fragments only). */
		public Boolean bold;
		/** Tooltip, plain text with {@code \n} (blocks only). */
		public String tooltip;
		/** Fragment or same-origin path link (blocks only). */
		public String href;
		/** Accessible name (blocks only). */
		public String label;

		/**
		 * Creates a text fragment.
		 *
		 * @param text The text.
		 * @return A new fragment.
		 */
		public static Frag text(String text) {
			var f = new Frag();
			f.text = text;
			return f;
		}

		/**
		 * Creates a block fragment.
		 *
		 * @return A new fragment.
		 */
		public static Frag block() {
			var f = new Frag();
			f.block = true;
			return f;
		}

		/** @param value The style. @return This object. */
		public Frag style(Style value) { style = value; return this; }
		/** @param value The colour. @return This object. */
		public Frag color(String value) { color = value; return this; }
		/** @param value Bold. @return This object. */
		public Frag bold(boolean value) { bold = value ? Boolean.TRUE : null; return this; }
		/** @param value The tooltip. @return This object. */
		public Frag tooltip(String value) { tooltip = value; return this; }
		/** @param value The link. @return This object. */
		public Frag href(String value) { href = value; return this; }
		/** @param value The accessible name. @return This object. */
		public Frag label(String value) { label = value; return this; }

		Frag copy() {
			var c = new Frag();
			c.block = block;
			c.text = text;
			c.style = style;
			c.color = color;
			c.bold = bold;
			c.tooltip = tooltip;
			c.href = href;
			c.label = label;
			return c;
		}

		void validate(int i) {
			if (block) {
				if (text != null || bold != null)
					throw iaex("ConsoleOutputLine frags[%s] is a block fragment and must not carry text or bold.", i);
				if (tooltip != null && tooltip.length() > MAX_TOOLTIP_CHARS)
					throw iaex("ConsoleOutputLine frags[%s].tooltip exceeds %s chars.", i, MAX_TOOLTIP_CHARS);
				if (label != null && label.length() > MAX_LABEL_CHARS)
					throw iaex("ConsoleOutputLine frags[%s].label exceeds %s chars.", i, MAX_LABEL_CHARS);
				if (href != null && ! isSafeLineHref(href))
					throw iaex("ConsoleOutputLine frags[%s].href must be a fragment or same-origin path; got '%s'.", i, clip(href));
			} else {
				if (text == null)
					throw iaex("ConsoleOutputLine frags[%s] is a text fragment and needs text.", i);
				if (tooltip != null || href != null || label != null)
					throw iaex("ConsoleOutputLine frags[%s] is a text fragment and must not carry tooltip, href or label.", i);
				text = sanitize(text);
			}
			if (color != null && ! isSafeColor(color))
				throw iaex("ConsoleOutputLine frags[%s].color must be #rgb, #rrggbb or rgb(r,g,b); got '%s'.", i, clip(color));
		}

		Map<String,Object> toContractMap() {
			var m = new LinkedHashMap<String,Object>();
			if (block) {
				m.put("block", true);
			} else {
				m.put("text", text);
			}
			if (style != null)
				m.put("style", style.wire());
			if (color != null)
				m.put("color", color);
			if (Boolean.TRUE.equals(bold))
				m.put("bold", true);
			if (tooltip != null)
				m.put("tooltip", tooltip);
			if (href != null)
				m.put("href", href);
			if (label != null)
				m.put("label", label);
			return m;
		}
	}

	/** Line number, 1-based, server-assigned. */
	public Long n;
	/** Optional timestamp. */
	public Instant instant;
	/** Severity; the factories always set it. */
	public Level level;
	/** Plain text body (exclusive with {@link #frags}). */
	public String text;
	/** Fragment body (exclusive with {@link #text}). */
	public List<Frag> frags;
	/** Display hints. */
	public Ui ui;

	/**
	 * Creates a text line.
	 *
	 * @param level The level.
	 * @param text The text.
	 * @return A new line.
	 */
	public static ConsoleOutputLine of(Level level, String text) {
		var l = new ConsoleOutputLine();
		l.level = level == null ? Level.INFO : level;
		l.text = text;
		return l;
	}

	/** @param text The text. @return A new INFO line. */
	public static ConsoleOutputLine info(String text) { return of(Level.INFO, text); }
	/** @param text The text. @return A new WARNING line. */
	public static ConsoleOutputLine warning(String text) { return of(Level.WARNING, text); }
	/** @param text The text. @return A new SEVERE line. */
	public static ConsoleOutputLine severe(String text) { return of(Level.SEVERE, text); }
	/** @param text The text. @return A new FINE line. */
	public static ConsoleOutputLine fine(String text) { return of(Level.FINE, text); }

	/**
	 * Creates an INFO fragment line.
	 *
	 * @param frags The fragments.
	 * @return A new line.
	 */
	public static ConsoleOutputLine frags(Frag...frags) {
		var l = new ConsoleOutputLine();
		l.level = Level.INFO;
		l.frags = new ArrayList<>(Arrays.asList(frags));
		return l;
	}

	/** @param value The line number. @return This object. */
	public ConsoleOutputLine n(long value) { n = value; return this; }
	/** @param value The instant. @return This object. */
	public ConsoleOutputLine instant(Instant value) { instant = value; return this; }
	/** @param value The level. @return This object. */
	public ConsoleOutputLine level(Level value) { level = value; return this; }
	/** @param value The text. @return This object. */
	public ConsoleOutputLine text(String value) { text = value; return this; }
	/** @param value The style. @return This object. */
	public ConsoleOutputLine style(Style value) { ui().style = value; return this; }
	/** @param value The colour. @return This object. */
	public ConsoleOutputLine color(String value) { ui().color = value; return this; }
	/** @param value The icon name. @return This object. */
	public ConsoleOutputLine icon(String value) { ui().icon = value; return this; }
	/** @param value The marker flag. @return This object. */
	public ConsoleOutputLine marker(boolean value) { ui().marker = value ? Boolean.TRUE : null; return this; }

	/**
	 * Sets an inline image.
	 *
	 * @param src Same-origin rooted path.
	 * @param alt Alternative text.
	 * @return This object.
	 */
	public ConsoleOutputLine image(String src, String alt) {
		var i = new Image();
		i.src = src;
		i.alt = alt;
		ui().image = i;
		return this;
	}

	private Ui ui() {
		if (ui == null)
			ui = new Ui();
		return ui;
	}

	/**
	 * Returns a deep copy.
	 *
	 * @return A new line that shares no mutable state with this one.
	 */
	public ConsoleOutputLine copy() {
		var c = new ConsoleOutputLine();
		c.n = n;
		c.instant = instant;
		c.level = level;
		c.text = text;
		if (frags != null) {
			c.frags = new ArrayList<>(frags.size());
			for (var f : frags)
				c.frags.add(f.copy());
		}
		c.ui = ui == null ? null : ui.copy();
		return c;
	}

	/**
	 * Normalizes and validates this line (fail-closed).
	 *
	 * <p>
	 * Control characters other than tab and newline become U+FFFD, and text over {@link #MAX_TEXT_CHARS} is
	 * truncated with a {@code … [truncated N chars]} suffix. Every other violation throws.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If the line is invalid.
	 */
	public ConsoleOutputLine validate() {
		if (n != null && n < 1)
			throw iaex("ConsoleOutputLine n must be >= 1; got %s.", n);
		if ((text == null) == (frags == null))
			throw iaex("ConsoleOutputLine needs exactly one of text or frags.");
		if (text != null) {
			text = truncate(sanitize(text));
		} else {
			if (frags.isEmpty() || frags.size() > MAX_FRAGS)
				throw iaex("ConsoleOutputLine frags must have 1..512 entries; got %s.", frags.size());
			for (var i = 0; i < frags.size(); i++) {
				var f = frags.get(i);
				if (f == null)
					throw iaex("ConsoleOutputLine frags[%s] is null.", i);
				f.validate(i);
			}
		}
		if (ui != null)
			ui.validate();
		return this;
	}

	/**
	 * Returns the wire form: an ordered map with absent members omitted.
	 *
	 * @return A new map.
	 */
	public Map<String,Object> toContractMap() {
		var m = new LinkedHashMap<String,Object>();
		if (n != null)
			m.put("n", n);
		if (instant != null)
			m.put("instant", INSTANT_FORMAT.format(instant));
		if (level != null)
			m.put("level", level.name());
		if (text != null) {
			m.put("text", text);
		} else if (frags != null) {
			var l = new ArrayList<Object>(frags.size());
			for (var f : frags)
				l.add(f.toContractMap());
			m.put("frags", l);
		}
		if (ui != null && ! ui.isEmpty())
			m.put("ui", ui.toContractMap());
		return m;
	}

	/** Replaces C0 controls other than tab and newline with U+FFFD. */
	static String sanitize(String s) {
		StringBuilder sb = null;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < 0x20 && c != '\t' && c != '\n') {
				if (sb == null)
					sb = new StringBuilder(s);
				sb.setCharAt(i, '\uFFFD');
			}
		}
		return sb == null ? s : sb.toString();
	}

	/** Cut point for {@code s} at {@code max} chars that never splits a surrogate pair. */
	static int cutPoint(String s, int max) {
		var cut = max;
		if (cut > 0 && Character.isHighSurrogate(s.charAt(cut - 1)))
			cut--;
		return cut;
	}

	/** The truncation suffix for {@code dropped} chars. */
	static String truncationSuffix(int dropped) {
		return "… [truncated " + dropped + " chars]";
	}

	private static String truncate(String s) {
		if (s.length() <= MAX_TEXT_CHARS)
			return s;
		var cut = cutPoint(s, MAX_TEXT_CHARS);
		return s.substring(0, cut) + truncationSuffix(s.length() - cut);
	}

	/**
	 * Converts one raw line that may contain ANSI escape sequences into an {@link Level#INFO INFO} line.
	 *
	 * <p>
	 * Uses a fresh {@link AnsiDecoder}, so no SGR state carries between calls. Never throws.
	 *
	 * @param raw The raw line. <jk>null</jk> is treated as {@code ""}.
	 * @return A validated line.
	 */
	public static ConsoleOutputLine fromAnsi(String raw) {
		return fromAnsi(Level.INFO, raw);
	}

	/**
	 * Converts one raw line that may contain ANSI escape sequences.
	 *
	 * @param level The level. <jk>null</jk> means {@link Level#INFO}.
	 * @param raw The raw line. <jk>null</jk> is treated as {@code ""}.
	 * @return A validated line.
	 */
	public static ConsoleOutputLine fromAnsi(Level level, String raw) {
		return new AnsiDecoder().line(level, raw);
	}

	/**
	 * A stateful converter from ANSI SGR styling to fragments.
	 *
	 * <p>
	 * SGR state carries across {@link #line(Level, String)} calls the way it does in a terminal. One instance
	 * belongs to one stream. Not thread-safe.
	 */
	public static final class AnsiDecoder {

		private static final int MAX_PARAMS = 32;
		private static final int MAX_PARAM = 65_535;
		private static final int[] CUBE = {0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff};
		private static final double MIN_LUMINANCE = 0.05;
		private static final double MAX_LUMINANCE = 0.85;

		private Style fgStyle;
		private String fgColor;
		private boolean bold;
		private boolean dim;

		private final List<Frag> runs = new ArrayList<>();
		private StringBuilder cur;
		private Style curStyle;
		private String curColor;
		private boolean curBold;

		/**
		 * Clears all SGR state.
		 *
		 * @return This object.
		 */
		public AnsiDecoder reset() {
			fgStyle = null;
			fgColor = null;
			bold = false;
			dim = false;
			return this;
		}

		/**
		 * Converts one raw line, carrying SGR state from previous calls.
		 *
		 * @param level The level. <jk>null</jk> means {@link Level#INFO}.
		 * @param raw The raw line. <jk>null</jk> is treated as {@code ""}.
		 * @return A validated line. Never throws.
		 */
		public ConsoleOutputLine line(Level level, String raw) {
			var s = raw == null ? "" : raw;
			var end = s.length();
			while (end > 0 && s.charAt(end - 1) == '\r')
				end--;
			s = s.substring(0, end);
			runs.clear();
			cur = null;
			var cr = s.lastIndexOf('\r');
			if (cr >= 0) {
				scan(s.substring(0, cr), false);
				s = s.substring(cr + 1);
			}
			scan(s, true);
			flush();
			return build(level == null ? Level.INFO : level);
		}

		//--------------------------------------------------------------------------------------------------------------
		// Tokeniser
		//--------------------------------------------------------------------------------------------------------------

		private void scan(String s, boolean emit) {
			var n = s.length();
			var i = 0;
			while (i < n) {
				var c = s.charAt(i);
				if (c == 0x1b) {
					if (i + 1 >= n)
						return;
					var d = s.charAt(i + 1);
					if (d == '[') {
						i = csi(s, i + 2);
					} else if (d == ']') {
						i = stringSequence(s, i + 2, true);
					} else if (d == 'P' || d == 'X' || d == '^' || d == '_') {
						i = stringSequence(s, i + 2, false);
					} else {
						var j = i + 1;
						while (j < n && s.charAt(j) >= 0x20 && s.charAt(j) <= 0x2f)
							j++;
						if (j >= n)
							return;
						var f = s.charAt(j);
						i = (f >= 0x30 && f <= 0x7e) ? j + 1 : j;
					}
					continue;
				}
				if (c == '\u009b') {
					i = csi(s, i + 1);
					continue;
				}
				if (emit)
					emit(c);
				i++;
			}
		}

		/** Parses a CSI starting at its first parameter byte; returns the index to resume at. */
		private int csi(String s, int start) {
			var n = s.length();
			var j = start;
			while (j < n && s.charAt(j) >= 0x30 && s.charAt(j) <= 0x3f)
				j++;
			var paramEnd = j;
			while (j < n && s.charAt(j) >= 0x20 && s.charAt(j) <= 0x2f)
				j++;
			if (j >= n)
				return n;
			var f = s.charAt(j);
			if (f < 0x40 || f > 0x7e)
				return j;
			if (f == 'm' && j == paramEnd)
				sgr(s.substring(start, paramEnd));
			return j + 1;
		}

		/** Skips an OSC/DCS/SOS/PM/APC payload; returns the index after its terminator, or the end. */
		private static int stringSequence(String s, int start, boolean belTerminates) {
			var n = s.length();
			for (var j = start; j < n; j++) {
				var c = s.charAt(j);
				if (belTerminates && c == 0x07)
					return j + 1;
				if (c == 0x1b && j + 1 < n && s.charAt(j + 1) == '\\')
					return j + 2;
			}
			return n;
		}

		//--------------------------------------------------------------------------------------------------------------
		// SGR
		//--------------------------------------------------------------------------------------------------------------

		private void sgr(String params) {
			if (params.isEmpty()) {
				reset();
				return;
			}
			for (var k = 0; k < params.length(); k++) {
				var ch = params.charAt(k);
				if (ch != ';' && (ch < '0' || ch > '9'))
					return;
			}
			var parts = params.split(";", -1);
			if (parts.length > MAX_PARAMS)
				return;
			var p = new int[parts.length];
			for (var k = 0; k < parts.length; k++) {
				if (parts[k].isEmpty())
					continue;
				if (parts[k].length() > 9)
					return;
				var v = Long.parseLong(parts[k]);
				if (v > MAX_PARAM)
					return;
				p[k] = (int)v;
			}
			for (var k = 0; k < p.length; k++) {
				var c = p[k];
				if (c == 38 || c == 48) {
					if (k + 1 >= p.length)
						return;
					var mode = p[k + 1];
					var need = mode == 5 ? 1 : mode == 2 ? 3 : 0;
					if (need == 0) {
						k++;
						continue;
					}
					if (p.length - (k + 2) < need)
						return;
					var bad = false;
					for (var q = k + 2; q < k + 2 + need; q++)
						bad |= p[q] > 255;
					if (! bad && c == 38) {
						if (mode == 5)
							indexed(p[k + 2]);
						else
							computed(p[k + 2], p[k + 3], p[k + 4]);
					}
					k += 1 + need;
					continue;
				}
				apply(c);
			}
		}

		private void apply(int c) {
			if (c == 0) {
				reset();
			} else if (c == 1) {
				bold = true;
			} else if (c == 2) {
				dim = true;
			} else if (c == 22) {
				bold = false;
				dim = false;
			} else if (c == 39) {
				foreground(null, null);
			} else if ((c >= 30 && c <= 37) || (c >= 90 && c <= 97)) {
				base(c);
			}
			// Backgrounds (40-47, 49, 100-107) and every other code are consumed and ignored.
		}

		private void base(int c) {
			switch (c) {
				case 31, 91 -> foreground(Style.ERROR, null);
				case 32, 92 -> foreground(Style.SUCCESS, null);
				case 33, 93 -> foreground(Style.WARN, null);
				case 34, 94 -> foreground(Style.ACCENT, null);
				case 90 -> foreground(Style.MUTED, null);
				case 35 -> foreground(null, "#8a2fa0");
				case 95 -> foreground(null, "#a347ba");
				case 36 -> foreground(null, "#0e6f7d");
				case 96 -> foreground(null, "#117a89");
				default -> foreground(null, null); // 30, 37, 97: invisible on some theme, so dropped.
			}
		}

		private void indexed(int n) {
			if (n < 8) {
				base(30 + n);
			} else if (n < 16) {
				base(90 + n - 8);
			} else if (n < 232) {
				var i = n - 16;
				computed(CUBE[i / 36], CUBE[(i / 6) % 6], CUBE[i % 6]);
			} else {
				var g = 8 + 10 * (n - 232);
				computed(g, g, g);
			}
		}

		private void computed(int r, int g, int b) {
			var l = luminance(r, g, b);
			if (l < MIN_LUMINANCE || l > MAX_LUMINANCE)
				foreground(null, null);
			else
				foreground(null, String.format("#%02x%02x%02x", r, g, b));
		}

		private void foreground(Style style, String color) {
			fgStyle = style;
			fgColor = color;
		}

		/** WCAG relative luminance of an sRGB colour. */
		static double luminance(int r, int g, int b) {
			return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
		}

		private static double channel(int c) {
			var s = c / 255.0;
			return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
		}

		//--------------------------------------------------------------------------------------------------------------
		// Runs and assembly
		//--------------------------------------------------------------------------------------------------------------

		private void emit(char c) {
			var style = fgColor != null ? null : fgStyle != null ? fgStyle : dim ? Style.MUTED : null;
			if (cur == null || style != curStyle || ! Objects.equals(fgColor, curColor) || bold != curBold) {
				flush();
				cur = new StringBuilder();
				curStyle = style;
				curColor = fgColor;
				curBold = bold;
			}
			cur.append(c < 0x20 && c != '\t' && c != '\n' ? '\uFFFD' : c);
		}

		private void flush() {
			if (cur == null || cur.length() == 0)
				return;
			var f = Frag.text(cur.toString()).style(curStyle).color(curColor).bold(curBold);
			runs.add(f);
			cur = null;
		}

		private static boolean isPlain(Frag f) {
			return f.style == null && f.color == null && f.bold == null;
		}

		private ConsoleOutputLine build(Level level) {
			if (runs.isEmpty())
				return of(level, "").validate();
			if (runs.size() == 1 && isPlain(runs.get(0)))
				return of(level, runs.get(0).text).validate();

			var kept = new ArrayList<Frag>(Math.min(runs.size(), MAX_FRAGS + 1));
			var total = 0;
			var dropped = 0;
			var cut = false;
			for (var f : runs) {
				var t = f.text;
				if (cut) {
					dropped += t.length();
					continue;
				}
				if (total + t.length() <= MAX_TEXT_CHARS) {
					kept.add(f);
					total += t.length();
					continue;
				}
				var keep = cutPoint(t, MAX_TEXT_CHARS - total);
				if (keep > 0) {
					f.text = t.substring(0, keep);
					kept.add(f);
				}
				dropped += t.length() - keep;
				cut = true;
			}
			var suffix = dropped > 0 ? Frag.text(truncationSuffix(dropped)).style(Style.MUTED) : null;

			if (kept.size() + (suffix == null ? 0 : 1) > MAX_FRAGS) {
				var keepIdx = MAX_FRAGS - 2;
				var tail = kept.get(keepIdx);
				var sb = new StringBuilder(tail.text);
				for (var i = keepIdx + 1; i < kept.size(); i++)
					sb.append(kept.get(i).text);
				tail.text = sb.toString();
				kept = new ArrayList<>(kept.subList(0, keepIdx + 1));
			}
			if (suffix != null)
				kept.add(suffix);

			var line = new ConsoleOutputLine();
			line.level = level;
			line.frags = kept;
			return line.validate();
		}
	}
}
