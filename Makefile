init:
	git config core.hooksPath .githooks
compose-up:
	docker compose --env-file .env.${network} -p schedules-${network} up -d --build

mutation:
    mvn test-compile org.pitest:pitest-maven:mutationCoverage -DmutationThreshold=12