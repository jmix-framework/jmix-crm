# ![CRM](../src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

🖥️ [Online Demo](https://demo.jmix.io/b2b-crm/login)

🌐 Jezici: [English](../README.md) | [Русский](README_ru.md) | [Deutsch](README_de.md) | [Italiano](README_it.md) | [Español](README_es.md) | [Tiếng Việt](README_vi.md) | [Srpski](README_sr.md)

`B2B CRM` je poslovna demonstraciona aplikacija zasnovana na `Jmix platformi` sa ugrađenim `AI`, koja prikazuje kako se grade produkcijski spremni poslovni sistemi, uključujući `kupce`, `porudžbine`, `fakturisanje`, `finansije` i `analitiku`.

## 📑 Sadržaj

- [Tehnički stek](#-tehnički-stek)
- [Pregled](#-pregled)
- [AI asistent](#-ai-asistent)
- [Dodaci](#-dodaci)
- [Pokretanje aplikacije](#-pokretanje-i-izvršavanje-aplikacije)
- [Demo podaci](#-demo-podaci)
- [Nalozi](#-korisnički-nalozi-aplikacije)
- [Model domena](#-model-domena)
- [Model uloga](#-model-uloga)
- [Više o Jmix-u](#ℹ-više-o-jmix-u)
- [FAQ](#-faq)

## 🛠️ Tehnički stek

- Java 21
- Jmix (Spring Boot & Vaadin Flow)
- HSQLDB

## 📖 Pregled

<details>
<summary>📸 Snimci ekrana (kliknite da proširite)</summary>

<br>

<h3>Stranica za prijavu</h3>
<img width="1496" height="816" alt="Stranica za prijavu" src="../images/screenshots/login-page.png" />

<h3>Kontrolna tabla</h3>
<img width="1496" height="816" alt="Kontrolna tabla" src="../images/screenshots/dashboard.png" />

<h3>CRM AI</h3>
<img width="1496" height="818" alt="CRM AI" src="../images/screenshots/crm-ai.png" />

<h3>Klijenti</h3>
<img width="1496" height="816" alt="Klijenti" src="../images/screenshots/clients.png" />

<h3>Porudžbine</h3>
<img width="1496" height="817" alt="Porudžbine" src="../images/screenshots/orders.png" />

<h3>O aplikaciji</h3>
<img width="1496" height="816" alt="O aplikaciji" src="../images/screenshots/about.png" />

</details>

Ovaj projekat modeluje tipičan B2B prodajni proces:

- Upravljanje katalogom proizvoda i kategorija
- Održavanje podataka o klijentima i kontaktima
- Praćenje porudžbina i stavki porudžbina
- Izdavanje faktura i evidentiranje uplata
- Postavljanje poslovnih pitanja AI asistentu
- Praćenje zadataka i nedavnih aktivnosti
- Analiza prodajnih rezultata

## 🤖 AI Asistent

Aplikacija uključuje ugrađeni radni prostor `CRM AI` za analizu CRM podataka korišćenjem prirodnog jezika.

Ključne mogućnosti:

- Postavljanje poslovnih pitanja o klijentima, porudžbinama, fakturama, uplatama i rezultatima prodaje
- Poštovanje prava pristupa podacima trenutnog korisnika i čuvanje privatnosti razgovora
- Korišćenje ugrađenih poslovnih izvještaja kao što su `Client 360 Report` i `Category Cashflow Risk Allocation Report`
- Čuvanje istorije razgovora sa automatski generisanim naslovima razgovora
- Otpremanje datoteka u razgovor i analiza podržanih dokumenata i slika
- Generisanje interaktivnih linkova ka CRM zapisima direktno u odgovorima

Konfiguracija:

- Postavite `spring.ai.openai.api-key` u datoteci [application.properties](../src/main/resources/application.properties) ili obezbijedite promenljivu okruženja `SPRING_AI_OPENAI_APIKEY`

Kada je funkcionalnost omogućena, otvorite stavku `CRM AI` u glavnom meniju da biste započeli novi razgovor.

## 🧩 Dodaci

- [AI Tools](https://www.jmix.io/marketplace/ai-tools/)
- [Audit](https://www.jmix.io/marketplace/audit/)
- [Application Settings](https://www.jmix.io/marketplace/application-settings/)
- [Charts](https://www.jmix.io/marketplace/charts/)
- [Data tools](https://www.jmix.io/marketplace/data-tools/)
- [Dynamic attributes](https://www.jmix.io/marketplace/dynamic-attributes/)
- [Grid export](https://www.jmix.io/marketplace/grid-export-actions/)
- [Reports](https://www.jmix.io/marketplace/reports/)
- Local file storage, Localizations

## 🚀 Pokretanje i izvršavanje aplikacije

Preduslovi: Java 21+

### Pokretanje projekta

1. Pokrenite Jmix konfiguraciju za pokretanje [B2B CRM](../.run/crm-app.run.xml) ili izvršite:

   ```bash
   ./gradlew bootRun
   ```

2. [Otvorite URL aplikacije](http://localhost:8080/b2b-crm)

### Pokretanje putem JAR datoteke

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

### Pokretanje putem Docker-a

```bash
docker build -t jmix-crm .
```

```bash
docker run --rm -p 8080:8080 jmix-crm
```

### Pokretanje putem Docker Compose-a

```bash
docker-compose up
```

## 🎲 Demo podaci

Lokalni profil generiše demo podatke prilikom pokretanja aplikacije:

- Generisanje demo podataka možete onemogućiti pomoću svojstva `crm.generateDemoData`
  u [datoteci application.properties](../src/main/resources/application.properties)
- Katalog se uvozi iz datoteke [catalog.xlsx](../src/main/resources/demo-data/catalog.xlsx)

## 👥 Korisnički nalozi aplikacije

| Pozicija        | Korisničko ime | Lozinka | Pristup                                           |
|-----------------|----------------|---------|---------------------------------------------------|
| Administrator   | ```admin```    | admin   | Potpun pristup svim podacima i podešavanjima      |
| Supervisor      | ```james```    | james   | Menadžer + upravljanje katalogom + dodjela naloga |
| Manager         | ```manager```  | manager | Potpun pristup svim klijentima i porudžbinama     |
| Account Manager | ```alice```    | alice   | Vidi samo klijente dodijeljene Alice Brown        |
| Account Manager | ```robert```   | robert  | Vidi samo klijente dodijeljene Robert Taylor      |

## ⚙️ Model domena

```mermaid
classDiagram
    Client o-- Contact
    Client o-- Order
    Client o-- Invoice
    Client o-- Payment
    Client o-- Address

    Order *-- OrderItem
    OrderItem --> CategoryItem
    Category o-- CategoryItem

    Invoice o-- Payment
```

## 🔐 Model uloga

Aplikacija koristi hijerarhijski model uloga:

- `Administrator`: Potpun pristup svim funkcionalnostima aplikacije, entitetima i podešavanjima.
- `Supervisor`: Proširuje ulogu Menadžera dodatnim administrativnim mogućnostima:
    - Upravljanje katalogom proizvoda (kategorije i artikli).
    - Dodjeljivanje menadžera klijenata klijentima.
- `Manager`: Osnovna uloga za prodajne aktivnosti.
    - Potpun pristup klijentima, kontaktima, porudžbinama, fakturama i uplatama.
    - Pristup katalogu proizvoda samo za čitanje.
    - Upravljanje sopstvenim zadacima.
- `UI Minimal`: Minimalan pristup koji omogućava prijavu u sistem i osnovnu navigaciju.

## ℹ️  Više o Jmix-u

- 🌐 Veb sajt: https://www.jmix.io/
- 📚 Dokumentacija: https://docs.jmix.io/
- 💻 GitHub: https://github.com/jmix-framework/jmix
- 🎥 YouTube: https://www.youtube.com/@jmixframework
- 💬 Forum: https://forum.jmix.io/
- 💼 LinkedIn: https://www.linkedin.com/company/jmix-framework/

## 💬 FAQ

> Šta je Jmix?

Jmix je full-stack open-source Java platforma za razvoj poslovnog softvera sa lokalnim i javnim modelima.
Pomaže razvojnim timovima da brže grade interne poslovne aplikacije uz potpunu kontrolu nad izvornim kodom, arhitekturom i isporukom. Jmix objedinjuje Java, Spring Boot, poslovni korisnički interfejs, bezbjednost, pristup podacima, alate za vizuelni razvoj i razvoj uz podršku AI u jednoj platformi.

Saznajte više:
- https://www.jmix.io/
- https://docs.jmix.io/
- https://github.com/jmix-framework/jmix

---

> Zašto je Jmix dobar za izgradnju CRM sistema?

CRM sistemi su postali osnova moderne poslovne automatizacije i daleko su prevazišli običnu evidenciju podataka. Kako se poslovni zahtjevi u prodaji brzo mijenjaju, CRM sistemi moraju omogućiti i brze izmjene procesa, modela podataka i korisničkog iskustva, uz očuvanje visokih standarda bezbjednosti i usklađenosti.
Jmix pruža te mogućnosti odmah po instalaciji, što razvojnim timovima omogućava da se fokusiraju na poslovnu logiku umjesto na infrastrukturu. Ova demonstracija pokazuje kako se produkcijski spremne poslovne aplikacije mogu razvijati pomoću Jmix-a i AI.

---

> Da li je ovo prava aplikacija ili samo demonstracija?

B2B CRM je demonstraciona aplikacija napravljena da prikaže produkcijski spremnu arhitekturu i prakse razvoja poslovnog softvera.
Uključuje stvarne poslovne scenarije, moderan korisnički interfejs, AI mogućnosti, bezbjednost, izvještavanje i integracione šablone koji se mogu ponovo koristiti u vašim poslovnim projektima.
