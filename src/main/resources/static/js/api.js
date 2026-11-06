const API_BASE_URL = "http://localhost:8080/api/v1";
const CACHE_DURATION_MS = 30_000;
const getCache = new Map();
const pendingGets = new Map();

export class ApiError extends Error {
    constructor(message, status, code, details) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.body !== undefined) headers.set("Content-Type", "application/json");

    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers
        });
    } catch (error) {
        throw new Error(`Could not reach the backend at ${API_BASE_URL}. ${error.message}`);
    }

    const responseText = await response.text();
    let payload = null;
    if (responseText) {
        try {
            payload = JSON.parse(responseText);
        } catch {
            payload = responseText;
        }
    }

    if (!response.ok) {
        const message = payload && typeof payload === "object" && payload.message
            ? payload.message
            : `The backend request failed with status ${response.status}.`;
        const code = payload && typeof payload === "object" ? payload.code : undefined;
        throw new ApiError(message, response.status, code, payload);
    }
    return payload;
}

function get(path) {
    const cached = getCache.get(path);
    if (cached && Date.now() - cached.createdAt < CACHE_DURATION_MS) {
        return Promise.resolve(cached.value);
    }
    if (pendingGets.has(path)) return pendingGets.get(path);

    const pending = request(path)
        .then(value => {
            getCache.set(path, { value, createdAt: Date.now() });
            return value;
        })
        .finally(() => pendingGets.delete(path));
    pendingGets.set(path, pending);
    return pending;
}

export function clearApiCache() {
    getCache.clear();
}

export function getPlayers(name, page = 0, size = 20) {
    const query = new URLSearchParams({ name, page: String(page), size: String(size) });
    return get(`/players?${query}`);
}

export function getPlayer(externalPlayerId) {
    return get(`/players/${encodeURIComponent(externalPlayerId)}`);
}

export function getPlayerStatistics(externalPlayerId, season) {
    const query = new URLSearchParams({ season: String(season) });
    return get(`/players/${encodeURIComponent(externalPlayerId)}/statistics?${query}`);
}

export function getPlayerPrediction(externalPlayerId, season) {
    const query = new URLSearchParams({ season: String(season) });
    return get(`/players/${encodeURIComponent(externalPlayerId)}/prediction?${query}`);
}

export function getSeasons() {
    return get("/seasons");
}

export function getLeaderboard(season) {
    return get(`/seasons/${encodeURIComponent(season)}/leaderboard`);
}

export function comparePlayers(season, playerIds) {
    if (!Array.isArray(playerIds) || playerIds.length < 2 || playerIds.length > 10) {
        throw new Error("Select between 2 and 10 players to compare.");
    }
    if (playerIds.some(id => !Number.isInteger(id) || id <= 0)) {
        throw new Error("Each selected player must have a valid external player ID.");
    }
    if (new Set(playerIds).size !== playerIds.length) {
        throw new Error("A player can only be included once in a comparison.");
    }
    return request("/players/compare", {
        method: "POST",
        body: JSON.stringify({ season: Number(season), playerIds })
    });
}

export async function refreshSeason(season) {
    const result = await request(`/seasons/${encodeURIComponent(season)}/refresh`, { method: "POST" });
    clearApiCache();
    return result;
}
