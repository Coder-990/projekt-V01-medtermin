# Pregled provjera nakon popravaka

Ovdje su spremljeni izvještaji za popravljenu verziju. Za redoslijed lokalne demonstracije otvori [vodič za obranu](../../obrana/obrana-projekta.md), a za poveznice na kod, testove i ograničenja mjerenja [testove i promjene](../../obrana/testovi-i-promjene.md).

## Testovi

Dokumentirano mjerenje svih Java testova: 157 testova, 156 prolazi i jedan neizmijenjeni izvorni test pada jer za tuđi nalaz očekuje ranjivi HTTP 200 umjesto 404. Dokumentirana pokrivenost svih testova je 100 % linija i grana; to ne znači da svi testovi prolaze ili da su svi napadi obuhvaćeni.

- [Crveno/zeleno: isti sigurnosni testovi prije i poslije](crveno-zeleno.md).
- [JaCoCo XML popravljene verzije](jacoco-popravljen.xml).
- [HTML pokrivenost samo postojećih funkcionalnih testova](../../obrana/pokrivenost-postojecih/index.html): zasebno mjerenje, 87 od 88 prolazi, 90,7 % linija i 69,5 % grana.

## Izvještaji alata

- [OSV](osv-java.txt), [Trivy](trivy-java.txt) i [SBOM](sbom-cyclonedx-java.json): provjere ovisnosti i njihov inventar.
- [Dependency-Check HTML](dependency-check-report.html), [JSON](dependency-check-report.json) i [log](dependency-check.log).
- [Semgrep SARIF](semgrep-java.sarif) i [SpotBugs / FindSecBugs](spotbugs-java.txt).
- [ZAP HTML izvještaj](zap-lokalno.html): za oblikovani prikaz otvori datoteku u pregledniku.

Nalaz alata treba provjeriti u kodu; odsutnost nalaza nije dokaz odsutnosti ranjivosti. Izvještaji predstavljaju spremljeno skeniranje, a ne novu provjeru pri otvaranju ovog dokumenta. Usporedivi početni izvještaji nalaze se u [mapi prije](../prije/).

Dependency-Check iznimka za CVE-2018-1258 ograničena je pravilima u [suppression konfiguraciji](../../scripts/dependency-check-suppressions.xml). Pri promjeni ovisnosti ili identifikacije komponente ponovno provjeri opravdanost iznimke.

## Ograničenja lokalne demonstracije

Aplikacija koristi H2 u memoriji, početne testne račune i lokalnu poštu s reset tokenima. Zaštita pošte zahtijeva POSIX dozvole, a sigurno otvaranje nalaza podršku za `SecureDirectoryStream`.

Zahtjevi s autentikacijskim kolačićem provjeravaju pouzdani origin; ograničenje pokušaja prijave radi po IP adresi. Za produkciju treba zasebno provjeriti HTTPS i Secure kolačiće, dopuštene origine, pouzdanost proxyja i izvorne IP adrese, trajnu pohranu, dostavu pošte i upravljanje tajnama. Lokalna konfiguracija i spremljeni izvještaji ne potvrđuju spremnost za produkciju.
