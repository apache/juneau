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

class Option_Test extends TestBase {

	@Test void a01_basicSetters() {
		Option x = option()
			.disabled("a")
			.label("b")
			.selected("c")
			.value("d")
			._class("e")
			.accesskey("f")
			.contenteditable("g")
			.dir("h")
			.hidden("i")
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
			.spellcheck("bj")
			.style("bk")
			.tabindex("bl")
			.title("bm")
			.translate("bn")
			.text("bo");

		assertString(
			"<option disabled='a' label='b' selected='c' value='d' class='e' accesskey='f' contenteditable='g' dir='h' hidden='i' id='j' lang='k' onabort='l' onblur='m' oncancel='n' oncanplay='o' oncanplaythrough='p' onchange='q' onclick='r' oncuechange='s' ondblclick='t' ondurationchange='u' onemptied='v' onended='w' onerror='x' onfocus='y' oninput='z' oninvalid='aa' onkeydown='ab' onkeypress='ac' onkeyup='ad' onload='ae' onloadeddata='af' onloadedmetadata='ag' onloadstart='ah' onmousedown='ai' onmouseenter='aj' onmouseleave='ak' onmousemove='al' onmouseout='am' onmouseover='an' onmouseup='ao' onmousewheel='ap' onpause='aq' onplay='ar' onplaying='as' onprogress='at' onratechange='au' onreset='av' onresize='aw' onscroll='ax' onseeked='ay' onseeking='az' onselect='ba' onshow='bb' onstalled='bc' onsubmit='bd' onsuspend='be' ontimeupdate='bf' ontoggle='bg' onvolumechange='bh' onwaiting='bi' spellcheck='bj' style='bk' tabindex='bl' title='bm' translate='bn'>bo</option>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<option></option>", option());
	}

	@Test void a03_otherConstructors() {
		Option x1 = new Option("a");
		assertString("<option>a</option>", x1);

		Option x2 = new Option("a", "b");
		assertString("<option value='a'>b</option>", x2);

	}
}