
package org.qaspark.raceconditionlab;

import org.qaspark.raceconditionlab.entity.Product;
import org.qaspark.raceconditionlab.repository.ProductRepository;
import org.qaspark.raceconditionlab.repository.PurchaseRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.qaspark.raceconditionlab.service.PurchaseService;
import org.springframework.dao.DataIntegrityViolationException;

import static io.restassured.RestAssured.given;

@SpringBootTest(
        classes = RaceConditionLabApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class RaceConditionTest
        extends AbstractTestNGSpringContextTests {

    @LocalServerPort
    private int port;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @BeforeMethod
    public void resetTestData() {

        purchaseRepository.deleteAll();
        productRepository.deleteAll();

        productRepository.save(new Product("P100", 1));

        RestAssured.port = port;
    }

    @Test
    public void verifyInventoryAcrossTwentyConcurrentIterations()
            throws Exception {

        int iterations = 20;
        int requestsPerIteration = 10;

        ExecutorService executor =
                Executors.newFixedThreadPool(requestsPerIteration);

        int totalCreated = 0;
        int totalConflicts = 0;

        try {

            for (int iteration = 1;
                 iteration <= iterations;
                 iteration++) {

                // Reset database before each iteration
                purchaseRepository.deleteAll();

                Product product = productRepository
                        .findByProductId("P100")
                        .orElseThrow();

                product.setAvailableQuantity(1);
                productRepository.save(product);

                CountDownLatch ready =
                        new CountDownLatch(requestsPerIteration);

                CountDownLatch start =
                        new CountDownLatch(1);

                List<Future<Response>> futures = new ArrayList<>();

                // Submit 10 concurrent requests
                for (int i = 1; i <= requestsPerIteration; i++) {

                    final String customerId =
                            "ITER" + iteration + "_C" + i;

                    Callable<Response> request = () -> {

                        ready.countDown();
                        start.await();

                        return given()
                                .contentType("application/json")
                                .body("""
                                {
                                  "productId": "P100",
                                  "customerId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(customerId))
                                .post("/api/purchases");
                    };

                    futures.add(executor.submit(request));
                }

                Assert.assertTrue(
                        ready.await(10, TimeUnit.SECONDS),
                        "Requests did not become ready"
                );

                // Release all 10 requests together
                start.countDown();

                int created = 0;
                int conflicts = 0;

                // Collect responses
                for (Future<Response> future : futures) {

                    Response response =
                            future.get(30, TimeUnit.SECONDS);

                    int statusCode = response.statusCode();

                    if (statusCode == 201) {
                        created++;
                    } else if (statusCode == 409) {
                        conflicts++;
                    } else {
                        Assert.fail(
                                "Unexpected HTTP status: " + statusCode
                        );
                    }
                }

                totalCreated += created;
                totalConflicts += conflicts;

                // Verify HTTP behavior for this iteration
                Assert.assertEquals(
                        created, 1,
                        "Iteration " + iteration +
                                ": exactly one purchase should succeed"
                );

                Assert.assertEquals(
                        conflicts, 9,
                        "Iteration " + iteration +
                                ": nine requests should be rejected"
                );

                // Verify committed database state
                Product finalProduct = productRepository
                        .findByProductId("P100")
                        .orElseThrow();

                long purchaseCount =
                        purchaseRepository.countByProductId("P100");

                long totalPurchasedQuantity =
                        purchaseRepository.findAll()
                                .stream()
                                .filter(p ->
                                        p.getProductId().equals("P100"))
                                .mapToLong(p -> p.getQuantity())
                                .sum();

                Assert.assertEquals(
                        finalProduct.getAvailableQuantity().intValue(),
                        0,
                        "Incorrect inventory in iteration " + iteration
                );

                Assert.assertEquals(
                        purchaseCount,
                        1L,
                        "Incorrect purchase count in iteration " + iteration
                );

                Assert.assertEquals(
                        totalPurchasedQuantity,
                        1L,
                        "Inventory invariant violated in iteration " + iteration
                );

                System.out.println(
                        "Iteration " + iteration +
                                " PASSED | Created: " + created +
                                " | Conflicts: " + conflicts +
                                " | Inventory: " +
                                finalProduct.getAvailableQuantity() +
                                " | Purchases: " + purchaseCount
                );
            }

            System.out.println("\n========== FINAL SUMMARY ==========");

            System.out.println("Iterations: " + iterations);
            System.out.println(
                    "Total requests: " +
                            (iterations * requestsPerIteration)
            );
            System.out.println("Total Created: " + totalCreated);
            System.out.println("Total Conflicts: " + totalConflicts);

            System.out.println("===================================");

            // Overall assertions
            Assert.assertEquals(totalCreated, 20);
            Assert.assertEquals(totalConflicts, 180);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldPurchaseWhenOneItemIsAvailable() {

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C201",
            "quantity": 1
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(201);

        Product product = productRepository.findByProductId("P100")
                .orElseThrow();

        Assert.assertEquals(product.getAvailableQuantity(), 0);
        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 1);
    }

    @Test
    public void shouldRejectPurchaseWhenQuantityExceedsStock() {

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C202",
            "quantity": 2
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(409);

        Product product = productRepository.findByProductId("P100")
                .orElseThrow();

        Assert.assertEquals(product.getAvailableQuantity(), 1);
        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 0);
    }

    @Test
    public void shouldRejectZeroQuantity() {

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C203",
            "quantity": 0
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(400);

        Assert.assertEquals(
                productRepository.findByProductId("P100")
                        .orElseThrow().getAvailableQuantity(), 1
        );

        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 0);
    }

    @Test
    public void shouldRejectNegativeQuantity() {

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C204",
            "quantity": -1
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(400);

        Assert.assertEquals(
                productRepository.findByProductId("P100")
                        .orElseThrow().getAvailableQuantity(), 1
        );

        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 0);
    }

    @Test
    public void shouldRejectPurchaseWhenInventoryIsZero() {

        Product product = productRepository.findByProductId("P100")
                .orElseThrow();

        product.setAvailableQuantity(0);
        productRepository.save(product);

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C205",
            "quantity": 1
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(409);

        Assert.assertEquals(
                productRepository.findByProductId("P100")
                        .orElseThrow().getAvailableQuantity(), 0
        );

        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 0);
    }

    @Test
    public void shouldReturn404WhenProductDoesNotExist() {

        String requestBody = """
        {
            "productId": "INVALID_PRODUCT",
            "customerId": "C301",
            "quantity": 1
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(404);

        Assert.assertEquals(purchaseRepository.count(), 0);
    }

    @Test
    public void shouldReturn409WhenInventoryIsInsufficient() {

        String requestBody = """
        {
            "productId": "P100",
            "customerId": "C302",
            "quantity": 2
        }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/purchases")
                .then()
                .statusCode(409);

        Product product = productRepository.findByProductId("P100")
                .orElseThrow();

        Assert.assertEquals(product.getAvailableQuantity(), 1);
        Assert.assertEquals(purchaseRepository.countByProductId("P100"), 0);
    }

    @Test
    public void shouldRollbackInventoryWhenPurchaseCreationFails() {

        // Initial inventory is 1, configured by @BeforeMethod.

        try {
            // Deliberately pass null customerId.
            // Database NOT NULL constraint should reject the purchase.
            purchaseService.purchase("P100", null, 1);

            Assert.fail("Expected database constraint violation");

        } catch (DataIntegrityViolationException exception) {
            // Expected failure.
        }

        // Verify inventory reservation was rolled back.
        Product product = productRepository.findByProductId("P100")
                .orElseThrow();

        Assert.assertEquals(
                product.getAvailableQuantity(),
                1,
                "Inventory should be restored after rollback"
        );

        // Verify no purchase was created.
        Assert.assertEquals(
                purchaseRepository.countByProductId("P100"),
                0,
                "Failed purchase must not create a purchase record"
        );
    }

}
