alter table teams
    add column external_provider varchar(100),
    add column external_id varchar(100);

alter table matches
    add column external_provider varchar(100),
    add column external_id varchar(100);

create unique index uk_teams_tournament_external
    on teams (tournament_id, external_provider, external_id)
    where external_provider is not null and external_id is not null;

create unique index uk_matches_tournament_external
    on matches (tournament_id, external_provider, external_id)
    where external_provider is not null and external_id is not null;
