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
package org.apache.juneau.dto.html5;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Html5BasicTest {

	@Test
	public void testToString() {
		Form f = form("bar",
			fieldset(
				legend("foo:"),
				"Name:", input("text"), br(),
				"Email:", input("text"), br(),
				"X:", keygen().name("X"),
				label("label")._for("Name")
			)
		);

		String r = f.toString();
		assertEquals("<form action='bar'><fieldset><legend>foo:</legend>Name:<input type='text'/><br/>Email:<input type='text'/><br/>X:<keygen name='X'/><label for='Name'>label</label></fieldset></form>", r);
	}
}
