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
package org.apache.juneau.marshall.marshaller;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.uon.*;

/**
 * A pairing of a {@link UonSerializer} and {@link UonParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Uon <jv>uon</jv> = <jk>new</jk> Uon();
 * 	MyPojo <jv>myPojo</jv> = <jv>uon</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>uon</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Uon.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Uon.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age, UON format):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30)
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30,address=(street=123+Main+St,city=Boston,state=MA),tags=@(a,b,c))
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Uon extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Uon DEFAULT = new Uon();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link UonSerializer#DEFAULT} and {@link UonParser#DEFAULT}.
	 */
	public Uon() {
		this(UonSerializer.DEFAULT, UonParser.DEFAULT);
	}

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
}