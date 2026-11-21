(function() {
    function formatSegment(value) {
        return String(Math.max(0, value)).padStart(2, '0');
    }

    function updateCountdown(el) {
        const targetIso = el.getAttribute('data-unlock-utc');
        if (!targetIso) return false;

        const diffMs = Date.parse(targetIso) - Date.now();
        if (diffMs <= 0) {
            el.innerHTML = '<span class="badge text-bg-success fs-6 px-3 py-2">Unlock time reached! Refreshing...</span>';
            if (el.getAttribute('data-auto-reload') === 'true' && !el.dataset.reloaded) {
                el.dataset.reloaded = 'true';
                setTimeout(() => window.location.reload(), 1500);
            }
            return false;
        }

        const totalSeconds = Math.floor(diffMs / 1000);
        const days = Math.floor(totalSeconds / 86400);
        const hours = Math.floor((totalSeconds % 86400) / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;

        if (el.getAttribute('data-compact') === 'true') {
            el.textContent = `${days}d ${formatSegment(hours)}h ${formatSegment(minutes)}m ${formatSegment(seconds)}s`;
        } else {
            el.innerHTML = `
                <div class="d-flex justify-content-center gap-2 gap-sm-3 flex-wrap">
                    <div class="bg-white border rounded-3 px-3 py-2 text-center shadow-sm" style="min-width: 72px;">
                        <div class="fs-3 fw-bold text-primary">${days}</div>
                        <div class="small text-muted">Days</div>
                    </div>
                    <div class="bg-white border rounded-3 px-3 py-2 text-center shadow-sm" style="min-width: 72px;">
                        <div class="fs-3 fw-bold text-primary">${formatSegment(hours)}</div>
                        <div class="small text-muted">Hours</div>
                    </div>
                    <div class="bg-white border rounded-3 px-3 py-2 text-center shadow-sm" style="min-width: 72px;">
                        <div class="fs-3 fw-bold text-primary">${formatSegment(minutes)}</div>
                        <div class="small text-muted">Mins</div>
                    </div>
                    <div class="bg-white border rounded-3 px-3 py-2 text-center shadow-sm" style="min-width: 72px;">
                        <div class="fs-3 fw-bold text-primary">${formatSegment(seconds)}</div>
                        <div class="small text-muted">Secs</div>
                    </div>
                </div>`;
        }
        return true;
    }

    function init() {
        const elements = Array.from(document.querySelectorAll('[data-unlock-utc]'));
        if (elements.length === 0) return;

        elements.forEach(updateCountdown);
        setInterval(() => elements.forEach(updateCountdown), 1000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();