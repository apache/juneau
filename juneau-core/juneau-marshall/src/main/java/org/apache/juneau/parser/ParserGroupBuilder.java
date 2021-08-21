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
package org.apache.juneau.parser;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Builder class for creating instances of {@link ParserGroup}.
 */
@FluentSetters
public class ParserGroupBuilder {

	private final AList<Object> parsers;
	private BeanContextBuilder bcBuilder;

	/**
	 * Create an empty parser group builder.
	 */
	public ParserGroupBuilder() {
		this.parsers = AList.create();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public ParserGroupBuilder(ParserGroup copyFrom) {
		this.parsers = AList.create().appendReverse(copyFrom.getParsers());
	}

	/**
	 * Associates an existing bean context builder with all serializer builders in this group.
	 *
	 * @param value The bean contest builder to associate.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder beanContextBuilder(BeanContextBuilder value) {
		bcBuilder = value;
		forEach(x -> x.beanContextBuilder(value));
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(Class<?>...p) {
		parsers.appendReverse(instantiate(Arrays.asList(p)));
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * <p>
	 * When passing in pre-instantiated parsers to this group, applying properties and transforms to the group
	 * do not affect them.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(Parser...p) {
		parsers.appendReverse(instantiate(Arrays.asList(p)));
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * <p>
	 * Objects can either be instances of parsers or parser classes.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(List<Object> p) {
		parsers.appendReverse(instantiate(p));
		return this;
	}

	/**
	 * Registers the specified parsers with this group.
	 *
	 * <p>
	 * Objects can either be instances of parsers or parser classes.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder append(Object...p) {
		parsers.appendReverse(instantiate(Arrays.asList(p)));
		return this;
	}

	/**
	 * Creates a new {@link ParserGroup} object using a snapshot of the settings defined in this builder.
	 *
	 * <p>
	 * This method can be called multiple times to produce multiple parser groups.
	 *
	 * @return A new {@link ParserGroup} object.
	 */
	public ParserGroup build() {
		List<Parser> l = new ArrayList<>();
		for (Object p : parsers) {
			if (p instanceof ParserBuilder) {
				l.add(((ParserBuilder)p).build());
			} else {
				l.add((Parser)p);
			}
		}
		return new ParserGroup(ArrayUtils.toReverseArray(Parser.class, l));
	}

	/**
	 * Performs an action on all parser builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder forEach(Consumer<ParserBuilder> action) {
		builders(ParserBuilder.class).forEach(action);
		return this;
	}

	/**
	 * Performs an action on all reader parser builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder forEachRP(Consumer<ReaderParserBuilder> action) {
		return forEach(ReaderParserBuilder.class, action);
	}

	/**
	 * Performs an action on all input stream parser builders in this group.
	 *
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public ParserGroupBuilder forEachISP(Consumer<InputStreamParserBuilder> action) {
		return forEach(InputStreamParserBuilder.class, action);
	}

	/**
	 * Performs an action on all parser builders of the specified type in this group.
	 *
	 * @param type The parser builder type.
	 * @param action The action to perform.
	 * @return This object (for method chaining).
	 */
	public <T extends ParserBuilder> ParserGroupBuilder forEach(Class<T> type, Consumer<T> action) {
		builders(type).forEach(action);
		return this;
	}

	@SuppressWarnings("unchecked")
	private List<Object> instantiate(List<?> l) {
		List<Object> l2 = new ArrayList<>(l.size());
		for (int i = 0; i < l.size(); i++) {
			Object o = l.get(i);
			if (o instanceof Class) {
				ParserBuilder b = Parser.createParserBuilder((Class<? extends Parser>)o);
				if (bcBuilder != null)
					b.beanContextBuilder(bcBuilder);
				o = b;
			}
			l2.add(o);
		}
		return l2;
	}

	@SuppressWarnings("unchecked")
	private <T extends ParserBuilder> Stream<T> builders(Class<T> type) {
		return parsers.stream().filter(x -> type.isInstance(x)).map(x -> (T)x);
	}
}
