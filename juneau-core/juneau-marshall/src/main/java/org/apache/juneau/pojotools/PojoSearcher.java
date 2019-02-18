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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Designed to provide paging on POJOs consisting of arrays and collections.
 *
 * <p>
 * Allows you to quickly return subsets of arrays and collections based on position/limit arguments.
 */
@SuppressWarnings({"rawtypes"})
public final class PojoSearcher implements PojoTool<SearchArgs> {

	/**
	 * Default reusable searcher.
	 */
	public static final PojoSearcher DEFAULT = new PojoSearcher();

	final MatcherFactory[] factories;

	/**
	 * TODO
	 *
	 * @param factories
	 */
	public PojoSearcher(MatcherFactory...factories) {
		this.factories = factories;
	}

	/**
	 * TODO
	 *
	 */
	public PojoSearcher() {
		this(NumberMatcherFactory.DEFAULT, TimeMatcherFactory.DEFAULT, StringMatcherFactory.DEFAULT);
	}

	@Override /* PojoTool */
	public Object run(BeanSession session, Object input, SearchArgs args) {

		ClassMeta<?> type = session.getClassMetaForObject(input);
		Map<String,String> search = args.getSearch();

		if (search.isEmpty() || type == null || ! type.isCollectionOrArray())
			return input;

		List<Object> l = null;
		RowMatcher rowMatcher = new RowMatcher(session, search);

		if (type.isCollection()) {
			Collection c = (Collection)input;
			l = new ArrayList<>(c.size());
			for (Object o : c) {
				if (rowMatcher.matches(o))
					l.add(o);
			}

		} else /* isArray */ {
			int size = Array.getLength(input);
			l = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				Object o = Array.get(input, i);
				if (rowMatcher.matches(o))
					l.add(o);
			}
		}

		return l;
	}

	//====================================================================================================
	// MapMatcher
	//====================================================================================================
	/*
	 * Matches on a Map only if all specified entry matchers match.
	 */
	private class RowMatcher {

		Map<String,ColumnMatcher> entryMatchers = new HashMap<>();
		BeanSession bs;

		RowMatcher(BeanSession bs, Map query) {
			this.bs = bs;
			for (Map.Entry e : (Set<Map.Entry>)query.entrySet())
				entryMatchers.put(asString(e.getKey()), new ColumnMatcher(bs, asString(e.getValue())));
		}

		boolean matches(Object o) {
			if (o == null)
				return false;
			ClassMeta<?> cm = bs.getClassMetaForObject(o);
			if (cm.isMapOrBean()) {
				Map m = cm.isMap() ? (Map)o : bs.toBeanMap(o);
				for (Map.Entry<String,ColumnMatcher> e : entryMatchers.entrySet()) {
					String key = e.getKey();
					Object val = null;
					if (m instanceof BeanMap) {
						val = ((BeanMap)m).getRaw(key);
					} else {
						val = m.get(key);
					}
					if (! e.getValue().matches(val))
						return false;
				}
				return true;
			}
			if (cm.isCollection()) {
				for (Object o2 : (Collection)o)
					if (! matches(o2))
						return false;
				return true;
			}
			if (cm.isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					if (! matches(Array.get(o, i)))
						return false;
				return true;
			}
			return false;
		}
	}

	//====================================================================================================
	// ObjectMatcher
	//====================================================================================================
	/*
	 * Matcher that uses the correct matcher based on object type.
	 * Used for objects when we can't determine the object type beforehand.
	 */
	private class ColumnMatcher {

		String searchPattern;
		Matcher[] matchers;
		BeanSession bs;

		ColumnMatcher(BeanSession bs, String searchPattern) {
			this.bs = bs;
			this.searchPattern = searchPattern;
			this.matchers = new Matcher[factories.length];
		}

		boolean matches(Object o) {
			ClassMeta<?> cm = bs.getClassMetaForObject(o);
			if (cm == null)
				return false;
			if (cm.isCollection()) {
				for (Object o2 : (Collection)o)
					if (matches(o2))
						return true;
				return false;
			}
			if (cm.isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					if (matches(Array.get(o, i)))
						return true;
				return false;
			}
			for (int i = 0; i < factories.length; i++) {
				if (factories[i].canMatch(cm)) {
					if (matchers[i] == null)
						matchers[i] = factories[i].create(searchPattern);
					return matchers[i].matches(cm, o);
				}
			}
			return false;
		}
	}
}