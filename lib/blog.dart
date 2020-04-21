import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:html/dom.dart' as dom;
import 'package:mensa_italia/login.dart';
import 'package:page_transition/page_transition.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webfeed/domain/rss_feed.dart';

import 'home_full.dart';
import 'main.dart';


class BlogBlock extends StatefulWidget {
  @override
  _BlogBlockState createState() => _BlogBlockState();
}

class _BlogBlockState extends State<BlogBlock> {



  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }

  RssFeed rssFeed;
  Future init() async {
    rssFeed = new RssFeed.parse(await API().getBlogEvent());
    isPreparing=false;
    setState(() {

    });
  }
  bool isPreparing=true;
  @override
  Widget build(BuildContext context) {
    return CardClipperElements(
        Column(
          children: <Widget>[
            Container(
              width: double.infinity,
              padding: EdgeInsets.all(10),
              decoration: BoxDecoration(
                  color: Color(0xFF184295)
              ),
              child: AutoSizeText("Eventi".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
            ),
            isPreparing?Container(
              margin: EdgeInsets.only(top: 50),
              child: LoadingDialog(),
            ):Column(
              children:List.generate(1, (i){
                if(rssFeed.items.elementAt(i).media.contents.first.url!=""){
                  return EventItem(rssFeed.items.elementAt(i).link,rssFeed.items.elementAt(i).media.contents.first.url);
                }else{
                  return Container();
                }

              }),
            ),

            GestureDetector(
              onTap: (){

                Navigator.push(context, PageTransition(child: BlogMensa(title:"Eventi"), type: PageTransitionType.rightToLeft));
              },
              child: Container(
                padding: EdgeInsets.all(20),
                decoration: BoxDecoration(
                ),
                child:Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Expanded(
                      child: AutoSizeText("Vedi tutti gli eventi", style: TextStyle(color: Theme.of(context).accentColor),),
                    ),
                    Container(
                      width: 40,
                    ),
                    Icon(
                        Icons.arrow_forward,
                        color: Theme.of(context).accentColor
                    )

                  ],
                ),
              ),
            ),
          ],
        ),
    );
  }
}

class CardClipperElements extends StatelessWidget {

  Widget child;

  CardClipperElements(this.child);


  @override
  Widget build(BuildContext context) {
    return Card(

      elevation: 3.0,
      margin: EdgeInsets.only(left: 10, right: 10, top: 5, bottom: 5),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: child,
      ),
    );
  }
}



class BlogMensa extends StatefulWidget {

  String title;

  BlogMensa({this.title});

  @override
  _BlogMensaState createState() => _BlogMensaState();
}

class _BlogMensaState extends State<BlogMensa> {


  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    init();
  }

  RssFeed rssFeed;
  Future init() async {
    rssFeed = new RssFeed.parse(await API().getBlogEvent());
    isPreparing=false;
    setState(() {

    });
  }
  bool isPreparing=true;

  @override
  Widget build(BuildContext context) {

    return Scaffold(


      appBar: AppBar(
        title: AutoSizeText((widget.title??"Mensa Italia").toUpperCase()),
        actions: <Widget>[
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              widget.title!=null?Container():GestureDetector(
                onTap: () async {

                  SharedPreferences prefs = await SharedPreferences.getInstance();
                  await prefs.setBool("isJumped", false);
                  Navigator.pushReplacement(context, PageTransition(child: HomePage(), type: PageTransitionType.rightToLeft));
                },
                child: AutoSizeText("ACCEDI")
              )
            ],
          ),
          widget.title!=null?Container():Container(width: 20,)
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: <Widget>[


            widget.title!=null?Container():SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: <Widget>[
                  GestureDetector(
                    onTap: () async {
                      tryToLunchUrl('https://www.mensaitalia.it/cose-il-mensa/');
                    },
                    child: Container(
                      margin: EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                              width: 60,
                              height: 60,
                              margin: EdgeInsets.only(bottom: 10),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                  color: Theme.of(context).accentColor,
                                  borderRadius: BorderRadius.circular(200)
                              ),
                              child: Icon(Icons.whatshot, color: Colors.white,)
                          ),

                          AutoSizeText("Cosa siamo".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                        ],
                      ),
                    ),

                  ),
                  GestureDetector(
                    onTap: () async {
                      tryToLunchUrl('https://www.mensaitalia.it/storia/');
                    },
                    child: Container(
                      margin: EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                              width: 60,
                              height: 60,
                              margin: EdgeInsets.only(bottom: 10),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                  color: Theme.of(context).accentColor,
                                  borderRadius: BorderRadius.circular(200)
                              ),
                              child: Icon(Icons.change_history, color: Colors.white,)
                          ),

                          AutoSizeText("storia".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                        ],
                      ),
                    ),

                  ),
                  GestureDetector(
                    onTap: () async {
                      tryToLunchUrl('https://www.mensaitalia.it/statuto/');
                    },
                    child: Container(
                      margin: EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                              width: 60,
                              height: 60,
                              margin: EdgeInsets.only(bottom: 10),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                  color: Theme.of(context).accentColor,
                                  borderRadius: BorderRadius.circular(200)
                              ),
                              child: Icon(Icons.wb_incandescent, color: Colors.white,)
                          ),

                          AutoSizeText("statuto".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                        ],
                      ),
                    ),

                  ),
                  GestureDetector(
                    onTap: () async {
                      tryToLunchUrl('https://www.mensaitalia.it/domande-frequenti/');
                    },
                    child: Container(
                      margin: EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                              width: 60,
                              height: 60,
                              margin: EdgeInsets.only(bottom: 10),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                  color: Theme.of(context).accentColor,
                                  borderRadius: BorderRadius.circular(200)
                              ),
                              child: Icon(Icons.question_answer, color: Colors.white,)
                          ),

                          AutoSizeText("domande".toUpperCase(), style: TextStyle(color: Theme.of(context).accentColor, fontWeight: FontWeight.bold, fontSize: 10),textAlign: TextAlign.center,),
                        ],
                      ),
                    ),

                  ),

                ],
              ),
            ),


            widget.title!=null?Container():Container(
              width: double.infinity,
              padding: EdgeInsets.all(10),
              decoration: BoxDecoration(
                  color: Color(0xFF184295)
              ),
              child: AutoSizeText("EVENTI".toUpperCase(), style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 20),textAlign: TextAlign.center,),
            ),

            isPreparing?Container(
              margin: EdgeInsets.only(top: 50),
              child: LoadingDialog(),
            ):Column(
              children:List.generate(rssFeed.items.length, (i){
                if(rssFeed.items.elementAt(i).media.contents.first.url!=""){
                  return EventItem(rssFeed.items.elementAt(i).link,rssFeed.items.elementAt(i).media.contents.first.url);
                }else{
                  return Container();
                }

              }),
            )

          ],
        ),
      ),

    );
  }

  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }
}


class EventItem extends StatefulWidget {
  String url;
  String image;
  EventItem(this.url,this.image);
  @override
  _EventItemState createState() => _EventItemState();
}

class _EventItemState extends State<EventItem> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: (){
        tryToLunchUrl(widget.url);
      },
      child: Card(
        margin: EdgeInsets.all(10),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
        elevation: 0.0,
        child: Container(
          width: MediaQuery.of(context).size.width,
          decoration: BoxDecoration(
              image: DecorationImage(image: CachedNetworkImageProvider(widget.image)),
              borderRadius: BorderRadius.circular(25)
          ),
          child: Opacity(opacity: 0.0, child: CachedNetworkImage(imageUrl:widget.image, width: MediaQuery.of(context).size.width,),),
        ),
      ),
    );
  }

  tryToLunchUrl(String url) async {

    if (await canLaunch(url)) {
      await launch(url);
    }
  }
}



