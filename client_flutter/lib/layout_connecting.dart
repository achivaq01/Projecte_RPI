import 'package:flutter/cupertino.dart';

class LayoutConnecting extends StatelessWidget {
  const LayoutConnecting({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text("WebSockets Client"),
      ),
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: const [
          SizedBox(height: 75),
          Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                "Connecting...",
                style: TextStyle(
                  fontSize: 14,
                  color: Color.fromRGBO(0, 200, 0, 1),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
