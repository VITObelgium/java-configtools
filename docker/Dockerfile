FROM maven:3.6.1-jdk-11

ARG USER_ID
ARG GROUP_ID
RUN groupadd -g ${GROUP_ID} jenkins \
 && useradd --create-home -u ${USER_ID} -g jenkins jenkins
