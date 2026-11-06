import { getPlayers } from "./api.js";
import { element, formatValue, playerPhoto, setStatus } from "./ui.js";

const form = document.querySelector("#player-search-form");
const input = document.querySelector("#player-search-name");
const status = document.querySelector("#search-status");
const results = document.querySelector("#search-results");
const loadMoreButton = document.querySelector("#load-more");
const pageSize = 20;
let currentPage = 0;
let totalPages = 0;
let currentName = "";
let loading = false;

function appendResult(player) {
    const item = element("li");
    item.append(playerPhoto(player));
    const heading = element("h3");
    const externalPlayerId = Number(player.externalPlayerId);
    if (Number.isInteger(externalPlayerId) && externalPlayerId > 0) {
        const link = element("a", player.name || "Unnamed player");
        link.href = `player-details.html?id=${encodeURIComponent(externalPlayerId)}`;
        heading.append(link);
    } else {
        heading.textContent = player.name || "Unnamed player";
    }
    item.append(heading);
    item.append(element("p", `Team: ${formatValue(player.currentClub)}`));
    item.append(element("p", `Nationality: ${formatValue(player.nationality)}`));
    item.append(element("p", `Position: ${formatValue(player.position)}`));
    results.append(item);
}

async function searchPlayers(name, page, append) {
    if (loading) return;
    loading = true;
    loadMoreButton.disabled = true;
    setStatus(status, append ? "Loading more players…" : "Searching players…", "loading");
    if (!append) {
        results.replaceChildren();
        loadMoreButton.hidden = true;
    }

    try {
        const result = await getPlayers(name, page, pageSize);
        const players = Array.isArray(result.content) ? result.content : [];
        if (!append && players.length === 0) {
            setStatus(status, `No players matched “${name}”.`, "empty");
            return;
        }
        for (const player of players) appendResult(player);
        currentPage = result.page;
        totalPages = result.totalPages;
        loadMoreButton.hidden = currentPage + 1 >= totalPages;
        setStatus(status, `Showing ${results.children.length} of ${result.totalElements} matching players.`, "success");
    } catch (error) {
        setStatus(status, `Could not search players: ${error.message}`, "error");
        loadMoreButton.hidden = true;
    } finally {
        loading = false;
        loadMoreButton.disabled = false;
    }
}

form.addEventListener("submit", event => {
    event.preventDefault();
    const name = input.value.trim();
    if (name.length < 3) {
        results.replaceChildren();
        loadMoreButton.hidden = true;
        setStatus(status, "Enter at least 3 characters to search. Player names are searched directly; an ID is not required.", "empty");
        return;
    }
    currentName = name;
    searchPlayers(currentName, 0, false);
});

loadMoreButton.addEventListener("click", () => {
    if (currentName && currentPage + 1 < totalPages) searchPlayers(currentName, currentPage + 1, true);
});
