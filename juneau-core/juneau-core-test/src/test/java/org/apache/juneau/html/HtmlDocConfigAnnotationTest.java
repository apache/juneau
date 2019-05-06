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

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @HtmlDocConfig annotation.
 */
public class HtmlDocConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t.getClass().isArray())
				return apply(ArrayUtils.toList(t, Object.class));
			if (t instanceof Collection)
				return ((Collection<?>)t)
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t instanceof HtmlDocTemplate)
				return ((HtmlDocTemplate)t).getClass().getSimpleName();
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
		HtmlDocSerializerSession x = HtmlDocSerializer.create().applyAnnotations(m, sr).build().createSession();
		check("foo", x.getAside());
		check("foo", x.getFooter());
		check("foo", x.getHead());
		check("foo", x.getHeader());
		check("foo", x.getNav());
		check("foo2", x.getNavlinks());
		check("foo", x.getNoResultsMessage());
		check("true", x.isNowrap());
		check("foo2", x.getScript());
		check("foo2", x.getStyle());
		check("foo2", x.getStylesheet());
		check("BasicHtmlDocTemplate", x.getTemplate());
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
		HtmlDocSerializerSession x = HtmlDocSerializer.create().applyAnnotations(m, sr).build().createSession();
		check("", x.getAside());
		check("", x.getFooter());
		check("", x.getHead());
		check("", x.getHeader());
		check("", x.getNav());
		check("", x.getNavlinks());
		check("<p>no results</p>", x.getNoResultsMessage());
		check("false", x.isNowrap());
		check("", x.getScript());
		check("", x.getStyle());
		check("", x.getStylesheet());
		check("BasicHtmlDocTemplate", x.getTemplate());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		HtmlDocSerializerSession x = HtmlDocSerializer.create().applyAnnotations(m, sr).build().createSession();
		check("", x.getAside());
		check("", x.getFooter());
		check("", x.getHead());
		check("", x.getHeader());
		check("", x.getNav());
		check("", x.getNavlinks());
		check("<p>no results</p>", x.getNoResultsMessage());
		check("false", x.isNowrap());
		check("", x.getScript());
		check("", x.getStyle());
		check("", x.getStylesheet());
		check("BasicHtmlDocTemplate", x.getTemplate());
	}
}