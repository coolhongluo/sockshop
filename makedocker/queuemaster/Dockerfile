FROM openjdk:8-jre-alpine
ENV APP_ROOT=/root/servicestage/queuemaster/
ENV LOG_ROOT=/var/log/queuemaster/
RUN mkdir -p $APP_ROOT
RUN mkdir -p $LOG_ROOT

COPY ./queue-master.jar $APP_ROOT
RUN cd $APP_ROOT && chmod -R 770 .
CMD ["/bin/sh","-c","java -jar /root/servicestage/queuemaster/queue-master.jar >> $LOG_ROOT/queuemaster.log"]

