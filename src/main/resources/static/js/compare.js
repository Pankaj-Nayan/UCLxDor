import { comparePlayers, getPlayers, getSeasons } from "./api.js";
import { element, formatProbability, formatValue, populateSeasonSelect, setStatus } from "./ui.js";

const searchForm = document.querySelector("#comparison-search-form");
const searchInput = document.querySelector("#comparison-search-name");
const searchStatus = document.querySelector("#comparison-search-status");
const searchResults = document.querySelector("#comparison-search-results");
const selectedStatus = document.querySelector("#selected-status");
const selectedList = document.querySelector("#selected-players");
const seasonSelect = document.querySelector("#comparison-season");
const compareButton = document.querySelector("#compare-button");
const comparisonStatus = document.querySelector("#comparison-status");
const comparisonResults = document.querySelector("#comparison-results");
const selectedPlayers = new Map();

function updateSelectedPlayers() {
    selectedList.replaceChildren();
    for (const player of selectedPlayers.values()) {
        const item = element("li");
        item.append(element("span", `${player.name || "Unnamed player"}${player.currentClub ? ` — ${player.currentClub}` : ""}`));
        const remove = element("button", "Remove");
        remove.type = "button";
        remove.addEventListener("click", () => {
            selectedPlayers.delete(Number(player.externalPlayerId));
            updateSelectedPlayers();
        });
        item.append(remove);
        selectedList.append(item);
    }
    for (const addButton of searchResults.querySelectorAll("button[data-player-id]")) {
        const playerId = Number(addButton.dataset.playerId);
        const selected = selectedPlayers.has(playerId);
        addButton.disabled = selected || selectedPlayers.size >= 10;
        addButton.textContent = selected ? "Added" : "Add to comparison";
    }
    setStatus(selectedStatus, `${selectedPlayers.size} player${selectedPlayers.size === 1 ? "" : "s"} selected. Select 2–10 players to compare.`, selectedPlayers.size ? "success" : "empty");
    compareButton.disabled = selectedPlayers.size < 2 || selectedPlayers.size > 10 || seasonSelect.disabled;
}

function renderSearchResult(player) {
    const item = element("li");
    const description = `${player.name || "Unnamed player"}${player.currentClub ? ` — ${player.currentClub}` : ""}`;
    item.append(element("span", description));
    const id = Number(player.externalPlayerId);
    const addButton = element("button", selectedPlayers.has(id) ? "Added" : "Add to comparison");
    addButton.type = "button";
    addButton.dataset.playerId = String(id);
    addButton.disabled = !Number.isInteger(id) || id <= 0 || selectedPlayers.has(id) || selectedPlayers.size >= 10;
    addButton.addEventListener("click", () => {
        if (selectedPlayers.size >= 10 || selectedPlayers.has(id)) return;
        selectedPlayers.set(id, player);
        addButton.disabled = true;
        addButton.textContent = "Added";
        updateSelectedPlayers();
    });
    item.append(addButton);
    searchResults.append(item);
}

searchForm.addEventListener("submit", async event => {
    event.preventDefault();
    const name = searchInput.value.trim();
    searchResults.replaceChildren();
    if (name.length < 3) {
        setStatus(searchStatus, "Enter at least 3 characters. Search by player name; no player ID is required.", "empty");
        return;
    }
    setStatus(searchStatus, "Searching players…", "loading");
    try {
        const result = await getPlayers(name, 0, 20);
        const players = Array.isArray(result.content) ? result.content : [];
        if (!players.length) {
            setStatus(searchStatus, `No players matched “${name}”.`, "empty");
            return;
        }
        players.forEach(renderSearchResult);
        setStatus(searchStatus, `Found ${players.length} matching players.`, "success");
    } catch (error) {
        setStatus(searchStatus, `Could not search players: ${error.message}`, "error");
    }
});

compareButton.addEventListener("click", async () => {
    if (selectedPlayers.size < 2 || selectedPlayers.size > 10) return;
    compareButton.disabled = true;
    comparisonResults.replaceChildren();
    setStatus(comparisonStatus, "Comparing player statistics and predictions…", "loading");
    try {
        const result = await comparePlayers(Number(seasonSelect.value), [...selectedPlayers.keys()]);
        if (renderComparison(result)) {
            setStatus(comparisonStatus, `Comparison for the ${result.season} season. Probabilities are model estimates, not voting results.`, "success");
        } else {
            setStatus(comparisonStatus, "The backend returned no comparison results for the selected players.", "empty");
        }
    } catch (error) {
        setStatus(comparisonStatus, `Could not compare players: ${error.message}`, "error");
    } finally {
        compareButton.disabled = selectedPlayers.size < 2 || selectedPlayers.size > 10 || seasonSelect.disabled;
    }
});

function renderComparison(result) {
    comparisonResults.replaceChildren();
    const players = Array.isArray(result.players) ? result.players : [];
    if (!players.length) return false;

    const table = element("table");
    const caption = element("caption", `Player statistics and model estimates, season ${result.season}`);
    table.append(caption);
    const head = element("thead");
    const headerRow = element("tr");
    headerRow.append(element("th", "Metric"));
    players.forEach(player => headerRow.append(element("th", player.name || "Unnamed player")));
    head.append(headerRow);
    table.append(head);

    const metrics = [
        ["Goals", "goals", false],
        ["Assists", "assists", false],
        ["Appearances", "appearances", false],
        ["Minutes played", "minutesPlayed", false],
        ["Average rating", "averageRating", false],
        ["Performance score", "performanceScore", false],
        ["Estimated winning probability", "estimatedWinningProbability", true]
    ];
    const body = element("tbody");
    for (const [label, field, probability] of metrics) {
        const row = element("tr");
        row.append(element("th", label));
        const values = players.map(player => player[field] === null || player[field] === undefined
            ? Number.NaN
            : Number(player[field]));
        const presentValues = values.filter(Number.isFinite);
        const maximum = presentValues.length ? Math.max(...presentValues) : null;
        const hasDifference = presentValues.some(value => value !== presentValues[0]);
        players.forEach((player, index) => {
            const cell = element("td");
            const value = player[field];
            cell.textContent = probability ? formatProbability(value) : formatValue(value);
            if (hasDifference && Number.isFinite(values[index]) && values[index] === maximum) {
                cell.append(element("strong", " (highest)"));
            }
            row.append(cell);
        });
        body.append(row);
    }
    table.append(body);
    comparisonResults.append(table);
    return true;
}

async function initialize() {
    try {
        const seasons = await getSeasons();
        if (!Array.isArray(seasons) || seasons.length === 0) {
            setStatus(comparisonStatus, "No supported seasons are currently available.", "empty");
            updateSelectedPlayers();
            return;
        }
        populateSeasonSelect(seasonSelect, seasons);
        updateSelectedPlayers();
    } catch (error) {
        setStatus(comparisonStatus, `Could not load seasons: ${error.message}`, "error");
        updateSelectedPlayers();
    }
}

updateSelectedPlayers();
initialize();
