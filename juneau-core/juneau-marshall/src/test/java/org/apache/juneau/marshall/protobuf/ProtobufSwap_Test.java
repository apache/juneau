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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Object-swap dispatch regression tests for the protobuf binary codec.
 *
 * <p>
 * The protobuf field table ({@link ProtobufClassMeta}) is built once per bean from declared/raw property types and is
 * session-independent, so class/session-registered swaps (builder {@code swaps(...)} / {@code @Swap} on a type) are
 * resolved at serialize/parse time via {@code getSwap(session)}.  These tests pin that dispatch on the scalar,
 * surrogate-bean, list-element, and map-value paths.  Each swap is registered on <b>both</b> the serializer and the
 * parser and round-tripped through the binary codec.
 */
class ProtobufSwap_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// POJOs + swaps
	//-----------------------------------------------------------------------------------------------------------------

	public static class Color {
		public String name;
		public Color() {}
		public Color(String name) { this.name = name; }
	}

	/** POJO -> scalar (String) swap. */
	public static class ColorSwap extends ObjectSwap<Color,String> {
		@Override
		public String swap(MarshallingSession session, Color o) { return o == null ? null : o.name; }
		@Override
		public Color unswap(MarshallingSession session, String o, ClassMeta<?> hint, String attrName) { return o == null ? null : new Color(o); }
	}

	public static class Point {
		public int x;
		public int y;
		public Point() {}
		public Point(int x, int y) { this.x = x; this.y = y; }
	}

	public static class PointSurrogate {
		public int px;
		public int py;
		public PointSurrogate() {}
	}

	/** POJO -> surrogate-bean (message) swap. */
	public static class PointSwap extends ObjectSwap<Point,PointSurrogate> {
		@Override
		public PointSurrogate swap(MarshallingSession session, Point o) {
			if (o == null)
				return null;
			var p = new PointSurrogate();
			p.px = o.x;
			p.py = o.y;
			return p;
		}
		@Override
		public Point unswap(MarshallingSession session, PointSurrogate o, ClassMeta<?> hint, String attrName) {
			return o == null ? null : new Point(o.px, o.py);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean holders
	//-----------------------------------------------------------------------------------------------------------------

	public static class ScalarSwapBean {
		public Color color;
		public ScalarSwapBean() {}
		public ScalarSwapBean(Color color) { this.color = color; }
	}

	public static class SurrogateSwapBean {
		public Point point;
		public SurrogateSwapBean() {}
		public SurrogateSwapBean(Point point) { this.point = point; }
	}

	public static class ListSwapBean {
		public java.util.List<Color> colors;
		public ListSwapBean() {}
		public ListSwapBean(java.util.List<Color> colors) { this.colors = colors; }
	}

	public static class MapSwapBean {
		public java.util.Map<String,Color> colors;
		public MapSwapBean() {}
		public MapSwapBean(java.util.Map<String,Color> colors) { this.colors = colors; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_scalarSwapOnBeanProperty() throws Exception {
		var ser = ProtobufSerializer.create().swaps(ColorSwap.class).build();
		var par = ProtobufParser.create().swaps(ColorSwap.class).build();
		var b2 = par.parse(ser.serialize(new ScalarSwapBean(new Color("red"))), ScalarSwapBean.class);
		assertBean(b2, "color{name}", "{red}");
	}

	@Test
	void a02_surrogateBeanSwap() throws Exception {
		var ser = ProtobufSerializer.create().swaps(PointSwap.class).build();
		var par = ProtobufParser.create().swaps(PointSwap.class).build();
		var b2 = par.parse(ser.serialize(new SurrogateSwapBean(new Point(3, 4))), SurrogateSwapBean.class);
		assertBean(b2, "point{x,y}", "{3,4}");
	}

	@Test
	void a03_listElementSwap() throws Exception {
		var ser = ProtobufSerializer.create().swaps(ColorSwap.class).build();
		var par = ProtobufParser.create().swaps(ColorSwap.class).build();
		var b2 = par.parse(ser.serialize(new ListSwapBean(list(new Color("red"), new Color("blue")))), ListSwapBean.class);
		assertBeans(b2.colors, "name", "red", "blue");
	}

	@Test
	void a04_mapValueSwap() throws Exception {
		var ser = ProtobufSerializer.create().swaps(ColorSwap.class).build();
		var par = ProtobufParser.create().swaps(ColorSwap.class).build();
		var b2 = par.parse(ser.serialize(new MapSwapBean(map("primary", new Color("red"), "secondary", new Color("blue")))), MapSwapBean.class);
		assertBean(b2.colors.get("primary"), "name", "red");
		assertBean(b2.colors.get("secondary"), "name", "blue");
	}
}
