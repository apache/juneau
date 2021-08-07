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
import static java.util.Arrays.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HtmlDocConfig @HtmlDocConfig} annotation.
 */
public class HtmlDocConfigAnnotation {

	/**
	 * Applies {@link HtmlDocConfig} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<HtmlDocConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(HtmlDocConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlDocConfig> ai, ContextPropertiesBuilder b) {
			HtmlDocConfig a = ai.getAnnotation();

			b.setIf(a.aside().length > 0, HTMLDOC_aside, resolveList(a.aside(), b.peek(String[].class, HTMLDOC_aside)));
			b.setIf(! "DEFAULT".equalsIgnoreCase(a.asideFloat()), HTMLDOC_asideFloat, a.asideFloat().toUpperCase());
			b.setIf(a.footer().length > 0, HTMLDOC_footer, resolveList(a.footer(), b.peek(String[].class, HTMLDOC_footer)));
			b.setIf(a.head().length > 0, HTMLDOC_head, resolveList(a.head(), b.peek(String[].class, HTMLDOC_head)));
			b.setIf(a.header().length > 0, HTMLDOC_header, resolveList(a.header(), b.peek(String[].class, HTMLDOC_header)));
			b.setIf(a.nav().length > 0, HTMLDOC_nav, resolveList(a.nav(), b.peek(String[].class, HTMLDOC_nav)));
			b.setIf(a.navlinks().length > 0, HTMLDOC_navlinks, resolveLinks(a.navlinks(), b.peek(String[].class, HTMLDOC_navlinks)));
			b.setIfNotEmpty(HTMLDOC_noResultsMessage, string(a.noResultsMessage()));
			b.setIfNotEmpty(HTMLDOC_nowrap, bool(a.nowrap()));
			b.setIf(a.script().length > 0, HTMLDOC_script, resolveList(a.script(), b.peek(String[].class, HTMLDOC_script)));
			b.setIf(a.style().length > 0, HTMLDOC_style, resolveList(a.style(), b.peek(String[].class, HTMLDOC_style)));
			b.setIf(a.stylesheet().length > 0, HTMLDOC_stylesheet, resolveList(a.stylesheet(), b.peek(String[].class, HTMLDOC_stylesheet)));
			b.setIf(a.template() != HtmlDocTemplate.Null.class, HTMLDOC_template, a.template());
			asList(a.widgets()).stream().forEach(x -> b.prependTo(HTMLDOC_widgets, x));
		}

		private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

		private String[] resolveLinks(Object[] value, String[] prev) {
			AList<String> list = AList.create();
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
}