MVNW := mvn
BUILD_NUMBER := SNAPSHOT

build-all:
	${MVNW} -Dmaven.test.skip=true -Dplugin-version=${BUILD_NUMBER} clean package