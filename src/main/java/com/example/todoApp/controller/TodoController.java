package com.example.todoApp.controller;

import com.example.todoApp.service.TodoService;
import com.example.todoApp.models.Todo;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/todo")
@Slf4j
public class TodoController {
    @Autowired
    private TodoService todoService;

       //Path variable
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Todo was not found!")
    })
       @GetMapping("/{id}")
        ResponseEntity<Todo> getTodoById(@PathVariable long id){
        try{
            Todo createdTodo = todoService.getTodoById(id);
            return new ResponseEntity<>(createdTodo, HttpStatus.OK);
        } catch (RuntimeException exception) {
            log.info("Error");
            log.warn("");
            log.error("", exception);
            return new ResponseEntity<>((HttpHeaders) null, HttpStatus.NOT_FOUND);
        }

    }

    @GetMapping
    ResponseEntity<List<Todo>> getTodos(){
           return new ResponseEntity<List<Todo>>(todoService.getTodos(),HttpStatus.OK);
    }

    @GetMapping("/page")
    ResponseEntity<Page<Todo>> getTodosPaged(@RequestParam int page, @RequestParam int size){
           return new ResponseEntity<>(todoService.getAllTodosPages(page, size),HttpStatus.OK);
    }


       //RequestBody
    @PostMapping("/create")
    ResponseEntity<Todo> createUser(@RequestBody Todo todo){
        Todo createdTodo = todoService.createTodo(todo);
        return new ResponseEntity<>(createdTodo, HttpStatus.CREATED);
    }

    //PutMapping
    @PutMapping
    ResponseEntity<Todo> updateToDoById(@RequestBody Todo todo){
        return new ResponseEntity<>(todoService.updateTodo(todo), HttpStatus.OK);
    }

    //DeleteMapping
    @DeleteMapping("{id}")
    void deleteToDoById(@PathVariable long id){
        todoService.deleteTodoById(id);
    }

}
