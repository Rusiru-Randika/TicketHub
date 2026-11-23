const state = {
  token: localStorage.getItem('tickethub_token') || null,
  user: JSON.parse(localStorage.getItem('tickethub_user') || 'null'),
  events: []
};

/* ---------- API (same origin, so it works on localhost, LAN IP and ngrok) ---------- */
async function api(path, { method = 'GET', body, token = state.token } = {}) {
  const headers = { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { /* not JSON */ }
  if (!res.ok) {
    if (res.status === 401 && token === state.token && state.token && !path.startsWith('/api/auth/')) {
      logout('Session expired or token rejected - please log in again.');
    }
    const err = new Error((data && data.message) || ('HTTP ' + res.status));
    err.status = res.status;
    throw err;
  }
  return data;
}

/* ---------- Alerts & Helpers ---------- */
function showAlert(message, type = 'error') {
  const banner = document.getElementById('alertBanner');
  document.getElementById('alertText').textContent = message;
  banner.className = `alert ${type === 'success' ? 'alert-success' : 'alert-error'}`;
  banner.classList.remove('hidden');
}

function hideAlert() {
  document.getElementById('alertBanner').classList.add('hidden');
}

function openModal(id) { document.getElementById(id).classList.remove('hidden'); }
function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

function logout(reason) {
  state.token = null;
  state.user = null;
  localStorage.removeItem('tickethub_token');
  localStorage.removeItem('tickethub_user');
  renderShell();
  if (reason) showAlert(reason, 'error');
}

/* ---------- Auth ---------- */
function renderShell() {
  const loggedIn = Boolean(state.token && state.user);
  document.getElementById('authSection').classList.toggle('hidden', loggedIn);
  document.getElementById('appSection').classList.toggle('hidden', !loggedIn);
  document.getElementById('userControls').classList.toggle('hidden', !loggedIn);

  if (loggedIn) {
    document.getElementById('userBadge').textContent = `${state.user.username} · ${state.user.role}`;
    const isAdmin = state.user.role === 'ADMIN';
    const isStaff = isAdmin || state.user.role === 'ORGANIZER';
    document.getElementById('adminNavBtn').classList.toggle('hidden', !isAdmin);
    document.getElementById('newEventBtn').classList.toggle('hidden', !isStaff);
    document.getElementById('reservationsTitle').textContent = isStaff ? 'All / Event Reservations' : 'My Reservations';
    document.getElementById('jwtRawInput').value = state.token;
    decodeJwt(state.token);
    loadEvents();
  }
}

document.getElementById('showLoginTab').addEventListener('click', () => {
  document.getElementById('showLoginTab').classList.add('active');
  document.getElementById('showRegisterTab').classList.remove('active');
  document.getElementById('loginForm').classList.remove('hidden');
  document.getElementById('registerForm').classList.add('hidden');
});

document.getElementById('showRegisterTab').addEventListener('click', () => {
  document.getElementById('showRegisterTab').classList.add('active');
  document.getElementById('showLoginTab').classList.remove('active');
  document.getElementById('registerForm').classList.remove('hidden');
  document.getElementById('loginForm').classList.add('hidden');
});

document.querySelectorAll('[data-demo-user]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.getElementById('loginUsername').value = btn.dataset.demoUser;
    document.getElementById('loginPassword').value = btn.dataset.demoPass;
  });
});

document.getElementById('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  hideAlert();
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: {
        username: document.getElementById('loginUsername').value.trim(),
        password: document.getElementById('loginPassword').value
      },
      token: null
    });
    state.token = data.token;
    state.user = { username: data.username, fullName: data.fullName, role: data.role };
    localStorage.setItem('tickethub_token', state.token);
    localStorage.setItem('tickethub_user', JSON.stringify(state.user));
    renderShell();
  } catch (err) {
    showAlert(err.message);
  }
});

document.getElementById('registerForm').addEventListener('submit', async e => {
  e.preventDefault();
  hideAlert();
  try {
    await api('/api/auth/register', {
      method: 'POST',
      body: {
        username: document.getElementById('regUsername').value.trim(),
        fullName: document.getElementById('regFullName').value.trim(),
        password: document.getElementById('regPassword').value,
        role: 'CUSTOMER'
      },
      token: null
    });
    showAlert('Registration successful! Please log in.', 'success');
    document.getElementById('showLoginTab').click();
    document.getElementById('loginUsername').value = document.getElementById('regUsername').value.trim();
  } catch (err) {
    showAlert(err.message);
  }
});

document.getElementById('logoutBtn').addEventListener('click', () => logout());
document.getElementById('alertClose').addEventListener('click', hideAlert);
document.getElementById('themeToggle').addEventListener('click', () => document.body.classList.toggle('dark'));

/* ---------- Navigation ---------- */
document.querySelectorAll('.nav-pill').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.nav-pill').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.view-panel').forEach(p => p.classList.add('hidden'));
    document.getElementById(btn.dataset.view).classList.remove('hidden');

    if (btn.dataset.view === 'eventsView') loadEvents();
    if (btn.dataset.view === 'reservationsView') loadReservations();
    if (btn.dataset.view === 'adminView') loadAdminData();
    if (btn.dataset.view === 'jwtView') {
      document.getElementById('jwtRawInput').value = state.token || '';
      decodeJwt(state.token);
    }
  });
});

/* ---------- Events ---------- */
async function loadEvents(keyword = '') {
  try {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    state.events = await api(`/api/events${query}`);
    renderEvents();
  } catch (err) {
    showAlert(err.message);
  }
}

function renderEvents() {
  const grid = document.getElementById('eventsGrid');
  if (!state.events.length) {
    grid.innerHTML = '<div class="card muted">No events found.</div>';
    return;
  }

  const role = state.user.role;
  grid.innerHTML = state.events.map(ev => {
    const booked = ev.totalSeats - ev.availableSeats;
    const pct = ev.totalSeats > 0 ? Math.round((booked / ev.totalSeats) * 100) : 0;
    const canEdit = role === 'ADMIN' || (role === 'ORGANIZER' && ev.organizerUsername === state.user.username);
    const canDelete = role === 'ADMIN';
    const canBook = role === 'CUSTOMER' && ev.availableSeats > 0;

    return `
      <div class="card event-card">
        <div class="event-card-head">
          <h3>${ev.name}</h3>
          <span class="badge ${ev.availableSeats > 0 ? 'badge-green' : 'badge-red'}">${ev.availableSeats} available</span>
        </div>
        <div class="event-meta">
          <span>📍 ${ev.venue}</span>
          <span>🕒 ${new Date(ev.eventDate).toLocaleString()}</span>
          <span>👤 ${ev.organizerUsername}</span>
        </div>
        <p class="muted">${ev.description || ''}</p>
        <div class="meter"><div class="meter-fill" style="width:${pct}%"></div></div>
        <div class="event-footer">
          <span>${booked}/${ev.totalSeats} booked</span>
          <span>Rs. ${Number(ev.ticketPrice).toLocaleString()}</span>
        </div>
        <div class="card-actions">
          ${canBook ? `<button class="btn btn-primary btn-sm" onclick="openBookModal('${ev.id}')">Book</button>` : ''}
          ${canEdit ? `<button class="btn btn-secondary btn-sm" onclick="openEditEventModal('${ev.id}')">Edit</button>` : ''}
          ${canDelete ? `<button class="btn btn-danger btn-sm" onclick="deleteEvent('${ev.id}')">Delete</button>` : ''}
        </div>
      </div>
    `;
  }).join('');
}

document.getElementById('searchForm').addEventListener('submit', e => {
  e.preventDefault();
  loadEvents(document.getElementById('searchInput').value.trim());
});

document.getElementById('newEventBtn').addEventListener('click', () => {
  document.getElementById('eventModalTitle').textContent = 'Create Event';
  document.getElementById('eventForm').reset();
  document.getElementById('eventIdInput').value = '';
  openModal('eventModal');
});

window.openEditEventModal = id => {
  const ev = state.events.find(e => e.id === id);
  if (!ev) return;
  document.getElementById('eventModalTitle').textContent = 'Edit Event';
  document.getElementById('eventIdInput').value = ev.id;
  document.getElementById('eventNameInput').value = ev.name;
  document.getElementById('eventDescInput').value = ev.description || '';
  document.getElementById('eventVenueInput').value = ev.venue;
  document.getElementById('eventDateInput').value = ev.eventDate.slice(0, 16);
  document.getElementById('eventSeatsInput').value = ev.totalSeats;
  document.getElementById('eventPriceInput').value = ev.ticketPrice;
  openModal('eventModal');
};

document.getElementById('eventForm').addEventListener('submit', async e => {
  e.preventDefault();
  const id = document.getElementById('eventIdInput').value;
  const body = {
    name: document.getElementById('eventNameInput').value.trim(),
    description: document.getElementById('eventDescInput').value.trim(),
    venue: document.getElementById('eventVenueInput').value.trim(),
    eventDate: document.getElementById('eventDateInput').value + ':00',
    totalSeats: Number(document.getElementById('eventSeatsInput').value),
    ticketPrice: Number(document.getElementById('eventPriceInput').value)
  };
  try {
    if (id) {
      await api(`/api/events/${id}`, { method: 'PUT', body });
      showAlert('Event updated!', 'success');
    } else {
      await api('/api/events', { method: 'POST', body });
      showAlert('Event created!', 'success');
    }
    closeModal('eventModal');
    loadEvents();
  } catch (err) {
    showAlert(err.message);
  }
});

window.deleteEvent = async id => {
  if (!confirm('Delete this event?')) return;
  try {
    await api(`/api/events/${id}`, { method: 'DELETE' });
    showAlert('Event deleted.', 'success');
    loadEvents();
  } catch (err) {
    showAlert(err.message);
  }
};

/* ---------- Reservations ---------- */
window.openBookModal = eventId => {
  const ev = state.events.find(e => e.id === eventId);
  if (!ev) return;
  document.getElementById('bookModalTitle').textContent = `Book: ${ev.name}`;
  document.getElementById('bookEventIdInput').value = ev.id;
  document.getElementById('bookReservationIdInput').value = '';
  document.getElementById('bookEventSummary').textContent = `${ev.availableSeats} seats available · Rs. ${ev.ticketPrice} each`;
  document.getElementById('bookSeatsInput').value = 2;
  openModal('bookModal');
};

window.openEditReservationModal = (resId, currentSeats) => {
  document.getElementById('bookModalTitle').textContent = 'Update Reservation Seats';
  document.getElementById('bookEventIdInput').value = '';
  document.getElementById('bookReservationIdInput').value = resId;
  document.getElementById('bookEventSummary').textContent = `Current seats: ${currentSeats}`;
  document.getElementById('bookSeatsInput').value = currentSeats;
  openModal('bookModal');
};

document.getElementById('bookForm').addEventListener('submit', async e => {
  e.preventDefault();
  const resId = document.getElementById('bookReservationIdInput').value;
  const eventId = document.getElementById('bookEventIdInput').value;
  const seats = Number(document.getElementById('bookSeatsInput').value);

  try {
    if (resId) {
      await api(`/api/reservations/${resId}`, { method: 'PUT', body: { seats } });
      showAlert('Reservation updated!', 'success');
      closeModal('bookModal');
      loadReservations();
    } else {
      await api('/api/reservations', { method: 'POST', body: { eventId, seats } });
      showAlert('Tickets booked!', 'success');
      closeModal('bookModal');
      loadEvents();
    }
  } catch (err) {
    showAlert(err.message);
  }
});

async function loadReservations() {
  try {
    const endpoint = state.user.role === 'CUSTOMER' ? '/api/reservations/my' : '/api/reservations';
    const list = await api(endpoint);
    const container = document.getElementById('reservationsList');
    if (!list.length) {
      container.innerHTML = '<div class="card muted">No reservations found.</div>';
      return;
    }
    container.innerHTML = list.map(r => {
      const isConfirmed = r.status === 'CONFIRMED';
      const canModify = isConfirmed && (state.user.role === 'ADMIN' || r.customerUsername === state.user.username);
      return `
        <div class="card res-card">
          <div>
            <strong>${r.eventName}</strong> · <span class="badge ${isConfirmed ? 'badge-green' : 'badge-red'}">${r.status}</span>
            <div class="muted">Code: ${r.reservationCode} · Seats: ${r.seats} · Total: Rs. ${Number(r.totalAmount).toLocaleString()} · Customer: ${r.customerUsername}</div>
          </div>
          <div class="card-actions">
            ${canModify ? `<button class="btn btn-secondary btn-sm" onclick="openEditReservationModal('${r.id}', ${r.seats})">Change Seats</button>` : ''}
            ${canModify ? `<button class="btn btn-danger btn-sm" onclick="cancelReservation('${r.id}')">Cancel</button>` : ''}
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    showAlert(err.message);
  }
}

window.cancelReservation = async id => {
  if (!confirm('Cancel this reservation?')) return;
  try {
    await api(`/api/reservations/${id}`, { method: 'DELETE' });
    showAlert('Reservation cancelled and seats released.', 'success');
    loadReservations();
  } catch (err) {
    showAlert(err.message);
  }
};

document.getElementById('refreshReservationsBtn').addEventListener('click', loadReservations);

/* ---------- Admin ---------- */
async function loadAdminData() {
  try {
    const [stats, users] = await Promise.all([api('/api/admin/stats'), api('/api/admin/users')]);
    document.getElementById('statsGrid').innerHTML = `
      <div class="card stat-box"><div class="stat-value">${stats.totalUsers}</div><div class="stat-label">Users</div></div>
      <div class="card stat-box"><div class="stat-value">${stats.totalEvents}</div><div class="stat-label">Events</div></div>
      <div class="card stat-box"><div class="stat-value">${stats.totalTicketsSold}</div><div class="stat-label">Tickets Sold</div></div>
      <div class="card stat-box"><div class="stat-value">Rs. ${Number(stats.totalRevenue).toLocaleString()}</div><div class="stat-label">Revenue</div></div>
    `;
    document.getElementById('usersTableBody').innerHTML = users.map(u => `
      <tr>
        <td>${u.username}</td>
        <td>${u.fullName}</td>
        <td><span class="badge badge-green">${u.role}</span></td>
        <td class="card-actions">
          <button class="btn btn-secondary btn-sm" onclick="openEditUserModal('${u.id}', '${u.username}', '${u.fullName.replace(/'/g, "\\'")}', '${u.role}')">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="deleteUser('${u.id}')">Delete</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    showAlert(err.message);
  }
}

document.getElementById('refreshAdminBtn').addEventListener('click', loadAdminData);

document.getElementById('newUserBtn').addEventListener('click', () => {
  document.getElementById('userModalTitle').textContent = 'Add User';
  document.getElementById('userForm').reset();
  document.getElementById('userIdInput').value = '';
  document.getElementById('userUsernameWrap').classList.remove('hidden');
  document.getElementById('userUsernameInput').required = true;
  openModal('userModal');
});

window.openEditUserModal = (id, username, fullName, role) => {
  document.getElementById('userModalTitle').textContent = `Edit User (${username})`;
  document.getElementById('userIdInput').value = id;
  document.getElementById('userUsernameWrap').classList.add('hidden');
  document.getElementById('userUsernameInput').required = false;
  document.getElementById('userFullNameInput').value = fullName;
  document.getElementById('userRoleInput').value = role;
  document.getElementById('userPasswordInput').value = '';
  openModal('userModal');
};

document.getElementById('userForm').addEventListener('submit', async e => {
  e.preventDefault();
  const id = document.getElementById('userIdInput').value;
  const fullName = document.getElementById('userFullNameInput').value.trim();
  const role = document.getElementById('userRoleInput').value;
  const password = document.getElementById('userPasswordInput').value;

  try {
    if (id) {
      const body = { fullName, role };
      if (password) body.password = password;
      await api(`/api/admin/users/${id}`, { method: 'PUT', body });
      showAlert('User updated!', 'success');
    } else {
      await api('/api/auth/register', {
        method: 'POST',
        body: { username: document.getElementById('userUsernameInput').value.trim(), fullName, role, password }
      });
      showAlert('User created!', 'success');
    }
    closeModal('userModal');
    loadAdminData();
  } catch (err) {
    showAlert(err.message);
  }
});

window.deleteUser = async id => {
  if (!confirm('Delete this user?')) return;
  try {
    await api(`/api/admin/users/${id}`, { method: 'DELETE' });
    showAlert('User deleted.', 'success');
    loadAdminData();
  } catch (err) {
    showAlert(err.message);
  }
};

/* ---------- JWT Inspector ---------- */
function decodeJwt(token) {
  if (!token || token.split('.').length !== 3) {
    document.getElementById('jwtHeaderOut').textContent = 'Invalid JWT format';
    document.getElementById('jwtPayloadOut').textContent = 'Invalid JWT format';
    return;
  }
  const [h, p] = token.split('.');
  const b64Decode = s => JSON.stringify(JSON.parse(atob(s.replace(/-/g, '+').replace(/_/g, '/'))), null, 2);
  try {
    document.getElementById('jwtHeaderOut').textContent = b64Decode(h);
    document.getElementById('jwtPayloadOut').textContent = b64Decode(p);
  } catch (e) {
    document.getElementById('jwtPayloadOut').textContent = 'Unable to decode Base64Url';
  }
}

document.getElementById('decodeJwtBtn').addEventListener('click', () => {
  decodeJwt(document.getElementById('jwtRawInput').value.trim());
});

document.getElementById('tamperJwtBtn').addEventListener('click', async () => {
  const token = document.getElementById('jwtRawInput').value.trim();
  const parts = token.split('.');
  if (parts.length !== 3) return;
  // Flip one character in the payload so the signature no longer matches
  const tamperedPayload = parts[1].slice(0, -1) + (parts[1].slice(-1) === 'A' ? 'B' : 'A');
  const tamperedToken = `${parts[0]}.${tamperedPayload}.${parts[2]}`;
  const out = document.getElementById('tamperResult');
  try {
    await api('/api/auth/me', { token: tamperedToken });
  } catch (err) {
    out.textContent = `Server rejected tampered JWT with HTTP ${err.status}: ${err.message}`;
    out.className = 'alert alert-error';
    out.classList.remove('hidden');
  }
});

/* ---------- Modal Close & Init ---------- */
document.querySelectorAll('[data-close-modal]').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.closeModal));
});

renderShell();
