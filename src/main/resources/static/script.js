// Shared script for the login, register and tasks pages of TaskFlow
// Served by the TaskFlow API itself, so the API is on the same origin
const API_BASE = "/api/v1";
const TOKEN_KEY = "accessToken";

const STATUS_LABELS = {
    TODO: "To do",
    IN_PROGRESS: "In progress",
    COMPLETED: "Completed"
};

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function goToLogin() {
    localStorage.removeItem(TOKEN_KEY);
    window.location.href = "login.html";
}

// Builds a readable message from the API's problem+json error body
async function errorMessage(response, fallback) {
    try {
        const problem = await response.json();
        if (problem.errors) {
            const fields = Object.entries(problem.errors).map(([field, message]) => `${field}: ${message}`);
            return `${problem.detail || fallback}\n${fields.join("\n")}`;
        }
        return problem.detail || fallback;
    } catch {
        return fallback;
    }
}

// fetch() with the access token attached; an expired or invalid token sends the user back to login
async function apiFetch(path, options = {}, fallbackError = "Request failed") {
    const headers = { ...(options.headers || {}) };
    const token = getToken();
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }
    if (options.body) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (response.status === 401 && token) {
        alert(await errorMessage(response, "Your session has ended. Please log in again."));
        goToLogin();
        throw new Error("Unauthorized");
    }
    if (!response.ok) {
        throw new Error(await errorMessage(response, fallbackError));
    }
    return response;
}

function showError(error) {
    if (error.message !== "Unauthorized") {
        alert(error.message);
    }
}

// ---------------------------------------------------------------- Login / register

async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    try {
        const response = await apiFetch("/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        }, "Login failed");
        const data = await response.json();
        localStorage.setItem(TOKEN_KEY, data.accessToken);
        window.location.href = "tasks.html";
    } catch (error) {
        showError(error);
    }
}

async function register() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    try {
        await apiFetch("/auth/register", {
            method: "POST",
            body: JSON.stringify({ email, password })
        }, "Registration failed");
        alert("Registration successful! Please log in.");
        window.location.href = "login.html";
    } catch (error) {
        showError(error);
    }
}

function logout() {
    goToLogin();
}

// ---------------------------------------------------------------- Tasks

function createTaskCard(task) {
    const card = document.createElement("div");
    card.className = "task-card";
    if (task.status === "COMPLETED") {
        card.classList.add("completed");
    }

    const title = document.createElement("span");
    title.className = "task-title";
    title.textContent = task.title;

    const statusSelect = document.createElement("select");
    statusSelect.className = "task-status";
    statusSelect.setAttribute("aria-label", `Status of ${task.title}`);
    for (const [value, label] of Object.entries(STATUS_LABELS)) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        option.selected = value === task.status;
        statusSelect.appendChild(option);
    }
    statusSelect.addEventListener("change", () => changeStatus(task.id, statusSelect.value));

    const deleteBtn = document.createElement("button");
    deleteBtn.className = "delete-btn";
    deleteBtn.textContent = "X";
    deleteBtn.setAttribute("aria-label", `Delete ${task.title}`);
    deleteBtn.addEventListener("click", () => deleteTask(task.id));

    card.append(title, statusSelect, deleteBtn);
    return card;
}

async function loadTasks() {
    const taskList = document.getElementById("task-list");
    try {
        const response = await apiFetch("/tasks?size=100", {}, "Failed to load tasks");
        const page = await response.json();

        taskList.innerHTML = "";
        if (page.content.length === 0) {
            taskList.innerHTML = '<p id="empty-message">No tasks yet. Add your first task!</p>';
        } else {
            page.content.forEach(task => taskList.appendChild(createTaskCard(task)));
        }
    } catch (error) {
        showError(error);
        taskList.innerHTML = '<p id="empty-message">Failed to load tasks.</p>';
    }
    loadInsights();
}

async function addTask() {
    const input = document.getElementById("new-task");
    const title = input.value.trim();
    if (!title) {
        alert("Please enter a task");
        return;
    }

    try {
        await apiFetch("/tasks", {
            method: "POST",
            body: JSON.stringify({ title })
        }, "Failed to add task");
        input.value = "";
        loadTasks();
    } catch (error) {
        showError(error);
    }
}

async function changeStatus(id, status) {
    try {
        await apiFetch(`/tasks/${id}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status })
        }, "Failed to update task");
    } catch (error) {
        showError(error);
    }
    loadTasks();
}

async function deleteTask(id) {
    try {
        await apiFetch(`/tasks/${id}`, { method: "DELETE" }, "Failed to delete task");
    } catch (error) {
        showError(error);
    }
    loadTasks();
}

// ---------------------------------------------------------------- Process insights

function formatDuration(seconds) {
    if (seconds === null || seconds === undefined) {
        return "–";
    }
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    if (minutes > 0) return `${minutes}m`;
    return `${seconds}s`;
}

async function loadInsights() {
    const insights = document.getElementById("insights");
    if (!insights) {
        return;
    }
    try {
        const [summary, cycleTime, variants] = await Promise.all([
            apiFetch("/analytics/summary").then(r => r.json()),
            apiFetch("/analytics/cycle-time").then(r => r.json()),
            apiFetch("/analytics/variants").then(r => r.json())
        ]);

        document.getElementById("stat-cases").textContent = summary.totalCases;
        document.getElementById("stat-open").textContent = summary.openCases;
        document.getElementById("stat-cycle").textContent = formatDuration(cycleTime.averageSeconds);
        document.getElementById("stat-rework").textContent = `${summary.reworkRate}%`;

        const list = document.getElementById("variant-list");
        list.innerHTML = "";
        if (variants.length === 0) {
            list.innerHTML = "<li>No activity yet.</li>";
        }
        variants.slice(0, 3).forEach(variant => {
            const item = document.createElement("li");
            item.textContent = `${variant.activities.join(" → ")}  (${variant.caseCount} × ${variant.percentage}%)`;
            list.appendChild(item);
        });
    } catch (error) {
        showError(error);
    }
}

async function exportEventLog() {
    try {
        const response = await apiFetch("/analytics/event-log/export", {}, "Export failed");
        const url = URL.createObjectURL(await response.blob());
        const link = document.createElement("a");
        link.href = url;
        link.download = "taskflow-event-log.csv";
        link.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        showError(error);
    }
}

// ---------------------------------------------------------------- Page setup

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("task-list")) {
        if (!getToken()) {
            goToLogin();
            return;
        }
        document.getElementById("new-task").addEventListener("keydown", event => {
            if (event.key === "Enter") addTask();
        });
        loadTasks();
    }
});
