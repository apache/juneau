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
package org.apache.juneau.objecttools;

import static java.util.Arrays.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * POJO model paginator.
 *
 * <p>
 * 	This class is designed to extract sublists from arrays/collections of maps or beans.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MyBean[] <jv>arrayOfBeans</jv> = ...;
 * 	ObjectPaginator <jv>paginator</jv> = ObjectPaginator.<jsm>create</jsm>();
 *
 * 	<jc>// Returns all rows from 100 to 110.</jc>
 * 	List&lt;MyBean&gt; <jv>result</jv> = <jv>paginator</jv>.run(<jv>arrayOfBeans</jv>, 100, 10);
 * </p>
 * <p>
 * 	The tool can be used against the following data types:
 * </p>
 * <ul>
 * 	<li>Arrays/collections of maps or beans.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public final class ObjectPaginator implements ObjectTool<PageArgs> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 * @return A new {@link ObjectPaginator} object.
	 */
	public static ObjectPaginator create() {
		return new ObjectPaginator();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for executing the paginator.
	 *
	 * @param <R> The collection element type.
	 * @param input The input.  Must be a collection or array of objects.
	 * @param pos The zero-index position to start from.
	 * @param limit The max number of entries to retrieve.
	 * @return A sublist of representing the entries from the position with the specified limit.
	 */
	@SuppressWarnings("unchecked")
	public <R> List<R> run(Object input, int pos, int limit) {
		BeanSession bs = BeanContext.DEFAULT_SESSION;
		Object r = run(BeanContext.DEFAULT_SESSION, input, PageArgs.create(pos, limit));
		if (r instanceof List)
			return (List<R>)r;
		return bs.convertToType(r, List.class);
	}

	@Override /* ObjectTool */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object run(BeanSession session, Object input, PageArgs args) {

		if (input == null)
			return null;

		ClassMeta type = session.getClassMetaForObject(input);

		if (! type.isCollectionOrArray())
			return input;

		int pos = args.getPosition();
		int limit = args.getLimit();

		if (type.isArray()) {
			int size = Array.getLength(input);
			int end = (limit+pos >= size) ? size : limit + pos;
			pos = Math.min(pos, size);
			ClassMeta<?> et = type.getElementType();
 			if (! et.isPrimitive())
				return copyOfRange((Object[])input, pos, end);
			if (et.is(boolean.class))
				return copyOfRange((boolean[])input, pos, end);
			if (et.is(byte.class))
				return copyOfRange((byte[])input, pos, end);
			if (et.is(char.class))
				return copyOfRange((char[])input, pos, end);
			if (et.is(double.class))
				return copyOfRange((double[])input, pos, end);
			if (et.is(float.class))
				return copyOfRange((float[])input, pos, end);
			if (et.is(int.class))
				return copyOfRange((int[])input, pos, end);
			if (et.is(long.class))
				return copyOfRange((long[])input, pos, end);
			return copyOfRange((short[])input, pos, end);
		}

		List l = type.isList() ? (List)input : new ArrayList((Collection)input);
		int end = (limit+pos >= l.size()) ? l.size() : limit + pos;
		pos = Math.min(pos, l.size());
		return l.subList(pos, end);
	}
}