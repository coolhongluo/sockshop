package works.weave.socks.queuemaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.servicecomb.springboot.starter.provider.EnableServiceComb;

@SpringBootApplication
@EnableServiceComb
public class QueueMasterApplication {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(QueueMasterApplication.class, args);
    }
}
