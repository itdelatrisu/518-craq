#!/bin/bash

# ------------------------------------------------
# Software installation on Ubuntu 14.04 LTS 64-bit
# Emulab OSID: UBUNTU14-64-STD
# ------------------------------------------------
# sudo apt-get -y install software-properties-common python-software-properties
# sudo add-apt-repository ppa:openjdk-r/ppa -y
# sudo apt-get update
# sudo apt-get -y install openjdk-8-jdk maven screen
# sudo update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# ------------------------------------------------
# Software installation on Ubuntu 16.04 LTS 64-bit
# Emulab OSID: UBUNTU16-64-STD
# ------------------------------------------------
sudo apt-get update
sudo apt-get -y install default-jdk maven screen
