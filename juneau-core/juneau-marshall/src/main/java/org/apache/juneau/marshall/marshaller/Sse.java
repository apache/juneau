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

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.sse.*;

/**
 * Pairs {@link SseSerializer} and {@link SseParser} into a single class with convenience
 * read/write methods for the <c>text/event-stream</c> wire format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of events using the DEFAULT instance</jc>
 * 	List&lt;SseEvent&gt; <jv>events</jv> = List.<jsm>of</jsm>(
 * 		<jk>new</jk> SseEvent(<js>"progress"</js>, <js>"step 1"</js>),
 * 		<jk>new</jk> SseEvent(<js>"progress"</js>, <js>"step 2"</js>)
 * 	);
 * 	String <jv>wire</jv> = Sse.<jsm>of</jsm>(<jv>events</jv>);
 *
 * 	<jc>// Parse a wire string back into a list of events</jc>
 * 	List&lt;SseEvent&gt; <jv>parsed</jv> = Sse.<jsm>to</jsm>(<jv>wire</jv>, List.<jk>class</jk>, SseEvent.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
public class Sse extends CharMarshaller {

	/**
	 * Default reusable instance.
	 */
	public static final Sse DEFAULT = new Sse();

	/**
	 * Constructor.
	 */
	public Sse() {
		this(SseSerializer.DEFAULT, SseParser.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param s The serializer.
	 * @param p The parser.
	 */
	public Sse(SseSerializer s, SseParser p) {
		super(s, p);
	}
}
