CREATE TABLE testpipelinebatchsql (
  id SERIAL PRIMARY KEY,
  value INTEGER,
  txn BIGINT
);

CREATE TABLE testpipelineregularkafkasql (
  id SERIAL PRIMARY KEY,
  value INTEGER,
  txn BIGINT
);

CREATE TABLE testpipelineregulartstreamssql (
  id SERIAL PRIMARY KEY,
  value INTEGER,
  txn BIGINT
);