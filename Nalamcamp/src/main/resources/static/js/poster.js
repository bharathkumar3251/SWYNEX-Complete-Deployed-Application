const posterUploadForm = document.getElementById('posterUploadForm');
const posterFileInput = document.getElementById('posterFileInput');
const uploadBtn = document.getElementById('uploadBtn');
const skipToManualBtn = document.getElementById('skipToManualBtn');
const uploadFeedback = document.getElementById('uploadFeedback');

const panelStep1 = document.getElementById('panelStep1');
const panelStep2 = document.getElementById('panelStep2');
const panelStep3 = document.getElementById('panelStep3');

const pStep1 = document.getElementById('pStep1');
const pStep2 = document.getElementById('pStep2');
const pStep3 = document.getElementById('pStep3');
const pLine1 = document.getElementById('pLine1');
const pLine2 = document.getElementById('pLine2');

const rawOcrText = document.getElementById('rawOcrText');
const matchForm = document.getElementById('matchForm');
const posterCheckIdHidden = document.getElementById('posterCheckIdHidden');
const correctedCampName = document.getElementById('correctedCampName');
const correctedOrganizer = document.getElementById('correctedOrganizer');
const correctedDate = document.getElementById('correctedDate');
const correctedVenue = document.getElementById('correctedVenue');
const verifyBtn = document.getElementById('verifyBtn');
const btnBackToStep1 = document.getElementById('btnBackToStep1');
const btnBackToStep2 = document.getElementById('btnBackToStep2');
const resetOcrBtn = document.getElementById('resetOcrBtn');
const matchFeedback = document.getElementById('matchFeedback');

const resultBanner = document.getElementById('resultBanner');
const matchedCampDetails = document.getElementById('matchedCampDetails');
const resCampName = document.getElementById('resCampName');
const resOrganizer = document.getElementById('resOrganizer');
const resDate = document.getElementById('resDate');
const resVenue = document.getElementById('resVenue');
const resVerification = document.getElementById('resVerification');
const resViewCampLink = document.getElementById('resViewCampLink');
const manualReviewBox = document.getElementById('manualReviewBox');
const submitManualReviewBtn = document.getElementById('submitManualReviewBtn');
const manualReviewFeedback = document.getElementById('manualReviewFeedback');

let currentStep = 1;

function setPosterStep(targetStep) {
  currentStep = targetStep;

  panelStep1.classList.remove('active');
  panelStep2.classList.remove('active');
  panelStep3.classList.remove('active');

  pStep1.classList.remove('active', 'completed');
  pStep2.classList.remove('active', 'completed');
  pStep3.classList.remove('active', 'completed');
  pLine1.classList.remove('completed');
  pLine2.classList.remove('completed');

  const circle1 = document.getElementById('pCircle1');
  const circle2 = document.getElementById('pCircle2');
  const circle3 = document.getElementById('pCircle3');

  if (targetStep === 1) {
    panelStep1.classList.add('active');
    pStep1.classList.add('active');
    circle1.textContent = '1';
    circle2.textContent = '2';
    circle3.textContent = '3';
  } else if (targetStep === 2) {
    panelStep2.classList.add('active');
    pStep1.classList.add('completed');
    pStep2.classList.add('active');
    circle1.textContent = '✓';
    circle2.textContent = '2';
    circle3.textContent = '3';
    pLine1.classList.add('completed');
  } else if (targetStep === 3) {
    panelStep3.classList.add('active');
    pStep1.classList.add('completed');
    pStep2.classList.add('completed');
    pStep3.classList.add('active');
    circle1.textContent = '✓';
    circle2.textContent = '✓';
    circle3.textContent = '3';
    pLine1.classList.add('completed');
    pLine2.classList.add('completed');
  }

  window.scrollTo({ top: 120, behavior: 'smooth' });
}

posterUploadForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const file = posterFileInput.files[0];
  if (!file) {
    uploadFeedback.innerHTML = '<div class="alert alert-error">Please choose a valid poster image file.</div>';
    return;
  }

  uploadBtn.disabled = true;
  uploadBtn.textContent = 'Processing OCR Extraction...';
  uploadFeedback.innerHTML = '<div class="loading">Running Tesseract Optical Character Recognition on server...</div>';

  const formData = new FormData();
  formData.append('file', file);

  try {
    const resp = await apiRequest('/api/poster-checks', {
      method: 'POST',
      body: formData
    });

    uploadFeedback.innerHTML = '<div class="alert alert-success">Text extracted successfully from poster.</div>';
    populateExtractedFields(resp);
    setPosterStep(2);
  } catch (err) {
    if (err.status === 503) {
      uploadFeedback.innerHTML = `
        <div class="alert alert-info">
          <p><strong>OCR Service Notice:</strong> Tesseract OCR engine is not configured on the host server. Switching to manual verification mode below.</p>
        </div>
      `;
      populateExtractedFields({
        posterCheckId: null,
        originalFileName: file.name,
        extractedText: 'OCR extraction unavailable on host system. Please enter poster details manually below.',
        suggestedOrganizer: '',
        suggestedCampDate: '',
        suggestedVenue: '',
        suggestedCampName: ''
      });
      setPosterStep(2);
      return;
    }

    uploadFeedback.innerHTML = `<div class="alert alert-error">Extraction failed: ${escapeHtml(err.message)}</div>`;
  } finally {
    uploadBtn.disabled = false;
    uploadBtn.textContent = 'Extract Poster Details (OCR) →';
  }
});

skipToManualBtn.addEventListener('click', () => {
  populateExtractedFields({
    posterCheckId: null,
    originalFileName: 'manual-entry',
    extractedText: 'Manual entry mode (no poster image uploaded).',
    suggestedOrganizer: '',
    suggestedCampDate: '',
    suggestedVenue: '',
    suggestedCampName: ''
  });
  setPosterStep(2);
});

function populateExtractedFields(data) {
  rawOcrText.textContent = data.extractedText || 'No text extracted.';
  posterCheckIdHidden.value = data.posterCheckId || '';
  correctedCampName.value = data.suggestedCampName || '';
  correctedOrganizer.value = data.suggestedOrganizer || '';
  correctedDate.value = data.suggestedCampDate || '';
  correctedVenue.value = data.suggestedVenue || '';
}

btnBackToStep1.addEventListener('click', () => {
  setPosterStep(1);
});

btnBackToStep2.addEventListener('click', () => {
  setPosterStep(2);
});

resetOcrBtn.addEventListener('click', () => {
  posterUploadForm.reset();
  matchForm.reset();
  uploadFeedback.innerHTML = '';
  matchFeedback.innerHTML = '';
  resultBanner.innerHTML = '';
  matchedCampDetails.style.display = 'none';
  manualReviewBox.style.display = 'none';
  setPosterStep(1);
});

matchForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const pcId = posterCheckIdHidden.value;

  verifyBtn.disabled = true;
  verifyBtn.textContent = 'Cross-referencing Records...';
  matchFeedback.innerHTML = '<div class="loading">Cross-referencing approved medical camps registry...</div>';

  const matchPayload = {
    campName: correctedCampName.value.trim(),
    organizerName: correctedOrganizer.value.trim(),
    campDate: correctedDate.value ? correctedDate.value : null,
    venue: correctedVenue.value.trim()
  };

  try {
    let result;
    if (pcId) {
      result = await apiRequest(`/api/poster-checks/${pcId}/match`, {
        method: 'POST',
        body: JSON.stringify(matchPayload)
      });
    } else {
      const queryParams = new URLSearchParams();
      if (matchPayload.venue) queryParams.append('area', matchPayload.venue);
      if (matchPayload.campDate) queryParams.append('date', matchPayload.campDate);
      if (matchPayload.campName) queryParams.append('service', matchPayload.campName);

      const found = await apiRequest('/api/camps?' + queryParams.toString());
      if (found && found.length > 0) {
        const top = found[0];
        result = {
          status: 'MATCHED',
          matchedCampId: top.id,
          matchedCampName: top.name,
          matchedCampDate: top.campDate,
          matchedVenue: top.venue,
          verificationSource: top.verificationSource || 'Official Public Health Circular'
        };
      } else {
        result = {
          status: 'UNABLE_TO_CONFIRM',
          discrepancies: 'No approved camp matches the manual criteria entered.'
        };
      }
    }

    matchFeedback.innerHTML = '';
    renderOutcome(result, matchPayload);
    setPosterStep(3);
  } catch (err) {
    matchFeedback.innerHTML = `<div class="alert alert-error">Verification check failed: ${escapeHtml(err.message)}</div>`;
  } finally {
    verifyBtn.disabled = false;
    verifyBtn.textContent = 'Proceed to Step 3: Match Approved Records →';
  }
});

function renderOutcome(res, inputData) {
  resultBanner.innerHTML = '';
  matchedCampDetails.style.display = 'none';
  manualReviewBox.style.display = 'none';

  if (res.status === 'MATCHED') {
    resultBanner.innerHTML = `
      <div class="alert alert-success">
        <h3 style="color: #14532d; margin-bottom: 0.35rem;">✓ Camp Details Match Approved Registry</h3>
        <p>This medical camp is officially registered and authorized under district public health guidelines.</p>
      </div>
    `;

    resCampName.textContent = res.matchedCampName || inputData.campName || 'Approved Medical Camp';
    resOrganizer.textContent = res.suggestedOrganizer || inputData.organizerName || 'Authorized Health Partner';
    resDate.textContent = formatDate(res.suggestedCampDate || inputData.campDate);
    resVenue.textContent = res.suggestedVenue || inputData.venue || '-';
    resVerification.textContent = res.verificationSource || 'Tamil Nadu DHS Circular';

    if (res.matchedCampId) {
      resViewCampLink.setAttribute('href', `camp-details.html?id=${res.matchedCampId}`);
      resViewCampLink.style.display = 'inline-block';
    } else {
      resViewCampLink.style.display = 'none';
    }

    matchedCampDetails.style.display = 'block';
  } else if (res.status === 'DISCREPANCY') {
    resultBanner.innerHTML = `
      <div class="alert alert-warning">
        <h3 style="color: #78350f; margin-bottom: 0.35rem;">⚠ Details Differ from Official Record</h3>
        <p>A matching camp was identified, but specific details on your poster deviate from the approved record.</p>
        <div style="font-weight: 700; margin-top: 0.5rem; font-size: 0.88rem;">Discrepancy Details:</div>
        <p style="font-size: 0.88rem;">${escapeHtml(res.discrepancies || 'Venue or organizer does not align with registered record.')}</p>
      </div>
    `;

    resCampName.textContent = res.matchedCampName || '-';
    resOrganizer.textContent = res.suggestedOrganizer || '-';
    resDate.textContent = formatDate(res.suggestedCampDate || inputData.campDate);
    resVenue.textContent = res.suggestedVenue || '-';
    resVerification.textContent = res.verificationSource || 'Registered Health Notice';

    if (res.matchedCampId) {
      resViewCampLink.setAttribute('href', `camp-details.html?id=${res.matchedCampId}`);
      resViewCampLink.style.display = 'inline-block';
    } else {
      resViewCampLink.style.display = 'none';
    }

    matchedCampDetails.style.display = 'block';
  } else {
    resultBanner.innerHTML = `
      <div class="alert alert-info">
        <h3 style="color: #1e3a8a; margin-bottom: 0.35rem;">ℹ Unable to Confirm Against Current Listings</h3>
        <p>We could not match this poster with currently approved district health circulars.</p>
        <p style="font-size: 0.85rem; margin-top: 0.5rem;">Public Health Notice: An unmatched poster alone is insufficient evidence to label it fake. Many legitimate camps are announced locally before online database synchronization.</p>
      </div>
    `;
    manualReviewBox.style.display = 'block';
  }
}

submitManualReviewBtn.addEventListener('click', async () => {
  const pcId = posterCheckIdHidden.value;
  if (!pcId) {
    manualReviewFeedback.innerHTML = '<div class="alert alert-warning">Manual review submission requires an uploaded poster image file. Please upload a file in Step 1.</div>';
    return;
  }

  submitManualReviewBtn.disabled = true;
  submitManualReviewBtn.textContent = 'Submitting for Scrutiny...';
  manualReviewFeedback.innerHTML = '<div class="loading">Submitting poster check to district health officer queue...</div>';

  try {
    await apiRequest(`/api/poster-checks/${pcId}/submit-review`, {
      method: 'POST'
    });

    manualReviewFeedback.innerHTML = `
      <div class="alert alert-success">
        <strong>Submitted for Public Health Scrutiny:</strong> This poster check has been placed in the reviewer queue (Status: PENDING_REVIEW). A health officer will independently verify it against district circulars.
      </div>
    `;
    submitManualReviewBtn.style.display = 'none';
  } catch (err) {
    manualReviewFeedback.innerHTML = `<div class="alert alert-error">Submission failed: ${escapeHtml(err.message)}</div>`;
    submitManualReviewBtn.disabled = false;
    submitManualReviewBtn.textContent = 'Submit Poster for Manual Health Officer Review';
  }
});
