@echo off
if not exist lib\ ( mkdir lib )
if not exist bin\ ( mkdir bin )
if not exist lib\mysql-connector-j.jar (
    echo Downloading MySQL Connector/J JDBC driver...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile 'lib\mysql-connector-j.jar'"
)
echo Compiling Hospital Blood Bank & Donor Matcher application...
javac -d bin -cp "lib\*" src\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Launching Hospital Blood Bank & Donor Matcher GUI...
java -cp "bin;lib\*" src.BloodBankManager
pause
