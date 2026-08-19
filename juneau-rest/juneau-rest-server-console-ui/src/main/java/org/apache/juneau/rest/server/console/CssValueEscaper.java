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
package org.apache.juneau.rest.server.console;

/**
 * A declaration-boundary CSS escaper for the {@code --jc-name: <value>;} custom-property declaration
 * {@code ConsoleChromeMixin} appends to the served {@code chrome.css}.
 *
 * <p>
 * Every occurrence of the CSS/markup breakout set &mdash; <code>; {  } \ &lt; &gt;</code> &mdash; plus any C0
 * control character is replaced by its CSS numeric escape (<code>\3B&nbsp;</code> for <code>;</code>,
 * <code>\7B&nbsp;</code> for <code>{</code>, <code>\7D&nbsp;</code> for <code>}</code>, <code>\5C&nbsp;</code> for
 * <code>\</code>, <code>\3C&nbsp;</code> for <code>&lt;</code>, <code>\3E&nbsp;</code> for <code>&gt;</code>,
 * trailing-space terminator included). Everything else passes through unescaped.
 *
 * <p>
 * <b>This is defense-in-depth for the {@code :root{}} declaration boundary &mdash; not the sink-safety
 * mechanism.</b> Sink safety is {@code CssValueGrammar}'s accept-known-safe grammar plus {@code chrome.css}'s
 * sink-property rules (color/font/length tokens never sink into a url-capable property). This escaper is
 * deliberately <b>not</b> widened to include <code>(</code>/<code>)</code> &mdash; doing so would corrupt
 * legitimate {@code linear-gradient(...)} / {@code rgb(...)} values on decode &mdash; and is explicitly
 * <b>not</b> trusted to make a raw {@code url\n(} "safe": {@code CssValueGrammar} already REJECTs any value
 * containing a C0/C1 control character before it ever reaches this class, so the C0 clause here is a leftover
 * belt for any code path that emits a pre-validation value, not a control someone should rely on.
 */
final class CssValueEscaper {

	private CssValueEscaper() {}

	/**
	 * Escapes the CSS/markup breakout set and C0 control characters in the specified value.
	 *
	 * @param value The value to escape.
	 * @return The escaped value, safe to place inside a {@code --jc-name: <value>;} declaration.
	 */
	static String escape(String value) {
		var sb = new StringBuilder(value.length());
		for (var i = 0; i < value.length(); i++) {
			var c = value.charAt(i);
			if (isBreakout(c)) {
				sb.append('\\').append(Integer.toHexString(c).toUpperCase(java.util.Locale.ROOT)).append(' ');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static boolean isBreakout(char c) {
		return c == ';' || c == '{' || c == '}' || c == '\\' || c == '<' || c == '>' || c <= 0x1F;
	}
}
