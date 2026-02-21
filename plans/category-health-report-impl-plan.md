# Implementation Plan: Category Health & Vitality Score Report

Dieses Dokument beschreibt die technische Umsetzung des "Category Health & Vitality Score" Reports. Ziel ist es, eine komplexe Analyseebene einzuziehen, die über die UI manuell und über die KI automatisiert ausgewertet werden kann.

## 1. Übersicht
*   **Report Name:** Category Health & Vitality Score
*   **Code:** `category-health-report`
*   **Einstiegspunkt UI:** `CategoryListView` (Katalog -> Kategorien)
*   **Hauptkomponenten:**
    *   `CategoryAnalyticsService`: Rechenlogik für Metriken und Scoring.
    *   `CategoryHealthDataLoader`: Java-DataLoader für die Report-Engine.
    *   `category-health-report.html`: HTML/FreeMarker Template für die Darstellung.

## 2. Metriken & Scoring-Logik (0-100 Punkte)

Der Score wird pro Kategorie für einen wählbaren Zeitraum (Default: letzte 30 Tage) berechnet.

| Metrik | Gewichtung | Logik |
| :--- | :--- | :--- |
| **Umsatztrend (PoP)** | 35% | Vergleich Umsatz (Zeitraum) vs. Umsatz (Vorperiode). >10% Wachstum = volle Punktzahl. |
| **Zahlungsrisiko** | 25% | Anteil des Umsatzes in dieser Kategorie, der mit `OVERDUE` Invoices verknüpft ist. 0% Risiko = 100 Punkte. |
| **Kundenkonzentration** | 20% | Anteil der Top-3 Kunden am Kategorie-Umsatz. <30% Konzentration = hohe Stabilität (100 Pkt). |
| **Neukunden-Quote** | 20% | Umsatzanteil von Kunden, die vor < 90 Tagen angelegt wurden. >15% Quote = hohe Vitalität (100 Pkt). |

### Klassifizierung:
*   **STRONG (Score >= 75):** Kategorie wächst gesund, geringes Risiko.
*   **WATCH (Score 40-74):** Gemischte Signale, genauere Beobachtung nötig.
*   **AT_RISK (Score < 40):** Sinkender Umsatz, hohes Zahlungsrisiko oder kritische Abhängigkeit.

## 3. Implementierungsschritte

### Schritt 1: Datenmodell-Erweiterung (optional)
*   Erweiterung von `OrderStatus` um `CANCELLED (99)`, um Storno-Raten präziser messen zu können.

### Schritt 2: CategoryAnalyticsService
*   Methode `calculateCategoryHealth(Category category, LocalDateRange range)`
*   Berechnung der vier Teilmetriken über optimierte JPQL-Abfragen (Aggregationen).
*   Zusammenführung zum Composite Score.

### Schritt 3: Jmix Report Definition
*   Registrierung des Reports via `@ReportDef`.
*   Einsatz von `CategoryHealthDataLoader` (implementiert `ReportDataLoader`).
*   Parameter: `startDate`, `endDate` (Required).

### Schritt 4: UI Integration
*   Anpassung von `CategoryListView`:
    *   Hinzufügen eines Buttons "Health Report" im `buttonsPanel`.
    *   Aufruf des `UiReportRunner` mit Selektion (optional) oder global für alle Kategorien.

### Schritt 5: AI Tooling
*   Registrierung des Codes in `CrmAnalyticsService.CRM_REPORTS`.
*   Der Report wird automatisch über `JmixReportDiscoveryTool` für das LLM sichtbar.

## 4. Testfälle

### A. Unit Tests (`CategoryAnalyticsServiceTest`)
*   `testScoreCalculation_StrongCategory`: Verifiziert, dass hohe Wachstumsraten und niedrige Risiken zu einem Score > 80 führen.
*   `testScoreCalculation_AtRiskCategory`: Verifiziert, dass hohe Kundenkonzentration (z.B. ein Kunde macht 90% Umsatz) den Score drastisch senkt.
*   `testPoPComparison`: Prüft die korrekte Berechnung der Vorperioden-Abgrenzung.

### B. Integration Tests (`CategoryHealthReportIntegrationTest`)
*   `testReportGeneration`: Erzeugt den Report technisch über den `ReportRunner` und prüft, ob das HTML-Dokument die erwarteten CSS-Klassen (`status-strong`, etc.) enthält.

### C. LLM / E2E Tests (`CrmAnalyticsServiceLLMTest`)
*   `testCategoryHealthQuestion`: "Welche unserer Top-Kategorien ist aktuell am riskantesten und warum?"
*   Erwartung: KI erkennt über das Tool den niedrigen Score und benennt die Ursache (z.B. "Hohes Zahlungsrisiko bei Kategorie Software").

## 5. Visualisierung (Template)
Das Template wird eine Matrix enthalten:
*   Header mit Gesamt-Durchschnitts-Score.
*   Tabelle aller Kategorien mit farbigen Badges (Grün/Gelb/Rot).
*   Mini-Charts (ASCII oder HTML-Balken) für die Trend-Visualisierung.
