# LLM Domain Insights - Example Questions

Based on the B2B CRM domain model, here are example questions that can be answered using LLM-based insights with structured domain data:

## 5 Example Questions for LLM Domain Insights

### 1. **Client Performance Analysis**
*"Which clients have the highest order volumes this quarter, and what are their most frequently purchased product categories?"*

**JPQL Query:**
```sql
-- Client order volumes this quarter
SELECT c.name, COUNT(o), SUM(o.total), cat.name as categoryName, COUNT(oi.categoryItem)
FROM Client c
LEFT JOIN c.orders o
LEFT JOIN o.orderItems oi
LEFT JOIN oi.categoryItem ci
LEFT JOIN ci.category cat
WHERE o.date >= :quarterStart AND o.date <= :quarterEnd
GROUP BY c.id, cat.id
ORDER BY COUNT(o) DESC, COUNT(oi.categoryItem) DESC
```

### 2. **Product Category Trends**
*"Which product categories are showing declining sales trends over the past 6 months, and what might be the potential reasons?"*

**JPQL Query:**
```sql
-- Category sales trends over 6 months
SELECT cat.name,
       MONTH(o.date) as orderMonth,
       COUNT(oi) as itemsSold,
       SUM(oi.netPrice * oi.quantity) as totalRevenue
FROM Category cat
LEFT JOIN cat.categoryItems ci
LEFT JOIN OrderItem oi ON oi.categoryItem = ci
LEFT JOIN oi.order o
WHERE o.date >= :sixMonthsAgo
GROUP BY cat.id, MONTH(o.date)
ORDER BY cat.name, orderMonth
```

### 3. **Revenue & Payment Patterns**
*"What are the payment behavior patterns of our top 10 clients, and which ones have concerning overdue invoice trends?"*

**JPQL Query:**
```sql
-- Top clients payment patterns and overdue invoices
SELECT c.name,
       AVG(DATEDIFF(p.date, i.date)) as avgPaymentDays,
       SUM(i.total) as totalInvoiced,
       SUM(CASE WHEN i.dueDate < CURRENT_DATE AND i.status != 2 THEN i.total ELSE 0 END) as overdueAmount,
       COUNT(CASE WHEN i.dueDate < CURRENT_DATE AND i.status != 2 THEN 1 END) as overdueCount
FROM Client c
LEFT JOIN c.invoices i
LEFT JOIN i.payments p
GROUP BY c.id
HAVING SUM(i.total) > 0
ORDER BY totalInvoiced DESC
LIMIT 10
```

### 4. **Order Fulfillment Insights**
*"Which orders are stuck in specific statuses for unusually long periods, and what common characteristics do these delayed orders share?"*

**JPQL Query:**
```sql
-- Orders with long status durations
SELECT o.number, o.status, o.date, o.client.name, o.total,
       DATEDIFF(CURRENT_DATE, o.lastModifiedDate) as daysInCurrentStatus,
       COUNT(oi) as itemCount,
       o.client.type as clientType
FROM Order_ o
LEFT JOIN o.orderItems oi
WHERE o.status IN (1, 2, 3) -- Pending/In Progress statuses
  AND DATEDIFF(CURRENT_DATE, o.lastModifiedDate) > 14 -- More than 2 weeks
GROUP BY o.id
ORDER BY daysInCurrentStatus DESC
```

### 5. **Account Manager Performance**
*"How do the different account managers compare in terms of client retention, average order values, and invoice collection rates?"*

**JPQL Query:**
```sql
-- Account manager performance metrics
SELECT u.firstName, u.lastName,
       COUNT(DISTINCT c.id) as clientCount,
       AVG(o.total) as avgOrderValue,
       SUM(o.total) as totalOrders,
       SUM(p.amount) / SUM(i.total) * 100 as collectionRate,
       COUNT(DISTINCT CASE WHEN o.date >= :lastYear THEN c.id END) as activeClientsLastYear
FROM User u
LEFT JOIN u.clients c
LEFT JOIN c.orders o
LEFT JOIN c.invoices i
LEFT JOIN i.payments p
WHERE u.clients IS NOT EMPTY
GROUP BY u.id
ORDER BY totalOrders DESC
```

---

## Domain Model Context

These questions leverage the rich relationships in the CRM domain model:

- **Clients** with their orders, invoices, payments, contacts, and account managers
- **Orders** with their items, status progression, and financial details
- **Categories** and **CategoryItems** for product analysis
- **Invoices** and **Payments** for financial insights
- **Users** (account managers) for performance analysis

Each question would allow the LLM to analyze structured data from multiple related tables and generate natural-language business insights, similar to "Which categories performed best last month?" or "Which books are dropping in sales?" but tailored to the B2B CRM domain.

## Implementation Approach

The application would:
1. Assemble structured prompts with relevant domain data from the CRM entities
2. Include historical data, relationships, and business context
3. Let the LLM generate natural-language insights and recommendations
4. Present actionable business intelligence to users