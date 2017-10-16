payment app
---
Payment application written in [Go] that check for authorisation of payment.Source Code will be available soon.



## Step1: 
git clone git@github.com:huawei-microservice-demo/payment.git

## Step2:
cd payment

## Step4: Build the docker image
docker build -t [docker-image-name:version-tag] .
put the docker file in same folder and execute the above command.

## Step5: Tag the image with service stage
docker tag [docker-image-name:version-tag]  registry.cn-north-1.hwclouds.com/hwcse/[docker-image-name:version-tag]
Note: hwcse is service stage username.
      registry.cn-north-1.hwclouds.com is service stage available zone.
      
## Step6: Docker login
docker login -u [username] -p [private-key] [registry-name]

## Step7: Docker push
docker push registry.cn-north-1.hwclouds.com/hwcse/[docker-image-name:version-tag]

## Step8: 
Create the appliation in service stage and give the respective docker image path.

## Step9: 
Login to the service stage and check the status of application.
