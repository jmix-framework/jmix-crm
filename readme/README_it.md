# ![CRM](../src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

Lingue: [English](../README.md) | [Русский](README_ru.md) | [Deutsch](README_de.md) | [Italiano](README_it.md) | [Español](README_es.md) | [Tiếng Việt](README_vi.md) | [Srpski](README_sr.md)

`B2B CRM` è un'applicazione demo sviluppata con Jmix che mostra come creare sistemi aziendali **pronti per la produzione**
per lavorare con `clienti`, `ordini`, `fatture`, `finanza` e `analitica`. <br>Rappresenta scenari reali **ERP/CRM** e dimostra
le best practice di modellazione del dominio, UI, sicurezza e implementazione della logica del business.

## 📑 Indice

- [Panoramica](#-panoramica)
- [Stack tecnico](#-stack-tecnico)
- [Add-on utilizzati](#-add-on-utilizzati)
- [Build ed esecuzione](#-build-ed-esecuzione)
- [Assistente AI](#-assistente-ai)
- [Dati demo](#-dati-demo)
- [Account](#-account-dellapplicazione)
- [Modello di dominio](#-modello-di-dominio)
- [Modello dei ruoli](#-modello-dei-ruoli)

## 📖 Panoramica

Questo progetto modella un tipico flusso di vendita B2B:

- Gestione del catalogo prodotti e categorie
- Gestione di clienti e contatti
- Tracciamento degli ordini e delle loro posizioni
- Emissione di fatture e registrazione dei pagamenti
- Richiesta di business-insight all'assistente AI
- Monitoraggio dei compiti e attività recenti
- Visualizzazione dell'analisi delle vendite

## 🛠️ Stack tecnico

- Java 21
- Jmix 2.8
- Spring Boot 3
- HSQLDB

## 🧩 Add-on utilizzati

- Audit
- Application settings
- Charts
- Data tools
- Dynamic attributes
- Grid export
- Local file storage
- Reports, incluso un modello di fattura

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

## 🎲 Dati demo

Il profilo locale genera dati demo all'avvio dell'applicazione:

- Puoi disabilitare la generazione dei dati demo con la proprietà `crm.generateDemoData`
  in [application.properties](../src/main/resources/application.properties)
- Il catalogo viene importato da [catalog.xlsx](../src/main/resources/demo-data/catalog.xlsx)

## 👥 Account dell'applicazione

| Posizione       | Nome utente   | Password | Accesso                                          |
|-----------------|---------------|----------|--------------------------------------------------|
| Administrator   | ```admin```   | admin    | Accesso completo a tutti i dati e impostazioni   |
| Supervisor      | ```james```   | james    | Manager + gestione catalogo + assegnazione account |
| Manager         | ```manager``` | manager  | Accesso completo a tutti i clienti e ordini      |
| Account Manager | ```alice```   | alice    | Vede solo i clienti assegnati ad Alice Brown     |
| Account Manager | ```robert```  | robert   | Vede solo i clienti assegnati a Robert Taylor    |

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
