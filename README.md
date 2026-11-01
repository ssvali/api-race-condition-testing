# API Race Condition Testing Lab

A practical API testing project that demonstrates how race conditions can cause inventory overselling when multiple requests try to purchase the same limited resource at the same time.

This lab reproduces the defect, applies an atomic inventory reservation, and verifies the behavior through concurrent API tests.

## Why this project?

A purchase API may work correctly when requests are executed one at a time, but fail when multiple customers submit requests concurrently.

For example, when only one item is available:

- Two customers submit a purchase request at nearly the same time.
- Both requests may read the same initial inventory.
- Both may believe the item is available.
- Both may create a purchase, resulting in overselling.

This project demonstrates the scenario from an **API testing perspective**: validate not only HTTP responses, but also the final business and database state.

## Tech stack

- Java 17
- Spring Boot 3.5.5
- Spring Data JPA
- H2 in-memory database
- Maven
- TestNG
- Rest Assured

## What the project demonstrates

1. Reproducing an inventory race condition with concurrent API requests.
2. Sending requests concurrently using Java concurrency utilities.
3. Preventing overselling with an atomic conditional database update.
4. Validating HTTP responses and persisted database state.
5. Testing invalid quantities, missing products, and insufficient inventory.
6. Verifying transaction rollback when purchase creation fails.

## How the scenario works

The application starts with one available item:

| Product ID | Available quantity |
|---|---:|
| P100 | 1 |

The purchase endpoint is:

```http
POST /api/purchases
Content-Type: application/json
```

Example request:

```json
{
  "productId": "P100",
  "customerId": "C101",
  "quantity": 1
}
```

### Expected behavior

When two requests compete for the last available item:

| Validation | Expected result |
|---|---|
| Successful request | `201 Created` |
| Request rejected due to insufficient inventory | `409 Conflict` |
| Final inventory | `0` |
| Purchase records | `1` |

The exact response contract is defined by this sample application.

## Concurrency test results

The fixed implementation was tested with 10 concurrent purchase requests per iteration, repeated across 20 iterations.

```text
========== FINAL SUMMARY ==========

Iterations:      20
Total requests:  200
Total Created:   20
Total Conflicts: 180

===================================
```

Each iteration produced:

- 10 concurrent requests
- 1 successful purchase
- 9 rejected requests
- Final inventory of 0
- Exactly 1 purchase record

The integration test suite also verified the following scenarios:

| Scenario | Expected HTTP response |
|---|---:|
| Successful purchase | `201 Created` |
| Insufficient inventory | `409 Conflict` |
| Zero quantity | `400 Bad Request` |
| Negative quantity | `400 Bad Request` |
| Product does not exist | `404 Not Found` |

A failure-injection test also verifies that inventory reservation is rolled back if purchase creation fails due to a database constraint violation.

**Latest recorded test result:** 9 tests passed, 0 failures, 0 errors.

These results are from this local H2-based lab. They are not a production-scale performance benchmark or a guarantee across all database platforms and deployment configurations.

## How to run the project

### Prerequisites

Make sure you have:

- JDK 17
- Maven 3.8 or later
- Git

Check your Java and Maven installations:

```bash
java -version
mvn -version
```

### Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd <YOUR_REPOSITORY_DIRECTORY>
```

Replace the placeholders with your GitHub repository URL and directory name.

### Run the application

```bash
mvn spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

The project uses an H2 in-memory database. The sample product `P100` is initialized with an available quantity of 1 when the application starts.

### Execute the integration tests

Run the complete test class:

```bash
mvn clean test -Dtest=RaceConditionTest
```

Run only the rollback scenario:

```bash
mvn clean test -Dtest=RaceConditionTest#shouldRollbackInventoryWhenPurchaseCreationFails
```

The integration tests start the Spring Boot application using a random HTTP port. You do not need to start the application separately before running the tests.

## API testing ideas to explore

Use this project as a starting point for experimenting with:

- Two requests competing for one available item.
- Multiple concurrent requests against limited inventory.
- Repeated concurrency test execution.
- Validation of response codes and database state.
- Transaction rollback after a failed purchase operation.

For APIs that modify shared resources, do not rely only on response codes. Verify that the persisted business state is correct after concurrent operations.

## Project scope

This repository is a focused learning and demonstration lab for API race-condition testing.

It is not intended to be a production-ready e-commerce or inventory management system. It does not include production authentication, payment processing, distributed deployment, or performance benchmarking.

## Related article

**Race Condition Testing in APIs: What Happens When Two Requests Arrive at the Same Time?**

[Add the Medium article URL here.]

Part of the series: **API Testing — Beyond the Happy Path**

Real-world scenarios. Hidden defects. Practical testing strategies.

## Contributions

Feel free to fork the repository, run the tests, and experiment with additional concurrency scenarios.

If you find an issue or have an idea for another API testing scenario, open an issue or submit a pull request.

## License

Add a license before inviting reuse or redistribution. Until a license is added, the repository is not automatically open for unrestricted reuse.
