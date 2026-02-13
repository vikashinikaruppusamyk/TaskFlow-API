package com.example.myname;

import com.example.myname.models.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

//CRUD - Create Read Update Delete
public interface TodoRepository extends JpaRepository<Todo, Long> {

}
