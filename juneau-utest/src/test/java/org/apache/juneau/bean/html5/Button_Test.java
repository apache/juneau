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

class Button_Test extends TestBase {

	@Test void a01_basicSetters() {
		Button x = button()
			.autofocus("a")
			.disabled("b")
			.form("c")
			.formaction("d")
			.formenctype("e")
			.formmethod("f")
			.formnovalidate("g")
			.formtarget("h")
			.menu("i")
			.name("j")
			.type("k")
			.value("l")
			._class("m")
			.accesskey("n")
			.contenteditable("o")
			.dir("p")
			.hidden("q")
			.id("r")
			.lang("s")
			.onabort("t")
			.onblur("u")
			.oncancel("v")
			.oncanplay("w")
			.oncanplaythrough("x")
			.onchange("y")
			.onclick("z")
			.oncuechange("aa")
			.ondblclick("ab")
			.ondurationchange("ac")
			.onemptied("ad")
			.onended("ae")
			.onerror("af")
			.onfocus("ag")
			.oninput("ah")
			.oninvalid("ai")
			.onkeydown("aj")
			.onkeypress("ak")
			.onkeyup("al")
			.onload("am")
			.onloadeddata("an")
			.onloadedmetadata("ao")
			.onloadstart("ap")
			.onmousedown("aq")
			.onmouseenter("ar")
			.onmouseleave("as")
			.onmousemove("at")
			.onmouseout("au")
			.onmouseover("av")
			.onmouseup("aw")
			.onmousewheel("ax")
			.onpause("ay")
			.onplay("az")
			.onplaying("ba")
			.onprogress("bb")
			.onratechange("bc")
			.onreset("bd")
			.onresize("be")
			.onscroll("bf")
			.onseeked("bg")
			.onseeking("bh")
			.onselect("bi")
			.onshow("bj")
			.onstalled("bk")
			.onsubmit("bl")
			.onsuspend("bm")
			.ontimeupdate("bn")
			.ontoggle("bo")
			.onvolumechange("bp")
			.onwaiting("bq")
			.spellcheck("br")
			.style("bs")
			.tabindex("bt")
			.title("bu")
			.translate("bv")
			.children("bw", strong("bx"));

		assertString(
			"<button autofocus='a' disabled='b' form='c' formaction='d' formenctype='e' formmethod='f' formnovalidate='g' formtarget='h' menu='i' name='j' type='k' value='l' class='m' accesskey='n' contenteditable='o' dir='p' hidden='q' id='r' lang='s' onabort='t' onblur='u' oncancel='v' oncanplay='w' oncanplaythrough='x' onchange='y' onclick='z' oncuechange='aa' ondblclick='ab' ondurationchange='ac' onemptied='ad' onended='ae' onerror='af' onfocus='ag' oninput='ah' oninvalid='ai' onkeydown='aj' onkeypress='ak' onkeyup='al' onload='am' onloadeddata='an' onloadedmetadata='ao' onloadstart='ap' onmousedown='aq' onmouseenter='ar' onmouseleave='as' onmousemove='at' onmouseout='au' onmouseover='av' onmouseup='aw' onmousewheel='ax' onpause='ay' onplay='az' onplaying='ba' onprogress='bb' onratechange='bc' onreset='bd' onresize='be' onscroll='bf' onseeked='bg' onseeking='bh' onselect='bi' onshow='bj' onstalled='bk' onsubmit='bl' onsuspend='bm' ontimeupdate='bn' ontoggle='bo' onvolumechange='bp' onwaiting='bq' spellcheck='br' style='bs' tabindex='bt' title='bu' translate='bv'>bw<strong>bx</strong></button>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<button></button>", button());
	}

	@Test void a03_otherConstructors() {
		Button x1 = new Button("a");
		assertString("<button type='a'></button>", x1);

		Button x2 = new Button("a", "b1", strong("b2"));
		assertString("<button type='a'>b1<strong>b2</strong></button>", x2);

	}
}
