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
			.onabort("l")
			.onblur("m")
			.oncancel("n")
			.oncanplay("o")
			.oncanplaythrough("p")
			.onchange("q")
			.onclick("r")
			.oncuechange("s")
			.ondblclick("t")
			.ondurationchange("u")
			.onemptied("v")
			.onended("w")
			.onerror("x")
			.onfocus("y")
			.oninput("z")
			.oninvalid("aa")
			.onkeydown("ab")
			.onkeypress("ac")
			.onkeyup("ad")
			.onload("ae")
			.onloadeddata("af")
			.onloadedmetadata("ag")
			.onloadstart("ah")
			.onmousedown("ai")
			.onmouseenter("aj")
			.onmouseleave("ak")
			.onmousemove("al")
			.onmouseout("am")
			.onmouseover("an")
			.onmouseup("ao")
			.onmousewheel("ap")
			.onpause("aq")
			.onplay("ar")
			.onplaying("as")
			.onprogress("at")
			.onratechange("au")
			.onreset("av")
			.onresize("aw")
			.onscroll("ax")
			.onseeked("ay")
			.onseeking("az")
			.onselect("ba")
			.onshow("bb")
			.onstalled("bc")
			.onsubmit("bd")
			.onsuspend("be")
			.ontimeupdate("bf")
			.ontoggle("bg")
			.onvolumechange("bh")
			.onwaiting("bi")
			.rel("bj")
			.spellcheck("bk")
			.style("bl")
			.tabindex("bm")
			.target("bn")
			.title("bo")
			.translate("bp")
			.type("bq");

		assertString(
			"<a class='a' accesskey='b' contenteditable='d' dir='e' download='f' hidden='g' href='h' hreflang='i' id='j' lang='k' onabort='l' onblur='m' oncancel='n' oncanplay='o' oncanplaythrough='p' onchange='q' onclick='r' oncuechange='s' ondblclick='t' ondurationchange='u' onemptied='v' onended='w' onerror='x' onfocus='y' oninput='z' oninvalid='aa' onkeydown='ab' onkeypress='ac' onkeyup='ad' onload='ae' onloadeddata='af' onloadedmetadata='ag' onloadstart='ah' onmousedown='ai' onmouseenter='aj' onmouseleave='ak' onmousemove='al' onmouseout='am' onmouseover='an' onmouseup='ao' onmousewheel='ap' onpause='aq' onplay='ar' onplaying='as' onprogress='at' onratechange='au' onreset='av' onresize='aw' onscroll='ax' onseeked='ay' onseeking='az' onselect='ba' onshow='bb' onstalled='bc' onsubmit='bd' onsuspend='be' ontimeupdate='bf' ontoggle='bg' onvolumechange='bh' onwaiting='bi' rel='bj' spellcheck='bk' style='bl' tabindex='bm' target='bn' title='bo' translate='bp' type='bq'>c1<strong>c2</strong></a>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<a></a>", a());
	}

	@Test void a03_otherConstructors() {
		A x1 = new A("a", new Object[]{"b1", strong("b2")});
		assertString("<a href='a'>b1<strong>b2</strong></a>", x1);

	}
}
