import { getLeaderboard, getSeasons } from "./api.js";
import { appendPlayerCard, element, formatProbability, playerPhoto, populateSeasonSelect, setStatus } from "./ui.js";

const seasonSelect = document.querySelector("#dashboard-season");
const seasonStatus = document.querySelector("#dashboard-season-status");
const featuredStatus = document.querySelector("#featured-status");
const featuredPlayers = document.querySelector("#featured-players");
const contendersStatus = document.querySelector("#contenders-status");
const topContenders = document.querySelector("#top-contenders");
const previewStatus = document.querySelector("#leaderboard-preview-status");
const leaderboardPreview = document.querySelector("#leaderboard-preview");
const contenderCount = document.querySelector("#overview-contenders");
const leadingScore = document.querySelector("#overview-score");
const leadingProbability = document.querySelector("#overview-probability");
let availableSeasons = [];

async function loadSeason(season) {
    featuredPlayers.replaceChildren();
    topContenders.replaceChildren();
    leaderboardPreview.replaceChildren();
    contenderCount.textContent = "—";
    leadingScore.textContent = "—";
    leadingProbability.textContent = "—";
    setStatus(featuredStatus, "Loading the top contenders…", "loading");
    setStatus(contendersStatus, "Loading contender predictions…", "loading");
    setStatus(previewStatus, "Loading leaderboard preview…", "loading");

    try {
        const leaderboard = await getLeaderboard(season);
        const contenders = Array.isArray(leaderboard.contenders) ? leaderboard.contenders : [];
        if (contenders.length === 0) {
            setStatus(featuredStatus, "No top contenders are available for this season.", "empty");
            setStatus(contendersStatus, "No contender predictions are available for this season.", "empty");
            setStatus(previewStatus, "The leaderboard is empty for this season.", "empty");
            return;
        }

        contenderCount.textContent = String(contenders.length);
        const score = Number(contenders[0].performanceScore);
        leadingScore.textContent = Number.isFinite(score) ? score.toFixed(1) : "—";
        leadingProbability.textContent = formatProbability(contenders[0].estimatedWinningProbability);

        for (const prediction of contenders.slice(0, 3)) appendPlayerCard(featuredPlayers, prediction, season);
        setStatus(featuredStatus, `Top three contenders from the ${season} leaderboard.`, "success");

        contenders.slice(0, 5).forEach(prediction => appendRankItem(topContenders, prediction, season, false));
        setStatus(contendersStatus, `Top contenders for ${season}. Probabilities are model estimates, not actual voting results.`, "success");

        contenders.slice(0, 5).forEach(prediction => appendRankItem(leaderboardPreview, prediction, season, true));
        setStatus(previewStatus, `Showing the top ${Math.min(5, contenders.length)} of ${contenders.length} ranked players.`, "success");
    } catch (error) {
        setStatus(featuredStatus, `Could not load featured players: ${error.message}`, "error");
        setStatus(contendersStatus, `Could not load contender predictions: ${error.message}`, "error");
        setStatus(previewStatus, `Could not load the leaderboard: ${error.message}`, "error");
    }
}

function appendRankItem(parent, prediction, season, includeScore) {
    const player = prediction.player || {};
    const item = element("li");
    item.append(playerPhoto(player));

    const entry = element("span", undefined, "rank-entry");
    const playerId = Number(player.externalPlayerId);
    if (Number.isInteger(playerId) && playerId > 0) {
        const link = element("a", player.name || "Unnamed player");
        link.href = `player-details.html?id=${encodeURIComponent(playerId)}&season=${encodeURIComponent(season)}`;
        const name = element("strong");
        name.append(link);
        entry.append(name);
    } else {
        entry.append(element("strong", player.name || "Unnamed player"));
    }

    const club = player.currentClub || player.nationality || "Season contender";
    entry.append(element("small", club));
    item.append(entry);

    const score = Number(prediction.performanceScore);
    const suffix = includeScore && Number.isFinite(score) ? ` · ${score.toFixed(1)} score` : "";
    item.append(element("span", `${formatProbability(prediction.estimatedWinningProbability)}${suffix}`, "rank-value"));
    parent.append(item);
}

async function initialize() {
    setStatus(seasonStatus, "Loading available seasons…", "loading");
    try {
        availableSeasons = await getSeasons();
        if (!Array.isArray(availableSeasons) || availableSeasons.length === 0) {
            seasonSelect.disabled = true;
            setStatus(seasonStatus, "No supported seasons are currently available.", "empty");
            return;
        }
        const selectedSeason = populateSeasonSelect(seasonSelect, availableSeasons);
        setStatus(seasonStatus, `Showing season ${selectedSeason}.`, "success");
        await loadSeason(selectedSeason);
    } catch (error) {
        setStatus(seasonStatus, `Could not load available seasons: ${error.message}`, "error");
    }
}

seasonSelect.addEventListener("change", () => {
    const season = Number(seasonSelect.value);
    if (Number.isInteger(season)) loadSeason(season);
});

initialize();
