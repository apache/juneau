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

import java.util.Map;

import org.apache.juneau.internal.*;

/**
 * A basic template for the HTML doc serializer.
 *
 * <p>
 * This class can be subclassed to customize page rendering.
 */
public class HtmlDocTemplateBasic implements HtmlDocTemplate {

	@Override /* HtmlDocTemplate */
	public void head(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		if (hasStyle(session)) {
			w.sTag(1, "style").nl(1);
			style(session, w, s, o);
			w.ie(1).eTag("style").nl(1);
		}
		if (hasScript(session)) {
			w.sTag(1, "script").nl(1);
			script(session, w, s, o);
			w.ie(1).eTag("script").nl(1);
		}
	}

	@Override /* HtmlDocTemplate */
	public void style(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {

		String[] stylesheet = session.getStylesheet();
		if (! ArrayUtils.contains("NONE", stylesheet))
			for (String ss : stylesheet)
				w.append(2, "@import ").q().append(session.resolveUri(ss)).q().appendln("; ");

		if (session.isNoWrap())
			w.appendln("div.data * {white-space:nowrap;} ");

		if (session.getStyle() != null)
			for (String style : session.getStyle())
				w.append(style).appendln(" ");
	}

	@Override /* HtmlDocTemplate */
	public void script(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {

		if (session.getScript() != null)
			for (String script : session.getScript())
				w.append(script);
	}

	@Override /* HtmlDocTemplate */
	public void body(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {

		if (hasHeader(session)) {
			w.sTag(1, "header").nl(1);
			header(session, w, s, o);
			w.ie(1).eTag("header").nl(1);
		}

		if (hasNav(session)) {
			w.sTag(1, "nav").nl(1);
			nav(session, w, s, o);
			w.ie(1).eTag("nav").nl(1);
		}

		w.sTag(1, "section").nl(1);

		w.sTag(2, "article").nl(2);
		article(session, w, s, o);
		w.ie(2).eTag("article").nl(2);

		if (hasAside(session)) {
			w.sTag(2, "aside").nl(2);
			aside(session, w, s, o);
			w.ie(2).eTag("aside").nl(2);
		}

		w.ie(1).eTag("section").nl(1);

		if (hasFooter(session)) {
			w.sTag(1, "footer").nl(1);
			footer(session, w, s, o);
			w.ie(1).eTag("footer").nl(1);
		}
	}

	@Override /* HtmlDocTemplate */
	public void header(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		// Write the title of the page.
		String header = session.getHeader();
		if (header != null) {
			if (exists(header))
				w.append(2, header).nl(2);
		} else {
			String title = session.getTitle();
			String description = session.getDescription();
			String branding = session.getBranding();
			if (exists(title))
				w.oTag(3, "h1").append('>').append(title).eTag("h1").nl(3);
			if (exists(description))
				w.oTag(3, "h2").append('>').append(description).eTag("h2").nl(3);
			if (exists(branding))
				w.append(3, branding).nl(3);
		}
	}


	@Override /* HtmlDocTemplate */
	public void nav(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		String nav = session.getNav();
		if (nav != null) {
			if (exists(nav))
				w.append(2, nav).nl(2);
		} else {
			Map<String,Object> htmlLinks = session.getLinks();
			if (htmlLinks != null) {
				w.sTag("ol");
				for (Map.Entry<String,Object> e : htmlLinks.entrySet()) {
					w.sTag("li");
					String v = e.getValue().toString();
					if (v.startsWith("<"))
						w.append(v);
					else
						w.oTag("a").attr("href", session.resolveUri(v), true).cTag().text(e.getKey(), true).eTag("a");
					w.eTag("li");
				}
				w.eTag("ol");
			}
		}
	}

	@Override /* HtmlDocTemplate */
	public void aside(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		String aside = session.getAside();
		if (exists(aside))
			w.append(aside);
	}

	@Override /* HtmlDocTemplate */
	public void article(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		// To allow for page formatting using CSS, we encapsulate the data inside two div tags:
		// <div class='outerdata'><div class='data' id='data'>...</div></div>
		w.oTag(3, "div").attr("class","outerdata").append('>').nl(3);
		w.oTag(4, "div").attr("class","data").attr("id", "data").append('>').nl(4);

		if (o == null) {
			w.append(5, "<null/>").nl(5);
		} else if (ObjectUtils.isEmpty(o)){
			String m = session.getNoResultsMessage();
			if (exists(m))
				w.append(5, m).nl(5);
		} else {
			s.parentSerialize(session, o);
		}

		w.ie(4).eTag("div").nl(4);
		w.ie(4).eTag("div").nl(3);
	}

	@Override /* HtmlDocTemplate */
	public void footer(HtmlDocSerializerSession session, HtmlWriter w, HtmlDocSerializer s, Object o) throws Exception {
		String footer = session.getFooter();
		if (exists(footer))
			w.append(footer);
	}

	@Override /* HtmlDocTemplate */
	public boolean hasStyle(HtmlDocSerializerSession session) {
		return true;
	}

	@Override /* HtmlDocTemplate */
	public boolean hasScript(HtmlDocSerializerSession session) {
		return true;
	}

	@Override /* HtmlDocTemplate */
	public boolean hasHeader(HtmlDocSerializerSession session) {
		return exists(session.getHeader()) || exists(session.getTitle()) || exists(session.getDescription());
	}

	@Override /* HtmlDocTemplate */
	public boolean hasNav(HtmlDocSerializerSession session) {
		return exists(session.getNav()) || session.getLinks() != null;
	}

	@Override /* HtmlDocTemplate */
	public boolean hasAside(HtmlDocSerializerSession session) {
		return exists(session.getAside());
	}

	@Override /* HtmlDocTemplate */
	public boolean hasFooter(HtmlDocSerializerSession session) {
		return exists(session.getFooter());
	}

	private static boolean exists(String s) {
		return s != null && ! "NONE".equals(s);
	}
}
