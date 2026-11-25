async function apiRequest(endpoint, options = {}) {
  const defaultHeaders = {};
  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers || {})
    }
  };

  try {
    const response = await fetch(endpoint, config);

    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      let errorMessage = 'Request failed (' + response.status + ')';
      if (typeof data === 'object' && data !== null) {
        errorMessage = data.reason || data.message || data.error || errorMessage;
      } else if (typeof data === 'string' && data.trim().length > 0) {
        errorMessage = data;
      }
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (err) {
    if (err.status) {
      throw err;
    }
    const networkError = new Error('Network error: Unable to reach the server. Please check your connection and retry.');
    networkError.status = 0;
    networkError.isNetworkError = true;
    throw networkError;
  }
}

function formatDate(isoDate) {
  if (!isoDate) return '-';
  const parts = isoDate.split('-');
  if (parts.length !== 3) return isoDate;
  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1;
  const day = parseInt(parts[2], 10);
  const date = new Date(year, month, day);
  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
}

function formatTime(timeStr) {
  if (!timeStr) return '-';
  const parts = timeStr.split(':');
  if (parts.length < 2) return timeStr;
  let hours = parseInt(parts[0], 10);
  const minutes = parts[1];
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12;
  hours = hours ? hours : 12;
  const formattedHours = hours < 10 ? '0' + hours : hours;
  return formattedHours + ':' + minutes + ' ' + ampm;
}

function formatFee(fee) {
  if (fee === null || fee === undefined || Number(fee) === 0) {
    return 'Free / ₹0.00';
  }
  return '₹' + Number(fee).toFixed(2);
}

function generateIdempotencyKey() {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 9);
  return 'IDEMP-' + timestamp + '-' + random;
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

async function checkCurrentSession() {
  try {
    return await apiRequest('/api/sessions/me');
  } catch (err) {
    return null;
  }
}

function updateGovClock() {
  const clockEl = document.getElementById('govClock');
  if (!clockEl) return;
  const now = new Date();
  const options = {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true
  };
  clockEl.textContent = now.toLocaleDateString('en-IN', options);
}

function initAccessibilityControls() {
  const btnNormal = document.getElementById('btnTextNormal');
  const btnLarge = document.getElementById('btnTextLarge');
  const btnXLarge = document.getElementById('btnTextXLarge');
  const btnContrast = document.getElementById('btnContrast');

  if (btnNormal) {
    btnNormal.addEventListener('click', () => {
      document.body.classList.remove('font-large', 'font-xlarge');
    });
  }

  if (btnLarge) {
    btnLarge.addEventListener('click', () => {
      document.body.classList.add('font-large');
      document.body.classList.remove('font-xlarge');
    });
  }

  if (btnXLarge) {
    btnXLarge.addEventListener('click', () => {
      document.body.classList.add('font-xlarge');
      document.body.classList.remove('font-large');
    });
  }

  if (btnContrast) {
    btnContrast.addEventListener('click', () => {
      document.body.classList.toggle('high-contrast');
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  updateGovClock();
  setInterval(updateGovClock, 1000);
  initAccessibilityControls();

  const currentPath = window.location.pathname;
  document.querySelectorAll('.gov-nav-inner a').forEach((link) => {
    const href = link.getAttribute('href');
    if (href && (currentPath.endsWith(href) || (href === 'index.html' && (currentPath === '/' || currentPath.endsWith('/'))))) {
      link.classList.add('active');
    }
  });
});
