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
package org.apache.juneau.examples.core.xml;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.SerializeException;
import org.apache.juneau.xml.XmlParser;
import org.apache.juneau.xml.XmlSerializer;



/**
 * TODO
 */
public class XmlSimpleExample {
    /**
     * TODO
     *
     * @param args
     * @throws SerializeException
     * @throws ParseException
     */
    public static void main(String[] args) throws SerializeException, ParseException {

        // Fill some data to a Pojo bean
        Pojo pojo = new Pojo("id","name");

        // Serialize to human readable XML and print
        String serial = XmlSerializer.DEFAULT_SQ_READABLE.serialize(pojo);
        System.out.println(serial);

        // Deserialize back to PojoComplex instance
        Pojo obj = XmlParser.DEFAULT.parse(serial, Pojo.class);

        assert obj.getId().equals(pojo.getId());
        assert obj.getName().equals(pojo.getName());

        // The object above can be parsed thanks to the @BeanConstructor annotation on PojoComplex
        // Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.

    }
}
