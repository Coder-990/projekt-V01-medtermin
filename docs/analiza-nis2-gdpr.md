# Analiza NIS2, ZKS i GDPR – MedTermin

## Opseg i status

MedTermin je obrazovni prototip za naručivanje i obradu zdravstvenih podataka. Pružatelji zdravstvene zaštite pripadaju sektoru zdravstva iz Priloga I NIS2 i ZKS-a. To samo po sebi ne dokazuje da je svaka poliklinika ključni ili važni subjekt: potrebno je utvrditi pravnu osobu, djelatnost, veličinu, moguće iznimke i odluku nadležnog tijela o kategorizaciji. Bez tih podataka status nije moguće konačno utvrditi.

NIS2 čl. 21 zahtijeva razmjerne tehničke, operativne i organizacijske mjere upravljanja rizikom, uključujući upravljanje incidentima, kontinuitet, sigurnost dobavnog lanca, provjeru učinkovitosti i kriptografiju. Hrvatski ZKS uređuje obveze kroz postupke kategorizacije i zahtjeve kibernetičke sigurnosti, osobito čl. 24–25 i provedbenu uredbu. Čl. 15 ZKS-a odnosi se na protokole suradnje, a ne opću obvezu mjera: uputa za predaju pogrešno ga navodi za tu svrhu.

Zdravstveni podaci posebna su kategorija osobnih podataka (GDPR čl. 9). Uz zakonitu osnovu obrade i odgovarajuću iznimku, treba ograničiti svrhu, količinu i rok pohrane, urediti ovlasti i obrade te procijeniti potrebu za procjenom učinka (DPIA). Čl. 32 zahtijeva mjere primjerene riziku, uključujući, prema potrebi, enkripciju, povjerljivost, cjelovitost, dostupnost, oporavak i redovitu provjeru.

## Mjere i dokazi

| Mjera | Dokaz / stvarno stanje |
|---|---|
| Autorizacija i RBAC | Provjere u kontrolerima i `AuthFilter`; testovi pristupa nalazima i rezervacijama. Provjera svih novih endpointa i centralna politika ostaju potrebni. |
| Lozinke | BCrypt za nove lozinke i migracija starih: `LozinkaHasher` i `AutentikacijaSigurnostTest`. |
| Revizija | Neuspjele prijave na WARN bez lozinke u `KorisnikService`; centralizirana revizija, zaštita i rokovi čuvanja zapisa nisu dokazani. |
| TLS | Lokalni razvoj koristi HTTP; TLS terminacija i HSTS produkcijski su preduvjeti, nisu dokazani implementacijom prototipa. |
| RLS i DDM | Moguće kontrole SQL Server laboratorija; aplikacijski H2 ih ne dokazuje. RLS ograničava retke, DDM maskira prikaz i sam nije zamjena za autorizaciju. Za produkciju provjeriti politike, identitete i ovlasti. |
| TDE | Moguća zaštita podataka SQL Servera u mirovanju; ne štiti podatke od ovlaštenog upita. Konfiguracija, ključevi i sigurnosne kopije zahtijevaju zaseban dokaz. |
| Kontinuirana provjera | `ci/` predviđa testove, SAST, SCA, tajne i DAST; stvarnu uspješnost treba potvrditi izvještajima izvođenja. |

## Incidenti

Za značajan incident NIS2 čl. 23 predviđa rano upozorenje u 24 sata od saznanja i prijavu u 72 sata, uz naknadno završno izvješće. Primjenjuju se i hrvatski propisi te postupak nadležnog tijela; nisu svi događaji automatski značajni incidenti. GDPR čl. 33 zasebno zahtijeva prijavu povrede osobnih podataka nadzornom tijelu bez nepotrebnog odgađanja, po mogućnosti unutar 72 sata, osim kad nije vjerojatan rizik za prava i slobode. Ako postoji visok rizik, čl. 34 propisuje i obavještavanje ispitanika uz zakonske iznimke. Rokovi i pragovi dvaju režima nisu zamjenjivi.

Izvori: [ZKS, NN 14/2024](https://narodne-novine.nn.hr/clanci/sluzbeni/full/2024_02_14_254.html), [Uredba o kibernetičkoj sigurnosti](https://narodne-novine.nn.hr/clanci/sluzbeni/2024_11_135_2217.html), [NIS2](https://eur-lex.europa.eu/eli/dir/2022/2555/oj), [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj).
