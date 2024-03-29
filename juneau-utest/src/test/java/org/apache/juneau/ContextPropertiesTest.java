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
package org.apache.juneau;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.junit.*;


/**
 * Test the ContextProperties class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ContextPropertiesTest {

	//-------------------------------------------------------------------------------------------------------------------
	// Other tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testEqualsWithAnnotations() {
		HtmlSerializer.Builder
			s1 = HtmlSerializer.create(),
			s2 = HtmlSerializer.create().applyAnnotations(B1Config.class),
			s3 = HtmlSerializer.create().applyAnnotations(B1Config.class),
			s4 = HtmlSerializer.create().applyAnnotations(B2Config.class);
		assertFalse(s1.hashKey().equals(s2.hashKey()));
		assertFalse(s1.hashKey().equals(s4.hashKey()));
		assertTrue(s2.hashKey().equals(s3.hashKey()));
	}

	@Html(on="B1", format=HtmlFormat.XML)
	private static class B1Config {}

	public static class B1 {}

	@Html(on="B2", format=HtmlFormat.HTML)
	private static class B2Config {}

	public static class B2 {}
}