package org.john.interpreter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication()
public class InterpreterApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterpreterApplication.class, args);
    }

}
