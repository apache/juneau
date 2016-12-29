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
package org.apache.juneau.msgpack;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link MsgPackSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link MsgPackSerializer#setProperty(String,Object)}
 * 	<li>{@link MsgPackSerializer#setProperties(ObjectMap)}
 * 	<li>{@link MsgPackSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link MsgPackSerializer#addBeanFilters(Class[])}
 * 	<li>{@link MsgPackSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link MsgPackSerializer#addToDictionary(Class[])}
 * 	<li>{@link MsgPackSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the MessagePack serializer</h6>
 * <p>
 * 	None.
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 */
public final class MsgPackSerializerContext extends SerializerContext {

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public MsgPackSerializerContext(ContextFactory cf) {
		super(cf);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("MsgPackSerializerContext", new ObjectMap()
			);
	}
}
