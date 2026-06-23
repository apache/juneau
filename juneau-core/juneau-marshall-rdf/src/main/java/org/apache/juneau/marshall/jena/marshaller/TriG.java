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
 * A pairing of a {@link TriGSerializer} and {@link TriGParser} into a single class with convenience to/of methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to TriG (Turtle with named graphs)</jc>
 * 	String <jv>trig</jv> = TriG.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse TriG into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = TriG.<jsm>to</jsm>(<jv>trig</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	TriG <jv>m</jv> = TriG.<jsf>DEFAULT</jsf>;
 * 	<jv>trig</jv> = <jv>m</jv>.of(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.to(<jv>trig</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, Turtle with named graph):</h5>
 * <p class='bcode'>
 * 	@prefix j: &lt;...&gt; .
 * 	&lt;...&gt; { &lt;...&gt; a j:Person ; j:name "Alice" ; j:age 30 . }
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bcode'>
 * 	@prefix j: &lt;...&gt; .
 * 	&lt;...&gt; { &lt;...&gt; a j:Person ; j:name "Alice" ; j:age 30 ;
 * 		j:address &lt;.../address&gt; . }
 * 	&lt;...&gt; { &lt;.../address&gt; a j:Address ; j:street "123 Main St" ;
 * 		j:city "Boston" ; j:state "MA" . }
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class TriG extends CharMarshaller {

	/** Default reusable instance.*/
	public static final TriG DEFAULT = new TriG();

	/** Constructor using defaults.*/
	public TriG() {
		this(TriGSerializer.DEFAULT, TriGParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public TriG(TriGSerializer s, TriGParser p) {
		super(s, p);
	}
}
