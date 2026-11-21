document.addEventListener("DOMContentLoaded", function() {
    const tzSelect = document.getElementById("defaultTimezone");
    if (!tzSelect) {
        return;
    }
    try {
        const detected = Intl.DateTimeFormat().resolvedOptions().timeZone;
        if (!detected) {
            return;
        }
        const hasOption = Array.from(tzSelect.options).some(function(opt) {
            return opt.value === detected;
        });
        if (hasOption && (!tzSelect.value || tzSelect.value === "UTC")) {
            tzSelect.value = detected;
        }
    } catch (e) {
        // Fallback to server-selected default timezone if Intl API is unavailable
    }
});