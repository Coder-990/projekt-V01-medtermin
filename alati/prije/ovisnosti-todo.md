# Ovisnosti — nalazi za provjeru

> Ovo su automatski nalazi skenera, a ne potvrda iskoristivosti u aplikaciji.
> Isti CVE moze se pojaviti u vise alata ili paketa. Brojevi se ne zbrajaju u broj jedinstvenih ranjivosti.

Izvori: `dependency-check-report.json`, `osv-java.txt`, `trivy-java.txt` iz ovog direktorija.
Parseri tekstualnih tablica podrzavaju trenutno spremljeni format; JSON je pouzdaniji za buduce skenove.
SpotBugs nalazi mogu ukljucivati kvalitetu koda, a ne samo sigurnost. Za konkretne OSV prioritete vidi `osv-detailed.md`.

| Skener | Broj nalaza u izvjestaju |
| --- | ---: |
| Semgrep | 6 |
| SpotBugs | 19 |
| Dependency-Check | 83 |
| OSV | 60 |
| Trivy | 60 |

## Ovisnosti

| Skener | Paket / JAR | ID nalaza | Ocjena / podaci |
| --- | --- | --- | --- |
| Dependency-Check | assertj-core-3.27.6.jar | CVE-2026-24400 | HIGH |
| Dependency-Check | jackson-databind-2.19.2.jar | CVE-2026-54512 | HIGH |
| Dependency-Check | jackson-databind-2.19.2.jar | CVE-2026-54513 | HIGH |
| Dependency-Check | jackson-databind-2.19.2.jar | CVE-2026-54514 | MEDIUM |
| Dependency-Check | jackson-databind-2.19.2.jar | CVE-2026-54515 | MEDIUM |
| Dependency-Check | log4j-api-2.24.3.jar | CVE-2026-34477 | MEDIUM |
| Dependency-Check | log4j-api-2.24.3.jar | CVE-2026-34479 | MEDIUM |
| Dependency-Check | log4j-api-2.24.3.jar | CVE-2026-49844 | MEDIUM |
| Dependency-Check | snakeyaml-1.33.jar | CVE-2022-1471 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-22735 | LOW |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-22737 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-22740 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-22741 | LOW |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-22745 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41838 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41839 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41840 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41841 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41842 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41843 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41844 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41845 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41846 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41848 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41850 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41851 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41852 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41853 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41854 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-41855 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47883 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47884 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47885 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47886 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47887 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47888 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47889 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47890 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47891 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47892 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-47893 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59280 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59281 | MEDIUM |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59282 | HIGH |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59283 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59313 | CRITICAL |
| Dependency-Check | spring-core-6.2.12.jar | CVE-2026-59314 | LOW |
| Dependency-Check | spring-security-crypto-6.5.6.jar | CVE-2018-1258 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2025-66614 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-24733 | LOW |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-24734 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-24880 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-25854 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-29145 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-29146 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-34483 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-34487 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-34500 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-41284 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-41293 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-42498 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-43512 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-43513 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-43514 | LOW |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-43515 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-50229 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-53404 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-53434 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-55276 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-55955 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-55956 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-59083 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-59084 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-65182 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-65183 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-65905 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-65927 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-66299 | MEDIUM |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-66422 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-68525 | CRITICAL |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-68569 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-68763 | HIGH |
| Dependency-Check | tomcat-embed-core-10.1.48.jar | CVE-2026-73180 | MEDIUM |
| OSV | ch.qos.logback:logback-core@1.5.20 | GHSA-jhq6-gfmj-v8fx | CVSS 2.9; fixed 1.5.34 |
| OSV | ch.qos.logback:logback-core@1.5.20 | GHSA-p47f-322f-whfh | CVSS 1.2; fixed 1.5.33 |
| OSV | ch.qos.logback:logback-core@1.5.20 | GHSA-qqpg-mvqg-649v | CVSS 1.8; fixed 1.5.25 |
| OSV | com.fasterxml.jackson.core:jackson-core@2.19.2 | GHSA-72hv-8253-57qq | CVSS 6.9; fixed 2.21.1 |
| OSV | com.fasterxml.jackson.core:jackson-core@2.19.2 | GHSA-7hhh-6rmp-j9qf | CVSS 7.5; fixed 2.21.7 |
| OSV | com.fasterxml.jackson.core:jackson-core@2.19.2 | GHSA-p6pp-m3f8-5c89 | CVSS 7.5; fixed 2.21.7 |
| OSV | com.fasterxml.jackson.core:jackson-core@2.19.2 | GHSA-r7wm-3cxj-wff9 | CVSS 8.7; fixed 2.21.4 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-3pjw-73gf-8qr5 | CVSS 6.5; fixed 2.21.4 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-5gvw-p9qm-jgwh | CVSS 6.5; fixed 2.21.5 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-5jmj-h7xm-6q6v | CVSS 5.3; fixed 2.21.5 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-cxp5-3px4-pw24 | CVSS 7.5; fixed 2.21.7 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-gx83-3vf8-gh7j | CVSS 5.6; fixed 2.21.6 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-hgj6-7826-r7m5 | CVSS 5.3; fixed 2.21.4 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-j3rv-43j4-c7qm | CVSS 8.1; fixed 2.21.4 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-q4xh-88c3-wmh7 | CVSS 7.5; fixed 2.21.6 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-rmj7-2vxq-3g9f | CVSS 8.1; fixed 2.21.4 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-vvgp-rfg2-7rr6 | CVSS 5.3; fixed 2.21.5 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-wjgm-6hv5-3cvf | CVSS 5.3; fixed 2.21.6 |
| OSV | com.fasterxml.jackson.core:jackson-databind@2.19.2 | GHSA-wv8q-qhhj-9h54 | CVSS 7.5; fixed 2.21.7 |
| OSV | org.apache.logging.log4j:log4j-api@2.24.3 | GHSA-qv9r-c865-cp47 | CVSS 6.3; fixed 2.25.5 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-563x-q5rq-57qp | CVSS 7.5; fixed 10.1.52 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-5m62-pw8w-7w9f | CVSS 9.1; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-5mp6-jrq3-r938 | CVSS 7.5; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-5v3h-fjv2-54fg | CVSS 6.5; fixed 10.1.56 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-9m3c-qcxr-9x87 | CVSS 6.9; fixed 10.1.53 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-9m89-8frq-c98c | CVSS 3.7; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-9xv2-5v5q-p794 | CVSS 9.8; fixed 10.1.58 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-fpj8-gq4v-p354 | CVSS 9.1; fixed 10.1.50 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-fv25-8xcx-gqjc | CVSS 7.3; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-gcx9-497g-6cp6 | CVSS 9.1; fixed 10.1.58 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-gx5v-xp9w-j4cg | CVSS 7.5; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-h3x4-894j-xpx5 | CVSS 9.1; fixed 10.1.58 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-h6fc-48rj-7qqh | CVSS 9.8; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-h792-v28v-ppgr | CVSS 7.3; fixed 10.1.56 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-mgp5-rv84-w37q | CVSS 7.5; fixed 10.1.52 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-mqg3-r7h5-24x4 | CVSS 9.1; fixed 10.1.56 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-r29c-68gh-xp6x | CVSS 9.8; fixed 10.1.55 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-rv64-5gf8-9qq8 | CVSS 7.5; fixed 10.1.54 |
| OSV | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | GHSA-x4m4-345f-5h5g | CVSS 7.5; fixed 10.1.54 |
| OSV | org.springframework.boot:spring-boot-autoconfigure@3.5.7 | GHSA-ggg2-9786-hwc8 | CVSS 5.3; fixed 3.5.15 |
| OSV | org.springframework.boot:spring-boot@3.5.7 | GHSA-wwpq-f5c3-7hvx | CVSS 7.0; fixed 3.5.14 |
| OSV | org.springframework:spring-core@6.2.12 | GHSA-659m-px2c-25wj | CVSS 3.7; fixed 6.2.19 |
| OSV | org.springframework:spring-expression@6.2.12 | GHSA-9f52-rjqv-25qv | CVSS 3.7; fixed 6.2.19 |
| OSV | org.springframework:spring-expression@6.2.12 | GHSA-r5w3-xv2f-j59q | CVSS 7.5; fixed 6.2.19 |
| OSV | org.springframework:spring-expression@6.2.12 | GHSA-wxpp-56q6-5pcg | CVSS 5.3; fixed 6.2.19 |
| OSV | org.springframework:spring-web@6.2.12 | GHSA-7m2p-62gw-p8qq | CVSS 4.2; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-3chg-m5w7-qfv5 | CVSS 7.1; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-4773-3jfm-qmx3 | CVSS 5.9; fixed 6.2.17 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-6hcq-hmm3-jj3c | CVSS 2.6; fixed 6.2.17 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-6p4f-wcwh-5vvm | CVSS 5.3; fixed 6.2.18 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-72pg-x5f8-j25j | CVSS 5.9; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-957g-f97v-vppc | CVSS 5.9; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-cjpg-rgq5-fr37 | CVSS 5.3; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-h3qp-gqrc-q736 | CVSS 4.2; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-j9f9-w8pj-32f8 | CVSS 9.8; fixed 7.0.9 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-mq64-j8f9-9gcj | CVSS 5.9; fixed 6.2.19 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-pc63-qcmh-9cmg | CVSS 9.8; fixed 7.0.9 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-wg35-8jpf-2xv3 | CVSS 3.1; fixed 6.2.18 |
| OSV | org.springframework:spring-webmvc@6.2.12 | GHSA-x23c-287f-qqv5 | CVSS 7.5; fixed 6.2.19 |
| OSV | org.yaml:snakeyaml@1.33 | GHSA-mjmj-j48q-9wg2 | CVSS 8.3; fixed 2.0 |
| Trivy | ch.qos.logback:logback-core@1.5.20 | CVE-2026-10532 | LOW |
| Trivy | ch.qos.logback:logback-core@1.5.20 | CVE-2026-1225 | LOW |
| Trivy | ch.qos.logback:logback-core@1.5.20 | CVE-2026-9828 | LOW |
| Trivy | com.fasterxml.jackson.core:jackson-core@2.19.2 | CVE-2026-18401 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-core@2.19.2 | CVE-2026-68494 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-core@2.19.2 | CVE-2026-89407 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-core@2.19.2 | CVE-2026-89425 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-19032 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-54512 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-54513 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-54514 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-54515 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-59888 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-59889 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-68497 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-77310 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-83557 | MEDIUM |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-91776 | HIGH |
| Trivy | com.fasterxml.jackson.core:jackson-databind@? | CVE-2026-91777 | HIGH |
| Trivy | org.apache.logging.log4j:log4j-api@2.24.3 | CVE-2026-49844 | ? |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2025-66614 | MEDIUM |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-24734 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-24880 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-25854 | MEDIUM |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-34483 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-34487 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-41284 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-41293 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-42498 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-43512 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-43513 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-43514 | LOW |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-43515 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-53404 | HIGH |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-55276 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-55956 | MEDIUM |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-65182 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-65905 | CRITICAL |
| Trivy | org.apache.tomcat.embed:tomcat-embed-core@10.1.48 | CVE-2026-68525 | CRITICAL |
| Trivy | org.springframework.boot:spring-boot-autoconfigure@? | CVE-2026-41001 | MEDIUM |
| Trivy | org.springframework.boot:spring-boot@3.5.7 | CVE-2026-40973 | HIGH |
| Trivy | org.springframework:spring-core@6.2.12 | CVE-2026-41848 | LOW |
| Trivy | org.springframework:spring-expression@? | CVE-2026-41850 | HIGH |
| Trivy | org.springframework:spring-expression@? | CVE-2026-41851 | MEDIUM |
| Trivy | org.springframework:spring-expression@? | CVE-2026-41852 | LOW |
| Trivy | org.springframework:spring-web@? | CVE-2026-41854 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-22735 | LOW |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-22737 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-22741 | LOW |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-22745 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41841 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41842 | HIGH |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41843 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41844 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41845 | HIGH |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41846 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-41853 | MEDIUM |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-47884 | CRITICAL |
| Trivy | org.springframework:spring-webmvc@? | CVE-2026-47890 | CRITICAL |
| Trivy | org.yaml:snakeyaml@1.33 | CVE-2022-1471 | HIGH |

## Ogranicenja lokacija

Semgrep lokacije provjeravaju se prema snippetu kada postoji. SpotBugs linije vrijede za verziju izvornog koda iz vremena skeniranja; nakon drugih izmjena treba ponoviti analizu.
Skripta normalizira samo vlastite generirane TODO komentare pri racunanju linija. Postojeci nalazi za samu ovu skriptu mogu zahtijevati novi Semgrep scan nakon njezine izmjene.

## Upozorenja

- Zastarjela ili nejednoznacna Semgrep lokacija: oznacavanje-ranjovisti.py:7; ponovi scan
- Zastarjela ili nejednoznacna Semgrep lokacija: oznacavanje-ranjovisti.py:36; ponovi scan
