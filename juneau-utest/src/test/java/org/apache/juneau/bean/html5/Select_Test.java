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

class Select_Test extends TestBase {

	@Test void a01_basicSetters() {
		Select x = select()
			.autofocus("a")
			.disabled("b")
			.form("c")
			.multiple("d")
			.name("e")
			.required("f")
			.size("g")
			._class("h")
			.accesskey("i")
			.contenteditable("j")
			.dir("k")
			.hidden("l")
			.id("m")
			.lang("n")
			.onabort("o")
			.onblur("p")
			.oncancel("q")
			.oncanplay("r")
			.oncanplaythrough("s")
			.onchange("t")
			.onclick("u")
			.oncuechange("v")
			.ondblclick("w")
			.ondurationchange("x")
			.onemptied("y")
			.onended("y")
			.onerror("aa")
			.onfocus("ab")
			.oninput("ac")
			.oninvalid("ad")
			.onkeydown("ae")
			.onkeypress("af")
			.onkeyup("ag")
			.onload("ah")
			.onloadeddata("ai")
			.onloadedmetadata("aj")
			.onloadstart("ak")
			.onmousedown("al")
			.onmouseenter("am")
			.onmouseleave("an")
			.onmousemove("ao")
			.onmouseout("ap")
			.onmouseover("aq")
			.onmouseup("ar")
			.onmousewheel("as")
			.onpause("at")
			.onplay("au")
			.onplaying("av")
			.onprogress("aw")
			.onratechange("ax")
			.onreset("ay")
			.onresize("az")
			.onscroll("ba")
			.onseeked("bb")
			.onseeking("bc")
			.onselect("bd")
			.onshow("be")
			.onstalled("bf")
			.onsubmit("bg")
			.onsuspend("bh")
			.ontimeupdate("bi")
			.ontoggle("bj")
			.onvolumechange("bk")
			.onwaiting("bl")
			.spellcheck("bm")
			.style("bn")
			.tabindex("bo")
			.title("bp")
			.translate("bq")
			.child("child1")
			.children("bs", strong("bs"));

		assertString(
			"<select autofocus='a' disabled='b' form='c' multiple='d' name='e' required='f' size='g' class='h' accesskey='i' contenteditable='j' dir='k' hidden='l' id='m' lang='n' onabort='o' onblur='p' oncancel='q' oncanplay='r' oncanplaythrough='s' onchange='t' onclick='u' oncuechange='v' ondblclick='w' ondurationchange='x' onemptied='y' onended='y' onerror='aa' onfocus='ab' oninput='ac' oninvalid='ad' onkeydown='ae' onkeypress='af' onkeyup='ag' onload='ah' onloadeddata='ai' onloadedmetadata='aj' onloadstart='ak' onmousedown='al' onmouseenter='am' onmouseleave='an' onmousemove='ao' onmouseout='ap' onmouseover='aq' onmouseup='ar' onmousewheel='as' onpause='at' onplay='au' onplaying='av' onprogress='aw' onratechange='ax' onreset='ay' onresize='az' onscroll='ba' onseeked='bb' onseeking='bc' onselect='bd' onshow='be' onstalled='bf' onsubmit='bg' onsuspend='bh' ontimeupdate='bi' ontoggle='bj' onvolumechange='bk' onwaiting='bl' spellcheck='bm' style='bn' tabindex='bo' title='bp' translate='bq'>child1bs<strong>bs</strong></select>",
			x
		);
	}

	@Test void a02_emptyBean() {
		assertString("<select></select>", select());
	}

	@Test void a03_otherConstructors() {
		var x1 = new Select("a", "b1", strong("b2"));
		assertString("<select name='a'>b1<strong>b2</strong></select>", x1);

	}

	@Test void a04_choose_withValue() {
		Select x = select("test",
			option("val1", "Option 1"),
			option("val2", "Option 2"),
			option("val3", "Option 3")
		).choose("val2");

		assertString("<select name='test'><option value='val1'>Option 1</option><option value='val2' selected='selected'>Option 2</option><option value='val3'>Option 3</option></select>", x);
	}

	@Test void a04_choose_withNull() {
		Select x = select("test",
			option("val1", "Option 1"),
			option("val2", "Option 2")
		).choose(null);

		assertString("<select name='test'><option value='val1'>Option 1</option><option value='val2'>Option 2</option></select>", x);
	}

	@Test void a04_choose_withNonMatchingValue() {
		Select x = select("test",
			option("val1", "Option 1"),
			option("val2", "Option 2")
		).choose("val3");

		assertString("<select name='test'><option value='val1'>Option 1</option><option value='val2'>Option 2</option></select>", x);
	}

	@Test void a04_choose_withNonOptionChildren() {
		Select x = select("test",
			option("val1", "Option 1"),
			"plain text",
			div("not an option")
		).choose("val1");

		assertString("<select name='test'><option value='val1' selected='selected'>Option 1</option>plain text<div>not an option</div></select>", x);
	}
}