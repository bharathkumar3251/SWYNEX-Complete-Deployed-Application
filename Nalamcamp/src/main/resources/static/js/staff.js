document.addEventListener('DOMContentLoaded', () => {
  const staffHeaderBar = document.getElementById('staffHeaderBar');
  const currentStaffEmail = document.getElementById('currentStaffEmail');
  const currentStaffRole = document.getElementById('currentStaffRole');
  const currentStaffOrg = document.getElementById('currentStaffOrg');
  const logoutBtn = document.getElementById('logoutBtn');

  const loginSection = document.getElementById('loginSection');
  const loginForm = document.getElementById('loginForm');
  const loginEmail = document.getElementById('loginEmail');
  const loginPassword = document.getElementById('loginPassword');
  const loginSubmitBtn = document.getElementById('loginSubmitBtn');
  const loginFeedback = document.getElementById('loginFeedback');

  const portalContainer = document.getElementById('portalContainer');
  const organizerDashboard = document.getElementById('organizerDashboard');
  const reviewerDashboard = document.getElementById('reviewerDashboard');
  const volunteerDashboard = document.getElementById('volunteerDashboard');

  const organizerFeedback = document.getElementById('organizerFeedback');
  const myCampsList = document.getElementById('myCampsList');
  const showCreateCampBtn = document.getElementById('showCreateCampBtn');
  const createCampFormCard = document.getElementById('createCampFormCard');
  const campFormTitle = document.getElementById('campFormTitle');
  const campForm = document.getElementById('campForm');
  const editCampId = document.getElementById('editCampId');
  const newCampName = document.getElementById('newCampName');
  const newCampDate = document.getElementById('newCampDate');
  const newCampArea = document.getElementById('newCampArea');
  const newCampVenue = document.getElementById('newCampVenue');
  const saveCampBtn = document.getElementById('saveCampBtn');
  const cancelCampFormBtn = document.getElementById('cancelCampFormBtn');

  const manageCountersCard = document.getElementById('manageCountersCard');
  const counterManagerTitle = document.getElementById('counterManagerTitle');
  const closeCounterManagerBtn = document.getElementById('closeCounterManagerBtn');
  const counterFeedback = document.getElementById('counterFeedback');
  const counterForm = document.getElementById('counterForm') || document.getElementById('addCounterForm');
  const counterCampId = document.getElementById('counterCampId');
  const counterName = document.getElementById('counterName');
  const counterFee = document.getElementById('counterFee');
  const counterStartTime = document.getElementById('counterStartTime');
  const counterEndTime = document.getElementById('counterEndTime');
  const counterBookingMode = document.getElementById('counterBookingMode');
  const counterCapacity = document.getElementById('counterCapacity');
  const counterItems = document.getElementById('counterItems');
  const saveCounterBtn = document.getElementById('saveCounterBtn');
  const campCountersList = document.getElementById('campCountersList') || document.getElementById('existingCountersList');

  const reviewerFeedback = document.getElementById('reviewerFeedback');
  const pendingCampsList = document.getElementById('pendingCampsList');
  const pendingPostersList = document.getElementById('pendingPostersList');
  const refreshReviewBtn = document.getElementById('refreshReviewBtn');

  const checkInForm = document.getElementById('checkInForm');
  const checkInTokenInput = document.getElementById('checkInTokenInput');
  const confirmCheckInBtn = document.getElementById('confirmCheckInBtn') || document.getElementById('checkInBtn');
  const checkInFeedback = document.getElementById('checkInFeedback');

  const walkInForm = document.getElementById('walkInForm');
  const walkInCampSelect = document.getElementById('walkInCampSelect');
  const walkInServiceSelect = document.getElementById('walkInServiceSelect') || document.getElementById('walkInSessionSelect');
  const walkInVisitorName = document.getElementById('walkInVisitorName');
  const walkInVisitorPhone = document.getElementById('walkInVisitorPhone');
  const confirmWalkInBtn = document.getElementById('confirmWalkInBtn') || document.getElementById('walkInBtn');
  const walkInFeedback = document.getElementById('walkInFeedback');

  const attendanceCampSelect = document.getElementById('attendanceCampSelect');
  const refreshAttendanceBtn = document.getElementById('refreshAttendanceBtn');
  const attendanceStatsGrid = document.getElementById('attendanceStatsGrid');
  const totalCheckedInCount = document.getElementById('totalCheckedInCount');
  const onlineCheckedInCount = document.getElementById('onlineCheckedInCount');
  const walkInCheckedInCount = document.getElementById('walkInCheckedInCount');
  const attendanceFeedback = document.getElementById('attendanceFeedback');

  let currentStaff = null;
  let cachedApprovedCamps = [];

  document.querySelectorAll('.quick-login').forEach((btn) => {
    btn.addEventListener('click', () => {
      loginEmail.value = btn.getAttribute('data-email');
      loginPassword.value = btn.getAttribute('data-pass');
      loginForm.dispatchEvent(new Event('submit'));
    });
  });

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginSubmitBtn.disabled = true;
    loginSubmitBtn.textContent = 'Signing In...';
    loginFeedback.innerHTML = '';

    try {
      await apiRequest('/api/sessions', {
        method: 'POST',
        body: JSON.stringify({
          email: loginEmail.value.trim(),
          password: loginPassword.value
        })
      });

      loginForm.reset();
      await initializeStaffSession();
    } catch (err) {
      loginFeedback.innerHTML = `
        <div class="alert alert-error">
          <strong>Authentication Failed:</strong> ${escapeHtml(err.message)}
        </div>
      `;
    } finally {
      loginSubmitBtn.disabled = false;
      loginSubmitBtn.textContent = 'Sign In';
    }
  });

  logoutBtn.addEventListener('click', async () => {
    try {
      await apiRequest('/api/sessions', { method: 'DELETE' });
    } catch (err) {
    }
    currentStaff = null;
    staffHeaderBar.style.display = 'none';
    portalContainer.style.display = 'none';
    organizerDashboard.style.display = 'none';
    reviewerDashboard.style.display = 'none';
    volunteerDashboard.style.display = 'none';
    loginSection.style.display = 'block';
  });

  async function initializeStaffSession() {
    currentStaff = await checkCurrentSession();
    if (!currentStaff) {
      staffHeaderBar.style.display = 'none';
      portalContainer.style.display = 'none';
      loginSection.style.display = 'block';
      return;
    }

    loginSection.style.display = 'none';
    staffHeaderBar.style.display = 'flex';
    portalContainer.style.display = 'block';

    currentStaffEmail.textContent = currentStaff.email;
    currentStaffRole.textContent = currentStaff.role;
    currentStaffOrg.textContent = currentStaff.organizerName ? `(${currentStaff.organizerName})` : '';

    organizerDashboard.style.display = 'none';
    reviewerDashboard.style.display = 'none';
    volunteerDashboard.style.display = 'none';

    if (currentStaff.role === 'ORGANIZER') {
      organizerDashboard.style.display = 'block';
      loadOrganizerCamps();
    } else if (currentStaff.role === 'REVIEWER') {
      reviewerDashboard.style.display = 'block';
      loadReviewQueue();
    } else if (currentStaff.role === 'VOLUNTEER') {
      volunteerDashboard.style.display = 'block';
      initVolunteerDashboard();
    }
  }

  showCreateCampBtn.addEventListener('click', () => {
    campForm.reset();
    editCampId.value = '';
    campFormTitle.textContent = 'Create New Camp Draft';
    createCampFormCard.style.display = 'block';
    manageCountersCard.style.display = 'none';
    newCampName.focus();
  });

  cancelCampFormBtn.addEventListener('click', () => {
    createCampFormCard.style.display = 'none';
    campForm.reset();
  });

  campForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    saveCampBtn.disabled = true;
    saveCampBtn.textContent = 'Saving...';
    organizerFeedback.innerHTML = '';

    const payload = {
      name: newCampName.value.trim(),
      campDate: newCampDate.value,
      area: newCampArea.value.trim(),
      venue: newCampVenue.value.trim()
    };

    const isEdit = Boolean(editCampId.value);
    const url = isEdit ? `/api/camps/${editCampId.value}` : '/api/camps';
    const method = isEdit ? 'PUT' : 'POST';

    try {
      await apiRequest(url, {
        method: method,
        body: JSON.stringify(payload)
      });

      createCampFormCard.style.display = 'none';
      campForm.reset();
      organizerFeedback.innerHTML = `
        <div class="alert alert-success">
          Camp draft ${isEdit ? 'updated' : 'created'} successfully.
        </div>
      `;
      loadOrganizerCamps();
    } catch (err) {
      organizerFeedback.innerHTML = `
        <div class="alert alert-error">
          <strong>Failed to save camp:</strong> ${escapeHtml(err.message)}
        </div>
      `;
    } finally {
      saveCampBtn.disabled = false;
      saveCampBtn.textContent = 'Save Camp Draft';
    }
  });

  async function loadOrganizerCamps() {
    myCampsList.innerHTML = '<div class="loading">Loading your camps...</div>';

    try {
      const camps = await apiRequest('/api/camps/my');
      myCampsList.innerHTML = '';

      if (!camps || camps.length === 0) {
        myCampsList.innerHTML = `
          <div class="empty-state">
            <h3>No Camps Created Yet</h3>
            <p>You have not organized any camps yet. Click "+ Create New Camp Draft" to begin.</p>
          </div>
        `;
        return;
      }

      camps.forEach((camp) => {
        const card = document.createElement('article');
        card.className = 'card';

        let badgeClass = 'badge-draft';
        if (camp.status === 'APPROVED') badgeClass = 'badge-approved';
        if (camp.status === 'PENDING_REVIEW') badgeClass = 'badge-pending';
        if (camp.status === 'CANCELLED') badgeClass = 'badge-cancelled';

        let feedbackHtml = '';
        if (camp.status === 'DRAFT' && camp.rejectionReason) {
          feedbackHtml = `
            <div class="alert alert-warning" style="margin-top: 0.75rem;">
              <strong>Reviewer Feedback for Corrections:</strong> ${escapeHtml(camp.rejectionReason)}
            </div>
          `;
        }

        let countersHtml = '';
        const count = camp.services ? camp.services.length : 0;
        if (count > 0) {
          const list = camp.services.map(s => `${escapeHtml(s.serviceName)} (${formatTime(s.startTime)}-${formatTime(s.endTime)})`).join(', ');
          countersHtml = `<div style="font-size: 0.85rem; color: #4b5563; margin-top: 0.5rem;"><strong>Service Counters (${count}):</strong> ${list}</div>`;
        } else {
          countersHtml = `<div style="font-size: 0.85rem; color: #b45309; margin-top: 0.5rem;"><strong>No counters added yet.</strong> Add at least one service counter before submitting for review.</div>`;
        }

        let actionsHtml = '';
        if (camp.status === 'DRAFT') {
          actionsHtml = `
            <button type="button" class="btn btn-secondary btn-sm manage-counters-btn" data-id="${camp.id}" data-name="${escapeHtml(camp.name)}">Manage Counters (${count})</button>
            <button type="button" class="btn btn-primary btn-sm submit-camp-btn" data-id="${camp.id}" ${count === 0 ? 'disabled' : ''}>Submit for Review</button>
            <button type="button" class="btn btn-secondary btn-sm edit-camp-btn" data-id="${camp.id}" data-name="${escapeHtml(camp.name)}" data-date="${camp.campDate}" data-area="${escapeHtml(camp.area)}" data-venue="${escapeHtml(camp.venue)}">Edit</button>
            <button type="button" class="btn btn-danger btn-sm delete-camp-btn" data-id="${camp.id}">Delete Draft</button>
          `;
        } else if (camp.status === 'APPROVED') {
          actionsHtml = `
            <a href="camp-details.html?id=${camp.id}" class="btn btn-secondary btn-sm" target="_blank">View Public Listing</a>
            <button type="button" class="btn btn-danger btn-sm cancel-camp-btn" data-id="${camp.id}">Cancel Camp</button>
          `;
        } else if (camp.status === 'PENDING_REVIEW') {
          actionsHtml = `<span style="font-size: 0.85rem; color: #92400e; font-weight: 600;">Currently undergoing reviewer inspection.</span>`;
        }

        card.innerHTML = `
          <div class="card-title">
            <div>
              <h3 style="font-size: 1.15rem; margin-bottom: 0.2rem;">${escapeHtml(camp.name)}</h3>
              <div style="font-size: 0.9rem; color: #4b5563;">Date: <strong>${formatDate(camp.campDate)}</strong> | Area: ${escapeHtml(camp.area)} | Venue: ${escapeHtml(camp.venue)}</div>
            </div>
            <span class="badge ${badgeClass}">${camp.status}</span>
          </div>

          ${feedbackHtml}
          ${countersHtml}

          <div class="btn-group" style="margin-top: 1rem; justify-content: flex-end;">
            ${actionsHtml}
          </div>
        `;

        myCampsList.appendChild(card);
      });

      attachOrganizerCampListeners();
    } catch (err) {
      myCampsList.innerHTML = `
        <div class="alert alert-error">
          Failed to load your camps: ${escapeHtml(err.message)}
        </div>
      `;
    }
  }

  function attachOrganizerCampListeners() {
    document.querySelectorAll('.manage-counters-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        const name = e.target.getAttribute('data-name');
        openCounterManager(id, name);
      });
    });

    document.querySelectorAll('.submit-camp-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        btn.disabled = true;
        btn.textContent = 'Submitting...';
        try {
          await apiRequest(`/api/camps/${id}/submit`, { method: 'POST' });
          organizerFeedback.innerHTML = `<div class="alert alert-success">Camp submitted for review successfully! Status is now PENDING_REVIEW.</div>`;
          loadOrganizerCamps();
        } catch (err) {
          organizerFeedback.innerHTML = `<div class="alert alert-error">Submission failed: ${escapeHtml(err.message)}</div>`;
          btn.disabled = false;
          btn.textContent = 'Submit for Review';
        }
      });
    });

    document.querySelectorAll('.edit-camp-btn').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        editCampId.value = e.target.getAttribute('data-id');
        newCampName.value = e.target.getAttribute('data-name');
        newCampDate.value = e.target.getAttribute('data-date');
        newCampArea.value = e.target.getAttribute('data-area');
        newCampVenue.value = e.target.getAttribute('data-venue');
        campFormTitle.textContent = 'Edit Camp Draft #' + editCampId.value;
        createCampFormCard.style.display = 'block';
        manageCountersCard.style.display = 'none';
        newCampName.focus();
        createCampFormCard.scrollIntoView({ behavior: 'smooth' });
      });
    });

    document.querySelectorAll('.delete-camp-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        if (!window.confirm('Are you sure you want to delete this draft camp?')) return;
        try {
          await apiRequest(`/api/camps/${id}`, { method: 'DELETE' });
          organizerFeedback.innerHTML = `<div class="alert alert-success">Camp draft deleted.</div>`;
          loadOrganizerCamps();
        } catch (err) {
          organizerFeedback.innerHTML = `<div class="alert alert-error">Deletion failed: ${escapeHtml(err.message)}</div>`;
        }
      });
    });

    document.querySelectorAll('.cancel-camp-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        if (!window.confirm('Are you sure you want to cancel this approved camp? Bookings will be rejected.')) return;
        try {
          await apiRequest(`/api/camps/${id}/status`, { method: 'PATCH' });
          organizerFeedback.innerHTML = `<div class="alert alert-success">Camp has been cancelled.</div>`;
          loadOrganizerCamps();
        } catch (err) {
          organizerFeedback.innerHTML = `<div class="alert alert-error">Cancellation failed: ${escapeHtml(err.message)}</div>`;
        }
      });
    });
  }

  async function openCounterManager(campId, campName) {
    createCampFormCard.style.display = 'none';
    counterCampId.value = campId;
    counterManagerTitle.textContent = 'Manage Counters — ' + campName;
    manageCountersCard.style.display = 'block';
    manageCountersCard.scrollIntoView({ behavior: 'smooth' });
    counterFeedback.innerHTML = '';
    loadCampCounters(campId);
  }

  closeCounterManagerBtn.addEventListener('click', () => {
    manageCountersCard.style.display = 'none';
    loadOrganizerCamps();
  });

  async function loadCampCounters(campId) {
    campCountersList.innerHTML = '<div class="loading">Loading counters...</div>';

    try {
      const counters = await apiRequest(`/api/camps/${campId}/services`);
      campCountersList.innerHTML = '';

      if (!counters || counters.length === 0) {
        campCountersList.innerHTML = `<p style="color: #6b7280; font-size: 0.9rem;">No counters created yet. Add one above.</p>`;
        return;
      }

      counters.forEach((s) => {
        const item = document.createElement('div');
        item.className = 'card';
        item.style.padding = '0.75rem 1rem';
        item.style.marginBottom = '0.75rem';
        item.style.backgroundColor = '#f9fafb';

        const hours = formatTime(s.startTime) + ' - ' + formatTime(s.endTime);
        item.innerHTML = `
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
            <div>
              <strong style="font-size: 1rem; color: #1b5e20;">${escapeHtml(s.serviceName)}</strong>
              <div style="font-size: 0.85rem; color: #4b5563;">
                Hours: ${hours} | Mode: ${s.bookingMode} | Online Capacity: ${s.onlineCapacity} | Fee: ${formatFee(s.fee)}
              </div>
              <div style="font-size: 0.85rem; color: #6b7280;">Items: ${escapeHtml(s.itemsToBring || 'None')}</div>
            </div>
            <div>
              <button type="button" class="btn btn-danger btn-sm delete-counter-btn" data-id="${s.id}">Remove</button>
            </div>
          </div>
        `;
        campCountersList.appendChild(item);
      });

      document.querySelectorAll('.delete-counter-btn').forEach((btn) => {
        btn.addEventListener('click', async (e) => {
          const sid = e.target.getAttribute('data-id');
          if (!window.confirm('Delete this service counter?')) return;
          try {
            await apiRequest(`/api/services/${sid}`, { method: 'DELETE' });
            loadCampCounters(counterCampId.value);
          } catch (err) {
            counterFeedback.innerHTML = `<div class="alert alert-error">Failed to delete counter: ${escapeHtml(err.message)}</div>`;
          }
        });
      });
    } catch (err) {
      campCountersList.innerHTML = `<div class="alert alert-error">Failed to load counters: ${escapeHtml(err.message)}</div>`;
    }
  }

  counterForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    saveCounterBtn.disabled = true;
    saveCounterBtn.textContent = 'Adding Counter...';
    counterFeedback.innerHTML = '';

    const payload = {
      serviceName: counterName.value.trim(),
      startTime: counterStartTime.value + (counterStartTime.value.length === 5 ? ':00' : ''),
      endTime: counterEndTime.value + (counterEndTime.value.length === 5 ? ':00' : ''),
      fee: parseFloat(counterFee.value || '0'),
      itemsToBring: counterItems.value.trim() || 'None',
      bookingMode: counterBookingMode.value,
      onlineCapacity: parseInt(counterCapacity.value, 10),
      capacity: parseInt(counterCapacity.value, 10)
    };

    try {
      await apiRequest(`/api/camps/${counterCampId.value}/services`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      counterForm.reset();
      counterFee.value = '0.00';
      counterStartTime.value = '09:00';
      counterEndTime.value = '13:00';
      counterCapacity.value = '20';
      counterBookingMode.value = 'BOTH';

      counterFeedback.innerHTML = `<div class="alert alert-success">Service counter added successfully.</div>`;
      loadCampCounters(counterCampId.value);
    } catch (err) {
      counterFeedback.innerHTML = `<div class="alert alert-error">Failed to add counter: ${escapeHtml(err.message)}</div>`;
    } finally {
      saveCounterBtn.disabled = false;
      saveCounterBtn.textContent = 'Add Counter to Camp';
    }
  });

  if (refreshReviewBtn) {
    refreshReviewBtn.addEventListener('click', () => {
      loadReviewQueue();
    });
  }

  async function loadReviewQueue() {
    pendingCampsList.innerHTML = '<div class="loading">Loading review queue...</div>';
    pendingPostersList.innerHTML = '<div class="loading">Loading poster checks...</div>';
    reviewerFeedback.innerHTML = '';

    try {
      const queue = await apiRequest('/api/review-queue');

      renderPendingCamps(queue.pendingCamps || []);
      renderPendingPosters(queue.pendingPosters || []);
    } catch (err) {
      reviewerFeedback.innerHTML = `<div class="alert alert-error">Failed to load review queue: ${escapeHtml(err.message)}</div>`;
    }
  }

  function renderPendingCamps(camps) {
    pendingCampsList.innerHTML = '';

    if (camps.length === 0) {
      pendingCampsList.innerHTML = `
        <div class="empty-state">
          <h3>No Pending Camp Submissions</h3>
          <p>All submitted medical camps have been reviewed.</p>
        </div>
      `;
      return;
    }

    camps.forEach((camp) => {
      const card = document.createElement('article');
      card.className = 'card';
      card.style.borderLeft = '4px solid #b45309';

      let servicesHtml = '';
      if (camp.services && camp.services.length > 0) {
        servicesHtml = camp.services.map(s => `<li><strong>${escapeHtml(s.serviceName)}</strong> (${formatTime(s.startTime)} - ${formatTime(s.endTime)}, ${s.bookingMode}, Capacity: ${s.onlineCapacity})</li>`).join('');
        servicesHtml = `<ul style="margin-left: 1.25rem; font-size: 0.9rem; margin-top: 0.35rem;">${servicesHtml}</ul>`;
      } else {
        servicesHtml = '<p style="color: #b91c1c; font-size: 0.85rem;">No counters attached.</p>';
      }

      card.innerHTML = `
        <div class="card-title">
          <div>
            <h3 style="font-size: 1.2rem; margin-bottom: 0.2rem;">${escapeHtml(camp.name)}</h3>
            <div style="font-size: 0.9rem; color: #4b5563;">Organized by: <strong>${escapeHtml(camp.organizerName || 'Unknown')}</strong></div>
          </div>
          <span class="badge badge-pending">Pending Review</span>
        </div>

        <div class="form-row" style="margin: 0.5rem 0; font-size: 0.95rem;">
          <div><strong>Date:</strong> ${formatDate(camp.campDate)}</div>
          <div><strong>Area:</strong> ${escapeHtml(camp.area)}</div>
          <div><strong>Venue:</strong> ${escapeHtml(camp.venue)}</div>
        </div>

        <div style="margin-top: 0.75rem;">
          <strong>Counters / Services:</strong>
          ${servicesHtml}
        </div>

        <div style="margin-top: 1.25rem; padding-top: 1rem; border-top: 1px solid #dcdcdc;">
          <div class="grid-2">
            <div style="background-color: #f0fdf4; padding: 0.85rem; border-radius: 4px; border: 1px solid #bbf7d0;">
              <h4 style="font-size: 0.95rem; color: #166534; margin-bottom: 0.5rem;">Action: Approve Camp</h4>
              <div class="form-group compact">
                <label for="source_${camp.id}">Official Verification Source *</label>
                <input type="text" id="source_${camp.id}" placeholder="e.g. Health Dept Circular #4421" required>
              </div>
              <button type="button" class="btn btn-primary btn-sm approve-camp-btn" data-id="${camp.id}" style="margin-top: 0.5rem;">Approve & Publish</button>
            </div>

            <div style="background-color: #fef2f2; padding: 0.85rem; border-radius: 4px; border: 1px solid #fecaca;">
              <h4 style="font-size: 0.95rem; color: #991b1b; margin-bottom: 0.5rem;">Action: Return for Corrections</h4>
              <div class="form-group compact">
                <label for="reason_${camp.id}">Correction Feedback Reason *</label>
                <input type="text" id="reason_${camp.id}" placeholder="e.g. Specify wheelchair access and room number" required>
              </div>
              <button type="button" class="btn btn-danger btn-sm return-camp-btn" data-id="${camp.id}" style="margin-top: 0.5rem;">Return to Draft</button>
            </div>
          </div>
        </div>
      `;

      pendingCampsList.appendChild(card);
    });

    document.querySelectorAll('.approve-camp-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        const sourceInput = document.getElementById('source_' + id);
        const source = sourceInput.value.trim();

        if (!source) {
          alert('Official verification source or reference number is required to approve a camp.');
          sourceInput.focus();
          return;
        }

        btn.disabled = true;
        btn.textContent = 'Approving...';
        try {
          await apiRequest(`/api/camps/${id}/review`, {
            method: 'PATCH',
            body: JSON.stringify({
              action: 'APPROVE',
              verificationSource: source
            })
          });
          reviewerFeedback.innerHTML = `<div class="alert alert-success">Camp #${id} approved and published successfully.</div>`;
          loadReviewQueue();
        } catch (err) {
          reviewerFeedback.innerHTML = `<div class="alert alert-error">Approval failed: ${escapeHtml(err.message)}</div>`;
          btn.disabled = false;
          btn.textContent = 'Approve & Publish';
        }
      });
    });

    document.querySelectorAll('.return-camp-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        const reasonInput = document.getElementById('reason_' + id);
        const reason = reasonInput.value.trim();

        if (!reason) {
          alert('A specific reason is required when returning a camp for corrections.');
          reasonInput.focus();
          return;
        }

        btn.disabled = true;
        btn.textContent = 'Returning...';
        try {
          await apiRequest(`/api/camps/${id}/review`, {
            method: 'PATCH',
            body: JSON.stringify({
              action: 'RETURN_FOR_CORRECTIONS',
              reason: reason
            })
          });
          reviewerFeedback.innerHTML = `<div class="alert alert-warning">Camp #${id} returned to organizer draft with feedback.</div>`;
          loadReviewQueue();
        } catch (err) {
          reviewerFeedback.innerHTML = `<div class="alert alert-error">Action failed: ${escapeHtml(err.message)}</div>`;
          btn.disabled = false;
          btn.textContent = 'Return to Draft';
        }
      });
    });
  }

  function renderPendingPosters(posters) {
    pendingPostersList.innerHTML = '';

    if (posters.length === 0) {
      pendingPostersList.innerHTML = `
        <div class="empty-state">
          <h3>No Posters Awaiting Manual Review</h3>
          <p>No unconfirmed poster checks are currently queued.</p>
        </div>
      `;
      return;
    }

    posters.forEach((p) => {
      const card = document.createElement('article');
      card.className = 'card';

      card.innerHTML = `
        <div class="card-title">
          <div>
            <h3 style="font-size: 1.1rem; margin-bottom: 0.2rem;">Poster Check: ${escapeHtml(p.posterCheckId)}</h3>
            <div style="font-size: 0.85rem; color: #4b5563;">File: ${escapeHtml(p.originalFileName || 'Uploaded Image')} | Submitted: ${p.createdAt || '-'}</div>
          </div>
          <span class="badge badge-pending">Manual Review</span>
        </div>

        <div style="background-color: #f9fafb; padding: 0.75rem; border-radius: 4px; border: 1px solid #e5e7eb; margin: 0.5rem 0; font-size: 0.85rem;">
          <div><strong>Suggested Title:</strong> ${escapeHtml(p.suggestedCampName || '-')}</div>
          <div><strong>Suggested Organizer:</strong> ${escapeHtml(p.suggestedOrganizer || '-')}</div>
          <div><strong>Suggested Date:</strong> ${formatDate(p.suggestedCampDate)}</div>
          <div><strong>Suggested Venue:</strong> ${escapeHtml(p.suggestedVenue || '-')}</div>
        </div>

        <div style="margin-top: 1rem; padding-top: 0.75rem; border-top: 1px solid #dcdcdc;">
          <div class="form-row">
            <div class="form-group compact">
              <label for="pstatus_${p.posterCheckId}">Decision Status *</label>
              <select id="pstatus_${p.posterCheckId}">
                <option value="MATCHED">MATCHED (Legitimate & Verified)</option>
                <option value="DISCREPANCIES">DISCREPANCIES (Partial Match)</option>
                <option value="UNABLE_TO_CONFIRM">UNABLE_TO_CONFIRM (No Record)</option>
              </select>
            </div>
            <div class="form-group compact">
              <label for="pnotes_${p.posterCheckId}">Review Notes</label>
              <input type="text" id="pnotes_${p.posterCheckId}" placeholder="e.g. Verified with primary health center circular">
            </div>
          </div>
          <button type="button" class="btn btn-primary btn-sm save-poster-review-btn" data-id="${p.posterCheckId}" style="margin-top: 0.5rem;">Save Poster Decision</button>
        </div>
      `;

      pendingPostersList.appendChild(card);
    });

    document.querySelectorAll('.save-poster-review-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.getAttribute('data-id');
        const statusSelect = document.getElementById('pstatus_' + id);
        const notesInput = document.getElementById('pnotes_' + id);

        btn.disabled = true;
        btn.textContent = 'Saving...';
        try {
          await apiRequest(`/api/poster-checks/${id}/review`, {
            method: 'PATCH',
            body: JSON.stringify({
              matchStatus: statusSelect.value,
              reviewNotes: notesInput.value.trim()
            })
          });
          reviewerFeedback.innerHTML = `<div class="alert alert-success">Poster check ${id} decision recorded.</div>`;
          loadReviewQueue();
        } catch (err) {
          reviewerFeedback.innerHTML = `<div class="alert alert-error">Review failed: ${escapeHtml(err.message)}</div>`;
          btn.disabled = false;
          btn.textContent = 'Save Poster Decision';
        }
      });
    });
  }

  async function initVolunteerDashboard() {
    try {
      cachedApprovedCamps = await apiRequest('/api/camps');
      populateVolunteerCampDropdowns();
    } catch (err) {
      attendanceFeedback.innerHTML = `<div class="alert alert-error">Failed to load approved camps list: ${escapeHtml(err.message)}</div>`;
    }
  }

  function populateVolunteerCampDropdowns() {
    walkInCampSelect.innerHTML = '<option value="">-- Choose Camp --</option>';
    attendanceCampSelect.innerHTML = '<option value="">-- Choose Camp --</option>';

    cachedApprovedCamps.forEach((camp) => {
      const opt1 = document.createElement('option');
      opt1.value = camp.id;
      opt1.textContent = `${camp.name} (${formatDate(camp.campDate)} - ${camp.venue})`;
      walkInCampSelect.appendChild(opt1);

      const opt2 = document.createElement('option');
      opt2.value = camp.id;
      opt2.textContent = `${camp.name} (${formatDate(camp.campDate)} - ${camp.area})`;
      attendanceCampSelect.appendChild(opt2);
    });

    if (cachedApprovedCamps.length > 0) {
      attendanceCampSelect.value = cachedApprovedCamps[0].id;
      loadAttendanceTotals(cachedApprovedCamps[0].id);
    }
  }

  walkInCampSelect.addEventListener('change', () => {
    const selectedCampId = walkInCampSelect.value;
    walkInServiceSelect.innerHTML = '<option value="">-- Choose Counter --</option>';

    if (!selectedCampId) {
      walkInServiceSelect.disabled = true;
      return;
    }

    const camp = cachedApprovedCamps.find(c => String(c.id) === String(selectedCampId));
    if (!camp || !camp.services || camp.services.length === 0) {
      walkInServiceSelect.innerHTML = '<option value="">No counters available</option>';
      walkInServiceSelect.disabled = true;
      return;
    }

    const walkInCounters = camp.services.filter(s => s.bookingMode === 'WALK_IN' || s.bookingMode === 'BOTH');
    if (walkInCounters.length === 0) {
      walkInServiceSelect.innerHTML = '<option value="">No walk-in counters in this camp</option>';
      walkInServiceSelect.disabled = true;
      return;
    }

    walkInCounters.forEach((s) => {
      const opt = document.createElement('option');
      opt.value = s.id;
      opt.textContent = `${s.serviceName} (${formatTime(s.startTime)} - ${formatTime(s.endTime)})`;
      walkInServiceSelect.appendChild(opt);
    });
    walkInServiceSelect.disabled = false;
  });

  checkInForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const token = checkInTokenInput.value.trim().toUpperCase();
    if (!token) return;

    confirmCheckInBtn.disabled = true;
    confirmCheckInBtn.textContent = 'Checking in...';
    checkInFeedback.innerHTML = '';

    try {
      const resp = await apiRequest(`/api/bookings/${encodeURIComponent(token)}/check-in`, {
        method: 'PATCH'
      });

      checkInFeedback.innerHTML = `
        <div class="alert alert-success">
          <p><strong>Check-in Successful!</strong></p>
          <div style="font-size: 0.9rem; margin-top: 0.35rem;">
            Token: <strong>${escapeHtml(resp.tokenNumber)}</strong> (${escapeHtml(resp.bookingReference)})<br>
            Resident: <strong>${escapeHtml(resp.visitorName)}</strong><br>
            Service: <strong>${escapeHtml(resp.serviceName)}</strong><br>
            Checked in at: <strong>${formatTime(resp.checkInTime ? resp.checkInTime.split('T')[1] || resp.checkInTime : '')}</strong>
          </div>
        </div>
      `;
      checkInForm.reset();

      if (attendanceCampSelect.value) {
        loadAttendanceTotals(attendanceCampSelect.value);
      }
    } catch (err) {
      if (err.status === 409) {
        checkInFeedback.innerHTML = `
          <div class="alert alert-error">
            <strong>Duplicate Check-In Rejected:</strong> This token has already been checked in.
          </div>
        `;
      } else {
        checkInFeedback.innerHTML = `
          <div class="alert alert-error">
            <strong>Check-in failed:</strong> ${escapeHtml(err.message)}
          </div>
        `;
      }
    } finally {
      confirmCheckInBtn.disabled = false;
      confirmCheckInBtn.textContent = 'Check In Visitor';
    }
  });

  walkInForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const sessionId = walkInServiceSelect.value;
    const name = walkInVisitorName.value.trim();
    const phone = walkInVisitorPhone.value.trim();

    if (!sessionId || !name || !phone) {
      walkInFeedback.innerHTML = `<div class="alert alert-error">Please select a counter and enter visitor name and phone number.</div>`;
      return;
    }

    confirmWalkInBtn.disabled = true;
    confirmWalkInBtn.textContent = 'Registering Walk-in...';
    walkInFeedback.innerHTML = '';

    try {
      const booking = await apiRequest(`/api/services/${sessionId}/walk-ins`, {
        method: 'POST',
        body: JSON.stringify({
          visitorName: name,
          visitorPhone: phone
        })
      });

      walkInFeedback.innerHTML = `
        <div class="alert alert-success">
          <p><strong>Walk-in Registered & Checked In!</strong></p>
          <div style="font-size: 0.9rem; margin-top: 0.35rem;">
            Assigned Token: <strong style="font-size: 1.1rem; color: #166534;">${escapeHtml(booking.tokenNumber)}</strong> (${escapeHtml(booking.bookingReference)})<br>
            Resident: <strong>${escapeHtml(booking.visitorName)}</strong><br>
            Service: <strong>${escapeHtml(booking.serviceName)}</strong><br>
            Status: <strong>Checked In</strong> (Walk-in slots do not consume online quota)
          </div>
        </div>
      `;

      walkInVisitorName.value = '';
      walkInVisitorPhone.value = '';

      if (attendanceCampSelect.value) {
        loadAttendanceTotals(attendanceCampSelect.value);
      }
    } catch (err) {
      walkInFeedback.innerHTML = `
        <div class="alert alert-error">
          <strong>Registration failed:</strong> ${escapeHtml(err.message)}
        </div>
      `;
    } finally {
      confirmWalkInBtn.disabled = false;
      confirmWalkInBtn.textContent = 'Register & Check In Walk-in';
    }
  });

  attendanceCampSelect.addEventListener('change', () => {
    const cid = attendanceCampSelect.value;
    if (cid) {
      loadAttendanceTotals(cid);
    } else {
      attendanceStatsGrid.style.display = 'none';
    }
  });

  refreshAttendanceBtn.addEventListener('click', () => {
    const cid = attendanceCampSelect.value;
    if (cid) {
      loadAttendanceTotals(cid);
    }
  });

  async function loadAttendanceTotals(campId) {
    attendanceFeedback.innerHTML = '<div class="loading">Loading attendance metrics...</div>';

    try {
      const stats = await apiRequest(`/api/camps/${campId}/attendance`);
      attendanceFeedback.innerHTML = '';

      const totalCheckedIn = (stats.counters || []).reduce((sum, c) => sum + (c.totalCheckedIn || 0), 0);
      const onlineCheckedIn = (stats.counters || []).reduce((sum, c) => sum + (c.onlineCheckedIn || 0), 0);
      const walkInCheckedIn = (stats.counters || []).reduce((sum, c) => sum + (c.walkInCount || 0), 0);

      totalCheckedInCount.textContent = stats.totalCheckedIn !== undefined ? stats.totalCheckedIn : totalCheckedIn;
      onlineCheckedInCount.textContent = stats.onlineCheckedIn !== undefined ? stats.onlineCheckedIn : onlineCheckedIn;
      walkInCheckedInCount.textContent = stats.walkInCount !== undefined ? stats.walkInCount : walkInCheckedIn;

      attendanceStatsGrid.style.display = 'grid';
    } catch (err) {
      attendanceFeedback.innerHTML = `
        <div class="alert alert-error">
          Failed to load attendance totals: ${escapeHtml(err.message)}
        </div>
      `;
      attendanceStatsGrid.style.display = 'none';
    }
  }

  initializeStaffSession();
});