export function element(tagName, text, className) {
    const node = document.createElement(tagName);
    if (text !== undefined && text !== null) node.textContent = String(text);
    if (className) node.className = className;
    return node;
}

export function setStatus(node, message, state = "info") {
    node.textContent = message;
    node.dataset.state = state;
}

export function formatValue(value) {
    if (value === null || value === undefined || value === "") return "—";
    return String(value);
}

export function formatProbability(value) {
    if (value === null || value === undefined || !Number.isFinite(Number(value))) return "—";
    return `${Number(value).toFixed(2)}%`;
}

export function populateSeasonSelect(select, seasons, preferredSeason) {
    const sorted = [...seasons].sort((first, second) => Number(second.season) - Number(first.season));
    select.replaceChildren();
    for (const season of sorted) {
        const option = element("option", `${season.season} — ${season.competition}`);
        option.value = String(season.season);
        select.append(option);
    }
    const preferred = sorted.find(season => String(season.season) === String(preferredSeason));
    const selected = preferred || sorted[0];
    if (selected) select.value = String(selected.season);
    select.disabled = sorted.length === 0;
    return selected ? Number(selected.season) : null;
}

export function seasonFromQuery() {
    const value = new URLSearchParams(window.location.search).get("season");
    const season = Number(value);
    return Number.isInteger(season) ? season : null;
}

export function playerPhoto(player) {
    const wrapper = element("span", undefined, "player-photo");
    const fallback = element("span", "Photo unavailable", "photo-fallback");
    const rawUrl = player && player.photoUrl;
    let imageUrl = null;
    if (rawUrl) {
        try {
            const parsed = new URL(rawUrl, window.location.href);
            if (parsed.protocol === "https:" || parsed.protocol === "http:") imageUrl = parsed.href;
        } catch {
            imageUrl = null;
        }
    }
    if (!imageUrl) {
        wrapper.append(fallback);
        return wrapper;
    }

    const image = element("img");
    image.src = imageUrl;
    image.alt = player.name ? `Photo of ${player.name}` : "Player photo";
    image.addEventListener("error", () => {
        image.hidden = true;
        wrapper.append(fallback);
    }, { once: true });
    wrapper.append(image);
    return wrapper;
}

export function appendPlayerCard(parent, prediction, season) {
    const player = prediction.player;
    const card = element("article", undefined, "player-card");
    card.append(playerPhoto(player));
    const heading = element("h3");
    const playerId = Number(player.externalPlayerId);
    if (Number.isInteger(playerId) && playerId > 0) {
        const link = element("a", player.name || "Unnamed player");
        link.href = `player-details.html?id=${encodeURIComponent(playerId)}&season=${encodeURIComponent(season)}`;
        heading.append(link);
    } else {
        heading.textContent = player.name || "Unnamed player";
    }
    card.append(heading);
    const details = [player.currentClub, player.nationality, player.position].filter(Boolean).join(" · ");
    if (details) card.append(element("p", details));
    card.append(element("p", `Estimated probability: ${formatProbability(prediction.estimatedWinningProbability)}`));
    parent.append(card);
}

export function statLabels(statistics) {
    return [
        ["Appearances", statistics.appearances],
        ["Goals", statistics.goals],
        ["Assists", statistics.assists],
        ["Minutes played", statistics.minutesPlayed],
        ["Average rating", statistics.averageRating],
        ["Shots", statistics.shots],
        ["Shots on target", statistics.shotsOnTarget],
        ["Key passes", statistics.keyPasses],
        ["Pass accuracy", statistics.passAccuracy],
        ["Tackles", statistics.tackles],
        ["Interceptions", statistics.interceptions],
        ["Saves", statistics.saves],
        ["Goals conceded", statistics.goalsConceded]
    ];
}
