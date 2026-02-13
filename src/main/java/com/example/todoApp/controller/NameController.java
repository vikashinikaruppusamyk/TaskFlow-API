package com.example.todoApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class NameController {
        @GetMapping("/myname")
        String showName(){
            return "Vikashini<3";
        }
}
