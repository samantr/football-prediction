create table app_user (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(255) not null,
    role varchar(50) not null
);

create table tournaments (
    id bigserial primary key,
    name varchar(255) not null,
    season varchar(100) not null,
    active boolean not null default false
);

create table teams (
    id bigserial primary key,
    tournament_id bigint not null references tournaments(id) on delete cascade,
    name varchar(255) not null,
    code varchar(50) not null,
    group_code varchar(50),
    constraint uk_team_tournament_code unique (tournament_id, code)
);

create table matches (
    id bigserial primary key,
    tournament_id bigint not null references tournaments(id) on delete cascade,
    match_no integer not null,
    stage varchar(50) not null,
    group_code varchar(50),
    home_team_id bigint references teams(id),
    away_team_id bigint references teams(id),
    placeholder_home varchar(255),
    placeholder_away varchar(255),
    kickoff_at timestamp not null,
    home_score integer,
    away_score integer,
    status varchar(50) not null,
    constraint uk_match_tournament_match_no unique (tournament_id, match_no)
);

create table predictions (
    id bigserial primary key,
    user_id bigint not null references app_user(id) on delete cascade,
    match_id bigint not null references matches(id) on delete cascade,
    predicted_home_score integer not null,
    predicted_away_score integer not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_prediction_user_match unique (user_id, match_id)
);

create table bracket_rules (
    id bigserial primary key,
    tournament_id bigint not null references tournaments(id) on delete cascade,
    target_match_id bigint not null references matches(id) on delete cascade,
    target_side varchar(20) not null,
    source_type varchar(50) not null,
    source_value varchar(100) not null
);

create index idx_teams_tournament on teams(tournament_id);
create index idx_matches_tournament on matches(tournament_id);
create index idx_matches_group on matches(tournament_id, stage, group_code);
create index idx_predictions_user on predictions(user_id);
create index idx_predictions_match on predictions(match_id);
create index idx_bracket_rules_tournament on bracket_rules(tournament_id);
