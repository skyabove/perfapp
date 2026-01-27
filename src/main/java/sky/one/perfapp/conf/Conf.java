package sky.one.perfapp.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Conf {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
