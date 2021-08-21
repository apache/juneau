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
package org.apache.juneau.serializer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for creating instances of {@link SerializerGroup}.
 */
@FluentSetters
public class SerializerGroupBuilder {

	private final AList<Object> serializers;
	private BeanContextBuilder bcBuilder;

	/**
	 * Create an empty serializer group builder.
	 */
	public SerializerGroupBuilder() {
		this.serializers = AList.create();
	}

	/**
	 * Associates an existing bean context builder with all serializer builders in this group.
	 *
	 * @param value The bean contest builder to associate.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder beanContextBuilder(BeanContextBuilder value) {
		bcBuilder = value;
		forEach(x -> x.beanContextBuilder(value));
		return this;
	}

	/**
	 * Clone an existing serializer group builder.
	 *
	 * @param copyFrom The serializer group that we're copying settings and serializers from.
	 */
	public SerializerGroupBuilder(SerializerGroup copyFrom) {
		this.serializers = AList.create().appendReverse(copyFrom.getSerializers());
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	@SafeVarargs
	public final SerializerGroupBuilder append(Class<? extends Serializer>...s) {
		serializers.appendReverse(instantiate(Arrays.asList(s)));
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * <p>
	 * When passing in pre-instantiated serializers to this group, applying properties and transforms to the group
	 * do not affect them.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Serializer...s) {
		serializers.appendReverse(instantiate(Arrays.asList(s)));
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * <p>
	 * Objects can either be instances of serializers or serializer classes.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(List<Object> s) {
		serializers.appendReverse(instantiate(s));
		return this;
	}

	/**
	 * Registers the specified serializers with this group.
	 *
	 * <p>
	 * Objects can either be instances of serializers or serializer classes.
	 *
	 * @param s The serializers to append to this group.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder append(Object...s) {
		serializers.appendReverse(instantiate(Arrays.asList(s)));
		return this;
	}

	/**
	 * Creates a new {@link SerializerGroup} object using a snapshot of the settings defined in this builder.
	 *
	 * <p>
	 * This method can be called multiple times to produce multiple serializer groups.
	 *
	 * @return A new {@link SerializerGroup} object.
	 */
	public SerializerGroup build() {
		List<Serializer> l = new ArrayList<>();
		for (Object s : serializers) {
			if (s instanceof SerializerBuilder) {
				l.add(((SerializerBuilder)s).build());
			} else {
				l.add((Serializer)s);
			}
		}
		return new SerializerGroup(ArrayUtils.toReverseArray(Serializer.class, l));
	}

	/**
	 * Performs an action on all serializer builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder forEach(Consumer<SerializerBuilder> action) {
		builders(SerializerBuilder.class).forEach(action);
		return this;
	}

	/**
	 * Performs an action on all writer serializer builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder forEachWS(Consumer<WriterSerializerBuilder> action) {
		return forEach(WriterSerializerBuilder.class, action);
	}

	/**
	 * Performs an action on all output stream serializer builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public SerializerGroupBuilder forEachOSS(Consumer<OutputStreamSerializerBuilder> action) {
		return forEach(OutputStreamSerializerBuilder.class, action);
	}

	/**
	 * Performs an action on all serializer builders of the specified type in this group.
	 *
	 * @param type The serializer builder type.
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public <T extends SerializerBuilder> SerializerGroupBuilder forEach(Class<T> type, Consumer<T> action) {
		builders(type).forEach(action);
		return this;
	}

	@SuppressWarnings("unchecked")
	private List<Object> instantiate(List<?> l) {
		List<Object> l2 = new ArrayList<>(l.size());
		for (int i = 0; i < l.size(); i++) {
			Object o = l.get(i);
			if (o instanceof Class) {
				SerializerBuilder b = Serializer.createSerializerBuilder((Class<? extends Serializer>)o);
				if (bcBuilder != null)
					b.beanContextBuilder(bcBuilder);
				o = b;
			}
			l2.add(o);
		}
		return l2;
	}

	@SuppressWarnings("unchecked")
	private <T extends SerializerBuilder> Stream<T> builders(Class<T> type) {
		return serializers.stream().filter(x -> type.isInstance(x)).map(x -> (T)x);
	}

}
