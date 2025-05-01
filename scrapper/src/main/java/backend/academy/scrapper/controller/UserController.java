package backend.academy.scrapper.controller;

import backend.academy.scrapper.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users/{chatId}")
    public void registerChat(@PathVariable long chatId) {
        userService.registerUser(chatId);
    }

}
