-- Mistakes log: a mistake is a sentence (so it shows up in Fill the gap, Translate and Review) that remembers
-- what the learner actually said or wrote.
alter table sentence add column source varchar(20) not null default 'MANUAL';  -- MANUAL | MISTAKE
alter table sentence add column wrong_version varchar(1000);
alter table sentence add column created_at timestamptz not null default now();
create index idx_sentence_source on sentence (source);
