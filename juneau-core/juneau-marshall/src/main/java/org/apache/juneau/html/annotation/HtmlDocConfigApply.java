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
import static org.apache.juneau.internal.StringUtils.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

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
	public HtmlDocConfigApply(Class<HtmlDocConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(AnnotationInfo<HtmlDocConfig> ai, PropertyStoreBuilder psb) {
		HtmlDocConfig a = ai.getAnnotation();
		if (a.aside().length > 0)
			psb.set(HTMLDOC_aside, resolveList(a.aside(), psb.peek(String[].class, HTMLDOC_aside)));
		if (! "DEFAULT".equalsIgnoreCase(a.asideFloat()))
			psb.set(HTMLDOC_asideFloat, a.asideFloat().toUpperCase());
		if (a.footer().length > 0)
			psb.set(HTMLDOC_footer, resolveList(a.footer(), psb.peek(String[].class, HTMLDOC_footer)));
		if (a.head().length > 0)
			psb.set(HTMLDOC_head, resolveList(a.head(), psb.peek(String[].class, HTMLDOC_head)));
		if (a.header().length > 0)
			psb.set(HTMLDOC_header, resolveList(a.header(), psb.peek(String[].class, HTMLDOC_header)));
		if (a.nav().length > 0)
			psb.set(HTMLDOC_nav, resolveList(a.nav(), psb.peek(String[].class, HTMLDOC_nav)));
		if (a.navlinks().length > 0)
			psb.set(HTMLDOC_navlinks, resolveLinks(a.navlinks(), psb.peek(String[].class, HTMLDOC_navlinks)));
		if (! a.noResultsMessage().isEmpty())
			psb.set(HTMLDOC_noResultsMessage, string(a.noResultsMessage()));
		if (! a.nowrap().isEmpty())
			psb.set(HTMLDOC_nowrap, bool(a.nowrap()));
		if (a.script().length > 0)
			psb.set(HTMLDOC_script, resolveList(a.script(), psb.peek(String[].class, HTMLDOC_script)));
		if (a.style().length > 0)
			psb.set(HTMLDOC_style, resolveList(a.style(), psb.peek(String[].class, HTMLDOC_style)));
		if (a.stylesheet().length > 0)
			psb.set(HTMLDOC_stylesheet, resolveList(a.stylesheet(), psb.peek(String[].class, HTMLDOC_stylesheet)));
		if (a.template() != HtmlDocTemplate.Null.class)
			psb.set(HTMLDOC_template, a.template());
		for (Class<? extends HtmlWidget> w : a.widgets()) {
			try {
				psb.prependTo(HTMLDOC_widgets, w.newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

	private String[] resolveLinks(Object[] value, String[] prev) {
		AList<String> list = AList.of();
		for (Object v : value) {
			String s = string(stringify(v));
			if (s == null)
				return new String[0];
			if ("INHERIT".equals(s)) {
				if (prev != null)
					list.a(prev);
			} else if (s.indexOf('[') != -1 && INDEXED_LINK_PATTERN.matcher(s).matches()) {
				Matcher lm = INDEXED_LINK_PATTERN.matcher(s);
				lm.matches();
				String key = lm.group(1);
				int index = Math.min(list.size(), Integer.parseInt(lm.group(2)));
				String remainder = lm.group(3);
				list.add(index, key.isEmpty() ? remainder : key + ":" + remainder);
			} else {
				list.add(s);
			}
		}
		return list.asArrayOf(String.class);
	}

	private String[] resolveList(Object[] value, String[] prev) {
		ASet<String> set = ASet.of();
		for (Object v : value) {
			String s = string(stringify(v));
			if ("INHERIT".equals(s)) {
				if (prev != null)
					set.a(prev);
			} else if ("NONE".equals(s)) {
				return new String[0];
			} else {
				set.add(s);
			}
		}
		return set.asArrayOf(String.class);
	}
}
