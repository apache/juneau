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

class Address_Test extends TestBase {

	@Test void a01_basicSetters() {
		Address x = address()
			._class("a")
			.accesskey("b")
			.contenteditable("c")
			.dir("d")
			.hidden("e")
			.id("f")
			.lang("g")
			.onabort("h")
			.onblur("i")
			.oncancel("j")
			.oncanplay("k")
			.oncanplaythrough("l")
			.onchange("m")
			.onclick("n")
			.oncuechange("o")
			.ondblclick("p")
			.ondurationchange("q")
			.onemptied("r")
			.onended("s")
			.onerror("t")
			.onfocus("u")
			.oninput("v")
			.oninvalid("w")
			.onkeydown("x")
			.onkeypress("y")
			.onkeyup("z")
			.onload("aa")
			.onloadeddata("ab")
			.onloadedmetadata("ac")
			.onloadstart("ad")
			.onmousedown("ae")
			.onmouseenter("af")
			.onmouseleave("ag")
			.onmousemove("ah")
			.onmouseout("ai")
			.onmouseover("aj")
			.onmouseup("ak")
			.onmousewheel("al")
			.onpause("am")
			.onplay("an")
			.onplaying("ao")
			.onprogress("ap")
			.onratechange("aq")
			.onreset("ar")
			.onresize("as")
			.onscroll("at")
			.onseeked("au")
			.onseeking("av")
			.onselect("aw")
			.onshow("ax")
			.onstalled("ay")
			.onsubmit("az")
			.onsuspend("ba")
			.ontimeupdate("bb")
			.ontoggle("bc")
			.onvolumechange("bd")
			.onwaiting("be")
			.spellcheck("bf")
			.style("bg")
			.tabindex("bh")
			.title("bi")
			.translate("bj")
			.children("bk", strong("bl"));

		assertString(
			"<address class='a' accesskey='b' contenteditable='c' dir='d' hidden='e' id='f' lang='g' onabort='h' onblur='i' oncancel='j' oncanplay='k' oncanplaythrough='l' onchange='m' onclick='n' oncuechange='o' ondblclick='p' ondurationchange='q' onemptied='r' onended='s' onerror='t' onfocus='u' oninput='v' oninvalid='w' onkeydown='x' onkeypress='y' onkeyup='z' onload='aa' onloadeddata='ab' onloadedmetadata='ac' onloadstart='ad' onmousedown='ae' onmouseenter='af' onmouseleave='ag' onmousemove='ah' onmouseout='ai' onmouseover='aj' onmouseup='ak' onmousewheel='al' onpause='am' onplay='an' onplaying='ao' onprogress='ap' onratechange='aq' onreset='ar' onresize='as' onscroll='at' onseeked='au' onseeking='av' onselect='aw' onshow='ax' onstalled='ay' onsubmit='az' onsuspend='ba' ontimeupdate='bb' ontoggle='bc' onvolumechange='bd' onwaiting='be' spellcheck='bf' style='bg' tabindex='bh' title='bi' translate='bj'>bk<strong>bl</strong></address>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<address></address>", address());
	}

	@Test void a03_otherConstructors() {
		Address x1 = new Address("a1", strong("a2"));
		assertString("<address>a1<strong>a2</strong></address>", x1);

	}
}
