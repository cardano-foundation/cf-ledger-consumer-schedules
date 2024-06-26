<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
	<activeProfiles>
		<activeProfile>cardano-common</activeProfile>
		<activeProfile>cardano-common-api</activeProfile>
	</activeProfiles>

	<profiles>
		<profile>
			<id>cardano-common</id>
			<repositories>
				<repository>
					<id>cardano-common</id>
					<url>${PRIVATE_MVN_REGISTRY_URL}</url>
					<snapshots>
						<enabled>true</enabled>
					</snapshots>
				</repository>
			</repositories>
		</profile>
		<profile>
			<id>cardano-common-api</id>
			<repositories>
				<repository>
					<id>cardano-common-api</id>
					<url>${PRIVATE_MVN_REGISTRY_URL}</url>
					<snapshots>
						<enabled>true</enabled>
					</snapshots>
				</repository>
			</repositories>
		</profile>
		<profile>
			<id>cardano-common-explorer</id>
			<repositories>
				<repository>
					<id>cardano-common-explorer</id>
					<url>${PRIVATE_MVN_REGISTRY_URL}</url>
					<snapshots>
						<enabled>true</enabled>
					</snapshots>
				</repository>
			</repositories>
		</profile>
	</profiles>
	<servers>
		<server>
			<id>cardano-common</id>
			<username>${PRIVATE_MVN_REGISTRY_USER}</username>
			<password>${PRIVATE_MVN_REGISTRY_PASS}</password>
		</server>
		<server>
			<id>cardano-common-api</id>
			<username>${PRIVATE_MVN_REGISTRY_USER}</username>
			<password>${PRIVATE_MVN_REGISTRY_PASS}</password>
		</server>
		<server>
			<id>cardano-common-explorer</id>
			<username>${PRIVATE_MVN_REGISTRY_USER}</username>
			<password>${PRIVATE_MVN_REGISTRY_PASS}</password>
		</server>
	</servers>
</settings>
