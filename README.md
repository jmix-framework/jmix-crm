# ![B2B CRM](src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

### Is a Jmix application for managing client relationships.

## Table of Contents

-   [Overview](#overview)
-   [Domain Model](#domain-model)
-   [Technical stack](#technical-stack)
-   [Add-ons in use](#add-ons-in-use)
-   [Build and run](#build-and-run)
-   [Demo data](#demo-data)
-   [Accounts](#application-accounts)

## Overview

This project models a typical B2B sales workflow:

-   Manage catalog of your products and categories
-   Maintain clients and contacts
-   Track orders and order items
-   Issue invoices and record payments
-   Monitor tasks and recent activities
-   See sales analytics

## Technical Stack

-   Java 21
-   Jmix 2.7
-   Spring Boot 3
-   HSQLDB

## Add-ons

-   Audit
-   Application settings
-   Data tools
-   Dynamic attributes
-   Reports (includes an invoice template)
-   Charts
-   Grid export
-   Local file storage
-   JMX console

## Build and Run

Prerequisites: Java 21+

### Run Project

1.  Run [B2B CRM](.run/b2b-app.run.xml) Jmix run configuration or execute
    
    ```bash
    ./gradlew bootRun
    ```
    
2.  [Open application URL](http://localhost:8080/crm)
    

### Build JAR:

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

### Run via Docker

```bash
docker build -t b2b-crm .
```

```bash
docker run --rm -p 8080:8080 b2b-crm
```

### Run via Docker Compose

```bash
docker-compose up
```

## Demo Data

The local profile generates demo data on the application start:

-   You can disable demo data generation with `crm.generateDemoData` property in [application.properties](src/main/resources/application.properties)
-   Catalog imported from [catalog.xlsx](src/main/resources/demo-data/catalog.xlsx)

## Application Accounts

| Position      | Username   | Password   |
|---------------|------------|------------|
| Administrator | admin      | admin      |
| Supervisor    | supervisor | supervisor |
| Manager       | manager    | manager    |
| User          | alice      | alice      |

## Domain Model

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