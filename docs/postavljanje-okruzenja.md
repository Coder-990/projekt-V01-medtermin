# Postavljanje okruženja – MedTermin Java

## Trenutni rezultat testova

`./mvnw clean test` izvršava 157 testova: 156 prolazi, a neizmijenjeni izvorni test za tuđi nalaz pada jer očekuje ranjivi HTTP 200 umjesto 404. Nakon tog pada pokreni `./mvnw jacoco:report` za izvještaj. Izvorna verzija ima 117 prolaznih testova; obje verzije imaju 100 % pokrivenosti Java linija i grana.


Ovaj repozitorij ima Maven projekt u korijenskoj mapi. Potrebni su JDK 21 i mrežni pristup za prvo preuzimanje ovisnosti; Maven Wrapper je uključen.

```bash
./mvnw -v
./mvnw clean verify
APP_COOKIE_SECURE=false ./mvnw spring-boot:run
```

Aplikacija zadano sluša samo `127.0.0.1:8080`. Početni računi s poznatim lozinkama stvaraju se automatski. Gornja naredba isključuje Secure kolačić jer lokalno nema HTTPS-a. Nikada ne koristiti te postavke za stvarne podatke. Za provjeru: `GET http://localhost:8080/api/health` i prijava `ana` prema README-u.

Svi testovi izvode se s `./mvnw clean verify`, a JaCoCo HTML izvještaj nastaje u `target/site/jacoco/index.html`. Testovi zahtijevaju dopuštenje za lokalni socket i koriste privremene mape. Python alati za izvještaje trebaju `python3 -m pip install -r requirements-security-tools.txt`.

Lokalna pošta sprema reset tokene u privatnu POSIX mapu; javno dostupna ili simbolička mapa se odbija. Dohvat nalaza zahtijeva datotečni sustav koji podržava `SecureDirectoryStream`. Ako sustav to ne podržava, zahtjev se sigurno odbija. Aplikacija koristi H2 u memoriji i simuliranu poštu te **nije spremna za produkciju**. Detalji, CSRF, ograničenja po IP adresi, proxy i potrebna produkcijska infrastruktura nalaze se u [pregledu provjera](../alati/poslije/README.md).

Sljedeći odjeljci o .NET-u i Pythonu zadržani su kao povijesni predložak; ti projekti nisu dio ovog repozitorija.

## .NET (mapa `dotnet/`)

| Alat | Verzija | Provjera |
|---|---|---|
| .NET SDK | 10.0 | `dotnet --version` |
| IDE | Visual Studio 2026, VS Code + *C# Dev Kit* + *REST Client*, ili JetBrains Rider | |

- **Visual Studio:** otvori `dotnet/MedTermin.sln`. Pokretanje: projekt `MedTermin.Api`, profil `http`.
  Testovi: *Test → Test Explorer → Run All*. Zahtjevi: `http/medtermin.http` (ugrađeni HTTP klijent).
- **VS Code:** *Open Folder…* → `dotnet/`; C# Dev Kit prepoznaje solution; testovi u *Testing* panelu.
- **Rider:** *Open* → `MedTermin.sln`.
- **Naredbeni redak** (u mapi `dotnet/`):
  ```bash
  dotnet run --project src/MedTermin.Api            # aplikacija na http://localhost:5080
  dotnet test                                              # svi testovi
  dotnet test --filter "Category=Funkcionalno"             # samo funkcionalni testovi
  dotnet test --filter "FullyQualifiedName~MojTest"        # odabrani test
  dotnet test --collect:"XPlat Code Coverage"              # pokrivenost (coverage.cobertura.xml u TestResults/)
  ```

## Python (mapa `python/`)

| Alat | Verzija | Provjera |
|---|---|---|
| Python | 3.12 ili noviji (python.org; na Windowsu označi *Add python.exe to PATH*) | `python --version` |
| IDE | PyCharm ili VS Code + *Python* + *REST Client* | |

Ovisnosti se instaliraju u **virtualno okruženje** u mapi `python/`:

```bash
python -m venv .venv
# Windows (PowerShell):  .\.venv\Scripts\Activate.ps1      Linux/macOS:  source .venv/bin/activate
pip install -r requirements-dev.txt
```

- **PyCharm:** *File → Open…* → mapa `python/`; *Settings → Project → Python Interpreter → Add Interpreter →
  Add Local Interpreter → Virtualenv* (postojeći `.venv` ili novi). Pokretanje: desni klik na `run.py` → *Run*.
  Testovi: *Settings → Tools → Python Integrated Tools → Default test runner = pytest*, zatim desni klik na `tests` → *Run*.
- **VS Code:** *Open Folder…* → `python/`; *Python: Select Interpreter* → `.venv`; pokretanje iz *Run and Debug*
  (konfiguracija je u `.vscode/launch.json`), testovi u *Testing* panelu, zahtjevi iz `http/medtermin.http` (REST Client).
- **Naredbeni redak** (u mapi `python/`, s aktivnim `.venv`):
  ```bash
  python -m medtermin                     # aplikacija na http://localhost:8000 (Swagger UI: /docs)
  pytest                                   # svi testovi
  pytest -m funkcionalno                   # samo funkcionalni testovi
  pytest tests/sigurnost -v                # tvoji testovi
  pytest --cov --cov-report=html           # pokrivenost koda (izvještaj u htmlcov/index.html)
  ```
  Na Windowsu, ako PowerShell ne dopušta aktivaciju: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## Povijesne opće upute

1. `GET http://localhost:<port>/api/health` → `{"status":"UP"}`
2. Prijava kao `ana` (vidi README) → dobiješ token.
3. Svi postojeći testovi prolaze.

## Česti problemi

| Problem | Rješenje |
|---|---|
| Port 8080/5080/8000 je zauzet | Zatvori proces koji ga koristi ili promijeni port (`server.port` / `launchSettings.json` / varijabla okoline `PORT`) samo lokalno. |
| IntelliJ ne vidi JDK 21 | *Project Structure → SDK → Add SDK → Download JDK → 21*. |
| `dotnet` ne pronalazi SDK 10 | Instaliraj .NET 10 SDK; `global.json` traži 10.0.x. |
| Proxy/firewall blokira preuzimanje ovisnosti | Postavi proxy za Maven (`~/.m2/settings.xml`), NuGet (`nuget.config`) ili pip (`pip install --proxy …`). |
| `pip install` javlja grešku pri izgradnji paketa | Nadogradi pip (`python -m pip install --upgrade pip`) i provjeri da koristiš Python 3.12+ iz `.venv`. |
