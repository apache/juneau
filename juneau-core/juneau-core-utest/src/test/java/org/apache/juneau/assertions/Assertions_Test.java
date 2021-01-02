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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Assertions_Test {

	@Test
	public void a01_basic() throws Exception {
		assertDate(new Date()).isAfter(new Date(0));
		assertInteger(2).isGt(1);
		assertLong(2l).isGt(1l);
		assertObject("foo").json().is("'foo'");
		assertString("foo").is("foo");
		assertThrowable(null).doesNotExist();
		assertArray(new String[0]).isEmpty();
		assertCollection(AList.of()).isEmpty();
		assertList(AList.of()).isEmpty();
		assertStream(new ByteArrayInputStream("foo".getBytes())).string().is("foo");
		assertStream((InputStream)null).string().doesNotExist();
		assertStream(Optional.of(new ByteArrayInputStream("foo".getBytes()))).string().is("foo");
		assertStream(Optional.empty()).string().doesNotExist();
		assertBytes("foo".getBytes()).string().is("foo");
		assertBytes((byte[])null).string().doesNotExist();
		assertBytes(Optional.of("foo".getBytes())).string().is("foo");
		assertBytes(Optional.empty()).string().doesNotExist();
		assertReader(new StringReader("foo")).is("foo");
		assertReader((Reader)null).doesNotExist();
		assertReader(Optional.of(new StringReader("foo"))).is("foo");
		assertReader(Optional.empty()).doesNotExist();
		assertThrown(()->{throw new RuntimeException("foo");}).is("foo");
		assertThrown(()->{}).doesNotExist();

		new Assertions();
	}

	@Test
	public void a02_stdout_stderr() throws Exception {
		assertThrown(()->assertObject(null).msg("Test message").stderr().stdout().exists()).exists();
	}
}
