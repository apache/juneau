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
import useBrokenLinks from '@docusaurus/useBrokenLinks';
import {GROUPS, PROJECTS} from '@site/src/data/projects';
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

function ProjectCards() {
  // Registers the #projects anchor so the footer's /#projects link passes Docusaurus' broken-anchor check.
  useBrokenLinks().collectAnchor('projects');
  return (
    <section id="projects" className={`juneau-project-cards ${styles.projects}`}>
      <div className="container">
        <h2>The Juneau projects</h2>
        <div className={styles.cardColumns}>
          {GROUPS.map((g) => (
            <div key={g.id}>
              <h3 className={styles.groupLabel}>{g.label}</h3>
              <ul className={styles.cardList}>
                {PROJECTS.filter((p) => p.group === g.id).map((p) => (
                  <li key={p.slug}>
                    <Link className={styles.card} to={`/about/${p.slug}/`}>
                      <strong>{p.name}</strong>
                      <span>{p.tagline}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function About() {
  return (
    <section className={styles.about}>
      <div className="container">
        <h2>See it running</h2>
        <p>
          The <Link to="/docs/topics/JuneauPetstore">juneau-petstore</Link> sample runs on Jetty and Spring Boot with an
          admin console that uses the projects above, and <Link to="/docs/topics/JuneauTerminal">juneau-terminal</Link>{' '}
          turns Maven, npm and other build output into the Terminal views the console shows.
        </p>
        <Link to="/docs/topics/JuneauPetstore">
          <img src="/topics/JuneauPetstore.Tour.1.webp" alt="The Juneau Petstore admin console" width="2560"
            height="1800" loading="lazy" style={{height: 'auto', maxWidth: '100%'}} />
        </Link>
        <h2>When to Choose Juneau</h2>
        <p>
          Not sure if Juneau is right for your project? Check out our detailed{' '}
          <Link to="/docs/topics/WhyJuneau">Why Choose Juneau?</Link> page for a comprehensive comparison with
          alternatives, or see our <Link to="/docs/topics/FrameworkComparisons">Framework Comparisons</Link> page for
          detailed technical comparisons.
        </p>
        <p>
          <strong>Quick summary:</strong> Choose Juneau when you need multi-format serialization, REST APIs with
          automatic content negotiation, or want an integrated solution with no required dependencies for marshalling (
          <Link to="/docs/topics/FrameworkComparisons">Framework Comparisons</Link>). Also consider it when the same
          Java service needs a server-rendered admin console (
          <Link to="/docs/topics/AdminConsoleChrome">console chrome</Link> and{' '}
          <Link to="/docs/topics/RichTableViews">rich table views</Link>) or an{' '}
          <Link to="/docs/topics/JuneauRestServerMcp">MCP</Link> endpoint, without adding a separate front-end stack.
          Upgrading from 9.x? See the <Link to="/docs/topics/V10MigrationGuide">V10.0 Migration Guide</Link>.
        </p>
        <p>
          For every module, including the bean DTOs, shaded jars and bundles, see the{' '}
          <Link to="/docs/topics/JuneauEcosystemOverview">Juneau Ecosystem</Link> overview.
        </p>
        <p>
          Questions via email to{' '}
          <a href="mailto:dev@juneau.apache.org?Subject=Apache%20Juneau%20question">dev@juneau.apache.org</a> are
          always welcome.
        </p>
        <p>
          Juneau is packed with features that may not be obvious at first. Users are encouraged to ask for code reviews
          by providing links to specific source files such as through GitHub. Not only can we help you with feedback,
          but it helps us understand usage patterns to further improve the product.
        </p>
      </div>
    </section>
  );
}

function QuickLinks() {
  return (
    <section className={styles.quickLinks}>
      <div className="container">
        <h2>Documentation &amp; reports</h2>
        <ul className={styles.links}>
          <li><a href="/site/apidocs/" target="_blank" rel="noopener noreferrer">Javadocs</a></li>
          <li><a href="/site/" target="_blank" rel="noopener noreferrer">Maven Site</a></li>
          <li><a href="/site/xref/" target="_blank" rel="noopener noreferrer">Source Cross-Reference</a></li>
          <li><a href="/site/surefire.html" target="_blank" rel="noopener noreferrer">Test Reports</a></li>
        </ul>
      </div>
    </section>
  );
}

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
                    Documentation
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
        <ProjectCards />
        <About />
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
        <QuickLinks />
      </main>
    </Layout>
  );
}
