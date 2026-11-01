
package org.qaspark.raceconditionlab.config;

import org.qaspark.raceconditionlab.entity.Product;
import org.qaspark.raceconditionlab.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(ProductRepository repository) {
        return args -> {

            System.out.println(">>> DATA INITIALIZER EXECUTED <<<");

            if (repository.findByProductId("P100").isEmpty()) {
                repository.save(new Product("P100", 1));
                System.out.println("P100 CREATED");
            } else {
                System.out.println("P100 ALREADY EXISTS");
            }
        };
    }
}
