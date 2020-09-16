import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:youtube_explode_dart/youtube_explode_dart.dart' as yt;


class YoutubeMensaPlayer extends StatefulWidget {
  @override
  _YoutubeMensaPlayerState createState() => _YoutubeMensaPlayerState();
}

class _YoutubeMensaPlayerState extends State<YoutubeMensaPlayer> {



  bool isLive=false;
  String videoUrl;
  String image;

  tryThis() async {
    yt.YoutubeExplode exploder = yt.YoutubeExplode();

    var channel = await exploder.channels.getUploads((await exploder.channels.get("UC9YB8yAsDGX6kjMZIZQQMPA")).id);

    yt.Video video = await exploder.videos.get(( await channel.first).url); // Returns a Video instance.
    try{
      String manifest= await exploder.videos.streamsClient.getHttpLiveStreamUrl(video.id);
      isLive=true;
    }catch(Exc){
      isLive=false;
    }

    videoUrl=video.url;
    image=video.thumbnails.highResUrl;
    if(mounted){
      setState(() {

      });
    }
  }

  @override
  void initState() {
    super.initState();

    tryThis();
  }

  bool mute=true;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints){
          return Container(
            decoration: BoxDecoration(
              color: Colors.white,
            ),
            child: Column(
              children: <Widget>[


                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(10),
                  decoration: BoxDecoration(
                      color: Color(0xFF184295)
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: <Widget>[

                      Expanded(
                        child: AutoSizeText("Youtube".toUpperCase(),
                          style: TextStyle(color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 20),
                          textAlign: TextAlign.center,
                          minFontSize: 0,
                          maxLines: 1,),

                      ),

                    ],
                  ),
                ),
                Column(
                  children: <Widget>[
                    videoUrl != null ? GestureDetector(onTap: () {
                      tryToLunchUrl(videoUrl);
                    },
                      child: Container(
                        height: constraints.maxWidth*9/16,
                        width: constraints.maxWidth,

                        decoration: BoxDecoration(
                            image: DecorationImage(
                                fit: BoxFit.cover,
                                image: NetworkImage(image)
                            )
                        ),
                      ),) : Container(
                      height: constraints.maxWidth*9/16,
                      width: constraints.maxWidth,
                      decoration: BoxDecoration(
                          color: Theme
                              .of(context)
                              .accentColor
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          CircularProgressIndicator(
                            valueColor: new AlwaysStoppedAnimation<Color>(
                                Colors.white),)
                        ],
                      ),
                    ),
                    GestureDetector(
                      onTap: () {
                        tryToLunchUrl(
                            "https://www.youtube.com/channel/UC9YB8yAsDGX6kjMZIZQQMPA");
                      },
                      child: Container(
                        padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                        decoration: BoxDecoration(
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment
                              .spaceBetween,
                          children: <Widget>[
                            Expanded(
                              child: AutoSizeText("Vedi tutti i video",
                                style: TextStyle(color: Theme
                                    .of(context)
                                    .accentColor),
                                minFontSize: 0,
                                maxLines: 2,),
                            ),
                            Icon(
                                Icons.arrow_forward,
                                color: Theme
                                    .of(context)
                                    .accentColor
                            )

                          ],
                        ),
                      ),
                    ),
                  ],
                ),


              ],
            ),
          );
        });
  }



  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }
}







