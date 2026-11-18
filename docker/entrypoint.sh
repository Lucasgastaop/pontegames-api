#!/bin/sh
set -u

if [ "${RABBITMQ_EMBUTIDO:-true}" = "true" ]; then
  echo "Subindo o RabbitMQ neste container."
  # O hostname do Render e longo demais e o no do Erlang nao autentica.
  export RABBITMQ_NODENAME=rabbit@localhost
  export RABBITMQ_USE_LONGNAME=false
  export RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS="+S 1:1"
  # Sem plugin HTTP. Se o RabbitMQ abrir 15672, o Render manda o site para ele e tudo volta 404.
  printf '%s\n' '[].' > /etc/rabbitmq/enabled_plugins
  /usr/local/bin/docker-entrypoint.sh rabbitmq-server &
fi

exec java -jar /app/app.jar
