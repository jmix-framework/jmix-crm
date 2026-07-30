# ![CRM](../src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

🖥️ [Online Demo](https://demo.jmix.io/b2b-crm/login)

🌐 Sprachen: [English](../README.md) | [Русский](README_ru.md) | [Deutsch](README_de.md) | [Italiano](README_it.md) | [Español](README_es.md) | [Tiếng Việt](README_vi.md) | [Srpski](README_sr.md)

`B2B CRM` ist eine Enterprise-Demoanwendung auf Basis des `Jmix Frameworks` mit integrierter `AI`, die zeigt, wie produktionsreife Geschäftssysteme für `Kunden`, `Aufträge`, `Rechnungsstellung`, `Finanzen` und `Analysen` entwickelt werden.

## 📑 Inhaltsverzeichnis

- [Technischer Stack](#-technischer-stack)
- [Überblick](#-überblick)
- [AI-Assistent](#-ai-assistent)
- [Add-ons](#-verwendete-add-ons)
- [Build & Start](#-build-und-start)
- [Demo-Daten](#-demo-daten)
- [Konten](#-anwendungskonten)
- [Domänenmodell](#-domänenmodell)
- [Rollenmodell](#-rollenmodell)
- [Mehr über Jmix](#ℹ-mehr-über-jmix)
- [FAQ](#-faq)

## 🛠️ Technischer Stack

- Java 21
- Jmix (Spring Boot & Vaadin Flow)
- HSQLDB

## 📖 Überblick

<details>
<summary>📸 Screenshots (zum Aufklappen klicken)</summary>

<br>

<h3>Anmeldeseite</h3>
<img width="1496" height="816" alt="Anmeldeseite" src="../images/screenshots/login-page.png" />

<h3>Dashboard</h3>
<img width="1496" height="816" alt="Dashboard" src="../images/screenshots/dashboard.png" />

<h3>CRM AI</h3>
<img width="1496" height="818" alt="CRM AI" src="../images/screenshots/crm-ai.png" />

<h3>Kunden</h3>
<img width="1496" height="816" alt="Kunden" src="../images/screenshots/clients.png" />

<h3>Aufträge</h3>
<img width="1496" height="817" alt="Aufträge" src="../images/screenshots/orders.png" />

<h3>Über die Anwendung</h3>
<img width="1496" height="816" alt="Über die Anwendung" src="../images/screenshots/about.png" />

</details>

Dieses Projekt modelliert einen typischen B2B-Vertriebsablauf:

- Produkt- und Kategorienkatalog verwalten
- Kunden und Kontakte pflegen
- Aufträge und Auftragspositionen nachverfolgen
- Rechnungen ausstellen und Zahlungen erfassen
- Einen AI-Assistenten nach geschäftlichen Erkenntnissen fragen
- Aufgaben und letzte Aktivitäten überwachen
- Vertriebsanalysen anzeigen

## 🤖 AI-Assistent

Die Anwendung enthält einen integrierten `CRM AI`-Arbeitsbereich für die natürlichsprachliche Analyse von CRM-Daten.

Wichtige Funktionen:

- Geschäftsfragen zu Kunden, Aufträgen, Rechnungen, Zahlungen und Vertriebsleistung stellen
- Die Datenzugriffsrechte des aktuellen Benutzers berücksichtigen und Konversationen nur für ihren Autor sichtbar halten
- Integrierte Geschäftsberichte wie `Client 360 Report` und `Category Cashflow Risk Allocation Report` verwenden
- Den Konversationsverlauf mit automatisch generierten Chat-Titeln speichern
- Dateien in die Konversation hochladen und den Assistenten unterstützte Dokumente und Bilder analysieren lassen
- Interaktive Links zu CRM-Datensätzen direkt in Antworten generieren

Konfiguration:

- Setze `spring.ai.openai.api-key` in [application.properties](../src/main/resources/application.properties) oder stelle die Umgebungsvariable `SPRING_AI_OPENAI_APIKEY` bereit

Nach der Aktivierung öffne den Menüpunkt `CRM AI` im Hauptmenü, um eine neue Konversation zu starten.

## 🧩 Verwendete Add-ons

- [AI Tools](https://www.jmix.io/marketplace/ai-tools/)
- [Audit](https://www.jmix.io/marketplace/audit/)
- [Application Settings](https://www.jmix.io/marketplace/application-settings/)
- [Charts](https://www.jmix.io/marketplace/charts/)
- [Data tools](https://www.jmix.io/marketplace/data-tools/)
- [Dynamic attributes](https://www.jmix.io/marketplace/dynamic-attributes/)
- [Grid export](https://www.jmix.io/marketplace/grid-export-actions/)
- [Reports](https://www.jmix.io/marketplace/reports/)
- Local file storage, Localizations

## 🚀 Build und Start

Voraussetzungen: Java 21+

### Projekt starten

1. Starte die Jmix-Run-Konfiguration [B2B CRM](../.run/crm-app.run.xml) oder führe aus

   ```bash
   ./gradlew bootRun
   ```

2. [Anwendungs-URL öffnen](http://localhost:8080/b2b-crm)

### Start per JAR

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

### Start per Docker

```bash
docker build -t jmix-crm .
```

```bash
docker run --rm -p 8080:8080 jmix-crm
```

### Start per Docker Compose

```bash
docker-compose up
```

## 🎲 Demo-Daten

Das lokale Profil generiert Demo-Daten beim Start der Anwendung:

- Die Generierung von Demo-Daten kann mit der Eigenschaft `crm.generateDemoData`
  in [application.properties](../src/main/resources/application.properties) deaktiviert werden
- Der Katalog wird aus [catalog.xlsx](../src/main/resources/demo-data/catalog.xlsx) importiert

## 👥 Anwendungskonten

| Position        | Benutzername  | Passwort | Zugriff                                             |
|-----------------|---------------|----------|-----------------------------------------------------|
| Administrator   | ```admin```   | admin    | Vollzugriff auf alle Daten und Einstellungen        |
| Supervisor      | ```james```   | james    | Manager + Katalogverwaltung + Konten zuweisen       |
| Manager         | ```manager``` | manager  | Vollzugriff auf alle Kunden und Aufträge            |
| Account Manager | ```alice```   | alice    | Sieht nur Kunden, die Alice Brown zugewiesen sind   |
| Account Manager | ```robert```  | robert   | Sieht nur Kunden, die Robert Taylor zugewiesen sind |

## ⚙️ Domänenmodell

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

## 🔐 Rollenmodell

Die Anwendung verwendet ein hierarchisches Rollenmodell:

- `Administrator`: Vollzugriff auf alle Anwendungsfunktionen, Entitäten und Einstellungen.
- `Supervisor`: Erweitert die Manager-Rolle um zusätzliche administrative Funktionen:
    - Produktkatalog verwalten, einschließlich Categories und Category Items.
    - Account Managers Kunden zuweisen.
- `Manager`: Primäre Rolle für Vertriebsprozesse.
    - Vollzugriff auf Clients, Contacts, Orders, Invoices und Payments.
    - Lesezugriff auf den Produktkatalog.
    - Eigene Tasks verwalten.
- `UI Minimal`: Minimaler Zugriff, der Anmeldung und grundlegende Navigation ermöglicht.

## ℹ️  Mehr über Jmix

- 🌐 Website: https://www.jmix.io/
- 📚 Dokumentation: https://docs.jmix.io/
- 💻 GitHub: https://github.com/jmix-framework/jmix
- 🎥 YouTube: https://www.youtube.com/@jmixframework
- 💬 Forum: https://forum.jmix.io/
- 💼 LinkedIn: https://www.linkedin.com/company/jmix-framework/

## 💬 FAQ

> Was ist Jmix?

Jmix ist eine Full-Stack-Open-Source-Java-Plattform für die Entwicklung von Unternehmenssoftware mit lokalen und öffentlichen Modellen.
Sie hilft Entwicklungsteams, interne Geschäftsanwendungen schneller zu erstellen und dabei die vollständige Kontrolle über Quellcode, Architektur und Deployment zu behalten. Jmix vereint Java, Spring Boot, Enterprise-UI, Sicherheit, Datenzugriff, visuelle Entwicklungswerkzeuge und AI-gestützte Entwicklung in einer einzigen Plattform.

Mehr erfahren:
- https://www.jmix.io/
- https://docs.jmix.io/
- https://github.com/jmix-framework/jmix

---

> Warum ist Jmix gut für den Aufbau von CRM-Systemen geeignet?

CRM-Systeme sind zum Rückgrat der modernen Unternehmensautomatisierung geworden und gehen weit über ein einfaches Aufzeichnungssystem hinaus. Da sich Geschäftsanforderungen im Vertrieb schnell ändern, müssen CRM-Systeme auch die Möglichkeit bieten, Workflows, Datenmodell und UX schnell anzupassen und dabei hohe Sicherheits- und Compliance-Standards einzuhalten.
Jmix bietet diese Fähigkeiten von Anfang an, sodass sich Entwickler auf die Geschäftslogik statt auf die Infrastruktur konzentrieren können. Diese Demo zeigt, wie produktionsreife Unternehmensanwendungen mit Jmix und AI entwickelt werden können.

---

> Ist das eine echte Anwendung oder nur eine Demo?

B2B CRM ist eine Demoanwendung, die produktionsreife Architektur und Praktiken der Unternehmensentwicklung demonstrieren soll.
Sie enthält reale Geschäftsszenarien, eine moderne UI, AI-Funktionen, Sicherheit, Reporting und Integrationsmuster, die in eigenen Unternehmensprojekten wiederverwendet werden können.
