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

class Track_Test extends TestBase {

	@Test void a01_basicSetters() {
		Track x = track()
			._default("a")
			.kind("b")
			.label("c")
			.src("d")
			.srclang("e")
			._class("f")
			.accesskey("g")
			.contenteditable("h")
			.dir("i")
			.hidden("j")
			.id("k")
			.lang("l")
			.onabort("m")
			.onblur("n")
			.oncancel("o")
			.oncanplay("p")
			.oncanplaythrough("q")
			.onchange("r")
			.onclick("s")
			.oncuechange("t")
			.ondblclick("u")
			.ondurationchange("v")
			.onemptied("w")
			.onended("x")
			.onerror("y")
			.onfocus("z")
			.oninput("aa")
			.oninvalid("ab")
			.onkeydown("ac")
			.onkeypress("ad")
			.onkeyup("ae")
			.onload("af")
			.onloadeddata("ag")
			.onloadedmetadata("ah")
			.onloadstart("ai")
			.onmousedown("aj")
			.onmouseenter("ak")
			.onmouseleave("al")
			.onmousemove("am")
			.onmouseout("an")
			.onmouseover("ao")
			.onmouseup("ap")
			.onmousewheel("aq")
			.onpause("ar")
			.onplay("as")
			.onplaying("at")
			.onprogress("au")
			.onratechange("av")
			.onreset("aw")
			.onresize("ax")
			.onscroll("ay")
			.onseeked("az")
			.onseeking("ba")
			.onselect("bb")
			.onshow("bc")
			.onstalled("bd")
			.onsubmit("be")
			.onsuspend("bf")
			.ontimeupdate("bg")
			.ontoggle("bh")
			.onvolumechange("bi")
			.onwaiting("bj")
			.spellcheck("bk")
			.style("bl")
			.tabindex("bm")
			.title("bn")
			.translate("bo");

		assertString(
			"<track default='a' kind='b' label='c' src='d' srclang='e' class='f' accesskey='g' contenteditable='h' dir='i' hidden='j' id='k' lang='l' onabort='m' onblur='n' oncancel='o' oncanplay='p' oncanplaythrough='q' onchange='r' onclick='s' oncuechange='t' ondblclick='u' ondurationchange='v' onemptied='w' onended='x' onerror='y' onfocus='z' oninput='aa' oninvalid='ab' onkeydown='ac' onkeypress='ad' onkeyup='ae' onload='af' onloadeddata='ag' onloadedmetadata='ah' onloadstart='ai' onmousedown='aj' onmouseenter='ak' onmouseleave='al' onmousemove='am' onmouseout='an' onmouseover='ao' onmouseup='ap' onmousewheel='aq' onpause='ar' onplay='as' onplaying='at' onprogress='au' onratechange='av' onreset='aw' onresize='ax' onscroll='ay' onseeked='az' onseeking='ba' onselect='bb' onshow='bc' onstalled='bd' onsubmit='be' onsuspend='bf' ontimeupdate='bg' ontoggle='bh' onvolumechange='bi' onwaiting='bj' spellcheck='bk' style='bl' tabindex='bm' title='bn' translate='bo'/>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<track/>", track());
	}

	@Test void a03_otherConstructors() {
		Track x1 = new Track("a", "b");
		assertString("<track src='a' kind='b'/>", x1);

	}
}