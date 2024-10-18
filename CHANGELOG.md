# Changelog

## 0.9.0 (2024-10-18)


### Features

* add composite index based on tx_hash column ([0aca6ac](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/0aca6ac47118ea966b5dd928303d41e50fd8ff4b))
* add gov action proposal info job ([a0b6e80](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a0b6e806bbf35f665a69514ae66996e3b278bffc))
* add information page for exported report ([5496576](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/54965768b130de3b493509988c0ecc92fe22896d))
* add job to aggregate Committee info ([785dc3a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/785dc3a1a32416ef12133699f37f2748a12fdff6))
* add job to refresh Materialized View latest_stake_address_balance ([cf0e1b5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cf0e1b52155e01f5ad3607aca7bb0711d605e6f4))
* add locking concurrent task ([44888b8](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/44888b88c619df4803c57906439399cc8cfea3bd))
* add materialized view to agg balance tables ([ce1f047](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ce1f047b232ecd9dd3c5f2a2825d90f495f5e6e6))
* add migration file to drop unused table in ledger-sync db ([eaaf0cc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/eaaf0cc69aa27f3e8cc5fd9d9cbd49d16f7a5be1))
* add new ls agg dbsource configuration ([7c12312](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7c123124e1f94ab52fa0e4fade7db1bfb96bf0e4))
* add on conflict do update for AddressTxCountSchedule ([9ec9178](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9ec9178789d00b2b4d089c4f71ae62b27e814ce0))
* add scheduled job to remove history record (unused) from address_balance ([5617b51](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5617b518d555c1fc54be5477db8dbd01dbdaa7b9))
* add scheduled job to remove history record (unused) from stake_address_balance ([66ef5bb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/66ef5bb5237f3f905d6d0be4ea1d40d4710fa10d))
* add stake_address_tx_count mat view ([b360518](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b36051852ed7e74763e68f3e0d8196a11de75aa5))
* add trigger on block table with postgres insert operation ([7184398](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7184398727836ba98cb7e1727ca7960f814cd633))
* add voting procedure job ([aafb363](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/aafb3631c271c89b3ca96bb60a622309d82b2d29))
* aggregate total token count ([7d501e7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7d501e70a4a221c4370c7427fbdd54fc81010c07))
* block distribution per epoch ([2c55e8f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2c55e8ff11d976d7b168b8f7344ebda28b1378d2))
* caculate delegators of DRep ([9e344d7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9e344d72877d8a80e66c3cadba8b7a9ad3cc7997))
* caculate governanceParticipationRate ([b8d69fa](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b8d69faebe99981a2fea3a66a4b46d8a16832986))
* calculated votingPower in agg pool info ([bc413e4](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/bc413e487db32de81516c441a6add1951fca1106))
* create job to count address transaction ([4fa6509](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4fa6509eed55afca74743b42e6d2aded2eb42688))
* create job to count token tx and add index for address tx count table ([c145441](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c1454412fb67f0f4a38627ac89c0e4a953c0927b))
* drop index before first time init ([ca169fa](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ca169fac1178e024e4a262c58375ae899b712d2e))
* export report with date time pattern ([8efa012](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8efa012ff57e490ceb4ad08226b790bbef0f1235))
* fetch and index data reference by anchor link ([4f638e3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4f638e31e1a9afc7ccfd9666656597c869d8d696))
* indexing column tables that relevant gov action ([95e35b6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/95e35b630d51062df8d8339c8235bb4ac4af4fdb))
* mapping fingerprint from subject ([956ead1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/956ead12abc22b5cbb16d60208196167bc899b3f))
* MET 1963 add drep info schedule ([2a0917f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2a0917f9f7d2da198b3245736c93ed44e626659f))
* MET-1761 add table verified_script ([33bbb2c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/33bbb2c04dd97592cb112ce756eb68e1680848a1))
* MET-1856 add flyway to create index related to multi_asset and script ([7cc35f9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7cc35f9d88721d1da0cf5a99a15b7d97630444db))
* MET-1856 add unit test for native script info schedule ([71d8d4e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/71d8d4e4149d71bffee114b22e9030f179fb7aaf))
* MET-1856 update env for native script info job ([ad13478](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ad13478dfa831e8b5e930b40aaf016f0b7dbe5bf))
* MET-1865 check native script not have token ([99001e3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/99001e35d62001fe4602b81ea24ffc5825f839b8))
* MET-1865 create job sync native script info ([2a8bad3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2a8bad3f00e3f2d6ae55032dca820e0c37007c1d))
* migrate flyway ([2b05825](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2b058251af5b1c9350a38b87743864531c5ccc53))
* modify condition that check status of DRep ([cc4b76d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cc4b76dff79c568ab2599ec79ea775f4395ed65b))
* move DRepInfo table to ledger sync db ([a7e5b67](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a7e5b6787b2f02c980e2671fa32d14822e1ecef6))
* plutus v3 support ([dd2244b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/dd2244b485c5107edfe430cfa29ef52ec0ff7f3a))
* remove initialDelay for AddressTxCountSchedule ([8661636](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8661636d617d7f1d141f801517a28d2020b7c670))
* replace latest_token_balance materialize with incrementally mechanisms ([001b5a8](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/001b5a8108fce8bfe3e2c37f0cf7470fd11205e6))
* rewrite token info scheduled job ([571a9bb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/571a9bb0b1560cef4a907661799b9e75ba7c90a6))
* **sanchonet:** switch profile not koios and check reward data avaiable for report data ([2ad01cc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2ad01cc782185d70976f162e3638450255e97988))
* sync address_tx_count ([8df282f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8df282f559f0e3d95087e21cb3f71db2b99938f9))
* sync stake_address_tx_count ([62562d7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/62562d7dcb5720ebcbd37473ec1ccf8f2797b3a7))
* update AggregateAnalyticSchedule ([48754c1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/48754c18f00eb4f769a92f5c73c0fc2c807a1987))
* update anchor download to support IPFS too ([8c64144](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8c64144e9e30c9b140a68526aafe2a540be441e8))
* update docker-compose.yml ([c022933](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c022933334142a28a9b328f9ae001a2795d09900))
* update DRepInfoSchedule ([ab10455](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ab104558e05f9d0c6079ab767f463b903efb6b90))
* update incremental with checkpoint for address_tx_count ([1c1fbf7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1c1fbf7486528a66ba6fa7bb3f736dc89c8aea58))
* update logic after separate common entity ([11d953b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/11d953b30df4b76907fd950888ac79af03d0b239))
* Update logic to aggregate token info data: txs count, number of holders, total volume, volume 24h ([261c187](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/261c187648c256991d3e70494a5a5c15858742a5))
* Update logic to get ADA transfer in staking delegation lifecycle report ([2301f1b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2301f1b388feeb393bf1e00f776792744a479562))
* Update logic to get number of asset holder of native script ([40b84db](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/40b84db16eba025896529bbf839347b77e335421))
* update NativeScriptInfoSchedule ([eaf19c9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/eaf19c9142e18d7fa30fdfb3ae8f8825d02c5266))
* update TokenInfoSchedule ([d19ad3e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d19ad3ec2068b5bfb82f70791abf6d6fe7a56770))
* update votes section ([b8f6eca](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b8f6eca2005228adcd4ce3e713148af0e98d3c22))


### Bug Fixes

* activeVoteStake and liveStake must be null ([6b4f5fc](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6b4f5fc8a0375646a164f2afb360e1256006c2fa))
* add default sort for paging ([e105334](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e105334f13a4ad7079388063abea20d2a1f73994))
* add fixed delay to consuming report and replace query get withdrawals on stake key report ([623132c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/623132c386f318f25e6190cbab82d5370c47a772))
* add logic check exists when updating table ([5e4cc48](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5e4cc48dec1cedee2e4c236e56671801151a0250))
* add logic to process token info in batches and add comments to the code ([599158c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/599158c04a325f93150b6a55a80a727a4076df60))
* add missing condition when create latest_token_balance view ([b744ae9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b744ae90394021eeb52ae6993496095e4bdb3263))
* adjust content length limit for url fetch ([1748b76](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1748b76cb0eb1df4074a8e185f60c41da7f4fd24))
* **AggregateAnalyticSchedule:** replace redis checkpoint concurrentTask with AtomicInteger to ensure thread-safe ([1c1a22b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1c1a22b7c6873e9987a61bd48e893d903911a39e))
* can't generate stake report ([a2905e8](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a2905e8274ac4d653c8984848a362f3101fb3fb9))
* can't update token-info after init ([ed1c8f5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ed1c8f5cbd2f2bbbb938511cc874747665fc3ecd))
* cannot convert enum to string in key hash redis ([541c37b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/541c37b8c82144ab73375e60b1e41866379aab53))
* catch exception when generate fingerprint for asset metadata ([d3125cf](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d3125cf37f06dd78984c8fcf9695f6ff65ed4c08))
* change condition that determined the status of gov action ([fcf232d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fcf232dedeac0576884ea7781c44d37a05fc416c))
* check update remove logo in token metadata ([41a46b6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/41a46b609db7ff00bad0efa5efd7fc7fed5076d7))
* compile fail ([42c9e9a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/42c9e9a84405b36ca9eac42e131dcba3318c6a39))
* delete old poolIds inactive when run job pool status ([eec3e16](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/eec3e16a4ac591e28476166e0e409a14ff4f7d86))
* don't compare max block checkpoint when update token-info ([bfe8701](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/bfe8701d968da0a4f4d432817fd15afae6ec374d))
* dRep inaccurately set as inactive ([e5dfb6f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e5dfb6f7e3e2f5afd22be44252657afbf0d42054))
* enable VotingProcedureSchedule ([c6ae496](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c6ae49690975bad4f2a93bcb5ea761cf52133666))
* error when generated stake address report ([89a054d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/89a054d42d5a381b496d61f56cd428c13f1517b6))
* ignore insert zero holders for the first time init ([8a693eb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8a693ebfbb04f90f6b18e88ee523d7c41601d270))
* issue with display gov action enum ([cfed237](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cfed237ecec90ccb88436060b254ceeccf25179d))
* MET-1700 fix wrong condition when update existing token info ([8023af5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8023af5d37148485bc5a22f6edf972092dc5de41))
* migrate flyway failed ([e955be9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e955be98294b1b557152d245db8ac286ab43d59a))
* missing drop old view even though have new view structure ([2eaf560](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2eaf560e3e91b07b552983883165861594697886))
* missing first stake address slice when init stake address tx count ([7b10109](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7b1010958f684080418e488ba58eec5e0d0cb8f4))
* modify fields to be nullable ([6ff1784](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6ff17844516848e40f53c5255807948c5180c560))
* modify logic to aggregate token info ([5f80a18](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5f80a1878a593803ed36bad5531454cabe482579))
* name the variable incorrectly ([21f49e9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/21f49e9a3cf30bbca9ee43ffa171be628eb0c11c))
* NaN value when divide to zero ([6651a06](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6651a065da915ceb2a2ea945dde4112287f154be))
* recalculate gov participation rate of SPOs ([fe0a4c2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fe0a4c28c50ac2bcc9c20ec41e6476b099a94e8d))
* recalculate the total volume of token info ([38f3e23](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/38f3e23f2b5cbd91b04fbcedde30d222b33a7f56))
* remove network in flyway sql ([2e55ff2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2e55ff2423f0c538ae2841eff9c82548a9648b89))
* remove unused migration in ledger-sync-v2 ([600fb4d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/600fb4d4f39fae74e3b5a8273a3f0067c8e13eec))
* remove zero holders record in latest token balance ([abbf7cb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/abbf7cb35db8184999870213b5817ceee7d973ea))
* rename value of @Transactional ([b8f003b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b8f003bc0487de30c79fe6ab9d9488ca01f2b78b))
* replace logic save checkpoint for native script info job ([a63a92b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a63a92b19e1609cd2cedab70d4d6f1cb52796b21))
* rollback BatchUtils to synchronously process ([aa653f2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/aa653f2c24046a9f530fd1135231756b38949b35))
* rollback to "old" query ([795a160](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/795a160541e0d8fa088f688bb3427d634f373a0e))
* round the double value ([87d8101](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/87d8101f64614cbe44a5d8520304ee5e52f48752))
* round the double value of gov participation rate of drep info ([e047a6e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e047a6e8bcc25ac75ac5acc6d89b9948e54b4652))
* sync-up logic stake and pool lifecycle when generating report ([5dee72d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/5dee72dc35f6acd67d634f45a1b456e8713bf451))
* TokenInfo blockNo checkpoint should compare with LatestTokenBalance blockNo checkpoint ([d5bd44f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d5bd44f55eeaf92d11c4ae14569722823c8faf7a))
* TokenInfo schedule, NativeScriptInfoSchedule now work ([c6b00f0](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c6b00f08556d73282e077b0a6ea2c84fd59d8273))
* trying to make initial token sync work ([68d85b9](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/68d85b953f02a404ada4d8bd910fd4c80f0cdae3))
* trying to remove redundant RedisTemplate ([8500aef](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/8500aef2d958e824f90df42c4b5f67f6123cad9a))
* unique account of epoch 0 ([c96f984](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/c96f9841ce0cb960cdb73ad26f68b4494c4cc3df))
* unique account variable on current epoch equal 0 ([2861e1a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2861e1a5c1450b99e2e4532b5450bc58559a26ff))
* update checkpoint logic ([d8ade5a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d8ade5ae812e3f91cc2a4be9962e5fa67237a12b))
* update logic syncing stake address tx count ([b03e96c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/b03e96c8e0f0283bd483ba525f0c36b32caa52c5))
* update logic to get holder of native script ([ed275c5](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ed275c5c7361cfed0827f45e8f69c66a5afdd109))
* update logic to get holder of token and native script ([6fd651a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/6fd651a0244db87387bfbffb7be0d18e6ca7baf7))
* update logic to get unique account from AddressTxAmount table ([20bd5b0](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/20bd5b0b1390881b495d8656484a0b577da9dac1))
* update logic to save logo of token ([20b1075](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/20b107589b884642edc12833ca58ff0b346f8f90))
* update materialized view latest_token_balance ([45211a7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/45211a792f69ac4ea92e73a826a955842a9a5cc5))
* update migration agg_address_token ([a62e803](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a62e80302f214e87ac942ac6c5dbe9acb25e3c8d))
* update migration agg_address_tx_balance ([ae925f1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/ae925f1f05ad7d4b8d3fbcfced8058ecc7dfc0ba))
* update migration asset_metadata ([99f494c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/99f494c6ff4d3bc721d9e3547a7025d18de0d562))
* update migration files to align with ledger sync v2 ([50d110e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/50d110ec28600cc72bf0e086d433baf9e99093f4))
* update order explorer migration file ([062a1af](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/062a1afc27701eb9008356e355991d0251ee6cd9))
* update order migration file ([211b9cb](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/211b9cb21a8c0aeddd9b4ec1421e81a39c0b3513))
* update redis standalone and cluster to synchronize with other services ([723899a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/723899ab4ae21fca7f5a89056aed2dcc1bdf5899))
* update stake balance when migrate with new lsv2 ([56ec8e2](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/56ec8e2c0413eb52ca6c0b814911dd7eeba7fdd4))
* update UniqueAccountSchedule ([cc1ffb3](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cc1ffb3c581a65bbdf4302cde908c1772ca7c42a))
* use a constant number ([4ff06c1](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4ff06c1210565b315d7ba8e88766be3b76ae53c8))
* using transactionTemplate to wrap dsl query ([f58a7dd](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f58a7dd0dea4d3e97b813ab31cc7366107b1a6bd))
* votingPower must be a double value ([920058a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/920058a43bfb3d4f0e5ec3e1c2c4455158f398d8))
* wrong input param when get existed data ([e6b890d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/e6b890d0b489c191b735215f524f69b2284e7c56))
* wrong logic to update address tx count ([2664901](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2664901054fae849be3a27249e6bcf4f898384eb))
* wrong query to get number of token holder ([087bf75](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/087bf75500d36d17ebf175d2af252406d78ad488))
* wrong query when update address tx count ([9c07833](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9c07833105e6213eb1d7d689448a7c85cc865fd9))
* wrong tx amount by stake ([65afbad](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/65afbad43317e69fc0a956f8c06b34387f29de5d))


### Performance Improvements

* add blockTime checkpoint to make query faster ([783cd4f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/783cd4ff101c256b01358e304affbbd2f5688209))
* add composite index on address_tx_amount table ([87e3d71](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/87e3d712c4890d8b0cd6735ce36d113e668cb87b))
* add composite index on address_tx_amount, token_tx_count table ([05a7473](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/05a74734d252ce2378d0189b47cbbbe00cfd7dda))
* async build token_info (totalVolumes, volumes24h, numberOfHolder) ([cd526cd](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cd526cd1c6d01f5dbd23f1ce71ba8da79490a469))
* async clean-up address balance history ([0d118db](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/0d118db4331ceb1579d69e41c3d75b609ee0cdaa))
* chunk process of update token tx count ([23feca7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/23feca7f8d0e18d3f012c85dffc39885699a8164))
* delete all zero holder after sync data to latest_token_balance ([02f791c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/02f791c8f6404d1e1690fe203bc8103f8a10e871))
* improve perf token info job by remove logic get multi-asset data ([a6b105f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a6b105f034181475b016759f63e89e260e817229))
* make latest token balance init faster ([1e8d86b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/1e8d86b4569f2a91da3ca5bca39e42cb24ac89b5))
* optimize logic to update address tx count ([d78889c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/d78889cf8ace9c8c38ff0773a4adddcdc34c0827))
* optimize query insert latest token balance ([4e1fe7e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/4e1fe7e5e2f9bf3ea1d7a4c3047058917d0cf88a))
* refresh materialized view concurrently ([a153b63](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a153b63a6d98e8044f070087434b6f31d4539176))
* reindexing address_tx_amount table ([cbb6af6](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/cbb6af60ca43667b461238281e9985e9ac3edc7a))
* rollback to best query after perf local testing ([fdb8d9b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fdb8d9bb5d5b4dbb68b03a3306f244e12570564e))
* trying to group token before fetch LatestTokenBalance ([9a793c7](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9a793c7c491a399d8b9f5aebab956e05f362e0c8))
* trying to improve batch processing util ([063e839](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/063e839cde42f911fc2e1306a01db66fbc3b5069))
* update query to insert latest token balance ([af8d54a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/af8d54a63bfc2e10022544134c16cb72a9d0c037))


### Documentation

* removed kafka from README.md ([90a581f](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/90a581ffc117739de5362ac85f2cdfc02129b6c5))
* updated getting started in Readme and added example docker-compose script ([48c48ae](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/48c48ae13065f8ac6bd9543f4b3d8a7a2023a399))


### Miscellaneous Chores

* release 0.2.18 ([a57c88a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/a57c88a4ac285a0b8b74e16e8a362f0e716f6e90))
* release 1.0.0 ([0509f7b](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/0509f7b7cda2a833f7fd02bb720f8085f291d71f))
* release 1.0.0 ([bed6c61](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/bed6c611d6dc1e349225dfafa404951bd2c86f80))
* release 1.1.0 ([f2b3228](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f2b3228baee85c4a0b386a6ee32f6a7c10a5607c))
* release 1.2.0 ([f510905](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/f5109059263ca3c0e264ff78f776bd07d5d1a601))
* release 1.3.0 ([7aba50e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/7aba50e237a433c02fd02ae562f0d233bc04236d))
* release 1.3.1 ([9a7c83d](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/9a7c83de852c5c7a240b3e2aa442c419cc8e7cb9))
* release 1.4.0 ([05fa6ce](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/05fa6ce3d722cbb897f7489f9206cf60b392c9e0))
* release 1.5.0 ([941112e](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/941112eefc0c7ba34beb02ad88d4aa1b3e5b780a))
* release 1.6.0 ([fcb230c](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/fcb230c2efb76fd8b57f73560e04d1d2f289fd3e))
* release 1.7.0 ([67931cf](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/67931cf283a6d4d635ef9f9f5e83f4e2961ed579))

## [0.9.0](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/compare/v0.8.0...v0.9.0) (2024-03-05)


### Bug Fixes

* unique account variable on current epoch equal 0 ([2861e1a](https://github.com/cardano-foundation/cf-ledger-consumer-schedules/commit/2861e1a5c1450b99e2e4532b5450bc58559a26ff))

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
