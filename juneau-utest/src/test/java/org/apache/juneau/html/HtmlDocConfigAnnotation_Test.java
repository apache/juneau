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
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @HtmlDocConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class HtmlDocConfigAnnotation_Test {

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

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig(
		aside="$X{foo}",
		footer="$X{foo}",
		head="$X{foo}",
		header="$X{foo}",
		nav="$X{foo}",
		navlinks="$X{foo1}",
		noResultsMessage="$X{foo}",
		nowrap="$X{true}",
		script="$X{foo1}",
		style="$X{foo1}",
		stylesheet="$X{foo1}",
		template=BasicHtmlDocTemplate.class
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("foo", x.getAside());
		check("foo", x.getFooter());
		check("foo", x.getHead());
		check("foo", x.getHeader());
		check("foo", x.getNav());
		check("foo1", x.getNavlinks());
		check("foo", x.getNoResultsMessage());
		check("true", x.isNowrap());
		check("foo1", x.getScript());
		check("foo1", x.getStyle());
		check("foo1", x.getStylesheet());
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
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
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
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
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
	// Inheritance tests
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig(
		aside={"$X{foo2}","$X{INHERIT}"},
		footer={"$X{foo2}","$X{INHERIT}"},
		head={"$X{foo2}","$X{INHERIT}"},
		header={"$X{foo2}","$X{INHERIT}"},
		nav={"$X{foo2}","$X{INHERIT}"},
		navlinks={"$X{foo2}","$X{INHERIT}"},
		script={"$X{foo2}","$X{INHERIT}"},
		style={"$X{foo2}","$X{INHERIT}"},
		stylesheet={"$X{foo2}","$X{INHERIT}"}
	)
	static class D1 extends A {}
	static ClassInfo d1 = ClassInfo.of(D1.class);

	@Test
	public void inheritance1() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, d1.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("foo2,foo", x.getAside());
		check("foo2,foo", x.getFooter());
		check("foo2,foo", x.getHead());
		check("foo2,foo", x.getHeader());
		check("foo2,foo", x.getNav());
		check("foo2,foo1", x.getNavlinks());
		check("foo2,foo1", x.getScript());
		check("foo2,foo1", x.getStyle());
		check("foo2,foo1", x.getStylesheet());
	}

	@HtmlDocConfig(
		aside={"$X{INHERIT}","$X{foo2}"},
		footer={"$X{INHERIT}","$X{foo2}"},
		head={"$X{INHERIT}","$X{foo2}"},
		header={"$X{INHERIT}","$X{foo2}"},
		nav={"$X{INHERIT}","$X{foo2}"},
		navlinks={"$X{INHERIT}","$X{foo2}"},
		script={"$X{INHERIT}","$X{foo2}"},
		style={"$X{INHERIT}","$X{foo2}"},
		stylesheet={"$X{INHERIT}","$X{foo2}"}
	)
	static class D2 extends A {}
	static ClassInfo d2 = ClassInfo.of(D2.class);

	@Test
	public void inheritance2() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, d2.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("foo,foo2", x.getAside());
		check("foo,foo2", x.getFooter());
		check("foo,foo2", x.getHead());
		check("foo,foo2", x.getHeader());
		check("foo,foo2", x.getNav());
		check("foo1,foo2", x.getNavlinks());
		check("foo1,foo2", x.getScript());
		check("foo1,foo2", x.getStyle());
		check("foo1,foo2", x.getStylesheet());
	}

	@HtmlDocConfig(
		aside={"$X{foo2}"},
		footer={"$X{foo2}"},
		head={"$X{foo2}"},
		header={"$X{foo2}"},
		nav={"$X{foo2}"},
		navlinks={"$X{foo2}"},
		script={"$X{foo2}"},
		style={"$X{foo2}"},
		stylesheet={"$X{foo2}"}
	)
	static class D3 extends A {}
	static ClassInfo d3 = ClassInfo.of(D3.class);

	@Test
	public void inheritance3() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, d3.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("foo2", x.getAside());
		check("foo2", x.getFooter());
		check("foo2", x.getHead());
		check("foo2", x.getHeader());
		check("foo2", x.getNav());
		check("foo2", x.getNavlinks());
		check("foo2", x.getScript());
		check("foo2", x.getStyle());
		check("foo2", x.getStylesheet());
	}

	@HtmlDocConfig(
		aside={"NONE"},
		footer={"NONE"},
		head={"NONE"},
		header={"NONE"},
		nav={"NONE"},
		navlinks={"NONE"},
		script={"NONE"},
		style={"NONE"},
		stylesheet={"NONE"}
	)
	static class D4 extends A {}
	static ClassInfo d4 = ClassInfo.of(D4.class);

	@Test
	public void inheritance4() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, d4.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("", x.getAside());
		check("", x.getFooter());
		check("", x.getHead());
		check("", x.getHeader());
		check("", x.getNav());
		check("", x.getNavlinks());
		check("", x.getScript());
		check("", x.getStyle());
		check("", x.getStylesheet());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Widgets
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig(
		aside="$W{E}",
		footer="$W{E}",
		head="$W{E}",
		header="$W{E}",
		nav="$W{E}",
		navlinks="$W{E}",
		noResultsMessage="$W{E}",
		nowrap="$W{E}",
		script="$W{E}",
		style="$W{E}",
		stylesheet="$W{E}",
		widgets=EWidget.class
	)
	static class E {}
	static ClassInfo e = ClassInfo.of(E.class);

	public static class EWidget implements HtmlWidget {
		@Override
		public String getName() {
			return "E";
		}
		@Override
		public String getHtml(VarResolverSession session) {
			return "xxx";
		}
		@Override
		public String getScript(VarResolverSession session) {
			return "yyy";
		}
		@Override
		public String getStyle(VarResolverSession session) {
			return "zzz";
		}
	}

	@Test
	public void widgets_basic() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, e.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("$W{E}", x.getAside());
		check("$W{E}", x.getFooter());
		check("$W{E}", x.getHead());
		check("$W{E}", x.getHeader());
		check("$W{E}", x.getNav());
		check("$W{E}", x.getNavlinks());
		check("$W{E}", x.getNoResultsMessage());
		check("false", x.isNowrap());
		check("$W{E}", x.getScript());
		check("$W{E}", x.getStyle());
		check("$W{E}", x.getStylesheet());
		check("BasicHtmlDocTemplate", x.getTemplate());
	}

	@Test
	public void widgets_resolution() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, e.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		String r = x.serialize(null).replaceAll("[\r\n]+", "|");
		assertString(r).isContains(
			"<aside>xxx</aside>",
			"<footer>xxx</footer>",
			"<head>xxx",
			"<style>@import \"xxx\"; xxx zzz</style>",
			"<nav><ol><li>xxx</li></ol>xxx</nav>",
			"<script>xxx| yyy|</script>"
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Rank sorting
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlDocConfig(
		rank=1,
		aside="f1"
	)
	static class F1 {}

	@HtmlDocConfig(
		aside="f2"
	)
	static class F2 extends F1 {}

	@HtmlDocConfig(
		rank=3,
		aside="f3"
	)
	static class F3 extends F2 {}

	@HtmlDocConfig(
		rank=2,
		aside="f4"
	)
	static class F4 extends F3 {}

	@HtmlDocConfig(
		rank=3,
		aside="f5"
	)
	static class F5 extends F4 {}

	static ClassInfo f1 = ClassInfo.of(F1.class);
	static ClassInfo f2 = ClassInfo.of(F2.class);
	static ClassInfo f3 = ClassInfo.of(F3.class);
	static ClassInfo f4 = ClassInfo.of(F4.class);
	static ClassInfo f5 = ClassInfo.of(F5.class);

	@Test
	public void e01_rankedAnnotations_f1() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, f1.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("f1", x.getAside());
	}

	@Test
	public void e02_rankedAnnotations_f2() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, f2.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("f1", x.getAside());
	}

	@Test
	public void e03_rankedAnnotations_f3() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, f3.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("f3", x.getAside());
	}

	@Test
	public void e04_rankedAnnotations_f4() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, f4.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("f3", x.getAside());
	}

	@Test
	public void e05_rankedAnnotations_f5() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, f5.getAnnotationList());
		HtmlDocSerializerSession x = HtmlDocSerializer.create().apply(al).build().getSession();
		check("f5", x.getAside());
	}
}