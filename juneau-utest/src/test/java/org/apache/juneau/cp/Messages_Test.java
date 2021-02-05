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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import static java.util.Locale.*;

import org.apache.juneau.cp.test3.*;
import org.apache.juneau.cp.test4.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Messages_Test {

	@Test
	public void a01_sameDirectory() throws Exception {
		Messages x1 = Messages.of(MessageBundleTest1.class);
		assertString(x1.getString("file")).is("MessageBundleTest1.properties");
		assertString(x1.forLocale(JAPANESE).getString("file")).is("MessageBundleTest1_ja.properties");
		assertString(x1.forLocale(JAPAN).getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x1.forLocale(CHINA).getString("file")).is("MessageBundleTest1.properties");
		assertString(x1.forLocale((Locale)null).getString("file")).is("MessageBundleTest1.properties");
	}

	@Test
	public void a02_customName() throws Exception {
		Messages x1 = Messages.of(MessageBundleTest1.class, "files/Test1");
		assertString(x1.getString("file")).is("files/Test1.properties");
		assertString(x1.forLocale(JAPANESE).getString("file")).is("files/Test1_ja.properties");
		assertString(x1.forLocale(JAPAN).getString("file")).is("files/Test1_ja_JP.properties");
		assertString(x1.forLocale(CHINA).getString("file")).is("files/Test1.properties");
		assertString(x1.forLocale((Locale)null).getString("file")).is("files/Test1.properties");

		Messages x2 = Messages.create(MessageBundleTest1.class).name(null).build();
		assertString(x2.getString("file")).is("MessageBundleTest1.properties");
		assertString(x2.forLocale(JAPANESE).getString("file")).is("MessageBundleTest1_ja.properties");
		assertString(x2.forLocale(JAPAN).getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x2.forLocale(CHINA).getString("file")).is("MessageBundleTest1.properties");
		assertString(x2.forLocale((Locale)null).getString("file")).is("MessageBundleTest1.properties");
	}

	@Test
	public void a03_customSearchPaths() throws Exception {
		Messages x = Messages.create(MessageBundleTest1.class).name("Test1").baseNames("{package}.files.{name}").build();
		assertString(x.getString("file")).is("files/Test1.properties");
		assertString(x.forLocale(JAPANESE).getString("file")).is("files/Test1_ja.properties");
		assertString(x.forLocale(JAPAN).getString("file")).is("files/Test1_ja_JP.properties");
		assertString(x.forLocale(CHINA).getString("file")).is("files/Test1.properties");
		assertString(x.forLocale((Locale)null).getString("file")).is("files/Test1.properties");

		Messages x2 = Messages.create(MessageBundleTest1.class).name("Test1").baseNames((String[])null).build();
		assertString(x2.getString("file")).is("{!file}");
	}

	@Test
	public void a04_customLocale() throws Exception {
		Messages x1 = Messages.create(MessageBundleTest1.class).locale(Locale.JAPAN).build();
		assertString(x1.getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x1.forLocale(JAPANESE).getString("file")).is("MessageBundleTest1_ja.properties");
		assertString(x1.forLocale(JAPAN).getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x1.forLocale(CHINA).getString("file")).is("MessageBundleTest1.properties");
		assertString(x1.forLocale((Locale)null).getString("file")).is("MessageBundleTest1.properties");

		Messages x2 = Messages.create(MessageBundleTest1.class).locale((Locale)null).build();
		assertString(x2.getString("file")).is("MessageBundleTest1.properties");
		assertString(x2.forLocale(JAPANESE).getString("file")).is("MessageBundleTest1_ja.properties");
		assertString(x2.forLocale(JAPAN).getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x2.forLocale(CHINA).getString("file")).is("MessageBundleTest1.properties");
		assertString(x2.forLocale((Locale)null).getString("file")).is("MessageBundleTest1.properties");
	}

	@Test
	public void a05_nonExistentBundle() throws Exception {
		Messages x1 = Messages.of(MessageBundleTest1.class, "Bad");
		assertString(x1.getString("file")).is("{!file}");
		assertString(x1.forLocale(JAPANESE).getString("file")).is("{!file}");
		assertString(x1.forLocale(JAPAN).getString("file")).is("{!file}");
		assertString(x1.forLocale(CHINA).getString("file")).is("{!file}");
		assertString(x1.forLocale((Locale)null).getString("file")).is("{!file}");

		Messages x2 = x1.forLocale(JAPANESE);
		assertString(x2.getString("file")).is("{!file}");
	}

	@Test
	public void a06_parent() throws Exception {
		Messages x1 = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertString(x1.getString("file")).is("Test2.properties");
		assertString(x1.forLocale(JAPANESE).getString("file")).is("Test2_ja.properties");
		assertString(x1.forLocale(JAPAN).getString("file")).is("Test2_ja_JP.properties");
		assertString(x1.forLocale(CHINA).getString("file")).is("Test2.properties");
		assertString(x1.forLocale((Locale)null).getString("file")).is("Test2.properties");

		Messages x2 = Messages.create(MessageBundleTest1.class).parent(Messages.of(Test2.class)).build();
		assertString(x2.getString("file")).is("MessageBundleTest1.properties");
		assertString(x2.getString("yyy")).is("bar");
	}

	@Test
	public void a07_nonExistentMessage() throws Exception {
		Messages x = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertString(x.getString("bad")).is("{!bad}");
	}

	@Test
	public void a08_nonExistentMessage() throws Exception {
		Messages x = Messages.create(MessageBundleTest1.class).name("Bad").parent(Messages.of(Test2.class)).build();
		assertString(x.getString("bad")).is("{!bad}");
	}

	@Test
	public void a09_keySet_prefix() throws Exception {
		Messages x = Messages.of(MessageBundleTest1.class);
		assertObject(new TreeSet<>(x.keySet("xx"))).asJson().is("['xx','xx.','xx.foo']");
	}

	@Test
	public void a10_getString() throws Exception {
		Messages x = Messages.of(MessageBundleTest1.class);
		assertString(x.getString("foo","bar")).is("foo bar");
		assertString(x.getString("bar","bar")).is("bar bar");
		assertString(x.getString("baz","bar")).is("{!baz}");
		assertString(x.forLocale(JAPAN).getString("foo","bar")).is("fooja bar");
		assertString(x.forLocale(CHINA).getString("foo","bar")).is("foo bar");
		assertString(x.forLocale(null).getString("foo","bar")).is("foo bar");
		assertString(x.forLocale(JAPAN).getString("baz")).is("baz");
		assertString(x.forLocale(CHINA).getString("baz")).is("{!baz}");
		assertString(x.forLocale(null).getString("baz")).is("{!baz}");
	}

	@Test
	public void a11_findFirstString() throws Exception {
		Messages x = Messages.of(MessageBundleTest1.class);
		assertString(x.findFirstString("baz","foo")).is("foo {0}");
		assertString(x.findFirstString("baz","baz")).isNull();
		assertString(x.forLocale(JAPAN).findFirstString("baz","foo")).is("baz");
		assertString(x.forLocale(CHINA).findFirstString("baz","baz")).isNull();
		assertString(x.forLocale(null).findFirstString("baz","baz")).isNull();
	}

	@Test
	public void a12_getKeys() throws Exception {
		Messages x = Messages.of(Test2.class);
		assertObject(x.getKeys()).asJson().is("['file','yyy']");
	}

	@Test
	public void a13_toString() throws Exception {
		Messages x = Messages.of(Test2.class);
		assertString(x).is("{file:'Test2.properties',yyy:'bar'}");
	}
}
