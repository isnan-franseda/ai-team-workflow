.PHONY: dev-up dev-down dev-reset

dev-up:
	docker compose up -d

dev-down:
	docker compose down

dev-reset:
	docker compose down -v
	docker compose up -d
