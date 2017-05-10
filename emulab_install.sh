#!/bin/bash

# ------------------------------------------------
# Software installation on Ubuntu 14.04 LTS 64-bit
# Emulab OSID: UBUNTU14-64-STD
# ------------------------------------------------

# install add-apt-repository
sudo apt-get -y install software-properties-common python-software-properties

# install openjdk-8 and maven
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk maven

# configure java binaries
sudo update-alternatives --config java
sudo update-alternatives --config javac

# set up project
cd ~
git clone https://github.com/itdelatrisu/518-craq.git
cd 518-craq/
mvn clean package
