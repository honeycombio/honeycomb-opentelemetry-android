
build:
	./gradlew build

test:
	./gradlew test

clean: clean-smoke-tests
	./gradlew clean

#: cleans up smoke test output
clean-smoke-tests:
	rm -rf ./smoke-tests/collector/data.json
	rm -rf ./smoke-tests/collector/data-results/*.json
	rm -rf ./smoke-tests/report.*

smoke-tests/collector/data.json:
	@echo ""
	@echo "+++ Zhuzhing smoke test's Collector data.json"
	@touch $@ && chmod o+w $@

smoke-docker: smoke-tests/collector/data.json
	@echo ""
	@echo "+++ Spinning up the smokers."
	@echo ""
	docker-compose up --build collector --detach

android-emulator:
	@echo ""
	@echo "+++ Setting up Android environment."
	@echo ""
	bash ./setup-android-env.sh

android-test: android-emulator
	@echo ""
	@echo "+++ Running Android tests."
	@echo ""
	./gradlew pixel8api35debugAndroidTest --rerun

smoke-bats: smoke-tests/collector/data.json
	@echo ""
	@echo "+++ Running bats smoke tests."
	@echo ""
	cd smoke-tests && bats ./smoke-e2e.bats --report-formatter junit --output ./

smoke: smoke-docker android-test smoke-bats

unsmoke:
	@echo ""
	@echo "+++ Spinning down the smokers."
	@echo ""
	docker-compose down --volumes
