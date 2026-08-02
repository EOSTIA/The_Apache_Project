// Dashboard
// Plain vanilla JS
// It polls each service's own /config endpoint directly (no gateway needed
// for local dev) and shows what's currently sitting in each service's
// in-memory ConcurrentHashMap.
//
// Note: this assumes WSL2 mirrored networking mode is enabled (see README),
// so "localhost" means the same thing from the browser (Windows) as it does
// for the services themselves (running in WSL).

const SERVICES = 
[
  { name: "payments", port: 8081 },
  { name: "auth", port: 8082 },
  { name: "inventory", port: 8083 },
];

const CONFIG_SERVER = "http://localhost:8084";
const REFRESH_MS = 3000;
const MAX_LOG_ENTRIES = 50;

const grid = document.getElementById("servicesGrid");
const activityList = document.getElementById("activityList");
const pushForm = document.getElementById("pushForm");
const pushStatus = document.getElementById("pushStatus");

function logActivity(message, isError = false) 
{
  const li = document.createElement("li");
  const time = new Date().toLocaleTimeString();
  li.innerHTML = `<span class="time">${time}</span>${message}`;
  if (isError) li.style.color = "#f87171";
  activityList.prepend(li);
  while (activityList.children.length > MAX_LOG_ENTRIES) 
  {
    activityList.removeChild(activityList.lastChild);
  }
}

function renderServiceCard(service, data, isUp) 
{
  let card = document.getElementById(`card-${service.name}`);
  if (!card) 
  {
    card = document.createElement("div");
    card.className = "service-card";
    card.id = `card-${service.name}`;
    grid.appendChild(card);
  }

  const rows = data && data.values
    ? Object.entries(data.values).map(([k, v]) => `<tr><td>${escapeHtml(k)}</td><td>${escapeHtml(v)}</td></tr>`).join("")
    : `<tr><td colspan="2">no config yet</td></tr>`;

  const lastUpdated = data && data.lastUpdated
    ? new Date(data.lastUpdated).toLocaleTimeString()
    : "never";

  card.innerHTML = `
    <h3><span class="dot ${isUp ? "" : "down"}"></span>${service.name} <span style="color:#8b93a7;font-weight:400;font-size:12px;">:${service.port}</span></h3>
    <div class="service-meta">${isUp ? `${data.count} keys · last update ${lastUpdated}` : "unreachable"}</div>
    <table class="config-table"><tbody>${rows}</tbody></table>
  `;
}

async function pollService(service) 
{
  try 
  {
    const res = await fetch(`http://localhost:${service.port}/api/${service.name}/config`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    renderServiceCard(service, data, true);
  } catch (err) 
  {
    renderServiceCard(service, null, false);
  }
}

function pollAll() 
{
  SERVICES.forEach(pollService);
}

function escapeHtml(str) 
{
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

pushForm.addEventListener("submit", async (e) => 
{
  e.preventDefault();

  const service = document.getElementById("serviceSelect").value;
  const key = document.getElementById("keyInput").value.trim();
  const value = document.getElementById("valueInput").value.trim();
  const user = document.getElementById("userInput").value;
  const pass = document.getElementById("passInput").value;

  pushStatus.textContent = "Pushing...";
  pushStatus.className = "";

  try 
  {
    const res = await fetch(`${CONFIG_SERVER}/api/config/push`, 
    {
      method: "POST",
      headers: 
      {
        "Content-Type": "application/json",
        "Authorization": "Basic " + btoa(`${user}:${pass}`),
      },
      body: JSON.stringify({ service, key, value }),
    });

    if (!res.ok) 
    {
      const text = await res.text();
      throw new Error(`HTTP ${res.status} - ${text}`);
    }

    const result = await res.json();
    pushStatus.textContent = `Published to ${result.topic}`;
    pushStatus.className = "status-ok";
    logActivity(`Pushed <b>${escapeHtml(key)}</b> = <b>${escapeHtml(value)}</b> to <b>${service}</b> (topic: ${result.topic})`);

    document.getElementById("keyInput").value = "";
    document.getElementById("valueInput").value = "";

    setTimeout(pollAll, 800);
  } catch (err) 
  {
    pushStatus.textContent = `Failed: ${err.message}`;
    pushStatus.className = "status-error";
    logActivity(`Push failed: ${escapeHtml(err.message)}`, true);
  }
});

pollAll();
setInterval(pollAll, REFRESH_MS);
logActivity("Dashboard loaded, polling started");