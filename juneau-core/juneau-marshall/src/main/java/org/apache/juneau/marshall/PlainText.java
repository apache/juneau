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

import org.apache.juneau.plaintext.*;

/**
 * A pairing of a {@link PlainTextSerializer} and {@link PlainTextParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Using instance.</jc>
 * 	PlainText plainText = <jk>new</jk> PlainText();
 * 	MyPojo myPojo = plainText.read(string, MyPojo.<jk>class</jk>);
 * 	String string = plainText.write(myPojo);
 * </p>
 * <p class='bcode w800'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo myPojo = PlainText.<jsf>DEFAULT</jsf>.read(string, MyPojo.<jk>class</jk>);
 * 	String string = PlainText.<jsf>DEFAULT</jsf>.write(myPojo);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.Marshalls}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class PlainText extends CharMarshall {

	/**
	 * Default reusable instance.
	 */
	public static final PlainText DEFAULT = new PlainText();

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
	public PlainText(PlainTextSerializer s, PlainTextParser p) {
		super(s, p);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link PlainTextSerializer#DEFAULT} and {@link PlainTextParser#DEFAULT}.
	 */
	public PlainText() {
		this(PlainTextSerializer.DEFAULT, PlainTextParser.DEFAULT);
	}
}
