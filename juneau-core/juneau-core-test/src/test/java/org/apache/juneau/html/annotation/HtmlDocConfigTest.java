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

import static org.junit.Assert.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.html.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @HtmlDocConfig annotation.
 */
public class HtmlDocConfigTest {

	private static void check(String expected, Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			return t.toString();
		}
	};

	static StringResolver sr = new StringResolver() {
		@Override
		public String resolve(String input) {
			if (input.startsWith("$"))
				input = input.substring(1);
			return input;
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig(
		aside="$foo",
		footer="$foo",
		head="$foo",
		header="$foo",
		nav="$foo",
		navlinks="$foo1",
		navlinks_replace="$foo2",
		noResultsMessage="$foo",
		nowrap="$true",
		script="$foo1",
		script_replace="$foo2",
		style="$foo1",
		style_replace="$foo2",
		stylesheet="$foo1",
		stylesheet_replace="$foo2",
		template=BasicHtmlDocTemplate.class
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		HtmlDocSerializer x = HtmlDocSerializer.create().applyAnnotations(m, sr).build();
		check("foo", x.getProperty(HTMLDOC_aside));
		check("foo", x.getProperty(HTMLDOC_footer));
		check("foo", x.getProperty(HTMLDOC_head));
		check("foo", x.getProperty(HTMLDOC_header));
		check("foo", x.getProperty(HTMLDOC_nav));
		check("foo2", x.getProperty(HTMLDOC_navlinks));
		check(null, x.getProperty(HTMLDOC_navlinks_add));
		check("foo", x.getProperty(HTMLDOC_noResultsMessage));
		check("true", x.getProperty(HTMLDOC_nowrap));
		check("foo2", x.getProperty(HTMLDOC_script));
		check(null, x.getProperty(HTMLDOC_script_add));
		check("foo2", x.getProperty(HTMLDOC_style));
		check(null, x.getProperty(HTMLDOC_style_add));
		check("foo2", x.getProperty(HTMLDOC_stylesheet));
		check(null, x.getProperty(HTMLDOC_stylesheet_add));
		check("BasicHtmlDocTemplate", x.getProperty(HTMLDOC_template));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void defaults() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		HtmlDocSerializer x = HtmlDocSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(HTMLDOC_aside));
		check(null, x.getProperty(HTMLDOC_footer));
		check(null, x.getProperty(HTMLDOC_head));
		check(null, x.getProperty(HTMLDOC_header));
		check(null, x.getProperty(HTMLDOC_nav));
		check(null, x.getProperty(HTMLDOC_navlinks));
		check(null, x.getProperty(HTMLDOC_navlinks_add));
		check(null, x.getProperty(HTMLDOC_noResultsMessage));
		check(null, x.getProperty(HTMLDOC_nowrap));
		check(null, x.getProperty(HTMLDOC_script));
		check(null, x.getProperty(HTMLDOC_script_add));
		check(null, x.getProperty(HTMLDOC_style));
		check(null, x.getProperty(HTMLDOC_style_add));
		check(null, x.getProperty(HTMLDOC_stylesheet));
		check(null, x.getProperty(HTMLDOC_stylesheet_add));
		check(null, x.getProperty(HTMLDOC_template));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		HtmlDocSerializer x = HtmlDocSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(HTMLDOC_aside));
	}
}