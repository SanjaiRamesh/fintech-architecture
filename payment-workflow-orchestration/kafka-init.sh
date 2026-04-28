#!/bin/bash
# Create required Kafka topics before starting the services.
# Adjust KAFKA_HOME or use kafka-topics.sh from your Kafka installation.
#
# Usage:
#   bash kafka-init.sh
#   or with a custom broker:
#   BOOTSTRAP=myhost:9092 bash kafka-init.sh

BOOTSTRAP=${BOOTSTRAP:-localhost:9092}

echo "Creating Kafka topics on $BOOTSTRAP ..."

kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic payment.events \
  --partitions 12 \
  --replication-factor 1

kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic audit.logs \
  --partitions 6 \
  --replication-factor 1 \
  --config retention.ms=7776000000

kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic notification.requests \
  --partitions 6 \
  --replication-factor 1

echo "Topics created:"
kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list | grep -E "payment|audit|notification"
