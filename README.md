# ![CRM](src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

🖥️ [Online Demo](https://demo.jmix.io/b2b-crm/login)

🌐 Languages: [English](README.md) | [Русский](readme/README_ru.md) | [Deutsch](readme/README_de.md) | [Italiano](readme/README_it.md) | [Español](readme/README_es.md) | [Tiếng Việt](readme/README_vi.md) | [Srpski](readme/README_sr.md)

`B2B CRM` is an enterprise demo application based on `Jmix framework` with built-in `AI` that showcases how to develop
production-ready business systems including `customers`, `orders`, `invoicing`, `finance` and `analytics`.

## 📑 Table of Contents

- [Technical Stack](#-technical-stack)
- [Overview](#-overview)
- [AI Assistant](#-ai-assistant)
- [Add-ons](#-add-ons)
- [Build & Run](#-build-and-run)
- [Demo Data](#-demo-data)
- [Accounts](#-application-accounts)
- [Domain Model](#-domain-model)
- [Role Model](#-role-model)
- [More About Jmix](#ℹ-more-about-jmix)
- [FAQ](#-faq)

## 🛠️ Technical Stack

- Java 21
- Jmix (Spring Boot & Vaadin Flow)
- HSQLDB

## 📖 Overview

<details>
<summary>📸 Screenshots (click to expand)</summary>

<h3>Login Page</h3>
<img width="1496" height="816" alt="Login Page" src="images/screenshots/login-page.png" />

<h3>Dashboard</h3>
<img width="1496" height="816" alt="Dashboard" src="images/screenshots/dashboard.png" />

<h3>CRM AI</h3>
<img width="1496" height="818" alt="CRM AI" src="images/screenshots/crm-ai.png" />

<h3>Clients</h3>
<img width="1496" height="816" alt="Clients" src="images/screenshots/clients.png" />

<h3>Orders</h3>
<img width="1496" height="817" alt="Orders" src="images/screenshots/orders.png" />

<h3>About</h3>
<img width="1496" height="816" alt="About" src="images/screenshots/about.png" />

</details>

### ✨ Main Features

This project models a typical B2B sales workflow:

- Manage the catalog of products and categories
- Maintain clients, contacts, and addresses
- Track orders through the sales funnel
- Issue invoices and record payments
- Plan and monitor user tasks
- Ask the built-in AI assistant for business insights
- See sales analytics on the dashboard and in built-in reports

#### 📈 Sales Automation

`B2B CRM` helps sales managers automate the sales process: the system keeps track of deals, invoices, payments
and user tasks, and provides quick analytics on clients. For example, it can quickly answer typical questions such as:

- How many deals are at the presale stage or awaiting payment, and for what total amount
- Which clients are the revenue leaders and which are the outsiders — and in which product categories
- How often the selected clients make purchases
- How proposals for a client compare to each other, and what were the maximum discounts for a particular product category

Normally, such requests require configuring specialized reports and analyst expertise. In `B2B CRM` it is enough to
write the request in natural language: the built-in [AI Assistant](#-ai-assistant)
helps analyze sales by aggregating data on deals, invoices, and payments while respecting the user's data access
permissions.

#### 🔽 Sales Funnel

The `Orders` screen features an interactive sales funnel based on order statuses: 
`New` → `Accepted` → `In Progress` →`Done`. 
Each stage shows the number of orders at that stage, and a single click takes the manager to the orders of the
selected stage — with the total, invoiced, paid, and remaining amounts for each order.

## 🤖 AI Assistant

The application includes a built-in `CRM AI` workspace for natural-language analysis of CRM data.

#### ✨ Key capabilities:

- Ask business questions about clients, orders, invoices, payments, and sales performance
- Upload entities and files to the conversation context
- Respect the current user's data access permissions and keep conversations private to their author
- Use built-in business reports such as `Client 360 Report` and `Category Cashflow Risk Allocation Report`
- Keep the conversation history with automatically generated chat titles
- Generate interactive links to CRM records directly in responses

#### ⚙️ Configuration:

Set `spring.ai.openai.api-key` in [application.properties](src/main/resources/application.properties) 
or provide the `SPRING_AI_OPENAI_APIKEY` environment variable.

When enabled, open the `CRM AI` item in the main menu to start a new conversation.

## 🧩 Add-ons

- [AI Tools](https://www.jmix.io/marketplace/ai-tools/)
- [Audit](https://www.jmix.io/marketplace/audit/)
- [Application Settings](https://www.jmix.io/marketplace/application-settings/)
- [Charts](https://www.jmix.io/marketplace/charts/)
- [Data tools](https://www.jmix.io/marketplace/data-tools/)
- [Dynamic attributes](https://www.jmix.io/marketplace/dynamic-attributes/)
- [Grid export](https://www.jmix.io/marketplace/grid-export-actions/)
- [Reports](https://www.jmix.io/marketplace/reports/)
- Local File Storage, Localizations

## 🚀 Build and Run

#### Run Project

1. Run [B2B CRM](.run/crm-app.run.xml) Jmix run configuration or execute

   ```bash
   ./gradlew bootRun
   ```

2. [Open application URL](http://localhost:8080/b2b-crm)

#### Run via JAR:

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

#### Run via Docker

```bash
docker build -t jmix-crm .
```

```bash
docker run --rm -p 8080:8080 jmix-crm
```

#### Run via Docker Compose

```bash
docker-compose up
```

## 🎲 Demo Data

The local profile generates demo data on the application start:

- You can disable demo data generation with `crm.generateDemoData` property
  in [application.properties](src/main/resources/application.properties)
- Catalog imported from [catalog.xlsx](src/main/resources/demo-data/catalog.xlsx)

## 👥 Application Accounts

| Position        | Username  | Password | Access                                         |
|-----------------|-----------|----------|------------------------------------------------|
| Administrator   | `admin`   | admin    | Full access to all data and settings           |
| Supervisor      | `james`   | james    | Manager + catalog management + assign accounts |
| Manager         | `manager` | manager  | Full access to all clients and orders          |
| Account Manager | `alice`   | alice    | Only sees clients assigned to Alice Brown      |
| Account Manager | `robert`  | robert   | Only sees clients assigned to Robert Taylor    |

## ⚙️ Domain Model

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

## 🔐 Role Model

The application uses a hierarchical role model:

| Role            | Description                                                                                                                                                     |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Administrator` | Full access to all application features, entities, and settings.                                                                                                |
| `Supervisor`    | Extends the `Manager` role with additional administrative capabilities: manage product catalog and assign account managers to clients.                          |
| `Manager`       | Primary role for sales operations. Full access to Clients, Contacts, Orders, Invoices, and Payments. Read-only access to the product catalog. Manage own Tasks. |
| `UI Minimal`    | Minimal access, allowing login and basic navigation.                                                                                                            |

## ℹ️ More about Jmix

| Source           | Link                                            |
|------------------|-------------------------------------------------|
| 🌐 Website       | https://www.jmix.io                             |
| 📚 Documentation | https://docs.jmix.io                            |
| 💬 Forum         | https://forum.jmix.io                           |
| 💻 GitHub        | https://github.com/jmix-framework/jmix          |
| 🎥 YouTube       | https://www.youtube.com/@jmixframework          |
| 💼 LinkedIn      | https://www.linkedin.com/company/jmix-framework |

## 💬 FAQ

> What is Jmix?

Jmix is a full-stack open-source Java platform for enterprise software development with local and public models. 
It helps development teams build internal business applications faster while keeping full control over the source code, architecture, and deployment. 
Jmix combines Java, Spring Boot, enterprise UI, security, data access, visual development tools, and AI-assisted development in a single platform.

**Learn more:**

| Source | Link                                   |
|--------|----------------------------------------|
| Site   | https://www.jmix.io/                   |
| Docs   | https://docs.jmix.io/                  |
| GitHub | https://github.com/jmix-framework/jmix |

---

> Why is Jmix good for building CRM systems?

CRM systems have become the backbone of modern enterprise automation, moving far beyond a simple system of records. As
business requirements in sales change rapidly, CRM systems must also provide capabilities to implement quick changes in
workflows, data model, and UX while preserving high security and compliance standards. Jmix provides these capabilities
out of the box, allowing developers to focus on business logic instead of infrastructure. This demo demonstrates how
production-ready enterprise applications can be developed using Jmix and AI.

---

> Is this a real application or just a demo?

B2B CRM is a demo application designed to demonstrate production-ready architecture and enterprise development
practices. It includes real business scenarios, modern UI, AI capabilities, security, reporting, and integration
patterns that can be reused in your own enterprise projects. 
