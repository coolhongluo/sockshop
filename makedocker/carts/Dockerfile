FROM openjdk:8-jre-alpine
ENV APP_ROOT=/root/servicestage/carts/
ENV LOG_ROOT=/var/log/carts/
RUN mkdir -p $APP_ROOT
RUN mkdir -p $LOG_ROOT

COPY ./carts.jar $APP_ROOT
RUN cd $APP_ROOT && chmod -R 770 .
CMD ["/bin/sh","-c","java -jar /root/servicestage/carts/carts.jar >> $LOG_ROOT/carts.log"]

