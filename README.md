# Sametime

Play Youtube videos at the same time as your friend(s)!

## Details

This application uses Play Framework, ScalaJS, and Scala. It is meant to run a server,
which has a simple UI for creating _rooms_. A _room_ consist of a video and a number, indicating how many people should be present before the video starts. Upon connection, the client will connect to the server via a websocket and indicate that it is ready to play. When enough clients are ready, the server will send a play message to all clients. The server does not take latency into account, so the video will probably be off. Most of the times, the offset should be small enough to not cause discomfort. 
