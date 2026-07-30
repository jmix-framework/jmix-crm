# ![CRM](../src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

🖥️ [Online Demo](https://demo.jmix.io/b2b-crm/login)

🌐 Lingue: [English](../README.md) | [Русский](README_ru.md) | [Deutsch](README_de.md) | [Italiano](README_it.md) | [Español](README_es.md) | [Tiếng Việt](README_vi.md) | [Srpski](README_sr.md)

`B2B CRM` è un'applicazione demo aziendale basata sul `framework Jmix` con `AI` integrata, che mostra come sviluppare sistemi di business pronti per la produzione per `clienti`, `ordini`, `fatture`, `finanza` e `analitica`.

## 📑 Indice

- [Stack tecnico](#-stack-tecnico)
- [Panoramica](#-panoramica)
- [Assistente AI](#-assistente-ai)
- [Add-on](#-add-on-utilizzati)
- [Build ed esecuzione](#-build-ed-esecuzione)
- [Dati demo](#-dati-demo)
- [Account](#-account-dellapplicazione)
- [Modello di dominio](#-modello-di-dominio)
- [Modello dei ruoli](#-modello-dei-ruoli)
- [Maggiori informazioni su Jmix](#ℹ-maggiori-informazioni-su-jmix)
- [FAQ](#-faq)

## 🛠️ Stack tecnico

- Java 21
- Jmix (Spring Boot & Vaadin Flow)
- HSQLDB

## 📖 Panoramica

<details>
<summary>📸 Screenshot (clicca per espandere)</summary>

<br>

<h3>Pagina di accesso</h3>
<img width="1496" height="816" alt="Pagina di accesso" src="../images/screenshots/login-page.png" />

<h3>Dashboard</h3>
<img width="1496" height="816" alt="Dashboard" src="../images/screenshots/dashboard.png" />

<h3>CRM AI</h3>
<img width="1496" height="818" alt="CRM AI" src="../images/screenshots/crm-ai.png" />

<h3>Clienti</h3>
<img width="1496" height="816" alt="Clienti" src="../images/screenshots/clients.png" />

<h3>Ordini</h3>
<img width="1496" height="817" alt="Ordini" src="../images/screenshots/orders.png" />

<h3>Informazioni</h3>
<img width="1496" height="816" alt="Informazioni" src="../images/screenshots/about.png" />

</details>

Questo progetto modella un tipico flusso di vendita B2B:

- Gestione del catalogo prodotti e categorie
- Gestione di clienti e contatti
- Tracciamento degli ordini e delle loro posizioni
- Emissione di fatture e registrazione dei pagamenti
- Richiesta di business-insight all'assistente AI
- Monitoraggio dei compiti e attività recenti
- Visualizzazione dell'analisi delle vendite

## 🤖 Assistente AI

L'applicazione include uno spazio di lavoro integrato `CRM AI` per l'analisi in linguaggio naturale dei dati CRM.

Funzionalità principali:

- Porre domande di business su clienti, ordini, fatture, pagamenti e performance delle vendite
- Rispettare le autorizzazioni di accesso ai dati dell'utente corrente e mantenere le conversazioni private per il loro autore
- Usare report aziendali integrati come `Client 360 Report` e `Category Cashflow Risk Allocation Report`
- Conservare la cronologia delle conversazioni con titoli generati automaticamente
- Caricare file nella conversazione e lasciare che l'assistente analizzi documenti e immagini supportati
- Generare link interattivi ai record CRM direttamente nelle risposte

Configurazione:

- Imposta `spring.ai.openai.api-key` in [application.properties](../src/main/resources/application.properties) oppure fornisci la variabile d'ambiente `SPRING_AI_OPENAI_APIKEY`

Dopo l'avvio apri la voce `CRM AI` nel menu principale per iniziare una nuova conversazione.

## 🧩 Add-on utilizzati

- [AI Tools](https://www.jmix.io/marketplace/ai-tools/)
- [Audit](https://www.jmix.io/marketplace/audit/)
- [Application Settings](https://www.jmix.io/marketplace/application-settings/)
- [Charts](https://www.jmix.io/marketplace/charts/)
- [Data tools](https://www.jmix.io/marketplace/data-tools/)
- [Dynamic attributes](https://www.jmix.io/marketplace/dynamic-attributes/)
- [Grid export](https://www.jmix.io/marketplace/grid-export-actions/)
- [Reports](https://www.jmix.io/marketplace/reports/)
- Local file storage, Localizations

## 🚀 Build ed esecuzione

Prerequisiti: Java 21+

### Esecuzione del progetto

1. Avvia la configurazione Jmix [B2B CRM](../.run/crm-app.run.xml) oppure esegui

   ```bash
   ./gradlew bootRun
   ```

2. [Apri l'URL dell'applicazione](http://localhost:8080/b2b-crm)

### Esecuzione tramite JAR

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

### Esecuzione tramite Docker

```bash
docker build -t jmix-crm .
```

```bash
docker run --rm -p 8080:8080 jmix-crm
```

### Esecuzione tramite Docker Compose

```bash
docker-compose up
```

## 🎲 Dati demo

Il profilo locale genera dati demo all'avvio dell'applicazione:

- Puoi disabilitare la generazione dei dati demo con la proprietà `crm.generateDemoData`
  in [application.properties](../src/main/resources/application.properties)
- Il catalogo viene importato da [catalog.xlsx](../src/main/resources/demo-data/catalog.xlsx)

## 👥 Account dell'applicazione

| Posizione       | Nome utente   | Password | Accesso                                            |
|-----------------|---------------|----------|----------------------------------------------------|
| Administrator   | ```admin```   | admin    | Accesso completo a tutti i dati e impostazioni     |
| Supervisor      | ```james```   | james    | Manager + gestione catalogo + assegnazione account |
| Manager         | ```manager``` | manager  | Accesso completo a tutti i clienti e ordini        |
| Account Manager | ```alice```   | alice    | Vede solo i clienti assegnati ad Alice Brown       |
| Account Manager | ```robert```  | robert   | Vede solo i clienti assegnati a Robert Taylor      |

## ⚙️ Modello di dominio

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

## 🔐 Modello dei ruoli

L'applicazione usa un modello di ruoli gerarchico:

- `Administrator`: accesso completo a tutte le funzionalità, entità e impostazioni dell'applicazione.
- `Supervisor`: estende il ruolo Manager con capacità amministrative aggiuntive:
    - Gestione del catalogo prodotti, incluse Categories e Category Items.
    - Assegnazione Account Managers ai Clients.
- `Manager`: ruolo principale per le operazioni di vendita.
    - Accesso completo a Clients, Contacts, Orders, Invoices e Payments.
    - Accesso in sola lettura al catalogo prodotti.
    - Gestione dei propri Tasks.
- `UI Minimal`: accesso minimo che consente login e navigazione di base.

## ℹ️  Maggiori informazioni su Jmix

- 🌐 Sito web: https://www.jmix.io/
- 📚 Documentazione: https://docs.jmix.io/
- 💻 GitHub: https://github.com/jmix-framework/jmix
- 🎥 YouTube: https://www.youtube.com/@jmixframework
- 💬 Forum: https://forum.jmix.io/
- 💼 LinkedIn: https://www.linkedin.com/company/jmix-framework/

## 💬 FAQ

> Che cos'è Jmix?

Jmix è una piattaforma Java full-stack open source per lo sviluppo di software aziendale con modelli locali e pubblici.
Aiuta i team di sviluppo a creare più rapidamente applicazioni di business interne mantenendo il pieno controllo su codice sorgente, architettura e deployment. Jmix combina Java, Spring Boot, UI aziendale, sicurezza, accesso ai dati, strumenti di sviluppo visuale e sviluppo assistito dall'AI in un'unica piattaforma.

Scopri di più:
- https://www.jmix.io/
- https://docs.jmix.io/
- https://github.com/jmix-framework/jmix

---

> Perché Jmix è adatto alla creazione di sistemi CRM?

I sistemi CRM sono diventati la spina dorsale della moderna automazione aziendale, andando molto oltre il semplice sistema di registrazione dei dati. Poiché i requisiti di business nelle vendite cambiano rapidamente, i sistemi CRM devono anche offrire la possibilità di implementare modifiche rapide a processi, modello dati e UX, mantenendo elevati standard di sicurezza e conformità.
Jmix fornisce queste capacità out of the box, permettendo agli sviluppatori di concentrarsi sulla logica di business invece che sull'infrastruttura. Questa demo mostra come applicazioni aziendali pronte per la produzione possano essere sviluppate con Jmix e AI.

---

> È un'applicazione reale o solo una demo?

B2B CRM è un'applicazione demo progettata per dimostrare un'architettura pronta per la produzione e pratiche di sviluppo aziendale.
Include scenari di business reali, UI moderna, funzionalità AI, sicurezza, reportistica e pattern di integrazione riutilizzabili nei tuoi progetti aziendali.
