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

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A subclass of {@link Marshall} for character-based serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.Marshalls">Overview &gt; juneau-marshall &gt;Marshalls</a>
 * </ul>
 */
public abstract class CharMarshall extends Marshall {

	private final WriterSerializer s;

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
	protected CharMarshall(WriterSerializer s, ReaderParser p) {
		super(s, p);
		this.s = s;
	}

	/**
	 * Serializes a POJO directly to a <code>String</code>.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Serializer */
	public final String write(Object o) throws SerializeException {
		return s.createSession().serializeToString(o);
	}
}
