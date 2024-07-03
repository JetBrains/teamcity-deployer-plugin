MVNW := mvn
BUILD_NUMBER := SNAPSHOT

build-all:
	${MVNW} -Dmaven.test.skip=true -Dplugin-version=${BUILD_NUMBER} clean package

all:
	${MVNW} -Daether.enhancedLocalRepository.trackingFilename=some_nonexistent_dummy_file_name package