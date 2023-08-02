MVNW := mvn
BUILD_NUMBER := SNAPSHOT
OPENAPI_VERSION := SNAPSHOT

install:
	cd teamcity_raw; ${MVNW} -f install_pom.xml package

build-all:
	${MVNW} -Dteamcity_version=${OPENAPI_VERSION} -Dplugin-version=${BUILD_NUMBER} clean package