#config example
#TARGET_VERSION=0.0.1                                                    ---------huawei cloud images repo save version.
ORIGIN_VERSION=1.2.3-SNAPSHOT                                            #---------images version been made "mvn -Pdocker".
#TENANT_NAME=xxxxxxxxxxx                                                 ---------huawei cloud tenant name.
#REPO_ADDRESS=registry.cn-north-1.hwclouds.com                           #---------huawei cloud images repo address.
REPO_ADDRESS=100.125.1.34:20202                                          #---------huawei cloud images repo address.
#USER_NAME=xxxxx                                                         ---------user name: login huawei cloud images repo.
#PW=xxxxxxx                                                              ---------paasword: login huawei cloud images repo.
#CUSTOMER_REPO_NAME=acmeair-customer                                     ---------customer repo name ,created by huawei cloud. 
#BOOKING_REPO_NAME=acmeair-booking                                       ---------booking repo name ,created by huawei cloud. 
#WEBSITE_REPO_NAME=acmeair-website                                       ---------website repo name ,created by huawei cloud. 


export targetversion=1.2.3
#java docker build
cd /opt/sockshop-demo/
#mvn clean  install   -Dmaven.test.skip=true  -settings=/opt/tank/acmeair_settings.xml
mvn clean install -Phuaweicloud -Dmaven.test.skip=true
cd /opt/sockshop-demo/makedocker

cp /opt/sockshop-demo/makedocker/carts/Dockerfile  /opt/sockshop-demo/carts/target/
cp /opt/sockshop-demo/makedocker/orders/Dockerfile  /opt/sockshop-demo/orders/target/
cp /opt/sockshop-demo/makedocker/shipping/Dockerfile  /opt/sockshop-demo/shipping/target/
cp /opt/sockshop-demo/makedocker/queuemaster/Dockerfile  /opt/sockshop-demo/queue-master/target/

cd /opt/sockshop-demo/carts/target/
docker build -t sockshop-carts-service:$ORIGIN_VERSION  . 
cd /opt/sockshop-demo/orders/target/
docker build -t sockshop-orders-service:$ORIGIN_VERSION  . 
cd /opt/sockshop-demo/shipping/target/
docker build -t sockshop-shipping-service:$ORIGIN_VERSION  . 
cd /opt/sockshop-demo/queue-master/target/
docker build -t sockshop-queuemaster-service:$ORIGIN_VERSION  . 

#front-end docker build
cd /opt/sockshop-demo/front-end/
docker build -t sockshop-frontend-service:$ORIGIN_VERSION .

#Go microservice build
cd /opt/sockshop-demo/payment/
docker build -t sockshop-payment-service:$ORIGIN_VERSION .
cd /opt/sockshop-demo/user/
docker build -t sockshop-user-service:$ORIGIN_VERSION .
cd /opt/sockshop-demo/catalogue/
docker build -t sockshop-catalogue-service:$ORIGIN_VERSION .

#docker tag/push
docker tag sockshop-frontend-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-frontend:$targetversion
docker tag sockshop-payment-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-payment:$targetversion
docker tag sockshop-user-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-user:$targetversion
docker tag sockshop-catalogue-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-cat:$targetversion
docker tag sockshop-carts-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-carts:$targetversion
docker tag sockshop-orders-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-orders:$targetversion
docker tag sockshop-shipping-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-shipping:$targetversion
docker tag sockshop-queuemaster-service:$ORIGIN_VERSION  ${REPO_ADDRESS}/hwcse/sockshop-queuemaster:$targetversion

#docker login -u cn-north-1@89WO1KDCRPKDMSGK4KQH -p 21071575be7dbfbc2cfc876141b422d8212509f50ab44346b880e72126565691 ${REPO_ADDRESS}
docker login -u cn-north-1@CEOCLCHHQOZ602DRFQ5L -p 882d640dce0eb45cf833e7aad7f10aa8e5e22fe32ee2cc6fc7b2fd421f37f792 ${REPO_ADDRESS}

docker push ${REPO_ADDRESS}/hwcse/sockshop-frontend:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-payment:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-user:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-cat:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-carts:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-orders:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-shipping:$targetversion
docker push ${REPO_ADDRESS}/hwcse/sockshop-queuemaster:$targetversion



