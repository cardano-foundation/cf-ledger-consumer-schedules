# Explorer Scheduled Jobs

<p align="left">
<img alt="Tests" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/tests.yaml/badge.svg" />
<img alt="Coverage" src="https://cardano-foundation.github.io/cf-ledger-consumer-schedules/badges/jacoco.svg" />
<img alt="Release" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/release.yaml/badge.svg?branch=main" />
<img alt="Publish" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/publish.yaml/badge.svg?branch=main" />
<a href="https://conventionalcommits.org"><img alt="conventionalcommits" src="https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits" /></a>
<a href="https://app.fossa.com/reports/07917f95-b55f-4246-8504-1e232cfb28af"><img alt="FOSSA Status" src="https://app.fossa.com/api/projects/custom%2B41588%2Fgit%40github.com%3Acardano-foundation%2Fcf-ledger-consumer-schedules.git.svg?type=small&issueType=license" /></a>
</p>

This repository executes tasks in a periodic sequence to precompute computationally intensive jobs.

👉 Check the [Explorer repository](https://github.com/cardano-foundation/cf-explorer) to understand how the microservices work together

## 🧪 Test Reports

To ensure the stability and reliability of this project, unit and mutation tests have been implemented. By clicking on the links below, you can access the detailed test reports and review the outcomes of the tests performed.

📊 [Mutation report](https://cardano-foundation.github.io/cf-ledger-consumer-schedules/mutation-report/)

## 🌱 Environment Variables
- LEDGER_SYNC_HOST: Ledger-sync database host.
- LEDGER_SYNC_PORT: Ledger-sync database port
- LEDGER_SYNC_USER: Ledger-sync database username
- LEDGER_SYNC_PASSWORD: Ledger-sync database password
- LEDGER_SYNC_DB: Ledger-sync database name
- LEDGER_SYNC_SCHEMA: Ledger-sync database schema
- LEDGER_SYNC_FLYWAY_ENABLE: Ledger-sync Migrate schema, set `true` if this is the first time run app

- LEDGER_SYNC_AGG_HOST= Ledger-sync aggregate database host.
- LEDGER_SYNC_AGG_PORT= Ledger-sync aggregate database port
- LEDGER_SYNC_AGG_USER= Ledger-sync aggregate database username
- LEDGER_SYNC_AGG_PASSWORD= Ledger-sync aggregate database password
- LEDGER_SYNC_AGG_DB= Ledger-sync aggregate database name
- LEDGER_SYNC_AGG_SCHEMA= Ledger-sync aggregate database schema
- LEDGER_SYNC_AGG_FLYWAY_ENABLE= Ledger-sync aggregate Migrate schema, set `true` if this is the first time run app

- EXPLORER_HOST: Explorer database host.
- EXPLORER_PORT: Explorer database port
- EXPLORER_USER: Explorer database username
- EXPLORER_PASSWORD: Explorer database password
- EXPLORER_DB: Explorer database name
- EXPLORER_SCHEMA: Explorer database schema
- EXPLORER_FLYWAY_ENABLE: Explorer Migrate schema, set `true` if this is the first time run app

- MAXIMUM_POOL_SIZE: Schedule will have job select parallel from database. If you want to task schedule as fast as possible set the `MAXIMUM_POOL_SIZE` as much as possible (cpu core * 4). This will reduce another app performance.
- REPORT_HISTORY_JOB_ENABLED: enable a report history job to delete expired file. Default `true`
- SET_EXPIRED_REPORTS_DELAY: delay time between each report history job run. Default `86400000` as 1 day
- LIMIT_CONTENT_PER_SHEET: limit content per sheet of export file, default `1000000`
- API_CHECK_REWARD_URL: api url to fetch reward
- API_CHECK_POOL_HISTORY_URL: api url to fetch pool history
- NETWORK_NAME: cardano node network name (preprod, testnet, mainnet)

- REPORT_S3_ACCESS_KEY: report aws s3 access key
- REPORT_S3_SECRET_KEY: report aws s3 secret key
- REPORT_S3_REGION: report aws s3 region
- REPORT_S3_BUCKET_NAME: report aws s3 bucket name
- REPORT_S3_STORAGE_ENDPOINT: report aws s3 storage endpoint

- TOKEN_LOGO_S3_ACCESS_KEY: token logo aws s3 access key
- TOKEN_LOGO_S3_SECRET_KEY: token logo aws s3 secret key
- TOKEN_LOGO_S3_REGION: token logo aws s3 region
- TOKEN_LOGO_S3_BUCKET_NAME: token logo aws s3 bucket name
- TOKEN_LOGO_S3_STORAGE_ENDPOINT: token logo aws s3 storage endpoint

- SPRING_PROFILES_ACTIVE: active profiles
- LOG: application log level
- META_DATA_JOB_ENABLED: enable metadata job
- TOKEN_METADATA_URL: url that store token
- TOKEN_METADATA_FOLDER: store token
- REDIS_SENTINEL_PASSWORD : Redis sentinel password. Default is redis_sentinel_pass.
- REDIS_SENTINEL_HOST : Redis sentinel host. Default is  cardano.redis.sentinel.
- REDIS_SENTINEL_PORT : Redis sentinel port. Default is 26379.
- REDIS_SENTINEL_MASTER_NAME : Redis master name. Default is mymaster.
- POOL_STATUS_FIXED_DELAY: delay time between each time get pool status
- NUMBER_DELEGATOR_FIXED_DELAY: delay time between each time get number delegator
- UNIQUE_ACCOUNT_FIXED_DELAY: fixed delay for job build cache unique account
- UNIQUE_ACCOUNT_ENABLED: enable unique account job
- STAKE_TX_BALANCE_JOB_ENABLED: enable stake tx balance job
- STAKE_TX_BALANCE_FIXED_DELAY: fixed delay for job stake tx balance
- TOKEN_INFO_JOB_ENABLED: enable token info job
- TOKEN_INFO_FIXED_DELAY: delay time between each time run token info job
- AGGREGATE_POOL_INFO_FIXED_DELAY: fixed delay for job aggregate pool info
- SMART_CONTRACT_INFO_FIXED_DELAY: fixed delay for job smart contract info
- NATIVE_SCRIPT_INFO_FIXED_DELAY: fixed delay for job native script info
- DREP_INFO_JOB_ENABLED: enable drep info job
- DREP_INFO_FIXED_DELAY: fixed delay for job drep info
- GOVERNANCE_INFO_JOB_ENABLED: enable governance info job
- GOVERNANCE_INFO_FIXED_DELAY: fixed delay for job governance info
- AGG_ANALYTIC_FIXED_DELAY: fixed delay for job aggregate analytic that related to address and token
- BLOCK_STATISTICS_DAILY_URL: The URL to get daily block statistics
- BLOCK_STATISTICS_PER_EPOCH_URL: The URL to get per-epoch block statistics.
- BLOCK_STATISTICS_JOB_ENABLED: Enable the crawl block statistics job. Default is true.
- GOV_ACTION_METADATA_JOB_ENABLED: Enable the upsert government action metadata job. Default is true.
- GOV_ACTION_METADATA_FIXED_DELAY: Fixed delay for the government action metadata scheduler.
- GOV_ACTION_METADATA_RETRY_COUNT: The number of retries to fetch the URL to get government action metadata.