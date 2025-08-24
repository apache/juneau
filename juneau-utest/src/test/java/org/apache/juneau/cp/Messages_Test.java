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
package org.apache.juneau.cp;

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.test3.*;
import org.apache.juneau.cp.test4.*;
import org.junit.jupiter.api.*;

class Messages_Test extends SimpleTestBase {

	@Test void a01_sameDirectory() {
		var x1 = Messages.of(MessageBundleTest1.class);
		assertEquals("MessageBundleTest1.properties", x1.getString("file"));
		assertEquals("MessageBundleTest1_ja.properties", x1.forLocale(JAPANESE).getString("file"));
		assertEquals("MessageBundleTest1_ja_JP.properties", x1.forLocale(JAPAN).getString("file"));
		assertEquals("MessageBundleTest1.properties", x1.forLocale(CHINA).getString("file"));
		assertEquals("MessageBundleTest1.properties", x1.forLocale((Locale)null).getString("file"));
	}

	@Test void a02_customName() {
		var x1 = Messages.of(MessageBundleTest1.class, "files/Test1");
		assertEquals("files/Test1.properties", x1.getString("file"));
		assertEquals("files/Test1_ja.properties", x1.forLocale(JAPANESE).getString("file"));
		assertEquals("files/Test1_ja_JP.properties", x1.forLocale(JAPAN).getString("file"));
		assertEquals("files/Test1.properties", x1.forLocale(CHINA).getString("file"));
		assertEquals("files/Test1.properties", x1.forLocale((Locale)null).getString("file"));

		var x2 = Messages.create(MessageBundleTest1.class).name(null).build();
		assertEquals("MessageBundleTest1.properties", x2.getString("file"));
		assertEquals("MessageBundleTest1_ja.properties", x2.forLocale(JAPANESE).getString("file"));
		assertEquals("MessageBundleTest1_ja_JP.properties", x2.forLocale(JAPAN).getString("file"));
		assertEquals("MessageBundleTest1.properties", x2.forLocale(CHINA).getString("file"));
		assertEquals("MessageBundleTest1.properties", x2.forLocale((Locale)null).getString("file"));
	}

	@Test void a03_customSearchPaths() {
		var x = Messages.create(MessageBundleTest1.class).name("Test1").baseNames("{package}.files.{name}").build();
		assertEquals("files/Test1.properties", x.getString("file"));
		assertEquals("files/Test1_ja.properties", x.forLocale(JAPANESE).getString("file"));
		assertEquals("files/Test1_ja_JP.properties", x.forLocale(JAPAN).getString("file"));
		assertEquals("files/Test1.properties", x.forLocale(CHINA).getString("file"));
		assertEquals("files/Test1.properties", x.forLocale((Locale)null).getString("file"));

		var x2 = Messages.create(MessageBundleTest1.class).name("Test1").baseNames((String[])null).build();
		assertEquals("{!file}", x2.getString("file"));
	}

	@Test void a04_customLocale() {
		var x1 = Messages.create(MessageBundleTest1.class).locale(Locale.JAPAN).build();
		assertEquals("MessageBundleTest1_ja_JP.properties", x1.getString("file"));
		assertEquals("MessageBundleTest1_ja.properties", x1.forLocale(JAPANESE).getString("file"));
		assertEquals("MessageBundleTest1_ja_JP.properties", x1.forLocale(JAPAN).getString("file"));
		assertEquals("MessageBundleTest1.properties", x1.forLocale(CHINA).getString("file"));
		assertEquals("MessageBundleTest1.properties", x1.forLocale((Locale)null).getString("file"));

		var x2 = Messages.create(MessageBundleTest1.class).locale((Locale)null).build();
		assertEquals("MessageBundleTest1.properties", x2.getString("file"));
		assertEquals("MessageBundleTest1_ja.properties", x2.forLocale(JAPANESE).getString("file"));
		assertEquals("MessageBundleTest1_ja_JP.properties", x2.forLocale(JAPAN).getString("file"));
		assertEquals("MessageBundleTest1.properties", x2.forLocale(CHINA).getString("file"));
		assertEquals("MessageBundleTest1.properties", x2.forLocale((Locale)null).getString("file"));
	}

	@Test void a05_nonExistentBundle() {
		var x1 = Messages.of(MessageBundleTest1.class, "Bad");
		assertEquals("{!file}", x1.getString("file"));
		assertEquals("{!file}", x1.forLocale(JAPANESE).getString("file"));
		assertEquals("{!file}", x1.forLocale(JAPAN).getString("file"));
		assertEquals("{!file}", x1.forLocale(CHINA).getString("file"));
		assertEquals("{!file}", x1.forLocale((Locale)null).getString("file"));

		Messages x2 = x1.forLocale(JAPANESE);
		assertEquals("{!file}", x2.getString("file"));
	}

	@Test void a06_parent() {
		var x1 = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertEquals("Test2.properties", x1.getString("file"));
		assertEquals("Test2_ja.properties", x1.forLocale(JAPANESE).getString("file"));
		assertEquals("Test2_ja_JP.properties", x1.forLocale(JAPAN).getString("file"));
		assertEquals("Test2.properties", x1.forLocale(CHINA).getString("file"));
		assertEquals("Test2.properties", x1.forLocale((Locale)null).getString("file"));

		var x2 = Messages.create(MessageBundleTest1.class).parent(Messages.of(Test2.class)).build();
		assertEquals("MessageBundleTest1.properties", x2.getString("file"));
		assertEquals("bar", x2.getString("yyy"));
	}

	@Test void a07_nonExistentMessage() {
		var x = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertEquals("{!bad}", x.getString("bad"));
	}

	@Test void a08_nonExistentMessage() {
		var x = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertEquals("{!bad}", x.getString("bad"));
	}

	@Test void a09_keySet_prefix() {
		var x = Messages.of(MessageBundleTest1.class);
		assertJson(new TreeSet<>(x.keySet("xx")), "['xx','xx.','xx.foo']");
	}

	@Test void a10_getString() {
		var x = Messages.of(MessageBundleTest1.class);
		assertEquals("foo bar", x.getString("foo","bar"));
		assertEquals("bar bar", x.getString("bar","bar"));
		assertEquals("{!baz}", x.getString("baz","bar"));
		assertEquals("fooja bar", x.forLocale(JAPAN).getString("foo","bar"));
		assertEquals("foo bar", x.forLocale(CHINA).getString("foo","bar"));
		assertEquals("foo bar", x.forLocale(null).getString("foo","bar"));
		assertEquals("baz", x.forLocale(JAPAN).getString("baz"));
		assertEquals("{!baz}", x.forLocale(CHINA).getString("baz"));
		assertEquals("{!baz}", x.forLocale(null).getString("baz"));
	}

	@Test void a11_findFirstString() {
		var x = Messages.of(MessageBundleTest1.class);
		assertEquals("foo {0}", x.findFirstString("baz","foo"));
		assertNull(x.findFirstString("baz","baz"));
		assertEquals("baz", x.forLocale(JAPAN).findFirstString("baz","foo"));
		assertNull(x.forLocale(CHINA).findFirstString("baz","baz"));
		assertNull(x.forLocale(null).findFirstString("baz","baz"));
	}

	@Test void a12_getKeys() {
		var x = Messages.of(Test2.class);
		assertJson(x.getKeys(), "['file','yyy']");
	}

	@Test void a13_toString() {
		var x = Messages.of(Test2.class);
		assertString("{file:'Test2.properties',yyy:'bar'}", x);
	}
}