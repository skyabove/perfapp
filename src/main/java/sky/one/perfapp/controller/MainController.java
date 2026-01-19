package sky.one.perfapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping
    public String getMessage() {
        return "Main controller is up";
    }

}
