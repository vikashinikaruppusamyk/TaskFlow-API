package com.example.todoApp.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    @PostMapping("/register")
    public String registerUser(@RequestBody Map<String,String> body){

    }

    @PostMapping("/login")
    public String loginUser(@RequestBody Map<String,String> body){

    }
}
