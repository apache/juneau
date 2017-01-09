/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.juneau.examples.core.json;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.json.JsonSerializerContext;

public class JsonConfigurationExample {
    public static void main(String[] args) throws Exception {
        Pojo aPojo = new Pojo("a","</pojo>");
        // Json Serializers can be configured using properties defined in JsonSerializerContext
        String withWhitespace = new JsonSerializer()
                .setProperty(JsonSerializerContext.JSON_useWhitespace, true)
                .serialize(aPojo);
        // the output will be padded with spaces after format characters
        System.out.println(withWhitespace);

        String escaped = new JsonSerializer()
                .setProperty(JsonSerializerContext.JSON_escapeSolidus, true)
                .serialize(aPojo);
        // the output will have escaped /
        System.out.println(escaped);


    }
}
