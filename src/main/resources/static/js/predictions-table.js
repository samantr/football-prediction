(function () {
    const table = document.querySelector("[data-predictions-table]");
    const tools = document.querySelector("[data-prediction-tools]");

    if (!table || !tools || !table.tBodies.length) {
        return;
    }

    const tbody = table.tBodies[0];
    const rows = Array.from(tbody.querySelectorAll("[data-prediction-row]"));
    const emptyRow = tbody.querySelector("[data-empty-row]");
    const filters = {
        user: tools.querySelector("[data-prediction-filter='user']"),
        match: tools.querySelector("[data-prediction-filter='match']"),
        result: tools.querySelector("[data-prediction-filter='result']")
    };
    const sortField = tools.querySelector("[data-prediction-sort-field]");
    const sortDirection = tools.querySelector("[data-prediction-sort-direction]");
    const resetButton = tools.querySelector("[data-prediction-reset]");

    function normalize(value) {
        return (value || "").toString().trim().toLocaleLowerCase("tr-TR");
    }

    function rowMatches(row) {
        const userFilter = normalize(filters.user.value);
        const matchFilter = normalize(filters.match.value);
        const resultFilter = normalize(filters.result.value);
        const resultText = `${row.dataset.result || ""} ${row.textContent || ""}`;

        return (!userFilter || normalize(row.dataset.user).includes(userFilter))
            && (!matchFilter || normalize(row.dataset.match).includes(matchFilter))
            && (!resultFilter || normalize(resultText).includes(resultFilter));
    }

    function sortValue(row, field) {
        if (field === "no" || field === "score") {
            return Number(row.dataset[field] || 0);
        }
        return normalize(row.dataset[field]);
    }

    function compareRows(left, right) {
        const field = sortField.value;
        const direction = sortDirection.value === "desc" ? -1 : 1;
        const leftValue = sortValue(left, field);
        const rightValue = sortValue(right, field);

        if (typeof leftValue === "number" && typeof rightValue === "number") {
            return (leftValue - rightValue) * direction;
        }
        return leftValue.localeCompare(rightValue, "tr-TR", {numeric: true}) * direction;
    }

    function applyTableState() {
        const visibleRows = rows.filter(rowMatches).sort(compareRows);

        rows.forEach(row => {
            row.hidden = true;
        });

        visibleRows.forEach(row => {
            row.hidden = false;
            tbody.insertBefore(row, emptyRow || null);
        });

        if (emptyRow) {
            emptyRow.hidden = visibleRows.length > 0;
        }
    }

    Object.values(filters).forEach(filter => {
        filter.addEventListener("input", applyTableState);
    });
    sortField.addEventListener("change", applyTableState);
    sortDirection.addEventListener("change", applyTableState);
    resetButton.addEventListener("click", () => {
        filters.user.value = "";
        filters.match.value = "";
        filters.result.value = "";
        sortField.value = "kickoff";
        sortDirection.value = "asc";
        applyTableState();
    });

    applyTableState();
})();
