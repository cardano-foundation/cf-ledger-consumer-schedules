init:
	git config core.hooksPath .githooks
compose-up:
	docker compose --env-file .env.${network} -p schedules-${network} up -d --build
