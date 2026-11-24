./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DskipTests
mkdir -p alati/poslije
cp target/bom.json alati/poslije/sbom-cyclonedx-java.json