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

class Canvas_Test extends TestBase {

	@Test void a01_basicSetters() {
		Canvas x = canvas()
			.height("a")
			.width("b")
			._class("c")
			.accesskey("d")
			.contenteditable("e")
			.dir("f")
			.hidden("g")
			.id("h")
			.lang("i")
			.onabort("j")
			.onblur("k")
			.oncancel("l")
			.oncanplay("m")
			.oncanplaythrough("n")
			.onchange("o")
			.onclick("p")
			.oncuechange("q")
			.ondblclick("r")
			.ondurationchange("s")
			.onemptied("t")
			.onended("u")
			.onerror("v")
			.onfocus("w")
			.oninput("x")
			.oninvalid("y")
			.onkeydown("z")
			.onkeypress("aa")
			.onkeyup("ab")
			.onload("ac")
			.onloadeddata("ad")
			.onloadedmetadata("ae")
			.onloadstart("af")
			.onmousedown("ag")
			.onmouseenter("ah")
			.onmouseleave("ai")
			.onmousemove("aj")
			.onmouseout("ak")
			.onmouseover("al")
			.onmouseup("am")
			.onmousewheel("an")
			.onpause("ao")
			.onplay("ap")
			.onplaying("aq")
			.onprogress("ar")
			.onratechange("as")
			.onreset("at")
			.onresize("au")
			.onscroll("av")
			.onseeked("aw")
			.onseeking("ax")
			.onselect("ay")
			.onshow("az")
			.onstalled("ba")
			.onsubmit("bb")
			.onsuspend("bc")
			.ontimeupdate("bd")
			.ontoggle("be")
			.onvolumechange("bf")
			.onwaiting("bg")
			.spellcheck("bh")
			.style("bi")
			.tabindex("bj")
			.title("bk")
			.translate("bl")
			.child("child1")
			.children("bm", strong("bn"));

		assertString(
			"<canvas height='a' width='b' class='c' accesskey='d' contenteditable='e' dir='f' hidden='g' id='h' lang='i' onabort='j' onblur='k' oncancel='l' oncanplay='m' oncanplaythrough='n' onchange='o' onclick='p' oncuechange='q' ondblclick='r' ondurationchange='s' onemptied='t' onended='u' onerror='v' onfocus='w' oninput='x' oninvalid='y' onkeydown='z' onkeypress='aa' onkeyup='ab' onload='ac' onloadeddata='ad' onloadedmetadata='ae' onloadstart='af' onmousedown='ag' onmouseenter='ah' onmouseleave='ai' onmousemove='aj' onmouseout='ak' onmouseover='al' onmouseup='am' onmousewheel='an' onpause='ao' onplay='ap' onplaying='aq' onprogress='ar' onratechange='as' onreset='at' onresize='au' onscroll='av' onseeked='aw' onseeking='ax' onselect='ay' onshow='az' onstalled='ba' onsubmit='bb' onsuspend='bc' ontimeupdate='bd' ontoggle='be' onvolumechange='bf' onwaiting='bg' spellcheck='bh' style='bi' tabindex='bj' title='bk' translate='bl'>child1bm<strong>bn</strong></canvas>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<canvas></canvas>", canvas());
	}

	@Test void a03_otherConstructors() {
		var x1 = new Canvas(1, 1);
		assertString("<canvas width='1' height='1'></canvas>", x1);

	}

	@Test void a04_fallbackContent() {
		// Test canvas with text fallback content
		Canvas x1 = canvas(300, 200)
			.id("myCanvas")
			.children("Your browser does not support the canvas element.");
		assertString("<canvas width='300' height='200' id='myCanvas'>Your browser does not support the canvas element.</canvas>", x1);

		// Test canvas with mixed fallback content (text + elements)
		Canvas x2 = canvas(400, 300)
			.id("chartCanvas")
			.children(
				"Your browser does not support canvas. ",
				a("https://example.com/fallback", "View static chart"),
				" instead."
			);
		assertString("<canvas width='400' height='300' id='chartCanvas'>Your browser does not support canvas.<sp> </sp><a href='https://example.com/fallback'>View static chart</a><sp> </sp>instead.</canvas>", x2);
	}
}