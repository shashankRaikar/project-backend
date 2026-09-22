@echo off
set "JAVA_HOME=C:\Users\Lenovo\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.12.v20260826-1216\jre"
set "PATH=%JAVA_HOME%\bin;C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin;%PATH%"

echo Starting Spring Boot backend on http://localhost:3030...
mvn spring-boot:run
