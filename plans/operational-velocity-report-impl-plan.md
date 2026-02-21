# Implementation Plan: Operational Status Velocity & Bottleneck Report

Dieses Dokument beschreibt die technische Umsetzung des Prozess-Analyse-Reports für die Auftragsabwicklung.

## 1. Übersicht
*   **Report Name:** Operational Status Velocity & Bottleneck Report
*   **Code:** `order-velocity-report`
*   **Einstiegspunkt UI:** `OrderListView` (Vertrieb -> Aufträge)
*   **Hauptkomponenten:**
    *   `OrderProcessAnalyticsService`: Berechnung von Durchlaufzeiten und Engpässen.
    *   `OrderVelocityDataLoader`: Java-DataLoader für den Report.
    *   `order-velocity-report.html`: HTML-Template mit Prozess-Visualisierung.

## 2. Metriken & Analyse-Logik

Der Report misst die Zeit (in Stunden/Tagen) zwischen den Lebenszyklus-Phasen eines Auftrags, aggregiert pro Kategorie.

### Phasen:
1.  **Reaktionszeit:** `NEW` -> `ACCEPTED`
2.  **Bearbeitungszeit:** `ACCEPTED` -> `DONE`
3.  **Gesamt-Lead-Time:** `Order.createdDate` -> `Order.status == DONE`

### Bottleneck-Erkennung:
Eine Phase wird als **Bottleneck** markiert, wenn:
*   Die durchschnittliche Dauer in einer Kategorie > 120% des globalen Durchschnitts aller Kategorien liegt.
*   Die Standardabweichung der Dauer hoch ist (instabiler Prozess).

## 3. Implementierungsschritte

### Schritt 1: Prozess-Daten-Erfassung
*   Da Jmix Audit (`EntityLog`) genutzt wird, greift der Service auf die `entity_log` Tabelle zu, um die Zeitstempel der Statusänderungen für `Order.status` zu extrahieren.
*   *Alternative:* Einführung von `statusChangedDate` Feldern in `Order`, falls der Zugriff auf das Audit-Log für den Report zu langsam ist.

### Schritt 2: OrderProcessAnalyticsService
*   Methode `getCategoryVelocityMetrics(LocalDateRange range)`
*   Gruppierung der Aufträge nach Kategorie.
*   Berechnung der durchschnittlichen Verweildauer pro Statusübergang.
*   Berechnung des globalen Benchmarks zum Vergleich.

### Schritt 3: UI Integration
*   Erweiterung der `OrderListView` um einen Button "Prozess-Analyse".
*   Möglichkeit, den Report nur für die aktuell gefilterten Aufträge in der Tabelle auszuführen.

## 4. Testfälle

### A. Unit Tests
*   `testVelocityCalculation`: Verifiziert, dass die Zeitdifferenzen zwischen Statusänderungen korrekt in Stunden umgerechnet werden.
*   `testBottleneckIdentification`: Prüft, ob eine Kategorie korrekt als "langsam" markiert wird, wenn sie über dem Benchmark liegt.

### B. LLM / E2E Tests
*   `testOrderBottleneckQuestion`: "Warum dauern Hardware-Bestellungen aktuell länger als Software-Bestellungen?"
*   Erwartung: Die KI identifiziert die spezifische Phase (z.B. "Die Phase ACCEPTED zu DONE dauert bei Hardware im Schnitt 5 Tage, bei Software nur 1 Tag").

## 5. Visualisierung
*   **Phasen-Diagramm:** Ein horizontales Balkendiagramm pro Kategorie, das die Dauer der einzelnen Phasen visualisiert.
*   **Farbkodierung:** Phasen, die den Benchmark überschreiten, werden rot markiert.
