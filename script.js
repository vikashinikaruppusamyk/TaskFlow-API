// Shared script for login, register, and todos pages
const SERVER_URL = "http://localhost:8081";
const token = localStorage.getItem("token");

// Login page logic
function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
   
    fetch(`${SERVER_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
    })
    .then(response => {
        if(!response.ok){
            throw new Error(data.message ||"Registration failed");
        }
        return response.json();
    })
    .then(data => {
        localStorage.setItem("token", data.token);
        window.location.href = "todos.html";
    })        
    .catch(error =>{
        alert(error.message);
    })
}

// Register page logic
function register() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    fetch(`${SERVER_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
    })
    .then(response => {
        if(!response.ok){
            return response.json().then(err => { 
                throw new Error(err.message || "Registration failed"); 
            });
        }
        return response.json();
    })
    .then(data => {
        alert("Registration Successful! Please login.");
        window.location.href = "login.html";
    })
    .catch(error => {
        alert(error.message);
    });
// ...existing code...

}

// Todos page logic
function createTodoCard(todo) {
    const card = document.createElement("div");
    card.className = "todo-card";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = todo.isCompleted;
    checkbox.addEventListener("change", function () {
        const updatedTodo = { ...todo, isCompleted: checkbox.checked };
        updateTodoStatus(updatedTodo);
    });
    const span = document.createElement("span");
    span.textContent = todo.title;

    if(todo.isCompleted){
        span.style.textDecoration = "line-through";
        span.style.color = "#aaa";
    }

    const deleteBtn = document.createElement("button");
    deleteBtn.textContent = "X";
    deleteBtn.onclick = function () {
        deleteTodo(todo.id);
    };

    card.appendChild(checkbox);
    card.appendChild(span);
    card.appendChild(deleteBtn);

    return card;
}

function loadTodos() {
    if(!token){
        alert("Please login to view your todos.");
        window.location.href = "login.html";
        return;
    }

    fetch(`${SERVER_URL}/api/v1/todo`, {
        method: "GET",
        headers: { "Authorization": `Bearer ${token}` }
    })
    .then(response => {
        if(!response.ok){
            throw new Error("Failed to load todos");
        }
        return response.json();
    })
    .then(data => {
        const todoList = document.getElementById("todo-list");
        todoList.innerHTML = "";

        if(!data || data.length === 0){
            todoList.innerHTML = "<p>No todos found. Add your first todo!</p>";
        } else {
            data.forEach(todo => {
                const card = createTodoCard(todo);
                todoList.appendChild(card);
            });
        }
    })
    .catch(error => {
        alert(error.message);
        document.getElementById("todo-list").innerHTML = "<p>Failed to load todos.</p>";
    });
}


function addTodo() {
    const input = document.getElementById("new-todo");
    const title = input.value.trim();

    if (!title) {
        alert("Please enter a todo");
        return;
    }

    fetch(`${SERVER_URL}/api/v1/todo/create`,{
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ title, isCompleted: false })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || "Failed to add todo");
            });
        }
        return response.json();
    })
    .then(() => {
        input.value = "";
        loadTodos();
    })
    .catch(error => {
        alert(error.message);
    });
}


function updateTodoStatus(todo) {
    fetch(`${SERVER_URL}/api/v1/todo`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}` 
        },
        body: JSON.stringify(todo)
    })
    .then(response => {
        if(!response.ok){
            throw new Error(data.message || "Failed to update todo");
        }
        return response.json();
    })
    .then(() => {
        loadTodos();
    })
    .catch(error => {
        alert(error.message);
    }); 
}


function deleteTodo(id) {
    fetch(`${SERVER_URL}/api/v1/todo/${id}`, {
        method: "DELETE",
        headers: {"Authorization": `Bearer ${token}` },
    })
    .then(response => {
        if(!response.ok){
            throw new Error(data.message || "Failed to delete todo");
        }
        return response.text();
    })
    .then(() => {
        loadTodos();
    })
    .catch(error => {
        alert(error.message);
    }); 
}

// Page-specific initializations
document.addEventListener("DOMContentLoaded", function () {
    if (document.getElementById("todo-list")) {
        loadTodos();
    }
});