package com.example.myname;

import com.example.myname.models.Todo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/todo")
public class TodoController {
    @Autowired
    private TodoService todoService;

       //Path variable
       @GetMapping("/{id}")
       ResponseEntity<Todo> getTodoById(@PathVariable long id){
        try{
            Todo createdTodo = todoService.getTodoById(id);
            return new ResponseEntity<>(createdTodo, HttpStatus.OK);
        } catch (RuntimeException exception) {
            return new ResponseEntity<>((HttpHeaders) null, HttpStatus.NOT_FOUND);
        }

    }

    @GetMapping
    ResponseEntity<List<Todo>> getTodos(){
           return new ResponseEntity<List<Todo>>(todoService.getTodos(),HttpStatus.OK);
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
