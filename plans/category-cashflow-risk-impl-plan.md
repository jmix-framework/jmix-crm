# Implementation Plan: Category Cashflow Risk Allocation Report

Dieses Dokument beschreibt die technische Umsetzung des finanzorientierten Risiko-Reports auf Kategorie-Ebene.

## 1. Übersicht
*   **Report Name:** Category Cashflow Risk Allocation Report
*   **Code:** `category-cashflow-risk-report`
*   **Einstiegspunkt UI:** `InvoiceListView` (Finanzen -> Rechnungen)
*   **Hauptkomponenten:**
    *   `CashflowAnalyticsService`: Allokationslogik von Zahlungen auf Kategorien.
    *   `CategoryCashflowDataLoader`: Java-DataLoader für den Report.
    *   `category-cashflow-risk-report.html`: HTML-Template mit Risiko-Metriken.

## 2. Metriken & Allokations-Logik

Dieser Report löst das Problem, dass Zahlungen (`Payment`) auf Rechnungs-Ebene erfolgen, das Risiko aber pro Produktkategorie analysiert werden soll.

### Allokations-Algorithmus:
1.  **Zahlungs-Split:** Ein Zahlungseingang wird prozentual auf die `OrderItems` der verknüpften Rechnung verteilt.
2.  **Kategorie-Mapping:** Der Anteil jedes `OrderItem` wird seiner jeweiligen `Category` zugeschrieben.
3.  **Metrik 1: Days-to-Cash (DTC):** Durchschnittliche Zeit von Rechnungsstellung bis zum tatsächlichen Zahlungseingang pro Kategorie.
4.  **Metrik 2: Revenue at Risk (RaR):** Summe aller offenen Beträge von `OVERDUE` Invoices, gewichtet nach Kategorie-Anteil.

## 3. Implementierungsschritte

### Schritt 1: CashflowAnalyticsService
*   Komplexe Java-Logik zur Traversierung der Kette: `Category <- CategoryItem <- OrderItem <- Order <- Invoice <- Payment`.
*   Berechnung der gewichteten DTC und RaR Metriken.
*   Behandlung von Teilzahlungen durch proportionale Allokation.

### Schritt 2: Report-Struktur
*   DataSets für:
    *   `RiskByCategory`: Zusammenfassung pro Kategorie (DTC, RaR, Total Invoiced, Total Paid).
    *   `CriticalInvoices`: Liste der Top-10 Rechnungen, die das Risiko in den schlechtesten Kategorien treiben.

### Schritt 3: UI Integration
*   Erweiterung der `InvoiceListView` um einen Button "Cashflow-Analyse".
*   Direkter Absprung zur Risiko-Übersicht für Finanz-Controller.

## 4. Testfälle

### A. Unit Tests
*   `testPaymentAllocation`: Verifiziert, dass eine 500€ Zahlung auf eine 1000€ Rechnung (bestehend aus 2 Kategorien à 500€) korrekt als 250€ Cash-In pro Kategorie gewertet wird.
*   `testDaysToCashCalculation`: Prüft die korrekte Berechnung der Zeitspanne über mehrere Teilzahlungen hinweg.

### B. LLM / E2E Tests
*   `testCashflowRiskQuestion`: "In welcher Kategorie ist unser Kapital am längsten gebunden?"
*   Erwartung: KI nennt die Kategorie mit dem höchsten DTC-Wert und erklärt den Zusammenhang zu offenen Rechnungen.

## 5. Visualisierung
*   **Risiko-Heatmap:** Kategorien auf einer Matrix (X-Achse: Umsatz, Y-Achse: DTC).
*   **Waterfall-Chart:** Darstellung von Invoiced -> Paid -> At Risk pro Kategorie.
