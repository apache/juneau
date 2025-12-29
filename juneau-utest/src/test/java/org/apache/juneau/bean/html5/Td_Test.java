/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Td_Test extends TestBase {

	@Test void a01_basicSetters() {
		Td x = td()
			.colspan("a")
			.headers("b")
			.rowspan("c")
			._class("d")
			.accesskey("e")
			.contenteditable("f")
			.dir("g")
			.hidden("h")
			.id("i")
			.lang("j")
			.onabort("k")
			.onblur("l")
			.oncancel("m")
			.oncanplay("n")
			.oncanplaythrough("o")
			.onchange("p")
			.onclick("q")
			.oncuechange("r")
			.ondblclick("s")
			.ondurationchange("t")
			.onemptied("u")
			.onended("v")
			.onerror("w")
			.onfocus("x")
			.oninput("y")
			.oninvalid("z")
			.onkeydown("aa")
			.onkeypress("ab")
			.onkeyup("ac")
			.onload("ad")
			.onloadeddata("ae")
			.onloadedmetadata("af")
			.onloadstart("ag")
			.onmousedown("ah")
			.onmouseenter("ai")
			.onmouseleave("aj")
			.onmousemove("ak")
			.onmouseout("al")
			.onmouseover("am")
			.onmouseup("an")
			.onmousewheel("ao")
			.onpause("ap")
			.onplay("aq")
			.onplaying("ar")
			.onprogress("as")
			.onratechange("at")
			.onreset("au")
			.onresize("av")
			.onscroll("aw")
			.onseeked("ax")
			.onseeking("ay")
			.onselect("az")
			.onshow("ba")
			.onstalled("bb")
			.onsubmit("bc")
			.onsuspend("bd")
			.ontimeupdate("be")
			.ontoggle("bf")
			.onvolumechange("bg")
			.onwaiting("bh")
			.spellcheck("bi")
			.style("bj")
			.tabindex("bk")
			.title("bl")
			.translate("bm")
			.children("bn", strong("bo"));

		assertString(
			"<td colspan='a' headers='b' rowspan='c' class='d' accesskey='e' contenteditable='f' dir='g' hidden='h' id='i' lang='j' onabort='k' onblur='l' oncancel='m' oncanplay='n' oncanplaythrough='o' onchange='p' onclick='q' oncuechange='r' ondblclick='s' ondurationchange='t' onemptied='u' onended='v' onerror='w' onfocus='x' oninput='y' oninvalid='z' onkeydown='aa' onkeypress='ab' onkeyup='ac' onload='ad' onloadeddata='ae' onloadedmetadata='af' onloadstart='ag' onmousedown='ah' onmouseenter='ai' onmouseleave='aj' onmousemove='ak' onmouseout='al' onmouseover='am' onmouseup='an' onmousewheel='ao' onpause='ap' onplay='aq' onplaying='ar' onprogress='as' onratechange='at' onreset='au' onresize='av' onscroll='aw' onseeked='ax' onseeking='ay' onselect='az' onshow='ba' onstalled='bb' onsubmit='bc' onsuspend='bd' ontimeupdate='be' ontoggle='bf' onvolumechange='bg' onwaiting='bh' spellcheck='bi' style='bj' tabindex='bk' title='bl' translate='bm'>bn<strong>bo</strong></td>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<td></td>", td());
	}

	@Test void a03_otherConstructors() {
		var x1 = new Td("a1", strong("a2"));
		assertString("<td>a1<strong>a2</strong></td>", x1);

	}
}