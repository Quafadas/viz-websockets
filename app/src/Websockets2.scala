package app

import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedTextMessage, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange
import scalatags.Text.all._

object Websockets2 extends cask.MainRoutes{

  @cask.get("/")
  def home() = {
    val name = "haoyi"
    val test = ""
    html(
      head(
        script(src :="https://cdn.jsdelivr.net/npm/vega@5"),
        script(src :="https://cdn.jsdelivr.net/npm/vega-lite@5"),
        script(src :="https://cdn.jsdelivr.net/npm/vega-embed@5")        
      ),
      body(
        //h1("viz"),
        div(id:="vis", height:="99vmin", width:="99vmin"),
        script(raw"""        
        let socket = new WebSocket('ws://localhost:8080/connect/viz');
        socket.onopen = function(e) {
          document.getElementById('vis').innerHTML = 'connected and waiting'
        };
        socket.onmessage = function(event) {
          console.log(event.data)
          const spec = JSON.parse(event.data)
          vegaEmbed('#vis', spec, {
            renderer: 'canvas', // renderer (canvas or svg)
            container: '#vis', // parent DOM container
            hover: true, // enable hover processing
            actions: true
        }).then(function(result) {
            console.log(`rendered spec`);
          })
        console.log(`Data received from server`);
        };

        socket.onclose = function(event) {
          if (event.wasClean) {
            console.log(`[close] Connection closed cleanly, code=$${event.code} reason=$${event.reason}`);
          } else {
            console.error('[close] Connection died');
          }
        };
        socket.onerror = function(error) {
          console.error(`[error] $${error.message}`);
        };
        
        """)
      )
    )
    
  }

  @cask.post("/viz")
  def recievedSpec(request: cask.Request) = {
    
    val theBody = ujson.read(request.text())
    channelCheat match {
      case None => cask.Response("no client is listening", statusCode = 418)
      case Some(value) => {
        println(theBody)
        WebSockets.sendTextBlocking(ujson.write(theBody), value)
        cask.Response("you should be looking at new viz")
      } 
    }    
  }

  var channelCheat : Option[WebSocketChannel] = None

  @cask.websocket("/connect/:userName")
  def showUserProfile(userName: String): cask.WebsocketResult = {
    
    new WebSocketConnectionCallback() {      
      override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
        channelCheat = Some(channel)
        channel.getReceiveSetter.set(
          new AbstractReceiveListener() {
            override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) = {
              message.getData match{
                case "" => channel.close()
                case data => WebSockets.sendTextBlocking(userName + " " + data, channel)
              }
            }
          }
        )
        channel.resumeReceives()
      }
    }
  }

  initialize()
}
