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
 * A pairing of a {@link N3Serializer} and {@link N3Parser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	N3 <jv>n3</jv> = <jk>new</jk> N3();
 * 	MyPojo <jv>myPojo</jv> = <jv>n3</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>n3</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = N3.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = N3.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, N3/Turtle-like):</h5>
 * <p class='bcode'>
 * 	@prefix j: &lt;...&gt; .
 * 	&lt;...&gt; a j:Person ; j:name "Alice" ; j:age 30 .
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bcode'>
 * 	@prefix j: &lt;...&gt; .
 * 	&lt;...&gt; a j:Person ; j:name "Alice" ; j:age 30 ;
 * 		j:address &lt;.../address&gt; ; j:tags &lt;.../tags&gt; .
 * 	&lt;.../address&gt; a j:Address ; j:street "123 Main St" ;
 * 		j:city "Boston" ; j:state "MA" .
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class N3 extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final N3 DEFAULT = new N3();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link N3Serializer#DEFAULT} and {@link N3Parser#DEFAULT}.
	 */
	public N3() {
		this(N3Serializer.DEFAULT, N3Parser.DEFAULT);
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
	public N3(N3Serializer s, N3Parser p) {
		super(s, p);
	}
}