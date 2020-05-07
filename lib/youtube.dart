






import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia/blog.dart';
import 'package:youtube_player_flutter/youtube_player_flutter.dart';

class YoutubeMensaPlayer extends StatefulWidget {
  @override
  _YoutubeMensaPlayerState createState() => _YoutubeMensaPlayerState();
}

class _YoutubeMensaPlayerState extends State<YoutubeMensaPlayer> {

  YoutubePlayerController _controller = YoutubePlayerController(
    initialVideoId: YoutubePlayer.convertUrlToId("https://www.youtube.com/watch?v=dP72jwZLyqQ"),
    flags: YoutubePlayerFlags(
      autoPlay: true,
      mute: false,
    ),
  );

  @override
  Widget build(BuildContext context) {
    return    CardClipperElements(      Column(
      children: <Widget>[
        Container(
          width: double.infinity,
          padding: EdgeInsets.all(10),
          decoration: BoxDecoration(
              color: Color(0xFF184295)
          ),
          child: AutoSizeText("ULTIMA LIVE".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
        ),
        YoutubePlayer(
          controller: _controller,
          showVideoProgressIndicator: true,
          progressIndicatorColor: Theme.of(context).accentColor,
          liveUIColor: Theme.of(context).accentColor,
          onReady: () {
          },
        )
      ],
    )
        );
  }
}







