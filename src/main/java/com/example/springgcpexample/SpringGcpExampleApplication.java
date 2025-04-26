package com.example.springgcpexample;

import com.example.springgcpexample.config.DependencyVersionVerifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class SpringGcpExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGcpExampleApplication.class, args);
    }

    @Bean
    public CommandLineRunner verifyDependencies(final DependencyVersionVerifier verifier, final Environment environment) {      return args -> {
            DependencyVersionVerifier.VerificationResult result = verifier.verify();
            if (!result.isCompatible()) {
                System.err.println("ERROR: " + result.getError());
                System.err.println("ACTION: " + result.getAction());
                throw new IllegalStateException("Dependency incompatibility detected");
            } else {
                System.out.println("All dependencies are compatible.");
            }
        };
    }
}
