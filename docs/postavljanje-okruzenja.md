# Postavljanje okruženja – projekt V01

Raspakiraj ZIP u mapu bez razmaka i dijakritika u putanji (npr. `C:\projekti\V01` ili `~/projekti/V01`).
Odaberi **jednu** tehnologiju: `java/`, `dotnet/` ili `python/`. Preporuka: odmah inicijaliziraj lokalni Git repozitorij
(`git init`, prvi commit = izvorno stanje) – tako lako uspoređuješ promjene i pokazuješ popravke.

## Java (mapa `java/`)

| Alat | Verzija | Provjera |
|---|---|---|
| JDK | 21 (npr. Eclipse Temurin 21) | `java -version` |
| Maven | nije potreban – projekt ima Maven Wrapper (`mvnw`, `mvnw.cmd`) | `./mvnw -v` |
| IDE | IntelliJ IDEA (Community ili Ultimate) ili VS Code + *Extension Pack for Java* + *Spring Boot Extension Pack* + *REST Client* | |

- **IntelliJ IDEA:** *File → Open…* → odaberi `java/pom.xml` → *Open as Project*. *Project Structure → SDK* = 21.
  Pokretanje: klasa `…Application` → ▶. Testovi: desni klik na `src/test/java` → *Run 'All Tests'*.
  Zahtjevi: otvori `http/medtermin.http` i klikni ▶ uz zahtjev.
- **VS Code:** *File → Open Folder…* → `java/`. Testovi u *Testing* panelu, zahtjevi iz `http/medtermin.http` (REST Client).
- **Naredbeni redak** (u mapi `java/`; na Windowsu `mvnw.cmd` umjesto `./mvnw`):
  ```bash
  ./mvnw spring-boot:run                 # aplikacija na http://localhost:8080
  ./mvnw test                            # svi testovi + JaCoCo izvještaj (target/site/jacoco/index.html)
  ./mvnw test -Dgroups=funkcionalno      # samo funkcionalni testovi
  ./mvnw test -Dtest=NazivMojegTesta     # jedan test
  ```
  Na macOS/Linuxu, ako `./mvnw` javi *Permission denied*: `chmod +x mvnw`.

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

## Provjera da sve radi

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
