/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filter;

import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;


/**
 * Specialized {@link PojoFilter} for surrogate classes.
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
 * Optionally, a public static method can be used to unfilter a class during parsing:
 * <p class='bcode'>
 * 	<jk>public class</jk> SurrogateClass {
 * 		...
 * 		<jk>public static</jk> NormalClass <jsm>toNormalClass</jsm>(SurrogateClass surrogateClass) {
 * 			<jk>return new</jk> NormalClass(surrogateClass.filteredField);
 * 		}
 * 	}
 * </p>
 * <p>
 * Surrogate classes must conform to the following:
 * <ul>
 * 	<li>It must have a one or more public constructors that take in a single parameter whose type is the normal types.
 * 		(It is possible to define a class as a surrogate for multiple class types by using multiple constructors with
 * 		different parameter types).
 * 	<li>It optionally can have a public static method that takes in a single parameter whose type is the filtered type
 * 		and returns an instance of the normal type.  This is called the unfilter method.  The method can be called anything.
 * 	<li>If an unfilter method is present, the class must also contain a no-arg constructor (so that the filtered class
 * 		can be instantiated by the parser before being converted into the normal class by the unfilter method).
 * </ul>
 * <p>
 * Surrogate classes are associated with serializers and parsers using the {@link CoreApi#addFilters(Class...)} method.
 * <p class='bcode'>
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> test() <jk>throws</jk> Exception {
 * 		JsonSerializer s = <jk>new</jk> JsonSerializer.Simple().addFilters(Surrogate.<jk>class</jk>);
 * 		JsonParser p = <jk>new</jk> JsonParser().addFilters(Surrogate.<jk>class</jk>);
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
 * 		<jc>// Constructor used during parsing (only needed if unfilter method specified)</jc>
 * 		<jk>public</jk> Surrogate() {}
 *
 * 		<jc>// Unfilter method (optional)</jc>
 * 		<jk>public static</jk> Normal <jsm>toNormal</jsm>(Surrogate f) {
 * 			Normal n = <jk>new</jk> Normal();
 * 			n.f1 = f.f2;
 * 			<jk>return</jk> n;
 * 		}
 * 	}
 * </p>
 * <p>
 * It should be noted that a surrogate class is functionally equivalent to the following {@link PojoFilter} implementation:
 * <p class='bcode'>
 * 	<jk>public static class</jk> SurrogateFilter <jk>extends</jk> PojoFilter&lt;Normal,Surrogate&gt; {
 * 		<jk>public</jk> Surrogate filter(Normal n) <jk>throws</jk> SerializeException {
 * 			<jk>return new</jk> Surrogate(n);
 * 		}
 * 		<jk>public</jk> Normal unfilter(Surrogate s, ClassMeta<?> hint) <jk>throws</jk> ParseException {
 * 			<jk>return</jk> Surrogate.<jsm>toNormal</jsm>(s);
 * 		}
 * 	}
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type that this filter applies to.
 * @param <F> The filtered class type.
 */
public class SurrogateFilter<T,F> extends PojoFilter<T,F> {

	private Constructor<F> constructor;   // public F(T t);
	private Method unfilterMethod;        // public static T valueOf(F f);

	/**
	 * Constructor.
	 *
	 * @param forClass The normal class.
	 * @param constructor The constructor on the surrogate class that takes the normal class as a parameter.
	 * @param unfilterMethod The static method that converts surrogate objects into normal objects.
	 */
	protected SurrogateFilter(Class<T> forClass, Constructor<F> constructor, Method unfilterMethod) {
		super(forClass, constructor.getDeclaringClass());
		this.constructor = constructor;
		this.unfilterMethod = unfilterMethod;
	}

	/**
	 * Given the specified surrogate class, return the list of POJO filters.
	 * A filter is returned for each public 1-arg constructor found.
	 * Returns an empty list if no public 1-arg constructors are found.
	 *
	 * @param c The surrogate class.
	 * @return The list of POJO filters that apply to this class.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static List<SurrogateFilter<?,?>> findFilters(Class<?> c) {
		List<SurrogateFilter<?,?>> l = new LinkedList<SurrogateFilter<?,?>>();
		for (Constructor<?> cc : c.getConstructors()) {
			if (cc.getAnnotation(BeanIgnore.class) == null) {
				Class<?>[] pt = cc.getParameterTypes();

				// Only constructors with one parameter.
				// Ignore instance class constructors.
				if (pt.length == 1 && pt[0] != c.getDeclaringClass()) {
					int mod = cc.getModifiers();
					if (Modifier.isPublic(mod)) {  // Only public constructors.

						// Find the unfilter method if there is one.
						Method unfilterMethod = null;
						for (Method m : c.getMethods()) {
							if (pt[0].equals(m.getReturnType())) {
								Class<?>[] mpt = m.getParameterTypes();
								if (mpt.length == 1 && mpt[0].equals(c)) { // Only methods with one parameter and where the return type matches this class.
									int mod2 = m.getModifiers();
									if (Modifier.isPublic(mod2) && Modifier.isStatic(mod2))  // Only public static methods.
										unfilterMethod = m;
								}
							}
						}

						l.add(new SurrogateFilter(pt[0], cc, unfilterMethod));
					}
				}
			}
		}
		return l;
	}

	@Override /* PojoFilter */
	public F filter(T o) throws SerializeException {
		try {
			return constructor.newInstance(o);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	@Override /* PojoFilter */
	@SuppressWarnings("unchecked")
	public T unfilter(F f, ClassMeta<?> hint) throws ParseException {
		if (unfilterMethod == null)
			throw new ParseException("static valueOf({0}) method not implement on surrogate class ''{1}''", f.getClass().getName(), getNormalClass().getName());
		try {
			return (T)unfilterMethod.invoke(null, f);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
