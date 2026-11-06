# Ballon d'Or Predictor backend

A Spring Boot REST API backed by MySQL and API-Football v3. The application stores provider player IDs and season statistics, builds a complete provider-reported UEFA Champions League (UCL) season cohort before estimating relative probabilities, and labels every probability as a model estimate rather than actual award odds.

## Requirements and setup

- Java 25 as configured in `pom.xml` (a Spring Boot-compatible Java version can be selected by changing `java.version`).
- MySQL with a database named `ballondor` created before first run.
- An API-Football/API-Sports key with access to the required player and league-statistics endpoints.

Copy `.env.example` into your shell's environment configuration; Spring does not automatically load `.env` files. Never commit a real key. PowerShell example:

```powershell
$env:MYSQL_URL = "jdbc:mysql://localhost:3306/ballondor?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "your-local-password"
$env:FOOTBALL_API_KEY = "your-provider-key"
.\mvnw.cmd spring-boot:run
```

The API key is sent only to API-Football in the `x-apisports-key` header and is never part of a response. Upstream failures log the provider HTTP status and response body in the application console, with the configured API key redacted. `spring.jpa.hibernate.ddl-auto=update` creates/updates tables without dropping data. Do not use this schema-update mode as a substitute for versioned migrations in a managed production database.

Relevant environment properties:

| Variable | Default | Purpose |
|---|---|---|
| `MYSQL_URL` | local MySQL `ballondor` | JDBC URL |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / empty | Database credentials |
| `FOOTBALL_API_KEY` | unset | Required provider key |
| `FOOTBALL_API_BASE_URL` | `https://v3.football.api-sports.io` | API-Football v3 base URL |
| `FOOTBALL_COMPETITION_ID` | `2` | UEFA Champions League provider ID |
| `FOOTBALL_API_SEASON` | `2024` | Season used for player-name searches |
| `FOOTBALL_API_MAX_PAGES` | `100` | Safety bound for provider pagination; incomplete season cohorts fail rather than being scored |
| `PREDICTION_SOFTMAX_TEMPERATURE` | `12.0` | Probability-distribution temperature; positive values only |

The UCL league ID and player-search season are configurable (defaults `2` and `2024`). The integration uses API-Football v3's `/leagues?id=...`, `/players?search=...&league=...&season=...&page=...`, `/players?id=...&league=...&season=...`, and paginated `/players?league=...&season=...&page=...` requests. Seasons are listed only when the provider reports player-statistics coverage. Historical availability, detailed fields and request quotas depend on the provider's current data coverage and subscription; unsupported or missing data is reported rather than synthesized. The official documentation URL returned HTTP 403 in the development environment, so confirm the configured key's plan and current response coverage before production use.

## Run and test

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Tests use H2 under the `test` profile and mock HTTP responses; they do not call the real provider. Start MySQL and configure the provider key before exercising live endpoints. The default application profile connects to MySQL and does not require the provider key until a football-data endpoint is used.

## Frontend

The plain HTML5 and vanilla JavaScript frontend is served by Spring Boot from `src/main/resources/static`; open `http://localhost:8080/` after starting the application. It includes the dashboard, player search and profiles, player comparison, leaderboard, and predictions. The shared `js/api.js` module calls the REST API at `http://localhost:8080/api/v1`. The leaderboard loads statistics on demand because its response contains predictions but no statistics; the comparison endpoint accepts at most 10 external player IDs per request.

## REST endpoints

Base URL: `http://localhost:8080/api/v1`. Search returns a paginated body; player IDs in routes are API-Football external IDs.

| Method and path | Description |
|---|---|
| `GET /players?name=Vinicius&page=0&size=20` | Provider-backed partial-name search, case-insensitive local pagination; minimum search length is 3 |
| `GET /players/{externalPlayerId}` | Player profile |
| `GET /players/{externalPlayerId}/statistics?season=2024` | Fetch and store actual UCL statistics |
| `GET /players/{externalPlayerId}/prediction?season=2024` | Player score, cohort-relative estimated probability and scoring explanation |
| `GET /seasons/{season}/leaderboard` | Rank the complete eligible provider player cohort for that season |
| `POST /players/compare` | Compare 2–10 players for one season |
| `GET /seasons` | Provider-reported seasons with player-statistics coverage |
| `POST /seasons/{season}/refresh` | Refresh all paginated player data for the season |

Examples:

```text
GET http://localhost:8080/api/v1/players?name=Vinicius&page=0&size=20
GET http://localhost:8080/api/v1/players/12345/statistics?season=2024
GET http://localhost:8080/api/v1/players/12345/prediction?season=2024
GET http://localhost:8080/api/v1/seasons/2024/leaderboard
GET http://localhost:8080/api/v1/seasons
POST http://localhost:8080/api/v1/players/compare
Content-Type: application/json

{"season":2024,"playerIds":[12345,67890]}
```

In Postman, start the application, create a request for the relevant method/path, enter the JSON body for comparison, and send. The provider key is configured on the backend, not in Postman. Error bodies consistently include `timestamp`, `status`, `code`, `message` and `path`; validation errors can additionally include `fieldErrors`.

## Prediction method and limitations

The season leaderboard is fetched and persisted on first prediction/leaderboard request (or explicitly via refresh). When the provider pagination limit would truncate a cohort, the request fails instead of assigning probabilities from an incomplete sample. Eligible players have positive appearances and minutes.

1. Counting metrics are converted to per-90 rates: `(goals + assists) × 90 / minutes`, shots on target, key passes, defensive actions (`tackles + interceptions`), saves and goals conceded. Missing values remain unavailable; known zero values remain zero.
2. Available values are converted to midrank percentiles within position groups (goalkeeper, defender, midfielder, attacker). If a position group has only one available player for a metric, the player is compared across the available cohort for that metric.
3. Position-specific weighted score: `100 × Σ(weight × percentile) / Σ(available weights)`. Configured weights are:

   | Position | Rating | Goal contributions/90 | Shots on target/90 | Key passes/90 | Defensive actions/90 | Pass accuracy | Saves/90 | Goals conceded/90 |
   |---|---:|---:|---:|---:|---:|---:|---:|---:|
   | Forward/other | 25% | 35% | 15% | 10% | 10% | 5% | — | — |
   | Midfielder | 25% | 20% | 10% | 20% | 15% | 10% | — | — |
   | Defender | 25% | 10% | 10% | 10% | 35% | 10% | — | — |
   | Goalkeeper | 30% | 10% | — | — | — | 10% | 30% | 20% |

   Lower goals-conceded rate is ranked better for goalkeepers. Missing metrics are excluded and the remaining weights are renormalized. The response includes each factor percentile, its effective normalized weight and score contribution.
4. The estimated probability is a relative softmax distribution across the full eligible season cohort: `100 × exp((score − maxScore) / T) / Σ exp((score − maxScore) / T)`, where `T` is `PREDICTION_SOFTMAX_TEMPERATURE`. The cohort's values sum to 100%.

The current model uses UCL statistics only. API-Football's player-statistics response does not provide a dependable team-achievement, knockout-stage or match-to-match consistency measure for this implementation, so those factors are explicitly excluded and returned as limitations. Average rating is used as an overall-performance measure, not as a consistency measure. The probabilities are not calibrated official Ballon d'Or probabilities and should not be presented as such.

## Package structure

```text
com.Bd.or.Ballondor
├── config       # API and prediction properties, RestClient and timeouts
├── controller   # Versioned REST endpoints
├── dto          # Request/response records
├── entity       # JPA Player, Season, PlayerStatistics and Prediction
├── exception    # Typed API errors and centralized JSON error handling
├── external     # API-Football v3 client and provider response models
├── mapper       # Provider/entity/DTO mapping
├── prediction   # Provider-independent statistics processing and scoring
├── repository   # Spring Data JPA repositories
└── service      # Search, statistics synchronization and prediction services
```

`Player` has a unique provider player ID. `PlayerStatistics` references `Player` and `Season` with a unique player/season constraint. `Prediction` references both and stores the numeric result plus serialized scoring explanation. `Season` is unique by competition/year and marks when its full candidate pool has been fetched.
