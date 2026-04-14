/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.html.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class HtmlAnnotation_Test extends TestBase {

	private static class X1 extends HtmlRender<Object> {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Html a1 = HtmlAnnotation.create()
		.anchorText("a")
		.description("b")
		.format(HtmlFormat.XML)
		.link("c")
		.noTableHeaders(true)
		.noTables(true)
		.render(X1.class)
		.build();

	Html a2 = HtmlAnnotation.create()
		.anchorText("a")
		.description("b")
		.format(HtmlFormat.XML)
		.link("c")
		.noTableHeaders(true)
		.noTables(true)
		.render(X1.class)
		.build();

	@Test void a01_basic() {
		assertBean(a1, "anchorText,description,format,link,noTableHeaders,noTables,render", "a,[b],XML,c,true,true,X1");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Html(
		anchorText="a",
		description={ "b" },
		format=HtmlFormat.XML,
		link="c",
		noTableHeaders=true,
		noTables=true,
		render=X1.class
	)
	public static class D1 {}
	Html d1 = D1.class.getAnnotationsByType(Html.class)[0];

	@Html(
		anchorText="a",
		description={ "b" },
		format=HtmlFormat.XML,
		link="c",
		noTableHeaders=true,
		noTables=true,
		render=X1.class
	)
	public static class D2 {}
	Html d2 = D2.class.getAnnotationsByType(Html.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}
