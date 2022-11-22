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
import org.apache.juneau.collections.*;

/**
 * Abstract subclass for object swaps that swap objects for object maps.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A swap that converts beans into generic maps.</jc>
 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> MapSwap&lt;<jk>byte</jk>[]&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> JsonMap swap(BeanSession <jv>session</jv>, MyBean <jv>bean</jv>) <jk>throws</jk> Exception {
 * 			<jk>return</jk> JsonMap.<jsm>of</jsm>(<js>"foo"</js>, <jv>bean</jv>.getFoo());
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> MyBean unswap(BeanSession <jv>session</jv>, JsonMap <jv>map</jv>, ClassMeta&lt;?&gt; <jv>hint</jv>) <jk>throws</jk> Exception {
 * 			<jk>return</jk> new</jk> MyBean(<jv>map</jv>.get(<js>"foo"</js>));
 * 		}
 * 	}
 *
 * 	<jc>// Use it to serialize a byte array.</jc>
 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().simple().swaps(MyBeanSwap.<jk>class</jk>).build();
 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean(<js>"bar"</js>));  <jc>// Produces "{foo:'bar'}"</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 *
 * @param <T> The normal form of the class.
 */
public abstract class MapSwap<T> extends ObjectSwap<T,JsonMap> {

	@Override /* ObjectSwap */
	public JsonMap swap(BeanSession session, T o) throws Exception {
		return super.swap(session, o);
	}

	@Override /* ObjectSwap */
	public T unswap(BeanSession session, JsonMap f, ClassMeta<?> hint) throws Exception {
		return super.unswap(session, f, hint);
	}
}
