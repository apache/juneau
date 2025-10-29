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
package org.apache.juneau.cp;

import static java.util.Locale.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.sub.*;
import org.junit.jupiter.api.*;

public class FileFinder_Test extends TestBase {  // NOSONAR - Needs to be public.

	private static String read(FileFinder ff, String path) throws Exception {
		return toUtf8(ff.getStream(path, null).orElse(null));
	}

	private static String read(FileFinder ff, String path, Locale locale) throws Exception {
		return toUtf8(ff.getStream(path, locale).orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_empty() throws Exception {
		var x = FileFinder
			.create()
			.build();

		assertNull(read(x,"files/test1a"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// File system tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_fileSystem_rootDir() throws Exception {
		var x = FileFinder
			.create()
			.dir(".")
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[home:/files/test1/"+p+"]", read(x,"files/test1/"+p));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x,"files/test1/dir/"+p));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x,"files/test1/dir/dir/"+p));
			assertContains("[home:/files/test1/"+p+"]", read(x," / files/test1/"+p+" / "));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x," / files/test1/dir/"+p+" / "));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x," / files/test1/dir/dir/"+p+" / "));
		}

		var badPatterns = a("files/test1/bad.txt","files/test1/../test1/_a.txt","files/test1/%2E%2E/test1/_a.txt","files/bad.txt",null,"",".","..","%2E","%2E%2E","j.class","k.properties");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void b02_fileSystem_subDir() throws Exception {
		var x = FileFinder
			.create()
			.dir("files")
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[home:/files/test1/"+p+"]", read(x,"test1/"+p));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p));
			assertContains("[home:/files/test1/"+p+"]", read(x,"/test1/"+p+"/"));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x,"/test1/dir/"+p+"/"));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x,"/test1/dir/dir/"+p+"/"));
		}

		var badPatterns = a("test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void b03_fileSystem_localized_flat() throws Exception {
		var x = FileFinder
			.create()
			.dir("files")
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");
		var patterns_ja = a("_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja");
		var patterns_ja_JP = a("_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP");

		for (var i = 0; i < patterns.length; i++) {
			var p = patterns[i];
			var p_ja = patterns_ja[i];
			var p_ja_JP = patterns_ja_JP[i];

			assertContains("[home:/files/test1/"+p+"]", read(x,"test1/"+p,null));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p,null));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p,null));
			assertContains("[home:/files/test1/"+p+"]", read(x,"/test1/"+p+"/",null));
			assertContains("[home:/files/test1/dir/"+p+"]", read(x,"/test1/dir/"+p+"/",null));
			assertContains("[home:/files/test1/dir/dir/"+p+"]", read(x,"/test1/dir/dir/"+p+"/",null));

			assertContains("[home:/files/test1/"+p_ja+"]", read(x,"test1/"+p,JAPANESE));
			assertContains("[home:/files/test1/dir/"+p_ja+"]", read(x,"test1/dir/"+p,JAPANESE));
			assertContains("[home:/files/test1/dir/dir/"+p_ja+"]", read(x,"test1/dir/dir/"+p,JAPANESE));
			assertContains("[home:/files/test1/"+p_ja+"]", read(x,"/test1/"+p+"/",JAPANESE));
			assertContains("[home:/files/test1/dir/"+p_ja+"]", read(x,"/test1/dir/"+p+"/",JAPANESE));
			assertContains("[home:/files/test1/dir/dir/"+p_ja+"]", read(x,"/test1/dir/dir/"+p+"/",JAPANESE));

			assertContains("[home:/files/test1/"+p_ja_JP+"]", read(x,"test1/"+p,JAPAN));
			assertContains("[home:/files/test1/dir/"+p_ja_JP+"]", read(x,"test1/dir/"+p,JAPAN));
			assertContains("[home:/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"test1/dir/dir/"+p,JAPAN));
			assertContains("[home:/files/test1/"+p_ja_JP+"]", read(x,"/test1/"+p+"/",JAPAN));
			assertContains("[home:/files/test1/dir/"+p_ja_JP+"]", read(x,"/test1/dir/"+p+"/",JAPAN));
			assertContains("[home:/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"/test1/dir/dir/"+p+"/",JAPAN));
		}

		var badPatterns = a("test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".","..","j.class","k.properties");

		for (var p : badPatterns) {
			assertNull(read(x,p,null));
			assertNull(read(x,p,JAPANESE));
			assertNull(read(x,p,JAPAN));
		}
	}

	@Test void b04_fileSystem_localized_hierarchical() throws Exception {
		var x = FileFinder
			.create()
			.dir("files/test2")
			.build();

		assertContains("[home:/files/test2/a.txt]", read(x,"a.txt", null));
		assertContains("[home:/files/test2/ja/a.txt]", read(x,"a.txt", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/a.txt]", read(x,"a.txt", JAPAN));
		assertContains("[home:/files/test2/a.txt]", read(x,"/a.txt/", null));
		assertContains("[home:/files/test2/ja/a.txt]", read(x,"/a.txt/", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/a.txt]", read(x,"/a.txt/", JAPAN));

		assertContains("[home:/files/test2/dir/a.txt]", read(x,"dir/a.txt", null));
		assertContains("[home:/files/test2/ja/dir/a.txt]", read(x,"dir/a.txt", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/dir/a.txt]", read(x,"dir/a.txt", JAPAN));
		assertContains("[home:/files/test2/dir/a.txt]", read(x,"/dir/a.txt/", null));
		assertContains("[home:/files/test2/ja/dir/a.txt]", read(x,"/dir/a.txt/", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/dir/a.txt]", read(x,"/dir/a.txt/", JAPAN));

		assertContains("[home:/files/test2/dir/dir/a.txt]", read(x,"dir/dir/a.txt", null));
		assertContains("[home:/files/test2/ja/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPAN));
		assertContains("[home:/files/test2/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", null));
		assertContains("[home:/files/test2/ja/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPANESE));
		assertContains("[home:/files/test2/ja/JP/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPAN));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Classpath tests - Classpath root
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_classpathRoot_rootDir() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/files/test1/"+p+"]", read(x,"files/test1/"+p));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x,"files/test1/dir/"+p));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x,"files/test1/dir/dir/"+p));
			assertContains("[cp:/files/test1/"+p+"]", read(x," / files/test1/"+p+" / "));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x," / files/test1/dir/"+p+" / "));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x," / files/test1/dir/dir/"+p+" / "));
		}

		var badPatterns = a("files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".","..","LocalizedFileStore_Test.class");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void c02_classpathRoot_subdir() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/files/test1/"+p+"]", read(x,"test1/"+p));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p));
			assertContains("[cp:/files/test1/"+p+"]", read(x," / test1/"+p+" / "));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x," / test1/dir/"+p+" / "));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x," / test1/dir/dir/"+p+" / "));
		}

		var badPatterns = a("files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void c03_classpathRoot_localized_flat() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");
		var patterns_ja = a("_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja");
		var patterns_ja_JP = a("_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP");

		for (var i = 0; i < patterns.length; i++) {
			var p = patterns[i];
			var p_ja = patterns_ja[i];
			var p_ja_JP = patterns_ja_JP[i];

			assertContains("[cp:/files/test1/"+p+"]", read(x,"test1/"+p,null));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p,null));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p,null));
			assertContains("[cp:/files/test1/"+p+"]", read(x,"/test1/"+p+"/",null));
			assertContains("[cp:/files/test1/dir/"+p+"]", read(x,"/test1/dir/"+p+"/",null));
			assertContains("[cp:/files/test1/dir/dir/"+p+"]", read(x,"/test1/dir/dir/"+p+"/",null));

			assertContains("[cp:/files/test1/"+p_ja+"]", read(x,"test1/"+p,JAPANESE));
			assertContains("[cp:/files/test1/dir/"+p_ja+"]", read(x,"test1/dir/"+p,JAPANESE));
			assertContains("[cp:/files/test1/dir/dir/"+p_ja+"]", read(x,"test1/dir/dir/"+p,JAPANESE));
			assertContains("[cp:/files/test1/"+p_ja+"]", read(x,"/test1/"+p+"/",JAPANESE));
			assertContains("[cp:/files/test1/dir/"+p_ja+"]", read(x,"/test1/dir/"+p+"/",JAPANESE));
			assertContains("[cp:/files/test1/dir/dir/"+p_ja+"]", read(x,"/test1/dir/dir/"+p+"/",JAPANESE));

			assertContains("[cp:/files/test1/"+p_ja_JP+"]", read(x,"test1/"+p,JAPAN));
			assertContains("[cp:/files/test1/dir/"+p_ja_JP+"]", read(x,"test1/dir/"+p,JAPAN));
			assertContains("[cp:/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"test1/dir/dir/"+p,JAPAN));
			assertContains("[cp:/files/test1/"+p_ja_JP+"]", read(x,"/test1/"+p+"/",JAPAN));
			assertContains("[cp:/files/test1/dir/"+p_ja_JP+"]", read(x,"/test1/dir/"+p+"/",JAPAN));
			assertContains("[cp:/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"/test1/dir/dir/"+p+"/",JAPAN));
		}

		var badPatterns = a("test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p,null));
			assertNull(read(x,p,JAPANESE));
			assertNull(read(x,p,JAPAN));
		}
	}

	@Test void c04_classpathRoot_localized_hierarchical() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files/test2", false)
			.build();

		assertContains("[cp:/files/test2/a.txt]", read(x,"a.txt", null));
		assertContains("[cp:/files/test2/ja/a.txt]", read(x,"a.txt", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/a.txt]", read(x,"a.txt", JAPAN));
		assertContains("[cp:/files/test2/a.txt]", read(x,"/a.txt/", null));
		assertContains("[cp:/files/test2/ja/a.txt]", read(x,"/a.txt/", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/a.txt]", read(x,"/a.txt/", JAPAN));

		assertContains("[cp:/files/test2/dir/a.txt]", read(x,"dir/a.txt", null));
		assertContains("[cp:/files/test2/ja/dir/a.txt]", read(x,"dir/a.txt", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/dir/a.txt]", read(x,"dir/a.txt", JAPAN));
		assertContains("[cp:/files/test2/dir/a.txt]", read(x,"/dir/a.txt/", null));
		assertContains("[cp:/files/test2/ja/dir/a.txt]", read(x,"/dir/a.txt/", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/dir/a.txt]", read(x,"/dir/a.txt/", JAPAN));

		assertContains("[cp:/files/test2/dir/dir/a.txt]", read(x,"dir/dir/a.txt", null));
		assertContains("[cp:/files/test2/ja/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPAN));
		assertContains("[cp:/files/test2/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", null));
		assertContains("[cp:/files/test2/ja/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPANESE));
		assertContains("[cp:/files/test2/ja/JP/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPAN));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Classpath tests - Classpath relative
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01a_classpathRelative_rootDir() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"files/test1/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"files/test1/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"files/test1/dir/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x," / files/test1/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x," / files/test1/dir/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x," / files/test1/dir/dir/"+p+" / "));
		}

		assertContains("[cp:/org/apache/juneau/cp/_a.txt]", read(x,"_a.txt"));

		var badPatterns = a("files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void d01b_classpathRelative_rootDir_recursive() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "", true)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"files/test1/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"files/test1/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"files/test1/dir/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x," / files/test1/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x," / files/test1/dir/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x," / files/test1/dir/dir/"+p+" / "));
		}
	}

	@Test void d02a_classpathRelative_subdir() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"test1/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x," / test1/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x," / test1/dir/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x," / test1/dir/dir/"+p+" / "));
		}

		var badPatterns = a("files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p));
		}
	}

	@Test void d02b_classpathRelative_subdir_recursive() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files", true)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");

		for (var p : patterns) {
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"test1/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x," / test1/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x," / test1/dir/"+p+" / "));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x," / test1/dir/dir/"+p+" / "));
		}
	}

	@Test void d03a_classpathRelative_localized_flat() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files", false)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");
		var patterns_ja = a("_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja");
		var patterns_ja_JP = a("_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP");

		for (var i = 0; i < patterns.length; i++) {
			var p = patterns[i];
			var p_ja = patterns_ja[i];
			var p_ja_JP = patterns_ja_JP[i];

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"test1/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"/test1/"+p+"/",null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"/test1/dir/"+p+"/",null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"/test1/dir/dir/"+p+"/",null));

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]", read(x,"test1/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]", read(x,"test1/dir/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]", read(x,"test1/dir/dir/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]", read(x,"/test1/"+p+"/",JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]", read(x,"/test1/dir/"+p+"/",JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]", read(x,"/test1/dir/dir/"+p+"/",JAPANESE));

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]", read(x,"test1/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]", read(x,"test1/dir/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"test1/dir/dir/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]", read(x,"/test1/"+p+"/",JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]", read(x,"/test1/dir/"+p+"/",JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"/test1/dir/dir/"+p+"/",JAPAN));
		}

		var badPatterns = a("test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".","..");

		for (var p : badPatterns) {
			assertNull(read(x,p,null));
			assertNull(read(x,p,JAPANESE));
			assertNull(read(x,p,JAPAN));
		}
	}

	@Test void d03b_classpathRelative_localized_flat_recursive() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files", true)
			.build();

		var patterns = a("_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo");
		var patterns_ja = a("_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja");
		var patterns_ja_JP = a("_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP");

		for (var i = 0; i < patterns.length; i++) {
			var p = patterns[i];
			var p_ja = patterns_ja[i];
			var p_ja_JP = patterns_ja_JP[i];

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"test1/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"test1/dir/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"test1/dir/dir/"+p,null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]", read(x,"/test1/"+p+"/",null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]", read(x,"/test1/dir/"+p+"/",null));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]", read(x,"/test1/dir/dir/"+p+"/",null));

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]", read(x,"test1/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]", read(x,"test1/dir/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]", read(x,"test1/dir/dir/"+p,JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]", read(x,"/test1/"+p+"/",JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]", read(x,"/test1/dir/"+p+"/",JAPANESE));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]", read(x,"/test1/dir/dir/"+p+"/",JAPANESE));

			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]", read(x,"test1/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]", read(x,"test1/dir/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"test1/dir/dir/"+p,JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]", read(x,"/test1/"+p+"/",JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]", read(x,"/test1/dir/"+p+"/",JAPAN));
			assertContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]", read(x,"/test1/dir/dir/"+p+"/",JAPAN));
		}
	}

	@Test void d04a_classpathRelative_localized_hierarchical() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files/test2", false)
			.build();

		assertContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]", read(x,"a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]", read(x,"a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]", read(x,"a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]", read(x,"/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]", read(x,"/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]", read(x,"/a.txt/", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]", read(x,"dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]", read(x,"dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]", read(x,"dir/a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]", read(x,"/dir/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]", read(x,"/dir/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]", read(x,"/dir/a.txt/", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]", read(x,"dir/dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPAN));
	}

	@Test void d04b_classpathRelative_localized_hierarchical_recursive() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.build();

		assertContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]", read(x,"a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]", read(x,"a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]", read(x,"a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]", read(x,"/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]", read(x,"/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]", read(x,"/a.txt/", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]", read(x,"dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]", read(x,"dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]", read(x,"dir/a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]", read(x,"/dir/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]", read(x,"/dir/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]", read(x,"/dir/a.txt/", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]", read(x,"dir/dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPAN));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]", read(x,"/dir/dir/a.txt/", JAPAN));

		FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(class|properties)")
			.build();
	}

	@Test void d05_classpathRelative_ignorePattern() throws Exception {
		var x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(txt)")
			.build();

		assertNull(read(x,"a.txt", null));
		assertNull(read(x,"a.txt", JAPANESE));
		assertNull(read(x,"a.txt", JAPAN));

		assertNull(read(x,"dir/a.txt", null));
		assertNull(read(x,"dir/a.txt", JAPANESE));
		assertNull(read(x,"dir/a.txt", JAPAN));

		assertNull(read(x,"dir/dir/a.txt", null));
		assertNull(read(x,"dir/dir/a.txt", JAPANESE));
		assertNull(read(x,"dir/dir/a.txt", JAPAN));

		x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(TXT)")
			.build();

		assertNull(read(x,"a.txt", null));
		assertNull(read(x,"a.txt", JAPANESE));
		assertNull(read(x,"a.txt", JAPAN));

		assertNull(read(x,"dir/a.txt", null));
		assertNull(read(x,"dir/a.txt", JAPANESE));
		assertNull(read(x,"dir/a.txt", JAPAN));

		assertNull(read(x,"dir/dir/a.txt", null));
		assertNull(read(x,"dir/dir/a.txt", JAPANESE));
		assertNull(read(x,"dir/dir/a.txt", JAPAN));

		x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude()
			.build();

		assertContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]", read(x,"a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]", read(x,"a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]", read(x,"a.txt", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]", read(x,"dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]", read(x,"dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]", read(x,"dir/a.txt", JAPAN));

		assertContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]", read(x,"dir/dir/a.txt", null));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPANESE));
		assertContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]", read(x,"dir/dir/a.txt", JAPAN));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_localDir_hashCode() {
		var s = set(new LocalDir(Paths.get("test")), new LocalDir(Paths.get("test")));
		assertSize(1, s);

		s = set(new LocalDir(Paths.get("test")), new LocalDir(Paths.get("test","test")));
		assertSize(2, s);

		s = set(new LocalDir(List.class,null), new LocalDir(List.class,null));
		assertSize(1, s);

		s = set(new LocalDir(List.class,null), new LocalDir(List.class,"foo"));
		assertSize(2, s);

		s = set(new LocalDir(List.class,null), new LocalDir(String.class,null));
		assertSize(2, s);
	}

	@Test void e02_caching() throws Exception {
		var x = FileFinder
			.create()
			.dir(".")
			.caching(100_000_000)
			.build();

		assertContains("[home:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));
		assertContains("[home:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));

		x = FileFinder
			.create()
			.dir(".")
			.caching(1)
			.build();

		assertContains("[home:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));
		assertContains("[home:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));

		x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/", false)
			.caching(100_000_000)
			.build();

		assertContains("[cp:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));
		assertContains("[cp:/files/test1/_a.txt]", read(x,"files/test1/_a.txt"));
	}

	@Test void e03_subclassing() {
		var x = E03b
			.create()
			.dir(".")
			.caching(100_000_000)
			.type(E03b.class)
			.build();
		assertInstanceOf(E03b.class, x);
	}

	public static class E03a extends FileFinder.Builder {
		protected E03a() {
			super(BeanStore.INSTANCE);
		}
	}
	public static class E03b extends BasicFileFinder {
		public static E03a create() {
			return new E03a();
		}
		public E03b(E03a x) {
			super(x);
		}
	}
}