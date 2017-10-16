FROM openjdk:8-jre-alpine
ENV APP_ROOT=/root/servicestage/shipping/
ENV LOG_ROOT=/var/log/shipping/
RUN mkdir -p $APP_ROOT
RUN mkdir -p $LOG_ROOT

COPY ./shipping.jar $APP_ROOT
RUN cd $APP_ROOT && chmod -R 770 .
CMD ["/bin/sh","-c","java -jar /root/servicestage/shipping/shipping.jar >> $LOG_ROOT/shipping.log"]

