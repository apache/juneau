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
package org.apache.juneau.marshall;

import org.apache.juneau.msgpack.*;

/**
 * A pairing of a {@link MsgPackSerializer} and {@link MsgPackParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Using instance.</jc>
 * 	MsgPack msgPack = <jk>new</jk> MsgPack();
 * 	MyPojo myPojo = msgPack.read(bytes, MyPojo.<jk>class</jk>);
 * 	<jk>byte</jk>[] bytes = msgPack.write(myPojo);
 * </p>
 * <p class='bcode w800'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo myPojo = MsgPack.<jsf>DEFAULT</jsf>.read(bytes, MyPojo.<jk>class</jk>);
 * 	<jk>byte</jk>[] bytes = MsgPack.<jsf>DEFAULT</jsf>.write(myPojo);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Marshalls}
 * </ul>
 */
public class MsgPack extends StreamMarshall {

	/**
	 * Default reusable instance.
	 */
	public static final MsgPack DEFAULT = new MsgPack();

	/**
	 * Constructor.
	 *
	 * @param s
	 * 	The serializer to use for serializing output.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param p
	 * 	The parser to use for parsing input.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	public MsgPack(MsgPackSerializer s, MsgPackParser p) {
		super(s, p);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link MsgPackSerializer#DEFAULT} and {@link MsgPackParser#DEFAULT}.
	 */
	public MsgPack() {
		this(MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT);
	}
}
