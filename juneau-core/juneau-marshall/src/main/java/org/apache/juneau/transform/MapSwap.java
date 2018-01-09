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
 * Abstract subclass for POJO swaps that swap objects for object maps.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jc>// A swap that converts beans into generic maps.</jc>
 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> MapSwap&lt;<jk>byte</jk>[]&gt; {
 * 		
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> ObjectMap swap(BeanSession session, MyBean myBean) <jk>throws</jk> Exception {
 * 			<jk>return new</jk> ObjectMap().append(<js>"foo"</js>, myBean.getFoo());
 * 		}
 * 		
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> MyBean unswap(BeanSession session, ObjectMap m, ClassMeta&lt;?&gt; hint) <jk>throws</jk> Exception {
 * 			<jk>return new</jk> MyBean(m.get(<js>"foo"</js>));
 * 		}
 * 	}
 * 
 * 	<jc>// Use it to serialize a byte array.</jc>
 * 	WriterSerializer s = JsonSerializer.<jsm>create</jsm>().simple().pojoSwaps(MyBeanSwap.<jk>class</jk>).build();
 * 	String json = s.serialize(<jk>new</jk> MyBean(<js>"bar"</js>));  <jc>// Produces "{foo:'bar'}"</jc>
 * </p>
 * 
 * @param <T> The normal form of the class.
 */
public abstract class MapSwap<T> extends PojoSwap<T,ObjectMap> {

	@Override /* PojoSwap */
	public ObjectMap swap(BeanSession session, T o) throws Exception {
		return super.swap(session, o);
	}

	@Override /* PojoSwap */
	public T unswap(BeanSession session, ObjectMap f, ClassMeta<?> hint) throws Exception {
		return super.unswap(session, f, hint);
	}
}
