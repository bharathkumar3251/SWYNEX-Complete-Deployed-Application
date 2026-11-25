const urlParams = new URLSearchParams(window.location.search);
const campId = urlParams.get('id');

const pageFeedback = document.getElementById('pageFeedback');
const campContainer = document.getElementById('campContainer');
const breadcrumbCampName = document.getElementById('breadcrumbCampName');
const campName = document.getElementById('campName');
const campOrganizer = document.getElementById('campOrganizer');
const campStatusBadge = document.getElementById('campStatusBadge');
const campDate = document.getElementById('campDate');
const campArea = document.getElementById('campArea');
const campVenue = document.getElementById('campVenue');
const organizerType = document.getElementById('organizerType');
const campVerification = document.getElementById('campVerification');
const cancellationAlert = document.getElementById('cancellationAlert');
const servicesList = document.getElementById('servicesList');

const bookingSection = document.getElementById('bookingSection');
const bookingServiceSummary = document.getElementById('bookingServiceSummary');
const bookingFeedback = document.getElementById('bookingFeedback');
const bookingForm = document.getElementById('bookingForm');
const bookingSessionId = document.getElementById('bookingSessionId');
const bookingIdempotencyKey = document.getElementById('bookingIdempotencyKey');
const visitorNameInput = document.getElementById('visitorNameInput');
const visitorPhoneInput = document.getElementById('visitorPhoneInput');
const cancelBookingBtn = document.getElementById('cancelBookingBtn');
const confirmBookingBtn = document.getElementById('confirmBookingBtn');

const confirmationSection = document.getElementById('confirmationSection');
const confTokenNumber = document.getElementById('confTokenNumber');
const confReference = document.getElementById('confReference');
const confServiceName = document.getElementById('confServiceName');
const confReportingWindow = document.getElementById('confReportingWindow');
const confVenue = document.getElementById('confVenue');
const confInstructions = document.getElementById('confInstructions');
const btnPrintConf = document.getElementById('btnPrintConf');
const btnGoToMyToken = document.getElementById('btnGoToMyToken');

let currentCamp = null;

if (!campId) {
  pageFeedback.innerHTML = `
    <div class="alert alert-error">
      <p><strong>Missing Camp Identifier:</strong> No medical camp was specified.</p>
      <a href="index.html" class="btn btn-secondary btn-sm" style="margin-top: 0.5rem;">Return to Find a Camp</a>
    </div>
  `;
} else {
  loadCampDetails();
}

async function loadCampDetails() {
  pageFeedback.innerHTML = '<div class="loading">Loading official medical camp particulars...</div>';

  try {
    const camp = await apiRequest(`/api/camps/${campId}`);
    currentCamp = camp;
    pageFeedback.innerHTML = '';
    renderCamp(camp);
  } catch (err) {
    pageFeedback.innerHTML = `
      <div class="alert alert-error">
        <p><strong>Failed to load medical camp:</strong> ${escapeHtml(err.message)}</p>
        <div class="btn-group" style="margin-top: 0.5rem;">
          <button type="button" class="btn btn-secondary btn-sm" id="retryCampBtn">Retry</button>
          <a href="index.html" class="btn btn-secondary btn-sm">Return to Listings</a>
        </div>
      </div>
    `;
    const retryBtn = document.getElementById('retryCampBtn');
    if (retryBtn) {
      retryBtn.addEventListener('click', loadCampDetails);
    }
  }
}

function renderCamp(camp) {
  campContainer.style.display = 'block';

  campName.textContent = camp.name;
  if (breadcrumbCampName) {
    breadcrumbCampName.textContent = camp.name;
  }
  campOrganizer.textContent = `Organized by: ${camp.organizerName || 'Community Public Health Partner'}`;

  const isCancelled = camp.status === 'CANCELLED';
  if (isCancelled) {
    campStatusBadge.textContent = 'Cancelled';
    campStatusBadge.className = 'badge badge-cancelled';
    cancellationAlert.style.display = 'block';
  } else {
    campStatusBadge.textContent = 'Approved &bull; Official';
    campStatusBadge.className = 'badge badge-approved';
    cancellationAlert.style.display = 'none';
  }

  campDate.textContent = formatDate(camp.campDate);
  campArea.textContent = camp.area;
  campVenue.textContent = camp.venue;
  organizerType.textContent = camp.organizerType === 'GOVERNMENT' ? 'Government Public Health Body' : 'Private / NGO Health Partner';
  campVerification.textContent = camp.verificationSource || 'Verified by District Health Circular';

  renderServices(camp.services || [], isCancelled);
}

function renderServices(services, isCampCancelled) {
  servicesList.innerHTML = '';

  if (!services || services.length === 0) {
    servicesList.innerHTML = `
      <div class="alert alert-warning">
        No consultation counters are currently listed for this medical camp.
      </div>
    `;
    return;
  }

  services.forEach((service) => {
    const card = document.createElement('div');
    card.className = 'card';

    const allowsOnline = service.bookingMode === 'BOTH' || service.bookingMode === 'ONLINE';
    const allowsWalkIn = service.bookingMode === 'BOTH' || service.bookingMode === 'WALK_IN';
    const hasOnlineTokens = service.availableTokens > 0;
    const canBook = !isCampCancelled && allowsOnline && hasOnlineTokens;

    let availabilityHtml = '';
    if (isCampCancelled) {
      availabilityHtml = '<span class="badge badge-cancelled">Bookings Closed (Camp Cancelled)</span>';
    } else {
      if (allowsOnline && service.availableTokens > 0) {
        availabilityHtml = `<span class="badge badge-approved">${service.availableTokens} online slots available</span>`;
      } else if (allowsOnline && service.availableTokens === 0) {
        if (allowsWalkIn) {
          availabilityHtml = '<span class="badge badge-warning">Online tokens full &bull; Venue walk-ins accepted</span>';
        } else {
          availabilityHtml = '<span class="badge badge-cancelled">Online tokens full</span>';
        }
      } else {
        availabilityHtml = '<span class="badge badge-draft">Walk-in only &bull; Tokens issued directly at venue</span>';
      }
    }

    let modeDescription = 'Online Tokens & Venue Walk-ins';
    if (service.bookingMode === 'ONLINE') {
      modeDescription = 'Online Tokens Only';
    } else if (service.bookingMode === 'WALK_IN') {
      modeDescription = 'Walk-in Only (Tokens issued at venue)';
    }

    const hours = `${formatTime(service.startTime)} - ${formatTime(service.endTime)}`;

    card.innerHTML = `
      <div class="card-header-bar">
        <div>
          <h3 style="margin-bottom: 0.2rem; color: var(--gov-green);">${escapeHtml(service.serviceName)}</h3>
          <div style="font-size: 0.88rem; color: var(--gov-text-muted); font-weight: 500;">
            Operating Window: <strong>${hours}</strong>
          </div>
        </div>
        <div>${availabilityHtml}</div>
      </div>

      <table class="detail-table" aria-label="Counter Details">
        <tbody>
          <tr>
            <th scope="row">Consultation Fee</th>
            <td><strong>${formatFee(service.fee)}</strong></td>
          </tr>
          <tr>
            <th scope="row">Booking Mode</th>
            <td>${modeDescription}</td>
          </tr>
          <tr>
            <th scope="row">Online Capacity</th>
            <td>${allowsOnline ? `${service.onlineReserved} / ${service.onlineCapacity} reserved (${service.availableTokens} remaining)` : 'Not applicable'}</td>
          </tr>
          <tr>
            <th scope="row">Mandatory Items to Bring</th>
            <td style="color: #1e3a8a; font-weight: 600;">${escapeHtml(service.itemsToBring || 'Aadhaar Card or Photo ID')}</td>
          </tr>
        </tbody>
      </table>

      <div class="step-actions" style="margin-top: 1rem; padding-top: 0.75rem;">
        <span style="font-size: 0.85rem; color: var(--gov-text-muted);">Reporting Window: <strong>${hours}</strong></span>
        <div class="btn-group">
          ${canBook ? `<a href="booking.html?campId=${currentCamp.id}&sessionId=${service.id}" class="btn btn-primary btn-sm">Proceed to Book Token (Step-by-Step) →</a>` : ''}
          ${canBook ? `<button type="button" class="btn btn-secondary btn-sm quick-book-trigger" data-session-id="${service.id}" data-service-name="${escapeHtml(service.serviceName)}" data-hours="${hours}">Quick Reserve on Page</button>` : ''}
        </div>
      </div>
    `;

    servicesList.appendChild(card);
  });

  document.querySelectorAll('.quick-book-trigger').forEach((btn) => {
    btn.addEventListener('click', () => {
      const sessionId = btn.getAttribute('data-session-id');
      const serviceName = btn.getAttribute('data-service-name');
      const hours = btn.getAttribute('data-hours');
      openQuickBooking(sessionId, serviceName, hours);
    });
  });
}

function openQuickBooking(sessionId, serviceName, hours) {
  bookingFeedback.innerHTML = '';
  bookingSessionId.value = sessionId;
  bookingIdempotencyKey.value = generateIdempotencyKey();

  bookingServiceSummary.textContent = `${serviceName} (${hours}) at ${currentCamp.venue}`;
  bookingSection.style.display = 'block';
  bookingSection.scrollIntoView({ behavior: 'smooth' });
}

cancelBookingBtn.addEventListener('click', () => {
  bookingSection.style.display = 'none';
  bookingForm.reset();
});

bookingForm.addEventListener('submit', async (e) => {
  e.preventDefault();

  const sessionId = bookingSessionId.value;
  const idempotencyKey = bookingIdempotencyKey.value;
  const visitorName = visitorNameInput.value.trim();
  const visitorPhone = visitorPhoneInput.value.trim();

  if (!visitorName || !visitorPhone) {
    bookingFeedback.innerHTML = '<div class="alert alert-error">Please fill in both your full name and 10-digit mobile phone number.</div>';
    return;
  }

  confirmBookingBtn.disabled = true;
  confirmBookingBtn.textContent = 'Reserving Token...';
  bookingFeedback.innerHTML = '<div class="loading">Reserving token on server...</div>';

  const payload = {
    visitorName: visitorName,
    phone: visitorPhone,
    visitorPhone: visitorPhone,
    idempotencyKey: idempotencyKey
  };

  try {
    const booking = await apiRequest(`/api/services/${sessionId}/bookings`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    bookingSection.style.display = 'none';
    bookingForm.reset();

    renderConfirmation(booking);
    loadCampDetails();
  } catch (err) {
    bookingFeedback.innerHTML = `
      <div class="alert alert-error">
        <p><strong>Reservation Failed:</strong> ${escapeHtml(err.message)}</p>
        <button type="submit" class="btn btn-secondary btn-sm" style="margin-top: 0.5rem;">Retry with same key (${idempotencyKey})</button>
      </div>
    `;
    confirmBookingBtn.disabled = false;
    confirmBookingBtn.textContent = 'Confirm Token Reservation →';
  }
});

function renderConfirmation(booking) {
  confTokenNumber.textContent = booking.tokenNumber || 'Token Confirmed';
  confReference.textContent = booking.bookingReference;
  confServiceName.textContent = booking.serviceName || '-';
  confReportingWindow.textContent = booking.reportingWindow || '-';
  confVenue.textContent = booking.venue || currentCamp.venue;
  confInstructions.textContent = booking.instructions || 'Aadhaar Card, medical cards, previous prescription';

  if (btnGoToMyToken) {
    btnGoToMyToken.setAttribute('href', `token.html?ref=${encodeURIComponent(booking.bookingReference)}`);
  }

  confirmationSection.style.display = 'block';
  confirmationSection.scrollIntoView({ behavior: 'smooth' });
}

if (btnPrintConf) {
  btnPrintConf.addEventListener('click', () => {
    window.print();
  });
}
