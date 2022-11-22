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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * POJO model viewer.
 *
 * <p>
 * 	This class is designed to extract properties from collections of maps or beans.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MyBean[] <jv>arrayOfBeans</jv> = ...;
 * 	ObjectViewer <jv>viewer</jv> = ObjectViewer.<jsm>create</jsm>();
 *
 * 	<jc>// Returns the 'foo' and 'bar' properties extracted into a list of maps.</jc>
 * 	List&lt;Map&gt; <jv>result</jv> = <jv>viewer</jv>.run(<jv>arrayOfBeans</jv>, <js>"foo,bar"</js>);
 * </p>
 * <p>
 * 	The tool can be used against the following data types:
 * </p>
 * <ul>
 * 	<li>Arrays/collections of maps or beans.
 * 	<li>Singular maps or beans.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class ObjectViewer implements ObjectTool<ViewArgs> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Default reusable searcher.
	 */
	public static final ObjectViewer DEFAULT = new ObjectViewer();

	/**
	 * Static creator.
	 *
	 * @return A new {@link ObjectViewer} object.
	 */
	public static ObjectViewer create() {
		return new ObjectViewer();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Runs this viewer on the specified collection or array of objects.
	 *
	 * @param input The input.  Must be an array or collection.
	 * @param args The view args.  See {@link ViewArgs} for format.
	 * @return The extracted properties from the collection of objects.
	 */
	public List<Map> run(Object input, String args) {
		return (List<Map>)run(BeanContext.DEFAULT_SESSION, input, ViewArgs.create(args));
	}

	/**
	 * Runs this viewer on a singleton object.
	 *
	 * @param input The input.  Must be a singleton object.
	 * @param args The view args.  See {@link ViewArgs} for format.
	 * @return The extracted properties from the object.
	 */
	public Map runSingle(Object input, String args) {
		return (Map)run(BeanContext.DEFAULT_SESSION, input, ViewArgs.create(args));
	}

	@Override /* ObjectTool */
	public Object run(BeanSession session, Object input, ViewArgs args) {

		if (input == null)
			return null;

		List<String> view = args.getView();
		ClassMeta type = session.getClassMetaForObject(input);

		if (type.isBeanMap())
			return new DelegateBeanMap(((BeanMap)input).getBean(), session).filterKeys(view);
		if (type.isMap())
			return new DelegateMap((Map)input, session).filterKeys(view);
		if (type.isBean())
			return new DelegateBeanMap(input, session).filterKeys(view);

		ArrayList<Object> l = null;

		if (type.isArray()) {
			int size = Array.getLength(input);
			l = list(size);
			for (int i = 0; i < size; i++)
				l.add(Array.get(input, i));
		} else if (type.isCollection()) {
			Collection c = (Collection)input;
			l = list(c.size());
			List<Object> l2 = l;
			c.forEach(x -> l2.add(x));
		} else {
			return input;
		}

		for (ListIterator li = l.listIterator(); li.hasNext();) {
			Object o = li.next();
			ClassMeta cm2 = session.getClassMetaForObject(o);

			if (cm2 == null)
				o = null;
			else if (cm2.isBeanMap())
				o = new DelegateBeanMap(((BeanMap)o).getBean(), session).filterKeys(view);
			else if (cm2.isMap())
				o = new DelegateMap((Map)o, session).filterKeys(view);
			else if (cm2.isBean())
				o = new DelegateBeanMap(o, session).filterKeys(view);

			li.set(o);
		}

		return l;
	}
}