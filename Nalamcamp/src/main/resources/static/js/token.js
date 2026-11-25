const lookupForm = document.getElementById('lookupForm');
const referenceInput = document.getElementById('referenceInput');
const lookupFeedback = document.getElementById('lookupFeedback');
const tokenResultSection = document.getElementById('tokenResultSection');

const tokenNumberDisplay = document.getElementById('tokenNumberDisplay');
const tokenStatusBadge = document.getElementById('tokenStatusBadge');
const referenceDisplay = document.getElementById('referenceDisplay');
const visitorNameDisplay = document.getElementById('visitorNameDisplay');
const visitorPhoneDisplay = document.getElementById('visitorPhoneDisplay');
const campNameDisplay = document.getElementById('campNameDisplay');
const serviceNameDisplay = document.getElementById('serviceNameDisplay');
const campDateDisplay = document.getElementById('campDateDisplay');
const reportingWindowDisplay = document.getElementById('reportingWindowDisplay');
const venueDisplay = document.getElementById('venueDisplay');
const feeDisplay = document.getElementById('feeDisplay');
const checkInStatusDisplay = document.getElementById('checkInStatusDisplay');
const instructionsDisplay = document.getElementById('instructionsDisplay');

const cancelledBanner = document.getElementById('cancelledBanner');
const checkedInBanner = document.getElementById('checkedInBanner');
const cancelTokenBtn = document.getElementById('cancelTokenBtn');
const cancelConfirmModal = document.getElementById('cancelConfirmModal');
const confirmCancelBtn = document.getElementById('confirmCancelBtn');
const abortCancelBtn = document.getElementById('abortCancelBtn');
const btnPrintToken = document.getElementById('btnPrintToken');

let currentBooking = null;

const urlParams = new URLSearchParams(window.location.search);
const initialRef = urlParams.get('ref');
if (initialRef) {
  referenceInput.value = initialRef.trim();
  lookupBooking(initialRef.trim());
}

lookupForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const ref = referenceInput.value.trim();
  if (!ref) {
    lookupFeedback.innerHTML = '<div class="alert alert-error">Please enter a valid booking reference code.</div>';
    return;
  }
  lookupBooking(ref);
});

async function lookupBooking(ref) {
  lookupFeedback.innerHTML = '<div class="loading">Searching public health appointment registry...</div>';
  tokenResultSection.style.display = 'none';
  cancelConfirmModal.style.display = 'none';

  try {
    const booking = await apiRequest('/api/bookings/' + encodeURIComponent(ref));
    currentBooking = booking;
    lookupFeedback.innerHTML = '';
    renderToken(booking);
  } catch (err) {
    lookupFeedback.innerHTML = `
      <div class="alert alert-error">
        <p><strong>Appointment Lookup Failed:</strong> ${escapeHtml(err.message)}</p>
        <p style="font-size: 0.85rem; margin-top: 0.25rem;">Please ensure you entered the exact reference code from your booking acknowledgment.</p>
      </div>
    `;
  }
}

function renderToken(booking) {
  tokenNumberDisplay.textContent = booking.tokenNumber || 'TK-CONFIRMED';
  referenceDisplay.textContent = booking.bookingReference;
  visitorNameDisplay.textContent = booking.visitorName || booking.residentName || '-';
  visitorPhoneDisplay.textContent = booking.phone || booking.visitorPhone || '-';
  campNameDisplay.textContent = booking.campName || 'Approved Community Health Camp';
  serviceNameDisplay.textContent = booking.serviceName || 'Consultation Counter';
  campDateDisplay.textContent = formatDate(booking.campDate);
  reportingWindowDisplay.textContent = booking.reportingWindow || 'Operating Hours';
  venueDisplay.textContent = booking.venue || '-';
  feeDisplay.textContent = formatFee(booking.fee);
  instructionsDisplay.textContent = booking.instructions || 'Aadhaar Card, medical cards, previous prescription';

  const isCancelled = booking.status === 'CANCELLED';
  const isCheckedIn = booking.checkedIn === true;

  if (isCancelled) {
    tokenStatusBadge.textContent = 'CANCELLED &bull; RELEASED';
    tokenStatusBadge.style.color = 'var(--gov-danger)';
    cancelledBanner.style.display = 'block';
    checkedInBanner.style.display = 'none';
    checkInStatusDisplay.innerHTML = '<span class="badge badge-cancelled">Cancelled</span>';
    cancelTokenBtn.style.display = 'none';
  } else if (isCheckedIn) {
    tokenStatusBadge.textContent = 'ADMITTED &bull; CHECKED IN';
    tokenStatusBadge.style.color = 'var(--gov-info)';
    cancelledBanner.style.display = 'none';
    checkedInBanner.style.display = 'block';
    const checkTime = booking.checkInTime ? formatTime(booking.checkInTime.split('T')[1] || booking.checkInTime) : 'Yes';
    checkInStatusDisplay.innerHTML = `<span class="badge badge-approved">Checked In (${checkTime})</span>`;
    cancelTokenBtn.style.display = 'none';
  } else {
    tokenStatusBadge.textContent = 'CONFIRMED &bull; AUTHORIZED';
    tokenStatusBadge.style.color = 'var(--gov-green)';
    cancelledBanner.style.display = 'none';
    checkedInBanner.style.display = 'none';
    checkInStatusDisplay.innerHTML = '<span class="badge badge-draft">Awaiting Reception Check-in</span>';
    cancelTokenBtn.style.display = 'inline-block';
  }

  tokenResultSection.style.display = 'block';
  tokenResultSection.scrollIntoView({ behavior: 'smooth' });
}

cancelTokenBtn.addEventListener('click', () => {
  cancelConfirmModal.style.display = 'block';
  cancelConfirmModal.scrollIntoView({ behavior: 'smooth' });
});

abortCancelBtn.addEventListener('click', () => {
  cancelConfirmModal.style.display = 'none';
});

confirmCancelBtn.addEventListener('click', async () => {
  if (!currentBooking) return;

  confirmCancelBtn.disabled = true;
  confirmCancelBtn.textContent = 'Cancelling Appointment...';
  lookupFeedback.innerHTML = '<div class="loading">Releasing token slot on server...</div>';

  try {
    const updated = await apiRequest('/api/bookings/' + encodeURIComponent(currentBooking.bookingReference) + '/cancel', {
      method: 'POST'
    });

    currentBooking = updated;
    lookupFeedback.innerHTML = `
      <div class="alert alert-warning">
        Appointment <strong>${escapeHtml(updated.bookingReference)}</strong> has been cancelled. Your slot has been returned to the community pool.
      </div>
    `;

    renderToken(updated);
    cancelConfirmModal.style.display = 'none';
  } catch (err) {
    lookupFeedback.innerHTML = `<div class="alert alert-error">Cancellation failed: ${escapeHtml(err.message)}</div>`;
  } finally {
    confirmCancelBtn.disabled = false;
    confirmCancelBtn.textContent = 'Yes, Cancel My Appointment';
  }
});

btnPrintToken.addEventListener('click', () => {
  window.print();
});
