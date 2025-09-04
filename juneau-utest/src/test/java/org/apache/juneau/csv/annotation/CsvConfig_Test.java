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
package org.apache.juneau.csv.annotation;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Tests the @CsvConfig annotation.
 */
class CsvConfig_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@CsvConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void defaultsSerializer() {
		var al = AnnotationWorkList.of(b.getAnnotationList());
		assertDoesNotThrow(()->CsvSerializer.create().apply(al).build());
	}

	@Test void defaultsParser() {
		var al = AnnotationWorkList.of(b.getAnnotationList());
		assertDoesNotThrow(()->CsvParser.create().apply(al).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void noAnnotationSerializer() {
		var al = AnnotationWorkList.of(b.getAnnotationList());
		assertDoesNotThrow(()->CsvSerializer.create().apply(al).build());
	}

	@Test void noAnnotationParser() {
		var al = AnnotationWorkList.of(b.getAnnotationList());
		assertDoesNotThrow(()->CsvParser.create().apply(al).build());
	}
}