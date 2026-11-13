./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DskipTests
mkdir -p alati/prije
cp target/bom.json alati/prije/sbom-cyclonedx-java.json