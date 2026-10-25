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

import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import Layout from '@theme/Layout';
import TabItem from '@theme/TabItem';
import Tabs from '@theme/Tabs';
import styles from './index.module.css';

const DESCRIPTION =
  'Apache Juneau: one bean, every format. Serialize Java objects to JSON, XML, HTML, YAML and more with one API, then serve them over REST.';

// Output captured once from the default marshallers (Json.of, Xml.of, Yaml.of, Html.of) on 10.0.0-SNAPSHOT.
// Re-capture rather than hand-edit if the serializers change.
const SAMPLES = [
  {value: 'json', label: 'JSON', call: 'Json.of(new Person("Ada", 36))', language: 'json',
    output: '{"age":36,"name":"Ada"}'},
  {value: 'xml', label: 'XML', call: 'Xml.of(new Person("Ada", 36))', language: 'xml',
    output: '<object><age>36</age><name>Ada</name></object>'},
  {value: 'yaml', label: 'YAML', call: 'Yaml.of(new Person("Ada", 36))', language: 'yaml',
    output: 'age: 36\nname: Ada'},
  {value: 'html', label: 'HTML', call: 'Html.of(new Person("Ada", 36))', language: 'html',
    output: '<table><tr><td>age</td><td>36</td></tr><tr><td>name</td><td>Ada</td></tr></table>'},
];

// The newest published release. Update it at release time together with the Downloads page and the announcement
// bar in docusaurus.config.ts (New Release Guidelines, step 6.4). static/javadocs/releases.json is not used here:
// it also lists unreleased SNAPSHOT javadoc builds and records build dates.
const LATEST_RELEASE = {version: '9.2.0', released: '2026-01-05'};

export default function Home() {
  return (
    <Layout description={DESCRIPTION}>
      <main>
        <section className={styles.hero}>
          <div className="container">
            <div className={styles.heroGrid}>
              <div>
                <h1 className={styles.tagline}>One bean. Every format.</h1>
                <p className={styles.subline}>
                  Serialize any Java object to JSON, XML, HTML, YAML and more with one API, then serve it over REST.
                </p>
                <div className={styles.buttons}>
                  <Link className="button button--primary button--lg" to="/docs/topics/GettingStarted">
                    Get started
                  </Link>
                  <Link className="button button--secondary button--lg" to="/docs/topics/JuneauEcosystemOverview">
                    Browse docs
                  </Link>
                </div>
              </div>
              <div className={styles.sample}>
                <CodeBlock language="java">{'record Person(String name, int age) {}'}</CodeBlock>
                <Tabs defaultValue="json">
                  {SAMPLES.map((s) => (
                    <TabItem key={s.value} value={s.value} label={s.label}>
                      <CodeBlock language="java">{s.call}</CodeBlock>
                      <CodeBlock language={s.language} title="Output">{s.output}</CodeBlock>
                    </TabItem>
                  ))}
                </Tabs>
              </div>
            </div>
          </div>
        </section>
        <section className={styles.strip}>
          <div className="container">
            <div className={styles.stripRow}>
              <p className={styles.release}>
                Latest release: <strong>{LATEST_RELEASE.version}</strong> (released {LATEST_RELEASE.released})
              </p>
              <ul className={styles.links}>
                <li><Link to={`/docs/release-notes/${LATEST_RELEASE.version}`}>Release notes</Link></li>
                <li><Link to="/downloads">Download</Link></li>
                <li><Link to="/docs/topics/V10MigrationGuide">v10 migration guide</Link></li>
              </ul>
            </div>
          </div>
        </section>
        <section className={styles.community}>
          <div className="container">
            <h2>Community</h2>
            <ul className={styles.links}>
              <li><Link href="https://github.com/apache/juneau">GitHub</Link></li>
              <li><Link href="https://lists.apache.org/list.html?dev@juneau.apache.org">Mailing lists</Link></li>
              <li><Link to="/docs/developer-info/02.01.BecomingAContributor">Contributing</Link></li>
            </ul>
          </div>
        </section>
      </main>
    </Layout>
  );
}
