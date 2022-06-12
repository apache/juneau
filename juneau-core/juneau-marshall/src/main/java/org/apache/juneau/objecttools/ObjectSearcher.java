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
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Provides searches against arrays and collections of maps and beans.
 *
 * <p>
 * 	The {@link ObjectSearcher} class is designed to provide searches across arrays and collections of beans and maps.
 * 	It allows you to quickly filter beans and maps using simple yet sophisticated search arguments.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MyBean[] <jv>arrayOfBeans</jv> = ...;
 * 	ObjectSearcher <jv>searcher</jv> = ObjectSearcher.<jsm>create</jsm>();
 *
 * 	BeanSession <jv>beanSession</jv> = BeanContext.<jsf>DEFAULT</jsf>.createSession();
 *
 * 	<jc>// Find beans whose 'foo' property is 'bar'.</jc>
 * 	SearchArgs <jv>searchArgs</jv> = SearchArgs.create("foo=X,bar=Y");
 *
 * 	<jc>// Returns a list of beans whose 'foo' property is 'X' and 'bar' property is 'Y'.</jc>
 * 	Object <jv>result</jv> = searcher.run(<jv>beanSession</jv>, <jv>arrayOfBeans</jv>, <jv>searchArgs</jv>);
 * </p>
 * <p>
 * 	The default searcher is configured with the following matcher factories that provides the capabilities of matching
 * 	against various data types:
 * </p>
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link StringMatcherFactory}
 * 	<li class='jc'>{@link NumberMatcherFactory}
 * 	<li class='jc'>{@link TimeMatcherFactory}
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.ObjectTools}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@SuppressWarnings({"rawtypes"})
public final class ObjectSearcher implements ObjectTool<SearchArgs> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Default reusable searcher.
	 */
	public static final ObjectSearcher DEFAULT = new ObjectSearcher();

	/**
	 * Static creator.
	 *
	 * @param factories
	 * 	The matcher factories to use.
	 * 	<br>If not specified, uses the following:
	 * 	<ul>
	 * 		<li>{@link StringMatcherFactory#DEFAULT}
	 * 		<li>{@link NumberMatcherFactory#DEFAULT}
	 * 		<li>{@link TimeMatcherFactory#DEFAULT}
	 * 	</ul>
	 * @return A new {@link ObjectSearcher} object.
	 */
	public static ObjectSearcher create(MatcherFactory...factories) {
		return new ObjectSearcher(factories);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final MatcherFactory[] factories;

	/**
	 * Constructor.
	 *
	 * @param factories
	 * 	The matcher factories to use.
	 * 	<br>If not specified, uses the following:
	 * 	<ul>
	 * 		<li>{@link NumberMatcherFactory#DEFAULT}
	 * 		<li>{@link TimeMatcherFactory#DEFAULT}
	 * 		<li>{@link StringMatcherFactory#DEFAULT}
	 * 	</ul>
	 */
	public ObjectSearcher(MatcherFactory...factories) {
		this.factories = factories.length == 0 ? new MatcherFactory[]{NumberMatcherFactory.DEFAULT, TimeMatcherFactory.DEFAULT, StringMatcherFactory.DEFAULT} : factories;
	}

	/**
	 * Convenience method for executing the searcher.
	 *
	 * @param input The input.
	 * @param searchArgs The search arguments.  See {@link SearchArgs} for format.
	 * @return A list of maps/beans matching the
	 */
	@SuppressWarnings("unchecked")
	public <R> List<R> run(Object input, String searchArgs) {
		Object r = run(BeanContext.DEFAULT_SESSION, input, SearchArgs.create(searchArgs));
		if (r instanceof List)
			return (List<R>)r;
		if (r instanceof Collection)
			return new ArrayList<R>((Collection)r);
		if (r.getClass().isArray())
			return Arrays.asList((R[])r);
		return null;
	}

	@Override /* ObjectTool */
	public Object run(BeanSession session, Object input, SearchArgs args) {

		ClassMeta<?> type = session.getClassMetaForObject(input);
		Map<String,String> search = args.getSearch();

		if (search.isEmpty() || type == null || ! type.isCollectionOrArray())
			return input;

		List<Object> l = null;
		RowMatcher rowMatcher = new RowMatcher(session, search);

		if (type.isCollection()) {
			Collection<?> c = (Collection)input;
			l = list(c.size());
			List<Object> l2 = l;
			c.forEach(x -> {
				if (rowMatcher.matches(x))
					l2.add(x);
			});

		} else /* isArray */ {
			int size = Array.getLength(input);
			l = list(size);
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

		@SuppressWarnings("unchecked")
		RowMatcher(BeanSession bs, Map query) {
			this.bs = bs;
			query.forEach((k,v) -> entryMatchers.put(stringify(k), new ColumnMatcher(bs, stringify(v))));
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
		AbstractMatcher[] matchers;
		BeanSession bs;

		ColumnMatcher(BeanSession bs, String searchPattern) {
			this.bs = bs;
			this.searchPattern = searchPattern;
			this.matchers = new AbstractMatcher[factories.length];
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