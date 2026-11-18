#!/bin/sh
set -u

if [ "${RABBITMQ_EMBUTIDO:-true}" = "true" ]; then
  echo "Subindo o RabbitMQ neste container."
  export RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS="+S 1:1"
  /usr/local/bin/docker-entrypoint.sh rabbitmq-server &
  i=0
  while [ "$i" -lt 60 ]; do
    if rabbitmq-diagnostics -q ping; then
      echo "RabbitMQ pronto."
      break
    fi
    i=$((i + 1))
    sleep 1
  done
  if ! rabbitmq-diagnostics -q ping; then
    echo "RabbitMQ nao subiu. A API continua e o aviso fica sem broker." >&2
  fi
fi

exec java -jar /app/app.jar
