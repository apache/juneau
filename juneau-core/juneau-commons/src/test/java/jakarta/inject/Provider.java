/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jakarta.inject;

/**
 * Test-only stand-in for the real {@code jakarta.inject.Provider} interface.
 *
 * <p>
 * {@code juneau-commons} deliberately has no compile-time dependency on the {@code jakarta.inject-api}
 * artifact - {@link org.apache.juneau.commons.inject.JsrSupport} recognizes JSR-330 provider types purely by
 * fully-qualified class NAME (see {@code JsrSupport#isProviderType(Class)}), not by an actual interface
 * reference. This minimal local re-declaration under the same FQN lets tests exercise that name-based
 * detection path (and the {@code java.lang.reflect.Proxy}-backed {@code InvocationHandler} it drives in
 * {@code ParameterInfo#resolveValue}) without pulling in the real dependency.
 *
 * @param <T> The type of value provided by this provider.
 */
public interface Provider<T> {

	/**
	 * Returns an instance of the provided type.
	 *
	 * @return An instance of the provided type.
	 */
	T get();

	/**
	 * Not part of the real JSR-330 {@code Provider} API - declared purely so a proxy backed by this test-only
	 * stand-in can invoke a method that {@code ParameterInfo#resolveValue}'s dynamic {@code InvocationHandler}
	 * doesn't special-case (unlike {@code get}/{@code toString}/{@code hashCode}/{@code equals}), exercising
	 * its final "unsupported provider method" fallback branch.
	 */
	void unsupportedMethod();
}
