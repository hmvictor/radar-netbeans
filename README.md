# radar-netbeans

Radar is a plugin for Netbeans to navigate directly from the issue to the code without leaving your IDE.

You can retrieve issues from a server or you can run a local analysis.


Notes
The plugin uses de web service API of sonar for remote retrieving of issues, so there can be some limitations on amount of displayed information. For example, at this moment, the maximum number of retrieved issues per request is 10,000.

It works from SonarQube 3.7 to SonarQube 4.5.
