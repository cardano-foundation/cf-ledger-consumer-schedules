# Cardano Schedules
Run schedule task
## Environment value
- HOST: database host
- POSTGRES_PORT: database port
- POSTGRES_DB: database name
- POSTGRES_USER: database access user name
- POSTGRES_PASSWORD:database user password
- POSTGRES_SCHEMA: database schema
- CRAWL_POOL_DATA_DELAY: delay time between each crawl pool metadata time)
- INSERT_POOL_DATA_DELAY: delay time between each listen to crawl process
- NETWORK_NAME: cardano node network name (preprod, testnet, mainnet)
- BOOSTRAP_SERVER_HOST: kafka host
- BOOSTRAP_SERVER_PORT: kafka port
- SPRING_PROFILES_ACTIVE: active profiles
- PORT: port running
- TOKEN-METDATA-URL: url that store token
- TOKEN-METADATA-FOLDER: store token
## Task
### Extract ledger state & insert
#### Require 
 - Docker (permission read docker.sock file to create container ) ```chmod -R o+r docker.sock```
 - Cardano-node image ```docker image pull inputoutput/cardano-node:latest```
#### Description
 Extract ledger dump file every epoch start from shelley era then wait consumer send message that consumered to extracted epoch then insert to database
### Crawl pool metadata
 Crawl pool name from internet, receive json only and max body size 512 bytes
### Crawl token metadata
 Crawl token metadata from internet
