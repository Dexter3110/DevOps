// Employee Skill Inventory — Dashboard
// Talks to the existing Spring Boot REST API at the same origin (/api/**).

const API = "/api";

const state = {
  employees: [],
  skills: [],
};

/* ---------------------------------------------------------- *
 * Small helpers
 * ---------------------------------------------------------- */

async function apiRequest(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body && body.message) message = body.message;
      else if (typeof body === "string") message = body;
      else if (body && body.errors) message = Object.values(body.errors).join(", ");
    } catch (_) {
      try { message = await res.text() || message; } catch (_) {}
    }
    throw new Error(message);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

function toast(message, type = "success") {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.className = `toast ${type}`;
  el.classList.remove("hidden");
  clearTimeout(toast._t);
  toast._t = setTimeout(() => el.classList.add("hidden"), 3200);
}

function fmtDate(value) {
  if (!value) return "—";
  return value; // backend sends ISO yyyy-MM-dd, fine to display as-is
}

function employeeName(e) {
  if (!e) return "Unknown";
  return `${e.firstName ?? ""} ${e.lastName ?? ""}`.trim() || "Unknown";
}

function daysUntil(dateStr) {
  if (!dateStr) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr);
  return Math.round((target - today) / (1000 * 60 * 60 * 24));
}

/* ---------------------------------------------------------- *
 * Navigation
 * ---------------------------------------------------------- */

const VIEW_TITLES = {
  dashboard: "Dashboard",
  employees: "Employees",
  skills: "Skill Catalogue",
  assignments: "Skill Assignments",
  search: "Search",
  alerts: "Exception Alerts",
};

function showView(name) {
  document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
  document.getElementById(`view-${name}`).classList.add("active");
  document.querySelectorAll(".nav-link").forEach((b) => b.classList.toggle("active", b.dataset.view === name));
  document.getElementById("viewTitle").textContent = VIEW_TITLES[name] || name;

  if (name === "dashboard") loadDashboard();
  if (name === "employees") loadEmployees();
  if (name === "skills") loadSkills();
  if (name === "assignments") loadAssignments();
}

document.querySelectorAll(".nav-link").forEach((btn) => {
  btn.addEventListener("click", () => showView(btn.dataset.view));
});

document.getElementById("refreshBtn").addEventListener("click", () => {
  const active = document.querySelector(".nav-link.active").dataset.view;
  showView(active);
});

/* ---------------------------------------------------------- *
 * API health check + environment badge
 * ---------------------------------------------------------- */

async function checkApiHealth() {
  const dot = document.getElementById("apiStatusDot");
  const text = document.getElementById("apiStatusText");
  try {
    await apiRequest("/skills");
    dot.className = "status-dot ok";
    text.textContent = "API connected";
  } catch (e) {
    dot.className = "status-dot err";
    text.textContent = "API unreachable";
  }
}

// Asks the running instance which environment it was started as
// (esi.environment, set per Spring profile — see Week 8 Jenkinsfile).
async function showEnvBadge() {
  try {
    const info = await apiRequest("/env");
    document.getElementById("envBadge").textContent = `ENV: ${(info.environment || "local").toUpperCase()} · PORT ${info.port}`;
  } catch (e) {
    document.getElementById("envBadge").textContent = "ENV: UNKNOWN";
  }
}

/* ---------------------------------------------------------- *
 * DASHBOARD
 * ---------------------------------------------------------- */

async function loadDashboard() {
  try {
    const [employees, skills, assignments, alertSummary] = await Promise.all([
      apiRequest("/employees"),
      apiRequest("/skills"),
      apiRequest("/employee-skills"),
      apiRequest("/alerts/summary"),
    ]);

    state.employees = employees;
    state.skills = skills;

    document.getElementById("statEmployees").textContent = employees.length;
    document.getElementById("statSkills").textContent = skills.length;
    document.getElementById("statAssignments").textContent = assignments.length;
    document.getElementById("statExpiring").textContent = alertSummary.expiringWithin30DaysCount ?? 0;

    renderCategoryBars(skills);
    renderProficiencyBars(assignments);
    renderDashboardAlerts(alertSummary.expiringCertifications || []);
  } catch (e) {
    toast(`Could not load dashboard: ${e.message}`, "error");
  }
}

function renderCategoryBars(skills) {
  const counts = {};
  skills.forEach((s) => { counts[s.category || "Uncategorised"] = (counts[s.category || "Uncategorised"] || 0) + 1; });
  renderBars("categoryBars", counts);
}

function renderProficiencyBars(assignments) {
  const order = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"];
  const counts = { BEGINNER: 0, INTERMEDIATE: 0, ADVANCED: 0, EXPERT: 0 };
  assignments.forEach((a) => {
    const key = (a.proficiencyLevel || "").toUpperCase();
    if (counts[key] !== undefined) counts[key]++;
  });
  renderBars("proficiencyBars", counts, order);
}

function renderBars(containerId, counts, order) {
  const container = document.getElementById(containerId);
  const keys = order || Object.keys(counts);
  const max = Math.max(1, ...Object.values(counts));
  container.innerHTML = keys.map((k) => {
    const v = counts[k] || 0;
    const pct = Math.round((v / max) * 100);
    return `
      <div class="bar-row">
        <span class="bar-label">${k}</span>
        <span class="bar-track"><span class="bar-fill" style="width:${pct}%"></span></span>
        <span class="bar-count">${v}</span>
      </div>`;
  }).join("") || `<p class="field-error" style="color:var(--text-muted)">No data yet.</p>`;
}

function renderDashboardAlerts(list) {
  const tbody = document.querySelector("#dashboardAlertsTable tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="4">No certifications expiring soon.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.slice(0, 6).map((a) => `
    <tr>
      <td>${employeeName(a.employee)}</td>
      <td>${a.skill ? a.skill.name : "—"}</td>
      <td>${a.certificationName || "—"}</td>
      <td>${fmtDate(a.expiryDate)}</td>
    </tr>`).join("");
}

/* ---------------------------------------------------------- *
 * EMPLOYEES
 * ---------------------------------------------------------- */

async function loadEmployees() {
  try {
    state.employees = await apiRequest("/employees");
    renderEmployeesTable(state.employees);
  } catch (e) {
    toast(`Could not load employees: ${e.message}`, "error");
  }
}

function renderEmployeesTable(list) {
  const tbody = document.querySelector("#employeesTable tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="6">No employees yet. Add the first one.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map((e) => `
    <tr>
      <td>${employeeName(e)}</td>
      <td>${e.email}</td>
      <td>${e.department}</td>
      <td>${e.designation}</td>
      <td>${fmtDate(e.hireDate)}</td>
      <td>
        <button class="btn btn-ghost btn-sm" onclick="openEmployeeModal(${e.id})">Edit</button>
        <button class="btn btn-danger btn-sm" onclick="deleteEmployee(${e.id})">Delete</button>
      </td>
    </tr>`).join("");
}

function openEmployeeModal(id) {
  const existing = id ? state.employees.find((e) => e.id === id) : null;
  openModal(existing ? "Edit Employee" : "Add Employee", `
    <form id="employeeForm" class="form-grid">
      <div class="form-field">
        <label>First name</label>
        <input name="firstName" required value="${existing ? existing.firstName : ""}" />
      </div>
      <div class="form-field">
        <label>Last name</label>
        <input name="lastName" required value="${existing ? existing.lastName : ""}" />
      </div>
      <div class="form-field full">
        <label>Email</label>
        <input type="email" name="email" required value="${existing ? existing.email : ""}" />
      </div>
      <div class="form-field">
        <label>Department</label>
        <input name="department" required value="${existing ? existing.department : ""}" />
      </div>
      <div class="form-field">
        <label>Designation</label>
        <input name="designation" required value="${existing ? existing.designation : ""}" />
      </div>
      <div class="form-field">
        <label>Hire date</label>
        <input type="date" name="hireDate" value="${existing && existing.hireDate ? existing.hireDate : ""}" />
      </div>
      <div class="form-field"></div>
      <div class="field-error full" id="employeeFormError"></div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" onclick="closeModal()">Cancel</button>
        <button type="submit" class="btn btn-primary">${existing ? "Save changes" : "Add employee"}</button>
      </div>
    </form>
  `);

  document.getElementById("employeeForm").addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const data = Object.fromEntries(new FormData(evt.target).entries());
    if (!data.hireDate) delete data.hireDate;
    try {
      if (existing) {
        await apiRequest(`/employees/${existing.id}`, { method: "PUT", body: JSON.stringify(data) });
        toast("Employee updated");
      } else {
        await apiRequest("/employees", { method: "POST", body: JSON.stringify(data) });
        toast("Employee added");
      }
      closeModal();
      loadEmployees();
    } catch (e) {
      document.getElementById("employeeFormError").textContent = e.message;
    }
  });
}

async function deleteEmployee(id) {
  if (!confirm("Delete this employee and all of their skill records?")) return;
  try {
    await apiRequest(`/employees/${id}`, { method: "DELETE" });
    toast("Employee deleted");
    loadEmployees();
  } catch (e) {
    toast(`Could not delete: ${e.message}`, "error");
  }
}

document.getElementById("addEmployeeBtn").addEventListener("click", () => openEmployeeModal(null));

/* ---------------------------------------------------------- *
 * SKILLS
 * ---------------------------------------------------------- */

async function loadSkills() {
  try {
    state.skills = await apiRequest("/skills");
    renderSkillsTable(state.skills);
  } catch (e) {
    toast(`Could not load skills: ${e.message}`, "error");
  }
}

function renderSkillsTable(list) {
  const tbody = document.querySelector("#skillsTable tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="5">No skills in the catalogue yet.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map((s) => `
    <tr>
      <td>${s.name}</td>
      <td>${s.category}</td>
      <td>${s.description || "—"}</td>
      <td><span class="pill ${s.status === "ACTIVE" ? "pill-active" : "pill-inactive"}">${s.status}</span></td>
      <td>
        <button class="btn btn-ghost btn-sm" onclick="openSkillModal(${s.id})">Edit</button>
        <button class="btn btn-danger btn-sm" onclick="deleteSkill(${s.id})">Delete</button>
      </td>
    </tr>`).join("");
}

function openSkillModal(id) {
  const existing = id ? state.skills.find((s) => s.id === id) : null;
  openModal(existing ? "Edit Skill" : "Add Skill", `
    <form id="skillForm" class="form-grid">
      <div class="form-field full">
        <label>Skill name</label>
        <input name="name" required value="${existing ? existing.name : ""}" />
      </div>
      <div class="form-field">
        <label>Category</label>
        <input name="category" required value="${existing ? existing.category : ""}" />
      </div>
      <div class="form-field">
        <label>Status</label>
        <select name="status">
          <option value="ACTIVE" ${existing && existing.status === "ACTIVE" ? "selected" : ""}>Active</option>
          <option value="INACTIVE" ${existing && existing.status === "INACTIVE" ? "selected" : ""}>Inactive</option>
        </select>
      </div>
      <div class="form-field full">
        <label>Description</label>
        <textarea name="description" rows="3">${existing && existing.description ? existing.description : ""}</textarea>
      </div>
      <div class="field-error full" id="skillFormError"></div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" onclick="closeModal()">Cancel</button>
        <button type="submit" class="btn btn-primary">${existing ? "Save changes" : "Add skill"}</button>
      </div>
    </form>
  `);

  document.getElementById("skillForm").addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const data = Object.fromEntries(new FormData(evt.target).entries());
    try {
      if (existing) {
        await apiRequest(`/skills/${existing.id}`, { method: "PUT", body: JSON.stringify(data) });
        toast("Skill updated");
      } else {
        await apiRequest("/skills", { method: "POST", body: JSON.stringify(data) });
        toast("Skill added");
      }
      closeModal();
      loadSkills();
    } catch (e) {
      document.getElementById("skillFormError").textContent = e.message;
    }
  });
}

async function deleteSkill(id) {
  if (!confirm("Delete this skill from the catalogue?")) return;
  try {
    await apiRequest(`/skills/${id}`, { method: "DELETE" });
    toast("Skill deleted");
    loadSkills();
  } catch (e) {
    toast(`Could not delete: ${e.message}`, "error");
  }
}

document.getElementById("addSkillBtn").addEventListener("click", () => openSkillModal(null));

/* ---------------------------------------------------------- *
 * ASSIGNMENTS (employee-skill records)
 * ---------------------------------------------------------- */

async function loadAssignments() {
  try {
    const [assignments, employees, skills] = await Promise.all([
      apiRequest("/employee-skills"),
      apiRequest("/employees"),
      apiRequest("/skills"),
    ]);
    state.employees = employees;
    state.skills = skills;
    renderAssignmentsTable(assignments);
  } catch (e) {
    toast(`Could not load assignments: ${e.message}`, "error");
  }
}

function renderAssignmentsTable(list) {
  const tbody = document.querySelector("#assignmentsTable tbody");
  if (!list.length) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="8">No skills assigned to employees yet.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map((a) => `
    <tr>
      <td>${employeeName(a.employee)}</td>
      <td>${a.skill ? a.skill.name : "—"}</td>
      <td>${a.proficiencyLevel}</td>
      <td>${a.yearsOfExperience ?? 0}</td>
      <td>${a.certificationName || "—"}</td>
      <td>${fmtDate(a.expiryDate)}</td>
      <td><span class="pill ${a.verified ? "pill-verified" : "pill-unverified"}">${a.verified ? "Verified" : "Pending"}</span></td>
      <td><button class="btn btn-danger btn-sm" onclick="deleteAssignment(${a.id})">Remove</button></td>
    </tr>`).join("");
}

async function ensureRefLists() {
  if (!state.employees.length) state.employees = await apiRequest("/employees");
  if (!state.skills.length) state.skills = await apiRequest("/skills");
}

async function openAssignmentModal() {
  await ensureRefLists();
  if (!state.employees.length || !state.skills.length) {
    toast("Add at least one employee and one skill first", "error");
    return;
  }
  openModal("Assign Skill to Employee", `
    <form id="assignmentForm" class="form-grid">
      <div class="form-field full">
        <label>Employee</label>
        <select name="employeeId" required>
          ${state.employees.map((e) => `<option value="${e.id}">${employeeName(e)} — ${e.department}</option>`).join("")}
        </select>
      </div>
      <div class="form-field full">
        <label>Skill</label>
        <select name="skillId" required>
          ${state.skills.map((s) => `<option value="${s.id}">${s.name} (${s.category})</option>`).join("")}
        </select>
      </div>
      <div class="form-field">
        <label>Proficiency level</label>
        <select name="proficiencyLevel" required>
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
          <option value="EXPERT">Expert</option>
        </select>
      </div>
      <div class="form-field">
        <label>Years of experience</label>
        <input type="number" name="yearsOfExperience" min="0" value="0" />
      </div>
      <div class="form-field">
        <label>Certification name</label>
        <input name="certificationName" placeholder="Optional" />
      </div>
      <div class="form-field">
        <label>Certification expiry</label>
        <input type="date" name="expiryDate" />
      </div>
      <div class="form-field checkbox full">
        <input type="checkbox" name="verified" id="verifiedCheck" />
        <label for="verifiedCheck">Verified by HR / Manager</label>
      </div>
      <div class="field-error full" id="assignmentFormError"></div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" onclick="closeModal()">Cancel</button>
        <button type="submit" class="btn btn-primary">Assign skill</button>
      </div>
    </form>
  `);

  document.getElementById("assignmentForm").addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const form = evt.target;
    const fd = new FormData(form);
    const employeeId = fd.get("employeeId");
    const skillId = fd.get("skillId");
    const body = {
      proficiencyLevel: fd.get("proficiencyLevel"),
      yearsOfExperience: Number(fd.get("yearsOfExperience") || 0),
      certificationName: fd.get("certificationName") || null,
      expiryDate: fd.get("expiryDate") || null,
      verified: form.querySelector("#verifiedCheck").checked,
    };
    try {
      await apiRequest(`/employee-skills/assign?employeeId=${employeeId}&skillId=${skillId}`, {
        method: "POST",
        body: JSON.stringify(body),
      });
      toast("Skill assigned");
      closeModal();
      loadAssignments();
    } catch (e) {
      document.getElementById("assignmentFormError").textContent = e.message;
    }
  });
}

async function deleteAssignment(id) {
  if (!confirm("Remove this skill assignment?")) return;
  try {
    await apiRequest(`/employee-skills/${id}`, { method: "DELETE" });
    toast("Assignment removed");
    loadAssignments();
  } catch (e) {
    toast(`Could not remove: ${e.message}`, "error");
  }
}

document.getElementById("addAssignmentBtn").addEventListener("click", openAssignmentModal);

/* ---------------------------------------------------------- *
 * SEARCH
 * ---------------------------------------------------------- */

document.getElementById("searchEmployeesForm").addEventListener("submit", async (evt) => {
  evt.preventDefault();
  const fd = new FormData(evt.target);
  const params = new URLSearchParams();
  ["department", "skillName", "proficiency"].forEach((k) => {
    const v = fd.get(k);
    if (v) params.set(k, v);
  });
  try {
    const results = await apiRequest(`/search/employees?${params.toString()}`);
    const tbody = document.querySelector("#searchEmployeesTable tbody");
    tbody.innerHTML = results.length
      ? results.map((e) => `<tr><td>${employeeName(e)}</td><td>${e.email}</td><td>${e.department}</td><td>${e.designation}</td></tr>`).join("")
      : `<tr class="empty-row"><td colspan="4">No matching employees.</td></tr>`;
  } catch (e) {
    toast(`Search failed: ${e.message}`, "error");
  }
});

document.getElementById("searchSkillsForm").addEventListener("submit", async (evt) => {
  evt.preventDefault();
  const fd = new FormData(evt.target);
  const params = new URLSearchParams();
  ["keyword", "category"].forEach((k) => {
    const v = fd.get(k);
    if (v) params.set(k, v);
  });
  try {
    const results = await apiRequest(`/search/skills?${params.toString()}`);
    const tbody = document.querySelector("#searchSkillsTable tbody");
    tbody.innerHTML = results.length
      ? results.map((s) => `<tr><td>${s.name}</td><td>${s.category}</td><td>${s.description || "—"}</td><td>${s.status}</td></tr>`).join("")
      : `<tr class="empty-row"><td colspan="4">No matching skills.</td></tr>`;
  } catch (e) {
    toast(`Search failed: ${e.message}`, "error");
  }
});

/* ---------------------------------------------------------- *
 * ALERTS
 * ---------------------------------------------------------- */

document.getElementById("alertsForm").addEventListener("submit", async (evt) => {
  evt.preventDefault();
  const days = document.getElementById("alertDays").value || 30;
  try {
    const results = await apiRequest(`/alerts/expiring-certifications?days=${days}`);
    const tbody = document.querySelector("#alertsTable tbody");
    tbody.innerHTML = results.length
      ? results.map((a) => {
          const left = daysUntil(a.expiryDate);
          const overdue = left !== null && left < 0;
          return `
            <tr>
              <td>${employeeName(a.employee)}</td>
              <td>${a.employee ? a.employee.department : "—"}</td>
              <td>${a.skill ? a.skill.name : "—"}</td>
              <td>${a.certificationName || "—"}</td>
              <td>${fmtDate(a.expiryDate)}</td>
              <td><span class="pill ${overdue ? "pill-warn" : "pill-unverified"}">${left === null ? "—" : overdue ? `${Math.abs(left)}d overdue` : `${left}d`}</span></td>
            </tr>`;
        }).join("")
      : `<tr class="empty-row"><td colspan="6">Nothing expiring in that window.</td></tr>`;
  } catch (e) {
    toast(`Could not load alerts: ${e.message}`, "error");
  }
});

/* ---------------------------------------------------------- *
 * MODAL
 * ---------------------------------------------------------- */

function openModal(title, bodyHtml) {
  document.getElementById("modalTitle").textContent = title;
  document.getElementById("modalBody").innerHTML = bodyHtml;
  document.getElementById("modalOverlay").classList.remove("hidden");
}
function closeModal() {
  document.getElementById("modalOverlay").classList.add("hidden");
  document.getElementById("modalBody").innerHTML = "";
}
document.getElementById("modalClose").addEventListener("click", closeModal);
document.getElementById("modalOverlay").addEventListener("click", (e) => {
  if (e.target.id === "modalOverlay") closeModal();
});

/* ---------------------------------------------------------- *
 * Boot
 * ---------------------------------------------------------- */

showEnvBadge();
checkApiHealth();
loadDashboard();
