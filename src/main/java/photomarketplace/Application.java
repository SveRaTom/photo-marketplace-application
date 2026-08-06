package photomarketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import photomarketplace.client.customoffer.CustomOfferClient;

@SpringBootApplication
@EnableFeignClients(basePackageClasses = CustomOfferClient.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
