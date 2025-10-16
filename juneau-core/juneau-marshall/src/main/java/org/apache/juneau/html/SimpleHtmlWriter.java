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
package org.apache.juneau.html;

import java.io.*;

/**
 * Utility class for creating custom HTML.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjson'>
 * 	String <jv>table</jv> = <jk>new</jk> SimpleHtmlWriter().sTag(<js>"table"</js>).sTag(<js>"tr"</js>).sTag(<js>"td"</js>)
 * 	.append(<js>"hello"</js>).eTag(<js>"td"</js>).eTag(<js>"tr"</js>).eTag(<js>"table"</js>).toString();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
public class SimpleHtmlWriter extends HtmlWriter {

	/**
	 * Constructor.
	 */
	public SimpleHtmlWriter() {
		super(new StringWriter(), true, 100, false, '\'', null);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return out.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter text(Object o, boolean preserveWhitespace) {
		super.text(o, preserveWhitespace);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(String ns, String name, boolean needsEncoding) {
		super.oTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(String ns, String name) {
		super.oTag(ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(String name) {
		super.oTag(name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(int indent, String ns, String name, boolean needsEncoding) {
		super.oTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(int indent, String ns, String name) {
		super.oTag(indent, ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oTag(int indent, String name) {
		super.oTag(indent, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(String ns, String name, boolean needsEncoding) {
		super.tag(ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(String ns, String name) {
		super.tag(ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(String name) {
		super.tag(name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(int indent, String name) {
		super.tag(indent, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(int indent, String ns, String name, boolean needsEncoding) {
		super.tag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter tag(int indent, String ns, String name) {
		super.tag(indent, ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(String ns, String name) {
		super.sTag(ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(String ns, String name, boolean needsEncoding) {
		super.sTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(int indent, String ns, String name) {
		super.sTag(indent, ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(int indent, String name) {
		super.sTag(indent, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(String name) {
		super.sTag(name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sTag(int indent, String ns, String name, boolean needsEncoding) {
		super.sTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(String ns, String name) {
		super.eTag(ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(String ns, String name, boolean needsEncoding) {
		super.eTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(int indent, String ns, String name) {
		super.eTag(indent, ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(int indent, String name) {
		super.eTag(indent, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(String name) {
		super.eTag(name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter eTag(int indent, String ns, String name, boolean needsEncoding) {
		super.eTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter attr(String name, Object value) {
		super.attr(name, value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter attr(String ns, String name, Object value) {
		super.attr(ns, name, value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter attr(String ns, String name, Object value, boolean valNeedsEncoding) {
		super.attr(ns, name, value, valNeedsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter attr(String name, Object value, boolean valNeedsEncoding) {
		super.attr(name, value, valNeedsEncoding);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter oAttr(String ns, String name) {
		super.oAttr(ns, name);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter s() {
		super.s();
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter q() {
		super.q();
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(char c) {
		super.append(c);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter cTag() {
		super.cTag();
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter ceTag() {
		super.ceTag();
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter attrUri(String name, Object value) {
		super.attrUri(name, value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter text(Object value) {
		super.text(value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter textUri(Object value) {
		super.textUri(value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter ie(int indent) {
		super.ie(indent);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter w(char c) {
		super.w(c);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter w(String s) {
		super.w(s);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter appendUri(Object value) {
		super.appendUri(value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter append(char[] value) {
		super.append(value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter sIf(boolean flag) {
		super.sIf(flag);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter nlIf(boolean flag, int indent) {
		super.nlIf(flag, indent);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter appendIf(boolean flag, String value) {
		super.appendIf(flag, value);
		return this;
	}

	@Override /* Overridden from HtmlWriter */
	public SimpleHtmlWriter appendIf(boolean flag, char value) {
		super.appendIf(flag, value);
		return this;
	}
}