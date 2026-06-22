(function () {
    const table = document.querySelector("[data-matches-table]");

    if (!table || !table.tBodies.length) {
        return;
    }

    const tools = document.querySelector("[data-match-tools]");
    const showClosedControl = tools ? tools.querySelector("[data-show-closed-predictions]") : null;
    const tbody = table.tBodies[0];
    const rows = Array.from(tbody.querySelectorAll("[data-match-row]"));
    const noVisibleMatchesRow = tbody.querySelector("[data-no-visible-matches-row]");

    function isOpen(row) {
        return row.dataset.open === "true";
    }

    function applyClosedPredictionFilter() {
        const showClosed = showClosedControl ? showClosedControl.checked : false;
        let visibleCount = 0;

        rows.forEach(row => {
            const visible = showClosed || isOpen(row);
            row.hidden = !visible;
            if (visible) {
                visibleCount++;
            }
        });

        if (noVisibleMatchesRow) {
            noVisibleMatchesRow.hidden = rows.length === 0 || visibleCount > 0;
        }
    }

    function setStatus(status, message, type) {
        if (!status) {
            return;
        }
        status.textContent = message || "";
        status.classList.remove("success", "error");
        if (type) {
            status.classList.add(type);
        }
    }

    function syncPenaltyRequirement(form) {
        const penaltyWinner = form.elements.predictedPenaltyWinner;

        if (!penaltyWinner) {
            return;
        }

        const predictedHomeScore = form.elements.predictedHomeScore.value;
        const predictedAwayScore = form.elements.predictedAwayScore.value;
        penaltyWinner.required = predictedHomeScore !== ""
            && predictedAwayScore !== ""
            && predictedHomeScore === predictedAwayScore;
    }

    async function readJsonOrFallback(response) {
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }
        return {
            success: false,
            message: response.ok ? "Kaydetme başarısız. Lütfen tekrar deneyin." : "Kaydetme başarısız."
        };
    }

    async function savePrediction(event) {
        const form = event.target.closest("form[data-ajax-action]");
        if (!form || !table.contains(form)) {
            return;
        }

        event.preventDefault();

        const status = form.querySelector("[data-row-status]");
        const submitButton = form.querySelector("button[type='submit']");

        setStatus(status, "Kaydediliyor...", "");
        if (submitButton) {
            submitButton.disabled = true;
        }

        try {
            const response = await fetch(form.dataset.ajaxAction, {
                method: "POST",
                body: new FormData(form),
                headers: {
                    "Accept": "application/json",
                    "X-Requested-With": "fetch"
                }
            });
            const payload = await readJsonOrFallback(response);

            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "Kaydetme başarısız.");
            }

            if (payload.predictedHomeScore !== null && payload.predictedHomeScore !== undefined) {
                form.elements.predictedHomeScore.value = payload.predictedHomeScore;
            }
            if (payload.predictedAwayScore !== null && payload.predictedAwayScore !== undefined) {
                form.elements.predictedAwayScore.value = payload.predictedAwayScore;
            }
            if (form.elements.predictedPenaltyWinner) {
                form.elements.predictedPenaltyWinner.value = payload.predictedPenaltyWinner || "";
                syncPenaltyRequirement(form);
            }

            setStatus(status, payload.message || "Kaydedildi", "success");
        } catch (error) {
            setStatus(status, error.message || "Kaydetme başarısız.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    }

    if (showClosedControl) {
        showClosedControl.addEventListener("change", applyClosedPredictionFilter);
    }
    rows.forEach(row => {
        const form = row.querySelector("form[data-ajax-action]");
        if (!form || !form.elements.predictedPenaltyWinner) {
            return;
        }

        form.addEventListener("input", () => syncPenaltyRequirement(form));
        form.addEventListener("change", () => syncPenaltyRequirement(form));
        syncPenaltyRequirement(form);
    });
    table.addEventListener("submit", savePrediction);

    applyClosedPredictionFilter();
})();
