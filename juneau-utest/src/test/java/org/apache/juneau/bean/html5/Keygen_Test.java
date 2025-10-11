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

class Keygen_Test extends TestBase {

	@Test void a01_basicSetters() {
		Keygen x = keygen()
			.autofocus("a")
			.challenge("b")
			.disabled("c")
			.form("d")
			.keytype("e")
			.name("f")
			._class("g")
			.accesskey("h")
			.contenteditable("i")
			.dir("j")
			.hidden("k")
			.id("l")
			.lang("m")
			.onabort("n")
			.onblur("o")
			.oncancel("p")
			.oncanplay("q")
			.oncanplaythrough("r")
			.onchange("s")
			.onclick("t")
			.oncuechange("u")
			.ondblclick("v")
			.ondurationchange("w")
			.onemptied("x")
			.onended("y")
			.onerror("z")
			.onfocus("aa")
			.oninput("ab")
			.oninvalid("ac")
			.onkeydown("ad")
			.onkeypress("ae")
			.onkeyup("af")
			.onload("ag")
			.onloadeddata("ah")
			.onloadedmetadata("ai")
			.onloadstart("aj")
			.onmousedown("ak")
			.onmouseenter("al")
			.onmouseleave("am")
			.onmousemove("an")
			.onmouseout("ao")
			.onmouseover("ap")
			.onmouseup("aq")
			.onmousewheel("ar")
			.onpause("as")
			.onplay("at")
			.onplaying("au")
			.onprogress("av")
			.onratechange("aw")
			.onreset("ax")
			.onresize("ay")
			.onscroll("az")
			.onseeked("ba")
			.onseeking("bb")
			.onselect("bc")
			.onshow("bd")
			.onstalled("be")
			.onsubmit("bf")
			.onsuspend("bg")
			.ontimeupdate("bh")
			.ontoggle("bi")
			.onvolumechange("bj")
			.onwaiting("bk")
			.spellcheck("bl")
			.style("bm")
			.tabindex("bn")
			.title("bo")
			.translate("bp");

		assertString(
			"<keygen autofocus='a' challenge='b' disabled='c' form='d' keytype='e' name='f' class='g' accesskey='h' contenteditable='i' dir='j' hidden='k' id='l' lang='m' onabort='n' onblur='o' oncancel='p' oncanplay='q' oncanplaythrough='r' onchange='s' onclick='t' oncuechange='u' ondblclick='v' ondurationchange='w' onemptied='x' onended='y' onerror='z' onfocus='aa' oninput='ab' oninvalid='ac' onkeydown='ad' onkeypress='ae' onkeyup='af' onload='ag' onloadeddata='ah' onloadedmetadata='ai' onloadstart='aj' onmousedown='ak' onmouseenter='al' onmouseleave='am' onmousemove='an' onmouseout='ao' onmouseover='ap' onmouseup='aq' onmousewheel='ar' onpause='as' onplay='at' onplaying='au' onprogress='av' onratechange='aw' onreset='ax' onresize='ay' onscroll='az' onseeked='ba' onseeking='bb' onselect='bc' onshow='bd' onstalled='be' onsubmit='bf' onsuspend='bg' ontimeupdate='bh' ontoggle='bi' onvolumechange='bj' onwaiting='bk' spellcheck='bl' style='bm' tabindex='bn' title='bo' translate='bp'/>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<keygen/>", keygen());
	}
}