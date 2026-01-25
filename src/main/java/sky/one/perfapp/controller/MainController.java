package sky.one.perfapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MainController {

    private final UserService userService;

    @GetMapping
    public String getMessage() {
        return "Main controller is up!";
    }

    @GetMapping("/users")
    public List<UserDto> getUsers() {
        return userService.getUsers();
    }

}
