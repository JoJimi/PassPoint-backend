package org.example.passpoint;

import org.springframework.boot.SpringApplication;

public class TestPassPointApplication {

    public static void main(String[] args) {
        SpringApplication.from(PassPointApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
