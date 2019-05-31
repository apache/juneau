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
package org.apache.juneau.rest;

import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Programmatic interface for setting properties used by the HtmlDoc serializer.
 *
 * <p>
 * Basically just a convenience wrapper around the servlet or method level properties for setting properties defined
 * by the {@link HtmlDocSerializer} class.
 *
 * <p>
 * This class is instantiated through the following methods:
 * <ul>
 * 	<li class='jm'>{@link RestResponse#getHtmlDocBuilder()} - Set values programmatically during a REST request.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation}
 * </ul>
 *
 * @deprecated Use {@link HtmlDocConfig}
 */
@Deprecated
public class HtmlDocBuilder {

	private final PropertyStoreBuilder builder;

	/**
	 * Constructor.
	 *
	 * @param builder
	 */
	public HtmlDocBuilder(PropertyStoreBuilder builder) {
		this.builder = builder;
	}

	/**
	 * Processes the contents of an {@link HtmlDoc} tag.
	 *
	 * @param hd
	 */
	public void process(HtmlDoc hd) {
		if (hd.header().length > 0)
			header((Object[])hd.header());
		if (hd.nav().length > 0)
			nav((Object[])hd.nav());
		if (hd.aside().length > 0)
			aside((Object[])hd.aside());
		if (hd.footer().length > 0)
			footer((Object[])hd.footer());
		if (hd.style().length > 0)
			style((Object[])hd.style());
		if (hd.script().length > 0)
			script((Object[])hd.script());
		if (hd.navlinks().length > 0)
			navlinks((Object[])hd.navlinks());
		if (hd.head().length > 0)
			head((Object[])hd.head());
		if (hd.stylesheet().length > 0)
			stylesheet((Object[])hd.stylesheet());
		if (! hd.noResultsMessage().isEmpty())
			noResultsMessage(hd.noResultsMessage());
		if (! hd.nowrap().isEmpty())
			nowrap(Boolean.valueOf(hd.nowrap()));
		if (hd.template() != HtmlDocTemplate.class)
			template(hd.template());
	}

	/**
	 * Sets the HTML header section contents.
	 *
	 * <p>
	 * The page header normally contains the title and description, but this value can be used to override the contents
	 * to be whatever you want.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is HTML.
	 * 	<li>
	 * 		When a value is specified, the {@link #navlinks(Object...)} value will be ignored.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#header() @HtmlDoc(header)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML header section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder header(Object...value) {
		return set(HTMLDOC_header, resolveList(value, getStringArray(HTMLDOC_header)));
	}

	/**
	 * Sets the links in the HTML nav section.
	 *
	 * <p>
	 * The page links are positioned immediately under the title and text.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is a lax-JSON map of key/value pairs where the keys are the link text and the values are
	 * 		relative (to the servlet) or absolute URLs.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Supports {@doc juneau-marshall.URIs} (e.g. <js>"servlet:/..."</js>, <js>"request:/..."</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#navlinks() @HtmlDoc(navlinks)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML nav section links links.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder navlinks(Object...value) {
		return set(HTMLDOC_navlinks, resolveLinks(value, getStringArray(HTMLDOC_navlinks)));
	}

	/**
	 * Sets the HTML nav section contents.
	 *
	 * <p>
	 * The nav section of the page contains the links.
	 *
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is HTML.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		When a value is specified, the {@link #navlinks(Object[])} value will be ignored.
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#nav() @HtmlDoc(nav)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML nav section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder nav(Object...value) {
		return set(HTMLDOC_nav, resolveList(value, getStringArray(HTMLDOC_nav)));
	}

	/**
	 * Sets the HTML aside section contents.
	 *
	 * <p>
	 * The aside section typically floats on the right side of the page.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is HTML.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#aside() @HtmlDoc(aside)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML aside section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to waste
	 * 				string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder aside(Object...value) {
		return set(HTMLDOC_aside, resolveList(value, getStringArray(HTMLDOC_aside)));
	}

	/**
	 * Sets the HTML footer section contents.
	 *
	 * <p>
	 * The footer section typically floats on the bottom of the page.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is HTML.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#footer() @HtmlDoc(footer)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML footer section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder footer(Object...value) {
		return set(HTMLDOC_footer, resolveList(value, getStringArray(HTMLDOC_footer)));
	}

	/**
	 * Sets the HTML CSS style section contents.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is CSS.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#style() @HtmlDoc(style)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML CSS style section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder style(Object...value) {
		return set(HTMLDOC_style, resolveList(value, getStringArray(HTMLDOC_style)));
	}

	/**
	 * Sets the CSS URL in the HTML CSS style section.
	 *
	 * <p>
	 * Specifies the URL to the stylesheet to add as a link in the style tag in the header.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is a comma-delimited list of URLs.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#stylesheet() @HtmlDoc(stylesheet)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The CSS URL in the HTML CSS style section.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder stylesheet(Object...value) {
		return set(HTMLDOC_stylesheet, resolveSet(value, getStringArray(HTMLDOC_nav)));
	}

	/**
	 * Sets the HTML script section contents.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is Javascript.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#script() @HtmlDoc(script)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML script section contents.
	 * 	Object will be converted to a string using {@link Object#toString()}.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder script(Object...value) {
		return set(HTMLDOC_script, resolveList(value, getStringArray(HTMLDOC_script)));
	}

	/**
	 * Sets the HTML head section contents.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of this value is HTML.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"INHERIT"</js> means copy the values from the parent.
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#head() @HtmlDoc(head)} annotation.
	 * </ul>
	 *
	 * @param value
	 * 	The HTML head section contents.
	 * 	<p>
	 * 	<ul class='doctree'>
	 * 		<li class='info'>
	 * 			<b>Tip:</b>  Use {@link StringMessage} to generate value with delayed serialization so as not to
	 * 				waste string concatenation cycles on non-HTML views.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder head(Object...value) {
		return set(HTMLDOC_head, resolveList(value, getStringArray(HTMLDOC_head)));
	}

	/**
	 * Shorthand method for forcing the rendered HTML content to be no-wrap.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#nowrap() @HtmlDoc(nowrap)} annotation.
	 * </ul>
	 *
	 * @param value The new nowrap setting.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder nowrap(boolean value) {
		return set(HTMLDOC_nowrap, value);
	}

	/**
	 * Specifies the text to display when serializing an empty array or collection.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#noResultsMessage() @HtmlDoc(noResultsMessage)} annotation.
	 * </ul>
	 *
	 * @param value The text to display when serializing an empty array or collection.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder noResultsMessage(Object value) {
		return set(HTMLDOC_noResultsMessage, value);
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 *
	 * <p>
	 * By default, uses {@link BasicHtmlDocTemplate} to render the contents, although you can provide your own custom
	 * renderer or subclasses from the basic class to have full control over how the page is rendered.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc(template)} annotation.
	 * </ul>
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder template(Class<? extends HtmlDocTemplate> value) {
		return set(HTMLDOC_template, value);
	}

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 *
	 * <p>
	 * By default, uses {@link BasicHtmlDocTemplate} to render the contents, although you can provide your own custom
	 * renderer or subclasses from the basic class to have full control over how the page is rendered.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		This is the programmatic equivalent to the {@link HtmlDoc#template() @HtmlDoc(template)} annotation.
	 * </ul>
	 *
	 * @param value The HTML page template to use to render the HTML page.
	 * @return This object (for method chaining).
	 */
	public HtmlDocBuilder template(HtmlDocTemplate value) {
		return set(HTMLDOC_template, value);
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

	private static String[] resolveLinks(Object[] value, String[] prev) {
		List<String> list = new ArrayList<>();
		for (Object v : value) {
			String s = stringify(v);
			if ("INHERIT".equals(s)) {
				list.addAll(Arrays.asList(prev));
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
		return list.toArray(new String[list.size()]);
	}

	private static String[] resolveSet(Object[] value, String[] prev) {
		Set<String> set = new HashSet<>();
		for (Object v : value) {
			String s = stringify(v);
			if ("INHERIT".equals(s)) {
				if (prev != null)
					set.addAll(Arrays.asList(prev));
			} else if ("NONE".equals(s)) {
				return new String[0];
			} else {
				set.add(s);
			}
		}
		return set.toArray(new String[set.size()]);
	}

	private static String[] resolveList(Object[] value, String[] prev) {
		Set<String> set = new LinkedHashSet<>();
		for (Object v : value) {
			String s = stringify(v);
			if ("INHERIT".equals(s)) {
				if (prev != null)
					set.addAll(Arrays.asList(prev));
			} else if ("NONE".equals(s)) {
				return new String[0];
			} else {
				set.add(s);
			}
		}
		return set.toArray(new String[set.size()]);
	}

	private HtmlDocBuilder set(String key, Object value) {
		builder.set(key, value);
		return this;
	}

	private String[] getStringArray(String name) {
		return builder.peek(String[].class, name);
	}
}
