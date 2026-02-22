package com.example.todoApp.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import lombok.Data;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Data
@Entity

public class Todo {
    @Id
    @GeneratedValue

    Long id;
    //@NotNull
    @NotBlank
    @Schema(name = "Title", example= "Complete Spring Boot")
    String title;
    //@NotNull

    @NotNull
    Boolean isCompleted;




}
