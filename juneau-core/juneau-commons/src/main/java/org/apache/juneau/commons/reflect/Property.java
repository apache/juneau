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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import org.apache.juneau.commons.function.*;

/**
 * A typed property descriptor that allows you to specify arbitrary consumers and producers
 * corresponding to setters and getters.
 *
 * <p>
 * This class provides a flexible, builder-based approach to defining properties on objects,
 * supporting both method-based and field-based access patterns. It's an improvement over
 * {@link Setter} by providing bidirectional access (getters and setters) and type safety.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Type-safe - generic types for object and value types
 * 	<li>Builder-based - fluent API for construction
 * 	<li>Flexible - supports arbitrary consumers and producers that can throw exceptions
 * 	<li>Convenience methods - easy integration with {@link FieldInfo} and {@link MethodInfo}
 * 	<li>Exception handling - uses {@link ThrowingFunction} and {@link ThrowingConsumer2} for exception support
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create property with method getter and setter</jc>
 * 	Property&lt;MyClass, String&gt; <jv>nameProperty</jv> = Property
 * 		.&lt;MyClass, String&gt;<jsm>create</jsm>()
 * 		.getter(<jv>obj</jv> -&gt; <jv>obj</jv>.getName())
 * 		.setter((<jv>obj</jv>, <jv>val</jv>) -&gt; <jv>obj</jv>.setName(<jv>val</jv>))
 * 		.build();
 *
 * 	<jc>// Create property from FieldInfo</jc>
 * 	FieldInfo <jv>field</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getField(<js>"name"</js>);
 * 	Property&lt;MyClass, String&gt; <jv>fieldProperty</jv> = Property
 * 		.&lt;MyClass, String&gt;<jsm>create</jsm>()
 * 		.field(<jv>field</jv>)
 * 		.build();
 *
 * 	<jc>// Create property from MethodInfo getter and setter</jc>
 * 	MethodInfo <jv>getter</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"getName"</js>);
 * 	MethodInfo <jv>setter</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"setName"</js>, String.<jk>class</jk>);
 * 	Property&lt;MyClass, String&gt; <jv>methodProperty</jv> = Property
 * 		.&lt;MyClass, String&gt;<jsm>create</jsm>()
 * 		.getter(<jv>getter</jv>)
 * 		.setter(<jv>setter</jv>)
 * 		.build();
 *
 * 	<jc>// Use property</jc>
 * 	MyClass <jv>obj</jv> = <jk>new</jk> MyClass();
 * 	<jv>nameProperty</jv>.set(<jv>obj</jv>, <js>"John"</js>);
 * 	String <jv>value</jv> = <jv>nameProperty</jv>.get(<jv>obj</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Setter} - Legacy setter interface
 * 	<li class='jc'>{@link FieldInfo} - Field introspection
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflect">juneau-commons-reflect</a>
 * </ul>
 *
 * @param <T> The object type.
 * @param <V> The value type.
 */
public class Property<T, V> {

	/**
	 * Creates a new builder for constructing a property.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Property&lt;MyClass, String&gt; <jv>prop</jv> = Property
	 * 		.&lt;MyClass, String&gt;<jsm>create</jsm>()
	 * 		.getter(<jv>obj</jv> -&gt; <jv>obj</jv>.getName())
	 * 		.setter((<jv>obj</jv>, <jv>val</jv>) -&gt; <jv>obj</jv>.setName(<jv>val</jv>))
	 * 		.build();
	 * </p>
	 *
	 * @param <T> The object type.
	 * @param <V> The value type.
	 * @return A new builder instance.
	 */
	public static <T, V> Builder<T, V> create() {
		return new Builder<>();
	}

	private final ThrowingFunction<T, V> producer;
	private final ThrowingConsumer2<T, V> consumer;

	/**
	 * Constructor.
	 *
	 * @param producer The producer function (getter). Can be <jk>null</jk>.
	 * @param consumer The consumer function (setter). Can be <jk>null</jk>.
	 */
	public Property(ThrowingFunction<T,V> producer, ThrowingConsumer2<T, V> consumer) {
		this.producer = producer;
		this.consumer = consumer;
	}

	/**
	 * Gets the value from the specified object using the producer (getter).
	 *
	 * @param object The object from which to get the value. Must not be <jk>null</jk>.
	 * @return The value.
	 * @throws ExecutableException
	 */
	public V get(T object) throws ExecutableException {
		assertArgNotNull("object", object);
		if (producer == null)
			throw exex("No getter defined for this property");
		return safe(() -> producer.applyThrows(object));
	}

	/**
	 * Sets the value on the specified object using the consumer (setter).
	 *
	 * @param object The object on which to set the value. Must not be <jk>null</jk>.
	 * @param value The value to set. Can be <jk>null</jk>.
	 * @throws ExecutableException
	 */
	public void set(T object, V value) throws ExecutableException {
		assertArgNotNull("object", object);
		if (consumer == null)
			throw exex("No setter defined for this property");
		safe(() -> consumer.acceptThrows(object, value));
	}

	/**
	 * Returns <jk>true</jk> if this property can be read.
	 *
	 * @return <jk>true</jk> if this property can be read.
	 */
	public boolean canRead() {
		return producer != null;
	}

	/**
	 * Returns <jk>true</jk> if this property can be written.
	 *
	 * @return <jk>true</jk> if this property can be written.
	 */
	public boolean canWrite() {
		return consumer != null;
	}

	/**
	 * Builder for constructing {@link Property} instances.
	 *
	 * @param <T> The object type.
	 * @param <V> The value type.
	 */
	public static class Builder<T, V> {
		private ThrowingFunction<T, V> producer;
		private ThrowingConsumer2<T, V> consumer;

		/**
		 * Sets the producer (getter) using a function.
		 *
		 * @param producer The producer function. Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T, V> getter(ThrowingFunction<T, V> producer) {
			this.producer = producer;
			return this;
		}

		/**
		 * Sets the consumer (setter) using a throwing consumer.
		 *
		 * @param consumer The consumer function. Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T, V> setter(ThrowingConsumer2<T, V> consumer) {
			this.consumer = consumer;
			return this;
		}

		/**
		 * Sets both getter and setter from a {@link FieldInfo}.
		 *
		 * <p>
		 * This is a convenience method that creates both producer and consumer from a field.
		 *
		 * @param field The field info. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder<T, V> field(FieldInfo field) {
			assertArgNotNull("field", field);
			field.accessible();
			boolean isStatic = field.isStatic();
			this.producer = obj -> (V)field.get(isStatic ? null : obj);
			this.consumer = (obj, val) -> field.set(isStatic ? null : obj, val);
			return this;
		}

		/**
		 * Sets the producer (getter) from a {@link MethodInfo}.
		 *
		 * <p>
		 * The method should take no parameters and return the property value.
		 *
		 * @param method The method info. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder<T, V> getter(MethodInfo method) {
			assertArgNotNull("method", method);
			method.accessible();
			boolean isStatic = method.isStatic();
			this.producer = obj -> (V)method.invoke(isStatic ? null : obj);
			return this;
		}

		/**
		 * Sets the consumer (setter) from a {@link MethodInfo}.
		 *
		 * <p>
		 * The method should take one parameter (the value to set) and return void.
		 *
		 * @param method The method info. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T, V> setter(MethodInfo method) {
			assertArgNotNull("method", method);
			method.accessible();
			boolean isStatic = method.isStatic();
			this.consumer = (obj, val) -> method.invoke(isStatic ? null : obj, val);
			return this;
		}

		/**
		 * Builds the property instance.
		 *
		 * @return A new property instance.
		 */
		public Property<T, V> build() {
			return new Property<>(producer, consumer);
		}
	}
}

