import { getLeaderboard, getPlayerStatistics, getSeasons, refreshSeason } from "./api.js";
import { element, formatProbability, formatValue, playerPhoto, populateSeasonSelect, seasonFromQuery, setStatus } from "./ui.js";

const seasonSelect = document.querySelector("#leaderboard-season");
const refreshButton = document.querySelector("#refresh-season");
const status = document.querySelector("#leaderboard-status");
const scope = document.querySelector("#probability-scope");
const sortSelect = document.querySelector("#leaderboard-sort");
const tableContainer = document.querySelector("#leaderboard-table-container");
const podium = document.querySelector("#leaderboard-podium");
let contenders = [];
let currentSeason = null;
const loadedStatistics = new Map();

function sortedContenders() {
    const ordered = [...contenders];
    switch (sortSelect.value) {
        case "score":
            ordered.sort((first, second) => second.performanceScore - first.performanceScore);
            break;
        case "name":
            ordered.sort((first, second) => (first.player.name || "").localeCompare(second.player.name || ""));
            break;
        default:
            ordered.sort((first, second) => second.estimatedWinningProbability - first.estimatedWinningProbability);
    }
    return ordered;
}

function renderTable() {
    tableContainer.replaceChildren();
    renderPodium();
    if (!contenders.length) return;

    const table = element("table");
    table.append(element("caption", `UEFA Champions League leaderboard, season ${currentSeason}`));
    const head = element("thead");
    const header = element("tr");
    for (const label of ["Rank", "Player", "Team", "Performance score", "Estimated probability", "Appearances", "Goals", "Assists", "Minutes", "Statistics"]) {
        header.append(element("th", label));
    }
    head.append(header);
    table.append(head);
    const body = element("tbody");

    sortedContenders().forEach((prediction, index) => {
        const player = prediction.player;
        const playerId = Number(player.externalPlayerId);
        const stats = loadedStatistics.get(playerId);
        const row = element("tr");
        row.append(element("td", index + 1));
        const nameCell = element("td", undefined, "leaderboard-player-cell");
        nameCell.append(playerPhoto(player));
        const playerName = element("span", undefined, "leaderboard-player-name");
        if (Number.isInteger(playerId) && playerId > 0) {
            const link = element("a", player.name || "Unnamed player");
            link.href = `player-details.html?id=${encodeURIComponent(playerId)}&season=${encodeURIComponent(currentSeason)}`;
            playerName.append(link);
        } else {
            playerName.textContent = player.name || "Unnamed player";
        }
        nameCell.append(playerName);
        row.append(nameCell);
        row.append(element("td", formatValue(player.currentClub)));
        row.append(element("td", formatValue(prediction.performanceScore)));
        row.append(element("td", formatProbability(prediction.estimatedWinningProbability)));
        row.append(element("td", formatValue(stats && stats.appearances)));
        row.append(element("td", formatValue(stats && stats.goals)));
        row.append(element("td", formatValue(stats && stats.assists)));
        row.append(element("td", formatValue(stats && stats.minutesPlayed)));
        const actionCell = element("td");
        const loadButton = element("button", stats ? "Refresh statistics" : "Load statistics");
        loadButton.type = "button";
        loadButton.dataset.playerId = String(playerId);
        loadButton.disabled = !Number.isInteger(playerId) || playerId <= 0;
        const rowStatus = element("span", "", "row-statistics-status");
        actionCell.append(loadButton, rowStatus);
        row.append(actionCell);
        body.append(row);
    });
    table.append(body);
    tableContainer.append(table);
}

function renderPodium() {
    podium.replaceChildren();
    sortedContenders().slice(0, 3).forEach((prediction, index) => {
        const player = prediction.player || {};
        const card = element("article", undefined, "podium-card");
        card.append(element("span", `RANK ${index + 1}`, "podium-rank"));
        card.append(playerPhoto(player));
        card.append(element("h3", player.name || "Unnamed player"));
        card.append(element("p", player.currentClub || "Club unavailable"));
        card.append(element("p", formatProbability(prediction.estimatedWinningProbability), "podium-value"));
        podium.append(card);
    });
}

async function loadLeaderboard(season) {
    currentSeason = season;
    contenders = [];
    loadedStatistics.clear();
    podium.replaceChildren();
    tableContainer.replaceChildren();
    scope.textContent = "";
    refreshButton.disabled = true;
    sortSelect.disabled = true;
    setStatus(status, `Loading the ${season} leaderboard…`, "loading");
    try {
        const result = await getLeaderboard(season);
        contenders = Array.isArray(result.contenders) ? result.contenders : [];
        scope.textContent = result.probabilityScope || "";
        if (!contenders.length) {
            setStatus(status, "No ranked players are available for this season.", "empty");
            return;
        }
        sortSelect.disabled = false;
        renderTable();
        setStatus(status, `${contenders.length} players ranked for season ${season}.`, "success");
    } catch (error) {
        setStatus(status, `Could not load the leaderboard: ${error.message}`, "error");
    } finally {
        refreshButton.disabled = seasonSelect.disabled;
    }
}

tableContainer.addEventListener("click", async event => {
    const button = event.target.closest("button[data-player-id]");
    if (!button || button.disabled) return;
    const playerId = Number(button.dataset.playerId);
    const row = button.closest("tr");
    const rowStatus = row.querySelector(".row-statistics-status");
    button.disabled = true;
    setStatus(rowStatus, " Loading…", "loading");
    try {
        const statistics = await getPlayerStatistics(playerId, currentSeason);
        loadedStatistics.set(playerId, statistics);
        renderTable();
    } catch (error) {
        setStatus(rowStatus, ` Statistics unavailable: ${error.message}`, "error");
    } finally {
        const currentButton = tableContainer.querySelector(`button[data-player-id="${playerId}"]`);
        if (currentButton) currentButton.disabled = false;
    }
});

refreshButton.addEventListener("click", async () => {
    if (currentSeason === null) return;
    refreshButton.disabled = true;
    setStatus(status, `Refreshing statistics for season ${currentSeason}…`, "loading");
    try {
        const result = await refreshSeason(currentSeason);
        setStatus(status, result.message || `Refreshed ${result.playersRefreshed} players.`, "success");
        await loadLeaderboard(currentSeason);
    } catch (error) {
        setStatus(status, `Could not refresh season statistics: ${error.message}`, "error");
    } finally {
        refreshButton.disabled = seasonSelect.disabled;
    }
});

seasonSelect.addEventListener("change", () => {
    const season = Number(seasonSelect.value);
    if (Number.isInteger(season)) loadLeaderboard(season);
});

sortSelect.addEventListener("change", renderTable);

async function initialize() {
    setStatus(status, "Loading available seasons…", "loading");
    try {
        const seasons = await getSeasons();
        if (!Array.isArray(seasons) || seasons.length === 0) {
            setStatus(status, "No supported seasons are currently available.", "empty");
            return;
        }
        currentSeason = populateSeasonSelect(seasonSelect, seasons, seasonFromQuery());
        refreshButton.disabled = false;
        await loadLeaderboard(currentSeason);
    } catch (error) {
        setStatus(status, `Could not load available seasons: ${error.message}`, "error");
    }
}

initialize();
