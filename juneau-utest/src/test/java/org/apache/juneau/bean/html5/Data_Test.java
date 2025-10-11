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

class Data_Test extends TestBase {

	@Test void a01_basicSetters() {
		Data x = data()
			.value("a")
			._class("b")
			.accesskey("c")
			.contenteditable("d")
			.dir("e")
			.hidden("f")
			.id("g")
			.lang("h")
			.onabort("i")
			.onblur("j")
			.oncancel("k")
			.oncanplay("l")
			.oncanplaythrough("m")
			.onchange("n")
			.onclick("o")
			.oncuechange("p")
			.ondblclick("q")
			.ondurationchange("r")
			.onemptied("s")
			.onended("t")
			.onerror("u")
			.onfocus("v")
			.oninput("w")
			.oninvalid("x")
			.onkeydown("y")
			.onkeypress("z")
			.onkeyup("aa")
			.onload("ab")
			.onloadeddata("ac")
			.onloadedmetadata("ad")
			.onloadstart("ae")
			.onmousedown("af")
			.onmouseenter("ag")
			.onmouseleave("ah")
			.onmousemove("ai")
			.onmouseout("aj")
			.onmouseover("ak")
			.onmouseup("al")
			.onmousewheel("am")
			.onpause("an")
			.onplay("ao")
			.onplaying("ap")
			.onprogress("aq")
			.onratechange("ar")
			.onreset("as")
			.onresize("at")
			.onscroll("au")
			.onseeked("av")
			.onseeking("aw")
			.onselect("ax")
			.onshow("ay")
			.onstalled("az")
			.onsubmit("ba")
			.onsuspend("bb")
			.ontimeupdate("bc")
			.ontoggle("bd")
			.onvolumechange("be")
			.onwaiting("bf")
			.spellcheck("bg")
			.style("bh")
			.tabindex("bi")
			.title("bj")
			.translate("bk")
			.children("bl", strong("bm"));

		assertString(
			"<data value='a' class='b' accesskey='c' contenteditable='d' dir='e' hidden='f' id='g' lang='h' onabort='i' onblur='j' oncancel='k' oncanplay='l' oncanplaythrough='m' onchange='n' onclick='o' oncuechange='p' ondblclick='q' ondurationchange='r' onemptied='s' onended='t' onerror='u' onfocus='v' oninput='w' oninvalid='x' onkeydown='y' onkeypress='z' onkeyup='aa' onload='ab' onloadeddata='ac' onloadedmetadata='ad' onloadstart='ae' onmousedown='af' onmouseenter='ag' onmouseleave='ah' onmousemove='ai' onmouseout='aj' onmouseover='ak' onmouseup='al' onmousewheel='am' onpause='an' onplay='ao' onplaying='ap' onprogress='aq' onratechange='ar' onreset='as' onresize='at' onscroll='au' onseeked='av' onseeking='aw' onselect='ax' onshow='ay' onstalled='az' onsubmit='ba' onsuspend='bb' ontimeupdate='bc' ontoggle='bd' onvolumechange='be' onwaiting='bf' spellcheck='bg' style='bh' tabindex='bi' title='bj' translate='bk'>bl<strong>bm</strong></data>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<data></data>", data());
	}

	@Test void a03_otherConstructors() {
		Data x1 = new Data("a", "b");
		assertString("<data value='a'>b</data>", x1);

	}
}