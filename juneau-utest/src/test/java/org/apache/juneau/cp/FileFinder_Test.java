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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.cp.sub.*;
import static java.util.Locale.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class FileFinder_Test {

	private InputStream stream(FileFinder ff, String path) throws Exception {
		return ff.getStream(path, null).orElse(null);
	}

	private InputStream stream(FileFinder ff, String path, Locale locale) throws Exception {
		return ff.getStream(path, locale).orElse(null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_empty() throws Exception {
		FileFinder x = FileFinder
			.create()
			.build();

		assertBytes(stream(x,"files/test1a")).asString().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// File system tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_fileSystem_rootDir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.dir(".")
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"files/test1/"+p)).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/"+p)).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/dir/"+p)).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/"+p+" / ")).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/"+p+" / ")).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/dir/"+p+" / ")).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");
		}

		String[] badPatterns = {"files/test1/bad.txt","files/test1/../test1/_a.txt","files/test1/%2E%2E/test1/_a.txt","files/bad.txt",null,"",".","..","%2E","%2E%2E","j.class","k.properties"};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).isNull();
		}
	}

	@Test
	public void b02_fileSystem_subDir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.dir("files")
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"test1/"+p)).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p)).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p)).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x,"/test1/"+p+"/")).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/")).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/")).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");
		}

		String[] badPatterns = {"test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).isNull();
		}
	}

	@Test
	public void b03_fileSystem_localized_flat() throws Exception {
		FileFinder x = FileFinder
			.create()
			.dir("files")
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};
		String[] patterns_ja = {"_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja"};
		String[] patterns_ja_JP = {"_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP"};

		for (int i = 0; i < patterns.length; i++) {
			String p = patterns[i], p_ja = patterns_ja[i], p_ja_JP = patterns_ja_JP[i];

			assertBytes(stream(x,"test1/"+p,null)).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p,null)).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,null)).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x,"/test1/"+p+"/",null)).asString().isContains("[home:/files/test1/"+p+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",null)).asString().isContains("[home:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",null)).asString().isContains("[home:/files/test1/dir/dir/"+p+"]");

			assertBytes(stream(x,"test1/"+p,JAPANESE)).asString().isContains("[home:/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPANESE)).asString().isContains("[home:/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPANESE)).asString().isContains("[home:/files/test1/dir/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPANESE)).asString().isContains("[home:/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPANESE)).asString().isContains("[home:/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPANESE)).asString().isContains("[home:/files/test1/dir/dir/"+p_ja+"]");

			assertBytes(stream(x,"test1/"+p,JAPAN)).asString().isContains("[home:/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPAN)).asString().isContains("[home:/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPAN)).asString().isContains("[home:/files/test1/dir/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPAN)).asString().isContains("[home:/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPAN)).asString().isContains("[home:/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPAN)).asString().isContains("[home:/files/test1/dir/dir/"+p_ja_JP+"]");
		}

		String[] badPatterns = {"test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".","..","j.class","k.properties"};

		for (String p : badPatterns) {
			assertBytes(stream(x,p,null)).isNull();
			assertBytes(stream(x,p,JAPANESE)).isNull();
			assertBytes(stream(x,p,JAPAN)).isNull();
		}
	}

	@Test
	public void b04_fileSystem_localized_hierarchical() throws Exception {
		FileFinder x = FileFinder
			.create()
			.dir("files/test2")
			.build();

		assertBytes(stream(x,"a.txt", null)).asString().isContains("[home:/files/test2/a.txt]");
		assertBytes(stream(x,"a.txt", JAPANESE)).asString().isContains("[home:/files/test2/ja/a.txt]");
		assertBytes(stream(x,"a.txt", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/a.txt]");
		assertBytes(stream(x,"/a.txt/", null)).asString().isContains("[home:/files/test2/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPANESE)).asString().isContains("[home:/files/test2/ja/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/a.txt]");

		assertBytes(stream(x,"dir/a.txt", null)).asString().isContains("[home:/files/test2/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).asString().isContains("[home:/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", null)).asString().isContains("[home:/files/test2/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPANESE)).asString().isContains("[home:/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/dir/a.txt]");

		assertBytes(stream(x,"dir/dir/a.txt", null)).asString().isContains("[home:/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).asString().isContains("[home:/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", null)).asString().isContains("[home:/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPANESE)).asString().isContains("[home:/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPAN)).asString().isContains("[home:/files/test2/ja/JP/dir/dir/a.txt]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Classpath tests - Classpath root
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_classpathRoot_rootDir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/", false)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"files/test1/"+p)).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/"+p)).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/dir/"+p)).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/"+p+" / ")).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/"+p+" / ")).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");
		}

		String[] badPatterns = {"files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".","..","LocalizedFileStore_Test.class"};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).setMsg("pattern=[{0}]", p).isNull();
		}
	}

	@Test
	public void c02_classpathRoot_subdir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files", false)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"test1/"+p)).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p)).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p)).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / test1/"+p+" / ")).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x," / test1/dir/"+p+" / ")).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");
		}

		String[] badPatterns = {"files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).isNull();
		}
	}

	@Test
	public void c03_classpathRoot_localized_flat() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files", false)
			.build();


		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};
		String[] patterns_ja = {"_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja"};
		String[] patterns_ja_JP = {"_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP"};

		for (int i = 0; i < patterns.length; i++) {
			String p = patterns[i], p_ja = patterns_ja[i], p_ja_JP = patterns_ja_JP[i];

			assertBytes(stream(x,"test1/"+p,null)).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p,null)).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,null)).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x,"/test1/"+p+"/",null)).asString().isContains("[cp:/files/test1/"+p+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",null)).asString().isContains("[cp:/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",null)).asString().isContains("[cp:/files/test1/dir/dir/"+p+"]");

			assertBytes(stream(x,"test1/"+p,JAPANESE)).asString().isContains("[cp:/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPANESE)).asString().isContains("[cp:/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPANESE)).asString().isContains("[cp:/files/test1/dir/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPANESE)).asString().isContains("[cp:/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/files/test1/dir/dir/"+p_ja+"]");

			assertBytes(stream(x,"test1/"+p,JAPAN)).asString().isContains("[cp:/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPAN)).asString().isContains("[cp:/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPAN)).asString().isContains("[cp:/files/test1/dir/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPAN)).asString().isContains("[cp:/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/files/test1/dir/dir/"+p_ja_JP+"]");
		}

		String[] badPatterns = {"test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p,null)).isNull();
			assertBytes(stream(x,p,JAPANESE)).isNull();
			assertBytes(stream(x,p,JAPAN)).isNull();
		}
	}


	@Test
	public void c04_classpathRoot_localized_hierarchical() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/files/test2", false)
			.build();

		assertBytes(stream(x,"a.txt", null)).asString().isContains("[cp:/files/test2/a.txt]");
		assertBytes(stream(x,"a.txt", JAPANESE)).asString().isContains("[cp:/files/test2/ja/a.txt]");
		assertBytes(stream(x,"a.txt", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/a.txt]");
		assertBytes(stream(x,"/a.txt/", null)).asString().isContains("[cp:/files/test2/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPANESE)).asString().isContains("[cp:/files/test2/ja/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/a.txt]");

		assertBytes(stream(x,"dir/a.txt", null)).asString().isContains("[cp:/files/test2/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).asString().isContains("[cp:/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", null)).asString().isContains("[cp:/files/test2/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/dir/a.txt]");

		assertBytes(stream(x,"dir/dir/a.txt", null)).asString().isContains("[cp:/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).asString().isContains("[cp:/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", null)).asString().isContains("[cp:/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPAN)).asString().isContains("[cp:/files/test2/ja/JP/dir/dir/a.txt]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Classpath tests - Classpath relative
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01a_classpathRelative_rootDir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "", false)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"files/test1/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
		}

		assertBytes(stream(x,"_a.txt")).asString().isContains("[cp:/org/apache/juneau/cp/_a.txt]");

		String[] badPatterns = {"files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).isNull();
		}
	}

	@Test
	public void d01b_classpathRelative_rootDir_recursive() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "", true)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"files/test1/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"files/test1/dir/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / files/test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
		}
	}

	@Test
	public void d02a_classpathRelative_subdir() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files", false)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"test1/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / test1/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x," / test1/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
		}

		String[] badPatterns = {"files/test1/bad.txt","files/test1/../test1/_a.txt","files/bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p)).isNull();
		}
	}

	@Test
	public void d02b_classpathRelative_subdir_recursive() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files", true)
			.build();

		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};

		for (String p : patterns) {
			assertBytes(stream(x,"test1/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x," / test1/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x," / test1/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x," / test1/dir/dir/"+p+" / ")).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
		}
	}

	@Test
	public void d03a_classpathRelative_localized_flat() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files", false)
			.build();


		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};
		String[] patterns_ja = {"_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja"};
		String[] patterns_ja_JP = {"_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP"};

		for (int i = 0; i < patterns.length; i++) {
			String p = patterns[i], p_ja = patterns_ja[i], p_ja_JP = patterns_ja_JP[i];

			assertBytes(stream(x,"test1/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x,"/test1/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");

			assertBytes(stream(x,"test1/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]");

			assertBytes(stream(x,"test1/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]");
		}

		String[] badPatterns = {"test1/bad.txt","test1/../test1/_a.txt","bad.txt",null,"",".",".."};

		for (String p : badPatterns) {
			assertBytes(stream(x,p,null)).isNull();
			assertBytes(stream(x,p,JAPANESE)).isNull();
			assertBytes(stream(x,p,JAPAN)).isNull();
		}
	}

	@Test
	public void d03b_classpathRelative_localized_flat_recursive() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files", true)
			.build();


		String[] patterns = {"_a.txt","_b",".c",".d.txt","e.txt","f","g_foo.txt","h.foo.txt","i_foo"};
		String[] patterns_ja = {"_a_ja.txt","_b_ja",".c",".d_ja.txt","e_ja.txt","f_ja","g_foo_ja.txt","h.foo_ja.txt","i_foo_ja"};
		String[] patterns_ja_JP = {"_a_ja_JP.txt","_b_ja_JP",".c",".d_ja_JP.txt","e_ja_JP.txt","f_ja_JP","g_foo_ja_JP.txt","h.foo_ja_JP.txt","i_foo_ja_JP"};

		for (int i = 0; i < patterns.length; i++) {
			String p = patterns[i], p_ja = patterns_ja[i], p_ja_JP = patterns_ja_JP[i];

			assertBytes(stream(x,"test1/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"test1/dir/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");
			assertBytes(stream(x,"/test1/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p+"]");

			assertBytes(stream(x,"test1/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja+"]");

			assertBytes(stream(x,"test1/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"test1/dir/dir/"+p,JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/"+p_ja_JP+"]");
			assertBytes(stream(x,"/test1/dir/dir/"+p+"/",JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test1/dir/dir/"+p_ja_JP+"]");
		}
	}

	@Test
	public void d04a_classpathRelative_localized_hierarchical() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "files/test2", false)
			.build();

		assertBytes(stream(x,"a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]");
		assertBytes(stream(x,"a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]");
		assertBytes(stream(x,"a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]");
		assertBytes(stream(x,"/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]");

		assertBytes(stream(x,"dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]");

		assertBytes(stream(x,"dir/dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]");
	}

	@Test
	public void d04b_classpathRelative_localized_hierarchical_recursive() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.build();

		assertBytes(stream(x,"a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]");
		assertBytes(stream(x,"a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]");
		assertBytes(stream(x,"a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]");
		assertBytes(stream(x,"/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]");
		assertBytes(stream(x,"/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]");

		assertBytes(stream(x,"dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"/dir/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]");

		assertBytes(stream(x,"dir/dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"/dir/dir/a.txt/", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]");

		x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(class|properties)")
			.build();
	}

	@Test
	public void d05_classpathRelative_ignorePattern() throws Exception {
		FileFinder x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(txt)")
			.build();

		assertBytes(stream(x,"a.txt", null)).isNull();
		assertBytes(stream(x,"a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"a.txt", JAPAN)).isNull();

		assertBytes(stream(x,"dir/a.txt", null)).isNull();
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"dir/a.txt", JAPAN)).isNull();

		assertBytes(stream(x,"dir/dir/a.txt", null)).isNull();
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).isNull();

		x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude("(?i).*\\.(TXT)")
			.build();

		assertBytes(stream(x,"a.txt", null)).isNull();
		assertBytes(stream(x,"a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"a.txt", JAPAN)).isNull();

		assertBytes(stream(x,"dir/a.txt", null)).isNull();
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"dir/a.txt", JAPAN)).isNull();

		assertBytes(stream(x,"dir/dir/a.txt", null)).isNull();
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).isNull();
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).isNull();

		x = FileFinder
			.create()
			.cp(FileFinder_Test2.class, "files/test2", true)
			.exclude()
			.build();

		assertBytes(stream(x,"a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/a.txt]");
		assertBytes(stream(x,"a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/a.txt]");
		assertBytes(stream(x,"a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/a.txt]");

		assertBytes(stream(x,"dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/a.txt]");
		assertBytes(stream(x,"dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/a.txt]");

		assertBytes(stream(x,"dir/dir/a.txt", null)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPANESE)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/dir/dir/a.txt]");
		assertBytes(stream(x,"dir/dir/a.txt", JAPAN)).asString().isContains("[cp:/org/apache/juneau/cp/files/test2/ja/JP/dir/dir/a.txt]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_localDir_hashCode() throws Exception {
		Set<LocalDir> s = null;

		s = set(new LocalDir(Paths.get("test")), new LocalDir(Paths.get("test")));
		assertCollection(s).isSize(1);

		s = set(new LocalDir(Paths.get("test")), new LocalDir(Paths.get("test","test")));
		assertCollection(s).isSize(2);

		s = set(new LocalDir(List.class,null), new LocalDir(List.class,null));
		assertCollection(s).isSize(1);

		s = set(new LocalDir(List.class,null), new LocalDir(List.class,"foo"));
		assertCollection(s).isSize(2);

		s = set(new LocalDir(List.class,null), new LocalDir(String.class,null));
		assertCollection(s).isSize(2);
	}

	@Test
	public void e02_caching() throws Exception {
		FileFinder x = FileFinder
			.create()
			.dir(".")
			.caching(100_000_000)
			.build();

		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[home:/files/test1/_a.txt]");
		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[home:/files/test1/_a.txt]");

		x = FileFinder
			.create()
			.dir(".")
			.caching(1)
			.build();

		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[home:/files/test1/_a.txt]");
		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[home:/files/test1/_a.txt]");

		x = FileFinder
			.create()
			.cp(FileFinder_Test.class, "/", false)
			.caching(100_000_000)
			.build();

		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[cp:/files/test1/_a.txt]");
		assertBytes(stream(x,"files/test1/_a.txt")).asString().isContains("[cp:/files/test1/_a.txt]");
	}

	@Test
	public void e03_subclassing() throws Exception {
		FileFinder x = E03b
			.create()
			.dir(".")
			.caching(100_000_000)
			.type(E03b.class)
			.build();
		assertObject(x).isType(E03b.class);
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
