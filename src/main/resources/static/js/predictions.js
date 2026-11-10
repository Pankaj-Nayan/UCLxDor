import { getLeaderboard, getSeasons } from "./api.js";
import { element, formatProbability, populateSeasonSelect, seasonFromQuery, setStatus } from "./ui.js";

const seasonSelect = document.querySelector("#predictions-season");
const status = document.querySelector("#predictions-status");
const scope = document.querySelector("#predictions-scope");
const list = document.querySelector("#predictions-list");
const limitations = document.querySelector("#prediction-limitations");

async function loadPredictions(season) {
    list.replaceChildren();
    limitations.replaceChildren();
    scope.textContent = "";
    setStatus(status, `Loading predictions for season ${season}…`, "loading");
    try {
        const leaderboard = await getLeaderboard(season);
        const contenders = Array.isArray(leaderboard.contenders) ? leaderboard.contenders : [];
        scope.textContent = leaderboard.probabilityScope || "";
        if (!contenders.length) {
            setStatus(status, "No predicted contenders are available for this season.", "empty");
            return;
        }
        contenders.forEach(prediction => {
            const player = prediction.player;
            const item = element("li", undefined, "prediction-card");
            item.append(element("h3", player.name || "Unnamed player"));
            if (player.currentClub) item.append(element("p", `Team: ${player.currentClub}`));
            if (player.nationality) item.append(element("p", `Nationality: ${player.nationality}`));
            item.append(element("p", `Estimated winning probability: ${formatProbability(prediction.estimatedWinningProbability)}`, "prediction-probability"));
            const score = Number(prediction.performanceScore);
            item.append(element("p", `Performance score: ${Number.isFinite(score) ? score.toFixed(1) : "—"} / 100`));
            const percentiles = prediction.factorPercentiles && typeof prediction.factorPercentiles === "object"
                ? Object.entries(prediction.factorPercentiles).filter(([, value]) => Number.isFinite(Number(value)))
                : [];
            const weights = prediction.factorWeights && typeof prediction.factorWeights === "object"
                ? prediction.factorWeights
                : {};
            const factors = percentiles
                .sort(([first], [second]) => Number(weights[second] || 0) - Number(weights[first] || 0))
                .slice(0, 4);
            if (factors.length) {
                const factorGrid = element("div", undefined, "prediction-factors");
                for (const [name, value] of factors) {
                    const factor = element("div", undefined, "prediction-factor");
                    factor.append(element("span", name.replaceAll("_", " ")));
                    factor.append(element("strong", `${Number(value).toFixed(1)} percentile`));
                    factorGrid.append(factor);
                }
                item.append(factorGrid);
            }
            const playerId = Number(player.externalPlayerId);
            if (Number.isInteger(playerId) && playerId > 0) {
                const link = element("a", "View player profile");
                link.href = `player-details.html?id=${encodeURIComponent(playerId)}&season=${encodeURIComponent(season)}`;
                item.append(link);
            }
            list.append(item);
        });
        for (const limitation of contenders[0].limitations || []) limitations.append(element("li", limitation));
        setStatus(status, `${contenders.length} contenders ranked for season ${season}. Probabilities are predictions, not actual voting results.`, "success");
    } catch (error) {
        setStatus(status, `Could not load predictions: ${error.message}`, "error");
    }
}

seasonSelect.addEventListener("change", () => {
    const season = Number(seasonSelect.value);
    if (Number.isInteger(season)) loadPredictions(season);
});

async function initialize() {
    setStatus(status, "Loading available seasons…", "loading");
    try {
        const seasons = await getSeasons();
        if (!Array.isArray(seasons) || seasons.length === 0) {
            setStatus(status, "No supported seasons are currently available.", "empty");
            return;
        }
        const season = populateSeasonSelect(seasonSelect, seasons, seasonFromQuery());
        await loadPredictions(season);
    } catch (error) {
        setStatus(status, `Could not load available seasons: ${error.message}`, "error");
    }
}

initialize();
