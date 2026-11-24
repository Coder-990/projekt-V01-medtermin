# Sigurnost baza podataka – Analiza prijetnji (Z1) i odgovori na pitanja

**Student:** Domagoj Borovčak  
**Domena:** MedTermin  
**Akademija:** Span Cyber Security Center – Secure Coding akademija  

---

## 1. Analiza prijetnji zatečenog stanja (Z1)

### Prijetnja 1: Aplikacijska prijava je član serverske uloge `sysadmin`
* **Što je pronađeno (upit i rezultat):**
  ```sql
  SELECT p.name AS Login, r.name AS Role
  FROM sys.server_role_members rm
  JOIN sys.server_principals r ON r.principal_id = rm.role_principal_id
  JOIN sys.server_principals p ON p.principal_id = rm.member_principal_id
  WHERE r.name = 'sysadmin' AND p.name = 'medtermin_app';
  ```
  *Rezultat:* `medtermin_app` je član poslužiteljske uloge `sysadmin`.
* **Scenarij napada:** Ako napadač iskoristi bilo koji SQL injection u aplikaciji, preuzima potpunu kontrolu nad cijelim SQL Serverom (kompromitacija svih baza, gašenje revizije, krađa podataka).
* **Rješenje:** Korak K2 / Zadatak **Z2.2** (`ALTER SERVER ROLE sysadmin DROP MEMBER [medtermin_app];`).

---

### Prijetnja 2: Aplikacijski korisnik ima ulogu `db_owner`
* **Što je pronađeno:**
  ```sql
  USE MedTermin;
  SELECT p.name AS Korisnik, r.name AS Uloga
  FROM sys.database_role_members rm
  JOIN sys.database_principals r ON r.principal_id = rm.role_principal_id
  JOIN sys.database_principals p ON p.principal_id = rm.member_principal_id
  WHERE r.name = 'db_owner' AND p.name = 'medtermin_app';
  ```
  *Rezultat:* `medtermin_app` je vlasnik baze (`db_owner`).
* **Scenarij napada:** Aplikacija može proizvoljno mijenjati shemu (DDL), brisati tablice, gasiti okidače i brisati vlastite tragove iz `dbo.revizija`.
* **Rješenje:** Korak K2 i K3 / Zadaci **Z2.3, Z3.1, Z3.2** (uklanjanje iz `db_owner`, prelazak na `uloga_aplikacija`).

---

### Prijetnja 3: `TRUSTWORTHY` je `ON` uz vlasnika baze `sa`
* **Što je pronađeno:**
  ```sql
  SELECT name, is_trustworthy_on, SUSER_SNAME(owner_sid) AS Vlasnik
  FROM sys.databases WHERE name = 'MedTermin';
  ```
  *Rezultat:* `is_trustworthy_on = 1`, vlasnik baze je `sa`.
* **Scenarij napada:** Kombinacija `db_owner` i `TRUSTWORTHY ON` omogućuje eskalaciju prava do `sysadmina` preko modula s `EXECUTE AS OWNER`.
* **Rješenje:** Korak K4 / Zadatak **Z8.2** (`ALTER DATABASE [MedTermin] SET TRUSTWORTHY OFF;`).

---

### Prijetnja 4: Uloga `public` ima `SELECT` na cijelu shemu `dbo`
* **Što je pronađeno:**
  ```sql
  USE MedTermin;
  SELECT dp.permission_name, dp.state_desc, s.name AS SchemaName
  FROM sys.database_permissions dp
  JOIN sys.schemas s ON dp.major_id = s.schema_id
  WHERE dp.grantee_principal_id = DATABASE_PRINCIPAL_ID('public');
  ```
  *Rezultat:* `public` ima dodijeljeno pravo `SELECT` na shemi `dbo`.
* **Scenarij napada:** Svaki korisnik koji se spoji na poslužitelj (npr. izvještajna prijava `bi_izvjestaji` ili `guest`) može čitati sve podatke, uključujući hasheve lozinki, OIB-ove i nalaze.
* **Rješenje:** Korak K3 / Zadatak **Z3.4** (`REVOKE SELECT ON SCHEMA::dbo FROM public;`).

---

### Prijetnja 5: Korisnik `guest` ima aktivan `CONNECT` u bazi
* **Što je pronađeno:**
  ```sql
  USE MedTermin;
  SELECT permission_name, state_desc 
  FROM sys.database_permissions 
  WHERE grantee_principal_id = DATABASE_PRINCIPAL_ID('guest') AND permission_name = 'CONNECT';
  ```
  *Rezultat:* Korisnik `guest` ima dozvolu `CONNECT`.
* **Scenarij napada:** Prijave koje nemaju definiranog korisnika u bazi MedTermin svejedno mogu pristupiti bazi u kontekstu korisnika `guest`.
* **Rješenje:** Korak K4 / Zadatak **Z8.3** (`REVOKE CONNECT FROM guest;`).

---

### Prijetnja 6: Korisnička podrška (`podrska`) je u `db_datareader` i vidi lozinke i nezaštićene podatke
* **Što je pronađeno:**
  ```sql
  USE MedTermin;
  SELECT r.name FROM sys.database_role_members rm
  JOIN sys.database_principals r ON r.principal_id = rm.role_principal_id
  WHERE rm.member_principal_id = DATABASE_PRINCIPAL_ID('podrska');
  ```
  *Rezultat:* Korisnik `podrska` je član `db_datareader`.
* **Scenarij napada:** Djelatnik podrške može čitati `dbo.korisnici.password_hash`, kao i OIB i nalaze u čistom tekstu, što je kršenje GDPR-a i rizik kompromitacije.
* **Rješenje:** Korak K2 i K6 / Zadaci **Z2.5, Z4.3, Z4.4, Z4.5** (uklanjanje iz `db_datareader`, DDM maskiranje, zabrana pristupa stupcu `password_hash`).

---

### Prijetnja 7: Odsutnost Row-Level Securityja (RLS)
* **Što je pronađeno:**
  ```sql
  USE MedTermin;
  SELECT * FROM sys.security_policies;
  ```
  *Rezultat:* Nema definiranih sigurnosnih politika.
* **Scenarij napada:** Baza vraća sve retke tablice `dbo.rezervacije`. Ako u aplikaciji postoji SQL injection ili IDOR propust, napadač dohvaća podatke svih korisnika.
* **Rješenje:** Korak K6 / Zadaci **Z4.1, Z4.2** (sigurnosna politika s FILTER predikatom uz `SESSION_CONTEXT`).

---

### Prijetnja 8: OIB u čistom tekstu i nepostojanje TDE enkripcije
* **Što je pronađeno:**
  ```sql
  SELECT db_name(database_id), encryption_state FROM sys.dm_database_encryption_keys WHERE database_id = DB_ID('MedTermin');
  ```
  *Rezultat:* Nema TDE enkripcije (`encryption_state = 0`), stupac `oib` u `dbo.rezervacije` je u čistom tekstu.
* **Scenarij napada:** Krađom MDF datoteke ili diska napadač ima pristup svim medicinskim i osobnim podacima.
* **Rješenje:** Koraci K5 i K7 / Zadaci **Z5.1, Z5.2, Z5.3, Z5.4** (šifriranje stupca OIB i TDE enkripcija baze).

---

### Prijetnja 9: Uključene opasne poslužiteljske opcije
* **Što je pronađeno:**
  ```sql
  SELECT name, value_in_use FROM sys.configurations 
  WHERE name IN ('clr enabled', 'cross db ownership chaining', 'Ad Hoc Distributed Queries', 'xp_cmdshell');
  ```
  *Rezultat:* Opcije su uključene (`value_in_use = 1`).
* **Scenarij napada:** Izvođenje proizvoljnog .NET koda (`clr`), prelazak granica baza (`cross db ownership chaining`) i napadi na vanjske servise (`Ad Hoc Distributed Queries`).
* **Rješenje:** Korak K4 / Zadatak **Z8.1** (isključivanje opcija putem `sp_configure`).

---

### Prijetnja 10: Odsutnost revizije (auditinga)
* **Što je pronađeno:**
  ```sql
  SELECT * FROM sys.server_audits WHERE is_state_enabled = 1;
  ```
  *Rezultat:* Nema aktivnih revizijskih dnevnika.
* **Scenarij napada:** Neovlašteni pristup i čitanje medicinskih podataka prolaze bez ikakvog forenzičkog traga (nemogućnost prijave povrede prema GDPR čl. 33).
* **Rješenje:** Korak K1 / Zadaci **Z6.1 - Z6.5** (uključivanje poslužiteljske i bazične revizije).

---

## 2. Odgovori na pitanja iz zadataka

### Pitanje Z3: Zašto okidač i dalje može pisati u `dbo.revizija` iako aplikacija nema nikakvo pravo na tu tablicu?
* **Odgovor:** Zbog mehanizma **Ownership Chaining** (lanac vlasništva). Budući da su okidač na `dbo.rezervacije` i tablica `dbo.revizija` u istoj shemi i imaju istog vlasnika (`dbo`), SQL Server provjerava samo pravo korisnika na izvođenje početne naredbe (INSERT/UPDATE/DELETE na `dbo.rezervacije`). Kod izvođenja okidača lanac se ne prekida, pa korisnik ne mora imati izravne ovlasti nad `dbo.revizija`. Time se sprječava da aplikacija ili napadač ručno mijenjaju ili brišu dnevnik revizije.

### Pitanje Z4: Zašto Dynamic Data Masking (DDM) nije zamjena za šifriranje ni za kontrolu pristupa?
* **Odgovor:** DDM mijenja samo prikaz podataka u rezultatima upita na prezentacijskom sloju. Podaci na disku i u memoriji ostaju u potpunom čistom tekstu. Korisnik koji ima pravo samo na maskirane podatke može inferencijom (npr. filtrom `WHERE oib LIKE '48%'`) metodom pogađanja doznati pravu vrijednost. Također, DDM ne štiti od krađe datoteka baze.

### Pitanje Z5: Od čega TDE štiti, a od čega ne (npr. od SQL injectiona)? Što se događa s kopijom baze ako se izgubi certifikat? Gdje čuvati `.pvk` i lozinku?
* **Odgovor:**
  * **Od čega štiti:** TDE štiti podatke u mirovanju (*data at rest*) – fizički disk, MDF/LDF datoteke i sigurnosne kopije.
  * **Od čega NE štiti:** TDE ne štiti od aplikacijskih napada (poput SQL injectiona) jer autorizirana aplikacija automatski prima dešifrirane podatke.
  * **Gubitak certifikata:** Ako se izgubi certifikat i njegov privatni ključ, sigurnosna kopija i podaci baze postaju **nepovratno izgubljeni** i nemoguće ih je vratiti na drugom poslužitelju.
  * **Pohrana:** Privatni ključ (`.pvk`) i lozinka moraju se čuvati odvojeno od baze, u trezoru tajni (npr. Azure Key Vault) ili HSM uređaju.

### Pitanje Z7: Zašto je neisprobana kopija baze gotovo jednako loša kao nikakva? Što nedostaje za pravu strategiju?
* **Odgovor:** Neisprobana kopija pruža lažan osjećaj sigurnosti jer bez testa povrata (*restore verification*) ne znamo je li backup oštećen ili nedostaje li certifikat za dešifriranje.
* **Elementi cjelovite strategije:**
  1. Redoviti diferencijalni i log backupi prema definiranom RPO cilju.
  2. Pohrana kopija izvan lokacije poslužitelja (*off-site*, pravilo 3-2-1).
  3. Jasno definirani **RPO** (maksimalno prihvatljiv gubitak podataka) i **RTO** (maksimalno vrijeme oporavka).
  4. Redoviti automatizirani probni povrati.
