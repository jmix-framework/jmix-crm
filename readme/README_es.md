# ![CRM](../src/main/resources/META-INF/resources/images/logo.svg) B2B CRM

🖥️ [Online Demo](https://demo.jmix.io/b2b-crm/login)

🌐 Idiomas: [English](../README.md) | [Русский](README_ru.md) | [Deutsch](README_de.md) | [Italiano](README_it.md) | [Español](README_es.md) | [Tiếng Việt](README_vi.md) | [Srpski](README_sr.md)

`B2B CRM` es una aplicación demo empresarial basada en el `framework Jmix` con `AI` integrada que muestra cómo desarrollar sistemas de negocio listos para producción con `clientes`, `pedidos`, `facturación`, `finanzas` y `analítica`.

## 📑 Índice

- [Stack tecnológico](#-stack-tecnológico)
- [Resumen](#-resumen)
- [Asistente de IA](#-asistente-de-ia)
- [Add-ons](#-add-ons-utilizados)
- [Build y ejecución](#-build-y-ejecución)
- [Datos demo](#-datos-demo)
- [Cuentas](#-cuentas-de-la-aplicación)
- [Modelo de dominio](#-modelo-de-dominio)
- [Modelo de roles](#-modelo-de-roles)
- [Más sobre Jmix](#ℹ-más-sobre-jmix)
- [FAQ](#-faq)

## 🛠️ Stack tecnológico

- Java 21
- Jmix (Spring Boot & Vaadin Flow)
- HSQLDB

## 📖 Resumen

<details>
<summary>📸 Capturas de pantalla (haz clic para expandir)</summary>

<br>

<h3>Página de inicio de sesión</h3>
<img width="1496" height="816" alt="Página de inicio de sesión" src="../images/screenshots/login-page.png" />

<h3>Panel de control</h3>
<img width="1496" height="816" alt="Panel de control" src="../images/screenshots/dashboard.png" />

<h3>CRM AI</h3>
<img width="1496" height="818" alt="CRM AI" src="../images/screenshots/crm-ai.png" />

<h3>Clientes</h3>
<img width="1496" height="816" alt="Clientes" src="../images/screenshots/clients.png" />

<h3>Pedidos</h3>
<img width="1496" height="817" alt="Pedidos" src="../images/screenshots/orders.png" />

<h3>Acerca de</h3>
<img width="1496" height="816" alt="Acerca de" src="../images/screenshots/about.png" />

</details>

Este proyecto modela un flujo típico de ventas B2B para:

- gestionar el catálogo de productos y categorías
- mantener clientes y contactos
- hacer seguimiento de pedidos y líneas de pedido
- emitir facturas y registrar pagos
- preguntar a un asistente de IA por perspectivas de negocio
- supervisar tareas y actividades recientes
- ver analítica de ventas

## 🤖 Asistente de IA

La aplicación incluye un espacio de trabajo integrado `CRM AI` para el análisis de datos CRM en lenguaje natural.

Capacidades principales:

- Hacer preguntas de negocio sobre clientes, pedidos, facturas, pagos y rendimiento de ventas
- Respetar los permisos de acceso a datos del usuario actual y mantener las conversaciones privadas para su autor
- Usar informes de negocio integrados como `Client 360 Report` y `Category Cashflow Risk Allocation Report`
- Mantener el historial de conversación con títulos de chat generados automáticamente
- Subir archivos a la conversación y permitir que el asistente analice documentos e imágenes compatibles
- Generar enlaces interactivos a registros CRM directamente en las respuestas

Configuración:

- Define `spring.ai.openai.api-key` en [application.properties](../src/main/resources/application.properties) o proporciona la variable de entorno `SPRING_AI_OPENAI_APIKEY`

Cuando esté habilitado, abre el elemento `CRM AI` en el menú principal para iniciar una nueva conversación.

## 🧩 Add-ons utilizados

- [AI Tools](https://www.jmix.io/marketplace/ai-tools/)
- [Audit](https://www.jmix.io/marketplace/audit/)
- [Application Settings](https://www.jmix.io/marketplace/application-settings/)
- [Charts](https://www.jmix.io/marketplace/charts/)
- [Data tools](https://www.jmix.io/marketplace/data-tools/)
- [Dynamic attributes](https://www.jmix.io/marketplace/dynamic-attributes/)
- [Grid export](https://www.jmix.io/marketplace/grid-export-actions/)
- [Reports](https://www.jmix.io/marketplace/reports/)
- Local file storage, Localizations

## 🚀 Build y ejecución

Requisitos: Java 21+

### Para ejecutar el proyecto

1. Ejecuta la configuración Jmix [B2B CRM](../.run/crm-app.run.xml) o ejecuta

   ```bash
   ./gradlew bootRun
   ```

2. [Abre la URL de la aplicación](http://localhost:8080/b2b-crm)

### Ejecutar mediante JAR

```bash
./gradlew bootJar -Pvaadin.productionMode
```

```bash
java -jar build/libs/crm.jar
```

### Ejecutar mediante Docker

```bash
docker build -t jmix-crm .
```

```bash
docker run --rm -p 8080:8080 jmix-crm
```

### Ejecutar mediante Docker Compose

```bash
docker-compose up
```

## 🎲 Datos demo

El perfil local genera datos demo al iniciar la aplicación:

- Puedes desactivar la generación de datos demo con la propiedad `crm.generateDemoData`
  en [application.properties](../src/main/resources/application.properties)
- El catálogo se importa desde [catalog.xlsx](../src/main/resources/demo-data/catalog.xlsx)

## 👥 Cuentas de la aplicación

| Puesto                                    | Usuario   | Contraseña | Acceso                                                |
| ----------------------------------------- | --------- | ---------- | ----------------------------------------------------- |
| Administrador del sistema (Administrator) | `admin`   | admin      | Acceso completo a todos los datos y configuraciones   |
| Supervisor                                | `james`   | james      | Manager + gestión de catálogo + asignación de cuentas |
| Manager                                   | `manager` | manager    | Acceso completo a todos los clientes y pedidos        |
| Gestora de cuenta (Account Manager)       | `alice`   | alice      | Solo ve clientes asignados a Alice Brown              |
| Gestor de cuenta (Account Manager)        | `robert`  | robert     | Solo ve clientes asignados a Robert Taylor            |

## ⚙️ Modelo de dominio

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

## 🔐 Modelo de roles

La aplicación usa un modelo jerárquico de roles:

- `Administrator`: acceso completo a todas las funciones, entidades y configuraciones de la aplicación.
- `Supervisor`: como el rol Manager (ver más abajo), pero con capacidades administrativas adicionales:
  - Gestiona el catálogo de productos, incluidas Categories y Category Items.
  - Asigna Account Managers a Clients.
- `Manager`: rol principal para operaciones de ventas.
  - Acceso completo a Clients, Contacts, Orders, Invoices y Payments.
  - Acceso de solo lectura al catálogo de productos.
  - Gestión de sus propias Tasks.
- `UI Minimal`: acceso mínimo que permite iniciar sesión y navegación básica.

## ℹ️  Más sobre Jmix

- 🌐 Sitio web: https://www.jmix.io/
- 📚 Documentación: https://docs.jmix.io/
- 💻 GitHub: https://github.com/jmix-framework/jmix
- 🎥 YouTube: https://www.youtube.com/@jmixframework
- 💬 Foro: https://forum.jmix.io/
- 💼 LinkedIn: https://www.linkedin.com/company/jmix-framework/

## 💬 FAQ

> ¿Qué es Jmix?

Jmix es una plataforma Java full-stack de código abierto para el desarrollo de software empresarial con modelos locales y públicos.
Ayuda a los equipos de desarrollo a crear aplicaciones de negocio internas más rápido manteniendo el control total sobre el código fuente, la arquitectura y el despliegue. Jmix combina Java, Spring Boot, UI empresarial, seguridad, acceso a datos, herramientas de desarrollo visual y desarrollo asistido por IA en una única plataforma.

Más información:
- https://www.jmix.io/
- https://docs.jmix.io/
- https://github.com/jmix-framework/jmix

---

> ¿Por qué Jmix es bueno para construir sistemas CRM?

Los sistemas CRM se han convertido en la columna vertebral de la automatización empresarial moderna, mucho más allá de un simple sistema de registros. Como los requisitos de negocio en ventas cambian rápidamente, los sistemas CRM también deben ofrecer la capacidad de implementar cambios rápidos en los flujos de trabajo, el modelo de datos y la UX, preservando altos estándares de seguridad y cumplimiento.
Jmix ofrece estas capacidades de fábrica, permitiendo a los desarrolladores centrarse en la lógica de negocio en lugar de la infraestructura. Esta demo muestra cómo se pueden desarrollar aplicaciones empresariales listas para producción usando Jmix e IA.

---

> ¿Es una aplicación real o solo una demo?

B2B CRM es una aplicación demo diseñada para demostrar una arquitectura lista para producción y prácticas de desarrollo empresarial.
Incluye escenarios de negocio reales, UI moderna, capacidades de IA, seguridad, informes y patrones de integración que puedes reutilizar en tus propios proyectos empresariales.
