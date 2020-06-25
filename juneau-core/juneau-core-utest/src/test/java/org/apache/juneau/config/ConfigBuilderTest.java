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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.config.store.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigBuilderTest {

	private static File tempDir;
	private static String TEMP_DIR;

	@BeforeClass
	public static void setup() {
		tempDir = new File(System.getProperty("java.io.tmpdir"), generateUUID(12));
		TEMP_DIR = tempDir.getAbsolutePath();
	}

	@AfterClass
	public static void teardown() {
		delete(tempDir);
	}

	@Test
	public void testGet_LONGRUNNING() throws Exception {
		File f;
		ConfigFileStore cfs = ConfigFileStore.create().directory(TEMP_DIR).useWatcher().watcherSensitivity(WatcherSensitivity.HIGH).build();
		ConfigBuilder cb = Config.create().store(cfs).name("TestGet.cfg");

		Config cf = cb.build();
		cf.set("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		assertFalse(f.exists());

		cf.commit();
		assertObject(cf.toMap()).json().is("{'':{},Test:{A:'a'}}");

		String NL = System.getProperty("line.separator");
		cf = cf.load("[Test]"+NL+"A = b"+NL, true);
		assertObject(cf.toMap()).json().is("{'':{},Test:{A:'b'}}");
	}
}