#!/bin/sh
set -eu

if [ -n "${MYSQL_CA_FILE:-}" ]; then
    truststore="$(mktemp -t hogwarts-cacerts.XXXXXX)"
    cp "$JAVA_HOME/lib/security/cacerts" "$truststore"
    keytool -importcert -noprompt -alias hogwarts-mysql-ca -file "$MYSQL_CA_FILE" \
        -keystore "$truststore" -storepass changeit
    exec java -Djavax.net.ssl.trustStore="$truststore" -Djavax.net.ssl.trustStorePassword=changeit -jar /app/app.jar
fi

exec java -jar /app/app.jar
