/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.transform;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;


/**
 * Specialized {@link PojoSwap} for surrogate classes.
 * <p>
 * Surrogate classes are used in place of other classes during serialization.
 * For example, you may want to use a surrogate class to change the names or order of bean
 * properties on a bean.
 * <p>
 * The following is an example of a surrogate class change changes a property name:
 * <p class='bcode'>
 * 	<jk>public class</jk> SurrogateClass {
 * 		<jk>public</jk> String surrogateField;  <jc>// New bean property</jc>
 *
 * 		<jk>public</jk> SurrogateClass(NormalClass normalClass) {
 * 			<jk>this</jk>.surrogateField = normalClass.normalField;
 * 		}
 * 	}
 * </p>
 * <p>
 * Optionally, a public static method can be used to untransform a class during parsing:
 * <p class='bcode'>
 * 	<jk>public class</jk> SurrogateClass {
 * 		...
 * 		<jk>public static</jk> NormalClass <jsm>toNormalClass</jsm>(SurrogateClass surrogateClass) {
 * 			<jk>return new</jk> NormalClass(surrogateClass.transformedField);
 * 		}
 * 	}
 * </p>
 * <p>
 * Surrogate classes must conform to the following:
 * <ul class='spaced-list'>
 * 	<li>It must have a one or more public constructors that take in a single parameter whose type is the normal types.
 * 		(It is possible to define a class as a surrogate for multiple class types by using multiple constructors with
 * 		different parameter types).
 * 	<li>It optionally can have a public static method that takes in a single parameter whose type is the transformed type
 * 		and returns an instance of the normal type.  This is called the untransform method.  The method can be called anything.
 * 	<li>If an untransform method is present, the class must also contain a no-arg constructor (so that the transformed class
 * 		can be instantiated by the parser before being converted into the normal class by the untransform method).
 * </ul>
 * <p>
 * Surrogate classes are associated with serializers and parsers using the {@link CoreApi#addTransforms(Class...)} method.
 * <p class='bcode'>
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> test() <jk>throws</jk> Exception {
 * 		JsonSerializer s = <jk>new</jk> JsonSerializer.Simple().addTransforms(Surrogate.<jk>class</jk>);
 * 		JsonParser p = <jk>new</jk> JsonParser().addTransforms(Surrogate.<jk>class</jk>);
 * 		String r;
 * 		Normal n = Normal.<jsm>create</jsm>();
 *
 * 		r = s.serialize(n);
 * 		assertEquals(<js>"{f2:'f1'}"</js>, r);
 *
 * 		n = p.parse(r, Normal.<jk>class</jk>);
 * 		assertEquals(<js>"f1"</js>, n.f1);
 * 	}
 *
 * 	<jc>// The normal class</jc>
 * 	<jk>public class</jk> Normal {
 * 		<jk>public</jk> String f1;
 *
 * 		<jk>public static</jk> Normal <jsm>create</jsm>() {
 * 			Normal n = <jk>new</jk> Normal();
 * 			n.f1 = <js>"f1"</js>;
 * 			<jk>return</jk> n;
 * 		}
 * 	}
 *
 * 	<jc>// The surrogate class</jc>
 * 	<jk>public static class</jk> Surrogate {
 * 		<jk>public</jk> String f2;
 *
 * 		<jc>// Surrogate constructor</jc>
 * 		<jk>public</jk> Surrogate(Normal n) {
 * 			f2 = n.f1;
 * 		}
 *
 * 		<jc>// Constructor used during parsing (only needed if untransform method specified)</jc>
 * 		<jk>public</jk> Surrogate() {}
 *
 * 		<jc>// Untransform method (optional)</jc>
 * 		<jk>public static</jk> Normal <jsm>toNormal</jsm>(Surrogate f) {
 * 			Normal n = <jk>new</jk> Normal();
 * 			n.f1 = f.f2;
 * 			<jk>return</jk> n;
 * 		}
 * 	}
 * </p>
 * <p>
 * It should be noted that a surrogate class is functionally equivalent to the following {@link PojoSwap} implementation:
 * <p class='bcode'>
 * 	<jk>public static class</jk> SurrogateSwap <jk>extends</jk> PojoSwap&lt;Normal,Surrogate&gt; {
 * 		<jk>public</jk> Surrogate swap(Normal n) <jk>throws</jk> SerializeException {
 * 			<jk>return new</jk> Surrogate(n);
 * 		}
 * 		<jk>public</jk> Normal unswap(Surrogate s, ClassMeta<?> hint) <jk>throws</jk> ParseException {
 * 			<jk>return</jk> Surrogate.<jsm>toNormal</jsm>(s);
 * 		}
 * 	}
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type that this transform applies to.
 * @param <F> The transformed class type.
 */
public class SurrogateSwap<T,F> extends PojoSwap<T,F> {

	private Constructor<F> constructor;   // public F(T t);
	private Method untransformMethod;        // public static T valueOf(F f);

	/**
	 * Constructor.
	 *
	 * @param forClass The normal class.
	 * @param constructor The constructor on the surrogate class that takes the normal class as a parameter.
	 * @param untransformMethod The static method that converts surrogate objects into normal objects.
	 */
	protected SurrogateSwap(Class<T> forClass, Constructor<F> constructor, Method untransformMethod) {
		super(forClass, constructor.getDeclaringClass());
		this.constructor = constructor;
		this.untransformMethod = untransformMethod;
	}

	/**
	 * Given the specified surrogate class, return the list of POJO swaps.
	 * A transform is returned for each public 1-arg constructor found.
	 * Returns an empty list if no public 1-arg constructors are found.
	 *
	 * @param c The surrogate class.
	 * @return The list of POJO swaps that apply to this class.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static List<SurrogateSwap<?,?>> findTransforms(Class<?> c) {
		List<SurrogateSwap<?,?>> l = new LinkedList<SurrogateSwap<?,?>>();
		for (Constructor<?> cc : c.getConstructors()) {
			if (cc.getAnnotation(BeanIgnore.class) == null) {
				Class<?>[] pt = cc.getParameterTypes();

				// Only constructors with one parameter.
				// Ignore instance class constructors.
				if (pt.length == 1 && pt[0] != c.getDeclaringClass()) {
					int mod = cc.getModifiers();
					if (Modifier.isPublic(mod)) {  // Only public constructors.

						// Find the untransform method if there is one.
						Method untransformMethod = null;
						for (Method m : c.getMethods()) {
							if (pt[0].equals(m.getReturnType())) {
								Class<?>[] mpt = m.getParameterTypes();
								if (mpt.length == 1 && mpt[0].equals(c)) { // Only methods with one parameter and where the return type matches this class.
									int mod2 = m.getModifiers();
									if (Modifier.isPublic(mod2) && Modifier.isStatic(mod2))  // Only public static methods.
										untransformMethod = m;
								}
							}
						}

						l.add(new SurrogateSwap(pt[0], cc, untransformMethod));
					}
				}
			}
		}
		return l;
	}

	@Override /* PojoSwap */
	public F swap(T o) throws SerializeException {
		try {
			return constructor.newInstance(o);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	@Override /* PojoSwap */
	@SuppressWarnings("unchecked")
	public T unswap(F f) throws ParseException {
		if (untransformMethod == null)
			throw new ParseException("static valueOf({0}) method not implement on surrogate class ''{1}''", f.getClass().getName(), getNormalClass().getName());
		try {
			return (T)untransformMethod.invoke(null, f);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
