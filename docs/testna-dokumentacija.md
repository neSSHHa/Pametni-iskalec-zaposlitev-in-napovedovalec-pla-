# Testna dokumentacija

## 1. Namen dokumenta

Dokument opisuje način testiranja sistema **Pametni iskalec zaposlitev in napovedovalec plač**. Testiranje je namenjeno preverjanju pravilnosti poslovne logike, delovanja API komunikacije, obdelave podatkov ter stabilnosti glavnih uporabniških tokov pred namestitvijo aplikacije v produkcijsko okolje.

Poudarek dokumenta je na konkretnih testih, uporabljenih orodjih, testnih scenarijih in pričakovanih rezultatih.

---

## 2. Vrste testiranja

| Vrsta testiranja    | Namen                                                                                   | Primer v projektu                                                                     |
| ------------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| Unit testi          | Preverjajo posamezne funkcije, razrede ali komponente neodvisno od preostalega sistema. | Backend servisi, React komponente, Salary Service logika.                             |
| Integracijski testi | Preverjajo pravilno sodelovanje več slojev sistema.                                     | Backend API, podatkovna baza, AI tokovi, Salary endpointi.                            |
| End-to-End testi    | Preverjajo glavne uporabniške tokove v brskalniku.                                      | Iskanje zaposlitev, nalaganje CV-ja, primerjava oglasov, statistika in napoved plače. |

---

## 3. Testirane komponente

| Komponenta      | Vloga v sistemu                                                             | Vrsta testiranja          | Orodja                                    |
| --------------- | --------------------------------------------------------------------------- | ------------------------- | ----------------------------------------- |
| Frontend        | Uporabniški vmesnik za iskanje, prikaz in primerjavo zaposlitev.            | Unit, E2E                 | Vitest, React Testing Library, Playwright |
| Backend         | API endpointi, filtriranje, priporočila, CV obdelava in povezava s podatki. | Unit, integracijski testi | JUnit, Spring Boot Test, Maven            |
| Salary Service  | Napoved plače in priprava modela za napovedovanje.                          | Unit, API testi           | pytest                                    |
| Podatkovna baza | Shranjevanje, branje in filtriranje oglasov.                                | Integracijski testi       | Spring Boot Test                          |

AI tokovi so trenutno preverjeni prek backend integracijskih in live AI testov. Ločena testna zbirka v modulu `ai-service` trenutno ni implementirana.

---

## 4. Unit testi

Unit testi preverjajo posamezne dele kode z uporabo mock objektov ali vnaprej pripravljenih testnih podatkov. Namenjeni so hitremu preverjanju logike brez zagona celotnega sistema.

### 4.1 Backend unit testi

| ID        | Orodje             | Testni scenarij                        | Kaj se preverja                                                 | Pričakovan rezultat                             |
| --------- | ------------------ | -------------------------------------- | --------------------------------------------------------------- | ----------------------------------------------- |
| BE-UT-001 | JUnit, Mockito     | Prazen filter pri iskanju oglasov.     | Obdelava praznega `JobFilterRequest`.                           | Sistem vrne razpoložljive oglase brez napake.   |
| BE-UT-002 | JUnit, Mockito     | Filtriranje po državi.                 | Ujemanje lokacije oglasa z izbrano državo.                      | Vrnejo se oglasi iz izbrane države.             |
| BE-UT-003 | JUnit, Mockito     | Filtriranje po mestu.                  | Ujemanje lokacije oglasa z izbranim mestom.                     | Vrnejo se oglasi iz izbranega mesta.            |
| BE-UT-004 | JUnit, Mockito     | Filtriranje po veščinah.               | Primerjava zahtevanih veščin z veščinami oglasa.                | Vrnejo se oglasi z ustreznimi veščinami.        |
| BE-UT-005 | JUnit, Mockito     | Filtriranje po več kriterijih.         | Kombinacija lokacije, veščin, tipa dela, izkušenj in izobrazbe. | Rezultati so pravilno filtrirani in rangirani.  |
| BE-UT-006 | JUnit, Mockito     | Obdelava CV podatkov.                  | Pretvorba CV vsebine v kriterije za iskanje.                    | Sistem vrne veljaven iskalni profil.            |
| BE-UT-007 | JUnit, Mockito     | Zahteva za napoved plače.              | Priprava podatkov za Salary Service in obdelava odgovora.       | Vrne se strukturiran odgovor z napovedjo plače. |
| BE-UT-008 | JUnit              | Validacija dovoljenih AI vrednosti.    | Usklajenost AI vrednosti s sistemskimi šifranti.                | Neveljavne vrednosti se ne uporabijo v filtru.  |

### 4.2 Frontend unit testi

| ID        | Orodje                                    | Testni scenarij                 | Kaj se preverja                                                                       | Pričakovan rezultat                                         |
| --------- | ----------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| FE-UT-001 | Vitest, React Testing Library             | Prikaz seznama rezultatov.      | Renderiranje naslova, podjetja, lokacije, veščin, odstotka ujemanja in datuma objave. | Komponenta prikaže ključne podatke o oglasih.               |
| FE-UT-002 | Vitest, React Testing Library             | Prikaz napovedi plače.          | Renderiranje kartice z napovedanim plačnim razponom.                                  | Kartica z napovedjo plače je vidna, ko so podatki na voljo. |
| FE-UT-003 | Vitest, React Testing Library, user-event | Sortiranje rezultatov.          | Privzeto sortiranje po ujemanju in preklop na sortiranje po datumu.                   | Vrstni red oglasov se spremeni glede na izbran kriterij.    |
| FE-UT-004 | Vitest, React Testing Library, user-event | Dodajanje oglasov v primerjavo. | Omejitev izbire oglasov za primerjavo.                                                | Sistem dovoli največ dva oglasa za primerjavo.              |
| FE-UT-005 | Vitest, React Testing Library, user-event | Prikaz podrobnosti oglasa.      | Odpiranje modala, prikaz vira, razširitev opisa in zapiranje.                         | Podrobnosti oglasa se pravilno prikažejo in zaprejo.        |

### 4.3 Salary Service unit testi

| ID         | Orodje | Testni scenarij                     | Kaj se preverja                                                                  | Pričakovan rezultat                                  |
| ---------- | ------ | ----------------------------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------- |
| SAL-UT-001 | pytest | API prejme veljavne vhodne podatke. | Obdelava zahteve za napoved plače.                                               | API vrne napoved plače v pravilni JSON strukturi.    |
| SAL-UT-002 | pytest | Zahteva za slovenski trg.           | Omejitev napovedi na avstrijski trg.                                             | Odgovor označi, da napoved ni na voljo.              |
| SAL-UT-003 | pytest | Manjkajoča država.                  | Privzeta obravnava trga.                                                         | Sistem predpostavi Avstrijo in to navede v odgovoru. |
| SAL-UT-004 | pytest | Manjkajoč model.                    | Obnašanje API-ja brez pripravljenega modela.                                     | Odgovor označi, da model še ni pripravljen.          |
| SAL-UT-005 | pytest | Priprava podatkov za model.         | Čiščenje podatkov, združevanje veščin in tipov dela, izločanje neveljavnih plač. | Podatki so pripravljeni za učenje modela.            |
| SAL-UT-006 | pytest | Učenje in napoved z modelom.        | Stabilnost model pipelinea in napovedovanje za nove kategorije.                  | Model se nauči in vrne veljavno pozitivno napoved.   |
| SAL-UT-007 | pytest | Izračun plačnega razpona.           | Razmerja po izkušnjah, fallback razmerje in zaokroževanje na 50 EUR.             | Minimalna in maksimalna plača sta pravilno izračunani. |
| SAL-UT-008 | pytest | Izpeljane značilke.                 | Senioriteta, domena, vloga, izkušnje in kategorije veščin.                       | Model prejme pravilno pripravljene značilke.         |
| SAL-UT-009 | pytest | Tržne salary baseline vrednosti.    | Median vrednosti po vlogi, domeni in vrsti veščin.                               | Napoved se smiselno prilagodi podatkom trga.         |
| SAL-UT-010 | pytest | Nezadostni podatki za trening.      | Zavrnitev treninga z manj kot 100 veljavnimi zapisi.                            | Trening se ustavi z nadzorovano napako.              |

### 4.4 Zagon unit testov

```bash
cd backend
mvn clean test
```

```bash
cd frontend
npm ci
npm test
```

```bash
cd salary-service
pytest
```

---

## 5. Integracijski testi

Integracijski testi preverjajo delovanje povezanih delov sistema. Pri teh testih se preverjajo REST API endpointi, povezava s podatkovno bazo ter tokovi med backendom, AI logiko in Salary Service logiko.

| ID      | Orodje                  | Testni scenarij                             | Kaj se preverja                                                           | Pričakovan rezultat                                 |
| ------- | ----------------------- | ------------------------------------------- | ------------------------------------------------------------------------- | --------------------------------------------------- |
| INT-001 | JUnit, Spring Boot Test | Pridobivanje oglasov prek backend API-ja.   | Delovanje endpointov za oglase in struktura odgovora.                     | API vrne veljaven seznam oglasov.                   |
| INT-002 | JUnit, Spring Boot Test | Filtriranje oglasov po kriterijih.          | Povezava med request DTO, servisom in testnimi podatki.                   | API vrne oglase, ki ustrezajo filtru.               |
| INT-003 | JUnit, Spring Boot Test | Preverjanje integritete uvoženih podatkov.  | Prisotnost in konsistentnost podatkov v bazi.                             | Podatki so pravilno zapisani in berljivi.           |
| INT-004 | JUnit, Spring Boot Test | Ujemanje CV profila z oglasi.               | Tok obdelave CV kriterijev in iskanja oglasov.                            | Sistem vrne ustrezno rangirane rezultate.           |
| INT-005 | JUnit, Spring Boot Test | Pridobivanje statistike.                    | Delovanje analitičnih endpointov in agregacij.                            | API vrne pravilne statistične podatke.              |
| INT-006 | JUnit, Spring Boot Test | Iskanje z naravnim jezikom.                 | Tok prompt, AI filter, backend filtriranje in rezultati.                  | Sistem vrne rezultate na podlagi prompta.           |
| INT-007 | JUnit, Spring Boot Test | Live AI integracijski testi.                | AI ekstrakcija promptov, CV ekstrakcija in iskanje z realističnimi vnosi. | Testi se izvedejo samo ob `RUN_LIVE_AI_TESTS=true`. |
| INT-008 | JUnit, Spring Boot Test | Napoved plače prek backend API-ja.          | Backend endpoint in obdelava odgovora Salary Service logike.              | API vrne strukturirano napoved plače.               |
| INT-009 | JUnit, Spring Boot Test | Zahteva z manjkajočimi ali delnimi podatki. | Validacija request/response toka.                                         | Sistem vrne nadzorovan odziv.                       |

### 5.1 Zagon integracijskih testov

Backend integracijski testi so poimenovani z vzorcem `*IntegrationTest.java`, zato jih Maven Surefire zajame pri ukazu `mvn clean test`.

```bash
cd backend
mvn clean test
```

Live AI testi so dodatno pogojeni z okoljsko spremenljivko:

```bash
RUN_LIVE_AI_TESTS=true mvn clean test
```

---

## 6. End-to-End testi

End-to-End testi preverjajo aplikacijo iz perspektive uporabnika. Izvajajo se v brskalniku z orodjem **Playwright** in preverjajo glavne funkcionalnosti aplikacije od uporabniške akcije do prikaza rezultata.

| ID      | Orodje               | Funkcionalnost               | Testni scenarij                                      | Kaj se preverja                                            | Pričakovan rezultat                                     |
| ------- | -------------------- | ---------------------------- | ---------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------------- |
| E2E-001 | Playwright, Chromium | Začetna stran                | Uporabnik odpre aplikacijo.                          | Nalaganje začetnega pogleda in osnovnih UI elementov.      | Začetna stran se pravilno prikaže.                      |
| E2E-002 | Playwright, Chromium | Iskanje s promptom           | Uporabnik vnese iskalni prompt.                      | Pošiljanje prompta in prikaz rezultatov.                   | Prikažejo se rezultati iskanja.                         |
| E2E-003 | Playwright, Chromium | Validacija prompta           | Uporabnik odda prazen prompt.                        | Obnašanje obrazca pri manjkajočem vnosu.                   | Sistem prikaže ustrezen odziv.                          |
| E2E-004 | Playwright, Chromium | Nalaganje CV-ja              | Uporabnik naloži testni CV dokument.                 | Upload datoteke, obdelava CV vsebine in prikaz rezultatov. | Sistem obdela CV in prikaže rezultate.                  |
| E2E-005 | Playwright, Chromium | Podrobnosti oglasa           | Uporabnik odpre podrobnosti oglasa.                  | Prikaz dodatnih podatkov, opisa in vira oglasa.            | Podrobnosti oglasa se pravilno prikažejo.               |
| E2E-006 | Playwright, Chromium | Primerjava oglasov           | Uporabnik doda oglase v primerjavo.                  | Dodajanje oglasov in prikaz primerjalnega pogleda.         | Primerjava izbranih oglasov se prikaže.                 |
| E2E-007 | Playwright, Chromium | Odstranjevanje iz primerjave | Uporabnik odstrani oglas iz primerjave.              | Posodobitev primerjalnega seznama.                         | Oglas se odstrani iz primerjave.                        |
| E2E-008 | Playwright, Chromium | Prazna primerjava            | Uporabnik odpre primerjavo brez oglasov.             | Prikaz praznega stanja.                                    | Sistem prikaže ustrezno prazno stanje.                  |
| E2E-009 | Playwright, Chromium | Sortiranje rezultatov        | Uporabnik spremeni kriterij sortiranja.              | Sprememba vrstnega reda rezultatov.                        | Rezultati se prikažejo v pravilnem vrstnem redu.        |
| E2E-010 | Playwright, Chromium | Statistika                   | Uporabnik odpre statistični pogled.                  | Prikaz agregiranih statističnih podatkov.                  | Statistika se pravilno prikaže.                         |
| E2E-011 | Playwright, Chromium | Statistika iz rezultatov     | Uporabnik pregleda statistiko prikazanih rezultatov. | Izračun statistike iz trenutnega nabora oglasov.           | Statistika ustreza prikazanim rezultatom.               |
| E2E-012 | Playwright, Chromium | Napoved plače                | Uporabnik zahteva napoved plače.                     | Prikaz napovedanega plačnega razpona.                      | Sistem prikaže napoved plače.                           |
| E2E-013 | Playwright, Chromium | Hitro iskanje s promptom     | Uporabnik uporabi hitri način iskanja.               | Hitri tok obdelave prompta in prikaz rezultatov.           | Rezultati se prikažejo v hitrem načinu.                 |
| E2E-014 | Playwright, Chromium | Hitro nalaganje CV-ja        | Uporabnik uporabi hitri način obdelave CV-ja.        | Upload CV-ja in hitra obdelava kriterijev.                 | CV se obdela in rezultati se prikažejo v hitrem načinu. |

### 6.1 Zagon E2E testov

```bash
cd e2e
npm ci
npx playwright test
```

Zaporedni zagon:

```powershell
cd e2e
./run-tests-sequential.ps1
```

---

## 7. Jenkins CI/CD izvajanje

Jenkins se uporablja za avtomatizirano izvajanje testov in za nadzor nad prehodom kode proti produkcijski različici aplikacije. Pipeline je razdeljen na osnovne CI korake in dodatne produkcijske korake za vejo `production`.

### 7.1 Osnovni CI koraki

Osnovni CI koraki se izvajajo na veji `main` in na drugih razvojnih vejah, kjer ni nastavljen poseben produkcijski pogoj.

| Faza                 | Ukaz                                                                                      | Namen                                                                                          | Pričakovan rezultat                                     |
| -------------------- | ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| Backend Tests        | `cd backend && mvn clean test`                                                            | Izvajanje backend unit in integracijskih testov, ki jih Maven zazna v testni fazi.             | Backend testna zbirka se zaključi brez napak.           |
| AI Service Tests     | `cd ai-service && mvn clean test`                                                         | Izvajanje Maven testne faze za AI Service; ločeni testni razredi trenutno niso implementirani. | Modul se uspešno prevede in testna faza se zaključi.    |
| Salary Service Tests | `cd salary-service`, priprava `.venv`, `pip install -r requirements.txt pytest`, `pytest` | Izvajanje pytest testov za napoved plače in pripravo modela.                                   | Salary Service testi se zaključijo brez napak.          |
| Frontend Tests       | `cd frontend && npm ci && npm test && npm run build`                                      | Namestitev odvisnosti, izvedba frontend unit testov in priprava produkcijske frontend verzije. | Frontend testi uspejo in aplikacija se uspešno sestavi. |

### 7.2 Production branch (`production`)

Na veji `production` Jenkins po osnovnih CI korakih izvede še produkcijske faze. Te faze preverijo, ali je aplikacija pripravljena za namestitev in ali glavne funkcionalnosti delujejo tudi v produkcijskem okolju.

| Faza                 | Ukaz oziroma mehanizem                                                                                                          | Namen                                                                         | Pričakovan rezultat                                         |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- | ----------------------------------------------------------- |
| Deploy Production    | `git fetch`, `git checkout production`, `git pull origin production`, zagon produkcijskih storitev                              | Pridobitev najnovejše produkcijske kode in namestitev aplikacije na strežnik. | Produkcijska verzija aplikacije je nameščena in zagnana.    |
| Live Smoke Tests     | `curl --fail --max-time 20 "$PRODUCTION_BASE_URL"` in `curl --fail --max-time 20 "$PRODUCTION_BASE_URL/api/jobs?page=0&size=1"` | Preverjanje dosegljivosti začetne strani in osnovnega backend endpointa.      | Produkcijska aplikacija in API sta dosegljiva.              |
| Production E2E Tests | `cd e2e && npm ci && npx playwright install --with-deps chromium && npx playwright test`                                        | Izvajanje Playwright testov proti produkcijskemu URL-ju.                      | Glavni uporabniški tokovi se uspešno izvedejo v brskalniku. |

Live AI testi so definirani v `AiLivePreDeploymentIntegrationTest`, vendar se izvedejo samo, če je nastavljena okoljska spremenljivka `RUN_LIVE_AI_TESTS=true`.

### 7.3 Deployment flow

1. Jenkins najprej izvede osnovne CI korake.
2. Na veji `production` pridobi najnovejšo produkcijsko verzijo kode.
3. Aplikacijo pripravi in zažene v produkcijskem okolju.
4. Izvede smoke preverjanje začetne strani in endpointa `/api/jobs`.
5. Izvede Playwright E2E teste proti produkcijskemu URL-ju.
6. Če so vse faze uspešne, se pipeline zaključi kot uspešen.

---

## 8. Kriteriji uspešnosti

Testiranje se šteje za uspešno, kadar se unit, integracijski in End-to-End testi zaključijo brez napak, Jenkins pipeline uspešno izvede zahtevane faze, produkcijska aplikacija pa uspešno prestane smoke preverjanje in glavne uporabniške tokove.

---

## 9. Zaključek

Projekt uporablja unit, integracijske in End-to-End teste za preverjanje posameznih komponent, povezav med moduli in glavnih uporabniških funkcionalnosti.

Jenkins avtomatizira osnovno preverjanje kode, na veji `production` pa izvede še namestitev aplikacije, smoke preverjanje in Playwright E2E teste. Takšen pristop omogoča bolj nadzorovan prehod sprememb v produkcijsko okolje in zmanjšuje možnost napak pri izdaji nove verzije aplikacije.
