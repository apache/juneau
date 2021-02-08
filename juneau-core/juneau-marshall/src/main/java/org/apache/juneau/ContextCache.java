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
import org.apache.juneau.collections.*;
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

	private final ConcurrentHashMap<Class<?>,ConcurrentHashMap<ContextProperties,Context>> contextCache = new ConcurrentHashMap<>();
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
	 * @param cp The property store to use to create the class.
	 * @return The
	 */
	public <T extends Context> T create(Class<T> c, ContextProperties cp) {
		String[] prefixes = getPrefixes(c);

		if (prefixes == null)
			return instantiate(c, cp);

		ConcurrentHashMap<ContextProperties,Context> m = getContextCache(c);

		cp = cp.subset(prefixes);

		Context context = m.get(cp);

		logCache(c, context != null);

		if (context == null) {
			context = instantiate(c, cp);
			m.putIfAbsent(cp, context);
		}

		return (T)context;
	}

	private <T extends Context> T instantiate(Class<T> c, ContextProperties cp) {
		try {
			return newInstance(c, cp);
		} catch (ContextRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ContextRuntimeException(e, "Could not create instance of class ''{0}''", c);
		}
	}

	private ConcurrentHashMap<ContextProperties,Context> getContextCache(Class<?> c) {
		ConcurrentHashMap<ContextProperties,Context> m = contextCache.get(c);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			ConcurrentHashMap<ContextProperties,Context> m2 = contextCache.putIfAbsent(c, m);
			if (m2 != null)
				m = m2;
		}
		return m;
	}

	private String[] getPrefixes(Class<?> c) {
		String[] prefixes = prefixCache.get(c);
		if (prefixes == null) {
			ASet<String> ps = ASet.of();
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
						ps.a(cc.prefixes());
				}
			}
			prefixes = ps.toArray(new String[ps.size()]);
			String[] p2 = prefixCache.putIfAbsent(c, prefixes);
			if (p2 != null)
				prefixes = p2;
		}
		return prefixes.length == 0 ? null : prefixes;
	}

	private <T> T newInstance(Class<T> cc, ContextProperties cp) throws Exception {
		return (T)castOrCreate(Context.class, cc, true, cp);
	}
}
