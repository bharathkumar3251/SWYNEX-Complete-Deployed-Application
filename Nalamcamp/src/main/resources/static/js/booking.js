let currentStep = 1;
let selectedCamp = null;
let selectedSession = null;
let allApprovedCamps = [];
let cachedBookingResponse = null;
let currentIdempotencyKey = generateIdempotencyKey();

const stepNodes = [
  document.getElementById('stepNode1'),
  document.getElementById('stepNode2'),
  document.getElementById('stepNode3'),
  document.getElementById('stepNode4')
];

const stepLines = [
  document.getElementById('line1'),
  document.getElementById('line2'),
  document.getElementById('line3')
];

const stepPanels = [
  document.getElementById('step1Panel'),
  document.getElementById('step2Panel'),
  document.getElementById('step3Panel'),
  document.getElementById('step4Panel')
];

const wizardFeedback = document.getElementById('wizardFeedback');
const preselectedCampCard = document.getElementById('preselectedCampCard');
const selectCampDropdown = document.getElementById('selectCampDropdown');
const counterSelectionList = document.getElementById('counterSelectionList');
const preCampName = document.getElementById('preCampName');
const preCampOrganizer = document.getElementById('preCampOrganizer');
const preCampDateArea = document.getElementById('preCampDateArea');
const preCampVenue = document.getElementById('preCampVenue');
const preCampSource = document.getElementById('preCampSource');

const btnStep1Next = document.getElementById('btnStep1Next');
const btnStep2Back = document.getElementById('btnStep2Back');
const btnStep2Next = document.getElementById('btnStep2Next');
const btnStep3Back = document.getElementById('btnStep3Back');
const btnStep3Confirm = document.getElementById('btnStep3Confirm');
const btnPrintSlip = document.getElementById('btnPrintSlip');
const btnBookAnother = document.getElementById('btnBookAnother');
const btnViewMyToken = document.getElementById('btnViewMyToken');

const inputResidentName = document.getElementById('inputResidentName');
const inputResidentPhone = document.getElementById('inputResidentPhone');
const inputResidentAge = document.getElementById('inputResidentAge');
const inputResidentGender = document.getElementById('inputResidentGender');
const inputResidentArea = document.getElementById('inputResidentArea');
const inputMedicalNotes = document.getElementById('inputMedicalNotes');
const chkDeclaration = document.getElementById('chkDeclaration');

const revCampName = document.getElementById('revCampName');
const revServiceName = document.getElementById('revServiceName');
const revCampDate = document.getElementById('revCampDate');
const revReportingWindow = document.getElementById('revReportingWindow');
const revVenue = document.getElementById('revVenue');
const revFee = document.getElementById('revFee');
const revItemsToBring = document.getElementById('revItemsToBring');
const revResidentName = document.getElementById('revResidentName');
const revResidentPhone = document.getElementById('revResidentPhone');
const revResidentDemo = document.getElementById('revResidentDemo');
const revResidentArea = document.getElementById('revResidentArea');

const slipTokenNumber = document.getElementById('slipTokenNumber');
const slipReference = document.getElementById('slipReference');
const slipResidentName = document.getElementById('slipResidentName');
const slipResidentPhone = document.getElementById('slipResidentPhone');
const slipCampName = document.getElementById('slipCampName');
const slipServiceName = document.getElementById('slipServiceName');
const slipCampDate = document.getElementById('slipCampDate');
const slipReportingWindow = document.getElementById('slipReportingWindow');
const slipVenue = document.getElementById('slipVenue');
const slipFee = document.getElementById('slipFee');
const slipInstructions = document.getElementById('slipInstructions');

function setWizardStep(targetStep) {
  currentStep = targetStep;
  wizardFeedback.innerHTML = '';

  stepPanels.forEach((panel, idx) => {
    if (idx === targetStep - 1) {
      panel.classList.add('active');
    } else {
      panel.classList.remove('active');
    }
  });

  stepNodes.forEach((node, idx) => {
    const stepNum = idx + 1;
    const circle = node.querySelector('.step-circle');
    node.classList.remove('active', 'completed');

    if (stepNum < targetStep) {
      node.classList.add('completed');
      circle.textContent = '✓';
    } else if (stepNum === targetStep) {
      node.classList.add('active');
      circle.textContent = stepNum;
    } else {
      circle.textContent = stepNum;
    }
  });

  stepLines.forEach((line, idx) => {
    if (idx < targetStep - 1) {
      line.classList.add('completed');
    } else {
      line.classList.remove('completed');
    }
  });

  window.scrollTo({ top: 120, behavior: 'smooth' });
}

async function initializeWizard() {
  const urlParams = new URLSearchParams(window.location.search);
  const targetCampId = urlParams.get('campId');
  const targetSessionId = urlParams.get('sessionId');

  try {
    wizardFeedback.innerHTML = '<div class="loading">Loading approved public health camps...</div>';
    allApprovedCamps = await apiRequest('/api/camps');
    wizardFeedback.innerHTML = '';

    selectCampDropdown.innerHTML = '<option value="">-- Choose Approved Camp --</option>';
    allApprovedCamps.forEach((c) => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `${c.name} (${c.area} - ${formatDate(c.campDate)})`;
      selectCampDropdown.appendChild(opt);
    });

    if (targetCampId) {
      selectCampDropdown.value = targetCampId;
      await onCampSelected(targetCampId, targetSessionId);
    }
  } catch (err) {
    wizardFeedback.innerHTML = `<div class="alert alert-error">Failed to load public medical camps: ${escapeHtml(err.message)}</div>`;
  }
}

selectCampDropdown.addEventListener('change', async () => {
  const cid = selectCampDropdown.value;
  if (!cid) {
    selectedCamp = null;
    selectedSession = null;
    preselectedCampCard.style.display = 'none';
    counterSelectionList.innerHTML = '<div class="alert alert-info">Please choose an approved camp above to inspect its active service counters.</div>';
    return;
  }
  await onCampSelected(cid, null);
});

async function onCampSelected(campId, preselectedSessionId) {
  try {
    counterSelectionList.innerHTML = '<div class="loading">Loading service counters for selected camp...</div>';
    selectedCamp = await apiRequest(`/api/camps/${campId}`);

    preCampName.textContent = selectedCamp.name;
    preCampOrganizer.textContent = `${selectedCamp.organizerName || 'Government / NGO Partner'} (${selectedCamp.organizerType || 'PUBLIC'})`;
    preCampDateArea.textContent = `${formatDate(selectedCamp.campDate)} &bull; ${selectedCamp.area}`;
    preCampVenue.textContent = selectedCamp.venue;
    preCampSource.textContent = selectedCamp.verificationSource || 'Official Public Health Notification';
    preselectedCampCard.style.display = 'block';

    renderCounters(selectedCamp.services || [], preselectedSessionId);
  } catch (err) {
    counterSelectionList.innerHTML = `<div class="alert alert-error">Failed to load camp details: ${escapeHtml(err.message)}</div>`;
  }
}

function renderCounters(services, preselectedSessionId) {
  if (!services || services.length === 0) {
    counterSelectionList.innerHTML = '<div class="alert alert-warning">No service counters are currently configured for this camp.</div>';
    return;
  }

  counterSelectionList.innerHTML = '';
  selectedSession = null;

  services.forEach((s) => {
    const card = document.createElement('div');
    card.className = 'card';
    card.style.cursor = 'pointer';
    card.style.border = '2px solid #cbd5e1';
    card.style.marginBottom = '0.75rem';

    const allowsOnline = s.bookingMode === 'BOTH' || s.bookingMode === 'ONLINE';
    const isAvailable = allowsOnline && s.availableTokens > 0;
    const hours = `${formatTime(s.startTime)} - ${formatTime(s.endTime)}`;

    let statusPill = '';
    if (!allowsOnline) {
      statusPill = '<span class="badge badge-draft">Walk-in Only</span>';
    } else if (s.availableTokens > 0) {
      statusPill = `<span class="badge badge-approved">${s.availableTokens} Slots Available</span>`;
    } else {
      statusPill = '<span class="badge badge-cancelled">Slots Full</span>';
    }

    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 0.5rem;">
        <div style="display: flex; align-items: flex-start; gap: 0.75rem;">
          <input type="radio" name="selectedCounterRadio" id="radio_${s.id}" value="${s.id}" ${!isAvailable ? 'disabled' : ''} style="margin-top: 0.3rem; width: 1.15rem; height: 1.15rem; accent-color: var(--gov-green);">
          <div>
            <label for="radio_${s.id}" style="font-size: 1.05rem; font-weight: 700; color: var(--gov-green); cursor: pointer; display: block; margin-bottom: 0.2rem;">
              ${escapeHtml(s.serviceName)}
            </label>
            <div style="font-size: 0.88rem; color: var(--gov-text-muted); font-weight: 500;">
              Reporting Hours: <strong>${hours}</strong> &bull; Fee: <strong>${formatFee(s.fee)}</strong>
            </div>
            <div style="font-size: 0.82rem; color: #64748b; margin-top: 0.25rem;">
              Items to bring: ${escapeHtml(s.itemsToBring || 'Valid ID Card')}
            </div>
          </div>
        </div>
        <div>${statusPill}</div>
      </div>
    `;

    const radio = card.querySelector(`input[type="radio"]`);

    if (isAvailable) {
      card.addEventListener('click', () => {
        radio.checked = true;
        highlightSelectedCounterCard(card, s);
      });
      radio.addEventListener('change', () => {
        highlightSelectedCounterCard(card, s);
      });
    } else {
      card.style.opacity = '0.6';
      card.style.cursor = 'not-allowed';
    }

    counterSelectionList.appendChild(card);

    if (preselectedSessionId && String(s.id) === String(preselectedSessionId) && isAvailable) {
      radio.checked = true;
      highlightSelectedCounterCard(card, s);
    }
  });
}

function highlightSelectedCounterCard(selectedCard, session) {
  selectedSession = session;
  document.querySelectorAll('#counterSelectionList .card').forEach((c) => {
    c.style.borderColor = '#cbd5e1';
    c.style.backgroundColor = '#ffffff';
  });
  selectedCard.style.borderColor = 'var(--gov-green)';
  selectedCard.style.backgroundColor = '#f0fdf4';
}

btnStep1Next.addEventListener('click', () => {
  if (!selectedCamp) {
    wizardFeedback.innerHTML = '<div class="alert alert-error">Please select a medical camp from the dropdown list.</div>';
    return;
  }
  if (!selectedSession) {
    wizardFeedback.innerHTML = '<div class="alert alert-error">Please choose an available service counter by clicking on its card.</div>';
    return;
  }
  setWizardStep(2);
});

btnStep2Back.addEventListener('click', () => {
  setWizardStep(1);
});

btnStep2Next.addEventListener('click', () => {
  const name = inputResidentName.value.trim();
  const phone = inputResidentPhone.value.trim();

  if (!name) {
    wizardFeedback.innerHTML = '<div class="alert alert-error">Please enter the resident\'s full name.</div>';
    inputResidentName.focus();
    return;
  }

  const phoneRegex = /^[0-9]{10}$/;
  if (!phoneRegex.test(phone)) {
    wizardFeedback.innerHTML = '<div class="alert alert-error">Please enter a valid 10-digit mobile number.</div>';
    inputResidentPhone.focus();
    return;
  }

  populateReviewStep();
  setWizardStep(3);
});

function populateReviewStep() {
  revCampName.textContent = selectedCamp.name;
  revServiceName.textContent = selectedSession.serviceName;
  revCampDate.textContent = formatDate(selectedCamp.campDate);
  revReportingWindow.textContent = `${formatTime(selectedSession.startTime)} - ${formatTime(selectedSession.endTime)}`;
  revVenue.textContent = `${selectedCamp.venue}, ${selectedCamp.area}`;
  revFee.textContent = formatFee(selectedSession.fee);
  revItemsToBring.textContent = selectedSession.itemsToBring || 'Aadhaar Card or Photo ID';

  revResidentName.textContent = inputResidentName.value.trim();
  revResidentPhone.textContent = inputResidentPhone.value.trim();

  const age = inputResidentAge.value.trim();
  const gender = inputResidentGender.value;
  let demoStr = 'Not Specified';
  if (age && gender !== 'Not Specified') {
    demoStr = `${age} Years / ${gender}`;
  } else if (age) {
    demoStr = `${age} Years`;
  } else if (gender !== 'Not Specified') {
    demoStr = gender;
  }
  revResidentDemo.textContent = demoStr;
  revResidentArea.textContent = inputResidentArea.value.trim() || selectedCamp.area;

  chkDeclaration.checked = false;
}

btnStep3Back.addEventListener('click', () => {
  setWizardStep(2);
});

btnStep3Confirm.addEventListener('click', async () => {
  if (!chkDeclaration.checked) {
    wizardFeedback.innerHTML = '<div class="alert alert-error">You must accept the self-declaration checkbox before proceeding.</div>';
    chkDeclaration.focus();
    return;
  }

  btnStep3Confirm.disabled = true;
  btnStep3Confirm.textContent = 'Issuing Official Token Slip...';
  wizardFeedback.innerHTML = '<div class="loading">Submitting reservation to public health server...</div>';

  const payload = {
    visitorName: inputResidentName.value.trim(),
    phone: inputResidentPhone.value.trim(),
    idempotencyKey: currentIdempotencyKey
  };

  try {
    const booking = await apiRequest(`/api/services/${selectedSession.id}/bookings`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    cachedBookingResponse = booking;
    populateSlipStep(booking);
    setWizardStep(4);
  } catch (err) {
    wizardFeedback.innerHTML = `
      <div class="alert alert-error">
        <p><strong>Reservation Failed:</strong> ${escapeHtml(err.message)}</p>
        <p style="margin-top: 0.5rem; font-size: 0.85rem;">You can safely click retry below. Your request key (${currentIdempotencyKey}) is preserved to prevent duplicate booking.</p>
      </div>
    `;
    btnStep3Confirm.disabled = false;
    btnStep3Confirm.textContent = 'Retry Reservation';
  }
});

function populateSlipStep(booking) {
  slipTokenNumber.textContent = booking.tokenNumber || 'TK-CONFIRMED';
  slipReference.textContent = booking.bookingReference;
  slipResidentName.textContent = inputResidentName.value.trim();
  slipResidentPhone.textContent = inputResidentPhone.value.trim();
  slipCampName.textContent = booking.campName || selectedCamp.name;
  slipServiceName.textContent = booking.serviceName || selectedSession.serviceName;
  slipCampDate.textContent = formatDate(booking.campDate || selectedCamp.campDate);
  slipReportingWindow.textContent = booking.reportingWindow || `${formatTime(selectedSession.startTime)} - ${formatTime(selectedSession.endTime)}`;
  slipVenue.textContent = booking.venue || selectedCamp.venue;
  slipFee.textContent = formatFee(booking.fee !== undefined ? booking.fee : selectedSession.fee);
  slipInstructions.textContent = booking.instructions || selectedSession.itemsToBring || 'Aadhaar Card, previous spectacles prescription, and medical records';

  btnViewMyToken.setAttribute('href', `token.html?ref=${encodeURIComponent(booking.bookingReference)}`);
}

btnPrintSlip.addEventListener('click', () => {
  window.print();
});

btnBookAnother.addEventListener('click', () => {
  currentIdempotencyKey = generateIdempotencyKey();
  inputResidentName.value = '';
  inputResidentPhone.value = '';
  inputResidentAge.value = '';
  inputResidentGender.value = 'Not Specified';
  inputResidentArea.value = '';
  inputMedicalNotes.value = '';
  chkDeclaration.checked = false;
  btnStep3Confirm.disabled = false;
  btnStep3Confirm.textContent = 'Confirm & Generate Official Token Slip →';
  setWizardStep(1);
});

initializeWizard();
