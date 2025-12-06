#!/bin/bash

# Compile all Java files
javac -d . Main/*.java

# Run Server in background
echo "Starting Server..."
java Main.Server &
SERVER_PID=$!

# Give server time to start
sleep 2

# Run Client in foreground
echo "Starting Client..."
java Main.Client

# Kill server when client exits
kill $SERVER_PID 2>/dev/null