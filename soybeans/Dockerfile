FROM debian:stable-slim

RUN apt update
RUN apt install -y \
locales python3 python3-cherrypy3 python3-selenium python3-schedule firefox-esr python3-psycopg2 python3-dotenv
RUN apt install -y python3-pip
RUN pip3 install cherrypy-cors peewee
RUN pip3 install jsonpickle
RUN echo "cs_CZ.UTF-8 UTF-8" >> /etc/locale.gen
RUN locale-gen

WORKDIR /app

COPY setup-tools/geckodriver /usr/bin

RUN chmod 777 /usr/bin/geckodriver

COPY . /app
