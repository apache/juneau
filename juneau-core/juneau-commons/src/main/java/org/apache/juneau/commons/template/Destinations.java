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
package org.apache.juneau.commons.template;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.svl.functions.*;

/**
 * Destination presets for {@link TemplateResolver.Builder#destination(Destination)}.
 *
 * <p>
 * Each preset pairs a value encoder (applied to resolved values, never to template text) with a {@link StructureRenderer}
 * that encodes every item and cell itself.  Only generic formats are offered; define a {@link Destination} for anything
 * else.
 *
 * <ul>
 * 	<li>{@link #HTML}: for text nodes and quoted attribute values only; it does not make a URL attribute safe (use
 * 		{@link #URL_COMPONENT} and a scheme allowlist for <c>href</c>/<c>src</c>).  Escapes <c>&amp; &lt; &gt; " '</c>; lists, blocks and tables become <c>ul</c>, <c>ol</c>,
 * 		<c>blockquote</c> and <c>table</c> elements.
 * 	<li>{@link #MARKDOWN}: backslash-escapes characters Markdown would interpret and flattens whitespace so a value
 * 		cannot start a new block; lists, quotes and pipe tables.  Bare URLs are not auto-linked (the <c>:</c> of
 * 		<c>://</c> and every <c>.</c> are escaped).
 * 	<li>{@link #PLAIN_TEXT}: drops control characters other than newline and tab; <c>- </c>, <c>N. </c> and <c>&gt; </c>
 * 		markers and column-aligned tables.
 * 	<li>{@link #JSON_STRING}: escapes for the inside of a JSON string literal.
 * 	<li>{@link #URL_COMPONENT}: percent-encodes for a URL path segment or query component.
 * </ul>
 */
@SuppressWarnings({
	"java:S127", // The markdown encoder skips ahead past a digit run by assigning the loop counter
	"java:S135", // Encoder loops use continue to skip dropped characters
	"java:S3776" // Encoders and renderers dispatch over every structure kind in one place
})
public final class Destinations {

	/** HTML. */
	public static final Destination HTML = new Destination("html", Destinations::htmlEncode, Destinations::renderHtml);

	/** Markdown. */
	public static final Destination MARKDOWN = new Destination("markdown", Destinations::markdownEncode, Destinations::renderMarkdown);

	/** Plain text. */
	public static final Destination PLAIN_TEXT = new Destination("plain-text", Destinations::plainEncode, Destinations::renderPlain);

	/** The inside of a JSON string literal (no surrounding quotes). */
	public static final Destination JSON_STRING = new Destination("json-string", Destinations::jsonEncode, (s, t) -> jsonEncode(renderPlain(s, t)));

	/** A URL path segment or query component. */
	public static final Destination URL_COMPONENT = new Destination("url-component", Destinations::urlEncode, (s, t) -> urlEncode(renderPlain(s, t)));

	private static final EncodingFunctions.HtmlEscape HTML_ESCAPE = new EncodingFunctions.HtmlEscape();
	private static final EncodingFunctions.UrlEncode URL_ENCODE = new EncodingFunctions.UrlEncode();

	private Destinations() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Encoders
	//-----------------------------------------------------------------------------------------------------------------

	private static String htmlEncode(String s) {
		return HTML_ESCAPE.invoke(s);
	}

	private static String urlEncode(String s) {
		// URLEncoder writes a space as '+' (and a literal '+' as %2B); a component wants %20.
		return URL_ENCODE.invoke(s).replace("+", "%20");
	}

	private static String plainEncode(String s) {
		var sb = new StringBuilder(s.length());
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\n' || c == '\t' || (c >= 0x20 && c != 0x7f))
				sb.append(c);
		}
		return sb.toString();
	}

	private static String jsonEncode(String s) {
		var sb = new StringBuilder(s.length() + 8);
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
					if (c < 0x20 || c == 0x7f || c == 0x2028 || c == 0x2029)
						sb.append(String.format("\\u%04x", (int)c));
					else
						sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes text for Markdown.
	 *
	 * <p>
	 * Whitespace runs (newlines included) collapse to one space and other control characters are dropped, so a value cannot
	 * start a new block or an indented code block.  The characters <c>\ ` * _ [ ] &lt; &gt; | ~ &amp;</c> are always
	 * backslash-escaped, as are <c>{ } ( ) ! .</c> and the colon of <c>://</c> (so a bare URL or address is not
	 * auto-linked and kramdown/MDX attribute braces are inert).  The first visible character is also escaped when it
	 * could open a block: <c># + - =</c>, and a digit run followed by <c>)</c>.
	 */
	private static String markdownEncode(String s) {
		var sb = new StringBuilder(s.length() + 8);
		var space = false;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (Character.isWhitespace(c)) {
				space = ! sb.isEmpty() || space;
				continue;
			}
			if (c < 0x20 || c == 0x7f)
				continue;
			if (space) {
				sb.append(' ');
				space = false;
			}
			sb.append(c);
		}
		if (space && ! sb.isEmpty())
			sb.append(' ');
		var t = sb.toString();
		var out = new StringBuilder(t.length() + 8);
		var lead = firstNonSpace(t);
		for (var i = 0; i < t.length(); i++) {
			var c = t.charAt(i);
			if ("\\`*_[]<>|~&{}()!.".indexOf(c) >= 0 || (i == lead && "#+-=".indexOf(c) >= 0)
				|| (c == ':' && t.startsWith("//", i + 1)))
				out.append('\\');
			if (i == lead && c >= '0' && c <= '9') {
				var j = i;
				while (j < t.length() && t.charAt(j) >= '0' && t.charAt(j) <= '9')
					j++;
				if (j < t.length() && t.charAt(j) == ')') {
					out.append(t, i, j).append("\\)");
					i = j;
					continue;
				}
			}
			out.append(c);
		}
		return out.toString();
	}

	private static int firstNonSpace(String t) {
		var i = 0;
		while (i < t.length() && t.charAt(i) == ' ')
			i++;
		return i;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Renderers
	//-----------------------------------------------------------------------------------------------------------------

	private static String renderHtml(Structure s, Function<Object,String> toText) {
		var sb = new StringBuilder();
		switch (s.kind()) {
			case UL, OL -> {
				var tag = s.kind() == Structure.Kind.UL ? "ul" : "ol";
				sb.append('<').append(tag).append('>');
				for (var o : s.items())
					sb.append("<li>").append(htmlItem(o, toText)).append("</li>");
				sb.append("</").append(tag).append('>');
			}
			case BLOCK -> sb.append("<blockquote>").append(htmlItem(s.items().get(0), toText)).append("</blockquote>");
			case TABLE -> {
				sb.append("<table><thead><tr>");
				for (var c : s.columns())
					sb.append("<th>").append(htmlEncode(c)).append("</th>");
				sb.append("</tr></thead><tbody>");
				for (var r : s.rows()) {
					sb.append("<tr>");
					for (var o : r)
						sb.append("<td>").append(htmlItem(o, toText)).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</tbody></table>");
			}
		}
		return sb.toString();
	}

	private static String htmlItem(Object o, Function<Object,String> toText) {
		return o instanceof Structure n ? renderHtml(n, toText) : htmlEncode(toText.apply(o));
	}

	private static String renderMarkdown(Structure s, Function<Object,String> toText) {
		var sb = new StringBuilder();
		switch (s.kind()) {
			case UL, OL -> {
				var n = 1;
				for (var o : s.items()) {
					var marker = s.kind() == Structure.Kind.UL ? "- " : (n++) + ". ";
					line(sb, marker + indentRest(mdItem(o, toText), marker.length()));
				}
			}
			case BLOCK -> {
				for (var l : mdItem(s.items().get(0), toText).split("\n", -1))
					line(sb, "> " + l);
			}
			case TABLE -> {
				var cols = new StringJoiner(" | ", "| ", " |");
				var rule = new StringJoiner(" | ", "| ", " |");
				for (var c : s.columns()) {
					cols.add(markdownEncode(c));
					rule.add("---");
				}
				line(sb, cols.toString());
				line(sb, rule.toString());
				for (var r : s.rows()) {
					var row = new StringJoiner(" | ", "| ", " |");
					for (var o : r)
						row.add(mdCell(o, toText));
					line(sb, row.toString());
				}
			}
		}
		return sb.toString();
	}

	private static String mdItem(Object o, Function<Object,String> toText) {
		return o instanceof Structure n ? renderMarkdown(n, toText) : markdownEncode(toText.apply(o));
	}

	private static String mdCell(Object o, Function<Object,String> toText) {
		// A nested structure cannot span lines inside a pipe table: flatten it to one line.
		return o instanceof Structure n ? markdownEncode(renderPlain(n, toText)) : markdownEncode(toText.apply(o));
	}

	private static String indentRest(String text, int indent) {
		return text.replace("\n", "\n" + " ".repeat(indent));
	}

	private static String renderPlain(Structure s, Function<Object,String> toText) {
		var sb = new StringBuilder();
		switch (s.kind()) {
			case UL, OL -> {
				var n = 1;
				for (var o : s.items()) {
					var marker = s.kind() == Structure.Kind.UL ? "- " : (n++) + ". ";
					line(sb, marker + indentRest(plainItem(o, toText), marker.length()));
				}
			}
			case BLOCK -> {
				for (var l : plainItem(s.items().get(0), toText).split("\n", -1))
					line(sb, "> " + l);
			}
			case TABLE -> {
				var grid = new ArrayList<List<String>>();
				var head = new ArrayList<String>();
				for (var c : s.columns())
					head.add(cell(c));
				grid.add(head);
				for (var r : s.rows()) {
					var row = new ArrayList<String>();
					for (var o : r)
						row.add(o instanceof Structure n ? cell(renderPlain(n, toText)) : cell(toText.apply(o)));
					grid.add(row);
				}
				var widths = new int[head.size()];
				for (var row : grid)
					for (var i = 0; i < row.size(); i++)
						widths[i] = Math.max(widths[i], row.get(i).length());
				for (var r = 0; r < grid.size(); r++) {
					line(sb, padRow(grid.get(r), widths));
					if (r == 0) {
						var rule = new ArrayList<String>();
						for (var w : widths)
							rule.add("-".repeat(Math.max(3, w)));
						line(sb, padRow(rule, widths));
					}
				}
			}
		}
		return sb.toString();
	}

	private static String plainItem(Object o, Function<Object,String> toText) {
		return o instanceof Structure n ? renderPlain(n, toText) : plainEncode(toText.apply(o));
	}

	private static String cell(String s) {
		return plainEncode(s).replace('\n', ' ').replace('\t', ' ');
	}

	private static String padRow(List<String> row, int[] widths) {
		var sb = new StringBuilder();
		for (var i = 0; i < row.size(); i++) {
			if (i > 0)
				sb.append("  ");
			sb.append(row.get(i));
			if (i < row.size() - 1)
				sb.append(" ".repeat(Math.max(0, widths[i] - row.get(i).length())));
		}
		return sb.toString();
	}

	private static void line(StringBuilder sb, String s) {
		if (! sb.isEmpty())
			sb.append('\n');
		sb.append(s);
	}
}
