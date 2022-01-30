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

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @HtmlConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class HtmlConfigAnnotation_Test {

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

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlConfig(
		addBeanTypes="$X{true}",
		addKeyValueTableHeaders="$X{true}",
		disableDetectLabelParameters="$X{true}",
		disableDetectLinksInStrings="$X{true}",
		labelParameter="$X{foo}",
		uriAnchorText="$X{TO_STRING}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		HtmlSerializerSession x = HtmlSerializer.create().apply(al).build().getSession();
		check("true", x.isAddBeanTypes());
		check("true", x.isAddKeyValueTableHeaders());
		check("false", x.isDetectLabelParameters());
		check("false", x.isDetectLinksInStrings());
		check("foo", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		HtmlParser.create().apply(al).build().createSession();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@HtmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void defaultsSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		HtmlSerializerSession x = HtmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddKeyValueTableHeaders());
		check("true", x.isDetectLabelParameters());
		check("true", x.isDetectLinksInStrings());
		check("label", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test
	public void defaultsParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		HtmlParser.create().apply(al).build().createSession();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		HtmlSerializerSession x = HtmlSerializer.create().apply(al).build().getSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isAddKeyValueTableHeaders());
		check("true", x.isDetectLabelParameters());
		check("true", x.isDetectLinksInStrings());
		check("label", x.getLabelParameter());
		check("TO_STRING", x.getUriAnchorText());
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		HtmlParser.create().apply(al).build().createSession();
	}
}
