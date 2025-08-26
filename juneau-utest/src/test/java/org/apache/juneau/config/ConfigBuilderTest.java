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
package org.apache.juneau.config;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

class ConfigBuilderTest extends SimpleTestBase {

	private static File tempDir;
	private static String TEMP_DIR;

	@BeforeAll
	static void setup() {
		tempDir = new File(System.getProperty("java.io.tmpdir"), random(12));
		TEMP_DIR = tempDir.getAbsolutePath();
	}

	@AfterAll
	static void teardown() {
		delete(tempDir);
	}

	@Test void a01_get_LONGRUNNING() throws Exception {
		File f;
		var cfs = FileStore.create().directory(TEMP_DIR).enableWatcher().watcherSensitivity(WatcherSensitivity.HIGH).build();
		Config.Builder cb = Config.create().store(cfs).name("TestGet.cfg");

		Config cf = cb.build();
		cf.set("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		assertFalse(f.exists());

		cf.commit();
		assertJson(cf.toMap(), "{'':{},Test:{A:'a'}}");

		String nl = System.getProperty("line.separator");
		cf = cf.load("[Test]"+nl+"A = b"+nl, true);
		assertJson(cf.toMap(), "{'':{},Test:{A:'b'}}");
	}
}