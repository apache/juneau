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
package org.apache.juneau.oapi;

import org.apache.juneau.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;
import static org.apache.juneau.TestUtils.*;

/**
 * Tests the @OpenApiConfig annotation.
 */
class OpenApiConfigAnnotation_Test extends SimpleTestBase {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@OpenApiConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void noValuesSerializer() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		assertNotThrown(()->OpenApiSerializer.create().apply(al).build().createSession());
	}

	@Test void noValuesParser() {
		var al = AnnotationWorkList.of(sr, b.getAnnotationList());
		assertNotThrown(()->OpenApiParser.create().apply(al).build().createSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		assertNotThrown(()->OpenApiSerializer.create().apply(al).build().createSession());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(sr, c.getAnnotationList());
		assertNotThrown(()->OpenApiParser.create().apply(al).build().createSession());
	}
}