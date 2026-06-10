# Football Prediction MVP

Minimal Spring Boot application for a football tournament prediction competition.

## Stack

- Java 21
- Spring Boot 3
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway

## Features

- Email/password signup and login
- Roles: `USER` and `ADMIN`
- Users create and edit only their own predictions
- Prediction editing closes 1 hour before kickoff in `PredictionService`
- Admin CRUD pages for tournaments, teams, and matches
- Admin result entry
- Public leaderboard
- User prediction report
- Admin report with users, games, predictions, and scores
- Multiple tournaments
- Bracket rule based knockout generation with `Admin review required` when standings or knockout results are not decisive

## Setup

1. Create a PostgreSQL database.

```sql
create user football with password 'football';
create database football_predictions owner football;
```

2. Configure the app.

Copy `.env.example` to `.env` and update values as needed. Spring Boot imports `.env` automatically through `spring.config.import`.

3. Run the app.

```bash
mvn spring-boot:run
```

4. Open the app.

```text
http://localhost:8080
```

Default development admin:

```text
email: admin@example.com
password: admin123
```

Change this in `.env` before using real data.

## MVP Workflow

1. Log in as admin.
2. Create a tournament and mark it active.
3. Create teams.
4. Create matches.
5. Users sign up, log in, open `/matches`, and submit predictions.
6. Predictions lock 1 hour before kickoff.
7. Admin enters results in `/admin/results`.
8. Leaderboard and reports update from the stored predictions and results.

## Scoring

- Exact score: 3 points
- Correct match outcome only: 1 point
- Wrong outcome: 0 points

## Bracket Rules

Admin can create bracket rules at `/admin/generate-next-round`.

Rule examples:

- `GROUP_WINNER` with source value `A` fills the target side with the winner of group A.
- `GROUP_RUNNER_UP` with source value `B` fills the target side with the runner-up of group B.
- `MATCH_WINNER` with source value `49` fills the target side with the winner of match 49.
- `MATCH_LOSER` with source value `50` fills the target side with the loser of match 50.

Group ranking uses points, goal difference, then goals for. If those values do not break a tie at the needed position, the target match is marked `ADMIN_REVIEW_REQUIRED` and the UI shows `Admin review required`.

## Main Routes

- `/` redirects to `/leaderboard`
- `/leaderboard`
- `/login`
- `/signup`
- `/matches`
- `/my-predictions`
- `/admin`
- `/admin/tournaments`
- `/admin/teams`
- `/admin/matches`
- `/admin/results`
- `/admin/reports`
- `/admin/generate-next-round`

## Notes

- Flyway owns the schema in `src/main/resources/db/migration`.
- JPA is configured with `ddl-auto=validate`.
- The app is intentionally server-rendered with Thymeleaf. No React is used.
