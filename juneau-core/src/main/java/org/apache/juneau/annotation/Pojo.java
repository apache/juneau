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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;

/**
 * Used to tailor how POJOs get interpreted by the framework.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Pojo {

	/**
	 * Associate a {@link PojoSwap} or {@link SurrogateSwap} with this class type.
	 *
	 * <p>
	 * Supports the following class types:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Any other class.  Will get interpreted as a {@link SurrogateSwap}.
	 * </ul>
	 *
	 * <h6 class='topic'>Example</h6>
	 * <p>
	 * 	In this case, a swap is being applied to a bean that will force it to be serialized as a <code>String</code>
	 * <p class='bcode'>
	 * 	<jc>// Our bean class</jc>
	 * 	<ja>@Pojo</ja>(swap=BSwap.<jk>class</jk>)
	 * 	<jk>public class</jk> B {
	 * 		<jk>public</jk> String <jf>f1</jf>;
	 * 	}
	 *
	 * 	<jc>// Our POJO swap to force the bean to be serialized as a String</jc>
	 * 	<jk>public class</jk> BSwap <jk>extends</jk> PojoSwap&lt;B,String&gt; {
	 * 		<jk>public</jk> String swap(B o) <jk>throws</jk> SerializeException {
	 * 			<jk>return</jk> o.f1;
	 * 		}
	 * 		<jk>public</jk> B unswap(String f) <jk>throws</jk> ParseException {
	 * 			B b1 = <jk>new</jk> B();
	 * 			b1.<jf>f1</jf> = f;
	 * 			<jk>return</jk> b1;
	 * 		}
	 * 	}
	 *
	 * 	<jk>public void</jk> test() <jk>throws</jk> Exception {
	 * 		WriterSerializer s = JsonSerializer.<jsf>DEFAULT</jsf>;
	 * 		B b = <jk>new</jk> B();
	 * 		b.<jf>f1</jf> = <js>"bar"</js>;
	 * 		String json = s.serialize(b);
	 * 		<jsm>assertEquals</jsm>(<js>"'bar'"</js>, json);
	 *
	 * 		ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 		b = p.parse(json, B.<jk>class</jk>);
	 * 		<jsm>assertEquals</jsm>(<js>"bar"</js>, t.<jf>f1</jf>);
	 * 	}
	 * </p>
	 * <p>
	 * 	Note that using this annotation is functionally equivalent to adding swaps to the serializers and parsers:
	 * <p class='bcode'>
	 * 	WriterSerializer s = <jk>new</jk> JsonSerializer.addPojoSwaps(BSwap.<jk>class</jk>);
	 * 	ReaderParser p = <jk>new</jk> JsonParser.addPojoSwaps(BSwap.<jk>class</jk>);
	 * </p>
	 */
	Class<?> swap() default Null.class;
}