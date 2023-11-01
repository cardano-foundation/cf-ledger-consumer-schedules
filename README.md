# Iris Scheduled Jobs

<p align="left">
<img alt="Tests" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/tests.yaml/badge.svg" />
<img alt="Release" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/release.yaml/badge.svg?branch=main" />
<img alt="Publish" src="https://github.com/cardano-foundation/cf-ledger-consumer-schedules/actions/workflows/publish.yaml/badge.svg?branch=main" />
<a href="https://conventionalcommits.org"><img alt="conventionalcommits" src="https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits" /></a>
</p>

This repository executes tasks in a periodic sequence to precompute computationally intensive jobs.

👉 Check the [Iris repository](https://github.com/cardano-foundation/cf-explorer) to understand how the microservices work together

## 🧪 Test Reports

To ensure the stability and reliability of this project, unit and mutation tests have been implemented. By clicking on the links below, you can access the detailed test reports and review the outcomes of the tests performed.

📊 [Mutation report](https://cardano-foundation.github.io/cf-ledger-consumer-schedules/mutation-report/)

## Environment value
- LEDGER_SYNC_HOST: Ledger-sync database host.
- LEDGER_SYNC_PORT: Ledger-sync database port
- LEDGER_SYNC_USER: Ledger-sync database username
- LEDGER_SYNC_PASSWORD: Ledger-sync database password
- LEDGER_SYNC_DB: Ledger-sync database name
- LEDGER_SYNC_FLYWAY_ENABLE: Ledger-sync Migrate schema, set `true` if this is the first time run app
- LEDGER_SYNC_FLYWAY_VALIDATE: Ledger-sync Flyway schema validation, default `false`
- EXPLORER_HOST: Analytics database host.
- EXPLORER_PORT: Analytics database port
- EXPLORER_USER: Analytics database username
- EXPLORER_PASSWORD: Analytics database password
- EXPLORER_DB: Analytics database name
- EXPLORER_FLYWAY_ENABLE: Analytics Migrate schema, set `true` if this is the first time run app
- EXPLORER_FLYWAY_VALIDATE: Analytics Flyway schema validation, default `false`
- POSTGRES_SCHEMA: database schema
- MAXIMUM_POOL_SIZE: Schedule will have job select parallel from database. If you want to task schedule as fast as possible set the `MAXIMUM_POOL_SIZE` as much as possible (cpu core * 4). This will reduce another app performance.
- REPORT_HISTORY_JOB_ENABLED: enable a report history job to delete expired file. Default `true`
- SET_EXPIRED_REPORTS_DELAY: delay time between each report history job run. Default `86400000` as 1 day
- LIMIT_CONTENT_PER_SHEET: limit content per sheet of export file, default `1000000`
- API_CHECK_REWARD_URL: api url to fetch reward
- API_CHECK_POOL_HISTORY_URL: api url to fetch pool history
- NETWORK_NAME: cardano node network name (preprod, testnet, mainnet)
- KAFKA_CONFIGURATION_ENABLED: enable kafka configuration, default `true`
- KAFKA_BOOSTRAP_SERVER_URL: kafka bootstrap serve. Default `localhost:9092`
- KAFKA_GROUP_ID: kafka group id.
- KAFKA_REPORTS_TOPIC: kafka topic to consume report. Default `dev.explorer.api.mainnet.reports`

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
- POOL_OFFLINE_DATA_JOB_ENABLED: enable fetch pool offline metadata job
- CRAWL_POOL_DATA_DELAY: delay time between each crawl pool metadata time)
- META_DATA_JOB_ENABLED: enable metadata job
- TOKEN_METADATA_URL: url that store token
- TOKEN_METADATA_FOLDER: store token
- REDIS_SENTINEL_PASSWORD : Redis sentinel password. Default is redis_sentinel_pass.
- REDIS_SENTINEL_HOST : Redis sentinel host. Default is  cardano.redis.sentinel.
- REDIS_SENTINEL_PORT : Redis sentinel port. Default is 26379.
- REDIS_SENTINEL_MASTER_NAME : Redis master name. Default is mymaster.
- TOP_DELEGATORS_FIXED_DELAY: top delegator fixed delay when run cron job
- POOL_STATUS_FIXED_DELAY: delay time between each time get pool status
- NUMBER_DELEGATOR_FIXED_DELAY: delay time between each time get number delegator
- UNIQUE_ACCOUNT_FIXED_DELAY: fixed delay for job build cache unique account
- UNIQUE_ACCOUNT_ENABLED: enable unique account job
- STAKE_TX_BALANCE_JOB_ENABLED: enable stake tx balance job
- STAKE_TX_BALANCE_FIXED_DELAY: fixed delay for job stake tx balance
- TOKEN_INFO_JOB_ENABLED: enable token info job
- TOKEN_INFO_FIXED_DELAY: delay time between each time run token info job
- AGGREGATE_POOL_INFO_FIXED_DELAY: fixed delay for job aggregate pool info
