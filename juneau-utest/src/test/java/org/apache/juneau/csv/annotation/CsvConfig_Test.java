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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

/**
 * Tests the @CsvConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class CsvConfig_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@CsvConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void defaultsSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(b.getAnnotationList());
		CsvSerializer.create().apply(al).build();
	}

	@Test
	public void defaultsParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(b.getAnnotationList());
		CsvParser.create().apply(al).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(b.getAnnotationList());
		CsvSerializer.create().apply(al).build();
	}

	@Test
	public void noAnnotationParser() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(b.getAnnotationList());
		CsvParser.create().apply(al).build();
	}
}