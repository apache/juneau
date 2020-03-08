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
package org.apache.juneau.uon;

import static org.junit.Assert.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.annotation.*;
import org.junit.*;

/**
 * Tests the @UonConfig annotation.
 */
public class UonConfigAnnotationTest {

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

	@UonConfig(
		addBeanTypes="$X{true}",
		decoding="$X{true}",
		encoding="$X{true}",
		paramFormat="$X{UON}",
		validateEnd="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicSerializer() throws Exception {
		AnnotationList al = a.getAnnotationList();
		UonSerializerSession x = UonSerializer.create().applyAnnotations(al, sr).build().createSession();
		check("true", x.isAddBeanTypes());
		check("true", x.isEncoding());
		check("UON", x.getParamFormat());
	}

	@Test
	public void basicParser() throws Exception {
		AnnotationList al = a.getAnnotationList();
		UonParserSession x = UonParser.create().applyAnnotations(al, sr).build().createSession();
		check("true", x.isDecoding());
		check("true", x.isValidateEnd());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@UonConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationList al = b.getAnnotationList();
		UonSerializerSession x = UonSerializer.create().applyAnnotations(al, sr).build().createSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isEncoding());
		check("UON", x.getParamFormat());
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationList al = b.getAnnotationList();
		UonParserSession x = UonParser.create().applyAnnotations(al, sr).build().createSession();
		check("false", x.isDecoding());
		check("false", x.isValidateEnd());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationList al = c.getAnnotationList();
		UonSerializerSession x = UonSerializer.create().applyAnnotations(al, sr).build().createSession();
		check("false", x.isAddBeanTypes());
		check("false", x.isEncoding());
		check("UON", x.getParamFormat());
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationList al = c.getAnnotationList();
		UonParserSession x = UonParser.create().applyAnnotations(al, sr).build().createSession();
		check("false", x.isDecoding());
		check("false", x.isValidateEnd());
	}
}
