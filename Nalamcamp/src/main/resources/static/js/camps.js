const filterForm = document.getElementById('filterForm');
const areaInput = document.getElementById('areaInput');
const dateInput = document.getElementById('dateInput');
const serviceInput = document.getElementById('serviceInput');
const clearBtn = document.getElementById('clearBtn');
const resultsFeedback = document.getElementById('resultsFeedback');
const resultsCount = document.getElementById('resultsCount');
const campsList = document.getElementById('campsList');
const statCampCount = document.getElementById('statCampCount');
const statServiceCount = document.getElementById('statServiceCount');

async function loadCamps(params = {}) {
  resultsFeedback.innerHTML = '<div class="loading">Loading approved medical camps from registry...</div>';
  campsList.innerHTML = '';
  resultsCount.textContent = '';

  const query = new URLSearchParams();
  if (params.area) query.append('area', params.area);
  if (params.date) query.append('date', params.date);
  if (params.service) query.append('service', params.service);

  const endpoint = '/api/camps' + (query.toString() ? '?' + query.toString() : '');

  try {
    const camps = await apiRequest(endpoint);
    resultsFeedback.innerHTML = '';

    if (!camps || camps.length === 0) {
      resultsCount.textContent = '0 Camps Found';
      resultsFeedback.innerHTML = `
        <div class="alert alert-info">
          <p><strong>No medical camps found matching your criteria.</strong></p>
          <p style="font-size: 0.88rem; margin-top: 0.35rem;">Try clearing the filter or checking neighboring localities.</p>
        </div>
      `;
      if (statCampCount) statCampCount.textContent = '0';
      if (statServiceCount) statServiceCount.textContent = '0';
      return;
    }

    resultsCount.textContent = camps.length === 1 ? '1 Camp Available' : `${camps.length} Camps Available`;
    if (statCampCount) statCampCount.textContent = camps.length;

    let totalServices = 0;
    camps.forEach((c) => {
      totalServices += (c.services ? c.services.length : 1);
    });
    if (statServiceCount) statServiceCount.textContent = totalServices;

    renderCamps(camps);
  } catch (err) {
    resultsFeedback.innerHTML = `
      <div class="alert alert-error">
        <p><strong>Failed to load medical camps:</strong> ${escapeHtml(err.message)}</p>
        <button type="button" class="btn btn-secondary btn-sm" id="retryLoadBtn" style="margin-top: 0.5rem;">Retry Loading</button>
      </div>
    `;
    const retryBtn = document.getElementById('retryLoadBtn');
    if (retryBtn) {
      retryBtn.addEventListener('click', () => loadCamps(params));
    }
  }
}

function renderCamps(camps) {
  campsList.innerHTML = '';

  camps.forEach((camp) => {
    const card = document.createElement('article');
    card.className = 'card';

    const isCancelled = camp.status === 'CANCELLED';
    const statusBadge = isCancelled
      ? '<span class="badge badge-cancelled">Cancelled</span>'
      : '<span class="badge badge-approved">Approved &bull; Official</span>';

    const services = camp.services || [];
    let countersSummaryHtml = '';

    if (services.length > 0) {
      const badges = services.map(s => {
        const remaining = s.availableTokens !== undefined ? s.availableTokens : (s.onlineCapacity - (s.onlineReserved || 0));
        const slotText = remaining > 0 ? `${remaining} slots left` : 'Slots Full';
        return `<span class="badge badge-draft" style="font-weight: 600; text-transform: none; margin-right: 0.35rem; margin-bottom: 0.35rem;">${escapeHtml(s.serviceName)} (${slotText})</span>`;
      }).join('');
      countersSummaryHtml = `
        <div style="margin-top: 0.75rem;">
          <div style="font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--gov-text-muted); margin-bottom: 0.25rem;">Active Service Counters</div>
          <div>${badges}</div>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="card-header-bar">
        <div>
          <h3 style="margin-bottom: 0.2rem;">
            <a href="camp-details.html?id=${camp.id}" style="color: var(--gov-green); text-decoration: none;">
              ${escapeHtml(camp.name)}
            </a>
          </h3>
          <div style="font-size: 0.88rem; color: var(--gov-text-muted);">
            Organizer: <strong>${escapeHtml(camp.organizerName || 'Community Health Partner')}</strong> (${escapeHtml(camp.organizerType || 'PUBLIC')})
          </div>
        </div>
        <div>${statusBadge}</div>
      </div>

      <table class="detail-table" aria-label="Camp Summary">
        <tbody>
          <tr>
            <th scope="row">Camp Date</th>
            <td><strong>${formatDate(camp.campDate)}</strong></td>
          </tr>
          <tr>
            <th scope="row">District / Area</th>
            <td>${escapeHtml(camp.area)}</td>
          </tr>
          <tr>
            <th scope="row">Venue Address</th>
            <td>${escapeHtml(camp.venue)}</td>
          </tr>
          <tr>
            <th scope="row">Circular Source</th>
            <td style="font-size: 0.85rem; color: #1e3a8a;">${escapeHtml(camp.verificationSource || 'Verified by District Health Circular')}</td>
          </tr>
        </tbody>
      </table>

      ${countersSummaryHtml}

      <div class="step-actions" style="margin-top: 1rem; padding-top: 0.75rem;">
        <a href="camp-details.html?id=${camp.id}" class="btn btn-secondary btn-sm">View Camp &amp; Counter Schedule</a>
        ${isCancelled ? '<span class="badge badge-cancelled">Bookings Closed</span>' : `<a href="booking.html?campId=${camp.id}" class="btn btn-primary btn-sm">Book Token (Step 1) →</a>`}
      </div>
    `;

    campsList.appendChild(card);
  });
}

filterForm.addEventListener('submit', (e) => {
  e.preventDefault();
  loadCamps({
    area: areaInput.value.trim(),
    date: dateInput.value,
    service: serviceInput.value.trim()
  });
});

clearBtn.addEventListener('click', () => {
  filterForm.reset();
  loadCamps({});
});

loadCamps({});
