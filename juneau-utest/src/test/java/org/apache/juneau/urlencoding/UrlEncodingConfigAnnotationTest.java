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
package org.apache.juneau.urlencoding;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.junit.*;

/**
 * Tests the @UrlEncodingConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class UrlEncodingConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@UrlEncodingConfig(
		expandedParams="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		UrlEncodingSerializerSession x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("true", x.isExpandedParams());
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		UrlEncodingParserSession x = UrlEncodingParser.create().apply(al).build().getSession();
		check("true", x.isExpandedParams());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@UrlEncodingConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		UrlEncodingSerializerSession x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		UrlEncodingParserSession x = UrlEncodingParser.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		UrlEncodingSerializerSession x = UrlEncodingSerializer.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		UrlEncodingParserSession x = UrlEncodingParser.create().apply(al).build().getSession();
		check("false", x.isExpandedParams());
	}
}
