package hr.span.akademija.medtermin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MedTerminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedTerminApplication.class, args);
    }
}
