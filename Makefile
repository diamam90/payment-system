DOCKER_COMPOSE = docker-compose
NEXUS_URL = http://localhost:8800
INFRA_SERVICES ?= nexus keycloak keycloak-postgres person-db prometheus loki tempo grafana kafka kafka-ui transaction-db1 transaction-db2 transaction-db3

.PHONY: all up start stop logs rebuild infra infra-logs infra-stop

all: up build-artifact start

ifeq ($(OS),Windows_NT)
WAIT_CMD = powershell -Command "while ($$true) { \
		try { \
			Invoke-WebRequest -UseBasicParsing -Uri $(NEXUS_URL)/service/rest/v1/status -ErrorAction Stop; \
			break \
		} \
		catch { \
			Write-Host 'Nexus not ready, sleeping...'; \
			Start-Sleep -Seconds 5 \
		} \
	}"
else
WAIT_CMD = until curl -I $(NEXUS_URL)/service/rest/v1/status; do \
echo 'Nexus not ready, sleeping...'; sleep 5; \
done
endif 

up: 
	$(DOCKER_COMPOSE) up -d nexus
	@echo "Waiting Nexus . . ."
	@$(WAIT_CMD)
	@echo "Nexus ready"

build-artifact: up
	$(DOCKER_COMPOSE) build person-service --no-cache
	$(DOCKER_COMPOSE) build transaction-service --no-cache

start: 
	$(DOCKER_COMPOSE) up -d

down:
	$(DOCKER_COMPOSE) down -v

clean: down
	rm -rf ./person-service/build
	rm -rf ./transaction-service/build

logs: 
	$(DOCKER_COMPOSE) logs -f --tail=200

infra-up:
	@echo "Start infrastructure services: $(INFRA_SERVICES)"
	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
	@echo "Waiting Nexus . . ."
	@$(WAIT_CMD)

infra-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

infra-down:
	$(DOCKER_COMPOSE) down -v $(INFRA_SERVICES)

rebuild: clean all
