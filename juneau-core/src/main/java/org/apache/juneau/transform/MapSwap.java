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
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Abstract subclass for POJO swaps that swap objects for object maps.
 *
 * @param <T> The normal form of the class.
 */
public abstract class MapSwap<T> extends PojoSwap<T,ObjectMap> {

	@Override /* PojoSwap */
	public ObjectMap swap(BeanSession session, T o) throws SerializeException {
		return super.swap(session, o);
	}

	@Override /* PojoSwap */
	public T unswap(BeanSession session, ObjectMap f, ClassMeta<?> hint) throws ParseException {
		return super.unswap(session, f, hint);
	}
}
