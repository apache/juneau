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
package org.apache.juneau.transform;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Used to swap out non-serializable objects with serializable replacements during serialization, and vis-versa during parsing.
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
 * <h6 class='topic'>Subtypes</h6>
 * <p>
 * 	The following abstract subclasses are provided for common swap types:
 * 	<ol>
 * 		<li>{@link StringSwap} - Objects swapped with strings.
 * 		<li>{@link MapSwap} - Objects swapped with {@link ObjectMap ObjectMaps}.
 * 	</ol>
 *
 * <h6 class='topic'>Localization</h6>
 * <p>
 * 	Swaps have access to the session locale and timezone through the {@link BeanSession#getLocale()} and {@link BeanSession#getTimeZone()}
 * 	methods.  This allows you to specify localized swap values when needed.
 * 	If using the REST server API, the locale and timezone are set based on the <code>Accept-Language</code> and <code>Time-Zone</code> headers
 * 	on the request.
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
 * <h6 class='topic'>Normal Class Type {@code <T>}</h6>
 * <p>
 * 	The normal object representation of an object.<br>
 *
 * <h6 class='topic'>One-way vs. Two-way Serialization</h6>
 * <p>
 * 	Note that while there is a unified interface for handling swaps during both serialization and parsing,
 * 	in many cases only one of the {@link #swap(BeanSession, Object)} or {@link #unswap(BeanSession, Object, ClassMeta)} methods will be defined
 * 	because the swap is one-way.  For example, a swap may be defined to convert an {@code Iterator} to a {@code ObjectList}, but
 * 	it's not possible to unswap an {@code Iterator}.  In that case, the {@code swap(Object}} method would
 * 	be implemented, but the {@code unswap(ObjectMap)} object would not, and the swap would be associated on
 * 	the serializer, but not the parser.  Also, you may choose to serialize objects like {@code Dates} to readable {@code Strings},
 * 	in which case it's not possible to reparse it back into a {@code Date}, since there is no way for the {@code Parser} to
 * 	know it's a {@code Date} from just the JSON or XML text.
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 * @param <T> The normal form of the class.
 * @param <S> The swapped form of the class.
 */
public abstract class PojoSwap<T,S> {

	private final Class<T> normalClass;
	private final Class<?> swapClass;
	private ClassMeta<?> swapClassMeta;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	protected PojoSwap() {
		normalClass = (Class<T>)ClassUtils.resolveParameterType(PojoSwap.class, 0, this.getClass());
		swapClass = ClassUtils.resolveParameterType(PojoSwap.class, 1, this.getClass());
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param swapClass The transformed class (serializable).
	 */
	protected PojoSwap(Class<T> normalClass, Class<?> swapClass) {
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
	 * @param session The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param o The object to be transformed.
	 *
	 * @return The transformed object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public S swap(BeanSession session, T o) throws SerializeException {
		throw new SerializeException("Swap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
	}

	/**
	 * If this transform is to be used to reconstitute POJOs that aren't true Java beans, it must implement this method.
	 * @param session The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param f The transformed object.
	 * @param hint If possible, the parser will try to tell you the object type being created.  For example,
	 * 	on a serialized date, this may tell you that the object being created must be of type {@code GregorianCalendar}.<br>
	 * 	This may be <jk>null</jk> if the parser cannot make this determination.
	 *
	 * @return The narrowed object.
	 * @throws ParseException If this method is not implemented.
	 */
	public T unswap(BeanSession session, S f, ClassMeta<?> hint) throws ParseException {
		throw new ParseException("Unswap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
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
	public Class<?> getSwapClass() {
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
	public ClassMeta<?> getSwapClassMeta(BeanContext beanContext) {
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
