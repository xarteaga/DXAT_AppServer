DXAT_AppServer
==============

Temporary how-to for this branch due to the DXAT midterm demo.<br><br>

This branch has the app server and the web server mixed with firsts steps of websocket (for browser side).<br><br>

HOW-TO get it working:<br><br>

1.- git clone https://github.com/xarteaga/DXAT_AppServer.git <br>
2.- git checkout push_data<br>
3.- import as maven project into eclipse<br>
4.- check inconsistencies<br>
4.1 - known solutions (todo's to solve issues once project import is done):<br>
      - import catalinya library:<br>
          - right-click on project on eclipse -> buil path -> configure build path...<br>
          - check libraries tab and click on add external JARs...<br>
          - search your downloaded tomcat directory, go to lib folder and add catalina.jar<br>
      - now, already being on the properties window for the project:<br>
          - go to Project Facets:<br>
              - and check only for Dynamic web module, Java and Javascript<br>
              
      Some errors will appeare due to the default java version loaded. Select 1.6 or greater version.
      
      
