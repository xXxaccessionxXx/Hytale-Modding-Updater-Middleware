@echo off
echo Starting Hytale Server with Updater Middleware attached...

:: Standard server launch looks like this:
:: java -jar hytale-server.jar

:: To attach our Middleware Watcher and Updater as a Java Agent,
:: we prepend the -javaagent flag before the server's jar!
:: 
:: Syntax: -javaagent:<path_to_agent_jar>[=agent_arguments]

java -javaagent:target/middleware-agent.jar -jar hytale-server.jar

pause
