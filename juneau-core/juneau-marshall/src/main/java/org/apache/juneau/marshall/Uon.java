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

import org.apache.juneau.uon.*;

/**
 * A pairing of a {@link UonSerializer} and {@link UonParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Using instance.</jc>
 * 	Uon uon = <jk>new</jk> Uon();
 * 	MyPojo myPojo = uon.read(string, MyPojo.<jk>class</jk>);
 * 	String string = uon.write(myPojo);
 * </p>
 * <p class='bcode w800'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo myPojo = Uon.<jsf>DEFAULT</jsf>.read(string, MyPojo.<jk>class</jk>);
 * 	String string = Uon.<jsf>DEFAULT</jsf>.write(myPojo);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Marshalls}
 * </ul>
 */
public class Uon extends CharMarshall {

	/**
	 * Default reusable instance.
	 */
	public static final Uon DEFAULT = new Uon();

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
	public Uon(UonSerializer s, UonParser p) {
		super(s, p);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link UonSerializer#DEFAULT} and {@link UonParser#DEFAULT}.
	 */
	public Uon() {
		this(UonSerializer.DEFAULT, UonParser.DEFAULT);
	}
}
