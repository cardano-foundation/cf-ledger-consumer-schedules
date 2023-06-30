# Changelog

## [0.2.4](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.3...v0.2.4) (2023-06-30)


### Features

* [MET-1154] add job cache for frequently called Token page ([63ab92f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/63ab92f8948fe0bc51a4985ce001e453e9d6c468))
* [MET-1154] add job cache for frequently called Token page ([4f31bc1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4f31bc18d736830286e0e55eefd318159e4ae54d))
* [MET-1154] add unit tests ([7c16e99](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7c16e9986cd4fa39e4dd2488406468097211b613))
* [MET-1154] resolve conflict ([ddd1b6b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ddd1b6b31f4cf74a694d1b98889934ecef15a959))
* [MET-1154] resolve conflict ([a63f40e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a63f40ecfc47f644b3b6fad28e1b0c77b1bb1209))
* added redis support for cluster and standalone mode ([#88](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/issues/88)) ([3b80d3c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3b80d3c88c42bac2577d40af39409cb158142816))
* met-1346 pool offline data logo, icon ([a75c330](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a75c3307debc226c366e680cb7c3f878e159690d))
* met-1346 update existed pool ([c5e0850](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c5e08504ea89eed8a9fc5658fcc2c1dfbf0865b9))
* MET-1392 get pool status and save to redis ([1205d2b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1205d2bfe600525638bed5173568c60b629441ff))
* MET-1393 add schedule for number delegator, refactor PoolStatusSchedule ([5f7c95e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5f7c95e105b92e5cfe47960df9f9dec0a3eedd41))


### Bug Fixes

* check certificate when retire and update in the same tx ([5b07c11](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5b07c11979edf5fa17471d31fa0df698d168d1df))
* **gha:** fixed PR builds ([bc79eeb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/bc79eeb67e53a55df813c46577a8ce94003a3e62))


### Miscellaneous Chores

* release 0.2.4 ([9954a24](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9954a241d1536b8fb5a71c138850078bcaa36b78))

## [0.2.3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.2...v0.2.3) (2023-06-22)


### Bug Fixes

* update README.md and generate a new version ([9078cbb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9078cbbcc5a129c9a0a059b7e389820eaf7f8d87))

## [0.2.2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.1...v0.2.2) (2023-06-22)


### Features

* [MET-1139] update job calculate agg for case Consumer interrupted ([d7e61de](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d7e61de843dbcc89b886a49c570bb95af8321551))
* [MET-1348] move job to schedule ([f59fdee](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f59fdee63a391d2b8cf2336369920a61e35dc76d))
* [MET-1348] remove space ([830e6c3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/830e6c307c0f6a3f4d245719a26ed19c9aa5766b))
* add log back ([144b07d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/144b07d6edb01b696ab3a3111ac580c6afa037fb))
* optimize-fetch-pool-offline-data ([4a4e388](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4a4e388780212a3bd7e1a0b5fe51f65623b4917a))


### Bug Fixes

* add flyway for update columns of asset metadata table ([2f0197d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2f0197de53aab1548a1ed1afae4f79ecfe8094ae))
* add pool history fetching flow to export data ([133dc7d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/133dc7db385f27adc9cb06d86070b5bc0e4ad966))
* **gha:** fix condition for main branch workflow trigger ([3dc08aa](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3dc08aa5568e37e9050564ff46261407ed676f12))
* remove transaction-type column in export file ([6b0dd67](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6b0dd67e3eb04fefe79cc2df158491d26a2b28a7))
* removed dirty changelog ([4e4242a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4e4242aa7d71cb44409f4e87a3c8987bbd35ba0d))
* removed dirty changelog ([e0ebb5b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e0ebb5b1ff2ed55bdcace2ae3f03b3c88041db56))
* update fetching poolhistory flow after change entity ([fa71447](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fa714471d79cdc62a8cbd8da1d4ade94fbab8996))


### Performance Improvements

* optimize flow of set-expired-report-history job ([00d2487](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/00d2487011e1a02fbd5e92ee7c982e1d6b323bde))


### Documentation

* update docker env in docker-compose and README file ([2732d0a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2732d0afcc8679d4e8276867a6380a19ece61521))
* update redis sentinel env ([45fad1c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/45fad1cb020a61ddba10f7f30b8f67a182ab7e49))
