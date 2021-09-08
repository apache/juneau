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
package org.apache.juneau.jso;

import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.reflect.*;
import org.junit.*;

/**
 * Tests the @JsoConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class JsoConfigAnnotationTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@JsoConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void defaultsSerializer() throws Exception {
		List<AnnotationWork> al = b.getAnnotationList().getWork(null);
		JsoSerializer.create().apply(al).build().createSession();
	}

	@Test
	public void defaultsParser() throws Exception {
		List<AnnotationWork> al = b.getAnnotationList().getWork(null);
		JsoParser.create().apply(al).build().createSession();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationSerializer() throws Exception {
		List<AnnotationWork> al = c.getAnnotationList().getWork(null);
		JsoSerializer.create().apply(al).build().createSession();
	}

	@Test
	public void noAnnotationParser() throws Exception {
		List<AnnotationWork> al = c.getAnnotationList().getWork(null);
		JsoParser.create().apply(al).build().createSession();
	}
}