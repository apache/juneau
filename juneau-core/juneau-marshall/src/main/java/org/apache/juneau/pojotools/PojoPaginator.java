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
package org.apache.juneau.pojotools;

import org.apache.juneau.*;

/**
 * Designed to provide paging on POJOs consisting of arrays and collections.
 * {@review}
 *
 * <p>
 * Allows you to quickly return subsets of arrays and collections based on position/limit arguments.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.PojoTools}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class PojoPaginator implements PojoTool<Object> {

	@Override /* PojoTool */
	public Object run(BeanSession session, Object input, Object args) {

//		if (input == null)
//			return null;
//
//		ClassMeta type = session.getClassMetaForObject(input);
//
//		if (! type.isCollectionOrArray())
//			return input;
//
//		int pos = args.getPosition();
//		int limit = args.getLimit();
//
//		if (type.isArray()) {
//			int size = Array.getLength(input);
//			int end = (limit+pos >= size) ? size : limit + pos;
//			pos = Math.min(pos, size);
//			ClassMeta<?> et = type.getElementType();
// 			if (! et.isPrimitive())
//				return copyOfRange((Object[])input, pos, end);
//			if (et.isType(boolean.class))
//				return copyOfRange((boolean[])input, pos, end);
//			if (et.isType(byte.class))
//				return copyOfRange((byte[])input, pos, end);
//			if (et.isType(char.class))
//				return copyOfRange((char[])input, pos, end);
//			if (et.isType(double.class))
//				return copyOfRange((double[])input, pos, end);
//			if (et.isType(float.class))
//				return copyOfRange((float[])input, pos, end);
//			if (et.isType(int.class))
//				return copyOfRange((int[])input, pos, end);
//			if (et.isType(long.class))
//				return copyOfRange((long[])input, pos, end);
//			if (et.isType(short.class))
//				return copyOfRange((short[])input, pos, end);
//			return null;
//		}
//
//		List l = type.isList() ? (List)input : new ArrayList((Collection)input);
//		int end = (limit+pos >= l.size()) ? l.size() : limit + pos;
//		pos = Math.min(pos, l.size());
//		return l.subList(pos, end);
		return null;
	}
}