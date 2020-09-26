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

import org.apache.juneau.html.*;

/**
 * A pairing of a {@link HtmlSerializer} and {@link HtmlParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Using instance.</jc>
 * 	Html html = <jk>new</jk> Html();
 * 	MyPojo myPojo = html.read(string, MyPojo.<jk>class</jk>);
 * 	String string = html.write(myPojo);
 * </p>
 * <p class='bcode w800'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo myPojo = Html.<jsf>DEFAULT</jsf>.read(string, MyPojo.<jk>class</jk>);
 * 	String string = Html.<jsf>DEFAULT</jsf>.write(myPojo);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Marshalls}
 * </ul>
 */
public class Html extends CharMarshall {

	/**
	 * Default reusable instance.
	 */
	public static final Html DEFAULT = new Html();

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
	public Html(HtmlSerializer s, HtmlParser p) {
		super(s, p);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link HtmlSerializer#DEFAULT} and {@link HtmlParser#DEFAULT}.
	 */
	public Html() {
		this(HtmlSerializer.DEFAULT, HtmlParser.DEFAULT);
	}
}
