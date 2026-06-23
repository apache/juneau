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
 * A pairing of a {@link NQuadsSerializer} and {@link NQuadsParser} into a single class with convenience read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean or map to N-Quads</jc>
 * 	String <jv>nquads</jv> = NQuads.<jsm>of</jsm>(<jv>myBean</jv>);
 *
 * 	<jc>// Parse N-Quads into a bean or map</jc>
 * 	MyPojo <jv>parsed</jv> = NQuads.<jsm>to</jsm>(<jv>nquads</jv>, MyPojo.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	NQuads <jv>m</jv> = NQuads.<jsf>DEFAULT</jsf>;
 * 	<jv>nquads</jv> = <jv>m</jv>.write(<jv>myBean</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>nquads</jv>, MyPojo.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age, N-Triples with graph):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" &lt;...&gt; .
 * 	&lt;...&gt; &lt;.../age&gt; "30"^^&lt;http://www.w3.org/2001/XMLSchema#int&gt; &lt;...&gt; .
 * </p>
 *
 * <h5 class='figure'>Complex (nested address + array):</h5>
 * <p class='bcode'>
 * 	&lt;...&gt; &lt;.../name&gt; "Alice" &lt;graph&gt; .
 * 	&lt;...&gt; &lt;.../address&gt; &lt;.../address&gt; &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../street&gt; "123 Main St" &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../city&gt; "Boston" &lt;graph&gt; .
 * 	&lt;.../address&gt; &lt;.../state&gt; "MA" &lt;graph&gt; .
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
public class NQuads extends CharMarshaller {

	/** Default reusable instance.*/
	public static final NQuads DEFAULT = new NQuads();

	/** Constructor using defaults.*/
	public NQuads() {
		this(NQuadsSerializer.DEFAULT, NQuadsParser.DEFAULT);
	}

	/**
	 * Constructor with serializer and parser.
	 *
	 * @param s The serializer to use.
	 * @param p The parser to use.
	 */
	public NQuads(NQuadsSerializer s, NQuadsParser p) {
		super(s, p);
	}
}
