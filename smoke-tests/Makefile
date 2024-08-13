#: cleans up smoke test output
clean-smoke-tests:
	rm -rf ./collector/data.json
	rm -rf ./collector/data-results/*.json
	rm -rf ./report.*

collector/data.json:
	@echo ""
	@echo "+++ Zhuzhing smoke test's Collector data.json"
	@touch $@ && chmod o+w $@

smoke-docker: collector/data.json
	@echo ""
	@echo "+++ Spinning up the smokers."
	@echo ""
	docker-compose up --build collector --detach
	docker-compose up --build android-tests

#npx cypress run --browser chrome

smoke-bats: collector/data.json
	@echo ""
	@echo "+++ Running bats smoke tests."
	@echo ""
	bats ./smoke-e2e.bats --report-formatter junit --output ./

smoke: smoke-docker smoke-cypress smoke-bats

unsmoke:
	@echo ""
	@echo "+++ Spinning down the smokers."
	@echo ""
	docker-compose down --volumes
