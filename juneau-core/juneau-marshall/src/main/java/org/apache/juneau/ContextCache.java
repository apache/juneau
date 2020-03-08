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
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Stores a cache of {@link Context} instances mapped by the property stores used to create them.
 *
 * <p>
 * The purpose of this class is to reuse instances of bean contexts, serializers, and parsers when they're being
 * re-created with previously-used property stores.
 *
 * <p>
 * Since serializers and parsers are immutable and thread-safe, we reuse them whenever possible.
 */
@SuppressWarnings("unchecked")
public class ContextCache {

	/**
	 * Reusable cache instance.
	 */
	public static final ContextCache INSTANCE = new ContextCache();

	private final static boolean USE_DEEP_MATCHING = Boolean.getBoolean("ContextCache.useDeepMatching");

	private final ConcurrentHashMap<Class<?>,ConcurrentHashMap<Integer,CacheEntry>> contextCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<?>,String[]> prefixCache = new ConcurrentHashMap<>();

	// When enabled, this will spit out cache-hit metrics to the console on shutdown.
	private static final boolean TRACK_CACHE_HITS = Boolean.getBoolean("juneau.trackCacheHits");
	static final Map<String,CacheHit> CACHE_HITS = new ConcurrentHashMap<>();
	static {
		if (TRACK_CACHE_HITS) {
			Runtime.getRuntime().addShutdownHook(
				new Thread() {
					@Override
					public void run() {
						int creates=0, cached=0;
						System.out.println("Cache Hits:  [CacheObject] = [numCreated,numCached,cacheHitPercentage]");
						for (Map.Entry<String,CacheHit> e : CACHE_HITS.entrySet()) {
							CacheHit ch = e.getValue();
							System.out.println("["+e.getKey()+"] = ["+ch.creates+","+ch.cached+","+((ch.cached*100)/(ch.creates+ch.cached))+"%]");
							creates += ch.creates;
							cached += ch.cached;
						}
						if (creates + cached > 0)
							System.out.println("[total] = ["+creates+","+cached+","+((cached*100)/(creates+cached))+"%]");
					}
				}
			);
		}
	}

	static void logCache(Class<?> contextClass, boolean wasCached) {
		if (TRACK_CACHE_HITS) {
			synchronized(ContextCache.class) {
				String c = contextClass.getSimpleName();
				CacheHit ch = CACHE_HITS.get(c);
				if (ch == null)
					ch = new CacheHit();
				if (wasCached)
					ch.cached++;
				else
					ch.creates++;
				ch = CACHE_HITS.put(c, ch);
			}
		}
	}

	static class CacheHit {
		public int creates, cached;
	}

	ContextCache() {}

	/**
	 * Creates a new instance of the specified context-based class, or an existing instance if one with the same
	 * property store was already created.
	 *
	 * @param c The instance of the class to create.
	 * @param ps The property store to use to create the class.
	 * @return The
	 */
	public <T extends Context> T create(Class<T> c, PropertyStore ps) {
		String[] prefixes = getPrefixes(c);

		if (prefixes == null)
			return instantiate(c, ps);

		ConcurrentHashMap<Integer,CacheEntry> m = getContextCache(c);

		Integer hashCode = ps.hashCode(prefixes);
		CacheEntry ce = m.get(hashCode);

		if (ce != null && USE_DEEP_MATCHING && ! ce.ps.equals(ps))
			throw new ContextRuntimeException("Property store hashcode mismatch!", new Object[0]);

		logCache(c, ce != null);

		if (ce == null) {
			ce = new CacheEntry(ps, instantiate(c, ps));
			m.putIfAbsent(hashCode, ce);
		}

		return (T)ce.context;
	}

	private <T extends Context> T instantiate(Class<T> c, PropertyStore ps) {
		try {
			return newInstance(c, ps);
		} catch (ContextRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ContextRuntimeException(e, "Could not create instance of class ''{0}''", c);
		}
	}

	private ConcurrentHashMap<Integer,CacheEntry> getContextCache(Class<?> c) {
		ConcurrentHashMap<Integer,CacheEntry> m = contextCache.get(c);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			ConcurrentHashMap<Integer,CacheEntry> m2 = contextCache.putIfAbsent(c, m);
			if (m2 != null)
				m = m2;
		}
		return m;
	}

	private String[] getPrefixes(Class<?> c) {
		String[] prefixes = prefixCache.get(c);
		if (prefixes == null) {
			Set<String> ps = new HashSet<>();
			for (ClassInfo c2 : ClassInfo.of(c).getAllParentsChildFirst()) {
				ConfigurableContext cc = c2.getLastAnnotation(ConfigurableContext.class);
				if (cc != null) {
					if (cc.nocache()) {
						prefixes = new String[0];
						break;
					}
					if (cc.prefixes().length == 0)
						ps.add(c2.getSimpleName());
					else
						ps.addAll(Arrays.asList(cc.prefixes()));
				}
			}
			prefixes = ps.toArray(new String[ps.size()]);
			String[] p2 = prefixCache.putIfAbsent(c, prefixes);
			if (p2 != null)
				prefixes = p2;
		}
		return prefixes.length == 0 ? null : prefixes;
	}

	private <T> T newInstance(Class<T> cc, PropertyStore ps) throws Exception {
		return (T)castOrCreate(Context.class, cc, true, ps);
	}

	private static class CacheEntry {
		final PropertyStore ps;
		final Context context;

		CacheEntry(PropertyStore ps, Context context) {
			this.ps = ps;
			this.context = context;
		}
	}
}
