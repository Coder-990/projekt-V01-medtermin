# Pokrivenost testovima

Mjerenje 15. 11. 2026., Java 21 i JaCoCo. Rezultati se odnose na Java produkcijski kod, ne na Python i shell skripte. 100 % znači da su dijelovi koda izvršeni, ne da su svi testovi prošli.

| Verzija i skup testova | Rezultat | Linije | Grane |
|---|---|---|---|
| Izvorni Git HEAD `e72c770106bed84a9f3b1ce599ec144cc4ea67c3`, izvornih 117 testova | 117 prolazi | 659/659 (100 %) | 225/225 (100 %) |
| Trenutni kod, sve testne klase | 156 prolazi, 1 pada | 806/806 (100 %) | 357/357 (100 %) |

Izvorni kod testiran je u čistoj privremenoj kopiji naredbom `./mvnw clean test`; zapis pokrivenosti je `../prije/jacoco-izvor.xml`. Nisu u njega uključeni naknadno dodani sigurnosni testovi: oni na izvornoj verziji namjerno padaju, što je dokaz crvenog stanja. Njihovi rezultati prije/poslije su u `crveno-zeleno.md`.

Popravljeni kod testiran je s `./mvnw clean test`; zbog pada izvornog funkcionalnog testa pokrenut je zatim `./mvnw jacoco:report`. Zapis pokrivenosti je `jacoco-popravljen.xml`; HTML je u `../../target/site/jacoco/index.html`. Padne `NalaziFunkcionalniTest.nevaljaniFormatVracaNalazUzNapomenu`: očekuje da korisnik Ana dobije tuđi nalaz HTTP 200, dok sigurnosni popravak vraća 404. Korisnik je izričito zatražio da se pravilo o neizmijenjenim funkcionalnim testovima zadrži. I taj test i sigurnosni test ne mogu istodobno proći za isti zahtjev; potrebno je razriješiti proturječje u izvornom testu sa školom. Test nije mijenjan niti je sigurnosni propust vraćen.
