#!/bin/bash
sudo yum update

echo "Installing Java 8....."
sudo yum install java-1.8.0
sudo yum install java-devel
sudo yum remove java-1.7.0-openjdk

curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
sudo yum install sbt


