
- run all tests
```shell script
./gradlew clean :test
```

- build no tests
```shell script
./gradlew build -x test
```

- generate javaDoc
```shell script
./gradlew generateJavadoc
```

- publish package to mavenlocal()
```shell script
./gradlew publishToMavenLocal
```

- publish package to GitLab no checksum
```shell script
./gradlew -Dorg.gradle.internal.publish.checksums.insecure=true publishMavenJavaPublicationToGitlabRepository
```

- local build for central publish
```shell script
./gradlew publishMavenJavaPublicationToLocalWithChecksumsRepository
```
- remove data & prepare archive
```shell script
./prepare_release.sh
```
