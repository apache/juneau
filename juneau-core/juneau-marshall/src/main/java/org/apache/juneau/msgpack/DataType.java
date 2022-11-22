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
package org.apache.juneau.msgpack;

/**
 * Constants for the MessagePack format.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MsgPackDetails">MessagePack Details</a>
 * </ul>
 */
enum DataType {
	NULL, BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, BIN, EXT, ARRAY, MAP, INVALID;

	boolean isOneOf(DataType...dataTypes) {
		for (DataType dt : dataTypes)
			if (this == dt)
				return true;
		return false;
	}

	static final int
		POSFIXINT_L  = 0x00,  //   pos fixint     0xxxxxxx     0x00 - 0x7f
		POSFIXINT_U  = 0x7F,
		FIXMAP_L     = 0x80,  //   fixmap         1000xxxx     0x80 - 0x8f
		FIXMAP_U     = 0x8F,
		FIXARRAY_L   = 0x90,  //   fixarray       1001xxxx     0x90 - 0x9f
		FIXARRAY_U   = 0x9F,
		FIXSTR_L     = 0xA0,  //   fixstr         101xxxxx     0xa0 - 0xbf
		FIXSTR_U     = 0xBF,
		NIL          = 0xC0,  //   nil            11000000     0xc0
		NU           = 0xC1,  //   (never used)   11000001     0xc1
		FALSE        = 0xC2,  //   false          11000010     0xc2
		TRUE         = 0xC3,  //   true           11000011     0xc3
		BIN8         = 0xC4,  //   bin 8          11000100     0xc4
		BIN16        = 0xC5,  //   bin 16         11000101     0xc5
		BIN32        = 0xC6,  //   bin 32         11000110     0xc6
		EXT8         = 0xC7,  //   ext 8          11000111     0xc7
		EXT16        = 0xC8,  //   ext 16         11001000     0xc8
		EXT32        = 0xC9,  //   ext 32         11001001     0xc9
		FLOAT32      = 0xCA,  //   float 32       11001010     0xca
		FLOAT64      = 0xCB,  //   float 64       11001011     0xcb
		UINT8        = 0xCC,  //   uint 8         11001100     0xcc
		UINT16       = 0xCD,  //   uint 16        11001101     0xcd
		UINT32       = 0xCE,  //   uint 32        11001110     0xce
		UINT64       = 0xCF,  //   uint 64        11001111     0xcf
		INT8         = 0xD0,  //   int 8          11010000     0xd0
		INT16        = 0xD1,  //   int 16         11010001     0xd1
		INT32        = 0xD2,  //   int 32         11010010     0xd2
		INT64        = 0xD3,  //   int 64         11010011     0xd3
		FIXEXT1      = 0xD4,  //   fixext 1       11010100     0xd4
		FIXEXT2      = 0xD5,  //   fixext 2       11010101     0xd5
		FIXEXT4      = 0xD6,  //   fixext 4       11010110     0xd6
		FIXEXT8      = 0xD7,  //   fixext 8       11010111     0xd7
		FIXEXT16     = 0xD8,  //   fixext 16      11011000     0xd8
		STR8         = 0xD9,  //   str 8          11011001     0xd9
		STR16        = 0xDA,  //   str 16         11011010     0xda
		STR32        = 0xDB,  //   str 32         11011011     0xdb
		ARRAY16      = 0xDC,  //   array 16       11011100     0xdc
		ARRAY32      = 0xDD,  //   array 32       11011101     0xdd
		MAP16        = 0xDE,  //   map 16         11011110     0xde
		MAP32        = 0xDF,  //   map 32         11011111     0xdf
		NEGFIXINT_L  = 0xE0,  //   neg fixint     111xxxxx     0xe0 - 0xff
		NEGFIXINT_U  = 0xFF;
}
