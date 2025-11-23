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
package org.apache.juneau.msgpack;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class MsgPackSerializerTest extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {

		test(null, "C0");

		test(false, "C2");
		test(true, "C3");

		//		positive fixnum stores 7-bit positive integer
		//		+--------+
		//		|0XXXXXXX|
		//		+--------+
		//
		//		int 8 stores a 8-bit signed integer
		//		+--------+--------+
		//		|  0xd0  |ZZZZZZZZ|
		//		+--------+--------+
		//
		//		int 16 stores a 16-bit big-endian signed integer
		//		+--------+--------+--------+
		//		|  0xd1  |ZZZZZZZZ|ZZZZZZZZ|
		//		+--------+--------+--------+
		//
		//		int 32 stores a 32-bit big-endian signed integer
		//		+--------+--------+--------+--------+--------+
		//		|  0xd2  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
		//		+--------+--------+--------+--------+--------+
		//
		//		int 64 stores a 64-bit big-endian signed integer
		//		+--------+--------+--------+--------+--------+--------+--------+--------+--------+
		//		|  0xd3  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
		//		+--------+--------+--------+--------+--------+--------+--------+--------+--------+
		//
		//		negative fixnum stores 5-bit negative integer
		//		+--------+
		//		|111YYYYY|
		//		+--------+
		//
		//		* 0XXXXXXX is 8-bit unsigned integer
		//		* 111YYYYY is 8-bit signed integer
		//

		test(0, "00");
		test(0x7F, "7F");

		test(0x80, "D1 00 80");
		test(0x0100, "D1 01 00");
		test(0x7FFF, "D1 7F FF");
		test(0x8000, "D2 00 00 80 00");
		test(0xFFFF, "D2 00 00 FF FF");
		test(0x00010000, "D2 00 01 00 00");
		test(Long.decode("0x000000007FFFFFFF"), "D2 7F FF FF FF");
		test(Long.decode("0x0000000080000000"), "D3 00 00 00 00 80 00 00 00");
		test(Long.decode("0x0000000100000000"), "D3 00 00 00 01 00 00 00 00");
		test(Long.decode("0x7FFFFFFFFFFFFFFF"), "D3 7F FF FF FF FF FF FF FF");
		test(-Long.decode("0x7FFFFFFFFFFFFFFF").longValue(), "D3 80 00 00 00 00 00 00 01");
		test(-1, "E1");
		test(-63, "FF");
		test(-64, "D0 C0");

		test(-0x7F, "D0 81");
		test(-0x80, "D1 FF 80");
		test(-0x0100, "D1 FF 00");
		test(-0x7FFF, "D1 80 01");
		test(-0x8000, "D2 FF FF 80 00");
		test(-0xFFFF, "D2 FF FF 00 01");
		test(-0x00010000, "D2 FF FF 00 00");
		test(-Long.decode("0x000000007FFFFFFF").longValue(), "D2 80 00 00 01");
		test(-Long.decode("0x0000000080000000").longValue(), "D3 FF FF FF FF 80 00 00 00");
		test(-Long.decode("0x0000000100000000").longValue(), "D3 FF FF FF FF 00 00 00 00");
		test(-Long.decode("0x7FFFFFFFFFFFFFFF").longValue(), "D3 80 00 00 00 00 00 00 01");

		//		float 32 stores a floating point number in IEEE 754 single precision floating point number format:
		//		+--------+--------+--------+--------+--------+
		//		|  0xca  |XXXXXXXX|XXXXXXXX|XXXXXXXX|XXXXXXXX|
		//		+--------+--------+--------+--------+--------+
		//
		//		float 64 stores a floating point number in IEEE 754 double precision floating point number format:
		//		+--------+--------+--------+--------+--------+--------+--------+--------+--------+
		//		|  0xcb  |YYYYYYYY|YYYYYYYY|YYYYYYYY|YYYYYYYY|YYYYYYYY|YYYYYYYY|YYYYYYYY|YYYYYYYY|
		//		+--------+--------+--------+--------+--------+--------+--------+--------+--------+
		//
		//		where
		//		* XXXXXXXX_XXXXXXXX_XXXXXXXX_XXXXXXXX is a big-endian IEEE 754 single precision floating point number.
		//		  Extension of precision from single-precision to double-precision does not lose precision.
		//		* YYYYYYYY_YYYYYYYY_YYYYYYYY_YYYYYYYY_YYYYYYYY_YYYYYYYY_YYYYYYYY_YYYYYYYY is a big-endian
		//		  IEEE 754 double precision floating point number

		test(0f, "CA 00 00 00 00");
		test(1f, "CA 3F 80 00 00");
		test(-1f, "CA BF 80 00 00");
		test(1d, "CB 3F F0 00 00 00 00 00 00");
		test(-1d, "CB BF F0 00 00 00 00 00 00");

		//		fixstr stores a byte array whose length is upto 31 bytes:
		//		+--------+========+
		//		|101XXXXX|  data  |
		//		+--------+========+
		//
		//		str 8 stores a byte array whose length is upto (2^8)-1 bytes:
		//		+--------+--------+========+
		//		|  0xd9  |YYYYYYYY|  data  |
		//		+--------+--------+========+
		//
		//		str 16 stores a byte array whose length is upto (2^16)-1 bytes:
		//		+--------+--------+--------+========+
		//		|  0xda  |ZZZZZZZZ|ZZZZZZZZ|  data  |
		//		+--------+--------+--------+========+
		//
		//		str 32 stores a byte array whose length is upto (2^32)-1 bytes:
		//		+--------+--------+--------+--------+--------+========+
		//		|  0xdb  |AAAAAAAA|AAAAAAAA|AAAAAAAA|AAAAAAAA|  data  |
		//		+--------+--------+--------+--------+--------+========+
		//
		//		where
		//		* XXXXX is a 5-bit unsigned integer which represents N
		//		* YYYYYYYY is a 8-bit unsigned integer which represents N
		//		* ZZZZZZZZ_ZZZZZZZZ is a 16-bit big-endian unsigned integer which represents N
		//		* AAAAAAAA_AAAAAAAA_AAAAAAAA_AAAAAAAA is a 32-bit big-endian unsigned integer which represents N
		//		* N is the length of data

		test("", "A0");
		test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "BF 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61");
		test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "D9 20 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61");

		//		fixarray stores an array whose length is upto 15 elements:
		//		+--------+~~~~~~~~~~~~~~~~~+
		//		|1001XXXX|    N objects    |
		//		+--------+~~~~~~~~~~~~~~~~~+
		//
		//		array 16 stores an array whose length is upto (2^16)-1 elements:
		//		+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//		|  0xdc  |YYYYYYYY|YYYYYYYY|    N objects    |
		//		+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		//		array 32 stores an array whose length is upto (2^32)-1 elements:
		//		+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//		|  0xdd  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|    N objects    |
		//		+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		//		where
		//		* XXXX is a 4-bit unsigned integer which represents N
		//		* YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		//		* ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		//		    N is the size of a array

		test(ints(), "90");
		test(ints(1), "91 01");
		test(ints(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1), "9F 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01");
		test(ints(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1), "DC 00 10 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01");

		//		fixmap stores a map whose length is upto 15 elements
		//		+--------+~~~~~~~~~~~~~~~~~+
		//		|1000XXXX|   N*2 objects   |
		//		+--------+~~~~~~~~~~~~~~~~~+
		//
		//		map 16 stores a map whose length is upto (2^16)-1 elements
		//		+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//		|  0xde  |YYYYYYYY|YYYYYYYY|   N*2 objects   |
		//		+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		//		map 32 stores a map whose length is upto (2^32)-1 elements
		//		+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//		|  0xdf  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|   N*2 objects   |
		//		+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		//		where
		//		* XXXX is a 4-bit unsigned integer which represents N
		//		* YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		//		* ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		//		* N is the size of a map
		//		* odd elements in objects are keys of a map
		//		* the next element of a key is its associated value

		test(JsonMap.ofJson("{}"), "80");
		test(JsonMap.ofJson("{1:1}"), "81 A1 31 01");
		test(JsonMap.ofJson("{1:1,2:1,3:1,4:1,5:1,6:1,7:1,8:1,9:1,a:1,b:1,c:1,d:1,e:1,f:1}"), "8F A1 31 01 A1 32 01 A1 33 01 A1 34 01 A1 35 01 A1 36 01 A1 37 01 A1 38 01 A1 39 01 A1 61 01 A1 62 01 A1 63 01 A1 64 01 A1 65 01 A1 66 01");
		test(JsonMap.ofJson("{1:1,2:1,3:1,4:1,5:1,6:1,7:1,8:1,9:1,a:1,b:1,c:1,d:1,e:1,f:1,g:1}"), "DE 00 10 A1 31 01 A1 32 01 A1 33 01 A1 34 01 A1 35 01 A1 36 01 A1 37 01 A1 38 01 A1 39 01 A1 61 01 A1 62 01 A1 63 01 A1 64 01 A1 65 01 A1 66 01 A1 67 01");
	}

	public static class Person {
		public String name = "John Smith";
		public int age = 21;
	}

	private static void test(Object input, String expected) throws Exception {
		var b = MsgPackSerializer.DEFAULT.serialize(input);
		assertEquals(expected, toSpacedHex(b));
	}
}