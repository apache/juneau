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
package org.apache.juneau.marshall.jena.marshaller;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link NTripleSerializer} and {@link NTripleParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	NTriple <jv>nTriple</jv> = <jk>new</jk> NTriple();
 * 	MyPojo <jv>myPojo</jv> = <jv>nTriple</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>nTriple</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = NTriple.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = NTriple.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" .
 * 	&lt;...&gt; &lt;.../age&gt; "30"^^&lt;http://www.w3.org/2001/XMLSchema#int&gt; .
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" .
 * 	&lt;...&gt; &lt;.../age&gt; "30"^^... .
 * 	&lt;...&gt; &lt;.../address&gt; &lt;.../address&gt; .
 * 	&lt;.../address&gt; &lt;.../street&gt; "123 Main St" .
 * 	&lt;.../address&gt; &lt;.../city&gt; "Boston" .
 * 	&lt;.../address&gt; &lt;.../state&gt; "MA" .
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class NTriple extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final NTriple DEFAULT = new NTriple();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link NTripleSerializer#DEFAULT} and {@link NTripleParser#DEFAULT}.
	 */
	public NTriple() {
		this(NTripleSerializer.DEFAULT, NTripleParser.DEFAULT);
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
	public NTriple(NTripleSerializer s, NTripleParser p) {
		super(s, p);
	}
}