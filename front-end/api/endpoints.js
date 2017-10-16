(function () {
    'use strict';

    var util = require('util'),
        express = require("express"),
        request = require("request"),
        fs = require('fs'),
        app = express(),
        Q = require('q'),
        os = require('os'),
        http = require('http'),
        https = require('https'),
        serviceCenterIp = process.env.SC_HOST,
        fs = require('fs'),
        serviceCenterPort = '30100';
    var options1 = {
        host: serviceCenterIp,
        path: '/registry/v3/microservices',
        port: serviceCenterPort,
        rejectUnauthorized: false
    };

    var headerObj
    var basePath = "/var/run/secrets/kubernetes.io/serviceaccount"; var url = ""; fs.readFile(basePath + '/namespace', 'utf8', function (err, data) {
        if (err) { return console.log(err); } var namespace = data;

        fs.readFile(basePath + '/token', 'utf8', function (err, data) {
            if (err) {
                return console.log(err);
            }
            var token = data;
            var options = {
                hostname: process.env.KUBERNETES_SERVICE_HOST,
                port: process.env.KUBERNETES_SERVICE_PORT,
                path: "/api/v1/namespaces/" + namespace + "/secrets/" + namespace + "-secret",
                rejectUnauthorized: false,
                headers: { 'Authorization': 'bearer ' + token },
                method: 'GET'
            };
            var req = https.request(options, (res) => {
                console.log('statusCode:', res.statusCode);
                res.on('data', (d) => {
                    var tokenData = JSON.parse(d);
                    var dockerconfigjson = tokenData['data']['.dockerconfigjson'];
                    var plianText = new Buffer(dockerconfigjson, 'base64').toString()
                    var authToken = JSON.parse(plianText);
                    var lastToken = authToken['auths'];
                    var authTemp = '';
                    for (var key in lastToken) {
                        authTemp = lastToken[key]
                    }
                    var tokenText = new Buffer(authTemp['auth'], 'base64').toString()
                    var index = tokenText.indexOf('@');
                    var project = tokenText.substring(0, index);
                    var aksk = tokenText.substring(tokenText.indexOf('@') + 1)
                    var aksks = aksk.split(':')
                    var ak = aksks[0]
                    var sk = aksks[1]
                    headerObj = { "X-Service-AK": ak, "X-Service-ShaAKSK": sk, "X-Service-Project": project }
                    options1["headers"] = headerObj;
                    process.stdout.write("project:" + project + "   ak:" + ak + "   sk:" + sk + "\n");
                    console.log("options1 after generating ak and sk", options1);
                    createServcie();
                });
            });

            req.on('error', (e) => {
                console.error(e);
            });
            req.end();
        });
    });

    console.log("endpoints.js is loaded");
    var domain = "";

    process.argv.forEach(function (val, index, array) {
        var arg = val.split("=");
        if (arg.length > 1) {
            if (arg[0] == "--domain") {
                domain = "." + arg[1];
                console.log("Setting domain to:", domain);
            }
        }
    });
    var getServiceCenterInstances = function (response) {
        console.log("Inside getServiceCenterInstances() ");
        var str = '';
        response.on('data', function (chunk) {
            str += chunk;
        });
        response.on('end', function () {
            var services = JSON.parse(str).services;
            var serviceDtls = [];
            for (var i = 0; i < services.length; i++) {
                console.log("Inside for loop services[i].serviceName.toLowerCase() !== 'servicecenter'", services[i].serviceName.toLowerCase() !== 'servicecenter', services[i].serviceName.toLowerCase());
                if (services[i].serviceName.toLowerCase() !== 'servicecenter') {
                    var curHeaders = headerObj;
                    curHeaders['X-consumerId'] = serviceId
                    curHeaders['serviceName'] = services[i].serviceName
                    var api = {
                        host: serviceCenterIp,
                        path: '/registry/v3/microservices/' + services[i].serviceId + '/instances',
                        port: serviceCenterPort,
                        rejectUnauthorized: false,
                        headers: curHeaders
                    };
                    https.request(api, getEndpoints).end();
                }
            }
        });


    }
    var serviceId = "";
    var createServcie = function () {
        var serviceOption = {
            host: serviceCenterIp,
            path: '/registry/v3/existence?type=microservice&appId=sockshop&serviceName=frontend&version=0.0.1',
            port: serviceCenterPort,
            method: 'GET',
            rejectUnauthorized: false,
            headers: headerObj
        };
        https.request(serviceOption, function (resp) {
            resp.setEncoding('utf8');
            resp.on('data', function (data) {
                console.info("search service respose:", data)
                if (resp.statusCode === 200) {
                    serviceId = JSON.parse(data)['serviceId']
                    registerInstance(serviceId);
                    https.request(options1, getServiceCenterInstances).end();
                } else {
                    var createOption = {
                        host: serviceCenterIp,
                        path: '/registry/v3/microservices',
                        port: serviceCenterPort,
                        method: 'POST',
                        rejectUnauthorized: false,
                        headers: headerObj
                    };
                    createOption['headers']['Content-Type'] = 'application/json';
                    var body = {};
                    var service = {
                        'serviceName': 'frontend',
                        'alias': 'website',
                        'appId': 'sockshop',
                        'version': '0.0.1',
                        'level': 'FRONT',
                        'status': 'UP',
                        'description': 'The sockshop website service',
                        'schemas': [],
                        'properties': { 'attr1': 'a' },
                        'paths': []
                    };
                    body['service'] = service;
                    var createReq = https.request(createOption, function (resp) {
                        resp.setEncoding('utf8');
                        resp.on('data', function (data) {
                            console.info('create service ', data)
                            serviceId = JSON.parse(data)['serviceId']
                            registerInstance(serviceId);
                            https.request(options1, getServiceCenterInstances).end();
                        })
                    });
                    console.info("create body:", JSON.stringify(body))
                    createReq.write(JSON.stringify(body));
                    createReq.end();
                }
            });
        }).end();
    }


    var registerInstance = function (serviceId) {
        var insOption = {
            host: serviceCenterIp,
            path: '/registry/v3/microservices/' + serviceId + '/instances',
            port: serviceCenterPort,
            method: 'POST',
            rejectUnauthorized: false,
            headers: headerObj
        };
        insOption['headers']['Content-Type'] = 'application/json';
        var insReq = https.request(insOption, function (resp) {
            resp.setEncoding('utf8');
            resp.on('data', (d) => {
                console.info("return:", d);
                var instanceId = JSON.parse(d)['instanceId'];
                insOption['method'] = 'PUT';
                insOption['path'] = '/registry/v3/microservices/' + serviceId + '/instances/' + instanceId + '/heartbeat';
                setInterval(function () {
                    https.request(insOption, function (heatResp) {
                        console.info("Heart beat ok", heatResp.statusCode)
                    }).end();
                }, 30000);
            });
        });
        var body = {}
        var hostName = process.env.HOSTNAME;
        var IPV4
        for (var i = 0; i < os.networkInterfaces().eth0.length; i++) {
            if (os.networkInterfaces().eth0[i].family == 'IPv4') {
                IPV4 = os.networkInterfaces().eth0[i].address;
            }
        }
        var instance = {}
        instance['endpoints'] = [IPV4 + ':8080']
        instance['hostName'] = hostName || IPV4;
        instance['status'] = 'UP';
        instance['stage'] = 'prod';
        body['instance'] = instance;
        console.info("register body:", JSON.stringify(body))
        insReq.write(JSON.stringify(body));
        insReq.end();
    }

    var getEndpoints = function (response) {
        var str = '';
        response.on('data', function (chunk) {
            str += chunk;
        });
        response.on('end', function () {
            try {
                console.log("Inside getEndpoints function with the response", str)
                var instance = JSON.parse(str).instances[0];
                var url = JSON.parse(str).instances[0].endpoints[0].toString().replace("rest", "http");
                console.log("getEndpoiints url --> ", url, "response.req._headers.servicename + Url", response.req._headers.servicename + "Url");
                module.exports[response.req._headers.servicename + "Url"] = util.format(url + "/" + response.req._headers.servicename);
                if (response.req._headers.servicename == "orders" || response.req._headers.servicename == "orders") {
                    module.exports["ordersUrl"] = util.format(url);
                }
                if (response.req._headers.servicename == "cart" || response.req._headers.servicename == "carts") {
                    console.log("cart found in service center");
                    module.exports["cartsUrl"] = util.format(url + "/" + response.req._headers.servicename);
                }
                if (response.req._headers.servicename == "catalogue") {
                    console.log("catalogue found in service center");
                    module.exports["catalogueUrl"] = util.format(url);
                    module.exports["tagsUrl"] = util.format(url + "/");
                }
                if (response.req._headers.servicename == "user") {
                    console.log("user found in service Center");
                    module.exports["customersUrl"] = util.format(url + "/customers");
                    module.exports["addressUrl"] = util.format(url + "/addresses");
                    module.exports["cardsUrl"] = util.format(url + "/cards");
                    module.exports["loginUrl"] = util.format(url + "/login");
                    module.exports["registerUrl"] = util.format(url + "/register");
                }
                console.log("getEndpoints try block--> ", module.exports);
            } catch (error) {
                console.log("getEndpoints catch block--> ", error);
            }

        });
    }
    console.log("About to call the dumb API version2.0.0 ");
    
    module.exports = {     
    };
}());
