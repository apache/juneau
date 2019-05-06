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
package org.apache.juneau.html.annotation;

import static org.apache.juneau.html.HtmlDocSerializer.*;
import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.utils.*;

/**
 * Applies {@link HtmlDocConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class HtmlDocConfigApply extends ConfigApply<HtmlDocConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public HtmlDocConfigApply(Class<HtmlDocConfig> c, StringResolver r) {
		super(c, r);
	}

	@Override
	public void apply(HtmlDocConfig a, PropertyStoreBuilder psb) {
		if (a.aside().length > 0)
			psb.set(HTMLDOC_aside, strings(a.aside()));
		if (a.footer().length > 0)
			psb.set(HTMLDOC_footer, strings(a.footer()));
		if (a.head().length > 0)
			psb.set(HTMLDOC_head, strings(a.head()));
		if (a.header().length > 0)
			psb.set(HTMLDOC_header, strings(a.header()));
		if (a.nav().length > 0)
			psb.set(HTMLDOC_nav, strings(a.nav()));
		if (a.navlinks().length > 0)
			psb.addTo(HTMLDOC_navlinks, strings(a.navlinks()));
		if (a.navlinks_replace().length > 0)
			psb.set(HTMLDOC_navlinks, strings(a.navlinks_replace()));
		if (! a.noResultsMessage().isEmpty())
			psb.set(HTMLDOC_noResultsMessage, string(a.noResultsMessage()));
		if (! a.nowrap().isEmpty())
			psb.set(HTMLDOC_nowrap, bool(a.nowrap()));
		if (a.script().length > 0)
			psb.addTo(HTMLDOC_script, strings(a.script()));
		if (a.script_replace().length > 0)
			psb.set(HTMLDOC_script, strings(a.script_replace()));
		if (a.style().length > 0)
			psb.addTo(HTMLDOC_style, strings(a.style()));
		if (a.style_replace().length > 0)
			psb.set(HTMLDOC_style, strings(a.style_replace()));
		if (a.stylesheet().length > 0)
			psb.addTo(HTMLDOC_stylesheet, strings(a.stylesheet()));
		if (a.stylesheet_replace().length > 0)
			psb.set(HTMLDOC_stylesheet, strings(a.stylesheet_replace()));
		if (a.template() != HtmlDocTemplate.Null.class)
			psb.set(HTMLDOC_template, a.template());
	}
}
