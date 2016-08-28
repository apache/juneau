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
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Used to swap out non-serializable objects with serializable replacements during serialization, and vis-versa during parsing.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	<code>PojoSwaps</code> are used to extend the functionality of the serializers and parsers to be able to handle POJOs
 * 	that aren't automatically handled by the serializers or parsers.  For example, JSON does not have a standard
 * 	representation for rendering dates.  By defining a special {@code Date} swap and associating it with a serializer and
 * 	parser, you can convert a {@code Date} object to a {@code String} during serialization, and convert that {@code String} object back into
 * 	a {@code Date} object during parsing.
 * <p>
 * 	Swaps MUST declare a public no-arg constructor so that the bean context can instantiate them.
 * <p>
 * 	<code>PojoSwaps</code> are associated with instances of {@link BeanContext BeanContexts} by passing the swap class to
 * 	the {@link CoreApi#addPojoSwaps(Class...)} method.<br>
 * 	When associated with a bean context, fields of the specified type will automatically be converted when the
 * 	{@link BeanMap#get(Object)} or {@link BeanMap#put(String, Object)} methods are called.<br>
 * <p>
 * 	<code>PojoSwaps</code> have two parameters:
 * 	<ol>
 * 		<li>{@code <T>} - The normal representation of an object.
 * 		<li>{@code <S>} - The swapped representation of an object.
 * 	</ol>
 * 	<br>
 * 	{@link Serializer Serializers} use swaps to convert objects of type T into objects of type S, and on calls to {@link BeanMap#get(Object)}.<br>
 * 	{@link Parser Parsers} use swaps to convert objects of type S into objects of type T, and on calls to {@link BeanMap#put(String,Object)}.
 *
 *
 * <h6 class='topic'>Swap Class Type {@code <S>}</h6>
 * <p>
 * 	The swapped object representation of an object must be an object type that the serializers can
 * 	natively convert to JSON (or language-specific equivalent).  The list of valid transformed types are as follows...
 * 	<ul class='spaced-list'>
 * 		<li>{@link String}
 * 		<li>{@link Number}
 * 		<li>{@link Boolean}
 * 		<li>{@link Collection} containing anything on this list.
 * 		<li>{@link Map} containing anything on this list.
 * 		<li>A java bean with properties of anything on this list.
 * 		<li>An array of anything on this list.
 * 	</ul>
 *
 *
 * <h6 class='topic'>Normal Class Type {@code <T>}</h6>
 * <p>
 * 	The normal object representation of an object.<br>
 *
 *
 * <h6 class='topic'>One-way vs. Two-way Serialization</h6>
 * <p>
 * 	Note that while there is a unified interface for handling swaps during both serialization and parsing,
 * 	in many cases only one of the {@link #swap(Object)} or {@link #unswap(Object)} methods will be defined
 * 	because the swap is one-way.  For example, a swap may be defined to convert an {@code Iterator} to a {@code ObjectList}, but
 * 	it's not possible to unswap an {@code Iterator}.  In that case, the {@code swap(Object}} method would
 * 	be implemented, but the {@code unswap(ObjectMap)} object would not, and the swap would be associated on
 * 	the serializer, but not the parser.  Also, you may choose to serialize objects like {@code Dates} to readable {@code Strings},
 * 	in which case it's not possible to reparse it back into a {@code Date}, since there is no way for the {@code Parser} to
 * 	know it's a {@code Date} from just the JSON or XML text.
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The normal form of the class.
 * @param <S> The swapped form of the class.
 */
public abstract class PojoSwap<T,S> {

	private final Class<T> normalClass;
	private final Class<S> swapClass;
	private ClassMeta<S> swapClassMeta;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	protected PojoSwap() {

		Class<?> t_normalClass = null, t_swapClass = null;

		Class<?> c = this.getClass().getSuperclass();
		Type t = this.getClass().getGenericSuperclass();
		while (c != PojoSwap.class) {
			t = c.getGenericSuperclass();
			c = c.getSuperclass();
		}

		// Attempt to determine the T and G classes using reflection.
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			Type[] pta = pt.getActualTypeArguments();
			if (pta.length == 2) {
				Type nType = pta[0];
				if (nType instanceof Class) {
					t_normalClass = (Class<T>)nType;

				// <byte[],x> ends up containing a GenericArrayType, so it has to
				// be handled as a special case.
				} else if (nType instanceof GenericArrayType) {
					Class<?> cmpntType = (Class<?>)((GenericArrayType)nType).getGenericComponentType();
					t_normalClass = Array.newInstance(cmpntType, 0).getClass();

				// <Class<?>,x> ends up containing a ParameterizedType, so just use the raw type.
				} else if (nType instanceof ParameterizedType) {
					t_normalClass = (Class<T>)((ParameterizedType)nType).getRawType();

				} else
					throw new RuntimeException("Unsupported parameter type: " + nType);
				if (pta[1] instanceof Class)
					t_swapClass = (Class<S>)pta[1];
				else if (pta[1] instanceof ParameterizedType)
					t_swapClass = (Class<S>)((ParameterizedType)pta[1]).getRawType();
				else
					throw new RuntimeException("Unexpected transformed class type: " + pta[1].getClass().getName());
			}
		}

		this.normalClass = (Class<T>)t_normalClass;
		this.swapClass = (Class<S>)t_swapClass;
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param swapClass The transformed class (serializable).
	 */
	protected PojoSwap(Class<T> normalClass, Class<S> swapClass) {
		this.normalClass = normalClass;
		this.swapClass = swapClass;
	}

	/**
	 * If this transform is to be used to serialize non-serializable POJOs, it must implement this method.
	 * <p>
	 * 	The object must be converted into one of the following serializable types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link String}
	 * 		<li>{@link Number}
	 * 		<li>{@link Boolean}
	 * 		<li>{@link Collection} containing anything on this list.
	 * 		<li>{@link Map} containing anything on this list.
	 * 		<li>A java bean with properties of anything on this list.
	 * 		<li>An array of anything on this list.
	 * 	</ul>
	 *
	 * @param o The object to be transformed.
	 * @return The transformed object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public S swap(T o) throws SerializeException {
		throw new SerializeException("Swap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
	}

	/**
	 *	Same as {@link #swap(Object)}, but override this method instead if you want access to the bean context that created this swap.
	 *
	 * @param o The object to be transformed.
	 * @param beanContext The bean context to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @return The transformed object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public S swap(T o, BeanContext beanContext) throws SerializeException {
		return swap(o);
	}

	/**
	 * If this transform is to be used to reconstitute POJOs that aren't true Java beans, it must implement this method.
	 *
	 * @param f The transformed object.
	 * 	This may be <jk>null</jk> if the parser cannot make this determination.
	 * @return The narrowed object.
	 * @throws ParseException If this method is not implemented.
	 */
	public T unswap(S f) throws ParseException {
		throw new ParseException("Unswap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
	}

	/**
	 *	Same as {@link #unswap(Object)}, but override this method if you need access to the real class type or the bean context that created this swap.
	 *
	 * @param f The transformed object.
	 * @param hint If possible, the parser will try to tell you the object type being created.  For example,
	 * 	on a serialized date, this may tell you that the object being created must be of type {@code GregorianCalendar}.<br>
	 * 	This may be <jk>null</jk> if the parser cannot make this determination.
	 * @param beanContext The bean context to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @return The narrowed object.
	 * @throws ParseException If this method is not implemented.
	 */
	public T unswap(S f, ClassMeta<?> hint, BeanContext beanContext) throws ParseException {
		return unswap(f);
	}

	/**
	 * Returns the T class, the normalized form of the class.
	 *
	 * @return The normal form of this class.
	 */
	public Class<T> getNormalClass() {
		return normalClass;
	}

	/**
	 * Returns the G class, the generialized form of the class.
	 * <p>
	 * 	Subclasses must override this method if the generialized class is {@code Object},
	 * 	meaning it can produce multiple generialized forms.
	 *
	 * @return The transformed form of this class.
	 */
	public Class<S> getSwapClass() {
		return swapClass;
	}

	/**
	 * Returns the {@link ClassMeta} of the transformed class type.
	 * This value is cached for quick lookup.
	 *
	 * @param beanContext The bean context to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @return The {@link ClassMeta} of the transformed class type.
	 */
	public ClassMeta<S> getSwapClassMeta(BeanContext beanContext) {
		if (swapClassMeta == null)
			swapClassMeta = beanContext.getClassMeta(swapClass);
		return swapClassMeta;
	}

	/**
	 * Checks if the specified object is an instance of the normal class defined on this swap.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the normal class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isNormalObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(normalClass, o.getClass());
	}

	/**
	 * Checks if the specified object is an instance of the swap class defined on this swap.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the transformed class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isSwappedObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(swapClass, o.getClass());
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return getClass().getSimpleName() + '<' + getNormalClass().getSimpleName() + "," + getSwapClass().getSimpleName() + '>';
	}
}
