package ecommerce.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user/")
public class UserController {

    @GetMapping
    public String userDashboard(){

        return "this is user dashboard";
    }
}
