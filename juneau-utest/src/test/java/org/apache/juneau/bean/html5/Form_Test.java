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

class Form_Test extends TestBase {

	@Test void a01_basicSetters() {
		Form x = form()
			.acceptcharset("a")
			.action("b")
			.autocomplete("c")
			.enctype("d")
			.method("e")
			.name("f")
			.novalidate(true)
			.target("h")
			._class("i")
			.accesskey("j")
			.contenteditable("k")
			.dir("l")
			.hidden("m")
			.id("n")
			.lang("o")
			.onabort("p")
			.onblur("q")
			.oncancel("r")
			.oncanplay("s")
			.oncanplaythrough("t")
			.onchange("u")
			.onclick("v")
			.oncuechange("w")
			.ondblclick("x")
			.ondurationchange("y")
			.onemptied("z")
			.onended("aa")
			.onerror("ab")
			.onfocus("ac")
			.oninput("ad")
			.oninvalid("ae")
			.onkeydown("af")
			.onkeypress("ag")
			.onkeyup("ah")
			.onload("ai")
			.onloadeddata("aj")
			.onloadedmetadata("ak")
			.onloadstart("al")
			.onmousedown("am")
			.onmouseenter("an")
			.onmouseleave("ao")
			.onmousemove("ap")
			.onmouseout("aq")
			.onmouseover("ar")
			.onmouseup("as")
			.onmousewheel("at")
			.onpause("au")
			.onplay("av")
			.onplaying("aw")
			.onprogress("ax")
			.onratechange("ay")
			.onreset("az")
			.onresize("ba")
			.onscroll("bb")
			.onseeked("bc")
			.onseeking("bd")
			.onselect("be")
			.onshow("bf")
			.onstalled("bg")
			.onsubmit("bh")
			.onsuspend("bi")
			.ontimeupdate("bj")
			.ontoggle("bk")
			.onvolumechange("bl")
			.onwaiting("bm")
			.spellcheck("bn")
			.style("bo")
			.tabindex("bp")
			.title("bq")
			.translate("br")
			.children("bs", strong("bt"));

		assertString(
			"<form accept-charset='a' action='b' autocomplete='c' enctype='d' method='e' name='f' novalidate='true' target='h' class='i' accesskey='j' contenteditable='k' dir='l' hidden='m' id='n' lang='o' onabort='p' onblur='q' oncancel='r' oncanplay='s' oncanplaythrough='t' onchange='u' onclick='v' oncuechange='w' ondblclick='x' ondurationchange='y' onemptied='z' onended='aa' onerror='ab' onfocus='ac' oninput='ad' oninvalid='ae' onkeydown='af' onkeypress='ag' onkeyup='ah' onload='ai' onloadeddata='aj' onloadedmetadata='ak' onloadstart='al' onmousedown='am' onmouseenter='an' onmouseleave='ao' onmousemove='ap' onmouseout='aq' onmouseover='ar' onmouseup='as' onmousewheel='at' onpause='au' onplay='av' onplaying='aw' onprogress='ax' onratechange='ay' onreset='az' onresize='ba' onscroll='bb' onseeked='bc' onseeking='bd' onselect='be' onshow='bf' onstalled='bg' onsubmit='bh' onsuspend='bi' ontimeupdate='bj' ontoggle='bk' onvolumechange='bl' onwaiting='bm' spellcheck='bn' style='bo' tabindex='bp' title='bq' translate='br'>bs<strong>bt</strong></form>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<form></form>", form());
	}

	@Test void a03_otherConstructors() {
		Form x1 = new Form("a");
		assertString("<form action='a'></form>", x1);

		Form x2 = new Form("a", "b1", strong("b2"));
		assertString("<form action='a'>b1<strong>b2</strong></form>", x2);

	}
}