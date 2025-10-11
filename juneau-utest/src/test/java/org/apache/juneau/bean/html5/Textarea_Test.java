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

class Textarea_Test extends TestBase {

	@Test void a01_basicSetters() {
		Textarea x = textarea()
			.autocomplete("a")
			.autofocus(true)
			.cols("c")
			.dirname("d")
			.disabled("e")
			.form("f")
			.inputmode("g")
			.maxlength("h")
			.minlength("i")
			.name("j")
			.placeholder("k")
			.readonly("l")
			.required("m")
			.rows(1)
			.wrap("o")
			._class("p")
			.accesskey("q")
			.contenteditable("r")
			.dir("s")
			.hidden("t")
			.id("u")
			.lang("v")
			.onabort("w")
			.onblur("x")
			.oncancel("y")
			.oncanplay("z")
			.oncanplaythrough("aa")
			.onchange("ab")
			.onclick("ac")
			.oncuechange("ad")
			.ondblclick("ae")
			.ondurationchange("af")
			.onemptied("ag")
			.onended("ah")
			.onerror("ai")
			.onfocus("aj")
			.oninput("ak")
			.oninvalid("al")
			.onkeydown("am")
			.onkeypress("an")
			.onkeyup("ao")
			.onload("ap")
			.onloadeddata("aq")
			.onloadedmetadata("ar")
			.onloadstart("as")
			.onmousedown("at")
			.onmouseenter("au")
			.onmouseleave("av")
			.onmousemove("aw")
			.onmouseout("ax")
			.onmouseover("ay")
			.onmouseup("az")
			.onmousewheel("ba")
			.onpause("bb")
			.onplay("bc")
			.onplaying("bd")
			.onprogress("be")
			.onratechange("bf")
			.onreset("bg")
			.onresize("bh")
			.onscroll("bi")
			.onseeked("bj")
			.onseeking("bk")
			.onselect("bl")
			.onshow("bm")
			.onstalled("bn")
			.onsubmit("bo")
			.onsuspend("bp")
			.ontimeupdate("bq")
			.ontoggle("br")
			.onvolumechange("bs")
			.onwaiting("bt")
			.spellcheck("bu")
			.style("bv")
			.tabindex("bw")
			.title("bx")
			.translate("by")
			.text("bz");

		assertString(
			"<textarea autocomplete='a' autofocus='true' cols='c' dirname='d' disabled='e' form='f' inputmode='g' maxlength='h' minlength='i' name='j' placeholder='k' readonly='l' required='m' rows='1' wrap='o' class='p' accesskey='q' contenteditable='r' dir='s' hidden='t' id='u' lang='v' onabort='w' onblur='x' oncancel='y' oncanplay='z' oncanplaythrough='aa' onchange='ab' onclick='ac' oncuechange='ad' ondblclick='ae' ondurationchange='af' onemptied='ag' onended='ah' onerror='ai' onfocus='aj' oninput='ak' oninvalid='al' onkeydown='am' onkeypress='an' onkeyup='ao' onload='ap' onloadeddata='aq' onloadedmetadata='ar' onloadstart='as' onmousedown='at' onmouseenter='au' onmouseleave='av' onmousemove='aw' onmouseout='ax' onmouseover='ay' onmouseup='az' onmousewheel='ba' onpause='bb' onplay='bc' onplaying='bd' onprogress='be' onratechange='bf' onreset='bg' onresize='bh' onscroll='bi' onseeked='bj' onseeking='bk' onselect='bl' onshow='bm' onstalled='bn' onsubmit='bo' onsuspend='bp' ontimeupdate='bq' ontoggle='br' onvolumechange='bs' onwaiting='bt' spellcheck='bu' style='bv' tabindex='bw' title='bx' translate='by'>bz</textarea>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<textarea></textarea>", textarea());
	}

	@Test void a03_otherConstructors() {
		Textarea x1 = new Textarea("a", "b");
		assertString("<textarea name='a'>b</textarea>", x1);

	}
}