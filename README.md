[![Build Status](https://travis-ci.org/microservices-demo/microservices-demo.svg?branch=master)](https://travis-ci.org/microservices-demo/microservices-demo)

# Sock Shop : A Microservice Demo Application

The application is the user-facing part of an online shop that sells socks. It is intended to aid the demonstration and testing of microservice and cloud native technologies.

It is built using [Spring Boot](http://projects.spring.io/spring-boot/), [Go kit](http://gokit.io) and [Node.js](https://nodejs.org/) and is packaged in Docker containers.

You can read more about the [application design](./internal-docs/design.md).

## Bugs, Feature Requests and Contributing

We'd love to see community contributions. We like to keep it simple and use Github issues to track bugs and feature requests and pull requests to manage contributions. See the [contribution information](.github/CONTRIBUTING.md) for more information.

## Screenshot

![Sock Shop frontend](https://github.com/microservices-demo/microservices-demo.github.io/raw/master/assets/sockshop-frontend.png)
## Repository Contents

Source:

- **carts**: The Java microservice which pushes the items into cart.
- **orders**: The Java microservice which handles the orders of users.
- **shipping**: The Java microservice which handles shippment task given by orders.
- **queuemaster**: The Java microservice which handles shippment task given by shipping app.
- **user**: The Go microservice which handles the user related information.
- **payment**: The Go microservice which handles the payment authentication.
- **catalogue**: The Go microservice which has all the catalogue of socks.
- **frontend**: The Nodejs microservice which handles the request from USER and call appropriate microservice.

## How to build

* Development Environment
  
  * Install [Maven](https://maven.apache.org/) to build the code.
  * We use [Docker](https://www.docker.com/) to run the integration test.
   
* Instructions for build the code base

  You can build the code by using maven from the root without running the test
        
      mvn clean install -Phuaweicloud -Dmaven.test.skip=true
      
      
## Sockshop AUTO CI/CD(based on the Service Stage )
本章节介绍基于华为微服务云应用平台（[Service Stage](https://servicestage.hwclouds.com)），实现自动编译、构建、部署、运行。

### auto build
####  环境准备 
* linux  
* docker 1.11.2(当前只支持该版本)   
* mvn 3.x  
* jdk 1.8+  
* Register the Service Stage account, create a cluster (eg sockshop), add cluster node resources, and create the corresponding depot addresses for each micro service image.
#### Steps
* Download sockshop-demo code to linux machine.
* Modify the root directory script ./scripts/dockerbuild.sh, set the script to depend on the environment variable, refer to the comments in the script to modify.
* Execute the script dockerbuild.sh, complete the compilation, mirror production, mirror upload to the Service Stage repository. The same time as

### auto deploy
** If you want to directly see the results of the deployment, you can not build their own, directly using the existing mirror, refer to the following steps to achieve acmeair deployment and application access. **

* Import the automatic deployment template, enter the Service Stage system, click to enter: application on the line> application layout> template, and then click on the 'create template', will write. /scripts/sockshop-blueprint-deploy-template-v1.tar.gz Automate deployment of templates can be imported.
* After creating the template, click the deployment menu on the template to complete the deployment automatically (provided that your cluster name is: acmeair, the deployment of the mirror address and template in the same; if the cluster and mirror address custom, please modify the template corresponding Field).
* Application list interface, see sockshop-frontend application details page, click on the 'visit address' in the application access address can be acmeeair system.

## sockshop Micro Service Governance
### Load balancing
* Increase the number of application examples: Apply the application line, click 'sockshop-carts', enter the application details, click on the 'upgrade', configure the number of instances 2, save the container configuration, click on the 'upgrade', the upgrade is successful, the instance page increases Newly created instance.
* View Dashboard: Application Development> Micro Service Management> Dashboard, click 'carts' to display two instances of the service and real-time data for each instance (total number of requests success, request rate per second, CPU usage, etc.) The
* Access the sockshop demo application login the user details. Choose your favourite socks and move to carts.
* Once shoping is done plae the order with patyment and address details.
* Once payment authentication is done then items will be shipped.

    
      
