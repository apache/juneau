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
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A pairing of a {@link CsvSerializer} and {@link CsvParser} into a single class with convenience read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to CSV</jc>
 * 	List&lt;Person&gt; <jv>people</jv> = List.of(<jk>new</jk> Person(<js>"Alice"</js>, 30), <jk>new</jk> Person(<js>"Bob"</js>, 25));
 * 	String <jv>csv</jv> = Csv.<jsm>of</jsm>(<jv>people</jv>);
 *
 * 	<jc>// Parse CSV into a list of beans</jc>
 * 	List&lt;Person&gt; <jv>parsed</jv> = Csv.<jsm>to</jsm>(<jv>csv</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 *
 * 	<jc>// Parse CSV into a list of maps</jc>
 * 	List&lt;Map&lt;String,String&gt;&gt; <jv>rows</jv> = Csv.<jsm>to</jsm>(<jv>csv</jv>, List.<jk>class</jk>, Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Csv <jv>m</jv> = Csv.<jsf>DEFAULT</jsf>;
 * 	<jv>csv</jv> = <jv>m</jv>.write(<jv>people</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>csv</jv>, List.<jk>class</jk>, Person.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (list of maps with a,b):</h5>
 * <p class='bcode'>
 * 	a,b
 * 	foo,bar
 * </p>
 *
 * <h5 class='figure'>Complex (list of beans with nested address, flattened):</h5>
 * <p class='bcode'>
 * 	name,age,address_street,address_city,address_state,tags
 * 	Alice,30,123 Main St,Boston,MA,"a,b,c"
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Csv extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Csv DEFAULT = new Csv();


	/**
	 * Constructor.
	 *
	 * <p>
	 * Uses {@link CsvSerializer#DEFAULT} and {@link CsvParser#DEFAULT}.
	 */
	public Csv() {
		this(CsvSerializer.DEFAULT, CsvParser.DEFAULT);
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
	public Csv(CsvSerializer s, CsvParser p) {
		super(s, p);
	}
}