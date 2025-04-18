include .env

.PHONY: test build-orchestrator build-proxy build-all run-test install

build-orchestrator:
	cd ./services/orchestrator; docker build -t fit-orchestrator:latest .

build-proxy:
	cd ./services/proxy; docker build -t fit-proxy:latest .

build-all: build-orchestrator build-proxy

run-test:
	cd ./lib; mvn test

install:
	mvn install -Dmaven.test.skip
