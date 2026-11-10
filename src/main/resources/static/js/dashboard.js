import { getLeaderboard, getSeasons } from "./api.js";
import { appendPlayerCard, element, formatProbability, populateSeasonSelect, setStatus } from "./ui.js";

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
    setStatus(featuredStatus, "Loading featured players…", "loading");
    setStatus(contendersStatus, "Loading contender predictions…", "loading");
    setStatus(previewStatus, "Loading leaderboard preview…", "loading");

    try {
        const leaderboard = await getLeaderboard(season);
        const contenders = Array.isArray(leaderboard.contenders) ? leaderboard.contenders : [];
        if (contenders.length === 0) {
            setStatus(featuredStatus, "No featured players are available for this season.", "empty");
            setStatus(contendersStatus, "No contender predictions are available for this season.", "empty");
            setStatus(previewStatus, "The leaderboard is empty for this season.", "empty");
            return;
        }

        contenderCount.textContent = String(contenders.length);
        const score = Number(contenders[0].performanceScore);
        leadingScore.textContent = Number.isFinite(score) ? score.toFixed(1) : "—";
        leadingProbability.textContent = formatProbability(contenders[0].estimatedWinningProbability);

        for (const prediction of contenders.slice(0, 3)) appendPlayerCard(featuredPlayers, prediction, season);
        setStatus(featuredStatus, `Featured players from the ${season} leaderboard.`, "success");

        for (const prediction of contenders.slice(0, 5)) {
            const item = element("li");
            const player = prediction.player;
            const name = player && player.name ? player.name : "Unnamed player";
            item.textContent = `${name} — ${formatProbability(prediction.estimatedWinningProbability)} estimated probability`;
            topContenders.append(item);
        }
        setStatus(contendersStatus, `Top contenders for ${season}. Probabilities are model estimates, not actual voting results.`, "success");

        for (const prediction of contenders.slice(0, 5)) {
            const item = element("li");
            const player = prediction.player;
            const name = player && player.name ? player.name : "Unnamed player";
            const club = player && player.currentClub ? ` · ${player.currentClub}` : "";
            item.textContent = `${name}${club} — ${formatProbability(prediction.estimatedWinningProbability)}`;
            leaderboardPreview.append(item);
        }
        setStatus(previewStatus, `Showing the top ${Math.min(5, contenders.length)} of ${contenders.length} ranked players.`, "success");
    } catch (error) {
        setStatus(featuredStatus, `Could not load featured players: ${error.message}`, "error");
        setStatus(contendersStatus, `Could not load contender predictions: ${error.message}`, "error");
        setStatus(previewStatus, `Could not load the leaderboard: ${error.message}`, "error");
    }
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
