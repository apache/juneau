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
package org.apache.juneau.transform;

import org.apache.juneau.*;

/**
 * Abstract subclass for POJO swaps that swap objects for strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A swap that converts byte arrays to BASE64-encoded strings.</jc>
 * 	<jk>public class</jk> ByteArrayBase64Swap <jk>extends</jk> StringSwap&lt;<jk>byte</jk>[]&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String swap(BeanSession session, <jk>byte</jk>[] b) <jk>throws</jk> Exception {
 * 			<jk>return</jk> StringUtils.<jsm>base64Encode</jsm>(b);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public byte</jk>[] unswap(BeanSession session, String s, ClassMeta&lt;?&gt; hint) <jk>throws</jk> Exception {
 * 			<jk>return</jk> StringUtils.<jsm>base64Decode</jsm>(s);
 * 		}
 * 	}
 *
 * 	<jc>// Use it to serialize a byte array.</jc>
 * 	WriterSerializer s = JsonSerializer.<jsm>create</jsm>().simple().pojoSwaps(ByteArrayBase64Swap.<jk>class</jk>).build();
 * 	String json = s.serialize(<jk>new byte</jk>[] {1,2,3});  <jc>// Produces "'AQID'"</jc>
 * </p>
 *
 * @param <T> The normal form of the class.
 */
public abstract class StringSwap<T> extends PojoSwap<T,String> {

	@Override /* PojoSwap */
	public String swap(BeanSession session, T o) throws Exception {
		return super.swap(session, o);
	}

	@Override /* PojoSwap */
	public T unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		return super.unswap(session, f, hint);
	}
}
