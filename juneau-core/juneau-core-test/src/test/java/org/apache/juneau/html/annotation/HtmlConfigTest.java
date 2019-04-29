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
import static org.apache.juneau.html.HtmlSerializer.*;

import java.util.function.*;

import org.apache.juneau.html.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @HtmlConfig annotation.
 */
public class HtmlConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
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

	@HtmlConfig(
		addBeanTypes="$true",
		addKeyValueTableHeaders="$true",
		detectLabelParameters="$true",
		detectLinksInStrings="$true",
		labelParameter="$foo",
		uriAnchorText="$TO_STRING"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		HtmlSerializer x = HtmlSerializer.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(SERIALIZER_addBeanTypes));
		check("true", x.getProperty(HTML_addKeyValueTableHeaders));
		check("true", x.getProperty(HTML_detectLabelParameters));
		check("true", x.getProperty(HTML_detectLinksInStrings));
		check("foo", x.getProperty(HTML_labelParameter));
		check("TO_STRING", x.getProperty(HTML_uriAnchorText));
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		HtmlParser x = HtmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(HTML_addKeyValueTableHeaders));
		check(null, x.getProperty(HTML_detectLabelParameters));
		check(null, x.getProperty(HTML_detectLinksInStrings));
		check(null, x.getProperty(HTML_labelParameter));
		check(null, x.getProperty(HTML_uriAnchorText));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void defaultsSerializer() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		HtmlSerializer x = HtmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(HTML_addKeyValueTableHeaders));
		check(null, x.getProperty(HTML_detectLabelParameters));
		check(null, x.getProperty(HTML_detectLinksInStrings));
		check(null, x.getProperty(HTML_labelParameter));
		check(null, x.getProperty(HTML_uriAnchorText));
	}

	@Test
	public void defaultsParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		HtmlParser x = HtmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
		check(null, x.getProperty(HTML_addKeyValueTableHeaders));
		check(null, x.getProperty(HTML_detectLabelParameters));
		check(null, x.getProperty(HTML_detectLinksInStrings));
		check(null, x.getProperty(HTML_labelParameter));
		check(null, x.getProperty(HTML_uriAnchorText));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		HtmlSerializer x = HtmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		HtmlParser x = HtmlParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SERIALIZER_addBeanTypes));
	}
}
