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
package org.apache.juneau.soap.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.soap.SoapXmlSerializer.*;

import java.util.function.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @SoapXmlConfig annotation.
 */
public class SoapXmlConfigTest {

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

	@SoapXmlConfig(
		soapAction="$foo"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		SoapXmlSerializer x = SoapXmlSerializer.create().applyAnnotations(m, sr).build();
		check("foo", x.getProperty(SOAPXML_SOAPAction));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@SoapXmlConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValues() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		SoapXmlSerializer x = SoapXmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SOAPXML_SOAPAction));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		SoapXmlSerializer x = SoapXmlSerializer.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(SOAPXML_SOAPAction));
	}
}
