# Cardano Schedules
Run schedule task
## Environment value
- HOST: database host
- POSTGRES_PORT: database port
- POSTGRES_DB: database name
- POSTGRES_USER: database access user name
- POSTGRES_PASSWORD:database user password
- POSTGRES_SCHEMA: database schema
- CARDANO_CLI_DELAY: delay time between each cli query (default is 10 ms for preprod and 2000ms for testnet and mainnet)
- CRAWL_POOL_DATA_DELAY: delay time between each crawl pool metadata time
- NETWORK_MAGIC: cardano node network magic number (1 - preprod)
- MAGIC_PARAM: cli network magic id (--mainnet for mainnet, --testnet-magic for testnet and preprod ...)
- DOCKER_HOST: path to docker host
- IMAGE_ID: cardano node image id in docker
- NETWORK_NAME: cardano node network name (preprod, testnet, mainnet)
- BOOSTRAP_SERVER_HOST: kafka host
- BOOSTRAP_SERVER_PORT: kafka port
- EPOCH_TOPIC: epoch topic 
- SPRING_PROFILES_ACTIVE: active profiles
- PORT: port running
## Task
### Extract ledger state & insert
#### Require 
 - Docker
 - Cardano-node image ```docker image pull inputoutput/cardano-node:latest```
#### Description
 Extract ledger dump file every epoch start from shelley era then wait consumer send message that consumered to extracted epoch then insert to database
### Crawl pool metadata
### Crawl token metadata
