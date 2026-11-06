import { getPlayer, getPlayerPrediction, getPlayerStatistics, getSeasons } from "./api.js";
import { element, formatProbability, playerPhoto, populateSeasonSelect, seasonFromQuery, setStatus, statLabels } from "./ui.js";

const externalPlayerId = Number(new URLSearchParams(window.location.search).get("id"));
const profileStatus = document.querySelector("#profile-status");
const profile = document.querySelector("#player-profile");
const seasonSelect = document.querySelector("#profile-season");
const statisticsStatus = document.querySelector("#statistics-status");
const statisticsList = document.querySelector("#player-statistics");
const predictionStatus = document.querySelector("#prediction-status");
const predictionContainer = document.querySelector("#player-prediction");

function renderStatistics(statistics) {
    statisticsList.replaceChildren();
    for (const [label, value] of statLabels(statistics)) {
        if (value === null || value === undefined) continue;
        statisticsList.append(element("dt", label), element("dd", value));
    }
    if (!statisticsList.children.length) {
        setStatus(statisticsStatus, "No statistics are available for this player in the selected season.", "empty");
    } else {
        setStatus(statisticsStatus, `Statistics for the ${statistics.season} season.`, "success");
    }
}

async function loadSeason(season) {
    statisticsList.replaceChildren();
    predictionContainer.replaceChildren();
    setStatus(statisticsStatus, "Loading season statistics…", "loading");
    setStatus(predictionStatus, "Loading the model estimate…", "loading");

    const [statisticsResult, predictionResult] = await Promise.allSettled([
        getPlayerStatistics(externalPlayerId, season),
        getPlayerPrediction(externalPlayerId, season)
    ]);

    if (statisticsResult.status === "fulfilled") {
        renderStatistics(statisticsResult.value);
    } else {
        setStatus(statisticsStatus, `Statistics are unavailable: ${statisticsResult.reason.message}`, "error");
    }

    if (predictionResult.status === "fulfilled") {
        const prediction = predictionResult.value;
        predictionContainer.append(element("p", `${formatProbability(prediction.estimatedWinningProbability)} estimated winning probability`));
        predictionContainer.append(element("p", `Performance score: ${prediction.performanceScore}`));
        if (prediction.probabilityScope) predictionContainer.append(element("p", prediction.probabilityScope));
        if (Array.isArray(prediction.limitations) && prediction.limitations.length) {
            const list = element("ul");
            for (const limitation of prediction.limitations) list.append(element("li", limitation));
            predictionContainer.append(list);
        }
        setStatus(predictionStatus, "This is a model estimate, not actual Ballon d'Or voting results.", "success");
    } else {
        setStatus(predictionStatus, `A prediction is unavailable: ${predictionResult.reason.message}`, "error");
    }
}

async function initialize() {
    if (!Number.isInteger(externalPlayerId) || externalPlayerId <= 0) {
        setStatus(profileStatus, "A valid player profile link is required. Search for a player by name first.", "error");
        setStatus(statisticsStatus, "No player selected.", "empty");
        setStatus(predictionStatus, "No player selected.", "empty");
        return;
    }

    setStatus(profileStatus, "Loading player profile…", "loading");
    const [playerResult, seasonsResult] = await Promise.allSettled([getPlayer(externalPlayerId), getSeasons()]);
    if (playerResult.status === "fulfilled") {
        const player = playerResult.value;
        profile.replaceChildren();
        const heading = element("h3", player.name || "Unnamed player");
        profile.append(heading);
        for (const [label, value] of [
            ["Age", player.age],
            ["Nationality", player.nationality],
            ["Position", player.position],
            ["Current club", player.currentClub]
        ]) {
            if (value !== null && value !== undefined) profile.append(element("p", `${label}: ${value}`));
        }
        profile.prepend(playerPhoto(player));
        setStatus(profileStatus, "Player profile loaded.", "success");
    } else {
        setStatus(profileStatus, `Could not load player profile: ${playerResult.reason.message}`, "error");
    }

    if (seasonsResult.status === "rejected") {
        setStatus(statisticsStatus, `Could not load available seasons: ${seasonsResult.reason.message}`, "error");
        setStatus(predictionStatus, "A season must be available before statistics or predictions can be loaded.", "error");
        return;
    }
    const seasons = seasonsResult.value;
    const selectedSeason = populateSeasonSelect(seasonSelect, seasons, seasonFromQuery());
    if (selectedSeason === null) {
        setStatus(statisticsStatus, "No supported seasons are currently available.", "empty");
        setStatus(predictionStatus, "No supported seasons are currently available.", "empty");
        return;
    }
    await loadSeason(selectedSeason);
}

seasonSelect.addEventListener("change", () => {
    const season = Number(seasonSelect.value);
    if (Number.isInteger(season)) loadSeason(season);
});

initialize();
