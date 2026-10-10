/* Render editable, plain-text specifications without interpreting HTML. */
window.renderEquipmentSpecs = function (container, text) {
    container.replaceChildren();
    const lines = (text || "").split(/\r?\n/).map(line => line.trim()).filter(Boolean);
    container.classList.add("equipment-spec-list");
    if (!lines.length) {
        const empty = document.createElement("p");
        empty.className = "equipment-spec-empty";
        empty.textContent = "No specifications added yet.";
        container.append(empty);
        return;
    }
    const list = document.createElement("dl");
    for (const line of lines) {
        const separator = line.indexOf(":");
        const row = document.createElement("div");
        if (separator > 0 && separator < line.length - 1) {
            const label = document.createElement("dt");
            const value = document.createElement("dd");
            label.textContent = line.slice(0, separator).trim();
            value.textContent = line.slice(separator + 1).trim();
            row.append(label, value);
        } else {
            const value = document.createElement("dd");
            value.className = "equipment-spec-note";
            value.textContent = line;
            row.append(value);
        }
        list.append(row);
    }
    container.append(list);
};
