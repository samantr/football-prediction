alter table matches
    add column penalty_winner varchar(20);

alter table predictions
    add column predicted_penalty_winner varchar(20);

alter table matches
    add constraint chk_matches_penalty_winner
        check (penalty_winner is null or penalty_winner in ('HOME', 'AWAY'));

alter table predictions
    add constraint chk_predictions_penalty_winner
        check (predicted_penalty_winner is null or predicted_penalty_winner in ('HOME', 'AWAY'));
