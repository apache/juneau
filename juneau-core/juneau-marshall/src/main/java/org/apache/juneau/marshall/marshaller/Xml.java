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
import org.apache.juneau.marshall.xml.*;

/**
 * A pairing of a {@link XmlSerializer} and {@link XmlParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Xml <jv>xml</jv> = <jk>new</jk> Xml();
 * 	MyPojo <jv>myPojo</jv> = <jv>xml</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>xml</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Xml.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Xml.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name&gt;</xt>Alice<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;age&gt;</xt>30<xt>&lt;/age&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name&gt;</xt>Alice<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;age&gt;</xt>30<xt>&lt;/age&gt;</xt>
 * 		<xt>&lt;address&gt;</xt>
 * 			<xt>&lt;street&gt;</xt>123 Main St<xt>&lt;/street&gt;</xt>
 * 			<xt>&lt;city&gt;</xt>Boston<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state&gt;</xt>MA<xt>&lt;/state&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;tags&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>a<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>b<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>c<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/tags&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Xml extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Xml DEFAULT = new Xml();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link XmlSerializer#DEFAULT} and {@link XmlParser#DEFAULT}.
	 */
	public Xml() {
		this(XmlSerializer.DEFAULT, XmlParser.DEFAULT);
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
	public Xml(XmlSerializer s, XmlParser p) {
		super(s, p);
	}
}