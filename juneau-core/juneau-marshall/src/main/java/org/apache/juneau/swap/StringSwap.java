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
package org.apache.juneau.swap;

import org.apache.juneau.*;

/**
 * Abstract subclass for object swaps that swap objects for strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A swap that converts byte arrays to BASE64-encoded strings.</jc>
 * 	<jk>public class</jk> ByteArrayBase64Swap <jk>extends</jk> StringSwap&lt;<jk>byte</jk>[]&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String swap(BeanSession <jv>session</jv>, <jk>byte</jk>[] <jv>bytes</jv>) <jk>throws</jk> Exception {
 * 			<jk>return</jk> StringUtils.<jsm>base64Encode</jsm>(<jv>bytes</jv>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public byte</jk>[] unswap(BeanSession <jv>session</jv>, String <jv>string</jv>, ClassMeta&lt;?&gt; <jv>hint</jv>) <jk>throws</jk> Exception {
 * 			<jk>return</jk> StringUtils.<jsm>base64Decode</jsm>(<jv>string</jv>);
 * 		}
 * 	}
 *
 * 	<jc>// Use it to serialize a byte array.</jc>
 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().simple().swaps(ByteArrayBase64Swap.<jk>class</jk>).build();
 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new byte</jk>[] {1,2,3});  <jc>// Produces "'AQID'"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 *
 * @param <T> The normal form of the class.
 */
public abstract class StringSwap<T> extends ObjectSwap<T,String> {

	/**
	 * Constructor.
	 */
	protected StringSwap() {
		super();
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 */
	protected StringSwap(Class<T> normalClass) {
		super(normalClass, String.class);
	}

	@Override /* ObjectSwap */
	public String swap(BeanSession session, T o) throws Exception {
		return super.swap(session, o);
	}

	@Override /* ObjectSwap */
	public T unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		return super.unswap(session, f, hint);
	}
}
