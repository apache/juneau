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
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class A_Test extends TestBase {

	@Test void a01_basicSetters() {
		A x = a()
			._class("a")
			.accesskey("b")
			.children("c1", strong("c2"))
			.contenteditable("d")
			.dir("e")
			.download("f")
			.hidden("g")
			.href("h")
			.hreflang("i")
			.id("j")
			.lang("k")
			.onclick("l")
			.onfocus("m")
			.onmouseover("n")
			.rel("o")
			.spellcheck("p")
			.style("q")
			.tabindex("r")
			.target("s")
			.title("t")
			.translate("u")
			.type("v");

		assertString(
			"<a class='a' accesskey='b' contenteditable='d' dir='e' download='f' hidden='g' href='h' hreflang='i' id='j' lang='k' onclick='l' onfocus='m' onmouseover='n' rel='o' spellcheck='p' style='q' tabindex='r' target='s' title='t' translate='u' type='v'>c1<strong>c2</strong></a>",
			x
		);
	}

	@Test @Disabled void a02_emptyBean() {
		assertString("<a></a>", a());
	}
}
