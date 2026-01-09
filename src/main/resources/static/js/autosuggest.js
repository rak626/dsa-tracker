document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll(".autosuggest").forEach(input => {

        const dropdown = document.createElement("ul");
        dropdown.className = "suggest-list";
        input.parentNode.style.position = "relative";
        input.parentNode.appendChild(dropdown);

        let activeIndex = -1;

        function clearDropdown() {
            dropdown.innerHTML = "";
            activeIndex = -1;
        }

        function getQuery() {
            const isMulti = input.dataset.multi === "true";
            if (!isMulti) return input.value.trim();
            return input.value.split(",").pop().trim();
        }

        function applySelection(value) {
            const isMulti = input.dataset.multi === "true";

            if (isMulti) {
                const parts = input.value.split(",");
                parts[parts.length - 1] = " " + value;
                input.value = parts.join(",").replace(/^ /, "");
            } else {
                input.value = value;
            }
            clearDropdown();
        }

        input.addEventListener("input", async () => {
            const query = getQuery();
            if (!query) {
                clearDropdown();
                return;
            }

            const res = await fetch(`${input.dataset.endpoint}?q=${query}`);
            const items = await res.json();

            dropdown.innerHTML = items.map(item =>
                `<li class="suggest-item">${item}</li>`
            ).join("");

            activeIndex = -1;
        });

        input.addEventListener("keydown", e => {
            const items = dropdown.querySelectorAll(".suggest-item");
            if (items.length === 0) return;

            if (e.key === "ArrowDown") {
                e.preventDefault();
                activeIndex = (activeIndex + 1) % items.length;
            }

            if (e.key === "ArrowUp") {
                e.preventDefault();
                activeIndex = (activeIndex - 1 + items.length) % items.length;
            }

            if (e.key === "Enter" && activeIndex >= 0) {
                e.preventDefault();
                applySelection(items[activeIndex].textContent);
                return;
            }

            items.forEach((item, index) => {
                item.classList.toggle("active", index === activeIndex);
            });
        });

        dropdown.addEventListener("click", e => {
            if (!e.target.classList.contains("suggest-item")) return;
            applySelection(e.target.textContent);
        });

        document.addEventListener("click", e => {
            if (!input.contains(e.target)) {
                clearDropdown();
            }
        });

    });

});
