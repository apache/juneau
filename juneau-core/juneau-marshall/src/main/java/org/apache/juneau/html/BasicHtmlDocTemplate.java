// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html;

import static org.apache.juneau.html.AsideFloat.*;

import org.apache.juneau.internal.*;

/**
 * A basic template for the HTML doc serializer.
 *
 * <p>
 * This class can be subclassed to customize page rendering.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class BasicHtmlDocTemplate implements HtmlDocTemplate {

	@Override /* HtmlDocTemplate */
	public void writeTo(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		w.sTag("html").nl(0);
		w.sTag(1, "head").nl(1);
		head(session, w, o);
		w.eTag(1, "head").nl(1);
		w.sTag(1, "body").nl(1);
		body(session, w, o);
		w.eTag(1, "body").nl(1);
		w.eTag("html").nl(0);
	}

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void head(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {

		String[] head = session.getHead();
		for (int i = 0; i < head.length; i++)
			w.sIf(i > 0).appendln(2, session.resolve(head[i]));

		if (hasStyle(session)) {
			w.sTag(2, "style").nl(2);
			style(session, w, o);
			w.ie(2).eTag("style").nl(2);
		}
		if (hasScript(session)) {
			w.sTag(2, "script").nl(2);
			script(session, w, o);
			w.ie(2).eTag("script").nl(2);
		}
	}

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt>/<xt>&lt;style&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void style(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		Flag addSpace = Flag.create();
		for (String s : session.getStylesheet())
			w.sIf(addSpace.getAndSet()).append(3, "@import ").q().append(session.resolveUri(session.resolve(s))).q().appendln(";");
		if (session.isNowrap())
			w.appendln(3, "div.data * {white-space:nowrap;} ");
		for (String s : session.getStyle())
			w.sIf(addSpace.getAndSet()).appendln(3, session.resolve(s));
		session.forEachWidget(x -> {
			w.sIf(addSpace.getAndSet()).appendln(3, session.resolve(x.getStyle(session.getVarResolver())));
		});
	}

	/**
	 * Renders the contents of the <code><xt>&lt;head&gt;</xt>/<xt>&lt;script&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void script(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		Flag addSpace = Flag.create();
		for (String s : session.getScript())
			w.sIf(addSpace.getAndSet()).append(3, session.resolve(s)).append('\n'); // Must always append a newline even if whitespace disabled!
		session.forEachWidget(x -> {
			w.sIf(addSpace.getAndSet()).append(3, session.resolve(x.getScript(session.getVarResolver()))).w('\n'); // Must always append a newline even if whitespace disabled!
		});
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void body(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {

		AsideFloat asideFloat = session.getAsideFloat();
		boolean hasAside = hasAside(session);

		if (hasHeader(session)) {
			w.sTag(2, "header").nl(2);
			header(session, w, o);
			w.ie(2).eTag("header").nl(2);
		}

		if (hasNav(session)) {
			w.sTag(2, "nav").nl(2);
			nav(session, w, o);
			w.ie(2).eTag("nav").nl(2);
		}

		if (hasAside && asideFloat.is(TOP)) {
			w.sTag(2, "section").nl(2);
			w.sTag(3, "aside").nl(3);
			aside(session, w, o);
			w.ie(3).eTag("aside").nl(3);
			w.ie(2).eTag("section").nl(2);
		}

		w.sTag(2, "section").nl(2);

		if (hasAside && asideFloat.is(LEFT)) {
			w.sTag(3, "aside").nl(3);
			aside(session, w, o);
			w.ie(3).eTag("aside").nl(3);
		}

		w.sTag(3, "article").nl(3);
		article(session, w, o);
		w.ie(3).eTag("article").nl(3);

		if (hasAside && asideFloat.isAny(RIGHT, DEFAULT)) {
			w.sTag(3, "aside").nl(3);
			aside(session, w, o);
			w.ie(3).eTag("aside").nl(3);
		}

		w.ie(2).eTag("section").nl(2);

		if (hasAside && asideFloat.is(BOTTOM)) {
			w.sTag(2, "section").nl(2);
			w.sTag(3, "aside").nl(3);
			aside(session, w, o);
			w.ie(3).eTag("aside").nl(3);
			w.ie(2).eTag("section").nl(2);
		}

		if (hasFooter(session)) {
			w.sTag(2, "footer").nl(2);
			footer(session, w, o);
			w.ie(2).eTag("footer").nl(2);
		}
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;header&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void header(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		// Write the title of the page.
		String[] header = session.getHeader();
		for (int i = 0; i < header.length; i++)
			w.sIf(i > 0).appendln(3, session.resolve(header[i]));
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;nav&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void nav(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		String[] links = session.getNavLinks();
		if (links.length > 0 && ! ArrayUtils.contains("NONE", links)) {
			w.sTag(3, "ol").nl(3);
			for (String l : links) {
				w.sTag(4, "li");
				l = session.resolve(l);
				if (l.matches("(?s)\\S+\\:.*")) {
					int i = l.indexOf(':');
					String key = l.substring(0, i);
					String val = l.substring(i+1).trim();
					if (val.startsWith("<"))
						w.nl(4).appendln(5, val);
					else
						w.oTag("a").attr("href", session.resolveUri(val), true).cTag().text(key, true).eTag("a");
					w.eTag("li").nl(4);
				} else {
					w.nl(4).appendln(5, l);
					w.eTag(4, "li").nl(4);
				}
			}
			w.eTag(3, "ol").nl(3);
		}
		String[] nav = session.getNav();
		if (nav.length > 0) {
			for (int i = 0; i < nav.length; i++)
				w.sIf(i > 0).appendln(3, session.resolve(nav[i]));
		}
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;aside&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void aside(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		String[] aside = session.getAside();
		for (int i = 0; i < aside.length; i++)
			w.sIf(i > 0).appendln(4, session.resolve(aside[i]));
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;article&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void article(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		// To allow for page formatting using CSS, we encapsulate the data inside two div tags:
		// <div class='outerdata'><div class='data' id='data'>...</div></div>
		w.oTag(4, "div").attr("class","outerdata").append('>').nl(4);
		w.oTag(5, "div").attr("class","data").attr("id", "data").append('>').nl(5);

		if (o == null) {
			w.append(6, "<null/>").nl(6);
		} else if (ObjectUtils.isEmpty(o)){
			String m = session.getNoResultsMessage();
			if (exists(m))
				w.append(6, session.resolve(m)).nl(6);
		} else {
			session.indent = 6;
			w.flush();
			session.parentSerialize(w, o);
		}

		w.ie(5).eTag("div").nl(5);
		w.ie(4).eTag("div").nl(4);
	}

	/**
	 * Renders the contents of the <code><xt>&lt;body&gt;</xt>/<xt>&lt;footer&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @param w The writer being written to.
	 * @param o The object being serialized.
	 * @throws Exception Any exception can be thrown.
	 */
	protected void footer(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
		String[] footer = session.getFooter();
		for (int i = 0; i < footer.length; i++)
			w.sIf(i > 0).appendln(3, session.resolve(footer[i]));
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;head&gt;</xt>/<xt>&lt;style&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasStyle(HtmlDocSerializerSession session) {
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;head&gt;</xt>/<xt>&lt;script&gt;</xt></code> element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasScript(HtmlDocSerializerSession session) {
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;header&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasHeader(HtmlDocSerializerSession session) {
		return session.getHeader().length > 0;
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;nav&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasNav(HtmlDocSerializerSession session) {
		return session.getNav().length > 0 || session.getNavLinks().length > 0;
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;aside&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasAside(HtmlDocSerializerSession session) {
		return session.getAside().length > 0;
	}

	/**
	 * Returns <jk>true</jk> if this page should render a <code><xt>&lt;body&gt;</xt>/<xt>&lt;footer&gt;</xt></code>
	 * element.
	 *
	 * @param session The current serializer session.
	 * @return A boolean flag.
	 */
	protected boolean hasFooter(HtmlDocSerializerSession session) {
		return session.getFooter().length > 0;
	}

	private static boolean exists(String s) {
		return s != null && ! "NONE".equals(s);
	}
}
