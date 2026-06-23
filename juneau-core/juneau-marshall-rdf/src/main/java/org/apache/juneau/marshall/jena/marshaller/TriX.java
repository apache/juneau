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
 * A pairing of a {@link TriXSerializer} and {@link TriXParser} into a single class with convenience read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to TriX (RDF/XML-based)</jc>
 * 	String <jv>trix</jv> = TriX.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse TriX into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = TriX.<jsm>to</jsm>(<jv>trix</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	TriX <jv>m</jv> = TriX.<jsf>DEFAULT</jsf>;
 * 	<jv>trix</jv> = <jv>m</jv>.write(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>trix</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;TriX&gt;</xt>
 * 		<xt>&lt;graph&gt;</xt>
 * 			<xt>&lt;uri&gt;</xt>...<xt>&lt;/uri&gt;</xt>
 * 			<xt>&lt;triple&gt;</xt>
 * 				<xt>&lt;id&gt;</xt>...<xt>&lt;/id&gt;</xt>
 * 				<xt>&lt;name&gt;</xt>Alice<xt>&lt;/name&gt;</xt>
 * 				<xt>&lt;age&gt;</xt>30<xt>&lt;/age&gt;</xt>
 * 			<xt>&lt;/triple&gt;</xt>
 * 		<xt>&lt;/graph&gt;</xt>
 * 	<xt>&lt;/TriX&gt;</xt>
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bxml'>
 * 	<xt>&lt;TriX&gt;</xt>
 * 		<xt>&lt;graph&gt;</xt>
 * 			<xt>&lt;triple&gt;</xt>...subject, predicate, object for name, age, address ref, tags...
 * 			<xt>&lt;triple&gt;</xt>...address subject, street, city, state...
 * 			<xt>&lt;triple&gt;</xt>...tags list members...
 * 		<xt>&lt;/graph&gt;</xt>
 * 	<xt>&lt;/TriX&gt;</xt>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class TriX extends CharMarshaller {

	/** Default reusable instance.*/
	public static final TriX DEFAULT = new TriX();

	/** Constructor using defaults.*/
	public TriX() {
		this(TriXSerializer.DEFAULT, TriXParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public TriX(TriXSerializer s, TriXParser p) {
		super(s, p);
	}
}
