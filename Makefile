.PHONY: up down logs test dev

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

test:
	cd backend && ./mvnw test

dev:
	@echo "Starting backend (port 8080) and frontend (port 5173) for local iteration."
	@echo "Run these in two separate terminals:"
	@echo "  cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run"
	@echo "  cd frontend && npm run dev"
