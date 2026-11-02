# Jenkins pret pour les pipelines du projet : plugins installes + client Docker
# (les etapes Maven, Node, sonar-scanner et kubectl tournent dans des conteneurs Docker).
FROM jenkins/jenkins:2.479.1-lts-jdk17

USER root
# Client Docker : Jenkins utilise le Docker de la machine (socket monte par docker-compose)
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
USER jenkins

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
