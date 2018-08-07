This is a sample Java source code of data pipeline application that I coded in South Korea.

I revised something in the source codes due to security issues.


How it works

At the first stage, both of xml files should be uploaded to data source server and data destnation server to be used in applications.

And then, "SrcDataColct" application should be installed data source server while "SrcDataRecptn" application should be installed data destination server.

"SrcDataColct" application doing following jobs: 
1. Connects data source's database
2. Selects raw data from the database
3. Makes a file in XML format using the raw data 
4. Transmits XML file and related files(Doc, ppt, xls, etc.) to the desination server using sftp

"SrcDataRecptn" application doing following jobs: 
1. Connects data destination's database
2. Reads transmitted XML files from data source's server 
3. Processes and pushes the data from XML files into database 
4. If job is done correctly, makes backup files of XML and deletes transmitted XML files
5. Trasmits related files to front-end web server to provide download service 
   - Website users cannot connect data destination server becuase it's internal server, 
     so I had to transmit again to the front-end web server that can connect both of internal and external
