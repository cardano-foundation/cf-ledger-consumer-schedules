# Cardano Schedules
Run schedule task
## Environment value
- POSTGRES_HOST: database host
- POSTGRES_PORT: database port
- POSTGRES_DB: database name
- POSTGRES_USER: database access user name
- POSTGRES_PASSWORD:database user password
- POSTGRES_SCHEMA: database schema
- MAXIMUM_POOL_SIZE: Schedule will have job select parallel from database. If you want to task schedule as fast as possible set the `MAXIMUM_POOL_SIZE` as much as possible (cpu core * 4). This will reduce another app performance. 
- FLYWAY_ENABLE: Migrate schema, set `true` if this is the first time run app
- FLYWAY_VALIDATE: Flyway schema validation, default `false`
- REPORT_HISTORY_JOB_ENABLED: enable report history job to delete expired file. Default `true`
- EXPIRED_REPORTS_RATE: delay time between each report history job run. Default `86400000` as 1 day
- API_CHECK_REWARD_URL: api url to fetch reward
- API_CHECK_POOL_HISTORY_URL: api url to fetch pool history
- NETWORK_NAME: cardano node network name (preprod, testnet, mainnet)
- KAFKA_CONFIGURATION_ENABLED: enable kafka configuration, default `true`
- KAFKA_BOOSTRAP_SERVER_URL: kafka bootstrap serve. Default `localhost:9092`
- KAFKA_GROUP_ID: kafka group id.
- KAFKA_REPORTS_TOPIC: kafka topic to comsume report. Default `dev.explorer.api.mainnet.reports`
- S3_ACCESS_KEY: the AWS access key 
- S3_SECRET_KEY: the AWS secret key 
- S3_REGION: the AWS region 
- S3_BUCKET_NAME: the AWS bucket
- S3_STORAGE_ENDPOINT: the storage endpoint, only for S3 clone (either on localhost, Minio, etc.)
- SPRING_PROFILES_ACTIVE: active profiles
- LOG: application log level 