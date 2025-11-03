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
package org.apache.juneau.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.common.io.*;
import org.junit.jupiter.api.*;

class StringBuilderWriterTest extends TestBase {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test void a01_test() throws Exception {
		var sbw = new StringBuilderWriter();
		sbw.write("abc");
		assertEquals("abc", sbw.toString());
		sbw.append("abc");
		assertEquals("abcabc", sbw.toString());
		sbw.write("abc", 1, 1);
		assertEquals("abcabcb", sbw.toString());
		sbw.append("abc", 1, 2);
		assertEquals("abcabcbb", sbw.toString());
		sbw.write((String)null);
		assertEquals("abcabcbbnull", sbw.toString());
		sbw.append((String)null);
		assertEquals("abcabcbbnullnull", sbw.toString());
		sbw.append((String)null,0,4);
		assertEquals("abcabcbbnullnullnull", sbw.toString());

		char[] buff = "abc".toCharArray();
		var sbw2 = new StringBuilderWriter();
		sbw2.write(buff, 0, buff.length);
		assertEquals("abc", sbw2.toString());
		sbw2.write(buff, 0, 0);
		assertEquals("abc", sbw2.toString());

		assertThrows(IndexOutOfBoundsException.class, ()-> sbw2.write(buff, -1, buff.length));
		assertThrows(IndexOutOfBoundsException.class, ()-> sbw2.write(buff, buff.length+1, 0));
		assertThrows(IndexOutOfBoundsException.class, ()-> sbw2.write(buff, buff.length-1, 2));
		assertThrows(IndexOutOfBoundsException.class, ()-> sbw2.write(buff, 0, buff.length+1));
		assertThrows(IndexOutOfBoundsException.class, ()-> sbw2.write(buff, 0, -1));

		sbw.flush();
		sbw.close();

		sbw2.flush();
		sbw2.close();
	}
}