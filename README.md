DXAT_AppServer
==============

Temporary how-to for this branch due to the DXAT midterm demo.

This branch has the app server and the web server mixed with firsts steps of websocket (for browser side).

HOW-TO get it working:

1.- git clone https://github.com/xarteaga/DXAT_AppServer.git
2.- git checkout push_data
3.- import as maven project into eclipse
4.- check inconsistencies
4.1 - known solutions (todo's to solve issues once project import is done):
      - import catalinya library:
          - right-click on project on eclipse -> buil path -> configure build path...
          - check libraries tab and click on add external JARs...
          - search your downloaded tomcat directory, go to lib folder and add catalina.jar
      - now, already being on the properties window for the project:
          - go to Project Facets:
              - and check only for Dynamic web module, Java and Javascript
              
      Some errors will appeare due to the default java version loaded. Select 1.6 or greater version.
      
      
