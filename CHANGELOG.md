# Changelog

## [0.8.0](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.7.0...v0.8.0) (2024-02-09)


### Features

* add migration file to drop unused table in ledger-sync db ([eaaf0cc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/eaaf0cc69aa27f3e8cc5fd9d9cbd49d16f7a5be1))


### Bug Fixes

* remove unused migration in ledger-sync-v2 ([600fb4d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/600fb4d4f39fae74e3b5a8273a3f0067c8e13eec))

## [0.7.0](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.18...v0.7.0) (2024-01-23)


### Features

* mapping fingerprint from subject ([956ead1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/956ead12abc22b5cbb16d60208196167bc899b3f))
* MET-1761 add table verified_script ([33bbb2c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/33bbb2c04dd97592cb112ce756eb68e1680848a1))
* MET-1856 add flyway to create index related to multi_asset and script ([7cc35f9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7cc35f9d88721d1da0cf5a99a15b7d97630444db))
* MET-1856 add unit test for native script info schedule ([71d8d4e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/71d8d4e4149d71bffee114b22e9030f179fb7aaf))
* MET-1856 update env for native script info job ([ad13478](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ad13478dfa831e8b5e930b40aaf016f0b7dbe5bf))
* MET-1865 check native script not have token ([99001e3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/99001e35d62001fe4602b81ea24ffc5825f839b8))
* MET-1865 create job sync native script info ([2a8bad3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2a8bad3f00e3f2d6ae55032dca820e0c37007c1d))


### Bug Fixes

* add default sort for paging ([e105334](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e105334f13a4ad7079388063abea20d2a1f73994))
* check update remove logo in token metadata ([41a46b6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/41a46b609db7ff00bad0efa5efd7fc7fed5076d7))
* delete old poolIds inactive when run job pool status ([eec3e16](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/eec3e16a4ac591e28476166e0e409a14ff4f7d86))
* remove network in flyway sql ([2e55ff2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2e55ff2423f0c538ae2841eff9c82548a9648b89))
* sync-up logic stake and pool lifecycle when generating report ([5dee72d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5dee72dc35f6acd67d634f45a1b456e8713bf451))
* update logic to save logo of token ([20b1075](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/20b107589b884642edc12833ca58ff0b346f8f90))
* update redis standalone and cluster to synchronize with other services ([723899a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/723899ab4ae21fca7f5a89056aed2dcc1bdf5899))
* wrong input param when get existed data ([e6b890d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e6b890d0b489c191b735215f524f69b2284e7c56))


### Performance Improvements

* improve perf token info job by remove logic get multi-asset data ([a6b105f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a6b105f034181475b016759f63e89e260e817229))

## [0.2.18](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.17...v0.2.18) (2023-11-09)


### Miscellaneous Chores

* release 0.2.18 ([a57c88a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a57c88a4ac285a0b8b74e16e8a362f0e716f6e90))

## [0.2.17](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.16...v0.2.17) (2023-10-30)


### Bug Fixes

* MET-1700 fix wrong condition when update existing token info ([8023af5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8023af5d37148485bc5a22f6edf972092dc5de41))

## [0.2.16](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.15...v0.2.16) (2023-10-11)


### Features

* MET-1700 add aggregated data of token scheduler ([c47e6ea](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c47e6eafbea51d7e7e5bd9aa6a9f1d95495d2910))
* rename flyway name ([5cb908a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5cb908aec991c3a9f5d65a8ccb570a285030f85e))
* update pool status for each pool ([102a7b2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/102a7b2065443fe89f9fdade6c6b7966d4ad3d1e))

## [0.2.15](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.14...v0.2.15) (2023-10-06)


### Features

* MET-1528 add StakeTxBalance job to sync data for stake chart ([d225ca7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d225ca745582956e12366704720ff2acec277abf))
* MET-1654 add poolName column to stake-key delegation transactions in export file ([2cd5040](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2cd5040eb3977b52bae0719256601ef7d5a73496))


### Documentation

* add env for stake-tx-balance job ([6fb26a4](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6fb26a4a6ebe0a799c1fb71b83fc5dc8771fc8ef))

## [0.2.14](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.13...v0.2.14) (2023-08-28)


### Bug Fixes

* drop column ts_json of tx_metadata ([89c3ba7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/89c3ba711712a9e93fe41f44fcb538d2a74c153f))
* MET 1479 return empty if epoch no is null ([de5ad60](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/de5ad601b66e9680f4f9dd38ba9e8bfd42cf2670))

## [0.2.13](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.12...v0.2.13) (2023-08-18)


### Bug Fixes

* MET-1590,MET-1599 update logic insert pool offline data ([7f91636](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7f916364bf2bada7bec6083ee566cc897b3e1c1d))

## [0.2.12](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.11...v0.2.12) (2023-08-08)


### Bug Fixes

* added string key serialiser ([2db645e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2db645e8ba84bfd89f4eee2ddc4555933afe64ea))

## [0.2.11](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.10...v0.2.11) (2023-08-08)


### Bug Fixes

* attempting to force a release ([cbf2a06](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cbf2a066ea3d7545437f0a160077ddc47a9b913d))

## [0.2.10](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.9...v0.2.10) (2023-08-02)


### Bug Fixes

* attempting to force a release ([70c08b9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/70c08b99f1b23bdbfcfbf7c6f05484f9039c84fc))

## [0.2.9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.8...v0.2.9) (2023-07-27)


### Features

* MET-1488 store logo of token to storage ([8d9817e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8d9817e4a5367e473ec2137155749a450ccf4316))
* **tests:** add unit-test for report func ([202f216](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/202f2163b4502717d0810110fce67c726152c0d3))

## [0.2.8](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.7...v0.2.8) (2023-07-18)


### Features

* MET-1271 fix create redis key ([7f43a68](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7f43a68ae3ad1f490d0da4adc679b58819a17699))


### Bug Fixes

* reward distribution of pool report not in epoch range ([0b96784](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/0b96784f153a01dee55b13642ebead3b1fcc04ed))

## [0.2.7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.6...v0.2.7) (2023-07-11)


### Features

* [MET-1139] update job calculate agg for case Consumer interrupted ([d7e61de](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d7e61de843dbcc89b886a49c570bb95af8321551))
* [MET-1154] add job cache for frequently called Token page ([63ab92f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/63ab92f8948fe0bc51a4985ce001e453e9d6c468))
* [MET-1154] add job cache for frequently called Token page ([4f31bc1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4f31bc18d736830286e0e55eefd318159e4ae54d))
* [MET-1154] add unit tests ([7c16e99](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7c16e9986cd4fa39e4dd2488406468097211b613))
* [MET-1154] resolve conflict ([ddd1b6b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ddd1b6b31f4cf74a694d1b98889934ecef15a959))
* [MET-1154] resolve conflict ([a63f40e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a63f40ecfc47f644b3b6fad28e1b0c77b1bb1209))
* [MET-1348] move job to schedule ([f59fdee](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f59fdee63a391d2b8cf2336369920a61e35dc76d))
* [MET-1348] remove space ([830e6c3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/830e6c307c0f6a3f4d245719a26ed19c9aa5766b))
* add common util for report func ([50402aa](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/50402aa07f74f9ea43c13e31ad385d76dfdca2af))
* add docker ([c9dd6f6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c9dd6f66e0592c4f80b8df6f9cde7de05ece931d))
* add log back ([144b07d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/144b07d6edb01b696ab3a3111ac580c6afa037fb))
* add reward fetching flow for queue-report ([f9fda2d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f9fda2d8563d6b1fe54e4d07ee0ba3789b3dfe90))
* add ts_json column for full text search in tx_metadata ([d00571d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d00571d28ef9880ba2bcc233cad3a5dc3036a66d))
* add unit test for export func ([2512fd6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2512fd68774f015b00f90ba5772e63fc7661bfeb))
* added redis support for cluster and standalone mode ([#88](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/issues/88)) ([3b80d3c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3b80d3c88c42bac2577d40af39409cb158142816))
* change src package name to org.cardanofoundation ([4398900](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/439890009abd441850919de468681e62dbeece4c))
* change time run aggregate job to 0:15, make sure no rollback ([3e0f7da](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3e0f7da54dfeeee96b0c0fcc22503dac7e7a2d9a))
* crawl metadata of multi asset ([54b302a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/54b302ae4d237291610b783c2220a70622a05e73))
* crawl pool offline metadata ([271ee7a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/271ee7a55a9550ce7f70d0eda5fc4500d0c3dff1))
* force 0.0.2 release and testing snapshot version update for develop branch ([67b0a21](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/67b0a21eb16f7aeed65feb205d1255f5641ac44c))
* force 0.0.2 release and testing snapshot version update for develop branch ([607f27d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/607f27da2f3f89f5cf471d406aeb2192fa289355))
* force 0.0.2 release and testing snapshot version update for develop branch ([e6ea242](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e6ea2421f18ba7cc9852190191d11e06b6440053))
* force 0.0.3 release and testing snapshot version update for develop branch ([58a98ff](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/58a98ff99bd53b6812322d6c3c4bc643739d3ebc))
* gitignore ([79035cc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/79035cc95c19feb4e5a57f0677f606ca6188eaed))
* kafka, redis configuration. Query cardano cli ([ed87b15](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ed87b1568bb0a5bdc00a27f287a885ec7e395cf2))
* log, crawl pool offline data and crawl test ([76c9071](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/76c9071c5d4c7a76ca213ee14dcd8702d011cd9f))
* MET-1076 enable flyway, add scrip migration for report function ([a8ff9bc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a8ff9bc0ab6c4c5cb8fbc09056901bdc306c785c))
* met-1346 pool offline data logo, icon ([a75c330](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a75c3307debc226c366e680cb7c3f878e159690d))
* met-1346 update existed pool ([c5e0850](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c5e08504ea89eed8a9fc5658fcc2c1dfbf0865b9))
* MET-1392 get pool status and save to redis ([1205d2b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1205d2bfe600525638bed5173568c60b629441ff))
* MET-1393 add schedule for number delegator, refactor PoolStatusSchedule ([5f7c95e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5f7c95e105b92e5cfe47960df9f9dec0a3eedd41))
* met-777 report queue system ([ff3aa62](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ff3aa62f9c3f540a9505fc2990d87b058437b71c))
* migrate to spring boot 3 ([08c124f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/08c124fb4aaf829ef78b663772c66482c0ed7c36))
* optimize-fetch-pool-offline-data ([4a4e388](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4a4e388780212a3bd7e1a0b5fe51f65623b4917a))
* remove m2 setting.xml ([a8cee61](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a8cee61d69ae515608766e1baaa68b06dd52f8c7))
* setup deployemnt for report queue ([085d745](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/085d745b6fbcc1e05095dfc7234f742fde1279d2))
* update README.md trigger build pipeline ([190beb4](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/190beb4e7a5766d3fee57dffd3d7459a3ba3fc7a))


### Bug Fixes

* add flyway for update columns of asset metadata table ([2f0197d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2f0197de53aab1548a1ed1afae4f79ecfe8094ae))
* add pool history fetching flow to export data ([133dc7d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/133dc7db385f27adc9cb06d86070b5bc0e4ad966))
* bug mapping objects with json pool offline data ([c26d0cf](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c26d0cf507855dcbe760d9e14e73bc8f048dfbaf))
* bug throw illegal argument exception for send request to ip:port address ([92ab6e7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/92ab6e7012cef4bcaa63c0a1edf4f9107deda3e1))
* bug wrong poolOfflineData foreign key ([635d84c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/635d84c47c50d672adafe4a2f45a993facae08a8))
* check certificate when retire and update in the same tx ([5b07c11](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5b07c11979edf5fa17471d31fa0df698d168d1df))
* fix ada transfer type, fetching reward flow ([84100a2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/84100a2781ee6120cbab16e226e38a5b28a6323a))
* fix bug export raw balance value ([9164078](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/916407898c2b4d223641b70e1d2a21cff24f6c5d))
* fix wrong uploaded_at datatype ([cc038db](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cc038dbb46ce48d780e1ace4b7867a4fd68f4dae))
* get sum of balance of address_tx_balance, add post contruct with agg job ([fcab4b1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fcab4b194ff8d08ccaef30d960bcaea3c256211a))
* **gha:** fix condition for main branch workflow trigger ([3dc08aa](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3dc08aa5568e37e9050564ff46261407ed676f12))
* **gha:** fixed docker image tag env var ([19f3efd](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/19f3efd03f9caffd9c1caa5c52e02aaa421e1e27))
* **gha:** fixed PR builds ([bc79eeb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/bc79eeb67e53a55df813c46577a8ce94003a3e62))
* Insert Flow Change From Snapshot mark to Go ([85e2e58](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/85e2e5852704e134c6fa38224dd85136ff4f2559))
* Insert Flow Change To Stake Set, Manager Docker Flow ([ace53da](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ace53dad24c62829965344de577497399a6ce347))
* remove cli ([5b99f8e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5b99f8ea110295e2d204258dbdabf067df5cb1e6))
* remove transaction-type column in export file ([6b0dd67](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6b0dd67e3eb04fefe79cc2df158491d26a2b28a7))
* removed dirty changelog ([4e4242a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4e4242aa7d71cb44409f4e87a3c8987bbd35ba0d))
* removed dirty changelog ([e0ebb5b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e0ebb5b1ff2ed55bdcace2ae3f03b3c88041db56))
* update fetching poolhistory flow after change entity ([fa71447](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fa714471d79cdc62a8cbd8da1d4ade94fbab8996))
* update README.md and generate a new version ([9078cbb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9078cbbcc5a129c9a0a059b7e389820eaf7f8d87))


### Performance Improvements

* optimize flow of set-expired-report-history job ([00d2487](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/00d2487011e1a02fbd5e92ee7c982e1d6b323bde))
* optimize performance, change limit size ([5771c3d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5771c3d867372d985b15487efb288a883c7f4c9a))


### Documentation

* change readme describe ([c73b94d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c73b94d0934cfd267b72bd5541ded06a6ff50c5c))
* update docker env in docker-compose and README file ([2732d0a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2732d0afcc8679d4e8276867a6380a19ece61521))
* update environment values ([1d5170b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1d5170b0925db80939744f6f742a78f1c8a469ff))
* update redis sentinel env ([45fad1c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/45fad1cb020a61ddba10f7f30b8f67a182ab7e49))


### Miscellaneous Chores

* release 0.2.7 ([9954a24](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9954a241d1536b8fb5a71c138850078bcaa36b78))

## [0.2.6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.5...v0.2.6) (2023-07-11)


### Features

* change time run aggregate job to 0:15, make sure no rollback ([3e0f7da](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/3e0f7da54dfeeee96b0c0fcc22503dac7e7a2d9a))


### Bug Fixes

* get sum of balance of address_tx_balance, add post contruct with agg job ([fcab4b1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fcab4b194ff8d08ccaef30d960bcaea3c256211a))

## [0.2.5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.2.4...v0.2.5) (2023-07-04)


### Features

* update README.md trigger build pipeline ([190beb4](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/190beb4e7a5766d3fee57dffd3d7459a3ba3fc7a))

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
