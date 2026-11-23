package kr.co.daeun.flowmatebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowMateBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowMateBeApplication.class, args);
    }

}
