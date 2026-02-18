package com.example.todoApp.service;

import com.example.todoApp.models.Todo;
import com.example.todoApp.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

//bean
@Service
public class TodoService {
    @Autowired
    private TodoRepository todoRepository;

    public Todo createTodo(Todo todo){
        return todoRepository.save(todo);
    }

    public Todo getTodoById(Long id){
        return todoRepository.findById(id).orElseThrow(() -> new RuntimeException("ToDo not found!"));
    }

    public Page<Todo> getAllTodosPages(int page, int size){
        Pageable pageable = PageRequest.of(page,size);
        return todoRepository.findAll(pageable);
    }


    public List<Todo> getTodos() {
        return todoRepository.findAll();
    }

    public Todo updateTodo(Todo todo){
        return todoRepository.save(todo);
    }

    public void deleteTodoById(Long id){
        todoRepository.delete(getTodoById(id));
    }

    public void deleteTodo(Todo todo){
        todoRepository.delete(todo);
    }
}
