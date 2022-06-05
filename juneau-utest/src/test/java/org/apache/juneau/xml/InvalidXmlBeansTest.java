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
package org.apache.juneau.xml;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the correct error messages are displayed when you do something wrong with the @Xml annotation.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class InvalidXmlBeansTest {

	private static final XmlSerializer
		s1 = XmlSerializer.DEFAULT_SQ;

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			{
				"BeanWithAttrFormat",
				new BeanWithAttrFormat(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithAttrFormat: Invalid format specified in @Xml annotation on bean: ATTR.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS,VOID",
			},
			{
				"BeanWithElementFormat",
				new BeanWithElementFormat(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithElementFormat: Invalid format specified in @Xml annotation on bean: ELEMENT.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS,VOID",
			},
			{
				"BeanWithCollapsedFormat",
				new BeanWithCollapsedFormat(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithCollapsedFormat: Invalid format specified in @Xml annotation on bean: COLLAPSED.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS,VOID",
			},
			{
				"BeanWithMixedFormat",
				new BeanWithMixedFormat(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithMixedFormat: Invalid format specified in @Xml annotation on bean: MIXED.  Must be one of the following: DEFAULT,ATTRS,ELEMENTS,VOID",
			},
			{
				"BeanWithMultipleAttrs",
				new BeanWithMultipleAttrs(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithMultipleAttrs: Multiple instances of ATTRS properties defined on class.  Only one property can be designated as such.",
			},
			{
				"BeanWithWrongAttrsType",
				new BeanWithWrongAttrsType(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithWrongAttrsType: Invalid type for ATTRS property.  Only properties of type Map and bean can be used.",
			},
			{
				"BeanWithMulipleElements",
				new BeanWithMulipleElements(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithMulipleElements: Multiple instances of ELEMENTS properties defined on class.  Only one property can be designated as such.",
			},
			{
				"BeanWithWrongElementsType",
				new BeanWithWrongElementsType(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithWrongElementsType: Invalid type for ELEMENTS property.  Only properties of type Collection and array can be used.",
			},
			{
				"BeanWithMulipleMixed",
				new BeanWithMulipleMixed(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithMulipleMixed: Multiple instances of MIXED properties defined on class.  Only one property can be designated as such.",
			},
			{
				"BeanWithConflictingChildNames",
				new BeanWithConflictingChildNames(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithConflictingChildNames: Multiple properties found with the child name 'X'.",
			},
			{
				"BeanWithElementsAndMixed",
				new BeanWithElementsAndMixed(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithElementsAndMixed: ELEMENTS and MIXED properties found on the same bean.  Only one property can be designated as such.",
			},
			{
				"BeanWithElementsAndElement",
				new BeanWithElementsAndElement(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithElementsAndElement: ELEMENTS and ELEMENT properties found on the same bean.  These cannot be mixed.",
			},
			{
				"BeanWithElementsAndDefault",
				new BeanWithElementsAndDefault(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithElementsAndDefault: ELEMENTS and ELEMENT properties found on the same bean.  These cannot be mixed.",
			},
			{
				"BeanWithElementsAndCollapsed",
				new BeanWithElementsAndCollapsed(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithElementsAndCollapsed: ELEMENTS and COLLAPSED properties found on the same bean.  These cannot be mixed.",
			},
			{
				"BeanWithChildAndPropNameConflict",
				new BeanWithChildAndPropNameConflict(),
				"org.apache.juneau.xml.InvalidXmlBeansTest$BeanWithChildAndPropNameConflict: Child element name conflicts found with another property.",
			},
		});
	}

	private String expected;
	private Object in;

	public InvalidXmlBeansTest(String label, Object in, String expected) throws Exception {
		this.in = in;
		this.expected = expected;
	}

	@Test
	public void test() {
		assertThrown(()->s1.serialize(in)).asMessage().is(expected);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans
	//-----------------------------------------------------------------------------------------------------------------

	@Xml(format=XmlFormat.ATTR)
	public static class BeanWithAttrFormat {
		public int f1;
	}

	@Xml(format=XmlFormat.ELEMENT)
	public static class BeanWithElementFormat {
		public int f1;
	}

	@Xml(format=XmlFormat.COLLAPSED)
	public static class BeanWithCollapsedFormat {
		public int f1;
	}

	@Xml(format=XmlFormat.MIXED)
	public static class BeanWithMixedFormat {
		public int f1;
	}

	public static class BeanWithMultipleAttrs {
		@Xml(format=XmlFormat.ATTRS)
		public JsonMap f1;
		@Xml(format=XmlFormat.ATTRS)
		public JsonMap f2;
	}

	public static class BeanWithWrongAttrsType {
		@Xml(format=XmlFormat.ATTRS)
		public JsonList f1;
	}

	public static class BeanWithMulipleElements {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f1;
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f2;
	}

	public static class BeanWithWrongElementsType {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonMap f1;
	}

	public static class BeanWithMulipleMixed {
		@Xml(format=XmlFormat.MIXED)
		public JsonList f1;
		@Xml(format=XmlFormat.MIXED)
		public JsonList f2;
	}

	public static class BeanWithConflictingChildNames {
		@Xml(format=XmlFormat.COLLAPSED, childName="X")
		public JsonList f1;
		@Xml(format=XmlFormat.COLLAPSED, childName="X")
		public JsonList f2;
	}

	public static class BeanWithElementsAndMixed {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f1;
		@Xml(format=XmlFormat.MIXED)
		public JsonList f2;
	}

	public static class BeanWithElementsAndElement {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f1;
		@Xml(format=XmlFormat.ELEMENT)
		public JsonList f2;
	}

	public static class BeanWithElementsAndDefault {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f1;
		public JsonList f2;
	}

	public static class BeanWithElementsAndCollapsed {
		@Xml(format=XmlFormat.ELEMENTS)
		public JsonList f1;
		@Xml(format=XmlFormat.COLLAPSED)
		public JsonList f2;
	}

	public static class BeanWithChildAndPropNameConflict {
		@Xml(format=XmlFormat.COLLAPSED, childName="f2")
		public JsonList f1;
		public JsonList f2;
	}
}
