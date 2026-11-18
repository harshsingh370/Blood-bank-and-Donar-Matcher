#!/bin/bash
cd "$(dirname "$0")"
mkdir -p lib bin

# Auto-download MySQL JDBC driver if missing
if [ ! -s lib/mysql-connector-j.jar ]; then
  echo "Downloading MySQL Connector/J JDBC driver..."
  curl -fL -o lib/mysql-connector-j.jar \
    https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar || {
      echo "Warning: curl failed. Please ensure lib/mysql-connector-j.jar is present."
    }
fi

echo "Compiling Hospital Blood Bank & Donor Matcher application..."
javac -d bin -cp "lib/*" src/*.java || {
  echo "Compilation failed! Check errors above."
  exit 1
}

echo "Launching Hospital Blood Bank & Donor Matcher GUI..."
java --enable-native-access=ALL-UNNAMED -cp "bin:lib/*" src.BloodBankManager
