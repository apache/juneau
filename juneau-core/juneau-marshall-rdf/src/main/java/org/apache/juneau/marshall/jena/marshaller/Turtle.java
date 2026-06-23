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

import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * A pairing of a {@link TurtleSerializer} and {@link TurtleParser} into a single class with convenience to/of methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for serializing and parsing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Turtle <jv>turtle</jv> = <jk>new</jk> Turtle();
 * 	MyPojo <jv>myPojo</jv> = <jv>turtle</jv>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>turtle</jv>.of(<jv>myPojo</jv>);
 * </p>
 * <p class='bcode'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Turtle.<jsf>DEFAULT</jsf>.to(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Turtle.<jsf>DEFAULT</jsf>.of(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
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
 * 	&lt;.../tags&gt; j:member "a", "b", "c" .
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class Turtle extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Turtle DEFAULT = new Turtle();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link TurtleSerializer#DEFAULT} and {@link TurtleParser#DEFAULT}.
	 */
	public Turtle() {
		this(TurtleSerializer.DEFAULT, TurtleParser.DEFAULT);
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
	public Turtle(TurtleSerializer s, TurtleParser p) {
		super(s, p);
	}
}