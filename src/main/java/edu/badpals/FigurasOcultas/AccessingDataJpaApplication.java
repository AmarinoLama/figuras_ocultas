package edu.badpals.FigurasOcultas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccessingDataJpaApplication {

  private static final Logger log = LoggerFactory.getLogger(AccessingDataJpaApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(AccessingDataJpaApplication.class);
  }

  /*@Bean
  public CommandLineRunner demo(CustomerRepository edu.badpals.Validacionformulario.repository) {
    return (args) -> {
      // save a few customers
      edu.badpals.Validacionformulario.repository.save(new Customer("Jack", "Bauer"));
      edu.badpals.Validacionformulario.repository.save(new Customer("Chloe", "O'Brian"));
      edu.badpals.Validacionformulario.repository.save(new Customer("Kim", "Bauer"));
      edu.badpals.Validacionformulario.repository.save(new Customer("David", "Palmer"));
      edu.badpals.Validacionformulario.repository.save(new Customer("Michelle", "Dessler"));

      // fetch all customers
      log.info("Customers found with findAll():");
      log.info("-------------------------------");
      edu.badpals.Validacionformulario.repository.findAll().forEach(customer -> {
        log.info(customer.toString());
      });
      log.info("");

      // fetch an individual customer by ID
      Customer customer = edu.badpals.Validacionformulario.repository.findById(1L);
      log.info("Customer found with findById(1L):");
      log.info("--------------------------------");
      log.info(customer.toString());
      log.info("");

      // fetch customers by last name
      log.info("Customer found with findByLastName('Bauer'):");
      log.info("--------------------------------------------");
      edu.badpals.Validacionformulario.repository.findByLastName("Bauer").forEach(bauer -> {
        log.info(bauer.toString());
      });
      log.info("");
    };
  }*/

}