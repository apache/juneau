/*
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
 */

module.exports = {
  docs: [
    {
      type: 'category',
      label: '1. Overview',
      collapsed: false,
      items: [
        '01-overview/index',
        '01-overview/01-marshalling',
        '01-overview/02-end-to-end-rest',
        '01-overview/03-rest-server',
        '01-overview/04-rest-client',
        '01-overview/05-dtos',
        '01-overview/06-config-files',
        '01-overview/07-fluent-assertions',
        '01-overview/08-general-design',
      ],
    },
    {
      type: 'category',
      label: '2. Juneau Marshall',
      collapsed: true,
      items: [
        '02-juneau-marshall/index',
        '02-juneau-marshall/01-marshallers',
        '02-juneau-marshall/02-serializers-and-parsers',
        '02-juneau-marshall/03-bean-contexts',
        {
          type: 'category',
          label: '4. Java Beans Support',
          items: [
            '02-juneau-marshall/04-java-beans-support/index',
            '02-juneau-marshall/04-java-beans-support/01-bean-annotations',
            '02-juneau-marshall/04-java-beans-support/02-bean-properties',
            '02-juneau-marshall/04-java-beans-support/03-bean-filters',
            // ... more subsections
          ],
        },
        '02-juneau-marshall/05-http-part-serializers-parsers',
        // ... continue with all sections
      ],
    },
    {
      type: 'category',
      label: '3. Juneau Marshall RDF',
      items: ['03-juneau-marshall-rdf/index'],
    },
    // ... continue with all major sections
    {
      type: 'category',
      label: '8. Juneau REST Server',
      collapsed: true,
      items: [
        '08-juneau-rest-server/index',
        '08-juneau-rest-server/01-getting-started',
        '08-juneau-rest-server/02-instantiation',
        // ... all REST server topics
      ],
    },
    // ... continue through all 18 major sections
  ],
};
