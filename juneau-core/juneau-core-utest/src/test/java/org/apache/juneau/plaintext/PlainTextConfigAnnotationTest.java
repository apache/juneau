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
package org.apache.juneau.plaintext;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @PlainTextConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class PlainTextConfigAnnotationTest {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@PlainTextConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesSerializer() throws Exception {
		AnnotationList al = b.getAnnotationList();
		PlainTextSerializer.create().applyAnnotations(al, sr).build().createSession();
	}

	@Test
	public void noValuesParser() throws Exception {
		AnnotationList al = b.getAnnotationList();
		PlainTextParser.create().applyAnnotations(al, sr).build().createSession();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationList al = c.getAnnotationList();
		PlainTextSerializer.create().applyAnnotations(al, sr).build().createSession();
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationList al = c.getAnnotationList();
		PlainTextParser.create().applyAnnotations(al, sr).build().createSession();
	}
}
