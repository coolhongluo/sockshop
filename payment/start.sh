#!/bin/sh

set -e
set -x

#hceck whether an evn exit
check_env_exist() {
    envname=$1
    envvalue=$2
    if [ -z "$envvalue" ]; then
        /bin/echo "env $envname not exist"
        exit 1
    fi
}

#////////////////////////////////////////////////////#
#          go SDK                                   #
#///////////////////////////////////////////////////#
#check_env_exist "CSE_SERVICE_CENTER" $CSE_SERVICE_CENTER

#name=app/user
#echo $(env)

#export CSE_REGISTRY_ADDR=$CSE_SERVICE_CENTER

listen_addr="0.0.0.0"
advertise_addr=$(ifconfig eth0 | grep -E 'inet\W' | grep -o -E [0-9]+.[0-9]+.[0-9]+.[0-9]+ | head -n 1)
#advertise_addr=$NETWORK_MGNTO_IP

cd payment

#replace ip addr
sed -i s/"listenAddress:\s\{1,\}[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"/"listenAddress: $listen_addr"/g conf/global.yaml
sed -i s/"advertiseAddress:\s\{1,\}[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"/"advertiseAddress: $advertise_addr"/g conf/global.yaml
sed -i s/"address:\s\{1,\}[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"/"address: $SC_HOST"/g conf/global.yaml

./payment --config-dir ./conf

while true; do
    sleep 60
done
