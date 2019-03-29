#!/bin/bash
mkdir './osrs'
wget -O './osrs/jagexappletviewer.jar' 'http://oldschool.runescape.com/downloads/jagexappletviewer.jar'
java -javaagent:/home/manpreet/Documents/workspace/mine/runecape-runner/runecapeagent_mvn/target/runecape-agent-1.0-SNAPSHOT.jar -Duser.home='./osrs' -Djava.class.path='./osrs/jagexappletviewer.jar' -Dcom.jagex.config='http://oldschool.runescape.com/jav_config.ws' 'jagexappletviewer' 'oldschool'
