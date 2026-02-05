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

/**
 * Constants for the MessagePack format.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MessagePackBasics">MessagePack Basics</a>
 * </ul>
 */
enum DataType {
	NULL, BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, BIN, EXT, ARRAY, MAP, INVALID;

	static final int POSFIXINT_L  = 0x00;  //   pos fixint     0xxxxxxx     0x00 - 0x7f
	static final int POSFIXINT_U  = 0x7F;
	static final int FIXMAP_L     = 0x80;  //   fixmap         1000xxxx     0x80 - 0x8f
	static final int FIXMAP_U     = 0x8F;
	static final int FIXARRAY_L   = 0x90;  //   fixarray       1001xxxx     0x90 - 0x9f
	static final int FIXARRAY_U   = 0x9F;
	static final int FIXSTR_L     = 0xA0;  //   fixstr         101xxxxx     0xa0 - 0xbf
	static final int FIXSTR_U     = 0xBF;
	static final int NIL          = 0xC0;  //   nil            11000000     0xc0
	static final int NU           = 0xC1;  //   (never used)   11000001     0xc1
	static final int FALSE        = 0xC2;  //   false          11000010     0xc2
	static final int TRUE         = 0xC3;  //   true           11000011     0xc3
	static final int BIN8         = 0xC4;  //   bin 8          11000100     0xc4
	static final int BIN16        = 0xC5;  //   bin 16         11000101     0xc5
	static final int BIN32        = 0xC6;  //   bin 32         11000110     0xc6
	static final int EXT8         = 0xC7;  //   ext 8          11000111     0xc7
	static final int EXT16        = 0xC8;  //   ext 16         11001000     0xc8
	static final int EXT32        = 0xC9;  //   ext 32         11001001     0xc9
	static final int FLOAT32      = 0xCA;  //   float 32       11001010     0xca
	static final int FLOAT64      = 0xCB;  //   float 64       11001011     0xcb
	static final int UINT8        = 0xCC;  //   uint 8         11001100     0xcc
	static final int UINT16       = 0xCD;  //   uint 16        11001101     0xcd
	static final int UINT32       = 0xCE;  //   uint 32        11001110     0xce
	static final int UINT64       = 0xCF;  //   uint 64        11001111     0xcf
	static final int INT8         = 0xD0;  //   int 8          11010000     0xd0
	static final int INT16        = 0xD1;  //   int 16         11010001     0xd1
	static final int INT32        = 0xD2;  //   int 32         11010010     0xd2
	static final int INT64        = 0xD3;  //   int 64         11010011     0xd3
	static final int FIXEXT1      = 0xD4;  //   fixext 1       11010100     0xd4
	static final int FIXEXT2      = 0xD5;  //   fixext 2       11010101     0xd5
	static final int FIXEXT4      = 0xD6;  //   fixext 4       11010110     0xd6
	static final int FIXEXT8      = 0xD7;  //   fixext 8       11010111     0xd7
	static final int FIXEXT16     = 0xD8;  //   fixext 16      11011000     0xd8
	static final int STR8         = 0xD9;  //   str 8          11011001     0xd9
	static final int STR16        = 0xDA;  //   str 16         11011010     0xda
	static final int STR32        = 0xDB;  //   str 32         11011011     0xdb
	static final int ARRAY16      = 0xDC;  //   array 16       11011100     0xdc
	static final int ARRAY32      = 0xDD;  //   array 32       11011101     0xdd
	static final int MAP16        = 0xDE;  //   map 16         11011110     0xde
	static final int MAP32        = 0xDF;  //   map 32         11011111     0xdf
	static final int NEGFIXINT_L  = 0xE0;  //   neg fixint     111xxxxx     0xe0 - 0xff
	static final int NEGFIXINT_U  = 0xFF;

	boolean isOneOf(DataType...dataTypes) {
		for (var dt : dataTypes)
			if (this == dt)
				return true;
		return false;
	}
}