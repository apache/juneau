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

class Area_Test extends TestBase {

	@Test void a01_basicSetters() {
		Area x = area()
			.alt("a")
			.coords("b")
			.download("c")
			.href("d")
			.hreflang("e")
			.rel("f")
			.shape("g")
			.target("h")
			.type("i")
			._class("j")
			.accesskey("k")
			.contenteditable("l")
			.dir("m")
			.hidden("n")
			.id("o")
			.lang("p")
			.onabort("q")
			.onblur("r")
			.oncancel("s")
			.oncanplay("t")
			.oncanplaythrough("u")
			.onchange("v")
			.onclick("w")
			.oncuechange("x")
			.ondblclick("y")
			.ondurationchange("z")
			.onemptied("aa")
			.onended("ab")
			.onerror("ac")
			.onfocus("ad")
			.oninput("ae")
			.oninvalid("af")
			.onkeydown("ag")
			.onkeypress("ah")
			.onkeyup("ai")
			.onload("aj")
			.onloadeddata("ak")
			.onloadedmetadata("al")
			.onloadstart("am")
			.onmousedown("an")
			.onmouseenter("ao")
			.onmouseleave("ap")
			.onmousemove("aq")
			.onmouseout("ar")
			.onmouseover("as")
			.onmouseup("at")
			.onmousewheel("au")
			.onpause("av")
			.onplay("aw")
			.onplaying("ax")
			.onprogress("ay")
			.onratechange("az")
			.onreset("ba")
			.onresize("bb")
			.onscroll("bc")
			.onseeked("bd")
			.onseeking("be")
			.onselect("bf")
			.onshow("bg")
			.onstalled("bh")
			.onsubmit("bi")
			.onsuspend("bj")
			.ontimeupdate("bk")
			.ontoggle("bl")
			.onvolumechange("bm")
			.onwaiting("bn")
			.spellcheck("bo")
			.style("bp")
			.tabindex("bq")
			.title("br")
			.translate("bs");

		assertString(
			"<area alt='a' coords='b' download='c' href='d' hreflang='e' rel='f' shape='g' target='h' type='i' class='j' accesskey='k' contenteditable='l' dir='m' hidden='n' id='o' lang='p' onabort='q' onblur='r' oncancel='s' oncanplay='t' oncanplaythrough='u' onchange='v' onclick='w' oncuechange='x' ondblclick='y' ondurationchange='z' onemptied='aa' onended='ab' onerror='ac' onfocus='ad' oninput='ae' oninvalid='af' onkeydown='ag' onkeypress='ah' onkeyup='ai' onload='aj' onloadeddata='ak' onloadedmetadata='al' onloadstart='am' onmousedown='an' onmouseenter='ao' onmouseleave='ap' onmousemove='aq' onmouseout='ar' onmouseover='as' onmouseup='at' onmousewheel='au' onpause='av' onplay='aw' onplaying='ax' onprogress='ay' onratechange='az' onreset='ba' onresize='bb' onscroll='bc' onseeked='bd' onseeking='be' onselect='bf' onshow='bg' onstalled='bh' onsubmit='bi' onsuspend='bj' ontimeupdate='bk' ontoggle='bl' onvolumechange='bm' onwaiting='bn' spellcheck='bo' style='bp' tabindex='bq' title='br' translate='bs'/>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<area/>", area());
	}

	@Test void a03_otherConstructors() {
		Area x1 = new Area("a", "b", "c");
		assertString("<area shape='a' coords='b' href='c'/>", x1);

	}
}
