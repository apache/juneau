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

import static org.apache.juneau.html.HtmlDocSerializer.*;

import org.apache.juneau.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link HtmlDocSerializer} class.
 */
public final class HtmlDocSerializerContext extends HtmlSerializerContext {

	final String[] style, stylesheet, script, navlinks, head, header, nav, aside, footer;
	final String noResultsMessage;
	final boolean nowrap;
	final HtmlDocTemplate template;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public HtmlDocSerializerContext(PropertyStore ps) {
		super(ps);
		style = ps.getProperty(HTMLDOC_style, String[].class, new String[0]);
		stylesheet = ps.getProperty(HTMLDOC_stylesheet, String[].class, new String[0]);
		script = ps.getProperty(HTMLDOC_script, String[].class, new String[0]);
		head = ps.getProperty(HTMLDOC_head, String[].class, new String[0]);
		header = ps.getProperty(HTMLDOC_header, String[].class, new String[0]);
		nav = ps.getProperty(HTMLDOC_nav, String[].class, new String[0]);
		aside = ps.getProperty(HTMLDOC_aside, String[].class, new String[0]);
		footer = ps.getProperty(HTMLDOC_footer, String[].class, new String[0]);
		nowrap = ps.getProperty(HTMLDOC_nowrap, boolean.class, false);
		navlinks = ps.getProperty(HTMLDOC_navlinks, String[].class, new String[0]);
		noResultsMessage = ps.getProperty(HTMLDOC_noResultsMessage, String.class, "<p>no results</p>");
		template = ps.getTypedProperty(HTMLDOC_template, HtmlDocTemplate.class, HtmlDocTemplateBasic.class);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlDocSerializerContext", new ObjectMap()
				.append("header", header)
				.append("nav", nav)
				.append("navlinks", navlinks)
				.append("aside", aside)
				.append("footer", footer)
				.append("style", style)
				.append("head", head)
				.append("stylesheet", stylesheet)
				.append("nowrap", nowrap)
				.append("template", template)
				.append("noResultsMessage", noResultsMessage)
			);
	}
}
