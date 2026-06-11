# Football Prediction MVP

Minimal Spring Boot application for a football tournament prediction competition.
The application UI is Turkish and rendered server-side with Thymeleaf.

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
- Optional admin import/sync from football-data.org
- Admin delete and test cleanup tools
- Public leaderboard
- User prediction report
- Admin report with users, games, predictions, and scores
- Multiple tournaments
- Bracket rule based knockout generation with the Turkish `Yönetici kontrolü gerekli` UI message when standings or knockout results are not decisive

## Setup

1. Create a PostgreSQL database.

```sql
create user football with password 'football';
create database football_predictions owner football;
```

2. Configure the app.

Copy `.env.example` to `.env` and update values as needed. Spring Boot imports `.env` automatically through `spring.config.import`.

Football-data.org import is optional. To enable it, create an API token at [football-data.org](https://www.football-data.org/client/register), then set it in `.env`:

```text
FOOTBALL_DATA_API_TOKEN=your-token-here
```

The first supported provider is football-data.org. The admin import page defaults to World Cup competition code `WC` and season `2026`.

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

Optional import workflow:

1. Set `FOOTBALL_DATA_API_TOKEN`.
2. Log in as admin and open `/admin/import`.
3. Select a tournament.
4. Sync teams, matches, or both.
5. Review and edit imported data manually as needed.

Imported data is stored in the local database. Local matches are matched by `externalProvider` and `externalId` first, existing imported matches are updated, new imported matches are created, and local matches are never deleted automatically.

## Admin Delete And Cleanup

Admin pages include Turkish `Sil` buttons for tournaments, teams, matches, and bracket rules. Deletes use POST forms with CSRF protection and a browser confirmation.

Safe delete rules:

- A match cannot be deleted if predictions exist.
- A team cannot be deleted if matches use it.
- A tournament cannot be deleted if teams, matches, predictions, or bracket rules exist.
- Bracket rules can be deleted from `/admin/generate-next-round`.

The admin cleanup page at `/admin/cleanup` can remove local/test data. It deletes predictions, bracket rules, matches, teams, tournaments, and normal users, but it does not delete admin users.

## Scoring

- Exact score: 3 points
- Correct match outcome only: 1 point
- Wrong outcome: 0 points
- Incomplete or unplayed match: 0 points

Examples:

- Real 2-1, prediction 2-1: 3 points
- Real 2-1, prediction 1-0: 1 point
- Real 2-1, prediction 1-1: 0 points
- Real 1-1, prediction 0-0: 1 point
- Real 1-1, prediction 1-1: 3 points
- Real not completed: 0 points

## Same Wi-Fi Access

English:

1. Run the app locally with `mvn spring-boot:run`.
2. On Windows, find your computer's local IP address:

```powershell
ipconfig
```

3. Other devices on the same Wi-Fi can open:

```text
http://LOCAL_IP:8080
```

Example:

```text
http://192.168.1.25:8080
```

Windows Firewall may ask for permission. Allow Java on Private networks.

If needed, configure these optional properties:

```properties
server.address=0.0.0.0
server.port=8080
```

Turkish:

1. Uygulamayı yerelde `mvn spring-boot:run` ile çalıştırın.
2. Windows'ta bilgisayarın yerel IP adresini bulun:

```powershell
ipconfig
```

3. Aynı Wi-Fi ağındaki diğer cihazlar şu adresi açabilir:

```text
http://LOCAL_IP:8080
```

Örnek:

```text
http://192.168.1.25:8080
```

Windows Güvenlik Duvarı izin isteyebilir. Java için Özel ağlarda izin verin.

Gerekirse şu isteğe bağlı ayarları kullanın:

```properties
server.address=0.0.0.0
server.port=8080
```

## Bracket Rules

Admin can create bracket rules at `/admin/generate-next-round`.

Rule examples:

- `GROUP_WINNER` with source value `A` fills the target side with the winner of group A.
- `GROUP_RUNNER_UP` with source value `B` fills the target side with the runner-up of group B.
- `MATCH_WINNER` with source value `49` fills the target side with the winner of match 49.
- `MATCH_LOSER` with source value `50` fills the target side with the loser of match 50.

Group ranking uses points, goal difference, then goals for. If those values do not break a tie at the needed position, the target match is marked `ADMIN_REVIEW_REQUIRED` and the UI shows `Yönetici kontrolü gerekli`.

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
- `/admin/import`
- `/admin/cleanup`
- `/admin/generate-next-round`

## Notes

- Flyway owns the schema in `src/main/resources/db/migration`.
- JPA is configured with `ddl-auto=validate`.
- The app is intentionally server-rendered with Thymeleaf. No React is used.
- User-facing pages, navigation, admin screens, reports, and messages are in Turkish.
